package com.gvera.gverabot.controller;

import com.gvera.gverabot.entity.UserState;
import com.gvera.gverabot.controller.constants.AppConstants;
import com.gvera.gverabot.entity.Item;
import com.gvera.gverabot.entity.Store;
import com.gvera.gverabot.entity.User;
import com.gvera.gverabot.repository.ItemRepository;
import com.gvera.gverabot.repository.StoreRepository;
import com.gvera.gverabot.repository.UserRepository;
import com.gvera.gverabot.service.StoreService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Component
@RequiredArgsConstructor
public class StoreController {
    private final UserRepository userRepository;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public void register(Message message, User currentUser, SendMessage response) {
        if (message.getText().equals("/start")) {
            response.setText("Hello!\n {brief explanation}\n Enter the name of your store:");
            return;
        }
        Store store = storeService.register(message, currentUser, response);
        response.setChatId(currentUser.getId());
        response.setText("Now enter contact details for " + store.getName());
    }


    @Transactional
    public void details(Message message, User currentUser, SendMessage response) {
        storeService.details(message, currentUser, response);
    }


    @Transactional
    public void confirm(Message message, User currentUser, SendMessage response) {
        storeService.confirm(message, currentUser, response);
    }

    public void menu(Message message, User currentUser, SendMessage response) {
        String storeName = message.getText();
        Optional<Store> optStore = storeRepository.findByName(storeName);
        if (optStore.isEmpty()) {
            response.setText(storeName + " is not found");
            return;
        } else {
            Store store = optStore.get();
            if (!store.getOwner().getId().equals(currentUser.getId())) {
                response.setText(storeName + " is not found");
            } else {
                storeService.save(store, currentUser);
            }
        }
        currentUser.setState(UserState.MENU);
        userRepository.save(currentUser);
        storeService.setReplyMenu(response);
    }

    public void process(Message message, User currentUser, SendMessage response) {
        String text = message.getText();
        storeService.setReplyMenu(response);

        if (text.equals(AppConstants.VIEW_INVENTORY)) {
            storeService.viewInventory(currentUser.getId(), response);
        } else if (text.equals(AppConstants.UPDATE_INVENTORY)) {
            storeService.updateInv(currentUser, response);
        } else if (text.equals(AppConstants.SET_DISCOUNT)) {
            storeService.setDiscount(currentUser, response);
        } else if (text.equals(AppConstants.GEN_REPORT)) {
            storeService.generateReport(message, currentUser, response);
        } else if (text.equals(AppConstants.GET_ALERT)) {

        } else if (text.equals(AppConstants.ADD_ITEM)) {
            storeService.addItemMenu(message, currentUser, response);
        } else if (text.equals(AppConstants.CANCEL)) {
            storeService.display(response, currentUser);
            response.setText("Choose the store:");
            currentUser.setState(UserState.CANCEL);
            userRepository.save(currentUser);
        }
        else {
            response.setText("Choose the action: ");
        }
    }



    public void updateMenu(String itemName, User currentUser, SendMessage response) {
        Optional<Item> optItem = itemRepository.findByName(itemName);

        if (optItem.isEmpty()) {
            response.setText("The item is not found");
            return;
        }

        Item item = storeService.tempSave(currentUser, optItem);

        StringBuilder sb = new StringBuilder();
        sb.append(item.getName())
                .append(", quantity: ")
                .append(item.getQuantity())
                .append(", price: ")
                .append(item.getPrice())
                .append("\nWhat do you want to change ?");

        response.setText(sb.toString());

        updateFieldMenu(response);
    }

    private void updateFieldMenu(SendMessage response) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> nameButton = new ArrayList<>();
        InlineKeyboardButton nameBtn = new InlineKeyboardButton(), priceBtn = new InlineKeyboardButton(), quantityBtn = new InlineKeyboardButton();
        nameBtn.setText(AppConstants.NAME);
        nameBtn.setCallbackData(AppConstants.NAME);
        nameButton.add(nameBtn);
        rows.add(nameButton);

        List<InlineKeyboardButton> priceButton = new ArrayList<>();
        priceBtn.setText(AppConstants.PRICE);
        priceBtn.setCallbackData(AppConstants.PRICE);
        priceButton.add(priceBtn);
        rows.add(priceButton);

        List<InlineKeyboardButton> quantityButton = new ArrayList<>();
        quantityBtn.setText(AppConstants.QUANTITY);
        quantityBtn.setCallbackData(AppConstants.QUANTITY);
        quantityButton.add(quantityBtn);
        rows.add(quantityButton);

        keyboardMarkup.setKeyboard(rows);
        response.setReplyMarkup(keyboardMarkup);
    }


    public void cancel(User currentUser, SendMessage response) {
        response.setText("Choose the store: ");
        storeService.display(response, currentUser);
    }

    public void setDiscountMenu(String itemName, User currentUser, SendMessage response) {
        Optional<Item> optItem = itemRepository.findByName(itemName);

        if (optItem.isEmpty()) {
            response.setText("Item is not found");
            return;
        }

        Item item = storeService.tempSaveDiscount(currentUser, optItem);

        response.setText("Now enter the discount for " + item.getName());
    }



    public void setDiscount(Message message, User currentUser, SendMessage response) {
        Double discount = null;
        try {
            discount = Double.valueOf(message.getText());
        } catch (NumberFormatException e) {
            response.setText("Invalid format of discount.");
        }

        storeService.setDiscount(currentUser, discount);
        response.setText("Discount is successfully set. \n Tap on 'View Inventory' to see the changes.");
    }



    public void updateFieldMenu(String field, User currentUser, SendMessage response) {
        if (field.equals(AppConstants.PRICE)) {
            response.setText("Enter the new price:");
            currentUser.setState(UserState.UPDATE_ITEM_FIELD_PRICE);
        } else if (field.equals(AppConstants.QUANTITY)) {
            response.setText("Enter the new quantity:");
            currentUser.setState(UserState.UPDATE_ITEM_FIELD_QUANTITY);
        } else if (field.equals(AppConstants.NAME)) {
            response.setText("Enter the new name:");
            currentUser.setState(UserState.UPDATE_ITEM_FIELD_NAME);
        } else {
            response.setText("Use buttons!");
            return;
        }
        userRepository.save(currentUser);
    }

    public void update(Message message, User currentUser, SendMessage response) {
        storeService.update(message, currentUser, response);
        response.setText("Successfully updated!");
    }

    public void addItemName(Message message, User currentUser, SendMessage response) {
        storeService.addItemName(message, currentUser, response);
        response.setText("Now enter the price for "+message.getText()+": \n\ni.g: 25000 = 25000 sum");
    }

    public void addItemPrice(Message message, User currentUser, SendMessage response) {
        Item item = storeService.addItemPrice(message, currentUser, response);
        response.setText("Enter the quantity for "+ item.getName() + ":");
    }

    public void addItemQuantity(Message message, User currentUser, SendMessage response) {
        Item item = storeService.addItemQuantity(message, currentUser, response);

        if (Objects.nonNull(item)) {
            StringBuilder result = new StringBuilder();
            result.append("Please check if everything is right. \n\n")
                    .append("Item's name: ")
                    .append(item.getName())
                    .append("\n")
                    .append("Item's price: ")
                    .append(item.getPrice())
                    .append("\n")
                    .append("Item's quantity: ")
                    .append(item.getQuantity())
                    .append("\n")
                    .append("Are you sure ?");

            response.setText(result.toString());
            storeService.yesOrNoKeyboardMarkup(response);
        }
    }

    public void confirmAddItem(Message message, User currentUser, SendMessage response) {
        storeService.confirmAddItem(message, currentUser, response);

        currentUser.setState(UserState.MENU);
        userRepository.save(currentUser);
        userRepository.save(currentUser);
        storeService.setReplyMenu(response);
    }

    public void display(Message message, User currentUser, SendMessage response) {
        storeService.display(message, currentUser, response);
    }
}
