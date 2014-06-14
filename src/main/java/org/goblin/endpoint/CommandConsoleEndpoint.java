package org.goblin.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.goblin.builder.GoblinCommanderBuilder;
import org.goblin.commander.GoblinCommander;
import org.goblin.dto.ProcessContext;
import org.goblin.dto.Result;
import org.goblin.model.ResultModel;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private ProcessContext processContext;

    static {
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    @OnOpen
    public void open(EndpointConfig config) {
        this.processContext = new ProcessContext();
    }

    @OnClose
    public void close(Session session, CloseReason reason) {
        try {
            session.close(reason);
        } catch (IOException e) {
            // ignore
        }
    }

    @OnError
    public void error(Throwable error) {
        error.printStackTrace();
    }

    @OnMessage
    public void message(Session session, String message) {
        try {
            Result result = GOBLIN_COMMANDER.execute(processContext, message);
            Process process = result.getProcess();
            if (process != null) {
                handleProcess(session.getBasicRemote(), process);
            } else {
                ResultModel resultModel = new ResultModel();
                resultModel.setSpeech(result.getSpeech());
                resultModel.setPrint(result.getPrint());
                resultModel.setForward(result.getForward());
                resultModel.setNext(result.getNext());
                resultModel.setSearch(result.getSearch());
                broadcast(session.getBasicRemote(), resultModel);
            }
        } catch (Exception e) {
            ResultModel resultModel = new ResultModel();
            resultModel.setSpeech("I have a trouble, please try a later.");
            broadcast(session.getBasicRemote(), resultModel);
        }
    }

    private void handleProcess(RemoteEndpoint.Basic client, Process process) {
        BufferedReader reader = null;
        try {
            InputStream inputStream = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                ResultModel resultModel = new ResultModel();
                resultModel.setPrint(line);
                broadcast(client, resultModel);
            }
            int exitValue = process.waitFor();
            if (0 == exitValue) {
                ResultModel resultModel = new ResultModel();
                resultModel.setPrint("successful");
                broadcast(client, resultModel);
            } else {
                ResultModel resultModel = new ResultModel();
                resultModel.setSpeech("Sorry, My hands are tied");
                broadcast(client, resultModel);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            ResultModel resultModel = new ResultModel();
            resultModel.setSpeech("Sorry, My hands are tied");
            broadcast(client, resultModel);
        } finally {
            CloseableUtilities.closeQuietly(reader);
        }
    }

    private void broadcast(RemoteEndpoint.Basic client, ResultModel result) {
        try {
            String message = OBJECT_MAPPER.writeValueAsString(result);
            client.sendText(message);
        } catch (IOException e) {
            //ignore
        }
    }
}
