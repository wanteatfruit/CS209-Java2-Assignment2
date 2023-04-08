package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.CommMessage;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private String username;

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
        }
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
            //broadcast
//            for (ClientHandler handler:server.handlers){
//                handler.sendCurrentUsers();
//            }
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
        }
    }
    public void sendCurrentUsers() throws IOException {
        CopyOnWriteArrayList<String> strings = new CopyOnWriteArrayList<>(server.userNames);
        CommMessage userList = new CommMessage(0, strings);
        System.out.println(userList.getMsgList().size()+" "+ socket.getPort());
        out.writeObject(userList);
        out.flush();
    }
}
