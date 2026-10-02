"""
Cart tools: add_to_cart, update_cart_item, remove_from_cart, view_cart, apply_promotion.
Mọi thao tác cart đều đọc/ghi trực tiếp Redis session (qua SessionState), không cần gọi service Java.
"""

import uuid
from typing import Any, Dict

from app.clients.core_service_client import CoreServiceClient
from app.config import settings
from app.session.models import CartItem, CartSnapshot, SessionState

# ─── JSON Schemas ──────────────────────────────────────────────────────────────

ADD_TO_CART_SCHEMA = {
    "name": "add_to_cart",
    "description": "Thêm một món vào giỏ hàng của phiên hiện tại, kèm topping (bằng topping_id lấy từ get_menu) và ghi chú.",
    "input_schema": {
        "type": "object",
        "properties": {
            "session_id": {"type": "string", "description": "🔒 system-injected"},
            "item_id": {"type": "string"},
            "quantity": {"type": "integer", "minimum": 1},
            "topping_ids": {
                "type": "array",
                "items": {"type": "string"},
                "description": "Danh sách topping_id, lấy từ get_menu — KHÔNG dùng tên topping tự do",
            },
            "note": {"type": "string", "description": "Ghi chú riêng cho món, ví dụ 'ít cay'"},
        },
        "required": ["session_id", "item_id", "quantity"],
    },
}

UPDATE_CART_ITEM_SCHEMA = {
    "name": "update_cart_item",
    "description": "Sửa số lượng, topping hoặc ghi chú của một dòng đã có trong giỏ hàng.",
    "input_schema": {
        "type": "object",
        "properties": {
            "session_id": {"type": "string", "description": "🔒 system-injected"},
            "cart_item_id": {"type": "string", "description": "ID dòng trong giỏ, lấy từ view_cart"},
            "quantity": {"type": "integer", "minimum": 0, "description": "0 = xoá dòng này khỏi giỏ"},
            "topping_ids": {"type": "array", "items": {"type": "string"}},
            "note": {"type": "string"},
        },
        "required": ["session_id", "cart_item_id"],
    },
}

REMOVE_FROM_CART_SCHEMA = {
    "name": "remove_from_cart",
    "description": "Xoá một dòng khỏi giỏ hàng.",
    "input_schema": {
        "type": "object",
        "properties": {
            "session_id": {"type": "string", "description": "🔒 system-injected"},
            "cart_item_id": {"type": "string"},
        },
        "required": ["session_id", "cart_item_id"],
    },
}

VIEW_CART_SCHEMA = {
    "name": "view_cart",
    "description": "Xem lại nội dung giỏ hàng hiện tại, mã khuyến mãi đang áp dụng (nếu có) và tổng tiền tạm tính.",
    "input_schema": {
        "type": "object",
        "properties": {"session_id": {"type": "string", "description": "🔒 system-injected"}},
        "required": ["session_id"],
    },
}

APPLY_PROMOTION_SCHEMA = {
    "name": "apply_promotion",
    "description": "Áp dụng mã khuyến mãi (voucher) vào giỏ hàng để nhận giảm giá. Nếu người dùng không nêu mã cụ thể hoặc nói 'áp voucher', để promo_code là 'auto' để hệ thống tự chọn mã tốt nhất.",
    "input_schema": {
        "type": "object",
        "properties": {
            "promo_code": {
                "type": "string",
                "description": "Mã khuyến mãi (ví dụ: CUABAC15, CHUNGCU15K). Để 'auto' nếu muốn tự động tìm và áp dụng voucher tốt nhất.",
            },
        },
    },
}

GET_PROMOTIONS_SCHEMA = {
    "name": "get_promotions",
    "description": "Lấy danh sách các mã giảm giá, voucher khuyến mãi có sẵn của quán và toàn sàn.",
    "input_schema": {
        "type": "object",
        "properties": {
            "shop_id": {"type": "string", "description": "ID hoặc tên của quán (tùy chọn)"},
        },
    },
}

# ─── Handlers ─────────────────────────────────────────────────────────────────


async def handle_get_promotions(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)
    shop_id = tool_input.get("shop_id") or state.cart.shop_id
    promos = await core.get_promotions(shop_id=shop_id)
    return {"promotions": promos, "count": len(promos)}


async def handle_add_to_cart(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    """
    Thêm CartItem vào state.cart. Invalidate order_confirmed nếu giỏ thay đổi.
    Tự động tìm kiếm giá và tên món ăn thật từ Core Service nếu chưa có.
    """
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)

    item_id_or_name = str(tool_input.get("item_id", "")).strip()
    item_name = tool_input.get("item_name") or item_id_or_name
    unit_price = float(tool_input.get("price") or 0.0)
    found_shop_id = tool_input.get("shop_id")

    # 1. Nếu item_id là số → gọi trực tiếp GET /core/items/{id} để lấy giá chính xác
    if item_id_or_name.isdigit():
        item_detail = await core.get_item(item_id_or_name)
        if item_detail:
            if not item_name or item_name == item_id_or_name:
                item_name = item_detail["name"]
            # Luôn dùng giá từ API, bất kể AI đã truyền gì
            unit_price = item_detail["price"]
            if not found_shop_id:
                found_shop_id = item_detail["shop_id"]

    # 2. Nếu vẫn chưa có giá hoặc item_id là tên → search_items theo keyword
    if unit_price <= 0.0 or not item_id_or_name.isdigit():
        matched_items = await core.search_items(keyword=item_name if item_name != item_id_or_name else item_id_or_name)
        if matched_items:
            m = matched_items[0]
            if not item_name or item_name == item_id_or_name:
                item_name = m.get("name") or item_name
            if unit_price <= 0.0:
                unit_price = float(m.get("price") or 0.0)
            if not found_shop_id:
                found_shop_id = m.get("shop_id")
            if not item_id_or_name.isdigit():
                tool_input["item_id"] = m.get("item_id")
                item_id_or_name = str(m.get("item_id"))
        elif (state.cart.shop_id or found_shop_id) and unit_price <= 0.0:
            sid = found_shop_id or state.cart.shop_id
            menu = await core.get_menu(str(sid))
            for m_item in menu.get("items", []):
                if str(m_item.get("item_id")) == item_id_or_name or item_id_or_name.lower() in m_item.get("name", "").lower():
                    item_name = m_item.get("name") or item_name
                    unit_price = float(m_item.get("price") or 0.0)
                    tool_input["item_id"] = str(m_item.get("item_id"))
                    break

    if found_shop_id:
        state.cart.shop_id = str(found_shop_id)

    new_item = CartItem(
        item_id=str(tool_input["item_id"]),
        quantity=tool_input["quantity"],
        topping_ids=tool_input.get("topping_ids", []),
        note=tool_input.get("note", ""),
        unit_price=unit_price,
        item_name=item_name,
    )
    state.cart.items.append(new_item)
    state.order_confirmed = False
<<<<<<< HEAD
    state.cart_updated = True
    state.rotate_idempotency_key()

    # Đồng bộ trực tiếp vào order-service cart để web hiển thị ngay trong giỏ hàng
    try:
        from app.clients.order_service_client import OrderServiceClient
        order_client = OrderServiceClient(base_url=settings.order_service_url, jwt=state.user_jwt)
        shop_id_int = int(state.cart.shop_id) if state.cart.shop_id and str(state.cart.shop_id).isdigit() else 1
        await order_client.sync_cart_to_order_service(
            shop_id=shop_id_int,
            items=[{
                "item_id": new_item.item_id,
                "item_name": new_item.item_name,
                "unit_price": new_item.unit_price,
                "quantity": new_item.quantity,
                "note": new_item.note,
            }],
            user_id=state.user_id,
            guest_session_id=state.guest_session_id,
        )
    except Exception as e:
        import logging
        logging.getLogger(__name__).warning(f"[CartTools] Failed to sync item to order-service: {e}")

=======
    state.rotate_idempotency_key()

>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    return {
        "success": True,
        "cart_item_id": new_item.cart_item_id,
        "item_name": item_name,
        "unit_price": unit_price,
        "quantity": tool_input["quantity"],
        "subtotal": unit_price * tool_input["quantity"],
        "message": f"Đã thêm {item_name} x{tool_input['quantity']} ({unit_price:,.0f}đ/món) vào giỏ hàng",
        "cart_total_items": len(state.cart.items),
    }


async def handle_update_cart_item(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    cart_item_id = tool_input["cart_item_id"]
    item = next((i for i in state.cart.items if i.cart_item_id == cart_item_id), None)

    if item is None:
        return {"success": False, "error": f"Không tìm thấy dòng {cart_item_id} trong giỏ hàng"}

    new_qty = tool_input.get("quantity", item.quantity)
    if new_qty == 0:
        state.cart.items = [i for i in state.cart.items if i.cart_item_id != cart_item_id]
        msg = f"Đã xoá {item.item_name or item.item_id} khỏi giỏ"
    else:
        item.quantity = new_qty
        if "topping_ids" in tool_input:
            item.topping_ids = tool_input["topping_ids"]
        if "note" in tool_input:
            item.note = tool_input["note"]
        msg = f"Đã cập nhật {item.item_name or item.item_id}"

    state.order_confirmed = False
<<<<<<< HEAD
    state.cart_updated = True
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    state.rotate_idempotency_key()
    return {"success": True, "message": msg}


async def handle_remove_from_cart(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    cart_item_id = tool_input["cart_item_id"]
    before = len(state.cart.items)
    state.cart.items = [i for i in state.cart.items if i.cart_item_id != cart_item_id]
    removed = before - len(state.cart.items)

    if removed == 0:
        return {"success": False, "error": "Không tìm thấy món trong giỏ"}

    state.order_confirmed = False
<<<<<<< HEAD
    state.cart_updated = True
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    state.rotate_idempotency_key()
    return {"success": True, "message": "Đã xoá khỏi giỏ hàng"}


async def handle_view_cart(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    cart = state.cart
    return {
        "items": [
            {
                "cart_item_id": i.cart_item_id,
                "item_id": i.item_id,
                "item_name": i.item_name,
                "quantity": i.quantity,
                "unit_price": i.unit_price,
                "topping_ids": i.topping_ids,
                "note": i.note,
                "line_total": i.unit_price * i.quantity,
            }
            for i in cart.items
        ],
        "promo_code": cart.promo_code,
        "discount_amount": cart.discount_amount,
        "subtotal": cart.subtotal,
        "total": cart.total,
        "display_text": cart.to_display_text(),
    }


async def handle_apply_promotion(tool_input: Dict[str, Any], state: SessionState) -> Dict[str, Any]:
    core = CoreServiceClient(base_url=settings.core_service_url, jwt=state.user_jwt)
    promo_code = str(tool_input.get("promo_code") or "").strip()

    # Nếu không có mã hoặc người dùng yêu cầu tự động ("auto", "best", "voucher")
    if not promo_code or promo_code.lower() in ["auto", "best", "voucher", "khuyenmai", "giamgia"]:
        shop_id = state.cart.shop_id
        promos = await core.get_promotions(shop_id=shop_id)
        valid_candidates = []
        for p in promos:
            min_val = float(p.get("min_order_value") or 0)
            if state.cart.subtotal >= min_val:
                ptype = p.get("promo_type")
                disc_val = float(p.get("discount_value") or 0)
                max_disc = float(p.get("max_discount_amount") or 999999999)
                if ptype == "PERCENT":
                    est_disc = min(state.cart.subtotal * (disc_val / 100.0), max_disc)
                else:
                    est_disc = min(disc_val, state.cart.subtotal)
                valid_candidates.append((est_disc, p.get("code"), p.get("description")))
        if valid_candidates:
            valid_candidates.sort(key=lambda x: x[0], reverse=True)
            promo_code = str(valid_candidates[0][1])
        else:
            return {
                "success": False,
                "reason": f"Chưa có voucher phù hợp với giá trị giỏ hàng ({state.cart.subtotal:,.0f}đ). Bạn có thể xem danh sách voucher có sẵn.",
            }

    result = await core.apply_promotion(
        promo_code=promo_code,
        user_type=state.user_type,
        area_code=state.area_code,
        cart_subtotal=state.cart.subtotal,
        shop_id=state.cart.shop_id,
        user_id=state.user_id,
    )
    if result.get("valid"):
        state.cart.promo_code = promo_code
        state.cart.discount_amount = result["discount_amount"]
        return {
            "success": True,
            "promo_code": promo_code,
            "discount_amount": result["discount_amount"],
            "subtotal": state.cart.subtotal,
            "new_total": state.cart.total,
            "message": f"Áp mã {promo_code} thành công, giảm {result['discount_amount']:,.0f}đ! Tổng thanh toán còn: {state.cart.total:,.0f}đ",
        }
    return {
        "success": False,
        "reason": result.get("reason_if_invalid", "Mã không hợp lệ hoặc chưa đủ điều kiện"),
    }
