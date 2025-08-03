package net.betrayd.webspeak.v1.util;

/**
 * <p>
 * Represents a "modifier" that can be applied to player audio, such as muting
 * or disabling spatialization. Unlike filters, audio modifiers impact server
 * code in addition to being replicated to the client.
 * </p>
 * <p>
 * All values in this record are optional. Missing options will revert to a
 * lower-priority modifier or the default value.
 * </p>
 * 
 * @param silenced    If set, server will tell the subject client to mute the
 *                    target player. This will override any mute/unmute flag on
 *                    the client, and will not be displayed in the UI.
 *                    <b>default: false</b>
 * @param spatialized Whether to spatialize the target player's audio.
 *                    <b>default: true</code>
 */
public record AudioModifier(Boolean silenced, Boolean spatialized) {
    public boolean isMuted() {
        return silenced != null ? silenced : false;
    }

    public boolean isSpatialized() {
        return spatialized != null ? spatialized : true;
    }

    /**
     * Combine two audio modifiers, with one taking priority over the other.
     * 
     * @param a The higher-priority modifier.
     * @param b The lower-priority modifier.
     * @return The combined modifier.
     */
    public static AudioModifier combine(AudioModifier a, AudioModifier b) {
        if (a == null)
            a = DEFAULT;
        if (b == null)
            b = DEFAULT;

        return new AudioModifier(
                a.silenced != null ? a.silenced : b.silenced,
                a.spatialized != null ? a.spatialized : b.spatialized);
    }

    public static final AudioModifier EMPTY = new AudioModifier(null, null);
    public static final AudioModifier DEFAULT = new AudioModifier(false, true);
}
