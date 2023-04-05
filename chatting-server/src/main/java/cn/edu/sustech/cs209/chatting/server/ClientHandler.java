package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    private Server server;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {

        try {
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream());
            while (true){
                String line = in.nextLine();
                System.out.println("Client sent "+line);
            }
        } catch (IOException e) {
            e.printStackTrace();
//                throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
                server.socketList.remove(socket);
                System.out.println("Server connection closed");
                System.out.printf("Connected clients: %d\n",server.socketList.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
