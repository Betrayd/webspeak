module net.betrayd.webspeak {
    requires static lombok;
    requires com.google.common;
    requires com.google.gson;
    requires org.eclipse.jetty.websocket.client;
    requires org.jetbrains.annotations;
    requires webrtc.java;

    exports net.betrayd.webspeak.event;
    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.math;
}
