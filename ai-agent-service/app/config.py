from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # LLM
    anthropic_api_key: str = ""
    gemini_api_key: str = ""
    llm_provider: str = "auto"  # "auto" | "gemini" | "anthropic"
    llm_model: str = "gemini-flash-lite-latest"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 1
    session_ttl_seconds: int = 10800  # 3 giờ

    # Downstream Java services
    auth_service_url: str = "http://localhost:8081"
    core_service_url: str = "http://localhost:8082"
    order_service_url: str = "http://localhost:8083"
    payment_service_url: str = "http://localhost:8085"
    analytics_service_url: str = "http://localhost:8087"

    # App
    port: int = 8088
    app_name: str = "AI Ordering Agent"

    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    @property
    def effective_provider(self) -> str:
        if self.llm_provider in ("gemini", "anthropic"):
            return self.llm_provider
        if self.gemini_api_key:
            return "gemini"
        if self.anthropic_api_key:
            if self.anthropic_api_key.startswith("sk-ant"):
                return "anthropic"
            return "gemini"
        return "gemini"

    @property
    def effective_gemini_key(self) -> str:
        if self.gemini_api_key:
            return self.gemini_api_key
        return self.anthropic_api_key


settings = Settings()

