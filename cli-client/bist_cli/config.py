"""
Configuration management for BIST CLI client.
Handles environment variables, settings, and secure token storage.
"""

import os
from pathlib import Path
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # API Configuration
    api_base_url: str = Field(
        default="http://localhost:8080",
        description="Base URL for BIST Trading Platform API"
    )

    # Logging
    log_level: str = Field(default="INFO", description="Logging level")
    log_file: str = Field(default="bist_cli.log", description="Log file path")

    # Session Configuration
    session_timeout: int = Field(default=1800, description="Session timeout in seconds")
    auto_refresh_token: bool = Field(default=True, description="Auto-refresh JWT token")

    # Display Settings
    theme: str = Field(default="dark", description="CLI theme (dark/light)")
    show_timestamps: bool = Field(default=True, description="Show timestamps in output")
    pagination_size: int = Field(default=20, description="Number of items per page")

    # AlgoLab Settings
    algolab_auto_connect: bool = Field(
        default=False,
        description="Automatically connect to AlgoLab on startup"
    )

    # Token Storage
    use_keyring: bool = Field(
        default=True,
        description="Use system keyring for secure token storage"
    )

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )


# Singleton settings instance
_settings: Optional[Settings] = None


def get_settings() -> Settings:
    """Get or create settings singleton."""
    global _settings
    if _settings is None:
        # Look for .env file in cli-client directory
        env_file = Path(__file__).parent.parent / ".env"
        if env_file.exists():
            _settings = Settings(_env_file=str(env_file))
        else:
            _settings = Settings()
    return _settings


def get_app_dir() -> Path:
    """Get application data directory."""
    app_dir = Path.home() / ".bist-cli"
    app_dir.mkdir(exist_ok=True)
    return app_dir


def get_cache_dir() -> Path:
    """Get cache directory."""
    cache_dir = get_app_dir() / "cache"
    cache_dir.mkdir(exist_ok=True)
    return cache_dir


def get_config_file() -> Path:
    """Get user config file path."""
    return get_app_dir() / "config.json"
