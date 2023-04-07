package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommMessage implements Serializable {
     int type=0;

     int code;
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

    public void setCode(int code) {
        this.code = code;
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

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setMsgList(ArrayList<String> msgList) {
        this.msgList = msgList;
    }

    public void setType(int type) {
        this.type = type;
    }
}
