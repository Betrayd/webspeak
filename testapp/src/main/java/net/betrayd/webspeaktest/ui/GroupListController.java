package net.betrayd.webspeaktest.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import net.betrayd.webspeak.v1.WebSpeakGroup;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeaktest.Player;
import net.betrayd.webspeaktest.WebSpeakTestApp;

// This is shit and unoptimized, but it's a test UI so I don't care.
public class GroupListController {
    
    @FXML
    private ListView<GroupEntry> groupList;
    
    private Player player;

    @FXML
    public void initialize() {
        groupList.setCellFactory(CheckBoxListCell.forListView(entry -> entry.selectedProperty));
        for (var group : WebSpeakTestApp.getInstance().getGlobalGroups()) {
            groupList.getItems().add(new GroupEntry(group));
        }
    }

    public void initPlayer(Player player) {
        if (this.player != null) {
            throw new IllegalStateException("Player is already initialized");
        }
        this.player = player;

        player.webPlayerProperty().addListener((prop, oldVal, newVal) -> {
            updatePlayerGroups(newVal);
        });
        if (player.getWebPlayer() != null) {
            updatePlayerGroups(player.getWebPlayer());
        }
    }

    @FXML
    public void shiftUp() {
        int selectedIndex = groupList.getSelectionModel().getSelectedIndex();
        if (selectedIndex <= 0)
            return;

        GroupEntry selectedItem = groupList.getItems().get(selectedIndex);
        GroupEntry prevItem = groupList.getItems().get(selectedIndex - 1);

        groupList.getItems().set(selectedIndex, prevItem);
        groupList.getItems().set(selectedIndex - 1, selectedItem);
        groupList.getSelectionModel().select(selectedItem);
        updatePlayerGroups();
    }

    @FXML
    public void shiftDown() {
        int selectedIndex = groupList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= groupList.getItems().size() - 1)
            return;
        
        GroupEntry selectedItem = groupList.getItems().get(selectedIndex);
        GroupEntry nextItem = groupList.getItems().get(selectedIndex + 1);

        groupList.getItems().set(selectedIndex, nextItem);
        groupList.getItems().set(selectedIndex + 1, selectedItem);
        groupList.getSelectionModel().select(selectedItem);
        updatePlayerGroups();
    }

    private void updatePlayerGroups() {
        if (player != null && player.getWebPlayer() != null) {
            updatePlayerGroups(player.getWebPlayer());
        }
    }

    private void updatePlayerGroups(WebSpeakPlayer player) {
        if (player == null)
            return;
        player.modifyGroups(list -> {
            list.clear();
            for (var groupEntry : groupList.getItems()) {
                if (groupEntry.selectedProperty.get()) {
                    list.add(groupEntry.group);
                }
            }
        });
    }

    private class GroupEntry {
        final WebSpeakGroup group;

        GroupEntry(WebSpeakGroup group) {
            this.group = group;
            selectedProperty.addListener((prop, oldVal, newVal) -> {
                updatePlayerGroups();
            });
        }

        @Override
        public String toString() {
            return group.getName();
        }

        BooleanProperty selectedProperty = new SimpleBooleanProperty();
    }
}
