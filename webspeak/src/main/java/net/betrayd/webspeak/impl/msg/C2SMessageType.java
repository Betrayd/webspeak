package net.betrayd.webspeak.impl.msg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.function.BiConsumer;

@Getter
public class C2SMessageType<K, T> {
    private final Class<T> payloadClass;
    private final BiConsumer<K, T> consumer;

    public C2SMessageType(Class<T> payloadClass, BiConsumer<K, T> consumer) {
        this.payloadClass = payloadClass;
        this.consumer = consumer;
    }

    public T fromJson(Gson gson, JsonObject payload) {
        return gson.fromJson(payload, payloadClass);
    }

    public void parseAndApply(K owner, Gson gson, JsonObject payload) {
        T value = fromJson(gson, payload);
        consumer.accept(owner, value);
    }
}
