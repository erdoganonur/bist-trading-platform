"""
Professional logging system for BIST CLI.
Provides file-based logging with proper formatting and rotation.
"""

import logging
import sys
from pathlib import Path
from logging.handlers import RotatingFileHandler
from typing import Optional

from .config import get_settings, get_app_dir

settings = get_settings()


def setup_logger(
    name: str,
    log_file: Optional[str] = None,
    level: Optional[str] = None
) -> logging.Logger:
    """
    Configure logging with file and console handlers.

    Args:
        name: Logger name (usually __name__)
        log_file: Optional log file name (defaults to settings.log_file)
        level: Optional log level (defaults to settings.log_level)

    Returns:
        Configured logger instance
    """
    # Get or create logger
    logger = logging.getLogger(name)

    # Avoid adding handlers multiple times
    if logger.handlers:
        return logger

    # Set level from settings or parameter
    log_level_str = level or settings.log_level
    log_level = getattr(logging, log_level_str.upper(), logging.INFO)
    logger.setLevel(log_level)

    # File handler (detailed logs with rotation)
    log_filename = log_file or settings.log_file
    log_path = get_app_dir() / log_filename

    file_handler = RotatingFileHandler(
        log_path,
        maxBytes=10 * 1024 * 1024,  # 10 MB
        backupCount=5,
        encoding='utf-8'
    )
    file_handler.setLevel(logging.DEBUG)

    file_formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    file_handler.setFormatter(file_formatter)

    # Console handler (warnings and errors only - not to interfere with Rich UI)
    console_handler = logging.StreamHandler(sys.stderr)
    console_handler.setLevel(logging.WARNING)

    console_formatter = logging.Formatter(
        '%(levelname)s: %(message)s'
    )
    console_handler.setFormatter(console_formatter)

    # Add handlers
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    return logger


def get_logger(name: str) -> logging.Logger:
    """
    Get or create a logger for the given name.

    Args:
        name: Logger name (usually __name__)

    Returns:
        Logger instance
    """
    logger = logging.getLogger(name)

    # If logger doesn't have handlers yet, set it up
    if not logger.handlers:
        return setup_logger(name)

    return logger


# Convenience function for logging API calls
def log_api_call(
    logger: logging.Logger,
    method: str,
    url: str,
    status_code: Optional[int] = None,
    duration_ms: Optional[float] = None,
    error: Optional[str] = None
) -> None:
    """
    Log API call with structured information.

    Args:
        logger: Logger instance
        method: HTTP method (GET, POST, etc.)
        url: Request URL
        status_code: Response status code
        duration_ms: Request duration in milliseconds
        error: Error message if request failed
    """
    if error:
        logger.error(
            f"API call failed: {method} {url} - Error: {error}",
            extra={
                'method': method,
                'url': url,
                'error': error
            }
        )
    elif status_code:
        log_level = logging.INFO if 200 <= status_code < 300 else logging.WARNING

        msg = f"API call: {method} {url} - Status: {status_code}"
        if duration_ms is not None:
            msg += f" - Duration: {duration_ms:.2f}ms"

        logger.log(
            log_level,
            msg,
            extra={
                'method': method,
                'url': url,
                'status_code': status_code,
                'duration_ms': duration_ms
            }
        )
    else:
        logger.debug(
            f"API call initiated: {method} {url}",
            extra={
                'method': method,
                'url': url
            }
        )


# Convenience function for logging WebSocket events
def log_websocket_event(
    logger: logging.Logger,
    event_type: str,
    details: Optional[dict] = None,
    error: Optional[str] = None
) -> None:
    """
    Log WebSocket event with structured information.

    Args:
        logger: Logger instance
        event_type: Type of WebSocket event (connect, disconnect, message, etc.)
        details: Optional event details
        error: Error message if event failed
    """
    if error:
        logger.error(
            f"WebSocket {event_type} failed: {error}",
            extra={
                'event_type': event_type,
                'error': error,
                **(details or {})
            }
        )
    else:
        logger.info(
            f"WebSocket {event_type}",
            extra={
                'event_type': event_type,
                **(details or {})
            }
        )


# Get the log file location
def get_log_file_path() -> Path:
    """Get the current log file path."""
    return get_app_dir() / settings.log_file


# Function to view recent logs
def view_recent_logs(lines: int = 50) -> list[str]:
    """
    Read and return recent log lines.

    Args:
        lines: Number of recent lines to return

    Returns:
        List of log lines
    """
    log_path = get_log_file_path()

    if not log_path.exists():
        return []

    try:
        with open(log_path, 'r', encoding='utf-8') as f:
            all_lines = f.readlines()
            return all_lines[-lines:]
    except Exception as e:
        return [f"Error reading log file: {str(e)}"]


# Function to clear logs
def clear_logs() -> bool:
    """
    Clear the log file.

    Returns:
        True if successful, False otherwise
    """
    log_path = get_log_file_path()

    try:
        if log_path.exists():
            log_path.unlink()
        return True
    except Exception:
        return False
