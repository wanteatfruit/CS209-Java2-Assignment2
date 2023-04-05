package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{
    private Socket socket= null;
    private PrintWriter out = null;
    private Scanner in;
    private Controller controller;

    public Client(Socket s,Controller c) throws IOException {
        socket=s;
        controller=c;
        in = new Scanner(s.getInputStream());
        out = new PrintWriter(s.getOutputStream());
        System.out.println("Connected to server");

    }
    @Override
    public void run() {
        while (true){
            if(controller.username!=null){
                System.out.println(controller.username);
                break;
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("client thread disconnected");

    }
}
