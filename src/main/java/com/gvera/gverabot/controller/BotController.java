package com.gvera.gverabot.controller;

import com.gvera.gverabot.TelegramBot;
import com.gvera.gverabot.UserState;
import com.gvera.gverabot.entity.User;
import com.gvera.gverabot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotController {
    private TelegramBot telegramBot;

    private final UserService userService;

    private final StoreController storeController;

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) throws TelegramApiException {
        Long chatId;
        Message message = update.getMessage();
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getFrom().getId();
            message = update.getCallbackQuery().getMessage();
        } else {
            chatId = message.getChatId();
        }
        var response = new SendMessage();

        Optional<User> optUser = userService.findById(chatId);

        User currentUser;

        if (message.getText().equals("/start")) {
            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(true);
            response.setReplyMarkup(removeKeyboard);

            if (optUser.isPresent()) {
                currentUser = optUser.get();
                storeController.display(message, currentUser, response);
            }
        }

        if (optUser.isEmpty()) {
            response.setText("Hello!\n {brief explanation}\n Enter the name of your store:");
            userService.save(new User(chatId, UserState.REGISTER_STORE));
        } else {
            currentUser = optUser.get();
            if (currentUser.getState().equals(UserState.REGISTER_STORE)) {
                storeController.register(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.STORE_DETAILS)) {
                storeController.details(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.CONFIRM)) {
                storeController.confirm(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.STORES) && !message.getText().equals("/start")) {
                storeController.menu(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.STORES)) {
                storeController.display(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.CANCEL) && !message.getText().equals("/start")) {
                storeController.menu(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.CANCEL)) {
                storeController.cancel(currentUser, response);
            } else if (currentUser.getState().equals(UserState.MENU)) {
                storeController.process(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.SET_DISCOUNT_MENU)) {
                storeController.setDiscountMenu(update.getCallbackQuery().getData(), currentUser, response);
            } else if (currentUser.getState().equals(UserState.SET_DISCOUNT)) {
                storeController.setDiscount(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.UPDATE_ITEM_MENU)) {
                storeController.updateMenu(update.getCallbackQuery().getData(), currentUser, response);
            } else if (currentUser.getState().equals(UserState.UPDATE_ITEM_FIELD_MENU)) {
                storeController.updateFieldMenu(update.getCallbackQuery().getData(), currentUser, response);
            } else if (currentUser.getState().equals(UserState.UPDATE_ITEM_FIELD_PRICE) ||
                       currentUser.getState().equals(UserState.UPDATE_ITEM_FIELD_NAME) ||
                       currentUser.getState().equals(UserState.UPDATE_ITEM_FIELD_QUANTITY)) {
                storeController.update(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.ADD_ITEM)) {
                storeController.addItemName(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.ADD_ITEM_PRICE)) {
                storeController.addItemPrice(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.ADD_ITEM_QUANTITY)) {
                storeController.addItemQuantity(message, currentUser, response);
            } else if (currentUser.getState().equals(UserState.ADD_ITEM_CONFIRM)) {
                storeController.confirmAddItem(message, currentUser, response);
            }
        }

        sendMessage(message, response);
    }

    private void sendMessage(Message message, SendMessage response) throws TelegramApiException {
        response.setChatId(message.getChatId());
        this.telegramBot.execute(response);
    }
}
