package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    @FXML
    Label currentUsername;

    @FXML
    Label currentOnlineCnt;

    @FXML
    TextArea inputArea;

    @FXML
    ListView<String> chatList;

    String username;
    Client client;

    private UpdateCheckService service;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new UpdateCheckService();
        try {
            Socket socket = new Socket("localhost", 5000);
            client = new Client(socket, this);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Called initialize");
        System.out.println(this);
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();


        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();
            try {
                boolean canLogin = client.postLogin(username);
                if (!canLogin) {
                    System.out.println("Invalid username " + input + ", exiting");
                    Platform.exit();
                }

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        currentUsername.setText(username);
        chatContentList.setCellFactory(new MessageCellFactory());




        service.setOnSucceeded(e->{
            try {
//                System.out.println(String.valueOf(client.getCurrentUsers().size()));
                currentOnlineCnt.setText(String.valueOf(client.getCurrentUsers().size()));
//                Message chat = client.getChat();
//                System.out.println(chat.getData());
            } catch (IOException ex) {
//                throw new RuntimeException(ex);
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        });
        service.start();



    }

    @FXML
    public void createPrivateChat() throws IOException, ClassNotFoundException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out

        List<String> currentUsers = client.getCurrentUsers();
        currentUsers.remove(username);
        System.out.println(currentUsers);
        userSel.getItems().removeAll();
        userSel.getItems().addAll(currentUsers);

        Button okBtn = new Button("OK");
        AtomicReference<String> selected = new AtomicReference<>("");
        okBtn.setOnAction(e -> {
            selected.set(userSel.getSelectionModel().getSelectedItem());
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if (chatList.getItems().contains(selected.get())) {
            chatList.getSelectionModel().select(selected.get());

        } else {
            chatList.getItems().add(selected.get());
            chatList.getSelectionModel().select(selected.get());
        }

    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException, ClassNotFoundException {
        String to = chatList.getSelectionModel().getSelectedItem();
        if (to == null) {
            // Display warning message
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No chat selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a chat to send a message.");
            alert.showAndWait();
            return;
        }

        String txt = inputArea.getText().trim();
        if (txt.equals("")) {
            // Display warning message
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Empty message");
            alert.setHeaderText(null);
            alert.setContentText("No blank text allowed.");
            alert.showAndWait();
            return;
        }

        Message message = new Message(System.currentTimeMillis(), username, to, txt);
        client.postChat(message);
        chatContentList.getItems().add(message);
        inputArea.clear();
        // Start the message checker thread
//        MessageChecker messageChecker = new MessageChecker(this, "to",client);
//        messageChecker.start();

    }

    public void receiveMessage(Message message){
        chatContentList.getItems().add(message);
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private static class UpdateCheckService extends ScheduledService<Boolean>{

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    updateMessage("Checking for updates");
                    return Math.random() <0.01;
                }
            };
        }
    }
}
