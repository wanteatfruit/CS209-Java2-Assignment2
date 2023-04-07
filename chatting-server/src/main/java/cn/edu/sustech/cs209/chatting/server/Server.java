package cn.edu.sustech.cs209.chatting.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    ServerSocket serverSocket;
    List<Socket> socketList = new ArrayList<>();
    List<String> userNames = new ArrayList<>();

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
            Thread t = new Thread(clientHandler);
            t.start();
            server.socketList.add(clientSocket);
            System.out.printf("Connected clients: %d\n",server.socketList.size());
        }
    }
}
