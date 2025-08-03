package net.betrayd.webspeak.v1.impl.net;

import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.betrayd.webspeak.v1.WebSpeakPlayer;

public interface C2SPacket<T> {
    public T read(String payload);
    public void apply(WebSpeakPlayer player, T val);

    public static class JsonC2SPacket<T> implements C2SPacket<T> {

        private static final Gson GSON = new Gson();

        private TypeToken<T> typeToken;
        private Class<T> clazz;

        private final BiConsumer<WebSpeakPlayer, T> applicator;

        public JsonC2SPacket(TypeToken<T> typeToken, BiConsumer<WebSpeakPlayer, T> applicator) {
            this.typeToken = typeToken;
            this.applicator = applicator;
        }

        public JsonC2SPacket(Class<T> clazz, BiConsumer<WebSpeakPlayer, T> applicator) {
            this.clazz = clazz;
            this.applicator = applicator;
        }

        @Override
        public T read(String payload) {
            if (typeToken != null) {
                return GSON.fromJson(payload, typeToken);
            } else if (clazz != null) {
                return GSON.fromJson(payload, clazz);
            } else {
                throw new IllegalStateException("Somehow, both typeToken and clazz are null.");
            }
        }

        @Override
        public void apply(WebSpeakPlayer player, T val) {
            applicator.accept(player, val);
        }
    }

}
