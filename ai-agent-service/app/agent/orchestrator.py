"""
Orchestrator — Vòng lặp chính xử lý hội thoại người dùng:
Hỗ trợ cả 2 Provider:
1. Google Gemini (thông qua SDK google-genai) — Auto-detected khi dùng Gemini API Key.
2. Anthropic Claude (thông qua SDK anthropic) — Dùng khi cung cấp Anthropic Key (sk-ant-...).
"""

import asyncio
import json
import logging
from typing import Any, Dict, List, Optional

import anthropic
from google import genai
from google.genai import types

from app.agent.guardrails import (
    GuardrailViolation,
    detect_user_confirmation,
    inject_system_fields,
    validate_tool_call,
)
from app.agent.system_prompt import build_system_prompt
from app.agent.tools import TOOL_REGISTRY, TOOL_SCHEMAS
from app.agent.tools.cart_tools import (
    handle_add_to_cart,
    handle_apply_promotion,
    handle_get_promotions,
    handle_remove_from_cart,
    handle_update_cart_item,
    handle_view_cart,
)
from app.agent.tools.order_tools import handle_create_order, handle_get_order_status
from app.agent.tools.payment_tools import handle_initiate_payment
from app.agent.tools.search_tools import handle_get_menu, handle_search_shops, handle_search_items
from app.agent.tools.user_tools import handle_get_user_addresses
from app.config import settings
from app.session.models import AgentTurn, SessionState
from app.session.redis_session_store import save_session

logger = logging.getLogger(__name__)

MAX_ITERATIONS = 10  # tránh vòng lặp vô tận


def _make_gemini_tools(state: SessionState):
    """
    Tạo danh sách các hàm Python bất đồng bộ cho Gemini AFC (Automatic Function Calling).
    Mỗi hàm đều áp dụng Guardrails và System Field Injection tương ứng.
    """
    async def search_shops(keyword: str = "", area_code: str = "") -> str:
        """Tìm kiếm quán ăn hoặc xem danh sách tất cả các quán ăn trong hệ thống."""
        try:
            inp = {"keyword": keyword, "area_code": area_code or state.area_code}
            validate_tool_call("search_shops", inp, state)
            inp = inject_system_fields("search_shops", inp, state)
            res = await handle_search_shops(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def search_items(keyword: str = "", max_price: Optional[float] = None) -> str:
        """Tìm kiếm các món ăn cụ thể theo tên hoặc tìm các món ăn ngon giá rẻ."""
        try:
            inp = {"keyword": keyword, "max_price": max_price}
            res = await handle_search_items(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except Exception as e:
            return f"[LỖI]: {e}"

    async def get_menu(shop_id: str) -> str:
        """Lấy menu chi tiết của một quán ăn theo shop_id."""
        try:
            inp = {"shop_id": shop_id}
            validate_tool_call("get_menu", inp, state)
            inp = inject_system_fields("get_menu", inp, state)
            res = await handle_get_menu(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def add_to_cart(item_id: str, quantity: int = 1, note: str = "") -> str:
        """Thêm món vào giỏ hàng với số lượng và ghi chú."""
        try:
            inp = {"item_id": item_id, "quantity": quantity, "topping_ids": [], "note": note}
            validate_tool_call("add_to_cart", inp, state)
            inp = inject_system_fields("add_to_cart", inp, state)
            res = await handle_add_to_cart(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def update_cart_item(cart_item_id: str, quantity: int, note: str = "") -> str:
        """Sửa số lượng hoặc ghi chú của món trong giỏ hàng (quantity=0 để xóa)."""
        try:
            inp = {"cart_item_id": cart_item_id, "quantity": quantity, "note": note}
            validate_tool_call("update_cart_item", inp, state)
            inp = inject_system_fields("update_cart_item", inp, state)
            res = await handle_update_cart_item(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def remove_from_cart(cart_item_id: str) -> str:
        """Xóa một món khỏi giỏ hàng theo cart_item_id."""
        try:
            inp = {"cart_item_id": cart_item_id}
            validate_tool_call("remove_from_cart", inp, state)
            inp = inject_system_fields("remove_from_cart", inp, state)
            res = await handle_remove_from_cart(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def view_cart() -> str:
        """Xem chi tiết giỏ hàng hiện tại, danh sách các món và tổng tiền."""
        try:
            res = await handle_view_cart({}, state)
            return json.dumps(res, ensure_ascii=False)
        except Exception as e:
            return f"[LỖI]: {e}"

    async def apply_promotion(promotion_code: str = "auto") -> str:
        """Áp dụng mã giảm giá cho giỏ hàng hiện tại. Truyền mã hoặc 'auto'/'best' để tự động chọn voucher tốt nhất."""
        try:
            inp = {"promo_code": promotion_code or "auto"}  # key phải khớp với handle_apply_promotion
            validate_tool_call("apply_promotion", inp, state)
            inp = inject_system_fields("apply_promotion", inp, state)
            res = await handle_apply_promotion(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def get_promotions(shop_id: str = "") -> str:
        """Lấy danh sách mã giảm giá, voucher khuyến mãi có sẵn của quán và toàn sàn."""
        try:
            sid = shop_id or state.cart.shop_id or ""
            res = await handle_get_promotions({"shop_id": sid}, state)
            return json.dumps(res, ensure_ascii=False)
        except Exception as e:
            return f"[LỖI]: {e}"

    async def get_user_addresses() -> str:
        """Lấy danh sách địa chỉ giao hàng đã lưu trong tài khoản người dùng. Gọi trước khi đặt đơn để xác nhận địa chỉ."""
        try:
            res = await handle_get_user_addresses({}, state)
            return json.dumps(res, ensure_ascii=False)
        except Exception as e:
            return f"[LỖI]: {e}"

    async def create_order(delivery_address: str = "", payment_method: str = "COD", note: str = "") -> str:
        """Tạo đơn hàng từ giỏ hàng. Chỉ gọi khi người dùng đã xác nhận đồng ý đặt."""
        try:
            if not delivery_address:
                addr_res = await handle_get_user_addresses({}, state)
                addrs = addr_res.get("addresses", [])
                if addrs:
                    default_addr = next((a["address"] for a in addrs if a.get("is_default")), addrs[0]["address"])
                    delivery_address = default_addr
                else:
                    delivery_address = "Địa chỉ nhận hàng (chưa có sẵn)"

            inp = {"delivery_address": delivery_address, "payment_method": payment_method, "note": note}
            validate_tool_call("create_order", inp, state)
            inp = inject_system_fields("create_order", inp, state)
            res = await handle_create_order(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def get_order_status(order_id: str) -> str:
        """Tra cứu trạng thái của đơn hàng đã đặt theo order_id."""
        try:
            inp = {"order_id": order_id}
            validate_tool_call("get_order_status", inp, state)
            inp = inject_system_fields("get_order_status", inp, state)
            res = await handle_get_order_status(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    async def initiate_payment(order_id: str, payment_method: str) -> str:
        """Khởi tạo thanh toán online (VNPay, MoMo, ZaloPay)."""
        try:
            inp = {"order_id": order_id, "payment_method": payment_method}
            validate_tool_call("initiate_payment", inp, state)
            inp = inject_system_fields("initiate_payment", inp, state)
            res = await handle_initiate_payment(inp, state)
            return json.dumps(res, ensure_ascii=False)
        except GuardrailViolation as e:
            return f"[BẢO VỆ]: {e}"
        except Exception as e:
            return f"[LỖI]: {e}"

    return [
        search_shops,
        search_items,
        get_menu,
        add_to_cart,
        update_cart_item,
        remove_from_cart,
        view_cart,
        get_promotions,
        get_user_addresses,
        apply_promotion,
        create_order,
        get_order_status,
        initiate_payment,
    ]


async def _run_gemini_turn(user_message: str, state: SessionState, system_prompt: str) -> str:
    """Xử lý hội thoại qua Google Gemini API."""
    gemini_key = settings.effective_gemini_key
    if not gemini_key:
        return (
            "⚠️ **Chưa cấu hình API Key:** Vui lòng cung cấp `GEMINI_API_KEY` hoặc `ANTHROPIC_API_KEY` trong file `.env`."
        )

    client = genai.Client(api_key=gemini_key)
    tools = _make_gemini_tools(state)

    # Giới hạn lịch sử gửi cho LLM (chỉ lấy 4 lượt gần nhất để tiết kiệm token tối đa)
    history = []
    recent_history = state.conversation_history[-5:-1] if len(state.conversation_history) > 5 else state.conversation_history[:-1]
    for turn in recent_history:
        if isinstance(turn.content, str) and turn.content.strip():
            role = "user" if turn.role == "user" else "model"
            history.append(types.Content(role=role, parts=[types.Part.from_text(text=turn.content[:1000])]))

    # Danh sách model luân chuyển tự động khi gặp 429 / Quota / Rate-limit (chỉ giữ các model đang hoạt động)
    candidate_models = [
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-2.5-flash-lite",
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
    ]
    last_err = None

    for model_name in candidate_models:
        try:
            logger.info(f"[Orchestrator Gemini] Đang gọi model: {model_name}")
            chat = client.aio.chats.create(
                model=model_name,
                history=history,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    tools=tools,
                    max_output_tokens=1024,
                ),
            )
            response = await chat.send_message(user_message)
            final_text = response.text or ""
            state.conversation_history.append(AgentTurn(role="assistant", content=final_text))
            logger.info(f"[Orchestrator Gemini] Thành công với model: {model_name}")
            return final_text
        except Exception as e:
            logger.warning(f"[Orchestrator Gemini] Lỗi với model {model_name}: {e}. Đang tự động chuyển model tiếp theo...")
            last_err = e
            err_str = str(e)
            if "401" in err_str or "API_KEY_INVALID" in err_str:
                return "⚠️ **Lỗi xác thực:** Gemini API Key không hợp lệ. Vui lòng kiểm tra lại key."
            # Với 429, RESOURCE_EXHAUSTED, 503 hoặc model not found: tự động thử model tiếp theo
            continue

    return f"⚠️ Hệ thống AI hiện đang bận hoặc quá tải quota ({str(last_err)[:120]}). Vui lòng thử lại sau giây lát."


async def _run_anthropic_turn(user_message: str, state: SessionState, system_prompt: str) -> str:
    """Xử lý hội thoại qua Anthropic Claude API."""
    messages = [
        {"role": turn.role, "content": turn.content}
        for turn in state.conversation_history
    ]

    client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key)
    iteration = 0
    final_text = ""

    while iteration < MAX_ITERATIONS:
        iteration += 1
        logger.info(f"[Orchestrator Anthropic] Iteration {iteration}...")

        try:
            response = await client.messages.create(
                model=settings.llm_model,
                max_tokens=4096,
                system=system_prompt,
                tools=TOOL_SCHEMAS,
                messages=messages,
            )
        except anthropic.AuthenticationError:
            logger.error("[Orchestrator] Anthropic API key không hợp lệ")
            return (
                "⚠️ **Lỗi cấu hình:** API key Anthropic không hợp lệ.\n"
                "Key hợp lệ bắt đầu bằng `sk-ant-`."
            )
        except anthropic.APIConnectionError as e:
            logger.error(f"[Orchestrator] Không kết nối được Anthropic: {e}")
            return "⚠️ Không thể kết nối tới Anthropic API. Vui lòng thử lại sau."
        except Exception as e:
            logger.error(f"[Orchestrator] LLM error: {e}")
            return f"⚠️ Lỗi AI: {str(e)[:200]}"

        if response.stop_reason == "end_turn":
            text_blocks = [b.text for b in response.content if hasattr(b, "text")]
            final_text = "\n".join(text_blocks)
            state.conversation_history.append(
                AgentTurn(role="assistant", content=response.content)
            )
            break

        if response.stop_reason == "tool_use":
            state.conversation_history.append(
                AgentTurn(role="assistant", content=response.content)
            )
            messages.append({"role": "assistant", "content": response.content})

            tool_result_contents = []
            for block in response.content:
                if block.type != "tool_use":
                    continue

                tool_name = block.name
                tool_input = dict(block.input)
                tool_use_id = block.id

                try:
                    validate_tool_call(tool_name, tool_input, state)
                    tool_input = inject_system_fields(tool_name, tool_input, state)
                    handler = TOOL_REGISTRY.get(tool_name)
                    if handler is None:
                        raise ValueError(f"Tool '{tool_name}' chưa được đăng ký")

                    tool_result = await handler(tool_input, state)
                    tool_result_contents.append({
                        "type": "tool_result",
                        "tool_use_id": tool_use_id,
                        "content": str(tool_result),
                    })
                except GuardrailViolation as e:
                    tool_result_contents.append({
                        "type": "tool_result",
                        "tool_use_id": tool_use_id,
                        "is_error": True,
                        "content": f"[GUARDRAIL] {e}",
                    })
                except Exception as e:
                    tool_result_contents.append({
                        "type": "tool_result",
                        "tool_use_id": tool_use_id,
                        "is_error": True,
                        "content": f"Lỗi thực thi tool: {str(e)}",
                    })

            messages.append({"role": "user", "content": tool_result_contents})
            state.conversation_history.append(
                AgentTurn(role="user", content=tool_result_contents)
            )
            continue

        final_text = "Xin lỗi, đã xảy ra lỗi xử lý. Vui lòng thử lại."
        break

    return final_text


async def run_agent_turn(
    user_message: str,
    state: SessionState,
) -> str:
    """
    Xử lý một lượt người dùng gửi tin nhắn.
    Tự động chọn Provider (Gemini hoặc Anthropic) dựa trên cấu hình và API key.
    state được cập nhật in-place và persist về Redis.
    """
    # 1. Detect xác nhận từ lượt người dùng này
    if detect_user_confirmation(user_message):
        state.order_confirmed = True
    else:
        if not state.last_order_id:
            state.order_confirmed = False

    # 2. Append user turn vào history
    state.conversation_history.append(AgentTurn(role="user", content=user_message))

    # 3. Build system prompt
    system_prompt = build_system_prompt(
        session_id=state.session_id,
        area_code=state.area_code,
        user_type=state.user_type,
        cart_snapshot=state.cart.to_display_text(),
    )

    # 4. Phân luồng theo Provider
    provider = settings.effective_provider
    logger.info(f"[Orchestrator] Sử dụng LLM Provider: {provider}")

    if provider == "gemini":
        final_text = await _run_gemini_turn(user_message, state, system_prompt)
    else:
        final_text = await _run_anthropic_turn(user_message, state, system_prompt)

    # 5. Lưu phiên vào Redis
    await save_session(state)

    return final_text
