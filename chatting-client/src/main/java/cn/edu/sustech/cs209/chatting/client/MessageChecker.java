package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;

public class MessageChecker extends Thread {
    private final Controller controller;
    private Client client;
    private final String selectedUser;

    public MessageChecker(Controller controller, String selectedUser, Client client) {
        this.controller = controller;
        this.selectedUser = selectedUser;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Wait for 5 seconds before checking for new messages
                Thread.sleep(5000);

                // Query the server for new messages
                List<Message> newMessages = client.getChat(selectedUser);
                if (newMessages != null) {
                    for (Message message : newMessages) {
                        Platform.runLater(() -> controller.receiveMessage(message));
                    }
                }
                // Update the UI with any new messages

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
