"""
Redis session store — lưu SessionState theo session_id.
Serialize/deserialize bằng Pydantic JSON.
"""

import json
from typing import Optional

import redis.asyncio as aioredis

from app.config import settings
from app.session.models import SessionState


def _get_client() -> aioredis.Redis:
    return aioredis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        db=settings.redis_db,
        decode_responses=True,
    )


def _key(session_id: str) -> str:
    return f"ai_agent:session:{session_id}"


async def load_session(session_id: str) -> Optional[SessionState]:
    client = _get_client()
    try:
        raw = await client.get(_key(session_id))
        if raw is None:
            return None
        return SessionState.model_validate_json(raw)
    finally:
        await client.aclose()


async def save_session(state: SessionState) -> None:
    client = _get_client()
    try:
        await client.setex(
            _key(state.session_id),
            settings.session_ttl_seconds,
            state.model_dump_json(),
        )
    finally:
        await client.aclose()


async def delete_session(session_id: str) -> None:
    client = _get_client()
    try:
        await client.delete(_key(session_id))
    finally:
        await client.aclose()


async def get_or_create_session(
    session_id: str,
    area_code: str = "UNKNOWN",
    user_type: str = "guest",
    user_jwt: Optional[str] = None,
    user_id: Optional[int] = None,
    guest_session_id: Optional[int] = None,
) -> SessionState:
    """Load session từ Redis; nếu không có thì tạo mới."""
    state = await load_session(session_id)
    if state is None:
        state = SessionState(
            session_id=session_id,
            area_code=area_code,
            user_type=user_type,
            user_jwt=user_jwt,
            user_id=user_id,
            guest_session_id=guest_session_id,
        )
        await save_session(state)
    else:
        # Cập nhật JWT và IDs mỗi lượt
        if user_jwt:
            state.user_jwt = user_jwt
        if user_id:
            state.user_id = user_id
            state.user_type = "customer"
        if guest_session_id:
            state.guest_session_id = guest_session_id
    return state
