package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private String username;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;

    private ArrayList<Set<String>> userChatPairs = new ArrayList<>();
//    private ArrayList<Set>
//    private ConcurrentHashMap<String, CopyOnWriteArrayList<Message>> privateChatList = new ConcurrentHashMap<>();

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
        //only private
        Set<String> pair = new HashSet<>();
        pair.add(this.username);
        pair.add(chatTo);
        if(!server.chatPairs.containsKey(pair)){
            server.chatPairs.put(pair, new ArrayList<>()); //new pair
            System.out.println("put new pair");
        }
        server.chatPairs.get(pair).add(chat);
        if(!userChatPairs.contains(pair)){
            userChatPairs.add(pair);
        }

        System.out.println(chat.getData());
        CommMessage reply = new CommMessage(200,"chat");
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
            case "getNewChatPerson":
//                System.out.println("check for new chat initiate");
                sendNewInit();
                break;
        }
    }

    public void sendNewInit() throws IOException {
        LinkedHashMap<Set<String>, ArrayList<Message>> currentChats =  new LinkedHashMap<>(server.chatPairs);
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>(); //new chats, usernames separated by comma
        for (Set<String> set: currentChats.keySet()) {
            if(set.contains(username) && !userChatPairs.contains(set)){ //new chat init from other user
                userChatPairs.add(set);
                StringBuilder toSend = new StringBuilder();
                for(String user:set){
                    if(user.equals(username)){
                        continue;
                    }
                    toSend.append(user); //only private
                }
                copyOnWriteArrayList.add(toSend.toString());
            }
        }
        CommMessage reply = new CommMessage(0,"return new chats");
        reply.setMsgList(copyOnWriteArrayList);
        System.out.println(reply.getMsgList());
        out.writeObject(reply);
        out.flush();
    }

    public void sendChat(CommMessage commMessage) throws IOException{
        LinkedHashMap<Set<String>, ArrayList<Message>> currentChats =  new LinkedHashMap<>(server.chatPairs);
        //private
        Set<String> currentChat = new HashSet<>();
        currentChat.add(username); // current
        currentChat.add(commMessage.getMsgList().get(0)); // other side
        ArrayList<Message> chats = currentChats.get(currentChat);
        if(chats==null){
            CommMessage reply = new CommMessage(0,"null messages");
            out.writeObject(reply);
            out.flush();
            return;
        }
        CopyOnWriteArrayList<Message> copyOnWriteArrayList = new CopyOnWriteArrayList<>(chats);
        CommMessage reply = new CommMessage(0,"reply send chat");
        reply.setChats(copyOnWriteArrayList);
        out.writeObject(reply);
        out.flush();

    }
    public void sendCurrentUsers() throws IOException {
        CopyOnWriteArrayList<String> strings = new CopyOnWriteArrayList<>(server.userNames);
        CommMessage userList = new CommMessage(0, strings);
//        System.out.println(userList.getMsgList().size()+" "+ socket.getPort());
        out.writeObject(userList);
        out.flush();
    }
}
