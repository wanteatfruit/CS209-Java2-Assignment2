package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    ServerSocket serverSocket;
    List<Socket> socketList = new ArrayList<>();
    CopyOnWriteArrayList<String> userNames = new CopyOnWriteArrayList<>();
//    List<Set<String>> chatPairs = new ArrayList<>();
    LinkedHashMap<Set<String>, ArrayList<Message>> chatPairs = new LinkedHashMap<>();
    List<ClientHandler> handlers = new ArrayList<>();

    public void startServer() throws IOException {

    }

    public Server() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        Server server = new Server();
//        server.startServer();
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Waiting for clients to connect" );
        while (true){
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket,server);
            server.handlers.add(clientHandler);
            Thread t = new Thread(clientHandler);
            t.start();
            server.socketList.add(clientSocket);
            System.out.printf("Connected clients: %d\n",server.socketList.size());
        }
    }
}
