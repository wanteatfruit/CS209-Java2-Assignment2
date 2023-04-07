package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.CommMessage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client implements Runnable{
    private Socket socket= null;
    private ObjectInputStream fromServer;
    private ObjectOutputStream toServer;
    private Controller controller;

    public Client(Socket s,Controller c) throws IOException {
        socket=s;
        controller=c;
        System.out.println("Initializing client");
        System.out.println("get stream" );
        InputStream stream=socket.getInputStream();
//        fromServer = new ObjectInputStream(stream);
        toServer = new ObjectOutputStream(socket.getOutputStream());

        System.out.println("Connected to server");
//        String currentuser = fromServer.readUTF();
//        System.out.println(currentuser);
    }

    public void login(String username) throws IOException {

        System.out.println(this.socket);
        System.out.println("Logging in "+username);
//        toServer.writeUTF(username);
        CommMessage login = new CommMessage(0,username);
        toServer.writeObject(login);
        toServer.flush();
        System.out.println("Sending complete");
    }
    @Override
    public void run() {
        while (true){
            if(controller.username!=null){
                System.out.println(controller.username);
                break;
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("client thread disconnected");

    }
}
