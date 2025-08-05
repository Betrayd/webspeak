package net.betrayd.webspeak.impl.msg;

import com.google.gson.JsonElement;
import net.betrayd.webspeak.WebSpeakPlayer;

/**
 * A type of message that can be received over the signalling channel.
 * @param <T> The message payload type.
 */
public interface C2SMessage<T> {
    T read(JsonElement payload);
    void apply(WebSpeakPlayer player, T val);
}
