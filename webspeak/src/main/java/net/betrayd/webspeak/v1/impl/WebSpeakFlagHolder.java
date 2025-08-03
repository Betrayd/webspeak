package net.betrayd.webspeak.v1.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.betrayd.webspeak.v1.WebSpeakFlags.WebSpeakFlag;

// We can use unchecked casts throughout because setFlag ensures we can only set flags with the right type.
@SuppressWarnings("unchecked")
public class WebSpeakFlagHolder {
    private final Map<WebSpeakFlag<?>, Object> map = new HashMap<>();

    public <T> T getFlag(WebSpeakFlag<T> flag) {
        if (flag == null)
            throw new NullPointerException("flag");

        T mapVal = (T) map.get(flag);
        if (mapVal != null) {
            return mapVal;
        } else {
            return flag.defaultValue();
        }
    }
    
    public <T> T setFlag(WebSpeakFlag<T> flag, T value) {
        if (Objects.equals(flag.defaultValue(), value)) {
            return (T) map.remove(value);
        } else {
            return (T) map.put(flag, value);
        }
    }

    public Map<WebSpeakFlag<?>, Object> getFlags() {
        return Collections.unmodifiableMap(map);
    }
}
