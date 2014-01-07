package org.goblin.endpoint;

import org.goblin.builder.GoblinCommanderBuilder;
import org.goblin.commander.GoblinCommander;
import org.goblin.dto.ProcessContext;
import org.goblin.exception.CommandExecuteException;
import org.goblin.exception.CommandNotFoundException;
import org.jmotor.util.CloseableUtilities;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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
    private static final GoblinCommander GOBLIN_COMMANDER = GoblinCommanderBuilder.newBuilder().build();
    private static final Set<CommandConsoleEndpoint> CONNECTIONS = new CopyOnWriteArraySet<>();
    private Session session;
    private ProcessContext processContext;

    @OnOpen
    public void open(Session session, EndpointConfig config) {
        this.session = session;
        this.processContext = new ProcessContext();
        CONNECTIONS.add(this);
    }

    @OnClose
    public void close(CloseReason reason) {
        CONNECTIONS.remove(this);
    }

    @OnError
    public void error(Throwable error) {
    }

    @OnMessage
    public void message(String message) {
        List<RemoteEndpoint.Basic> clients = new ArrayList<>(CONNECTIONS.size());
        for (CommandConsoleEndpoint client : CONNECTIONS) {
            clients.add(client.session.getBasicRemote());
        }
        BufferedReader reader = null;
        try {
            processContext = GOBLIN_COMMANDER.execute(processContext, message);
            Process process = processContext.getProcess();
            InputStream inputStream = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                broadcast(clients, line);
            }
            int exitValue = process.waitFor();
            if (0 == exitValue) {
                broadcast(clients, "successful");
            } else {
                broadcast(clients, "failure");
            }
        } catch (CommandNotFoundException e) {
            broadcast(clients, message + ": command not fund");
        } catch (CommandExecuteException | IOException | InterruptedException e) {
            e.printStackTrace();
            broadcast(clients, e.getLocalizedMessage());
        } finally {
            CloseableUtilities.closeQuietly(reader);
        }
    }

    private void broadcast(List<RemoteEndpoint.Basic> clients, String message) {
        try {
            for (RemoteEndpoint.Basic client : clients) {
                synchronized (client) {
                    client.sendText(message);
                }
            }
        } catch (IOException e) {
            //ignore
        }
    }
}
