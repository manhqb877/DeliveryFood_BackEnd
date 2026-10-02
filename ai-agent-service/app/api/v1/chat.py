"""
API endpoints:
- POST /api/v1/agent/chat   — endpoint chính
- GET  /api/v1/agent/health — health check
"""

<<<<<<< HEAD
import json
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel
from typing import Any, Dict, Optional

from app.agent.orchestrator import run_agent_turn
from app.session.redis_session_store import get_or_create_session, save_session
=======
from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel
from typing import Optional

from app.agent.orchestrator import run_agent_turn
from app.session.redis_session_store import get_or_create_session
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8

router = APIRouter(prefix="/api/v1/agent", tags=["agent"])


class ChatRequest(BaseModel):
    session_id: str
    message: str
    area_code: str = "UNKNOWN"
    user_type: str = "guest"  # "guest" | "customer"
<<<<<<< HEAD
    user_id: Optional[int] = None
    guest_session_id: Optional[int] = None
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8


class ChatResponse(BaseModel):
    session_id: str
    reply: str
<<<<<<< HEAD
    cart_updated: bool = False
    order_info: Optional[Dict[str, Any]] = None
    payment_qr: Optional[Dict[str, Any]] = None
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8


@router.post("/chat", response_model=ChatResponse)
async def chat(
    req: ChatRequest,
    authorization: Optional[str] = Header(default=None),
):
    """
    Endpoint chính cho AI Ordering Agent.
    - JWT người dùng được forward từ header Authorization.
    - session_id do client tạo và quản lý (UUID).
<<<<<<< HEAD
    - Hỗ trợ đồng bộ cart, user_id, guest_session_id, trả về thông tin đơn hàng và mã thanh toán QR.
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    """
    jwt_token: Optional[str] = None
    if authorization and authorization.startswith("Bearer "):
        jwt_token = authorization[len("Bearer "):]

    # Load hoặc tạo session
    state = await get_or_create_session(
        session_id=req.session_id,
        area_code=req.area_code,
        user_type=req.user_type,
        user_jwt=jwt_token,
<<<<<<< HEAD
        user_id=req.user_id,
        guest_session_id=req.guest_session_id,
    )

    if req.user_id:
        state.user_id = req.user_id
        state.user_type = "customer"
    if req.guest_session_id:
        state.guest_session_id = req.guest_session_id

=======
    )

>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    # Nếu có jwt_token và chưa có user_id trong session, tra cứu profile người dùng
    if jwt_token and not state.user_id:
        try:
            from app.clients.auth_service_client import AuthServiceClient
            from app.config import settings
            auth_client = AuthServiceClient(base_url=settings.auth_service_url, jwt=jwt_token)
            profile = await auth_client.get_user_profile()
            if profile and profile.get("id"):
                state.user_id = profile["id"]
                state.user_type = "customer"
        except Exception:
            pass

    # Chạy agent turn
    reply = await run_agent_turn(user_message=req.message, state=state)

<<<<<<< HEAD
    cart_was_updated = state.cart_updated
    order_info = state.last_order_info
    payment_qr = state.last_payment_qr

    # Nếu có đơn hàng vừa tạo, nhúng metadata JSON dạng comment để client luôn render được thẻ đơn hàng
    if order_info and "<!-- ORDER_DATA:" not in reply:
        reply += f"\n\n<!-- ORDER_DATA: {json.dumps(order_info, ensure_ascii=False)} -->"

    # Reset cờ cập nhật giỏ hàng sau khi đã chuẩn bị response
    state.cart_updated = False
    await save_session(state)

    return ChatResponse(
        session_id=req.session_id,
        reply=reply,
        cart_updated=cart_was_updated,
        order_info=order_info,
        payment_qr=payment_qr,
    )
=======
    return ChatResponse(session_id=req.session_id, reply=reply)
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
