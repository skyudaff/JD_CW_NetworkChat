package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {
    public static int PORT;
    private static final File SETTINGS = new File("src/main/resources/settings.txt");
    private static final String LOG_FILE = "src/main/resources/server.log";
    private static final Map<String, PrintWriter> clientWriters = new ConcurrentHashMap<>();
    private static final Set<String> clientNames = new CopyOnWriteArraySet<>();
    public static final AtomicBoolean isServerRunning = new AtomicBoolean(true);

    public static void main(String[] args) {
        loadServerSettings();
        startServer();
    }

    public static void loadServerSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS))) {
            String line = reader.readLine();
            PORT = Integer.parseInt(line.substring(line.indexOf(" ") + 1));
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

    public static void startServer() {
        Logger logger = new Logger(LOG_FILE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер работает по порту: " + PORT);

            while (isServerRunning.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(socket, logger).start();
                } catch (IOException e) {
                    if (isServerRunning.get()) {
                        System.err.println("Ошибка соединения с клиентом: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (isServerRunning.get()) {
                System.err.println("Ошибка запуска сервера: " + e.getMessage());
            }
        }

        for (PrintWriter writer : clientWriters.values()) {
            writer.println("Сервер завершает работу.");
            writer.close();
        }
    }

    public static void stopServer() {
        isServerRunning.set(false);
    }

    public static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private Logger logger;

        public ClientHandler(Socket socket, Logger logger) {
            this.socket = socket;
            this.logger = logger;
            out = null;
            in = null;
        }

        public void run() {
            boolean clientConnected = false;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                clientName = in.readLine();
                if (clientName == null || clientName.trim().isEmpty() || clientNames.contains(clientName)) {
                    out.println("Имя не задано или уже используется. \nВойдите под другим именем.");
                } else {
                    clientConnected = true;
                    out.println("* " + clientName + ", добро пожаловать в чат * (для выхода введите: /exit)");

                    System.out.println(clientName + " подключен");
                    logger.logMessage(clientName + " подключен");
                    broadcast(clientName + " подключен");
                    clientNames.add(clientName);

                    synchronized (clientWriters) {
                        clientWriters.put(clientName, out);
                    }

                    String message;
                    while ((message = in.readLine()) != null) {
                        if ("/exit".equalsIgnoreCase(message)) {
                            break;
                        }
                        System.out.println(message);
                        logger.logMessage(message);
                        broadcast(message);
                    }
                }
            } catch (SocketException e) {
                //  обработка разрыва соединения
            } catch (IOException e) {
                if (isServerRunning.get()) {
                    e.printStackTrace();
                }
            } finally {
                if (clientConnected) {
                    synchronized (clientWriters) {
                        clientWriters.remove(clientName);
                    }

                    System.out.println(clientName + " отключен");
                    logger.logMessage(clientName + " отключен");
                    broadcast(clientName + " отключен");
                    clientNames.remove(clientName);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(message);
                }
            }
        }
    }
}