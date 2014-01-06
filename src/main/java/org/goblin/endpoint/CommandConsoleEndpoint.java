package org.goblin.endpoint;

import org.goblin.builder.GoblinCommanderBuilder;
import org.goblin.commander.GoblinCommander;
import org.goblin.dto.ProcessContext;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Component:
 * Description:
 * Date: 14-1-6
 *
 * @author Andy Ai
 */
@ServerEndpoint("/console")
public class CommandConsoleEndpoint {
    private Session session;
    private static final ProcessContext PROCESS_CONTEXT = new ProcessContext();
    private static final GoblinCommander GOBLIN_COMMANDER = GoblinCommanderBuilder.newBuilder().build();
    private static final Set<CommandConsoleEndpoint> CONNECTIONS = new CopyOnWriteArraySet<>();

    @OnOpen
    public void open(Session session, EndpointConfig config) {
        this.session = session;
        CONNECTIONS.add(this);
    }

    @OnClose
    public void close(CloseReason reason) {
    }

    @OnError
    public void error(Throwable error) {
    }

    @OnMessage
    public void message(String message) {
        broadcast("I love you, message: " + message);
    }

    private void broadcast(String message) {
        for (CommandConsoleEndpoint client : CONNECTIONS) {
            client.session.getAsyncRemote().sendText(message);
        }
    }
}
