package net.betrayd.webspeak.v1.util;

/**
 * Contains human-readable metadata about a given player.
 * 
 * @param name   The player's username.
 * @param avatar The URL of an avatar image to use for the player. Should be
 *               square and small in size.
 */
public record WSPlayerListEntry(String name, String avatar) {
    public WSPlayerListEntry withName(String name) {
        return new WSPlayerListEntry(name, avatar);
    }

    public WSPlayerListEntry withAvatar(String avatar) {
        return new WSPlayerListEntry(name, avatar);
    }

    public boolean isValid() {
        return name != null && avatar != null;
    }

    public static final WSPlayerListEntry DEFAULT = new WSPlayerListEntry("", "");
}
