"""
Authentication flows for BIST CLI client.
Handles user login and AlgoLab broker authentication with OTP.
"""

from typing import Dict, Any, Optional

from rich.console import Console
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
import questionary

from .api_client import APIClient, APIError
from .utils import (
    print_success,
    print_error,
    print_warning,
    print_info,
    save_user_session,
    get_stored_token,
    store_token
)


console = Console()


class AuthenticationManager:
    """Manages authentication flows."""

    def __init__(self, api_client: APIClient):
        """
        Initialize authentication manager.

        Args:
            api_client: API client instance
        """
        self.api = api_client
        self.current_user: Optional[Dict[str, Any]] = None
        self.algolab_authenticated = False

    def login_flow(self) -> bool:
        """
        Interactive user login flow.

        Returns:
            True if login successful
        """
        console.print()
        console.print(Panel.fit(
            "[bold cyan]BIST Trading Platform[/bold cyan]\n"
            "Kullanıcı Girişi",
            border_style="cyan"
        ))
        console.print()

        # Check if already authenticated
        if self.api.is_authenticated():
            if Confirm.ask("Zaten giriş yapmışsınız. Yeni bir oturum başlatmak ister misiniz?"):
                self.api.logout()
            else:
                return True

        # Get credentials
        username = Prompt.ask("[cyan]Kullanıcı Adı[/cyan]")
        password = Prompt.ask("[cyan]Şifre[/cyan]", password=True)

        if not username or not password:
            print_error("Kullanıcı adı ve şifre gerekli")
            return False

        # Attempt login
        try:
            console.print("\n[dim]Giriş yapılıyor...[/dim]")
            response = self.api.login(username, password)

            # Store user info
            if "user" in response:
                self.current_user = response["user"]
                save_user_session(self.current_user)

            # Display success
            console.print()
            print_success("Giriş başarılı!")

            if self.current_user:
                user_info = (
                    f"[bold]Hoş geldiniz, {self.current_user.get('firstName', '')} "
                    f"{self.current_user.get('lastName', '')}![/bold]\n"
                    f"Kullanıcı Adı: {self.current_user.get('username', 'N/A')}\n"
                    f"Email: {self.current_user.get('email', 'N/A')}\n"
                    f"Rol: {self.current_user.get('role', 'USER')}"
                )
                console.print(Panel(user_info, border_style="green", title="Kullanıcı Bilgileri"))

            console.print()
            return True

        except APIError as e:
            console.print()
            print_error(f"Giriş başarısız: {e.message}")

            if e.status_code == 401:
                print_warning("Kullanıcı adı veya şifre hatalı")
            elif e.status_code == 403:
                print_warning("Hesabınız kilitlenmiş olabilir")

            return False
        except Exception as e:
            console.print()
            print_error(f"Beklenmeyen hata: {str(e)}")
            return False

    def algolab_auth_flow(self) -> bool:
        """
        Interactive AlgoLab broker authentication flow with OTP.

        Returns:
            True if authentication successful
        """
        console.print()
        console.print(Panel.fit(
            "[bold yellow]AlgoLab Broker Entegrasyonu[/bold yellow]\n"
            "İki Faktörlü Kimlik Doğrulama",
            border_style="yellow"
        ))
        console.print()

        # Check if user is logged in
        if not self.api.is_authenticated():
            print_error("Önce platforma giriş yapmalısınız")
            return False

        # Get AlgoLab credentials
        print_info("AlgoLab broker hesap bilgilerinizi girin")
        console.print()

        broker_username = Prompt.ask("[yellow]AlgoLab Kullanıcı Adı[/yellow]")
        broker_password = Prompt.ask("[yellow]AlgoLab Şifre[/yellow]", password=True)

        if not broker_username or not broker_password:
            print_error("Kullanıcı adı ve şifre gerekli")
            return False

        try:
            # Step 1: Initial login (triggers OTP SMS)
            console.print("\n[dim]AlgoLab'a bağlanılıyor ve OTP gönderiliyor...[/dim]")

            login_response = self.api.post(
                "/api/v1/broker/auth/login",
                {
                    "username": broker_username,
                    "password": broker_password
                }
            )

            # Check if SMS was sent (backend returns smsSent, not otpSent)
            if login_response.get("smsSent") or login_response.get("success"):
                console.print()
                print_success(login_response.get("message", "OTP kodu telefonunuza gönderildi"))
                console.print()

                # Step 2: Get OTP from user
                otp_code = Prompt.ask(
                    "[yellow]SMS ile gelen doğrulama kodunu girin (4-8 hane)[/yellow]",
                    default=""
                )

                if not otp_code or len(otp_code) < 4:
                    print_error("Geçersiz OTP kodu (en az 4 hane)")
                    return False

                # Step 3: Verify OTP (backend only needs otpCode, not username)
                console.print("\n[dim]OTP doğrulanıyor...[/dim]")

                verify_response = self.api.post(
                    "/api/v1/broker/auth/verify-otp",
                    {
                        "otpCode": otp_code
                    }
                )

                # Check verification result
                if verify_response.get("authenticated") and verify_response.get("success"):
                    console.print()
                    success_msg = verify_response.get("message", "AlgoLab kimlik doğrulama başarılı!")
                    print_success(success_msg)

                    # Display session info
                    session_info_lines = []

                    # Check for session expiration (backend returns sessionExpiresAt)
                    expires_at = verify_response.get("sessionExpiresAt")
                    if expires_at:
                        session_info_lines.append(f"[yellow]Oturum Geçerlilik:[/yellow] {expires_at}")

                    # Add message if present
                    if verify_response.get("message"):
                        session_info_lines.append(f"[green]{verify_response['message']}[/green]")

                    if session_info_lines:
                        console.print()
                        console.print(Panel(
                            "\n".join(session_info_lines),
                            border_style="green",
                            title="✓ AlgoLab Bağlantısı Başarılı"
                        ))

                    self.algolab_authenticated = True
                    console.print()
                    return True
                else:
                    error_msg = verify_response.get("message", "OTP doğrulama başarısız")
                    print_error(error_msg)
                    return False

            else:
                # No OTP required (unusual but handle it)
                print_success("AlgoLab kimlik doğrulama başarılı (OTP gerekmedi)")
                self.algolab_authenticated = True
                return True

        except APIError as e:
            console.print()
            print_error(f"AlgoLab kimlik doğrulama hatası: {e.message}")

            if e.status_code == 401:
                print_warning("BIST Platform authentication gerekli veya süresi dolmuş")
            elif e.status_code == 400:
                print_warning("AlgoLab kullanıcı adı/şifre hatalı veya geçersiz OTP")
            elif e.status_code == 428:
                print_warning("Önce /login ile SMS kodu almalısınız")
            elif e.status_code == 503:
                print_warning("AlgoLab servisi şu anda kullanılamıyor")
            else:
                print_warning(f"HTTP {e.status_code}: {e.message}")

            return False
        except Exception as e:
            console.print()
            print_error(f"Beklenmeyen hata: {str(e)}")

            # Show traceback only in debug/verbose mode (check environment or flag)
            import os
            if os.getenv("DEBUG") or os.getenv("VERBOSE"):
                import traceback
                console.print("\n[dim]Traceback:[/dim]")
                console.print(f"[dim]{traceback.format_exc()}[/dim]")

            return False

    def check_algolab_status(self) -> Dict[str, Any]:
        """
        Check AlgoLab authentication status.

        Returns:
            Status information
        """
        try:
            response = self.api.get("/api/v1/broker/auth/status")
            self.algolab_authenticated = response.get("authenticated", False)
            return response
        except Exception:
            self.algolab_authenticated = False
            return {"authenticated": False}

    def logout(self) -> None:
        """Logout from platform and clear session."""
        try:
            self.api.logout()
            self.current_user = None
            self.algolab_authenticated = False
            print_success("Çıkış yapıldı")
        except Exception as e:
            print_error(f"Çıkış hatası: {str(e)}")

    def get_user_profile(self) -> Optional[Dict[str, Any]]:
        """
        Get current user profile.

        Returns:
            User profile data
        """
        try:
            profile = self.api.get("/api/v1/users/profile")
            self.current_user = profile
            save_user_session(self.current_user)
            return profile
        except APIError as e:
            print_error(f"Profil alınamadı: {e.message}")
            return None

    def is_logged_in(self) -> bool:
        """Check if user is logged in."""
        return self.api.is_authenticated()

    def is_algolab_authenticated(self) -> bool:
        """Check if AlgoLab is authenticated."""
        return self.algolab_authenticated
