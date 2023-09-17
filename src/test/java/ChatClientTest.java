import org.example.ChatClient;
import org.example.ChatServer;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class ChatClientTest {
    ChatServer server;
    ChatClient client;
    Thread serverThread;


    @Before
    public void setUp() throws InterruptedException {
        server = new ChatServer();
        serverThread = new Thread(() -> {
            server.loadServerSettings();
            server.startServer();
        });
        serverThread.start();
        Thread.sleep(1000);

        client = new ChatClient();
        client.loadClientSettings();

        assertEquals("localhost", client.SERVER_IP);
        assertEquals(8083, client.SERVER_PORT);
    }


    @Test
    public void testSendMessage() throws IOException {
        Socket socket = new Socket(ChatClient.SERVER_IP, ChatClient.SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        String name = "Бот";
        out.println(name);

        String message = "добро пожаловать в чат * (для выхода введите: /exit)";
        out.println(name + ", " + message);

        String response = in.readLine();

        assertEquals("* " + name + ", " + message, response);

        out.println("/exit");
    }
}