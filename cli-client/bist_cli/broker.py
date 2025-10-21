"""
Broker operations for BIST CLI client.
Provides access to broker account information and operations.
"""

from typing import Dict, Any

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich import box

from .api_client import APIClient, APIError
from .utils import (
    print_success,
    print_error,
    print_info,
    print_warning,
    format_currency,
    format_timestamp
)
from .debug import (
    debug_print,
    debug_object,
    debug_websocket_message,
    is_debug_enabled
)


console = Console()


class BrokerManager:
    """Manages broker operations."""

    def __init__(self, api_client: APIClient):
        """
        Initialize broker manager.

        Args:
            api_client: API client instance
        """
        self.api = api_client

    def view_account_info(self) -> None:
        """Display broker account information."""
        try:
            console.print("\n[dim]Hesap bilgileri yükleniyor...[/dim]")

            response = self.api.get("/api/v1/broker/account")

            # AlgoLabResponse wrapper - extract content
            if isinstance(response, dict) and "content" in response:
                account = response.get("content", {})
            else:
                account = response

            console.print()
            console.print(Panel.fit(
                "[bold yellow]AlgoLab Broker Hesap Bilgileri[/bold yellow]",
                border_style="yellow"
            ))

            # Account summary
            summary = (
                f"[yellow]Hesap No:[/yellow] {account.get('accountNumber', 'N/A')}\n"
                f"[yellow]Müşteri No:[/yellow] {account.get('customerId', 'N/A')}\n"
                f"[yellow]Durum:[/yellow] {account.get('status', 'N/A')}\n"
                f"[yellow]Para Birimi:[/yellow] {account.get('currency', 'TRY')}"
            )

            # Balance information
            balance = (
                f"[green]Toplam Bakiye:[/green] {format_currency(account.get('totalBalance', 0))}\n"
                f"[cyan]Kullanılabilir:[/cyan] {format_currency(account.get('availableBalance', 0))}\n"
                f"[magenta]Bloke:[/magenta] {format_currency(account.get('blockedBalance', 0))}\n"
                f"[yellow]Portföy Değeri:[/yellow] {format_currency(account.get('portfolioValue', 0))}"
            )

            console.print()
            console.print(Panel(summary, title="Hesap Özeti", border_style="yellow"))
            console.print(Panel(balance, title="Bakiye Bilgileri", border_style="green"))
            console.print()

        except APIError as e:
            if e.status_code == 401:
                print_error("AlgoLab kimlik doğrulaması gerekli")
                print_info("Ana menüden 'AlgoLab Bağlantısı' seçeneğini kullanın")
            else:
                print_error(f"Hesap bilgisi alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_positions(self) -> None:
        """Display current positions."""
        try:
            console.print("\n[dim]Pozisyonlar yükleniyor...[/dim]")

            response = self.api.get("/api/v1/broker/positions")

            # Debug logging (only if debug mode is enabled)
            debug_object(response, "API Response for Positions")

            # AlgoLabResponse wrapper - extract content
            if isinstance(response, dict) and "content" in response:
                content = response.get("content", {})
                debug_print(f"Content extracted, type: {type(content)}")

                # If content is a dict with "positions" key, extract it
                if isinstance(content, dict) and "positions" in content:
                    positions = content.get("positions", [])
                    debug_print("Positions array extracted from content dict")
                # If content is already a list, use it directly
                elif isinstance(content, list):
                    positions = content
                    debug_print("Content is already a list")
                else:
                    positions = []
                    debug_print("Content format unexpected, using empty list")
            else:
                positions = response if isinstance(response, list) else []
                debug_print("Using response directly")

            debug_print(f"Final positions - type: {type(positions)}, count: {len(positions) if isinstance(positions, list) else 'N/A'}")

            if not positions or (isinstance(positions, list) and len(positions) == 0):
                print_info("Açık pozisyon bulunamadı")
                return

            # Create positions table
            table = Table(
                title="Açık Pozisyonlar",
                box=box.ROUNDED,
                show_header=True,
                header_style="bold yellow"
            )

            table.add_column("Sembol", style="cyan")
            table.add_column("Miktar", justify="right")
            table.add_column("Ort. Fiyat", justify="right", style="dim")
            table.add_column("Son Fiyat", justify="right", style="yellow")
            table.add_column("Kar/Zarar", justify="right")
            table.add_column("Kar/Zarar %", justify="right")

            total_pnl = 0.0

            for pos in positions:
                # AlgoLab field names mapping
                # AlgoLab uses: code, totalstock, maliyet, unitprice, profit
                # CLI expects: symbol, quantity, averagePrice, lastPrice, profitLoss

                symbol = pos.get("code") or pos.get("symbol", "N/A")

                # Parse quantity (might be string like "365.000000")
                quantity_str = pos.get("totalstock") or pos.get("quantity", "0")
                try:
                    quantity = int(float(str(quantity_str)))
                except (ValueError, TypeError):
                    quantity = 0

                # Parse prices (might be string)
                avg_price_str = pos.get("maliyet") or pos.get("cost") or pos.get("averagePrice", "0")
                try:
                    avg_price = float(str(avg_price_str))
                except (ValueError, TypeError):
                    avg_price = 0.0

                last_price_str = pos.get("unitprice") or pos.get("lastPrice", "0")
                try:
                    last_price = float(str(last_price_str))
                except (ValueError, TypeError):
                    last_price = 0.0

                # Parse profit/loss (might be string)
                pnl_str_raw = pos.get("profit") or pos.get("profitLoss", "0")
                try:
                    pnl = float(str(pnl_str_raw))
                except (ValueError, TypeError):
                    pnl = 0.0

                # Calculate profit/loss percentage if not provided
                pnl_pct = pos.get("profitLossPercent")
                if pnl_pct is None and avg_price > 0:
                    pnl_pct = (pnl / (avg_price * quantity)) * 100 if quantity > 0 else 0
                else:
                    try:
                        pnl_pct = float(str(pnl_pct)) if pnl_pct else 0
                    except (ValueError, TypeError):
                        pnl_pct = 0.0

                # Skip summary rows (type='0' or code='-')
                if symbol == "-" or pos.get("type") == "0" or pos.get("explanation") == "total":
                    continue

                total_pnl += pnl

                # Color code P&L
                pnl_color = "green" if pnl >= 0 else "red"
                pnl_str = f"[{pnl_color}]{format_currency(pnl)}[/{pnl_color}]"
                pnl_pct_str = f"[{pnl_color}]{pnl_pct:+.2f}%[/{pnl_color}]"

                table.add_row(
                    symbol,
                    str(quantity),
                    format_currency(avg_price),
                    format_currency(last_price),
                    pnl_str,
                    pnl_pct_str
                )

            console.print()
            console.print(table)

            # Show total P&L
            total_color = "green" if total_pnl >= 0 else "red"
            console.print(
                f"\n[bold]Toplam Kar/Zarar:[/bold] [{total_color}]{format_currency(total_pnl)}[/{total_color}]"
            )
            console.print()

        except APIError as e:
            if e.status_code == 401:
                print_error("AlgoLab kimlik doğrulaması gerekli")
            else:
                print_error(f"Pozisyonlar alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_algolab_status(self) -> None:
        """Display AlgoLab connection status."""
        try:
            console.print("\n[dim]AlgoLab durumu kontrol ediliyor...[/dim]")

            status = self.api.get("/api/v1/broker/auth/status")

            console.print()

            if status.get("authenticated"):
                status_text = (
                    f"[green]✓ Bağlı[/green]\n\n"
                    f"[yellow]Kullanıcı:[/yellow] {status.get('username', 'N/A')}\n"
                    f"[yellow]Oturum:[/yellow] {status.get('sessionId', 'N/A')[:16]}...\n"
                    f"[yellow]Geçerlilik:[/yellow] {format_timestamp(status.get('expiresAt', 'N/A'))}\n"
                    f"[yellow]WebSocket:[/yellow] {'Bağlı' if status.get('websocketConnected') else 'Bağlı Değil'}"
                )
                border_style = "green"
            else:
                status_text = (
                    "[red]✗ Bağlı Değil[/red]\n\n"
                    "AlgoLab broker entegrasyonu için kimlik doğrulama yapın."
                )
                border_style = "red"

            console.print(Panel(
                status_text,
                title="AlgoLab Durumu",
                border_style=border_style
            ))
            console.print()

        except APIError as e:
            print_error(f"Durum bilgisi alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def test_websocket_connection(self) -> None:
        """Test WebSocket connection to AlgoLab."""
        try:
            console.print("\n[dim]WebSocket bağlantısı test ediliyor...[/dim]")

            response = self.api.get("/api/v1/broker/websocket/status")

            console.print()

            if response.get("connected"):
                status_text = (
                    f"[green]✓ WebSocket Bağlı[/green]\n\n"
                    f"[yellow]URL:[/yellow] {response.get('url', 'N/A')}\n"
                    f"[yellow]Authenticated:[/yellow] {'Evet' if response.get('authenticated') else 'Hayır'}\n"
                    f"[yellow]Son Heartbeat:[/yellow] {format_timestamp(response.get('lastHeartbeat', 'N/A'))}\n"
                    f"[yellow]Mesaj Sayısı:[/yellow] {response.get('messageCount', 0)}"
                )
                border_style = "green"
            else:
                status_text = (
                    "[red]✗ WebSocket Bağlı Değil[/red]\n\n"
                    f"[yellow]Durum:[/yellow] {response.get('status', 'Disconnected')}\n"
                    f"[yellow]Son Hata:[/yellow] {response.get('lastError', 'N/A')}\n\n"
                    "WebSocket bağlantısını backend'den etkinleştirin:\n"
                    "[dim]application-dev.yml → algolab.websocket.enabled: true[/dim]"
                )
                border_style = "red"

            console.print(Panel(
                status_text,
                title="AlgoLab WebSocket Durumu",
                border_style=border_style
            ))
            console.print()

        except APIError as e:
            if e.status_code == 404:
                print_warning("WebSocket API endpoint'i bulunamadı")
                print_info("Backend'de WebSocket devre dışı olabilir")
            else:
                print_error(f"WebSocket durumu alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_realtime_ticks(self) -> None:
        """Display real-time tick data for a symbol with configurable debugging."""
        import time
        from rich.live import Live
        from rich.layout import Layout
        from rich.text import Text

        try:
            symbol = Prompt.ask(
                "\n[yellow]Sembol kodu (örn: USDTRY, AKBNK, THYAO)[/yellow]",
                default="USDTRY"
            )

            # Check backend connection first
            try:
                console.print(f"\n[dim]Backend bağlantısı kontrol ediliyor...[/dim]")
                ws_status = self.api.get("/api/v1/broker/websocket/status")
                debug_object(ws_status, "WebSocket Status")

                if not ws_status.get("connected"):
                    console.print("[red]⚠ WebSocket bağlı değil! AlgoLab login yapmanız gerekiyor.[/red]")
                    console.print("[yellow]Ana menü → AlgoLab Bağlantısı → AlgoLab Login[/yellow]\n")
                    return
            except Exception as e:
                console.print(f"[yellow]⚠ WebSocket status kontrolü başarısız: {str(e)}[/yellow]")

            # Subscribe to this symbol via backend WebSocket
            try:
                console.print(f"\n[dim]{symbol} için WebSocket subscription yapılıyor...[/dim]")
                response = self.api.post("/api/v1/broker/websocket/subscribe", data={"symbol": symbol, "channel": "tick"})

                debug_object(response, "Subscribe Response")

                if response.get("success"):
                    console.print(f"[green]✓ {symbol} için subscription başarılı[/green]")
                else:
                    console.print(f"[yellow]⚠ Subscription başarısız: {response.get('message', 'Unknown error')}[/yellow]")
            except Exception as e:
                console.print(f"[yellow]⚠ Subscription hatası (devam ediliyor): {str(e)}[/yellow]")

            console.print(f"\n[dim]{symbol} için real-time tick data gösteriliyor...[/dim]")
            debug_status = "ON" if is_debug_enabled() else "OFF"
            console.print(f"[dim]Çıkmak için Ctrl+C | Debug Mode: {debug_status}[/dim]\n")

            # Create tick data table
            def create_table(messages):
                table = Table(
                    title=f"{symbol} - Real-Time Tick Data (Son {len(messages)} mesaj)",
                    box=box.ROUNDED,
                    show_header=True,
                    header_style="bold yellow"
                )

                table.add_column("Zaman", style="dim", width=12)
                table.add_column("Sembol", style="cyan", width=10)
                table.add_column("Son Fiyat", style="yellow", justify="right", width=12)
                table.add_column("Değişim %", justify="right", width=12)
                table.add_column("Hacim", justify="right", width=15)
                table.add_column("Alış", justify="right", style="green", width=12)
                table.add_column("Satış", justify="right", style="red", width=12)

                for msg in messages[-15:]:  # Show last 15 messages
                    data = msg.get("data", {})
                    received_at = msg.get("receivedAt", "")

                    # Format time
                    try:
                        from datetime import datetime
                        dt = datetime.fromisoformat(received_at.replace("Z", "+00:00"))
                        time_str = dt.strftime("%H:%M:%S")
                    except:
                        time_str = received_at[:8] if len(received_at) >= 8 else received_at

                    # Format prices - match backend API field names (handle None values)
                    last_price = data.get("Price") or data.get("lastPrice") or 0
                    change_pct = data.get("changePercent") or data.get("change") or 0
                    volume = data.get("volume") or data.get("totalVolume") or 0
                    bid = data.get("bid") or data.get("bidPrice") or 0
                    ask = data.get("ask") or data.get("askPrice") or 0
                    symbol_code = data.get("Symbol") or data.get("symbol") or symbol

                    # Ensure numeric types (None-safe)
                    try:
                        last_price = float(last_price) if last_price is not None else 0
                        change_pct = float(change_pct) if change_pct is not None else 0
                        volume = int(volume) if volume is not None else 0
                        bid = float(bid) if bid is not None else 0
                        ask = float(ask) if ask is not None else 0
                    except (ValueError, TypeError):
                        pass  # Keep default 0 values

                    # Color code change
                    change_color = "green" if change_pct >= 0 else "red"
                    change_str = f"[{change_color}]{change_pct:+.2f}%[/{change_color}]"

                    table.add_row(
                        time_str,
                        symbol_code,
                        format_currency(last_price),
                        change_str,
                        f"{volume:,}" if volume else "-",
                        format_currency(bid) if bid else "-",
                        format_currency(ask) if ask else "-"
                    )

                return table

            # Polling loop with Live display (auto-refresh, no scrolling)
            with Live(create_table([]), refresh_per_second=4, console=console, screen=False) as live:
                consecutive_empty = 0
                poll_count = 0
                last_message_count = 0

                while True:
                    try:
                        poll_count += 1

                        # Poll backend for recent ticks
                        response = self.api.get(f"/api/v1/broker/websocket/stream/ticks/{symbol}?limit=15")

                        messages = response.get("messages", [])
                        message_count = len(messages)

                        if is_debug_enabled() and poll_count % 5 == 0:
                            # Show debug info every 5 polls
                            console.print(f"[dim]DEBUG #{poll_count} - Messages: {message_count}, Response keys: {list(response.keys())}[/dim]")

                        if messages:
                            consecutive_empty = 0

                            # Create status footer
                            status_text = f"[green]● LIVE[/green] | Messages: {message_count} | Poll: #{poll_count} | Updated: {time.strftime('%H:%M:%S')}"
                            if message_count != last_message_count:
                                status_text += f" [yellow]↑ +{message_count - last_message_count}[/yellow]"
                            last_message_count = message_count

                            # Update table with status
                            data_table = create_table(messages)
                            if is_debug_enabled():
                                data_table.caption = status_text

                            live.update(data_table)

                        else:
                            consecutive_empty += 1

                            # Create waiting info table with debug details
                            info_table = Table(title="Mesaj Bekleniyor", box=box.ROUNDED)
                            info_table.add_column("Durum", style="yellow")

                            info_table.add_row(f"[yellow]{symbol} için WebSocket mesajı bekleniyor...[/yellow]")
                            info_table.add_row("[dim]Backend WebSocket bağlantısının aktif olduğundan emin olun.[/dim]")

                            if is_debug_enabled():
                                info_table.add_row(f"[dim]Poll Count: {poll_count} | Empty Count: {consecutive_empty}[/dim]")
                                info_table.add_row(f"[dim]API Response: {response}[/dim]")

                            if consecutive_empty == 1:
                                info_table.add_row("\n[cyan]💡 İpucu: AlgoLab'a giriş yaptınız mı?[/cyan]")
                            elif consecutive_empty > 10:
                                info_table.add_row("\n[red]⚠ 10+ saniye veri yok! Bağlantıyı kontrol edin.[/red]")

                            live.update(info_table)

                        # Poll every 1 second
                        time.sleep(1)

                    except KeyboardInterrupt:
                        raise
                    except Exception as e:
                        if is_debug_enabled():
                            console.print(f"[red]DEBUG - Polling hatası: {str(e)}[/red]")
                        print_error(f"Polling hatası: {str(e)}")
                        time.sleep(2)

        except APIError as e:
            print_error(f"Tick stream'e erişilemedi: {e.message}")
        except KeyboardInterrupt:
            console.print("\n\n[yellow]Stream kapatıldı[/yellow]")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_order_book(self) -> None:
        """Display real-time order book for a symbol."""
        try:
            symbol = Prompt.ask(
                "\n[yellow]Sembol kodu (örn: AKBNK, THYAO)[/yellow]",
                default="AKBNK"
            )

            console.print(f"\n[dim]{symbol} emir defteri yükleniyor...[/dim]\n")

            # Get order book snapshot
            response = self.api.get(f"/api/v1/broker/orderbook/{symbol}")

            orderbook = response.get("content", response)

            # Create bid/ask tables
            bids = orderbook.get("bids", [])
            asks = orderbook.get("asks", [])

            # Bids table (Alış emirleri)
            bid_table = Table(
                title="ALIŞ EMİRLERİ (Bids)",
                box=box.ROUNDED,
                show_header=True,
                header_style="bold green"
            )
            bid_table.add_column("Fiyat", style="green")
            bid_table.add_column("Miktar", justify="right")
            bid_table.add_column("Emir Sayısı", justify="right", style="dim")

            for bid in bids[:10]:  # Show top 10
                bid_table.add_row(
                    format_currency(bid.get("price", 0)),
                    str(bid.get("quantity", 0)),
                    str(bid.get("orderCount", 0))
                )

            # Asks table (Satış emirleri)
            ask_table = Table(
                title="SATIŞ EMİRLERİ (Asks)",
                box=box.ROUNDED,
                show_header=True,
                header_style="bold red"
            )
            ask_table.add_column("Fiyat", style="red")
            ask_table.add_column("Miktar", justify="right")
            ask_table.add_column("Emir Sayısı", justify="right", style="dim")

            for ask in asks[:10]:  # Show top 10
                ask_table.add_row(
                    format_currency(ask.get("price", 0)),
                    str(ask.get("quantity", 0)),
                    str(ask.get("orderCount", 0))
                )

            console.print(bid_table)
            console.print()
            console.print(ask_table)

            # Show spread
            if bids and asks:
                spread = float(asks[0].get("price", 0)) - float(bids[0].get("price", 0))
                mid_price = (float(asks[0].get("price", 0)) + float(bids[0].get("price", 0))) / 2

                console.print(
                    f"\n[yellow]Spread:[/yellow] {format_currency(spread)}\n"
                    f"[yellow]Orta Fiyat:[/yellow] {format_currency(mid_price)}\n"
                )

        except APIError as e:
            if e.status_code == 404:
                print_warning("Order book API endpoint'i bulunamadı")
                print_info("Bu özellik henüz backend'de implement edilmemiş olabilir")
            else:
                print_error(f"Order book alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_trade_stream(self) -> None:
        """Display real-time trade stream for a symbol."""
        import time
        from rich.live import Live

        try:
            symbol = Prompt.ask(
                "\n[yellow]Sembol kodu (örn: USDTRY, AKBNK, THYAO)[/yellow]",
                default="USDTRY"
            )

            console.print(f"\n[dim]{symbol} için gerçekleşen işlemler gösteriliyor...[/dim]")
            console.print("[dim]Çıkmak için Ctrl+C[/dim]\n")

            # Create trade stream table
            def create_table(messages):
                table = Table(
                    title=f"{symbol} - Real-Time Trades (Son {len(messages)} işlem)",
                    box=box.ROUNDED,
                    show_header=True,
                    header_style="bold yellow"
                )

                table.add_column("Zaman", style="dim", width=12)
                table.add_column("Sembol", style="cyan", width=10)
                table.add_column("Fiyat", style="yellow", justify="right", width=12)
                table.add_column("Miktar", justify="right", width=12)
                table.add_column("Yön", justify="center", width=8)
                table.add_column("Tutar", justify="right", width=15)

                for msg in messages[-15:]:  # Show last 15 trades
                    data = msg.get("data", {})
                    received_at = msg.get("receivedAt", "")

                    # Format time
                    try:
                        from datetime import datetime
                        dt = datetime.fromisoformat(received_at.replace("Z", "+00:00"))
                        time_str = dt.strftime("%H:%M:%S")
                    except:
                        time_str = received_at[:8] if len(received_at) >= 8 else received_at

                    # Format trade data
                    price = data.get("price", 0)
                    quantity = data.get("quantity", 0)
                    side = data.get("side", "").upper()
                    amount = price * quantity if (price and quantity) else 0
                    symbol_code = data.get("symbol", symbol)

                    # Color code side
                    if side == "BUY":
                        side_str = "[green]ALIŞ[/green]"
                    elif side == "SELL":
                        side_str = "[red]SATIŞ[/red]"
                    else:
                        side_str = side or "-"

                    table.add_row(
                        time_str,
                        symbol_code,
                        format_currency(price),
                        f"{quantity:,}" if quantity else "-",
                        side_str,
                        format_currency(amount) if amount else "-"
                    )

                return table

            # Polling loop with Live display
            with Live(create_table([]), refresh_per_second=2, console=console) as live:
                consecutive_empty = 0
                while True:
                    try:
                        # Poll backend for recent trades
                        response = self.api.get(f"/api/v1/broker/websocket/stream/trades/{symbol}?limit=15")

                        messages = response.get("messages", [])

                        if messages:
                            consecutive_empty = 0
                            live.update(create_table(messages))
                        else:
                            consecutive_empty += 1
                            if consecutive_empty == 1:
                                # First time empty - show info
                                info_table = Table(title="İşlem Bekleniyor", box=box.ROUNDED)
                                info_table.add_column("Durum")
                                info_table.add_row(f"[yellow]{symbol} için WebSocket trade mesajı bekleniyor...[/yellow]")
                                info_table.add_row("[dim]Backend WebSocket bağlantısının aktif olduğundan emin olun.[/dim]")
                                info_table.add_row("[dim]Trade mesajları sadece işlem olduğunda gelir.[/dim]")
                                live.update(info_table)

                        # Poll every 1 second
                        time.sleep(1)

                    except KeyboardInterrupt:
                        raise
                    except Exception as e:
                        print_error(f"Polling hatası: {str(e)}")
                        time.sleep(2)

        except APIError as e:
            print_error(f"Trade stream'e erişilemedi: {e.message}")
        except KeyboardInterrupt:
            console.print("\n\n[yellow]Stream kapatıldı[/yellow]")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_multi_symbol_ticks(self) -> None:
        """Display real-time tick data for multiple symbols simultaneously."""
        import time
        from rich.live import Live
        from rich.columns import Columns
        from datetime import datetime

        try:
            symbols_input = Prompt.ask(
                "\n[yellow]Sembol kodları (virgülle ayırın)[/yellow]",
                default="USDTRY,EURTRY,THYAO"
            )
            symbols = [s.strip().upper() for s in symbols_input.split(",") if s.strip()]

            if not symbols:
                print_warning("En az bir sembol girmelisiniz")
                return

            console.print(f"\n[dim]{len(symbols)} sembol için subscription yapılıyor...[/dim]")

            # Subscribe to all symbols
            success_count = 0
            for symbol in symbols:
                try:
                    response = self.api.post("/api/v1/broker/websocket/subscribe",
                                           data={"symbol": symbol, "channel": "tick"})
                    if response.get("success"):
                        success_count += 1
                except Exception as e:
                    debug_print(f"Subscription error for {symbol}: {str(e)}")

            console.print(f"[green]✓ {success_count}/{len(symbols)} sembol için subscription başarılı[/green]")
            console.print(f"\n[dim]Multi-symbol tick data gösteriliyor...[/dim]")
            console.print(f"[dim]Çıkmak için Ctrl+C[/dim]\n")

            # Create individual symbol table
            def create_symbol_table(symbol: str, messages: list) -> Table:
                table = Table(
                    title=f"{symbol}",
                    box=box.ROUNDED,
                    show_header=True,
                    header_style="bold yellow",
                    width=50
                )

                table.add_column("Alan", style="cyan", width=15)
                table.add_column("Değer", justify="right", width=25)

                if messages:
                    msg = messages[-1]  # Get latest message
                    data = msg.get("data", {})
                    received_at = msg.get("receivedAt", "")

                    # Format time
                    try:
                        dt = datetime.fromisoformat(received_at.replace("Z", "+00:00"))
                        time_str = dt.strftime("%H:%M:%S")
                    except:
                        time_str = received_at[:8] if len(received_at) >= 8 else "-"

                    # Extract and format data (None-safe)
                    last_price = data.get("Price") or data.get("lastPrice") or 0
                    change_pct = data.get("changePercent") or data.get("change") or 0
                    volume = data.get("volume") or data.get("totalVolume") or 0
                    bid = data.get("bid") or data.get("bidPrice") or 0
                    ask = data.get("ask") or data.get("askPrice") or 0

                    # Ensure numeric types
                    try:
                        last_price = float(last_price) if last_price is not None else 0
                        change_pct = float(change_pct) if change_pct is not None else 0
                        volume = int(volume) if volume is not None else 0
                        bid = float(bid) if bid is not None else 0
                        ask = float(ask) if ask is not None else 0
                    except (ValueError, TypeError):
                        pass

                    # Color code change
                    change_color = "green" if change_pct >= 0 else "red"
                    change_str = f"[{change_color}]{change_pct:+.2f}%[/{change_color}]"

                    table.add_row("Zaman", f"[dim]{time_str}[/dim]")
                    table.add_row("Son Fiyat", f"[yellow]{format_currency(last_price)}[/yellow]")
                    table.add_row("Değişim", change_str)
                    table.add_row("Hacim", f"{volume:,}" if volume else "-")
                    table.add_row("Alış", f"[green]{format_currency(bid)}[/green]" if bid else "-")
                    table.add_row("Satış", f"[red]{format_currency(ask)}[/red]" if ask else "-")
                else:
                    table.add_row("Durum", "[yellow]Veri bekleniyor...[/yellow]")

                return table

            # Create multi-symbol layout
            def create_multi_layout(data_by_symbol: dict) -> Columns:
                tables = []
                for symbol in symbols:
                    messages = data_by_symbol.get(symbol, [])
                    tables.append(create_symbol_table(symbol, messages))

                # Arrange in columns (max 3 per row)
                return Columns(tables, equal=True, expand=True)

            # Polling loop
            with Live(create_multi_layout({}), refresh_per_second=2, console=console, screen=False) as live:
                poll_count = 0

                while True:
                    try:
                        poll_count += 1
                        data_by_symbol = {}

                        # Poll for each symbol
                        for symbol in symbols:
                            try:
                                response = self.api.get(f"/api/v1/broker/websocket/stream/ticks/{symbol}?limit=1")
                                data_by_symbol[symbol] = response.get("messages", [])
                            except Exception as e:
                                debug_print(f"Poll error for {symbol}: {str(e)}")
                                data_by_symbol[symbol] = []

                        # Update display
                        live.update(create_multi_layout(data_by_symbol))

                        time.sleep(0.5)  # Poll every 500ms

                    except KeyboardInterrupt:
                        console.print("\n[yellow]Multi-symbol monitoring durduruldu[/yellow]")
                        break
                    except Exception as e:
                        debug_print(f"Polling error: {str(e)}")
                        time.sleep(1)

        except KeyboardInterrupt:
            console.print("\n[yellow]İptal edildi[/yellow]")
        except Exception as e:
            print_error(f"Multi-symbol monitoring hatası: {str(e)}")
            debug_object(e, "Exception")

    def send_order(self) -> None:
        """Send a new order to the broker."""
        try:
            console.print()
            console.print(Panel.fit(
                "[bold red]⚠️  CANLI EMİR GÖNDERİMİ ⚠️[/bold red]\n\n"
                "[yellow]Bu GERÇEK bir emirdir ve GERÇEK PARA ile işlem yapacaktır![/yellow]\n"
                "[dim]Devam etmeden önce tüm bilgileri kontrol edin.[/dim]",
                border_style="red"
            ))

            # Get order details from user
            symbol = Prompt.ask("\n[cyan]Sembol kodu (örn: AKBNK, THYAO)[/cyan]").upper()

            console.print("\n[yellow]Emir Yönü:[/yellow]")
            console.print("  0 - ALIŞ (BUY)")
            console.print("  1 - SATIŞ (SELL)")
            direction = Prompt.ask("[cyan]Yön[/cyan]", choices=["0", "1"])

            console.print("\n[yellow]Fiyat Tipi:[/yellow]")
            console.print("  L - Limit")
            console.print("  P - Piyasa (Market)")
            price_type = Prompt.ask("[cyan]Fiyat Tipi[/cyan]", choices=["L", "P"], default="L").upper()

            price = None
            if price_type == "L":
                price = Prompt.ask("[cyan]Limit Fiyat[/cyan]")

            lot = Prompt.ask("[cyan]Lot (Miktar)[/cyan]")

            # Optional parameters
            sms = Prompt.ask("[cyan]SMS bildirimi (H/E)[/cyan]", choices=["H", "E"], default="H").upper()
            email = Prompt.ask("[cyan]Email bildirimi (H/E)[/cyan]", choices=["H", "E"], default="H").upper()

            # Confirmation
            console.print()
            console.print(Panel(
                f"[yellow]Sembol:[/yellow] {symbol}\n"
                f"[yellow]Yön:[/yellow] {'ALIŞ' if direction == '0' else 'SATIŞ'}\n"
                f"[yellow]Fiyat Tipi:[/yellow] {'Limit' if price_type == 'L' else 'Piyasa'}\n"
                f"[yellow]Fiyat:[/yellow] {price if price else 'Piyasa Fiyatı'}\n"
                f"[yellow]Lot:[/yellow] {lot}\n"
                f"[yellow]SMS:[/yellow] {'Evet' if sms == 'H' else 'Hayır'}\n"
                f"[yellow]Email:[/yellow] {'Evet' if email == 'H' else 'Hayır'}",
                title="Emir Özeti",
                border_style="yellow"
            ))

            if not Confirm.ask("\n[red]⚠️  Bu EMRİ göndermek istediğinizden emin misiniz?[/red]"):
                print_info("Emir iptal edildi")
                return

            console.print("\n[dim]Emir gönderiliyor...[/dim]")

            # Send order via API
            order_data = {
                "symbol": symbol,
                "direction": direction,
                "priceType": price_type,
                "lot": lot,
                "sms": sms,
                "email": email,
                "subAccount": "0"
            }

            if price:
                order_data["price"] = price

            response = self.api.post("/api/v1/broker/orders", data=order_data)

            console.print()
            if response.get("success"):
                print_success("Emir başarıyla gönderildi!")

                content = response.get("content", {})
                if content:
                    info_text = (
                        f"[yellow]Emir ID:[/yellow] {content.get('orderId', 'N/A')}\n"
                        f"[yellow]Broker Emir ID:[/yellow] {content.get('brokerOrderId', 'N/A')}\n"
                        f"[yellow]Durum:[/yellow] {content.get('status', 'N/A')}"
                    )
                    console.print(Panel(info_text, title="Emir Bilgileri", border_style="green"))
            else:
                print_error(f"Emir gönderilemedi: {response.get('message', 'Bilinmeyen hata')}")

            console.print()

        except APIError as e:
            if e.status_code == 401:
                print_error("AlgoLab kimlik doğrulaması gerekli")
            elif e.status_code == 402:
                print_error("Yetersiz bakiye")
            elif e.status_code == 429:
                print_error("Çok fazla istek. Lütfen bekleyin.")
            else:
                print_error(f"Emir gönderilemedi: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def cancel_order(self) -> None:
        """Cancel an existing order."""
        try:
            console.print()
            console.print(Panel.fit(
                "[bold red]⚠️  EMİR İPTAL ⚠️[/bold red]\n\n"
                "[yellow]Bu işlem GERİ ALINMADIR![/yellow]\n"
                "[dim]İptal edilen emirler tekrar aktif edilemez.[/dim]",
                border_style="red"
            ))

            order_id = Prompt.ask("\n[cyan]İptal edilecek Emir ID[/cyan]")

            if not Confirm.ask(f"\n[red]⚠️  {order_id} numaralı emri iptal etmek istediğinizden emin misiniz?[/red]"):
                print_info("İptal işlemi iptal edildi")
                return

            console.print("\n[dim]Emir iptal ediliyor...[/dim]")

            response = self.api.delete(f"/api/v1/broker/orders/{order_id}")

            console.print()
            if response.get("success"):
                print_success("Emir başarıyla iptal edildi!")
                console.print(f"[dim]{response.get('message', '')}[/dim]")
            else:
                print_error(f"Emir iptal edilemedi: {response.get('message', 'Bilinmeyen hata')}")

            console.print()

        except APIError as e:
            if e.status_code == 404:
                print_error("Emir bulunamadı")
            elif e.status_code == 400:
                print_error("Emir iptal edilemiyor (zaten gerçekleşmiş olabilir)")
            else:
                print_error(f"Emir iptal edilemedi: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def modify_order(self) -> None:
        """Modify an existing order."""
        try:
            console.print()
            console.print(Panel.fit(
                "[bold red]⚠️  EMİR GÜNCELLEME ⚠️[/bold red]\n\n"
                "[yellow]Bu CANLI bir emir değişikliğidir![/yellow]\n"
                "[dim]Değişiklikler anında uygulanır.[/dim]",
                border_style="red"
            ))

            order_id = Prompt.ask("\n[cyan]Güncellenecek Emir ID[/cyan]")

            console.print("\n[yellow]Yeni bilgileri girin (değiştirmek istemiyorsanız Enter'a basın):[/yellow]")

            price = Prompt.ask("[cyan]Yeni Limit Fiyat[/cyan]", default="")
            lot = Prompt.ask("[cyan]Yeni Lot (Miktar)[/cyan]", default="")

            if not price and not lot:
                print_warning("Hiçbir değişiklik yapılmadı")
                return

            # Build update data
            update_data = {
                "subAccount": "0",
                "viop": "H"
            }

            if price:
                update_data["price"] = price
            if lot:
                update_data["lot"] = lot

            # Show confirmation
            console.print()
            changes = []
            if price:
                changes.append(f"[yellow]Yeni Fiyat:[/yellow] {price}")
            if lot:
                changes.append(f"[yellow]Yeni Lot:[/yellow] {lot}")

            console.print(Panel(
                "\n".join(changes),
                title=f"Emir {order_id} Değişiklikleri",
                border_style="yellow"
            ))

            if not Confirm.ask(f"\n[red]⚠️  Bu değişiklikleri uygulamak istediğinizden emin misiniz?[/red]"):
                print_info("Güncelleme iptal edildi")
                return

            console.print("\n[dim]Emir güncelleniyor...[/dim]")

            response = self.api.put(f"/api/v1/broker/orders/{order_id}", data=update_data)

            console.print()
            if response.get("success"):
                print_success("Emir başarıyla güncellendi!")
                console.print(f"[dim]{response.get('message', '')}[/dim]")
            else:
                print_error(f"Emir güncellenemedi: {response.get('message', 'Bilinmeyen hata')}")

            console.print()

        except APIError as e:
            if e.status_code == 404:
                print_error("Emir bulunamadı")
            elif e.status_code == 400:
                print_error("Geçersiz güncelleme parametreleri")
            else:
                print_error(f"Emir güncellenemedi: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def view_order_history(self) -> None:
        """Display order history with filtering options."""
        try:
            console.print()
            console.print(Panel.fit(
                "[bold yellow]Emir Geçmişi[/bold yellow]\n\n"
                "Filtreleme seçenekleri (boş bırakırsanız tüm emirler gösterilir)",
                border_style="yellow"
            ))

            # Get filter options
            symbol = Prompt.ask("\n[cyan]Sembol filtresi (boş = tümü)[/cyan]", default="")

            console.print("\n[yellow]Durum filtresi:[/yellow]")
            console.print("  1 - Bekleyen (PENDING)")
            console.print("  2 - Gerçekleşen (FILLED)")
            console.print("  3 - İptal (CANCELLED)")
            console.print("  4 - Tümü")
            status_choice = Prompt.ask("[cyan]Durum[/cyan]", choices=["1", "2", "3", "4"], default="4")

            status_map = {
                "1": "PENDING",
                "2": "FILLED",
                "3": "CANCELLED",
                "4": ""
            }
            status = status_map.get(status_choice, "")

            # Build query parameters
            params = {}
            if symbol:
                params["symbol"] = symbol.upper()
            if status:
                params["status"] = status
            params["page"] = 0
            params["size"] = 20

            console.print("\n[dim]Emir geçmişi yükleniyor...[/dim]")

            response = self.api.get("/api/v1/broker/orders/history", params=params)

            orders = response.get("content", [])

            if not orders:
                print_info("Emir geçmişi bulunamadı")
                return

            # Create orders table
            table = Table(
                title=f"Emir Geçmişi ({len(orders)} emir)",
                box=box.ROUNDED,
                show_header=True,
                header_style="bold yellow"
            )

            table.add_column("Emir ID", style="dim", width=20)
            table.add_column("Sembol", style="cyan")
            table.add_column("Yön", justify="center")
            table.add_column("Tip", justify="center")
            table.add_column("Durum", justify="center")
            table.add_column("Miktar", justify="right")
            table.add_column("Fiyat", justify="right", style="yellow")
            table.add_column("Gerçekleşen", justify="right", style="green")
            table.add_column("Tarih", style="dim")

            for order in orders:
                order_id = order.get("orderId", "")[:18] + "..." if len(order.get("orderId", "")) > 20 else order.get("orderId", "N/A")
                symbol = order.get("symbol", "N/A")

                # Side
                side = order.get("side", "")
                side_str = "[green]ALIŞ[/green]" if side == "BUY" else "[red]SATIŞ[/red]" if side == "SELL" else side

                # Order type
                order_type = order.get("orderType", "N/A")

                # Status
                status_val = order.get("status", "N/A")
                if status_val == "FILLED":
                    status_str = "[green]GERÇEKLEŞEN[/green]"
                elif status_val == "CANCELLED":
                    status_str = "[red]İPTAL[/red]"
                elif status_val == "PENDING":
                    status_str = "[yellow]BEKLEYEN[/yellow]"
                else:
                    status_str = status_val

                quantity = order.get("quantity", 0)
                price = order.get("price", 0)
                filled_qty = order.get("filledQuantity", 0)

                # Format date
                created_at = order.get("createdAt", "")
                try:
                    if created_at:
                        from datetime import datetime
                        dt = datetime.fromisoformat(created_at.replace("Z", "+00:00"))
                        date_str = dt.strftime("%Y-%m-%d %H:%M")
                    else:
                        date_str = "N/A"
                except:
                    date_str = created_at[:16] if len(created_at) >= 16 else created_at

                table.add_row(
                    order_id,
                    symbol,
                    side_str,
                    order_type,
                    status_str,
                    str(quantity),
                    format_currency(price) if price else "-",
                    str(filled_qty) if filled_qty else "-",
                    date_str
                )

            console.print()
            console.print(table)

            # Show pagination info
            total_elements = response.get("totalElements", len(orders))
            total_pages = response.get("totalPages", 1)
            console.print(f"\n[dim]Toplam {total_elements} emir, {total_pages} sayfa[/dim]")
            console.print()

        except APIError as e:
            if e.status_code == 403:
                print_error("Emir geçmişine erişim yetkiniz yok")
            else:
                print_error(f"Emir geçmişi alınamadı: {e.message}")
        except Exception as e:
            print_error(f"Beklenmeyen hata: {str(e)}")

    def broker_menu(self) -> None:
        """Interactive broker operations menu."""
        while True:
            console.print()
            console.print(Panel.fit(
                "[bold yellow]Broker İşlemleri[/bold yellow]\n\n"
                "1. Hesap Bilgileri\n"
                "2. Açık Pozisyonlar\n"
                "3. AlgoLab Durumu\n"
                "4. WebSocket Testi\n"
                "5. Real-Time Tick Data (Tek Sembol)\n"
                "6. Multi-Symbol Monitor\n"
                "7. Order Book (Emir Defteri)\n"
                "8. Trade Stream (İşlem Akışı)\n"
                "9. ⚠️  Emir Gönder (YENİ!)\n"
                "10. ⚠️  Emir İptal (YENİ!)\n"
                "11. ⚠️  Emir Güncelle (YENİ!)\n"
                "12. 📋 Emir Geçmişi (YENİ!)\n"
                "13. Geri Dön",
                border_style="yellow"
            ))

            choice = Prompt.ask(
                "\n[yellow]Seçiminiz[/yellow]",
                choices=["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"],
                default="13"
            )

            if choice == "1":
                self.view_account_info()
            elif choice == "2":
                self.view_positions()
            elif choice == "3":
                self.view_algolab_status()
            elif choice == "4":
                self.test_websocket_connection()
            elif choice == "5":
                self.view_realtime_ticks()
            elif choice == "6":
                self.view_multi_symbol_ticks()
            elif choice == "7":
                self.view_order_book()
            elif choice == "8":
                self.view_trade_stream()
            elif choice == "9":
                self.send_order()
            elif choice == "10":
                self.cancel_order()
            elif choice == "11":
                self.modify_order()
            elif choice == "12":
                self.view_order_history()
            elif choice == "13":
                break
