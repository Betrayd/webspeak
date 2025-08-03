package net.betrayd.webspeak.v1.util;

import com.google.gson.annotations.SerializedName;

/**
 * Config parameters to send to the client about how to handle audio spatialization.
 */
public class PannerOptions {

    public static enum PanningModelType {
        @SerializedName("HRTF")
        HRTF,
        @SerializedName("equalpower")
        EQUAL_POWER
    }

    public static enum DistanceModelType {
        @SerializedName("linear")
        LINEAR,
        @SerializedName("inverse")
        INVERSE,
        @SerializedName("exponential")
        EXPONENTIAL
    }

    public float coneInnerAngle = 360;
    public float coneOuterAngle = 0;
    public float coneOuterGain = 0;
    public DistanceModelType distanceModel = DistanceModelType.INVERSE;
    public float maxDistance = 26;
    public float orientationX = 0;
    public float orientationY = 0;
    public float orientationZ = 0;
    public PanningModelType panningModel = PanningModelType.HRTF;
    public float positionX = 0;
    public float positionY = 0;
    public float positionZ = 0;
    public float refDistance = 1;
    public float rolloffFactor = 1;

    /**
     * Copy all parameters from another instance.
     * 
     * @param other Other instance.
     */
    public void copyFrom(PannerOptions other) {
        this.coneInnerAngle = other.coneInnerAngle;
        this.coneOuterAngle = other.coneOuterAngle;
        this.coneOuterGain = other.coneOuterGain;
        this.distanceModel = other.distanceModel;
        this.maxDistance = other.maxDistance;
        this.orientationX = other.orientationX;
        this.orientationY = other.orientationY;
        this.orientationZ = other.orientationZ;
        this.panningModel = other.panningModel;
        this.positionX = other.positionX;
        this.positionY = other.positionY;
        this.positionZ = other.positionZ;
        this.refDistance = other.refDistance;
        this.rolloffFactor = other.rolloffFactor;
    }

    /**
     * Copy all enabled parameters from a partial instance.
     * 
     * @param partial Partial instance.
     */
    public void copyFrom(Partial partial) {
        if (partial.coneInnerAngle != null)
            coneInnerAngle = partial.coneInnerAngle;
        if (partial.coneOuterAngle != null)
            coneOuterAngle = partial.coneOuterAngle;
        if (partial.coneOuterGain != null)
            coneOuterGain = partial.coneOuterGain;
        if (partial.distanceModel != null)
            distanceModel = partial.distanceModel;
        if (partial.maxDistance != null)
            maxDistance = partial.maxDistance;
        if (partial.orientationX != null)
            orientationX = partial.orientationX;
        if (partial.orientationY != null)
            orientationY = partial.orientationY;
        if (partial.orientationZ != null)
            orientationZ = partial.orientationZ;
        if (partial.panningModel != null)
            panningModel = partial.panningModel;
        if (partial.positionX != null)
            positionX = partial.positionX;
        if (partial.positionY != null)
            positionY = partial.positionY;
        if (partial.positionZ != null)
            positionZ = partial.positionZ;
        if (partial.refDistance != null)
            refDistance = partial.refDistance;
        if (partial.rolloffFactor != null)
            rolloffFactor = partial.rolloffFactor;
    }

    public Partial toPartial() {
        Partial partial = new Partial();
        partial.coneInnerAngle = this.coneInnerAngle;
        partial.coneOuterAngle = this.coneOuterAngle;
        partial.coneOuterGain = this.coneOuterGain;
        partial.distanceModel = this.distanceModel;
        partial.maxDistance = this.maxDistance;
        partial.orientationX = this.orientationX;
        partial.orientationY = this.orientationY;
        partial.orientationZ = this.orientationZ;
        partial.panningModel = this.panningModel;
        partial.positionX = this.positionX;
        partial.positionY = this.positionY;
        partial.positionZ = this.positionZ;
        partial.refDistance = this.refDistance;
        partial.rolloffFactor = this.rolloffFactor;
        return partial;
    }

    /**
     * A variation of PannerOptions where all fields are optional.
     */
    public static class Partial {
        public Float coneInnerAngle = null;
        public Float coneOuterAngle = null;
        public Float coneOuterGain = null;
        public DistanceModelType distanceModel = null;
        public Float maxDistance = null;
        public Float orientationX = null;
        public Float orientationY = null;
        public Float orientationZ = null;
        public PanningModelType panningModel = null;
        public Float positionX = null;
        public Float positionY = null;
        public Float positionZ = null;
        public Float refDistance = null;
        public Float rolloffFactor = null;
    }
}
