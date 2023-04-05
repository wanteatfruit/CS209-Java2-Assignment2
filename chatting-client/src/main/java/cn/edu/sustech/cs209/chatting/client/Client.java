package cn.edu.sustech.cs209.chatting.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{
    private Socket socket= null;
    private PrintWriter out = null;
    private Scanner in;
    private Main app;

    public Client(Socket s,Main m){
        socket=s;
        app=m;
        System.out.println("Connected to server");
    }
    @Override
    public void run() {

    }
}
