package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;

import javax.swing.plaf.synth.SynthEditorPaneUI;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private String username;
    private String password;

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
//            e.printStackTrace();
//                throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
                server.userNames.remove(username);
                server.socketList.remove(socket);

                System.out.println("Server connection closed");
                System.out.printf("Connected clients: %d\n",server.socketList.size());
                System.out.println(server.userNames);
            } catch (IOException e) {
//                throw new RuntimeException(e);
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
                break;
            case "postGroupChat":
                postGroupChat(msg);
                break;
            case "postFile":
                postFile(msg);
                break;
            case "register":
                register(msg);
                break;
        }
    }

    public void register(CommMessage msg) throws IOException {
        CopyOnWriteArrayList<String> params = msg.getMsgList();
        String username = params.get(0);
        String pw= params.get(1);
        if(!server.userPW.containsKey(username)){
            server.userPW.put(username,pw);
        }
        CommMessage message = new CommMessage(200,"ok");
        out.writeObject(message);
        out.flush();
    }

    public void postFile(CommMessage msg) throws IOException {
        byte[] fileBytes = msg.getFileBytes();
        System.out.println("Storing file");
        System.out.println(System.getProperty("user.dir"));
//        String dir = "files/"+msg.getMsgList().get(1);
        File dir = new File("files/"+msg.getMsgList().get(1));
        if(!dir.exists()){
            boolean result = dir.mkdir();
        }
        File file = new File(dir+"/"+msg.getMsgList().get(0));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(fileBytes);
        fileOutputStream.close();
        CommMessage reply = new CommMessage(200,"chat");

        out.writeObject(reply);
        out.flush();
    }

    public void postGroupChat(CommMessage msg) throws IOException {
        Message chat = msg.getChat();
        //format send to as comma separation
        String chatTo = chat.getSendTo();
        String[] groupMembers = chatTo.split(", ");
        Set<String> pair = new HashSet<>();
        pair.add(username);
        pair.addAll(Arrays.asList(groupMembers));

        if(!server.chatPairs.containsKey(pair)){
            server.chatPairs.put(pair, new ArrayList<>()); //new pair
            System.out.println("put new group");
        }
        server.chatPairs.get(pair).add(chat);
        if(!userChatPairs.contains(pair)){
            userChatPairs.add(pair);
        }
        CommMessage reply = new CommMessage(200,"chat");
        out.writeObject(reply);
        out.flush();
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
        CopyOnWriteArrayList<String> userNames = new CopyOnWriteArrayList<>(server.userNames);
        String username = msg.getMsgList().get(0);
        String pw = msg.getMsgList().get(1);
        CommMessage reply;
        System.out.println(username);
        if (!userNames.contains(username)){

            if(server.userPW.get(username)==null ||  !server.userPW.get(username).equals(pw)){
                reply = new CommMessage(400,"wrongPassword");
                out.writeObject(reply);
                out.flush();
                return;
            }
            server.userNames.add(username);
            System.out.println("Logged in successful");
            this.username = username;
            reply = new CommMessage(200,"true");
        }else{
            reply = new CommMessage(400,"duplicateName");
        }
        out.writeObject(reply);
        out.flush();

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
                case "getNewChatGroup":
                    sendNewGroupInit();
                    break;
            case "getGroupChat":
                sendGroupChat(msg);
                break;
            case "getFile":
                sendFile(msg);
                break;
        }
    }

    public void sendFile(CommMessage comm) throws IOException {

        File dir = new File("files/"+comm.getMsgList().get(1)+"/"+comm.getMsgList().get(0));
        System.out.println(dir);
        if(dir.exists()){
            CommMessage reply = new CommMessage(0,"ok");
            reply.setFileBytes(dir.toPath());
            out.writeObject(reply);
            out.flush();
        }
    }

    public void sendNewGroupInit() throws IOException {
        LinkedHashMap<Set<String>, ArrayList<Message>> currentChats =  new LinkedHashMap<>(server.chatPairs);
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>(); //new chats, usernames separated by comma
        for (Set<String> set: currentChats.keySet()) {
            if(set.contains(username) && !userChatPairs.contains(set) && set.size()>2){ //new chat init from other user
                userChatPairs.add(set);
                StringBuilder toSend = new StringBuilder();
                for(String user:set){
                    toSend.append(user).append(", ");
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


    public void sendGroupChat(CommMessage commMessage) throws IOException{
        LinkedHashMap<Set<String>, ArrayList<Message>> currentChats =  new LinkedHashMap<>(server.chatPairs);
        //private
        Set<String> currentChat = new HashSet<>();
        currentChat.add(username); // current
        CopyOnWriteArrayList<String> groupMembers = commMessage.getMsgList();
        currentChat.addAll(groupMembers);
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

    public void sendNewInit() throws IOException {
        LinkedHashMap<Set<String>, ArrayList<Message>> currentChats =  new LinkedHashMap<>(server.chatPairs);
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>(); //new chats, usernames separated by comma
        for (Set<String> set: currentChats.keySet()) {
            if(set.contains(username) && !userChatPairs.contains(set) && set.size()==2){ //new chat init from other user
                userChatPairs.add(set);
                StringBuilder toSend = new StringBuilder();
                for(String user:set){
                    if(user.equals(username)){
                        continue;
                    }
                    toSend.append(user);
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
