"""
Pydantic models cho SessionState, CartItem, AgentTurn.
"""

from __future__ import annotations

import uuid
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class CartItem(BaseModel):
    cart_item_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    item_id: str
    quantity: int = Field(ge=1)
    topping_ids: List[str] = Field(default_factory=list)
    note: str = ""
    unit_price: float = 0.0  # điền sau khi resolve từ Core Service
    item_name: str = ""       # tên hiển thị, cache từ get_menu


class CartSnapshot(BaseModel):
    shop_id: Optional[str] = None
    items: List[CartItem] = Field(default_factory=list)
    promo_code: Optional[str] = None
    discount_amount: float = 0.0

    @property
    def subtotal(self) -> float:
        return sum(i.unit_price * i.quantity for i in self.items)

    @property
    def total(self) -> float:
        return max(self.subtotal - self.discount_amount, 0.0)

    def to_display_text(self) -> str:
        if not self.items:
            return "Giỏ hàng trống"
        lines = []
        for item in self.items:
            toppings = f" + {', '.join(item.topping_ids)}" if item.topping_ids else ""
            note = f" ({item.note})" if item.note else ""
            lines.append(
                f"- {item.item_name or item.item_id} x{item.quantity}{toppings}{note}"
                f" — {item.unit_price:,.0f}đ/món"
            )
        if self.promo_code:
            lines.append(f"Mã KM: {self.promo_code} (-{self.discount_amount:,.0f}đ)")
        lines.append(f"**Tổng: {self.total:,.0f}đ**")
        return "\n".join(lines)


class AgentTurn(BaseModel):
    role: str  # "user" | "assistant"
    content: Any  # str hoặc list[block] theo Anthropic API


class SessionState(BaseModel):
    session_id: str
    area_code: str = "UNKNOWN"
    user_type: str = "guest"           # "guest" | "customer"
    user_id: Optional[int] = None
<<<<<<< HEAD
    guest_session_id: Optional[int] = None
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
    user_jwt: Optional[str] = None     # forward tới downstream services
    cart: CartSnapshot = Field(default_factory=CartSnapshot)
    conversation_history: List[AgentTurn] = Field(default_factory=list)
    idempotency_key: str = Field(default_factory=lambda: str(uuid.uuid4()))
    order_confirmed: bool = False      # cờ guardrail: người dùng đã xác nhận chưa
    last_order_id: Optional[str] = None
<<<<<<< HEAD
    last_order_info: Optional[Dict[str, Any]] = None
    last_payment_qr: Optional[Dict[str, Any]] = None
    cart_updated: bool = False
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8

    def rotate_idempotency_key(self) -> None:
        """Sinh key mới — gọi khi giỏ hàng thay đổi sau khi đã confirm."""
        self.idempotency_key = str(uuid.uuid4())
        self.order_confirmed = False
