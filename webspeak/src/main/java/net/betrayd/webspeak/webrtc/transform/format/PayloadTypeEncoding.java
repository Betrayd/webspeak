package net.betrayd.webspeak.webrtc.transform.format;

public enum PayloadTypeEncoding {
    OTHER,
    VP8,
    VP9,
    AV1,
    H264,
    RED,
    RTX,
    OPUS,
    TELEPHONE_EVENT;

    public static final String TEL_EVENT_TEXT = "telephone-event";

    public static PayloadTypeEncoding createFrom(String value) {
        try {
            if (value.toLowerCase() == TEL_EVENT_TEXT) {
                return TELEPHONE_EVENT;
            }
            return PayloadTypeEncoding.valueOf(value.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return OTHER;
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (super.equals(TELEPHONE_EVENT)) {
            builder.append(TEL_EVENT_TEXT);
        } else {
            builder.append(super.toString());
        }
        return builder.toString();
    }
}
