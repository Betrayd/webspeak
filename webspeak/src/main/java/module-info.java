module net.betrayd.webspeak {
    requires static lombok;
    requires org.jetbrains.annotations;
    requires org.eclipse.jetty.websocket.client;
    requires com.google.gson;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.event;
}