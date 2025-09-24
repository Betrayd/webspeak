package net.betrayd.webspeak.testapp.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;

import javafx.scene.layout.GridPane;
import lombok.Getter;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.testapp.Player;
import net.betrayd.webspeak.testapp.TestWebPlayer;
import net.betrayd.webspeak.testapp.WebSpeakTestApp;

import java.util.function.Consumer;
/*import net.betrayd.webspeak.WebSpeakChannel;
import net.betrayd.webspeak.util.WebSpeakEvents;
import net.betrayd.webspeak.util.WebSpeakEvents.WebSpeakEvent;
import net.betrayd.webspeaktest.Player;
import net.betrayd.webspeaktest.WebSpeakTestApp;*/

public class PlayerInfoController {

    public static PlayerInfoController loadInstance() {
        try {
            FXMLLoader loader = new FXMLLoader(PlayerInfoController.class.getResource("/ui/playerInfo.fxml"));
            loader.load();
            return loader.getController();
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Yeaah, there's gotta be a more "javafx" way to do this, but I can't be bothered to learn right now.
     */
    public final Event.Invokable<Consumer<Void>> ON_REQUEST_REMOVE = Event.create();

    private Player player;

    @Getter
    @FXML
    private TitledPane titledPane;

    /*@FXML
    private HBox titleBox;*/

    @FXML
    private GridPane gridPane;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private TextField nameField;

    @FXML
    private TextField sessionIdField;

   /* @FXML
    private Label connectionText;*/

    //TODO: unimplemented
    /*@FXML
    private ChoiceBox<WebSpeakChannel> channelSelector;*/

    //TODO: I don't know what this is
    /*
    @FXML
    private GroupListController groupListController;*/

    private BooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public boolean isSelected() {
        return selectedProperty.get();
    }

    public void setSelected(boolean selected) {
        selectedProperty.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    @FXML
    protected void initialize() {
        nameField.focusedProperty().addListener((prop, oldVal, newVal) -> {
            if (!newVal) {
                commitNameChange();
            }
        });

        connectionProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal == null) {
                //connectionText.setText("Not Connected");
                //connectionText.setTextFill(Color.RED);
            } else {
                //connectionText.setText(newVal);
                //connectionText.setTextFill(Color.GREEN);
            }
        });

        selectedProperty.addListener((prop, oldVal, newVal) -> {
            gridPane.pseudoClassStateChanged(SELECTED, newVal);
        });

        //titleBox.prefWidthProperty().bind(titledPane.widthProperty().subtract(60));

        //a BUNCH of channel stuff
        /*
        channelSelector.setConverter(new StringConverter<WebSpeakChannel>() {

            @Override
            public String toString(WebSpeakChannel object) {
                return object != null ? object.getName() : "null";
            }

            @Override
            public WebSpeakChannel fromString(String string) {
                throw new UnsupportedOperationException("Unimplemented method 'fromString'");
            }

        });

        channelSelector.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            if (!isChannelUpdating && getPlayer() != null) {
                isChannelUpdating = true;
                player.setChannel(newVal);
                isChannelUpdating = false;
            }
        });

        var channelList = WebSpeakTestApp.getInstance().getChannels();
        setupChannelList(channelList);
        channelList.addListener(new ListChangeListener<>() {

            @Override
            public void onChanged(Change<? extends WebSpeakChannel> c) {
                setupChannelList(c.getList());
            }

        });
    */
    }

    //it's been too long what even are channels?
    /*
    private boolean isChannelUpdating = false;



    private void setupChannelList(List<? extends WebSpeakChannel> channels) {
        var selected = channelSelector.getSelectionModel().getSelectedItem();
        channelSelector.getItems().clear();
        for (var channel : channels) {
            channelSelector.getItems().add(channel);
        }
        channelSelector.getSelectionModel().select(selected);
    }*/

    private StringProperty connectionProperty = new SimpleStringProperty();

    // Store as a variable so it can be unregistered
    private MapChangeListener<Player, String> mapChangeListener = (change) -> {
        if (change.getKey().equals(player))
            connectionProperty.set(change.getValueAdded());

    };

    public void initPlayer(Player player) {
        // titledPane.textProperty().bind(player.nameProperty());

        colorPicker.valueProperty().bindBidirectional(player.colorProperty);

        // Manually add listener so field can still be updated.
        nameField.setText(player.nameProperty.getName());
        player.nameProperty.addListener(this::updateNameProperty);
        player.webPlayerProperty.addListener(this::updateWebPlayerProperty);

        //manually call to update the value when init
        updateNameProperty(player.nameProperty, null, player.nameProperty.get());
        updateWebPlayerProperty(player.webPlayerProperty, null, player.webPlayerProperty.get());

        /*
        channelSelector.getSelectionModel().select(player.getChannel());
        channelSelector.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            player.setChannel(newVal);
        });

        groupListController.initPlayer(player);
        */
        //WebSpeakTestApp.getInstance().getConnectionIps().addListener(mapChangeListener);

        this.player = player;
    }

    private void updateNameProperty(ObservableValue<? extends String> prop, String oldVal, String newVal) {
        nameField.setText(newVal);
    }

    private void updateWebPlayerProperty(ObservableValue<? extends TestWebPlayer> prop, TestWebPlayer oldVal, TestWebPlayer newVal){
        if (newVal != null) {
            sessionIdField.setText(newVal.getSessionId());
        } else {
            sessionIdField.setText("");
        }
    }

    @FXML
    protected void onNameFieldCommit(ActionEvent e) {
        commitNameChange();
    }

    protected void commitNameChange() {
        player.nameProperty.set(nameField.getText());
    }


    public void onPlayerRemoved() {
        //WebSpeakTestApp.getInstance().getConnectionIps().removeListener(mapChangeListener);
        //TODO: add unimplemented method
    }

    @FXML
    private void removePlayer() {
        ON_REQUEST_REMOVE.invoke(null);
    }

}
