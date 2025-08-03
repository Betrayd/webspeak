package net.betrayd.webspeak.event;

import net.betrayd.webspeak.WebSpeakServer;

import java.util.function.Consumer;

public class WebSpeakEvents {
    private WebSpeakEvents() {};

    public static final WebSpeakEvent<Consumer<WebSpeakServer>> SERVER_STARTED = WebSpeakEvent.createSimple();

//    public static WebSpeakEvent<Consumer<>>
}
