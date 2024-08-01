package com.gvera.gverabot;

import com.gvera.gverabot.controller.BotController;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final BotController controller;

    public TelegramBot(BotController controller) {
        this.controller = controller;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        this.controller.registerBot(this);
        try {
            telegramBotsApi.registerBot(this);
        }
        catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
    @Override
    public void onUpdateReceived(Update update) {
        try {
            controller.processUpdate(update);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "gvera_bot";
    }

    @Override
    public String getBotToken() {
        return "7366527472:AAF9EqJesnj4_buZ9u7kSKaA3u5QJiz6bbU";
    }
}
