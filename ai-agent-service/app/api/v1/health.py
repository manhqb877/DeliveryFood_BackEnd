from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/agent", tags=["health"])


@router.get("/health")
async def health():
    return {"status": "ok", "service": "ai-agent-service"}
