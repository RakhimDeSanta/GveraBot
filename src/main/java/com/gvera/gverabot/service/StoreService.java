package com.gvera.gverabot.service;

import com.gvera.gverabot.entity.UserState;
import com.gvera.gverabot.controller.constants.AppConstants;
import com.gvera.gverabot.entity.Item;
import com.gvera.gverabot.entity.ItemAudit;
import com.gvera.gverabot.entity.Store;
import com.gvera.gverabot.entity.User;
import com.gvera.gverabot.repository.AuditRepository;
import com.gvera.gverabot.repository.ItemRepository;
import com.gvera.gverabot.repository.StoreRepository;
import com.gvera.gverabot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StoreService {
    private final AuditRepository auditRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final Map<Long, Store> tempStores = new ConcurrentHashMap<>();
    private final Map<Long, Item> tempItems = new ConcurrentHashMap<>();

    public void yesOrNoKeyboardMarkup(SendMessage response) {
        var replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        var row = new KeyboardRow();

        var yesButton = new KeyboardButton();
        yesButton.setText("Yes");
        var changeButton = new KeyboardButton();
        changeButton.setText("No");

        row.add(yesButton);
        row.add(changeButton);
        rows.add(row);
        replyKeyboardMarkup.setKeyboard(rows);
        response.setReplyMarkup(replyKeyboardMarkup);
    }

    public void viewInventory(Long userId, SendMessage response) {
        Store store = tempStores.get(userId);
        if (checkIfHasItems(store)) {
            response.setText("Your Inventory is empty");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("Here is the inventory of ")
                .append(store.getName())
                .append(":\n\n");

        List<Item> items = store.getItems();
        for (int i = 0; i < items.size(); i++) {
            result.append(i+1)
                    .append(". ");
            Item item = items.get(i);
            result.append(item.getName())
                    .append(", ")
                    .append("amount: ")
                    .append(item.getQuantity())
                    .append(", ")
                    .append("price: ")
                    .append(item.getPrice());

            if (Objects.nonNull(item.getDiscount())) {
                double pwd = item.getPrice() * (100d - item.getDiscount()) / 100;
                result.append("(with discount ")
                        .append(pwd)
                        .append(")");
            }
            result.append(", ")
                    .append("added on ")
                    .append(item.getDate())
                    .append("\n\n");
        }
        response.setText(result.toString());
    }

    public boolean checkIfHasItems(Store store) {
        return Objects.isNull(storeRepository.findById(store.getId()).get().getItems()) || store.getItems().isEmpty();
    }

    public void generateReport(Message message, User currentUser, SendMessage response) {
        StringBuilder result = new StringBuilder();

        Store store = tempStores.get(currentUser.getId());
        Double sum = 0d;
        for (var item : store.getItems())
            sum+=item.getPrice();

        result.append("There are ")
                .append(store.getItems().size())
                .append(" items in your store, costing ")
                .append(sum)
                .append(" sum")
                .append(" in total. \n\n");

        List<ItemAudit> itemAudits = auditRepository.findAllByUpdatedAtBetween(LocalDate.now().minusWeeks(1), LocalDate.now());

        List<Item> highlySoldItemsWeek = itemAudits.stream().filter((i) -> i.getSold() > 10)
                .map(ItemAudit::getItem).toList();

        List<Item> badSoldItemsWeek = itemAudits.stream().filter((i) -> i.getSold() < 5 && i.getSold() >= 1)
                .map(ItemAudit::getItem).toList();

        List<Item> notSoldItems = itemAudits.stream().filter((i) -> i.getSold() == 0)
                .map(ItemAudit::getItem).toList();

        if (!highlySoldItemsWeek.isEmpty()) {
            result.append("Highly sold items this week: \n");
            for (Item item : highlySoldItemsWeek) {
                result.append(item);
            }
        }

        if (!badSoldItemsWeek.isEmpty()) {
            result.append("Badly sold items: \n");
            for (Item item : badSoldItemsWeek) {
                result.append(item);
            }
        }

        if (!notSoldItems.isEmpty()) {
            for (Item item : notSoldItems) {
                result.append(item);
            }
        }

        result.append("\n\nThis report made on ")
                .append(LocalDate.now());

        response.setText(result.toString());
    }

    public void checkAndSave(Message message, Item item) {
        String text = message.getText();
        Integer quantity = Integer.valueOf(text);
        if (quantity < item.getQuantity()) {
            ItemAudit itemAudit = new ItemAudit(UUID.randomUUID(), item, LocalDate.now(), item.getQuantity() - quantity);
            auditRepository.save(itemAudit);
        }
    }

    public void addItemMenu(Message message, User currentUser, SendMessage response) {
        response.setText("Enter the name of the item: ");
        currentUser.setState(UserState.ADD_ITEM);
        userRepository.save(currentUser);
    }

    public void addItemName(Message message, User currentUser, SendMessage response) {
        if (message.getText().startsWith("/")) {
            response.setText("Name cannot start with /\nEnter another one");
            return;
        }
        Item item = new Item();
        item.setName(message.getText());
        tempItems.put(currentUser.getId(), item);

        currentUser.setState(UserState.ADD_ITEM_PRICE);
        userRepository.save(currentUser);
    }

    public Item addItemPrice(Message message, User currentUser, SendMessage response) {
        Item item = tempItems.get(currentUser.getId());
        item.setPrice(Double.valueOf(message.getText()));
        tempItems.put(currentUser.getId(), item);

        currentUser.setState(UserState.ADD_ITEM_QUANTITY);
        userRepository.save(currentUser);
        return item;
    }

    public Item addItemQuantity(Message message, User currentUser, SendMessage response) {
        Item item = tempItems.get(currentUser.getId());
        int quantity;
        try {
            quantity = Integer.parseInt(message.getText());
        } catch (NumberFormatException e) {
            response.setText("Enter a number, not a text! \n\ni.g: 5");
            return null;
        }
        item.setQuantity(quantity);
        tempItems.put(currentUser.getId(), item);

        currentUser.setState(UserState.ADD_ITEM_CONFIRM);
        userRepository.save(currentUser);
        return item;
    }


    public void confirmAddItem(Message message, User currentUser, SendMessage response) {
        String text = message.getText();
        if (text.equals("Yes")) {
            Item item = tempItems.get(currentUser.getId());
            item.setId(UUID.randomUUID());
            item.setDate(LocalDate.now());
            item.setStore(tempStores.get(currentUser.getId()));
            itemRepository.save(item);

            auditRepository.save(new ItemAudit(UUID.randomUUID(), item, item.getDate(), 0));

            Store store = tempStores.get(currentUser.getId());
            store.getItems().add(item);
            storeRepository.save(store);
            tempStores.put(currentUser.getId(), store);
            response.setText("Successfully added");
        } else {
            tempItems.remove(currentUser.getId());
            response.setText("You are on the menu");
        }
    }
    public void setReplyMenu(SendMessage response) {
        var replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        var row1 = new KeyboardRow();
        var row2 = new KeyboardRow();
        var row3 = new KeyboardRow();

        KeyboardButton viewInv = new KeyboardButton();
        KeyboardButton updateInv = new KeyboardButton();
        KeyboardButton discount = new KeyboardButton();
        KeyboardButton addItem = new KeyboardButton();
        KeyboardButton report = new KeyboardButton();
        KeyboardButton invAlert = new KeyboardButton();
        KeyboardButton cancel = new KeyboardButton();

        viewInv.setText(AppConstants.VIEW_INVENTORY);
        updateInv.setText(AppConstants.UPDATE_INVENTORY);
        discount.setText(AppConstants.SET_DISCOUNT);
        addItem.setText(AppConstants.ADD_ITEM);
        report.setText(AppConstants.GEN_REPORT);
        invAlert.setText(AppConstants.GET_ALERT);
        cancel.setText(AppConstants.CANCEL);

        row1.add(viewInv);
        row1.add(updateInv);
        row1.add(addItem);
        row2.add(discount);
        row2.add(report);
        row2.add(invAlert);
        row3.add(cancel);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        response.setText("Choose the action:");
        replyKeyboardMarkup.setKeyboard(rows);
        response.setReplyMarkup(replyKeyboardMarkup);
    }

    public void itemSold(Item item, Integer quantity) {
        Optional<ItemAudit> optItemAudit = auditRepository.findByItem(item);
        ItemAudit itemAudit;

        if (optItemAudit.isEmpty()) {
            itemAudit = new ItemAudit(UUID.randomUUID(), item, LocalDate.now(), item.getQuantity() - quantity);
        } else {
            itemAudit = optItemAudit.get();
            itemAudit.setSold(itemAudit.getSold()+(item.getQuantity() + quantity));
        }
            auditRepository.save(itemAudit);
    }

    public Store register(Message message, User currentUser, SendMessage response) {
        Store store = new Store(UUID.randomUUID(), message.getText());
        currentUser.setState(UserState.STORE_DETAILS);
        userRepository.save(currentUser);
        tempStores.put(currentUser.getId(), store);
        return store;
    }

    public void details(Message message, User currentUser, SendMessage response) {
        Store store = tempStores.get(currentUser.getId());
        store.setContactDetails(message.getText());
        tempStores.put(currentUser.getId(), store);
        confirm(response, store);
        currentUser.setState(UserState.CONFIRM);
        userRepository.save(currentUser);
    }

    public void confirm(SendMessage response, Store store) {
        // todo change to StringBuilder
        String result = store.getName() +
                "\n\n" +
                store.getContactDetails() +
                "\n" +
                "Is that correct ?";

        yesOrNoKeyboardMarkup(response);
        response.setText(result);
    }
    public void display(Message message, User currentUser, SendMessage response) {
        if (Objects.isNull(currentUser.getStores()) || currentUser.getStores().isEmpty()) {
            register(message, currentUser, response);
            currentUser.setState(UserState.REGISTER_STORE);
            userRepository.save(currentUser);
        }
        response.setText("We are happy to see you again!");
        currentUser.setState(UserState.STORES);
        userRepository.save(currentUser);
        display(response, currentUser);
    }

    public void display(SendMessage response, User user) {
        List<Store> stores = user.getStores();
        if (!stores.isEmpty()) {
            var replyKeyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> rows = new ArrayList<>();

            for (var store : stores) {
                var keyboardRow = new KeyboardRow();
                var keyboardButton = new KeyboardButton();
                keyboardButton.setText(store.getName());
                keyboardRow.add(keyboardButton);
                rows.add(keyboardRow);
            }

            replyKeyboardMarkup.setKeyboard(rows);
            response.setReplyMarkup(replyKeyboardMarkup);
        }
    }

    public void confirm(Message message, User currentUser, SendMessage response) {
        String text = message.getText();
        Store store = tempStores.get(currentUser.getId());
        if (text.equals("Yes")) {
            store.setOwner(currentUser);
            storeRepository.save(store);
            currentUser.getStores().add(store);
            currentUser.setState(UserState.STORES);
            userRepository.save(currentUser);
            response.setText("Done!");
            display(response, currentUser);
        } else if (text.equals("No")) {
            response.setText("Hello!\n {brief explanation}\n Enter the name of your store:");
            currentUser.setState(UserState.REGISTER_STORE);
            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(true);
            response.setReplyMarkup(removeKeyboard);
        } else {
            confirm(response, store);
            response.setText("Use buttons!");
        }
    }


    public void updateInv(User currentUser, SendMessage response) {
        Store store = tempStores.get(currentUser.getId());
        if (checkIfHasItems(store)) {
            response.setText("Your Inventory is empty");
            return;
        }

        viewInvInlineKeyboard(store, response);
        response.setText("Choose the item you want to change: ");
        currentUser.setState(UserState.UPDATE_ITEM_MENU);
        userRepository.save(currentUser);
    }

    public void viewInvInlineKeyboard(Store store, SendMessage response) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (var item : store.getItems()) {
            List<InlineKeyboardButton> itemButton = new ArrayList<>();
            InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText(item.getName());
            keyboardButton.setCallbackData(item.getName());
            itemButton.add(keyboardButton);
            rows.add(itemButton);
        }
        keyboardMarkup.setKeyboard(rows);
        response.setReplyMarkup(keyboardMarkup);
    }


    public void setDiscount(User currentUser, SendMessage response) {
        Store store = tempStores.get(currentUser.getId());
        if (checkIfHasItems(store)) {
            response.setText("Your Inventory is empty");
            return;
        }

        response.setText("Choose the item you want to set discount for: ");
        viewInvInlineKeyboard(store, response);

        currentUser.setState(UserState.SET_DISCOUNT_MENU);
        userRepository.save(currentUser);
    }

    public void save(Store store, User currentUser) {
        tempStores.put(currentUser.getId(), store);
    }

    public void setDiscount(User currentUser, Double discount) {
        Store store = tempStores.get(currentUser.getId());

        Item item = tempItems.get(currentUser.getId());
        item.setDiscount(discount);
        item.setStore(store);
        itemRepository.save(item);

        store.getItems().removeIf((i)-> i.getId().equals(item.getId()));
        store.getItems().add(item);
        tempStores.put(currentUser.getId(), store);
        storeRepository.save(store);

        tempItems.remove(currentUser.getId());
        currentUser.setState(UserState.MENU);
        userRepository.save(currentUser);
    }

    public void update(Message message, User currentUser, SendMessage response) {
        UserState state = currentUser.getState();
        Item item = tempItems.get(currentUser.getId());

        String text = message.getText();
        if (state.equals(UserState.UPDATE_ITEM_FIELD_PRICE)) {
            double price;
            try {
                price = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                response.setText("The price should not be a text!");
                return;
            }
            item.setPrice(price);
            checkAndSave(message, item);
        } else if (state.equals(UserState.UPDATE_ITEM_FIELD_NAME)) {
            item.setName(text);
        } else if (state.equals(UserState.UPDATE_ITEM_FIELD_QUANTITY)) {
            // todo change sold in ItemAudit when quantity is reduced
            Integer quantity = Integer.valueOf(text);
            if (quantity < item.getQuantity()) {
                itemSold(item, quantity);
                item.setQuantity(quantity);
            }
        }

        itemRepository.save(item);
        tempItems.remove(currentUser.getId());

        Store store = tempStores.get(currentUser.getId());
        store.getItems().removeIf((i)-> i.getId().equals(item.getId()));
        store.getItems().add(item);
        storeRepository.save(tempStores.get(currentUser.getId()));
        currentUser.setState(UserState.MENU);
        userRepository.save(currentUser);
    }

    public Item tempSave(User currentUser, Optional<Item> optItem) {
        Item item = optItem.get();
        tempItems.put(currentUser.getId(), item);
        currentUser.setState(UserState.UPDATE_ITEM_FIELD_MENU);
        userRepository.save(currentUser);
        return item;
    }

    public Item tempSaveDiscount(User currentUser, Optional<Item> optItem) {
        Item item = optItem.get();
        tempItems.put(currentUser.getId(), item);
        currentUser.setState(UserState.SET_DISCOUNT);
        userRepository.save(currentUser);
        return item;
    }
}
