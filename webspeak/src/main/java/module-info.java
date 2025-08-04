module net.betrayd.webspeak {
    requires com.google.common;
    requires com.google.gson;
    requires org.eclipse.jetty.ee10.websocket.jetty.server;
    requires org.eclipse.jetty.websocket.client;
    requires static lombok;
    requires org.jetbrains.annotations;
    requires webrtc.java;

    exports net.betrayd.webspeak.v1.util;
    exports net.betrayd.webspeak.v1;
    exports net.betrayd.webspeak.event;
    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.math;
}
