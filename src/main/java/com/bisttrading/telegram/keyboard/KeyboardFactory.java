package com.bisttrading.telegram.keyboard;

import com.bisttrading.entity.trading.Order;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating Telegram inline keyboards.
 * Provides pre-built keyboards for common use cases.
 */
public class KeyboardFactory {

    /**
     * Main menu keyboard
     */
    public static InlineKeyboardMarkup createMainMenuKeyboard(boolean isLoggedIn) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        if (isLoggedIn) {
            // Logged in menu
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("📊 Piyasa Verileri", "menu:market"),
                createButton("💼 Broker", "menu:broker")
            ));
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("📋 Emirler", "menu:orders"),
                createButton("⭐ Watchlist", "menu:watchlist")
            ));
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("👤 Profil", "menu:profile"),
                createButton("⚙️ Ayarlar", "menu:settings")
            ));
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("🚪 Çıkış Yap", "menu:logout")
            ));
        } else {
            // Not logged in menu
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("🔐 Giriş Yap", "login")
            ));
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("❓ Yardım", "help")
            ));
        }

        return builder.build();
    }

    /**
     * Market data menu keyboard
     */
    public static InlineKeyboardMarkup createMarketDataKeyboard() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("🔍 Sembol Ara", "market:search"),
                createButton("📈 Hisse Fiyatı", "market:quote")
            ))
            .keyboardRow(new InlineKeyboardRow(
                createButton("📊 Sektörler", "market:sectors"),
                createButton("🔥 Popüler", "market:trending")
            ))
            .keyboardRow(new InlineKeyboardRow(
                createButton("🔙 Ana Menü", "menu:main")
            ))
            .build();
    }

    /**
     * Broker menu keyboard
     */
    public static InlineKeyboardMarkup createBrokerKeyboard(boolean algoLabConnected) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        if (algoLabConnected) {
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("💰 Hesap Bilgileri", "broker:account"),
                createButton("📊 Pozisyonlar", "broker:positions")
            ));
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("✅ AlgoLab Durumu", "broker:status")
            ));
        } else {
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("🔗 AlgoLab Bağlan", "broker:connect")
            ));
        }

        builder.keyboardRow(new InlineKeyboardRow(
            createButton("🔙 Ana Menü", "menu:main")
        ));

        return builder.build();
    }

    /**
     * Confirmation keyboard (Yes/No)
     */
    public static InlineKeyboardMarkup createConfirmationKeyboard(String action) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("✅ Evet", "confirm:" + action + ":yes"),
                createButton("❌ Hayır", "confirm:" + action + ":no")
            ))
            .build();
    }

    /**
     * Back button keyboard
     */
    public static InlineKeyboardMarkup createBackButton(String backTo) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("🔙 Geri", backTo)
            ))
            .build();
    }

    /**
     * Orders menu keyboard
     */
    public static InlineKeyboardMarkup createOrdersMenuKeyboard() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("📋 Bekleyen Emirler", "orders:pending"),
                createButton("➕ Yeni Emir", "orders:create")
            ))
            .keyboardRow(new InlineKeyboardRow(
                createButton("🔙 Ana Menü", "menu:main")
            ))
            .build();
    }

    /**
     * Order list keyboard with cancel and modify buttons
     */
    public static InlineKeyboardMarkup createOrderListKeyboard(List<Order> orders) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            String symbolCode = order.getSymbol() != null ? order.getSymbol().getSymbol() : "N/A";

            // Create two buttons per order: Modify and Cancel
            builder.keyboardRow(new InlineKeyboardRow(
                createButton(
                    String.format("✏️ Düzenle #%d", i + 1),
                    "orders:modify:" + order.getId()
                ),
                createButton(
                    String.format("❌ İptal #%d", i + 1),
                    "orders:cancel:" + order.getId()
                )
            ));
        }

        // Add back button
        builder.keyboardRow(new InlineKeyboardRow(
            createButton("🔙 Ana Menü", "menu:main")
        ));

        return builder.build();
    }

    /**
     * Order side selection keyboard (Buy/Sell)
     */
    public static InlineKeyboardMarkup createOrderSideKeyboard() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("🟢 ALIS (BUY)", "orders:side:BUY"),
                createButton("🔴 SATIŞ (SELL)", "orders:side:SELL")
            ))
            .keyboardRow(new InlineKeyboardRow(
                createButton("❌ İptal", "menu:orders")
            ))
            .build();
    }

    /**
     * Order type selection keyboard (Market/Limit)
     */
    public static InlineKeyboardMarkup createOrderTypeKeyboard() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("💰 PIYASA (MARKET)", "orders:type:MARKET"),
                createButton("📊 LIMIT", "orders:type:LIMIT")
            ))
            .keyboardRow(new InlineKeyboardRow(
                createButton("❌ İptal", "menu:orders")
            ))
            .build();
    }

    /**
     * Position action keyboard (Buy/Sell buttons for a specific symbol)
     */
    public static InlineKeyboardMarkup createPositionActionKeyboard(String symbol) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("🟢 AL", "position:buy:" + symbol),
                createButton("🔴 SAT", "position:sell:" + symbol)
            ))
            .build();
    }

    /**
     * Positions list keyboard with action buttons for each position
     */
    public static InlineKeyboardMarkup createPositionsKeyboard(java.util.List<String> symbols) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

        // Add buy/sell buttons for each symbol
        for (String symbol : symbols) {
            builder.keyboardRow(new InlineKeyboardRow(
                createButton("🟢 AL " + symbol, "position:buy:" + symbol),
                createButton("🔴 SAT " + symbol, "position:sell:" + symbol)
            ));
        }

        // Add back button
        builder.keyboardRow(new InlineKeyboardRow(
            createButton("🔙 Broker Menüsü", "menu:broker")
        ));

        return builder.build();
    }

    /**
     * Helper method to create a button
     */
    private static InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
            .text(text)
            .callbackData(callbackData)
            .build();
    }
}
