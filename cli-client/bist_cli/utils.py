"""
Utility functions for BIST CLI client.
Includes token storage, formatting, and helper functions.
"""

import json
from pathlib import Path
from typing import Any, Dict, Optional
from datetime import datetime

from rich.console import Console
from rich.table import Table
from rich import box

from .config import get_settings, get_app_dir


console = Console()


# ============================================================================
# Token Storage (Keyring or File-based)
# ============================================================================

def get_stored_token(token_name: str) -> Optional[str]:
    """
    Get stored token from secure storage.

    Args:
        token_name: Name of the token (e.g., 'access_token')

    Returns:
        Token value or None
    """
    settings = get_settings()

    if settings.use_keyring:
        try:
            import keyring
            return keyring.get_password("bist-cli", token_name)
        except Exception as e:
            console.print(f"[yellow]Keyring error, falling back to file: {e}[/yellow]")

    # Fall back to file-based storage
    token_file = get_app_dir() / "tokens.json"
    if token_file.exists():
        try:
            with open(token_file, "r") as f:
                tokens = json.load(f)
                return tokens.get(token_name)
        except Exception as e:
            console.print(f"[yellow]Error reading tokens: {e}[/yellow]")

    return None


def store_token(token_name: str, token_value: str) -> None:
    """
    Store token in secure storage.

    Args:
        token_name: Name of the token
        token_value: Token value
    """
    settings = get_settings()

    if settings.use_keyring:
        try:
            import keyring
            keyring.set_password("bist-cli", token_name, token_value)
            return
        except Exception as e:
            console.print(f"[yellow]Keyring error, falling back to file: {e}[/yellow]")

    # Fall back to file-based storage
    token_file = get_app_dir() / "tokens.json"
    tokens = {}

    if token_file.exists():
        try:
            with open(token_file, "r") as f:
                tokens = json.load(f)
        except Exception:
            pass

    tokens[token_name] = token_value

    try:
        with open(token_file, "w") as f:
            json.dump(tokens, f, indent=2)
        # Set restrictive permissions (owner only)
        token_file.chmod(0o600)
    except Exception as e:
        console.print(f"[red]Error storing token: {e}[/red]")


def clear_tokens() -> None:
    """Clear all stored tokens."""
    settings = get_settings()

    if settings.use_keyring:
        try:
            import keyring
            for token_name in ["access_token", "refresh_token", "algolab_token"]:
                try:
                    keyring.delete_password("bist-cli", token_name)
                except Exception:
                    pass
        except Exception:
            pass

    # Also clear file-based tokens
    token_file = get_app_dir() / "tokens.json"
    if token_file.exists():
        token_file.unlink()

    console.print("[green]Tokens cleared successfully[/green]")


# ============================================================================
# Formatting & Display Utilities
# ============================================================================

def format_currency(amount: float, currency: str = "TRY") -> str:
    """
    Format currency amount.

    Args:
        amount: Amount to format
        currency: Currency code

    Returns:
        Formatted string
    """
    if currency == "TRY":
        return f"₺{amount:,.2f}"
    elif currency == "USD":
        return f"${amount:,.2f}"
    elif currency == "EUR":
        return f"€{amount:,.2f}"
    else:
        return f"{amount:,.2f} {currency}"


def format_percentage(value: float, show_sign: bool = True) -> str:
    """
    Format percentage value.

    Args:
        value: Percentage value
        show_sign: Show + sign for positive values

    Returns:
        Formatted string with color
    """
    color = "green" if value >= 0 else "red"
    sign = "+" if value > 0 and show_sign else ""

    return f"[{color}]{sign}{value:.2f}%[/{color}]"


def format_timestamp(timestamp: Any) -> str:
    """Format timestamp for display."""
    if isinstance(timestamp, str):
        try:
            dt = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
            return dt.strftime("%d.%m.%Y %H:%M:%S")
        except Exception:
            return timestamp
    elif isinstance(timestamp, datetime):
        return timestamp.strftime("%d.%m.%Y %H:%M:%S")
    else:
        return str(timestamp)


def create_table(title: str, columns: list, rows: list) -> Table:
    """
    Create a rich table.

    Args:
        title: Table title
        columns: List of column names
        rows: List of row data (list of lists)

    Returns:
        Rich Table object
    """
    table = Table(title=title, box=box.ROUNDED, show_header=True, header_style="bold cyan")

    for col in columns:
        table.add_column(col)

    for row in rows:
        table.add_row(*[str(cell) for cell in row])

    return table


def print_success(message: str) -> None:
    """Print success message."""
    console.print(f"[green]✓[/green] {message}")


def print_error(message: str) -> None:
    """Print error message."""
    console.print(f"[red]✗[/red] {message}")


def print_warning(message: str) -> None:
    """Print warning message."""
    console.print(f"[yellow]⚠[/yellow] {message}")


def print_info(message: str) -> None:
    """Print info message."""
    console.print(f"[blue]ℹ[/blue] {message}")


# ============================================================================
# Data Validation
# ============================================================================

def validate_tc_kimlik(tc_no: str) -> bool:
    """
    Validate Turkish ID number (TC Kimlik No).

    Args:
        tc_no: TC Kimlik number

    Returns:
        True if valid
    """
    if not tc_no or len(tc_no) != 11:
        return False

    if not tc_no.isdigit():
        return False

    if tc_no[0] == "0":
        return False

    # Calculate checksum
    try:
        digits = [int(d) for d in tc_no]

        # 10th digit check
        sum_odd = sum(digits[0:9:2])
        sum_even = sum(digits[1:9:2])
        check_10 = (sum_odd * 7 - sum_even) % 10

        if check_10 != digits[9]:
            return False

        # 11th digit check
        sum_all = sum(digits[0:10])
        check_11 = sum_all % 10

        return check_11 == digits[10]

    except Exception:
        return False


def validate_phone_number(phone: str) -> bool:
    """
    Validate Turkish phone number.

    Args:
        phone: Phone number

    Returns:
        True if valid
    """
    # Remove spaces and dashes
    clean = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

    # Check format: +905XXXXXXXXX
    if clean.startswith("+90"):
        return len(clean) == 13 and clean[3] == "5" and clean[3:].isdigit()
    # Check format: 05XXXXXXXXX
    elif clean.startswith("0"):
        return len(clean) == 11 and clean[1] == "5" and clean.isdigit()
    # Check format: 5XXXXXXXXX
    elif clean.startswith("5"):
        return len(clean) == 10 and clean.isdigit()

    return False


def normalize_phone_number(phone: str) -> str:
    """
    Normalize phone number to +905XXXXXXXXX format.

    Args:
        phone: Phone number

    Returns:
        Normalized phone number
    """
    clean = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

    if clean.startswith("+90"):
        return clean
    elif clean.startswith("0"):
        return "+9" + clean
    elif clean.startswith("5"):
        return "+90" + clean

    return phone


# ============================================================================
# Session & Cache
# ============================================================================

def save_user_session(user_data: Dict[str, Any]) -> None:
    """Save user session data to cache."""
    session_file = get_app_dir() / "session.json"
    try:
        with open(session_file, "w") as f:
            json.dump(user_data, f, indent=2)
        session_file.chmod(0o600)
    except Exception as e:
        console.print(f"[yellow]Warning: Could not save session: {e}[/yellow]")


def load_user_session() -> Optional[Dict[str, Any]]:
    """Load user session data from cache."""
    session_file = get_app_dir() / "session.json"
    if session_file.exists():
        try:
            with open(session_file, "r") as f:
                return json.load(f)
        except Exception:
            pass
    return None


def clear_user_session() -> None:
    """Clear user session data."""
    session_file = get_app_dir() / "session.json"
    if session_file.exists():
        session_file.unlink()
