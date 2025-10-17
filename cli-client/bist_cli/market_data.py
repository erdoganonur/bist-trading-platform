"""
Market data operations for BIST CLI client.
Provides access to symbols, prices, and market information.
"""

from typing import List, Dict, Any, Optional

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.prompt import Prompt
from rich import box

from .api_client import APIClient, APIError
from .utils import (
    print_success,
    print_error,
    print_info,
    format_currency,
    format_percentage,
    create_table
)


console = Console()


class MarketDataManager:
    """Manages market data operations."""

    def __init__(self, api_client: APIClient):
        """
        Initialize market data manager.

        Args:
            api_client: API client instance
        """
        self.api = api_client

    def view_symbols(self, page: int = 0, size: int = 20) -> None:
        """
        Display list of symbols with market data.

        Args:
            page: Page number
            size: Page size
        """
        try:
            console.print("\n[dim]Semboller yükleniyor...[/dim]")

            response = self.api.get(
                "/api/v1/symbols",
                params={"page": page, "size": size}
            )

            if "content" in response:
                symbols = response["content"]
                total = response.get("totalElements", 0)

                # Create table
                table = Table(
                    title=f"BIST Sembolleri (Sayfa {page + 1})",
                    box=box.ROUNDED,
                    show_header=True,
                    header_style="bold cyan"
                )

                table.add_column("Sembol", style="cyan", no_wrap=True)
                table.add_column("Şirket", style="white")
                table.add_column("Fiyat", justify="right", style="yellow")
                table.add_column("Değişim", justify="right")
                table.add_column("Hacim", justify="right", style="dim")

                for symbol in symbols:
                    # Format change percentage
                    change = symbol.get("changePercent", 0)
                    change_str = format_percentage(change)

                    # Format volume
                    volume = symbol.get("volume", 0)
                    volume_str = f"{volume:,}" if volume else "-"

                    table.add_row(
                        symbol.get("symbol", "N/A"),
                        symbol.get("name", "N/A")[:30],
                        format_currency(symbol.get("lastPrice", 0)),
                        change_str,
                        volume_str
                    )

                console.print()
                console.print(table)
                console.print(f"\n[dim]Toplam {total} sembol[/dim]")

            else:
                print_error("Sembol verisi alınamadı")

        except APIError as e:
            print_error(f"API hatası: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def search_symbol(self) -> None:
        """Interactive symbol search."""
        console.print()
        query = Prompt.ask("[cyan]Sembol veya şirket adı[/cyan]")

        if not query or len(query) < 1:
            print_error("En az 1 karakter girin")
            return

        try:
            console.print("\n[dim]Aranıyor...[/dim]")

            results = self.api.get("/api/v1/symbols/search", params={"q": query})

            if results:
                table = Table(
                    title=f"Arama Sonuçları: '{query}'",
                    box=box.ROUNDED,
                    show_header=True,
                    header_style="bold cyan"
                )

                table.add_column("Sembol", style="cyan")
                table.add_column("Şirket", style="white")
                table.add_column("Sektör", style="dim")
                table.add_column("Fiyat", justify="right", style="yellow")

                for symbol in results[:10]:  # Show top 10
                    table.add_row(
                        symbol.get("symbol", "N/A"),
                        symbol.get("name", "N/A"),
                        symbol.get("sector", "N/A"),
                        format_currency(symbol.get("lastPrice", 0))
                    )

                console.print()
                console.print(table)
                console.print(f"\n[dim]{len(results)} sonuç bulundu[/dim]")
            else:
                print_info("Sonuç bulunamadı")

        except APIError as e:
            print_error(f"Arama hatası: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_symbol_detail(self, symbol: Optional[str] = None) -> None:
        """
        Display detailed information for a symbol.

        Args:
            symbol: Symbol code (if None, prompts user)
        """
        if not symbol:
            console.print()
            symbol = Prompt.ask("[cyan]Sembol kodu (örn: AKBNK)[/cyan]").upper()

        if not symbol:
            print_error("Sembol kodu gerekli")
            return

        try:
            console.print(f"\n[dim]{symbol} detayları yükleniyor...[/dim]")

            data = self.api.get(f"/api/v1/symbols/{symbol}")

            # Display symbol details
            console.print()
            console.print(Panel.fit(
                f"[bold cyan]{data.get('symbol')}[/bold cyan] - {data.get('name', 'N/A')}",
                border_style="cyan"
            ))

            # Price information
            price_info = (
                f"[yellow]Son Fiyat:[/yellow] {format_currency(data.get('lastPrice', 0))}\n"
                f"[yellow]Değişim:[/yellow] {format_percentage(data.get('changePercent', 0))}\n"
                f"[yellow]Açılış:[/yellow] {format_currency(data.get('openPrice', 0))}\n"
                f"[yellow]En Yüksek:[/yellow] {format_currency(data.get('highPrice', 0))}\n"
                f"[yellow]En Düşük:[/yellow] {format_currency(data.get('lowPrice', 0))}\n"
                f"[yellow]Hacim:[/yellow] {data.get('volume', 0):,}"
            )

            # Additional info
            additional_info = (
                f"[cyan]Borsa:[/cyan] {data.get('exchange', 'N/A')}\n"
                f"[cyan]Sektör:[/cyan] {data.get('sector', 'N/A')}\n"
                f"[cyan]Durum:[/cyan] {data.get('tradingStatus', 'N/A')}"
            )

            console.print(Panel(price_info, title="Fiyat Bilgileri", border_style="yellow"))
            console.print(Panel(additional_info, title="Genel Bilgiler", border_style="cyan"))
            console.print()

        except APIError as e:
            if e.status_code == 404:
                print_error(f"Sembol bulunamadı: {symbol}")
            else:
                print_error(f"API hatası: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_sectors(self) -> None:
        """Display list of sectors."""
        try:
            console.print("\n[dim]Sektörler yükleniyor...[/dim]")

            sectors = self.api.get("/api/v1/symbols/metadata/sectors")

            if sectors:
                console.print()
                console.print(Panel.fit(
                    "[bold cyan]BIST Sektörleri[/bold cyan]",
                    border_style="cyan"
                ))

                # Create columns
                cols = 3
                rows = []
                for i in range(0, len(sectors), cols):
                    row = sectors[i:i + cols]
                    rows.append(row)

                table = Table(show_header=False, box=None, padding=(0, 2))
                for _ in range(cols):
                    table.add_column()

                for row in rows:
                    # Pad row if needed
                    while len(row) < cols:
                        row.append("")
                    table.add_row(*row)

                console.print()
                console.print(table)
                console.print(f"\n[dim]Toplam {len(sectors)} sektör[/dim]")

            else:
                print_error("Sektör bilgisi alınamadı")

        except APIError as e:
            print_error(f"API hatası: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def market_data_menu(self) -> None:
        """Interactive market data menu."""
        while True:
            console.print()
            console.print(Panel.fit(
                "[bold cyan]Piyasa Verileri[/bold cyan]\n\n"
                "1. Sembol Listesi\n"
                "2. Sembol Ara\n"
                "3. Sembol Detayı\n"
                "4. Sektörler\n"
                "5. Geri Dön",
                border_style="cyan"
            ))

            choice = Prompt.ask(
                "\n[cyan]Seçiminiz[/cyan]",
                choices=["1", "2", "3", "4", "5"],
                default="5"
            )

            if choice == "1":
                self.view_symbols()
            elif choice == "2":
                self.search_symbol()
            elif choice == "3":
                self.view_symbol_detail()
            elif choice == "4":
                self.view_sectors()
            elif choice == "5":
                break
