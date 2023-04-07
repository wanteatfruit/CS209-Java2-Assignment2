package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new ObjectInputStream(socket.getInputStream());
        this.out = new ObjectOutputStream(socket.getOutputStream()) ;
    }

    @Override
    public void run() {
        System.out.println("Client connected: " + socket.toString());
        try {

            while (true){
                System.out.println("Reading lines from client");
                CommMessage message = (CommMessage) in.readObject();
                System.out.println("Client sent "+message.getMsg());
            }
        } catch (IOException e) {
            e.printStackTrace();
//                throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
                server.socketList.remove(socket);
                System.out.println("Server connection closed");
                System.out.printf("Connected clients: %d\n",server.socketList.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
