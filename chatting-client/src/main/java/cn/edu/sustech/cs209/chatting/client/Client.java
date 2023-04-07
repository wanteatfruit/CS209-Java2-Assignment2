package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.CommMessage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client implements Runnable{
    private Socket socket= null;
    private ObjectInputStream fromServer;
    private InputStream inputStream;
    private ObjectOutputStream toServer;
    private Controller controller;

    public Client(Socket s,Controller c) throws IOException {
        socket=s;
        controller=c;
        System.out.println("Initializing client");
        System.out.println("get stream" );
        inputStream=socket.getInputStream();
//        fromServer = new ObjectInputStream(stream);
        toServer = new ObjectOutputStream(socket.getOutputStream());

        System.out.println("Connected to server");
    }

    public boolean postLogin(String username) throws IOException, ClassNotFoundException {

        System.out.println("Logging in "+username);
        CommMessage login = new CommMessage(0,"login");
        ArrayList<String> list = new ArrayList<>();
        list.add(username);
        login.setMsgList(list);
        toServer.writeObject(login);
        toServer.flush();
        System.out.println("Sending login post request");
        fromServer = new ObjectInputStream(inputStream);
        CommMessage msg = (CommMessage) fromServer.readObject();
        return msg.getType() == 200;
    }

    public List<String> getCurrentUsers() throws IOException, ClassNotFoundException {
        CommMessage getUsers = new CommMessage(1,"getUsers");
        toServer.writeObject(getUsers);
        toServer.flush();
        System.out.println("Sending get all user request");
        CommMessage reply = (CommMessage) fromServer.readObject();
        reply.getMsgList().forEach(System.out::println);
        return reply.getMsgList();
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
