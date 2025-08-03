package net.betrayd.webspeaktest.ui;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.util.StringConverter;
import net.betrayd.webspeak.v1.util.PannerOptions.DistanceModelType;
import net.betrayd.webspeaktest.PannerOptionsManager;
import net.betrayd.webspeaktest.WebSpeakTestApp;

public class PannerOptionsMenuController {
    @FXML
    private ChoiceBox<DistanceModelType> distanceModelPicker;

    @FXML
    private Slider maxDistanceSlider;

    @FXML
    private Slider refDistanceSlider;

    @FXML
    private Slider rolloffFactorSlider;

    @FXML
    private void initialize() {
        distanceModelPicker.getItems().addAll(DistanceModelType.values());
        distanceModelPicker.setConverter(new DistanceModelStringConverter());

        PannerOptionsManager pannerOptions = WebSpeakTestApp.getInstance().getPannerOptionsManager();

        var distanceModelProp = pannerOptions.distanceModelProperty();
        distanceModelPicker.getSelectionModel().select(distanceModelProp.get());
        distanceModelPicker.getSelectionModel().selectedItemProperty().addListener((prop, oldVal, newVal) -> {
            distanceModelProp.set(newVal);
        });

        maxDistanceSlider.valueProperty().bindBidirectional(pannerOptions.maxDistanceProperty());
        refDistanceSlider.valueProperty().bindBidirectional(pannerOptions.refDistanceProperty());
        rolloffFactorSlider.valueProperty().bindBidirectional(pannerOptions.rolloffFactorProperty());
    }

    @FXML
    public void resetMaxDistance() {
        maxDistanceSlider.setValue(26);
    }

    @FXML
    public void resetRefDistance() {
        refDistanceSlider.setValue(1);
    }

    @FXML
    public void resetRolloffFactor() {
        rolloffFactorSlider.setValue(1);
    }
}

class DistanceModelStringConverter extends StringConverter<DistanceModelType> {

    @Override
    public String toString(DistanceModelType object) {
        switch (object) {
            case LINEAR:
                return "Linear";
            case INVERSE:
                return "Inverse";
            case EXPONENTIAL:
                return "Exponential";
            default:
                return object.toString();
        }
    }

    @Override
    public DistanceModelType fromString(String string) {
        throw new UnsupportedOperationException("Unimplemented method 'fromString'");
    }
    
}
