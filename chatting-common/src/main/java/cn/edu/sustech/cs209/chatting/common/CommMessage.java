package cn.edu.sustech.cs209.chatting.common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommMessage implements Serializable {
     int type=0;

     String msg=null;
     CopyOnWriteArrayList<String> msgList=new CopyOnWriteArrayList<>();

     CopyOnWriteArrayList<Message> chats = new CopyOnWriteArrayList<>();
     Message chat= null;

     byte[] fileBytes;

    public CommMessage(int type, String msg){
        this.type = type ;
        this.msg = msg;
    }
    public CommMessage(int type,CopyOnWriteArrayList<String> msgList){
        this.msgList = msgList;
        this.type = type;
    }



    public CopyOnWriteArrayList<String> getMsgList() {
        return msgList;
    }

    public CopyOnWriteArrayList<Message> getChats() {
        return chats;
    }



    public void setChats(CopyOnWriteArrayList<Message> chats) {
        this.chats = chats;
    }

    public String getMsg() {
        return msg;
    }

    public void setChat(Message chat) {
        this.chat = chat;
    }

    public Message getChat() {
        return chat;
    }

    public void setFileBytes(Path path) throws IOException {
        fileBytes = Files.readAllBytes(path);
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public int getType() {
        return type;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setMsgList(CopyOnWriteArrayList<String> msgList) {
        this.msgList = msgList;
    }

    public void setType(int type) {
        this.type = type;
    }
}
