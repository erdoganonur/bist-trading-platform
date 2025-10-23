package com.bisttrading.telegram.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (isLoggedIn) {
            // Logged in menu
            keyboard.add(List.of(
                createButton("📊 Piyasa Verileri", "menu:market"),
                createButton("💼 Broker", "menu:broker")
            ));
            keyboard.add(List.of(
                createButton("📋 Emirler", "menu:orders"),
                createButton("⭐ Watchlist", "menu:watchlist")
            ));
            keyboard.add(List.of(
                createButton("👤 Profil", "menu:profile"),
                createButton("⚙️ Ayarlar", "menu:settings")
            ));
            keyboard.add(List.of(
                createButton("🚪 Çıkış Yap", "menu:logout")
            ));
        } else {
            // Not logged in menu
            keyboard.add(List.of(
                createButton("🔐 Giriş Yap", "auth:login")
            ));
            keyboard.add(List.of(
                createButton("❓ Yardım", "help")
            ));
        }

        return InlineKeyboardMarkup.builder()
            .keyboard(keyboard)
            .build();
    }

    /**
     * Market data menu keyboard
     */
    public static InlineKeyboardMarkup createMarketDataKeyboard() {
        return InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(
                    createButton("🔍 Sembol Ara", "market:search"),
                    createButton("📈 Hisse Fiyatı", "market:quote")
                ),
                List.of(
                    createButton("📊 Sektörler", "market:sectors"),
                    createButton("🔥 Popüler", "market:trending")
                ),
                List.of(
                    createButton("🔙 Ana Menü", "menu:main")
                )
            ))
            .build();
    }

    /**
     * Broker menu keyboard
     */
    public static InlineKeyboardMarkup createBrokerKeyboard(boolean algoLabConnected) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        if (algoLabConnected) {
            keyboard.add(List.of(
                createButton("💰 Hesap Bilgileri", "broker:account"),
                createButton("📊 Pozisyonlar", "broker:positions")
            ));
            keyboard.add(List.of(
                createButton("✅ AlgoLab Durumu", "broker:status")
            ));
        } else {
            keyboard.add(List.of(
                createButton("🔗 AlgoLab Bağlan", "broker:connect")
            ));
        }

        keyboard.add(List.of(
            createButton("🔙 Ana Menü", "menu:main")
        ));

        return InlineKeyboardMarkup.builder()
            .keyboard(keyboard)
            .build();
    }

    /**
     * Confirmation keyboard (Yes/No)
     */
    public static InlineKeyboardMarkup createConfirmationKeyboard(String action) {
        return InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(
                    createButton("✅ Evet", "confirm:" + action + ":yes"),
                    createButton("❌ Hayır", "confirm:" + action + ":no")
                )
            ))
            .build();
    }

    /**
     * Back button keyboard
     */
    public static InlineKeyboardMarkup createBackButton(String backTo) {
        return InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(createButton("🔙 Geri", backTo))
            ))
            .build();
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
