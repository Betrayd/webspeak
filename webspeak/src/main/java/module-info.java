module net.betrayd.webspeak {
    requires static lombok;
    requires org.jetbrains.annotations;
    requires org.eclipse.jetty.websocket.client;
    requires com.google.gson;
    requires com.google.common;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.event;
    exports net.betrayd.webspeak.math;
}