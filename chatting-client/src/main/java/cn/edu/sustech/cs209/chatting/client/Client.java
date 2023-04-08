package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client implements Runnable{
    private Socket socket= null;
    private ObjectInputStream fromServer;
    private InputStream inputStream;
    private ObjectOutputStream toServer;
    private Controller controller;

    int currentUser;

    public Client(Socket s,Controller c) throws IOException {
        socket=s;
        controller=c;
        System.out.println("Initializing client");
        System.out.println("get stream" );
        inputStream=socket.getInputStream();
//        fromServer = new ObjectInputStream(stream);
        toServer = new ObjectOutputStream(socket.getOutputStream());
        fromServer = new ObjectInputStream(inputStream);

        System.out.println("Connected to server");
    }

    public boolean postLogin(String username) throws IOException, ClassNotFoundException {

        System.out.println("Logging in "+username);
        CommMessage login = new CommMessage(0,"login");
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add(username);
        login.setMsgList(list);
        toServer.writeObject(login);
        toServer.flush();
        System.out.println("Sending login post request");
//        fromServer = new ObjectInputStream(inputStream);
        CommMessage msg = (CommMessage) fromServer.readObject();

        return msg.getType() == 200;
    }

    public List<String> getCurrentUsers() throws IOException, ClassNotFoundException {
        CommMessage getUsers = new CommMessage(1,"getUsers");
        toServer.writeObject(getUsers);
        toServer.flush();
//        System.out.println("Sending get all user request");
        CommMessage reply = (CommMessage) fromServer.readObject();
//        reply.getMsgList().forEach(System.out::println);
        return reply.getMsgList();
    }

    public boolean postChat(String chat) throws IOException, ClassNotFoundException {
        System.out.println("Sending chat message "+chat);
        CommMessage message = new CommMessage(0,"postChat");
        Message chatMessage = new Message(new Date().getTime(), controller.username, "",chat);
        message.setChat(chatMessage);
        toServer.writeObject(message);
        toServer.flush();
        fromServer = new ObjectInputStream(inputStream);
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getType()==200;
    }

    public List<String> getChat() throws IOException, ClassNotFoundException {
        CommMessage getChat = new CommMessage(1,"getChat");
        toServer.writeObject(getChat);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
        Message chat = reply.getChat();
        reply.getMsgList().forEach(System.out::println);
        return reply.getMsgList();
    }
    @Override
    public void run() {
        while (true){
            try {
                List<String> users = getCurrentUsers();
                this.currentUser = users.size();
                Thread.sleep(1000);
                System.out.println(currentUser);
                controller.currentOnlineCnt.setText(String.valueOf(currentUser));
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }


    }
}
