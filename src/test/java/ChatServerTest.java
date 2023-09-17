import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import org.example.ChatServer;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ChatServerTest {
    ChatServer server;

    @Before
    public void setUp() {
        server = new ChatServer();
        server.loadServerSettings();
    }

    @Test
    public void testLoadServerSettings() {
        assertEquals(8083, server.PORT);
    }

    @Test
    public void testMessageBroadcast() throws IOException, InterruptedException {
        Thread serverThread = new Thread(() -> {
            server.startServer();
        });
        serverThread.start();
        Thread.sleep(1000);

        Socket clientSocket1 = new Socket("localhost", 8083);
        Socket clientSocket2 = new Socket("localhost", 8083);

        BufferedReader in1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
        PrintWriter out1 = new PrintWriter(clientSocket1.getOutputStream(), true);

        BufferedReader in2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
        PrintWriter out2 = new PrintWriter(clientSocket2.getOutputStream(), true);

        String clientName1 = "Бот1";
        String clientName2 = "Бот2";

        out1.println(clientName1);
        out2.println(clientName2);

        in1.readLine();
        in2.readLine();

        String message = "Привет, боты";
        out1.println(message);

        assertEquals(message, in1.readLine());
        assertEquals(message, in2.readLine());

        clientSocket1.close();
        clientSocket2.close();

        server.stopServer();
        assertFalse(server.isServerRunning.get());

        Thread.sleep(1000);
        serverThread.interrupt();
        assertTrue(serverThread.isInterrupted());
    }
}