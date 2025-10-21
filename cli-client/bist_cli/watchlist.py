"""
Watchlist management for BIST CLI client.
Allows users to save and manage their favorite symbols.
"""

import json
from pathlib import Path
from typing import List, Dict, Optional

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich import box

from .config import get_app_dir
from .utils import print_success, print_error, print_info, print_warning
from .logger import get_logger

console = Console()
logger = get_logger(__name__)


class Watchlist:
    """Manage user watchlists for favorite symbols."""

    def __init__(self):
        """Initialize watchlist manager."""
        self.config_path = get_app_dir() / "watchlists.json"
        self.watchlists = self.load()

    def load(self) -> Dict[str, List[str]]:
        """
        Load watchlists from file.

        Returns:
            Dictionary of watchlist name to symbols
        """
        if self.config_path.exists():
            try:
                with open(self.config_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    logger.info(f"Loaded {len(data)} watchlists from {self.config_path}")
                    return data
            except Exception as e:
                logger.error(f"Failed to load watchlists: {e}")
                print_error(f"Watchlist yüklenemedi: {str(e)}")
                return {"default": []}
        else:
            logger.info("No watchlist file found, creating default")
            return {"default": []}

    def save(self) -> bool:
        """
        Save watchlists to file.

        Returns:
            True if successful, False otherwise
        """
        try:
            self.config_path.parent.mkdir(parents=True, exist_ok=True)
            with open(self.config_path, 'w', encoding='utf-8') as f:
                json.dump(self.watchlists, f, indent=2, ensure_ascii=False)
            logger.info(f"Saved watchlists to {self.config_path}")
            return True
        except Exception as e:
            logger.error(f"Failed to save watchlists: {e}")
            print_error(f"Watchlist kaydedilemedi: {str(e)}")
            return False

    def add_symbol(self, symbol: str, list_name: str = "default") -> bool:
        """
        Add symbol to watchlist.

        Args:
            symbol: Symbol code to add
            list_name: Watchlist name

        Returns:
            True if added, False if already exists
        """
        symbol = symbol.upper().strip()

        if list_name not in self.watchlists:
            self.watchlists[list_name] = []

        if symbol in self.watchlists[list_name]:
            return False

        self.watchlists[list_name].append(symbol)
        self.save()
        logger.info(f"Added {symbol} to watchlist '{list_name}'")
        return True

    def remove_symbol(self, symbol: str, list_name: str = "default") -> bool:
        """
        Remove symbol from watchlist.

        Args:
            symbol: Symbol code to remove
            list_name: Watchlist name

        Returns:
            True if removed, False if not found
        """
        symbol = symbol.upper().strip()

        if list_name in self.watchlists and symbol in self.watchlists[list_name]:
            self.watchlists[list_name].remove(symbol)
            self.save()
            logger.info(f"Removed {symbol} from watchlist '{list_name}'")
            return True

        return False

    def get_symbols(self, list_name: str = "default") -> List[str]:
        """
        Get symbols in watchlist.

        Args:
            list_name: Watchlist name

        Returns:
            List of symbols
        """
        return self.watchlists.get(list_name, [])

    def get_all_lists(self) -> List[str]:
        """
        Get all watchlist names.

        Returns:
            List of watchlist names
        """
        return list(self.watchlists.keys())

    def create_list(self, list_name: str) -> bool:
        """
        Create a new watchlist.

        Args:
            list_name: Name for the new watchlist

        Returns:
            True if created, False if already exists
        """
        list_name = list_name.strip()

        if list_name in self.watchlists:
            return False

        self.watchlists[list_name] = []
        self.save()
        logger.info(f"Created watchlist '{list_name}'")
        return True

    def delete_list(self, list_name: str) -> bool:
        """
        Delete a watchlist.

        Args:
            list_name: Watchlist name to delete

        Returns:
            True if deleted, False if not found or is default
        """
        if list_name == "default":
            print_warning("Default watchlist silinemez")
            return False

        if list_name in self.watchlists:
            del self.watchlists[list_name]
            self.save()
            logger.info(f"Deleted watchlist '{list_name}'")
            return True

        return False

    def rename_list(self, old_name: str, new_name: str) -> bool:
        """
        Rename a watchlist.

        Args:
            old_name: Current watchlist name
            new_name: New watchlist name

        Returns:
            True if renamed, False otherwise
        """
        if old_name not in self.watchlists:
            return False

        if new_name in self.watchlists:
            print_error("Bu isimde bir watchlist zaten var")
            return False

        self.watchlists[new_name] = self.watchlists.pop(old_name)
        self.save()
        logger.info(f"Renamed watchlist '{old_name}' to '{new_name}'")
        return True

    def view_all_lists(self) -> None:
        """Display all watchlists in a table."""
        console.print()
        console.print(Panel.fit(
            "[bold yellow]Watchlist Yönetimi[/bold yellow]",
            border_style="yellow"
        ))

        table = Table(
            title="Tüm Watchlist'ler",
            box=box.ROUNDED,
            show_header=True,
            header_style="bold yellow"
        )

        table.add_column("Liste Adı", style="cyan")
        table.add_column("Sembol Sayısı", justify="right", style="yellow")
        table.add_column("Semboller", style="dim")

        for list_name, symbols in self.watchlists.items():
            symbol_count = len(symbols)
            symbol_preview = ", ".join(symbols[:5])
            if symbol_count > 5:
                symbol_preview += f" ... (+{symbol_count - 5} daha)"

            table.add_row(
                list_name,
                str(symbol_count),
                symbol_preview or "[dim]Boş[/dim]"
            )

        console.print()
        console.print(table)
        console.print()

    def view_list(self, list_name: str = "default") -> None:
        """
        Display symbols in a specific watchlist.

        Args:
            list_name: Watchlist name to display
        """
        symbols = self.get_symbols(list_name)

        console.print()
        console.print(Panel.fit(
            f"[bold yellow]Watchlist: {list_name}[/bold yellow]",
            border_style="yellow"
        ))

        if not symbols:
            print_info(f"'{list_name}' watchlist'i boş")
            print_info("Yeni sembol eklemek için 'Watchlist Yönetimi → Sembol Ekle' kullanın")
            return

        table = Table(
            title=f"{list_name} - {len(symbols)} Sembol",
            box=box.ROUNDED,
            show_header=True,
            header_style="bold yellow"
        )

        table.add_column("#", style="dim", justify="right", width=5)
        table.add_column("Sembol", style="cyan", width=15)

        for idx, symbol in enumerate(symbols, 1):
            table.add_row(str(idx), symbol)

        console.print()
        console.print(table)
        console.print()

    def interactive_menu(self) -> None:
        """Interactive watchlist management menu."""
        while True:
            console.print()
            console.print(Panel.fit(
                "[bold yellow]Watchlist Yönetimi[/bold yellow]\n\n"
                "1. Watchlist Göster\n"
                "2. Tüm Watchlist'leri Göster\n"
                "3. Sembol Ekle\n"
                "4. Sembol Çıkar\n"
                "5. Yeni Watchlist Oluştur\n"
                "6. Watchlist Sil\n"
                "7. Watchlist Adını Değiştir\n"
                "8. Geri Dön",
                border_style="yellow"
            ))

            choice = Prompt.ask(
                "\n[yellow]Seçiminiz[/yellow]",
                choices=["1", "2", "3", "4", "5", "6", "7", "8"],
                default="8"
            )

            if choice == "1":
                self._menu_view_list()
            elif choice == "2":
                self.view_all_lists()
            elif choice == "3":
                self._menu_add_symbol()
            elif choice == "4":
                self._menu_remove_symbol()
            elif choice == "5":
                self._menu_create_list()
            elif choice == "6":
                self._menu_delete_list()
            elif choice == "7":
                self._menu_rename_list()
            elif choice == "8":
                break

    def _menu_view_list(self) -> None:
        """Menu: View a specific watchlist."""
        lists = self.get_all_lists()

        if not lists:
            print_info("Henüz watchlist oluşturulmamış")
            return

        console.print("\n[bold]Mevcut Watchlist'ler:[/bold]")
        for idx, list_name in enumerate(lists, 1):
            console.print(f"{idx}. {list_name}")

        list_name = Prompt.ask(
            "\n[yellow]Watchlist adı[/yellow]",
            default="default"
        )

        if list_name not in self.watchlists:
            print_error(f"'{list_name}' bulunamadı")
            return

        self.view_list(list_name)

    def _menu_add_symbol(self) -> None:
        """Menu: Add symbol to watchlist."""
        symbol = Prompt.ask("\n[yellow]Sembol kodu[/yellow]").upper().strip()

        lists = self.get_all_lists()
        console.print("\n[bold]Mevcut Watchlist'ler:[/bold]")
        for list_name in lists:
            console.print(f"  - {list_name}")

        list_name = Prompt.ask(
            "\n[yellow]Watchlist adı[/yellow]",
            default="default"
        )

        if self.add_symbol(symbol, list_name):
            print_success(f"✓ {symbol} '{list_name}' listesine eklendi")
        else:
            print_warning(f"{symbol} zaten '{list_name}' listesinde")

    def _menu_remove_symbol(self) -> None:
        """Menu: Remove symbol from watchlist."""
        lists = self.get_all_lists()

        console.print("\n[bold]Mevcut Watchlist'ler:[/bold]")
        for list_name in lists:
            symbols = self.get_symbols(list_name)
            console.print(f"  - {list_name}: {', '.join(symbols) if symbols else '[dim]Boş[/dim]'}")

        list_name = Prompt.ask(
            "\n[yellow]Watchlist adı[/yellow]",
            default="default"
        )

        symbols = self.get_symbols(list_name)
        if not symbols:
            print_info(f"'{list_name}' listesi boş")
            return

        symbol = Prompt.ask("\n[yellow]Çıkarılacak sembol[/yellow]").upper().strip()

        if self.remove_symbol(symbol, list_name):
            print_success(f"✓ {symbol} '{list_name}' listesinden çıkarıldı")
        else:
            print_error(f"{symbol} '{list_name}' listesinde bulunamadı")

    def _menu_create_list(self) -> None:
        """Menu: Create new watchlist."""
        list_name = Prompt.ask("\n[yellow]Yeni watchlist adı[/yellow]").strip()

        if self.create_list(list_name):
            print_success(f"✓ '{list_name}' watchlist'i oluşturuldu")
        else:
            print_error(f"'{list_name}' zaten var")

    def _menu_delete_list(self) -> None:
        """Menu: Delete a watchlist."""
        lists = [l for l in self.get_all_lists() if l != "default"]

        if not lists:
            print_info("Silinebilecek watchlist yok (default silinemez)")
            return

        console.print("\n[bold]Silinebilir Watchlist'ler:[/bold]")
        for list_name in lists:
            console.print(f"  - {list_name}")

        list_name = Prompt.ask("\n[yellow]Silinecek watchlist[/yellow]").strip()

        if Confirm.ask(f"\n[red]'{list_name}' watchlist'ini silmek istediğinizden emin misiniz?[/red]"):
            if self.delete_list(list_name):
                print_success(f"✓ '{list_name}' silindi")
            else:
                print_error(f"'{list_name}' silinemedi")

    def _menu_rename_list(self) -> None:
        """Menu: Rename a watchlist."""
        lists = self.get_all_lists()

        console.print("\n[bold]Mevcut Watchlist'ler:[/bold]")
        for list_name in lists:
            console.print(f"  - {list_name}")

        old_name = Prompt.ask("\n[yellow]Değiştirilecek watchlist[/yellow]").strip()
        new_name = Prompt.ask("[yellow]Yeni ad[/yellow]").strip()

        if self.rename_list(old_name, new_name):
            print_success(f"✓ '{old_name}' → '{new_name}' olarak değiştirildi")
        else:
            print_error(f"Watchlist adı değiştirilemedi")