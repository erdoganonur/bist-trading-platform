"""
Debug utilities for BIST CLI client.
Provides conditional debug logging based on configuration.
"""

from typing import Any, Optional
from rich.console import Console
from rich.panel import Panel
from rich.syntax import Syntax
from rich.table import Table
from rich import box
import json

from .config import get_settings

console = Console()
settings = get_settings()


def is_debug_enabled() -> bool:
    """Check if debug mode is enabled."""
    return settings.debug_mode


def is_api_debug_enabled() -> bool:
    """Check if API debug logging is enabled."""
    return settings.debug_api_calls


def is_websocket_debug_enabled() -> bool:
    """Check if WebSocket debug logging is enabled."""
    return settings.debug_websocket


def debug_print(message: str, title: Optional[str] = None, style: str = "dim") -> None:
    """Print debug message if debug mode is enabled."""
    if is_debug_enabled():
        if title:
            console.print(f"\n[bold {style}]{title}[/bold {style}]")
        console.print(f"[{style}]🐛 DEBUG: {message}[/{style}]")


def debug_api_call(method: str, url: str, data: Optional[dict] = None, response: Optional[dict] = None) -> None:
    """Log API call details if API debug is enabled."""
    if not is_api_debug_enabled():
        return

    table = Table(title=f"API Call: {method} {url}", box=box.ROUNDED, border_style="blue")
    table.add_column("Property", style="cyan")
    table.add_column("Value", style="yellow")

    table.add_row("Method", method)
    table.add_row("URL", url)

    if data:
        data_json = json.dumps(data, indent=2)
        table.add_row("Request Data", data_json[:200] + "..." if len(data_json) > 200 else data_json)

    if response:
        status = response.get("status", "N/A")
        table.add_row("Response Status", str(status))

        # Show response preview
        if "data" in response or "content" in response:
            resp_data = response.get("data") or response.get("content")
            resp_json = json.dumps(resp_data, indent=2) if resp_data else "null"
            preview = resp_json[:300] + "..." if len(resp_json) > 300 else resp_json
            table.add_row("Response Preview", preview)

    console.print(table)


def debug_websocket_message(message_type: str, symbol: str, data: dict) -> None:
    """Log WebSocket message details if WebSocket debug is enabled."""
    if not is_websocket_debug_enabled():
        return

    console.print(f"\n[bold blue]WebSocket Message: {message_type}[/bold blue]")
    console.print(f"[dim]Symbol: {symbol}[/dim]")

    # Format data as JSON
    try:
        json_str = json.dumps(data, indent=2)
        syntax = Syntax(json_str, "json", theme="monokai", line_numbers=False)
        console.print(Panel(syntax, title="Message Data", border_style="blue"))
    except Exception as e:
        console.print(f"[dim]Data: {data}[/dim]")
        console.print(f"[red]Failed to format as JSON: {e}[/red]")


def debug_object(obj: Any, title: str = "Object", max_depth: int = 3) -> None:
    """Debug print any Python object with pretty formatting."""
    if not is_debug_enabled():
        return

    console.print(f"\n[bold blue]🔍 DEBUG: {title}[/bold blue]")

    try:
        # Try to serialize as JSON
        if hasattr(obj, "__dict__"):
            obj_dict = obj.__dict__
        elif isinstance(obj, (dict, list)):
            obj_dict = obj
        else:
            obj_dict = {"value": str(obj), "type": type(obj).__name__}

        json_str = json.dumps(obj_dict, indent=2, default=str)
        syntax = Syntax(json_str, "json", theme="monokai", line_numbers=True)
        console.print(Panel(syntax, border_style="blue"))
    except Exception as e:
        console.print(f"[dim]{obj}[/dim]")
        console.print(f"[red]Failed to serialize: {e}[/red]")


def debug_table(data: dict, title: str = "Debug Data") -> None:
    """Display debug data in a table format."""
    if not is_debug_enabled():
        return

    table = Table(title=title, box=box.SIMPLE, border_style="dim")
    table.add_column("Key", style="cyan")
    table.add_column("Value", style="yellow")
    table.add_column("Type", style="dim")

    for key, value in data.items():
        value_str = str(value)
        if len(value_str) > 100:
            value_str = value_str[:97] + "..."

        table.add_row(key, value_str, type(value).__name__)

    console.print(table)


def debug_exception(exc: Exception, context: str = "") -> None:
    """Pretty print exception details for debugging."""
    if not is_debug_enabled():
        return

    import traceback

    console.print(f"\n[bold red]🐛 EXCEPTION: {context}[/bold red]")
    console.print(f"[red]Type: {type(exc).__name__}[/red]")
    console.print(f"[red]Message: {str(exc)}[/red]")

    # Show traceback
    tb_str = "".join(traceback.format_tb(exc.__traceback__))
    syntax = Syntax(tb_str, "python", theme="monokai", line_numbers=False)
    console.print(Panel(syntax, title="Traceback", border_style="red"))


def toggle_debug_mode() -> bool:
    """Toggle debug mode on/off."""
    settings.debug_mode = not settings.debug_mode
    status = "ENABLED" if settings.debug_mode else "DISABLED"
    console.print(f"\n[bold yellow]Debug Mode: {status}[/bold yellow]")
    return settings.debug_mode


def get_debug_info() -> dict:
    """Get current debug configuration."""
    return {
        "debug_mode": settings.debug_mode,
        "debug_api_calls": settings.debug_api_calls,
        "debug_websocket": settings.debug_websocket,
        "log_level": settings.log_level,
        "log_file": settings.log_file
    }


def print_debug_status() -> None:
    """Print current debug status."""
    info = get_debug_info()

    table = Table(title="Debug Configuration", box=box.ROUNDED, border_style="yellow")
    table.add_column("Setting", style="cyan")
    table.add_column("Status", style="yellow")

    for key, value in info.items():
        status = "✓ ON" if value is True else ("✗ OFF" if value is False else str(value))
        color = "green" if value is True else ("red" if value is False else "yellow")
        table.add_row(key.replace("_", " ").title(), f"[{color}]{status}[/{color}]")

    console.print()
    console.print(table)
    console.print()
