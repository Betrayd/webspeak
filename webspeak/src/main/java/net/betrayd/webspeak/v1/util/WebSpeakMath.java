package net.betrayd.webspeak.v1.util;

import net.betrayd.webspeak.v1.util.PannerOptions.DistanceModelType;

public class WebSpeakMath {

    /**
     * The point at which volume attenuation is considered silent.
     */
    public static final double GAIN_EPSILON = 0.05d;

    /**
     * Compute the gain for an audio source using the <code>linear</code> model.
     * 
     * @param distance      The distance from the listener.
     * @param maxDistance   <code>PannerNode.maxDistance</code>
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The gain at the specified distance.
     */
    public static double computeLinearGain(double distance, double maxDistance, double refDistance, double rolloffFactor) {
        if (distance < refDistance) {
            return 1;
        }
        return 1 - rolloffFactor * (distance - refDistance) / (maxDistance - refDistance);
    }

    /**
     * Compute the distance at which a linear gain model will reach zero.
     * 
     * @param maxDistance   <code>PannerNode.maxDistance</code>
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The distance at which gain = 0.
     */
    public static double linearGainRange(double maxDistance, double refDistance, double rolloffFactor) {
        return -((-maxDistance + refDistance - refDistance * rolloffFactor) / rolloffFactor);
    }

    /**
     * Compute the gain for an audio source using the <code>inverse</code> model.
     * 
     * @param distance      The distance from the listener.
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The gain at the specified distance.
     */
    public static double computeInverseGain(double distance, double refDistance, double rolloffFactor) {
        if (distance < refDistance) {
            return 1;
        }
        return refDistance / (refDistance + rolloffFactor * (distance - refDistance));
    }

    /**
     * Compute the distance at which an inverse gain model will reach a certian
     * gain.
     * 
     * @param gain          Target gain. May not be zero.
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The distance at which gain = value.
     */
    public static double invertInverseGain(double gain, double refDistance, double rolloffFactor) {
        return (refDistance - refDistance * gain) / (rolloffFactor * gain) + refDistance;
    }

    /**
     * Compute the gain for an audio source using the <code>exponential</code>
     * model.
     * 
     * @param distance      The distance from the listener.
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The gain at the specified distance.
     */
    public static double computeExponentialGain(double distance, double refDistance, double rolloffFactor) {
        if (distance < refDistance) {
            return 1;
        }

        return Math.pow(distance / refDistance, -rolloffFactor);
    }

    /**
     * Compute the distance at which an exponential gain model will reach a certian
     * gain.
     * 
     * @param gain          Target gain. May not be zero.
     * @param refDistance   <code>PannerNode.refDistance</code>
     * @param rolloffFactor <code>PannerNode.rolloffFactor</code>
     * @return The gain at the specified distance.
     */
    public static double invertExponentialGain(double gain, double refDistance, double rolloffFactor) {
        return refDistance * Math.pow(gain, -(1 / rolloffFactor));
    }

    /**
     * Compute the max range for a given set of panner options.
     * 
     * @param pannerOptions Panner options to use.
     * @param epsilon       Gain value at which audio is considered silent. Must be
     *                      a non-zero positive value for any distance model other
     *                      than linear.
     * @return The max audible range.
     */
    public static double getMaxRange(PannerOptions pannerOptions, double epsilon) {
        if (pannerOptions.distanceModel != DistanceModelType.LINEAR && epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be greater than zero for any distance model other than linear.");
        }

        switch(pannerOptions.distanceModel) {
            case LINEAR:
                return linearGainRange(pannerOptions.maxDistance, pannerOptions.refDistance, pannerOptions.rolloffFactor);
            case INVERSE:
                return invertInverseGain(epsilon, pannerOptions.refDistance, pannerOptions.rolloffFactor);
            case EXPONENTIAL:
                return invertExponentialGain(epsilon, pannerOptions.refDistance, pannerOptions.rolloffFactor);
            default:
                throw new IllegalArgumentException("Unknown distance model: " + pannerOptions.distanceModel);
        }
    }

    /**
     * Compute the max range for a given set of panner options, assuming an epsilon of <code>0.1f</code>
     * @param pannerOptions Panner options to use.
     * @return The max audible range.
     */
    public static double getMaxRange(PannerOptions pannerOptions) {
        return getMaxRange(pannerOptions, GAIN_EPSILON);
    }
}
