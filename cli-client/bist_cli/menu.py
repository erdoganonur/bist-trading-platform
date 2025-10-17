"""
Main menu and UI for BIST CLI client.
Provides interactive navigation and user interface.
"""

from typing import Optional

from rich.console import Console
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich.table import Table
from rich import box

from .api_client import APIClient
from .auth import AuthenticationManager
from .market_data import MarketDataManager
from .broker import BrokerManager
from .utils import print_success, print_error, print_info, load_user_session


console = Console()


class MainMenu:
    """Main menu controller."""

    def __init__(self, debug: bool = False):
        """Initialize main menu with optional debug mode."""
        self.api = APIClient(debug=debug)
        self.auth = AuthenticationManager(self.api)
        self.market_data = MarketDataManager(self.api)
        self.broker = BrokerManager(self.api)

    def show_welcome_banner(self) -> None:
        """Display welcome banner."""
        banner = """
[bold cyan]
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║     🚀 BIST TRADING PLATFORM - CLI CLIENT 🚀             ║
║                                                           ║
║     Borsa İstanbul İnteraktif Ticaret Platformu          ║
║     Version 1.0.0                                         ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
[/bold cyan]
"""
        console.print(banner)

    def show_main_menu(self) -> str:
        """
        Display main menu and get user choice.

        Returns:
            User's menu choice
        """
        # Check authentication status
        auth_status = "✓ Giriş Yapıldı" if self.auth.is_logged_in() else "✗ Giriş Yapılmadı"
        algolab_status = "✓ Bağlı" if self.auth.is_algolab_authenticated() else "✗ Bağlı Değil"

        # Get current user info
        user_info = ""
        if self.auth.current_user:
            username = self.auth.current_user.get("username", "Unknown")
            user_info = f"\n[dim]Kullanıcı: {username}[/dim]"

        menu_text = f"""[bold cyan]ANA MENÜ[/bold cyan]
{user_info}

[bold]Durum:[/bold]
  Platform: [{'green' if self.auth.is_logged_in() else 'red'}]{auth_status}[/]
  AlgoLab:  [{'green' if self.auth.is_algolab_authenticated() else 'red'}]{algolab_status}[/]

[bold]Seçenekler:[/bold]

  [cyan]1.[/cyan]  📊 Piyasa Verileri
  [cyan]2.[/cyan]  💼 Broker İşlemleri
  [cyan]3.[/cyan]  👤 Profil Bilgileri
  [cyan]4.[/cyan]  🔐 AlgoLab Bağlantısı
  [cyan]5.[/cyan]  ⚙️  Ayarlar
  [cyan]6.[/cyan]  🚪 Çıkış
"""

        console.print()
        console.print(Panel.fit(menu_text, border_style="cyan"))

        choice = Prompt.ask(
            "\n[cyan]Seçiminiz[/cyan]",
            choices=["1", "2", "3", "4", "5", "6"],
            default="6"
        )

        return choice

    def handle_market_data(self) -> None:
        """Handle market data menu."""
        if not self.auth.is_logged_in():
            print_error("Piyasa verilerine erişmek için giriş yapmalısınız")
            return

        self.market_data.market_data_menu()

    def handle_broker_operations(self) -> None:
        """Handle broker operations menu."""
        if not self.auth.is_logged_in():
            print_error("Broker işlemleri için giriş yapmalısınız")
            return

        if not self.auth.is_algolab_authenticated():
            print_info("AlgoLab broker bağlantısı gerekli")
            if Confirm.ask("AlgoLab kimlik doğrulaması yapmak ister misiniz?"):
                if self.auth.algolab_auth_flow():
                    self.broker.broker_menu()
            return

        self.broker.broker_menu()

    def handle_profile(self) -> None:
        """Handle user profile display."""
        if not self.auth.is_logged_in():
            print_error("Profil bilgilerine erişmek için giriş yapmalısınız")
            return

        console.print("\n[dim]Profil bilgileri yükleniyor...[/dim]")

        profile = self.auth.get_user_profile()

        if profile:
            console.print()
            console.print(Panel.fit(
                "[bold cyan]Kullanıcı Profili[/bold cyan]",
                border_style="cyan"
            ))

            # Personal info
            personal_info = (
                f"[yellow]Ad Soyad:[/yellow] {profile.get('firstName', '')} {profile.get('lastName', '')}\n"
                f"[yellow]Kullanıcı Adı:[/yellow] {profile.get('username', 'N/A')}\n"
                f"[yellow]Email:[/yellow] {profile.get('email', 'N/A')}\n"
                f"[yellow]Telefon:[/yellow] {profile.get('phoneNumber', 'N/A')}"
            )

            # Account info
            account_info = (
                f"[cyan]TC Kimlik No:[/cyan] {profile.get('tcKimlikNo', 'N/A')}\n"
                f"[cyan]Rol:[/cyan] {profile.get('role', 'USER')}\n"
                f"[cyan]Durum:[/cyan] {profile.get('accountStatus', 'N/A')}\n"
                f"[cyan]Kayıt Tarihi:[/cyan] {profile.get('createdAt', 'N/A')[:10]}"
            )

            # Verification status
            email_verified = "✓" if profile.get("emailVerified") else "✗"
            phone_verified = "✓" if profile.get("phoneVerified") else "✗"
            kyc_verified = "✓" if profile.get("kycVerified") else "✗"

            verification = (
                f"Email Doğrulama: [{('green' if profile.get('emailVerified') else 'red')}]{email_verified}[/]\n"
                f"Telefon Doğrulama: [{('green' if profile.get('phoneVerified') else 'red')}]{phone_verified}[/]\n"
                f"KYC Doğrulama: [{('green' if profile.get('kycVerified') else 'red')}]{kyc_verified}[/]"
            )

            console.print()
            console.print(Panel(personal_info, title="Kişisel Bilgiler", border_style="yellow"))
            console.print(Panel(account_info, title="Hesap Bilgileri", border_style="cyan"))
            console.print(Panel(verification, title="Doğrulama Durumu", border_style="magenta"))
            console.print()

    def handle_algolab_connection(self) -> None:
        """Handle AlgoLab authentication."""
        if not self.auth.is_logged_in():
            print_error("AlgoLab bağlantısı için önce platforma giriş yapmalısınız")
            return

        # Check current status
        status = self.auth.check_algolab_status()

        if status.get("authenticated"):
            console.print()
            print_success("AlgoLab zaten bağlı")
            self.broker.view_algolab_status()

            if not Confirm.ask("Yeni bir bağlantı başlatmak ister misiniz?"):
                return

        # Start authentication flow
        self.auth.algolab_auth_flow()

    def handle_settings(self) -> None:
        """Handle settings menu."""
        console.print()
        console.print(Panel.fit(
            "[bold magenta]Ayarlar[/bold magenta]\n\n"
            "1. Bağlantı Testi\n"
            "2. Token Temizle\n"
            "3. Oturum Bilgileri\n"
            "4. Geri Dön",
            border_style="magenta"
        ))

        choice = Prompt.ask(
            "\n[magenta]Seçiminiz[/magenta]",
            choices=["1", "2", "3", "4"],
            default="4"
        )

        if choice == "1":
            self._test_connection()
        elif choice == "2":
            self._clear_tokens()
        elif choice == "3":
            self._show_session_info()

    def _test_connection(self) -> None:
        """Test API connection."""
        console.print("\n[dim]API bağlantısı test ediliyor...[/dim]")

        if self.api.test_connection():
            print_success("API bağlantısı başarılı")
            console.print(f"[dim]Base URL: {self.api.base_url}[/dim]")
        else:
            print_error("API'ye bağlanılamadı")
            console.print(f"[dim]Base URL: {self.api.base_url}[/dim]")
            print_info("BIST Trading Platform'un çalıştığından emin olun")

    def _clear_tokens(self) -> None:
        """Clear stored tokens."""
        if Confirm.ask("\nTüm kayıtlı token'lar silinecek. Devam edilsin mi?"):
            from .utils import clear_tokens
            clear_tokens()
            self.auth.logout()

    def _show_session_info(self) -> None:
        """Show session information."""
        console.print()

        info_lines = []

        # API info
        info_lines.append(f"[yellow]API Base URL:[/yellow] {self.api.base_url}")
        info_lines.append(
            f"[yellow]Kimlik Doğrulama:[/yellow] "
            f"[{'green' if self.api.is_authenticated() else 'red'}]"
            f"{'Aktif' if self.api.is_authenticated() else 'Pasif'}[/]"
        )

        # User session
        session = load_user_session()
        if session:
            info_lines.append(f"[yellow]Oturum:[/yellow] Kayıtlı")
            info_lines.append(f"[yellow]Kullanıcı:[/yellow] {session.get('username', 'N/A')}")
        else:
            info_lines.append(f"[yellow]Oturum:[/yellow] Yok")

        console.print(Panel(
            "\n".join(info_lines),
            title="Oturum Bilgileri",
            border_style="cyan"
        ))
        console.print()

    def run(self) -> None:
        """Run the main menu loop."""
        # Show welcome banner
        self.show_welcome_banner()

        # Test API connection
        if not self.api.test_connection():
            print_error("API'ye bağlanılamadı. Platform çalışıyor mu kontrol edin.")
            console.print(f"[dim]Base URL: {self.api.base_url}[/dim]\n")

        # Login flow
        if not self.auth.is_logged_in():
            if not self.auth.login_flow():
                print_error("Giriş başarısız. Program sonlandırılıyor.")
                return

        # Main menu loop
        while True:
            try:
                choice = self.show_main_menu()

                if choice == "1":
                    self.handle_market_data()
                elif choice == "2":
                    self.handle_broker_operations()
                elif choice == "3":
                    self.handle_profile()
                elif choice == "4":
                    self.handle_algolab_connection()
                elif choice == "5":
                    self.handle_settings()
                elif choice == "6":
                    # Exit
                    console.print()
                    if Confirm.ask("Çıkmak istediğinizden emin misiniz?"):
                        console.print()
                        print_success("Güle güle!")
                        console.print()
                        break

            except KeyboardInterrupt:
                console.print("\n")
                if Confirm.ask("Çıkmak istediğinizden emin misiniz?"):
                    console.print()
                    print_success("Güle güle!")
                    console.print()
                    break
                continue
            except Exception as e:
                print_error(f"Beklenmeyen hata: {str(e)}")
                console.print("[dim]Ana menüye dönülüyor...[/dim]")
