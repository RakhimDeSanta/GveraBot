package com.gvera.gverabot.controller;

import com.gvera.gverabot.entity.Item;
import com.gvera.gverabot.entity.Store;
import com.gvera.gverabot.entity.User;
import com.gvera.gverabot.repository.ItemRepository;
import com.gvera.gverabot.repository.StoreRepository;
import com.gvera.gverabot.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemController {
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;


    @Transactional
    public void viewItems(Message message, User currentUser, SendMessage response) {

    }

    private void setReplyKeyboardItems(Store store, SendMessage response) {
        List<Item> items = store.getItems();
        var replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        if (!items.isEmpty()) {
            for (var item : items) {
                var keyboardRow = new KeyboardRow();
                var keyboardButton = new KeyboardButton();
                keyboardButton.setText(item.getName());
                keyboardRow.add(keyboardButton);
                rows.add(keyboardRow);
            }
            replyKeyboardMarkup.setKeyboard(rows);
            response.setReplyMarkup(replyKeyboardMarkup);
            response.setText("Here is the inventory of " + store.getName());
        } else {
            response.setText("Your inventory is empty");
        }
        var keyboardRow = new KeyboardRow();
        var keyBoardButton = new KeyboardButton();
        keyBoardButton.setText("Add");
        keyboardRow.add(keyBoardButton);
        rows.add(keyboardRow);
        replyKeyboardMarkup.setKeyboard(rows);
        response.setReplyMarkup(replyKeyboardMarkup);
    }
}
