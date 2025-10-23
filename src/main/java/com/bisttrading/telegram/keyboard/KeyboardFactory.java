package com.bisttrading.telegram.keyboard;

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
     * Helper method to create a button
     */
    private static InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
            .text(text)
            .callbackData(callbackData)
            .build();
    }
}
