package net.betrayd.webspeaktest;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.util.PannerOptions;
import net.betrayd.webspeak.v1.util.WebSpeakMath;
import net.betrayd.webspeak.v1.util.PannerOptions.DistanceModelType;

public class PannerOptionsManager {
    private final WebSpeakTestApp app;

    public PannerOptionsManager(WebSpeakTestApp app) {
        this.app = app;
        distanceModelProperty.addListener((prop, oldVal, newVal) -> {
            if (app.getServer() != null) {
                var partial = new PannerOptions.Partial();
                partial.distanceModel = newVal;
                app.getServer().updatePannerOptions(partial);
            }
        });

        maxDistanceProperty.addListener((prop, oldVal, newVal) -> {
            if (app.getServer() != null) {
                var partial = new PannerOptions.Partial();
                partial.maxDistance = newVal.floatValue();
                app.getServer().updatePannerOptions(partial);
            }
        });

        refDistanceProperty.addListener((prop, oldVal, newVal) -> {
            if (app.getServer() != null) {
                var partial = new PannerOptions.Partial();
                partial.refDistance = newVal.floatValue();
                app.getServer().updatePannerOptions(partial);
            }
        });

        rolloffFactorProperty.addListener((prop, oldVal, newVal) -> {
            if (app.getServer() != null) {
                var partial = new PannerOptions.Partial();
                partial.rolloffFactor = newVal.floatValue();
                app.getServer().updatePannerOptions(partial);
            }
        });

        DoubleBinding scopeRadiusBinding = Bindings.createDoubleBinding(() -> {
            switch(distanceModelProperty.get()) {
                case LINEAR:
                    return WebSpeakMath.linearGainRange(maxDistanceProperty.get(), refDistanceProperty.get(), rolloffFactorProperty.get());
                case INVERSE:
                    return WebSpeakMath.invertInverseGain(WebSpeakMath.GAIN_EPSILON, refDistanceProperty.get(), rolloffFactorProperty.get());
                case EXPONENTIAL:
                    return WebSpeakMath.invertExponentialGain(WebSpeakMath.GAIN_EPSILON, refDistanceProperty.get(), rolloffFactorProperty.get());
                default:
                    throw new IllegalArgumentException("Unknown distance model: " + distanceModelProperty.get());
            }
        }, distanceModelProperty, maxDistanceProperty, refDistanceProperty, rolloffFactorProperty);

        scopeRadiusProperty.bind(scopeRadiusBinding);
    }

    public void applyAllPannerOptions(WebSpeakServer server) {
        server.getPannerOptions().distanceModel = getDistanceModel();
        server.getPannerOptions().maxDistance = getMaxDistance();
        server.getPannerOptions().refDistance = getRefDistance();
        server.getPannerOptions().rolloffFactor = getRolloffFactor();
        server.updatePannerOptions();
    }

    public WebSpeakTestApp getApp() {
        return app;
    }

    private final ObjectProperty<DistanceModelType> distanceModelProperty = new SimpleObjectProperty<>(DistanceModelType.INVERSE);

    public DistanceModelType getDistanceModel() {
        return distanceModelProperty.get();
    }

    public void setDistanceModel(DistanceModelType distanceModel) {
        distanceModelProperty.set(distanceModel);
    }

    public ObjectProperty<DistanceModelType> distanceModelProperty() {
        return distanceModelProperty;
    }

    private final FloatProperty maxDistanceProperty = new SimpleFloatProperty(26);

    public float getMaxDistance() {
        return maxDistanceProperty.get();
    }

    public void setMaxDistance(float maxDistance) {
        maxDistanceProperty.set(maxDistance);
    }

    public FloatProperty maxDistanceProperty() {
        return maxDistanceProperty;
    }

    private final FloatProperty refDistanceProperty = new SimpleFloatProperty(1);

    public float getRefDistance() {
        return refDistanceProperty.get();
    }

    public void setRefDistance(float refDistance) {
        refDistanceProperty.set(refDistance);
    }

    public FloatProperty refDistanceProperty() {
        return refDistanceProperty;
    }

    private final FloatProperty rolloffFactorProperty = new SimpleFloatProperty(1);

    public float getRolloffFactor() {
        return rolloffFactorProperty.get();
    }

    public void setRolloffFactor(float rolloffFactor) {
        rolloffFactorProperty.set(rolloffFactor);
    }

    public FloatProperty rolloffFactorProperty() {
        return rolloffFactorProperty;
    }

    private final DoubleProperty scopeRadiusProperty = new SimpleDoubleProperty();

    public double getScopeRadius() {
        return scopeRadiusProperty.get();
    }

    public ReadOnlyDoubleProperty scopeRadiusProperty() {
        return scopeRadiusProperty;
    }
}
