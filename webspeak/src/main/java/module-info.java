module net.betrayd.webspeak {
    requires static lombok;
    requires org.jetbrains.annotations;
    requires org.eclipse.jetty.websocket.client;
    requires com.google.gson;
    requires com.google.common;

    //TODO: restrict this, (needs to be there to start because of reflective access)
    exports net.betrayd.webspeak.impl.relay;

    exports net.betrayd.webspeak;
    exports net.betrayd.webspeak.event;
    exports net.betrayd.webspeak.math;
}