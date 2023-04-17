package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;
import sun.jvm.hotspot.gc.z.ZPageAllocator;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client implements Runnable{
    public static final String DELIMETER = ", ";
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

    public boolean postLogin(String username,String password) throws IOException, ClassNotFoundException {

        System.out.println("Logging in "+username);
        CommMessage login = new CommMessage(0,"login");
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add(username);
        list.add(password);
        login.setMsgList(list);
        toServer.writeObject(login);
        toServer.flush();
        System.out.println("Sending login post request");
        CommMessage msg = (CommMessage) fromServer.readObject();

        return msg.getType() == 200;
    }

    public void registerUser(String username,String password) throws IOException, ClassNotFoundException {
        System.out.println("Registering");
        CommMessage register = new CommMessage(0,"register");
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add(username);
        list.add(password);
        register.setMsgList(list);
        toServer.writeObject(register);
        toServer.flush();
        CommMessage msg = (CommMessage) fromServer.readObject();
        return;
    }


    public List<String> getCurrentUsers() throws IOException, ClassNotFoundException {
        CommMessage getUsers = new CommMessage(1,"getUsers");
        toServer.writeObject(getUsers);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getMsgList();
    }
    public CommMessage checkNewChat() throws IOException, ClassNotFoundException {
        CommMessage checkChat = new CommMessage(1,"getNewChatPerson");

        toServer.writeObject(checkChat);
        toServer.flush();
        return (CommMessage) fromServer.readObject();
    }

    public CommMessage checkNewGroupChat() throws IOException, ClassNotFoundException {
        CommMessage check = new CommMessage(1,"getNewChatGroup");
        toServer.writeObject(check);
        toServer.flush();
        return (CommMessage) fromServer.readObject();
    }

    public boolean postChat(Message chat) throws IOException, ClassNotFoundException {
        CommMessage message = new CommMessage(0,"postChat");
        message.setChat(chat);
        toServer.writeObject(message);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getType()==200;
    }

    public boolean sendFile(File file) throws IOException, ClassNotFoundException {
        CommMessage message = new CommMessage(0,"postFile");
        if(file!=null){
            System.out.println("Sending file");
            message.setFileBytes(file.toPath());
            toServer.writeObject(message);
            toServer.flush();
        }
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getType()==200;
    }

    public byte[] getFile(String path) throws IOException, ClassNotFoundException {
        CommMessage message = new CommMessage(0,"getFile");
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        copyOnWriteArrayList.add(path);
        message.setMsgList(copyOnWriteArrayList);
        toServer.writeObject(message);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getFileBytes();
    }

    public CopyOnWriteArrayList<Message> getChat(String from) throws IOException, ClassNotFoundException {
        CommMessage getChat = new CommMessage(1,"getChat");
        CopyOnWriteArrayList<String> params = new CopyOnWriteArrayList<>();
        params.add(from);
        getChat.setMsgList(params);
        toServer.writeObject(getChat);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
//        System.out.println(reply.getChats());
        return reply.getChats();
    }

    public CopyOnWriteArrayList<Message> getGroupChat(String group) throws IOException, ClassNotFoundException {
        CommMessage getChat = new CommMessage(1,"getGroupChat");
        //group separated by commas+space
        String[] userNames = group.split(DELIMETER);
        CopyOnWriteArrayList<String> users = new CopyOnWriteArrayList<>(Arrays.asList(userNames));
        getChat.setMsgList(users);
        toServer.writeObject(getChat);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
//        System.out.println(reply.getChats());
        return reply.getChats();

    }

    public boolean postGroupChat(Message chat) throws IOException, ClassNotFoundException {
        CommMessage message = new CommMessage(0,"postGroupChat");
        message.setChat(chat);
        toServer.writeObject(message);
        toServer.flush();
        CommMessage reply = (CommMessage) fromServer.readObject();
        return reply.getType()==200;
    }

    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        inputStream=socket.getInputStream();
//        fromServer = new ObjectInputStream(stream);
        toServer = new ObjectOutputStream(socket.getOutputStream());
        fromServer = new ObjectInputStream(inputStream);
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
