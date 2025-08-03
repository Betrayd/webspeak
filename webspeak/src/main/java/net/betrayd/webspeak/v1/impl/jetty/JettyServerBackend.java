package net.betrayd.webspeak.v1.impl.jetty;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;

import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.ServerBackend;

public class JettyServerBackend implements ServerBackend {

    private final WebSpeakServer webSpeakServer;
    private Server jettyServer;
    private int port = -1;

    public JettyServerBackend(WebSpeakServer webSpeakServer) {
        this.webSpeakServer = webSpeakServer;
    }

    @Override
    public WebSpeakServer getServer() {
        return webSpeakServer;
    }

    @Override
    public void start(int port) throws Exception {
        if (jettyServer != null) {
            throw new IllegalStateException("Server has already started.");
        }
        this.port = port;
        var server = jettyServer = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        JettyWebSocketServletContainerInitializer.configure(context, null);
        ServletHolder wsHolder = new ServletHolder("connect", new PlayerWSConnectionServlet(webSpeakServer));
        context.addServlet(wsHolder, "/connect");

        // Assign app at the end for thread safety; if something tries to access the app
        // before it's done initializing, it will return null.
        server.start();
        jettyServer = server;
    }

    @Override
    public void stop() throws Exception {
        jettyServer.stop();
    }

    @Override
    public boolean isRunning() {
        return jettyServer != null && jettyServer.isRunning();
    }

    @Override
    public int getPort() {
        return port;
    }
    
}
