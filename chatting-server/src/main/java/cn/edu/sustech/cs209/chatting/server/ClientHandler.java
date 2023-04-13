package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private String username;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<Message>> privateChatList = new ConcurrentHashMap<>();

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
//                System.out.println("Reading lines from client");
                CommMessage message = (CommMessage) in.readObject();
                int msgType = message.getType();
                if(msgType==0){ //post message
                    handlePost(message);
                } else if (msgType==1) { // get
                    handleGet(message);
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
                server.userNames.remove(username);
                server.socketList.remove(socket);

                System.out.println("Server connection closed");
                System.out.printf("Connected clients: %d\n",server.socketList.size());
                System.out.println(server.userNames);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public void handlePost(CommMessage msg) throws IOException {
        switch (msg.getMsg()){
            case "login":
                checkLogin(msg);
                break;
            case "postChat":
                postChat(msg);

        }
    }

    public void postChat(CommMessage msg) throws IOException {
        Message chat = msg.getChat();
        String chatTo = chat.getSendTo();
        if(!privateChatList.containsKey(chatTo)){
            privateChatList.put(chatTo,new CopyOnWriteArrayList<>());
        }
        privateChatList.get(chatTo).add(chat);

        System.out.println(chat.getData());
        CommMessage reply = new CommMessage(200,"chat");
//        reply.setChat(chat);
        out.writeObject(reply);
        out.flush();
    }

    private void checkLogin(CommMessage msg) throws IOException {
        String username = msg.getMsgList().get(0);
        CommMessage reply;
        System.out.println(username);
        if (!server.userNames.contains(username)){
            server.userNames.add(username);
            System.out.println("Logged in successful");
            this.username = username;
            reply = new CommMessage(200,"true");
            out.writeObject(reply);
            out.flush();
        }else{
            reply = new CommMessage(400,"duplicateName");
            out.writeObject(reply);
            out.flush();
        }

    }

    public void handleGet(CommMessage msg) throws IOException {
        switch (msg.getMsg()){
            case "getUsers":
                sendCurrentUsers();
                break;
            case "getChat":
                System.out.println("get chat");
                sendChat(msg); //have params
                break;
        }
    }

    public void sendChat(CommMessage msg) throws IOException{
        System.out.println(privateChatList);
        CommMessage reply = new CommMessage(0,"getChat");
        String from = msg.getMsgList().get(0);
        CopyOnWriteArrayList<Message> messages = privateChatList.get(from);
        System.out.println(messages);
        reply.setChats(messages);
        out.writeObject(reply);
        out.flush();
//        return messages;

    }
    public void sendCurrentUsers() throws IOException {
        CopyOnWriteArrayList<String> strings = new CopyOnWriteArrayList<>(server.userNames);
        CommMessage userList = new CommMessage(0, strings);
//        System.out.println(userList.getMsgList().size()+" "+ socket.getPort());
        out.writeObject(userList);
        out.flush();
    }
}
