#!/usr/bin/env python3
"""
BIST Trading Platform - CLI Client
Main entry point for the interactive command-line interface.
"""

import sys
from pathlib import Path

import typer
from rich.console import Console

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from bist_cli.menu import MainMenu
from bist_cli.config import get_settings
from bist_cli.utils import clear_tokens, print_error, print_success


app = typer.Typer(
    name="bist-cli",
    help="BIST Trading Platform Interactive CLI Client",
    add_completion=False
)
console = Console()


@app.command()
def main(
    test_connection: bool = typer.Option(
        False,
        "--test-connection",
        "-t",
        help="Test API connection and exit"
    ),
    clear_cache: bool = typer.Option(
        False,
        "--clear-tokens",
        "-c",
        help="Clear stored tokens and exit"
    ),
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Enable verbose output"
    ),
    debug: bool = typer.Option(
        False,
        "--debug",
        "-d",
        help="Enable debug mode (show HTTP requests/responses)"
    ),
    version: bool = typer.Option(
        False,
        "--version",
        help="Show version and exit"
    )
) -> None:
    """
    BIST Trading Platform - Interactive CLI Client

    Borsa İstanbul için interaktif komut satırı arayüzü.
    Kullanıcı girişi, AlgoLab entegrasyonu ve piyasa verilerine erişim sağlar.
    """
    # Handle version flag
    if version:
        from bist_cli import __version__
        console.print(f"BIST CLI Client v{__version__}")
        return

    # Handle clear tokens flag
    if clear_cache:
        console.print("\n[yellow]Token'lar temizleniyor...[/yellow]")
        clear_tokens()
        console.print()
        return

    # Handle test connection flag
    if test_connection:
        from bist_cli.api_client import APIClient

        settings = get_settings()
        console.print(f"\n[cyan]API Bağlantısı Test Ediliyor...[/cyan]")
        console.print(f"[dim]Base URL: {settings.api_base_url}[/dim]\n")

        api = APIClient()
        if api.test_connection():
            print_success("API bağlantısı başarılı!")
            console.print()
        else:
            print_error("API'ye bağlanılamadı!")
            console.print("[yellow]BIST Trading Platform'un çalıştığından emin olun.[/yellow]")
            console.print(f"[dim]URL: {settings.api_base_url}/actuator/health[/dim]\n")
            sys.exit(1)
        return

    # Run main menu
    try:
        menu = MainMenu(debug=debug)
        menu.run()
    except KeyboardInterrupt:
        console.print("\n")
        print_success("Program sonlandırıldı")
        console.print()
    except Exception as e:
        console.print(f"\n[red]Kritik hata:[/red] {str(e)}\n")
        if verbose:
            console.print_exception()
        sys.exit(1)


if __name__ == "__main__":
    app()
