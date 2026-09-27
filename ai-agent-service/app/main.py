"""
FastAPI entrypoint cho ai-agent-service.
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.chat import router as chat_router
from app.api.v1.health import router as health_router
from app.config import settings

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="AI Ordering Agent Service",
    description="Hyperlocal Food Delivery — AI Agent (Python/FastAPI)",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router)
app.include_router(health_router)


@app.on_event("startup")
async def startup():
    logging.info(f"AI Agent Service starting on port {settings.port}")
    logging.info(f"LLM model: {settings.llm_model}")
    logging.info(f"Redis: {settings.redis_host}:{settings.redis_port}/{settings.redis_db}")
