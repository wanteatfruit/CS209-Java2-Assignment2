package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
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
                int msgType = message.getType();
                if(msgType==0){ //login message
                    String username= message.getMsg();
                    System.out.println("Client's username is "+username);
                    // send userlist to client
                    CommMessage userList = new CommMessage(1, (ArrayList<String>) server.userNames);
                    out.writeObject(userList);
                    out.flush();
                    server.userNames.add(username);
                }
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
