package net.betrayd.webspeak.v1.impl.jetty;

import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;

import net.betrayd.webspeak.v1.WebSpeakServer;

public class PlayerWSConnectionServlet extends JettyWebSocketServlet {

    private final WebSpeakServer server;

    public PlayerWSConnectionServlet(WebSpeakServer server) {
        this.server = server;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        // TODO: can we do player validation here?
        factory.setCreator((req, resp) -> {
            return new PlayerWSConnection(server);
        });
    }
    
}
