package org.example;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ChatClient {
    public static final String SERVER_IP = "localhost";
    public static int SERVER_PORT;
    public static final File SETTINGS = new File("src/main/resources/settings.txt");
    private static final String LOG_FILE = "src/main/resources/client.log";

    public static void main(String[] args) {
        loadClientSettings();
        startClient();
    }

    public static void loadClientSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS))) {
            String line = reader.readLine();
            SERVER_PORT = Integer.parseInt(line.substring(line.indexOf(" ") + 1));
        } catch (NumberFormatException e) {
            System.out.println("Порт не найден");
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.out.println("Не найден файл: \"" + SETTINGS + "\"");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void startClient() {
        Logger logger = new Logger(LOG_FILE);
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Введите имя для входа: ");
            String name = scanner.nextLine();
            out.println(name);

            Thread receiverThread = new Thread(() -> {
                String message;
                try {
                    while ((message = in.readLine()) != null ||
                            !"Имя не задано или уже используется".contains(message)) {
                        if (!message.startsWith(name + ": ")) {
                            System.out.println(message);
                        }
                    }
                } catch (NullPointerException e) {
                    System.exit(1);
                } catch (IOException e) {
                    System.err.println("Соединение разорвано сервером");
                    System.exit(1);
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            });
            receiverThread.start();

            Thread senderThread = new Thread(() -> {
                String input;
                while (true) {
                    input = scanner.nextLine();
                    if ("/exit".equalsIgnoreCase(input)) {
                        out.println("/exit");
                        out.flush();
                        break;
                    }
                    out.println(name + ": " + input);
                    logger.logMessage(name + ": " + input);
                    out.flush();
                }
            });
            senderThread.start();

            senderThread.join();
            receiverThread.join();
        } catch (
                UnknownHostException e) {
            System.err.println("Сервер не найден");
            System.exit(1);
        } catch (
                IOException e) {
            System.err.println("Сервер недоступен");
            System.exit(1);
        } catch (
                InterruptedException e) {
            e.printStackTrace();
        }
    }
}