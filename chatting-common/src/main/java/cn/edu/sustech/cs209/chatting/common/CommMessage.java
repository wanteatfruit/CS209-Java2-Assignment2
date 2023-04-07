package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommMessage implements Serializable {
     int type=0;
     String msg=null;
     ArrayList<String> msgList=new ArrayList<>();

    public CommMessage(int type, String msg){
        this.type = type ;
        this.msg = msg;
    }
    public CommMessage(int type,ArrayList<String> msgList){
        this.msgList = msgList;
        this.type = type;
    }

    public List<String> getMsgList() {
        return msgList;
    }

    public String getMsg() {
        return msg;
    }

    public int getType() {
        return type;
    }
}
