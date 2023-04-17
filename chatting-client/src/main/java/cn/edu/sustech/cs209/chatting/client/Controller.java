package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.CommMessage;
import cn.edu.sustech.cs209.chatting.common.Message;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

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
    String pw;
    Client client;

    HashMap<String, ArrayList<Message>> allChats = new LinkedHashMap<>();

    private UpdateCheckService service;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println(System.getProperty("user.dir")); //relative path is chatting-client
        service = new UpdateCheckService();
        try {
            Socket socket = new Socket("localhost", 5000);
            client = new Client(socket, this);

        } catch (IOException e) {
//            throw new RuntimeException(e);
            System.out.println("The server is not started");
            // Display warning message
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("The server is not started");
            alert.setHeaderText(null);
            alert.setContentText("The server is not started");
            alert.showAndWait();
            System.exit(0);
        }

        System.out.println("Called initialize");
        System.out.println(this);





        Dialog<Pair<String, String>> registerDialog = new Dialog<>();
        registerDialog.setTitle("Register");
        registerDialog.setHeaderText(null);

// Set the button types
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        registerDialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

// Create the username and password labels and fields
        GridPane registerPane = new GridPane();
        registerPane.setHgap(10);
        registerPane.setVgap(10);
        registerPane.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        registerPane.add(new Label("Username:"), 0, 0);
        registerPane.add(usernameField, 1, 0);
        registerPane.add(new Label("Password:"), 0, 1);
        registerPane.add(passwordField, 1, 1);

        registerDialog.getDialogPane().setContent(registerPane);

// Convert the result to a username-password-pair when the register button is clicked
        registerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return new Pair<>(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = registerDialog.showAndWait();

        result.ifPresent(usernamePassword -> {
            try {
                client.registerUser(result.get().getKey(),result.get().getValue());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        ButtonType loginButton = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButton, ButtonType.CANCEL);


        GridPane loginPane = new GridPane();
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(20, 150, 10, 10));

        TextField loginUsername = new TextField();
        usernameField.setPromptText("Username");
        PasswordField loginPW = new PasswordField();
        passwordField.setPromptText("Password");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButton) {
                return new Pair<>(loginUsername.getText(), loginPW.getText());
            }
            return null;
        });

        loginPane.add(new Label("Username:"), 0, 0);
        loginPane.add(loginUsername, 1, 0);
        loginPane.add(new Label("Password:"), 0, 1);
        loginPane.add(loginPW, 1, 1);

        dialog.getDialogPane().setContent(loginPane);



        Optional<Pair<String, String>> input = dialog.showAndWait();



        if (input.isPresent() && !input.get().getKey().isEmpty() && !input.get().getValue().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get().getKey();
            pw = input.get().getValue();
            try {
                boolean canLogin = client.postLogin(username, pw);
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


        service.setOnSucceeded(e -> {
            try {
                chatContentList.setCellFactory(new MessageCellFactory());
                List<String> currentUsers = client.getCurrentUsers();
                currentOnlineCnt.setText(String.valueOf(currentUsers.size()));
                CommMessage privateChat;
                CommMessage groupChat;
                try {
                     privateChat = client.checkNewChat();
                     groupChat = client.checkNewGroupChat();
                }catch (IOException exception){
                    exception.printStackTrace();
                    System.out.println("message getting fail");
                    return;
                }

                for (int i = 0; i < groupChat.getMsgList().size(); i++) {
                    String[] groupMembers = groupChat.getMsgList().get(i).split(Client.DELIMETER);
                    System.out.println(Arrays.toString(groupMembers));
                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(groupMembers));
                    String name = getChatRoomName(arrayList);
                    chatList.getItems().add(name);
                }
                for (int i = 0; i < privateChat.getMsgList().size(); i++) {
                    String name = privateChat.getMsgList().get(i);
                    chatList.getItems().add(name);
                }

                String chattingTo = chatList.getSelectionModel().getSelectedItem();
                CopyOnWriteArrayList<Message> chats;
                if (chattingTo != null) { //check income msg in the current window
                    if (!IsGroupChat(chattingTo)) {
                        chats = client.getChat(chattingTo);
                    } else {
                        chats = client.getGroupChat(formatGroupName(chattingTo));
                    }
                    if (chats != null) {
                        chats.removeAll(allChats.get(chattingTo));
                        if(chats.size()>0){
                            allChats.get(chattingTo).addAll(chats);
                            chatContentList.setCellFactory(new MessageCellFactory());
                            for (Message msg:chats){
                                if(!msg.getSentBy().equals(username)) {
                                    chatContentList.getItems().add(msg);
                                }
                            }
                        }
                    }
                }


            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//                ex.printStackTrace();
                System.out.println("IOException");
                System.out.println("The server is not started");
                // Display warning message
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("The server is not started");
                alert.setHeaderText(null);
                alert.setContentText("The server is not started");
                alert.showAndWait();
                System.exit(0);
                try {
                    Socket socket = new Socket("localhost", 5000);
                    client.setSocket(socket);
                } catch (IOException exc) {
                    System.out.println("Attempting to reconnect");
                    Alert connectAttempt = new Alert(Alert.AlertType.WARNING);
                    connectAttempt.setTitle("The server is not started");
                    connectAttempt.setHeaderText(null);
                    connectAttempt.setContentText("The server is not started");
                    connectAttempt.showAndWait();
                }
            } catch (ClassNotFoundException ex) {
//                throw new RuntimeException(ex);
                System.out.println("Runtime exception");
            }
        });
        service.start();

        Thread thread = new Thread(()->{
            //Background work
            while (true) {
                // Listen for messages from server
                try {
                    client.getCurrentUsers();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                Platform.runLater(() -> {
                    // Do something with the messages
//                    client.run();
                });
            }
        });


        chatList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (t1 != null) {
                    try {
                        CopyOnWriteArrayList<Message> chats;
                        if (IsGroupChat(t1)) {
                            chats = client.getGroupChat(formatGroupName(t1));
                        } else {
                            chats = client.getChat(t1); //only get msg from the other side
                        }

                        if (chats != null) {

                            if (!allChats.containsKey(t1)) {
                                allChats.put(t1, new ArrayList<>());
                            }else{
                                chats.removeAll(allChats.get(t1));
                            }
                            allChats.get(t1).addAll(chats);
                            chatContentList.getItems().clear();
                            chatContentList.setCellFactory(new MessageCellFactory());
                            chatContentList.getItems().addAll(allChats.get(t1));
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        });
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
            allChats.put(selected.get(), new ArrayList<>()); //sender's side
            chatList.getSelectionModel().select(selected.get());
        }

    }



    @FXML
    public void createGroupChat() throws IOException, ClassNotFoundException {
        Dialog<List<String>> dialog = new Dialog<>();
        // Create a multi-select list of users
        ListView<String> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List<String> currentUsers = client.getCurrentUsers();
        currentUsers.remove(username);
        listView.getItems().removeAll();
        listView.getItems().addAll(currentUsers);

        dialog.getDialogPane().setContent(listView);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // Convert the result to a list of selected usernames
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                List<String> selectedUsers = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
                selectedUsers.add(username); // add current client
                return selectedUsers;
            }
            return null;
        });

        Optional<List<String>> result = dialog.showAndWait();
        String chatRoomName;
        List<String> selectedNames = result.get();
        Collections.sort(selectedNames);
        chatRoomName = getChatRoomName(selectedNames);
        if (chatList.getItems().contains(chatRoomName)) {
            chatList.getSelectionModel().select(chatRoomName);

        } else {
            chatList.getItems().add(chatRoomName);
            allChats.put(formatGroupName(chatRoomName), new ArrayList<>()); //sender's side
            chatList.getSelectionModel().select(chatRoomName);
        }
        System.out.println(formatGroupName(chatRoomName));

    }

    private static String getChatRoomName(List<String> selectedNames) {
        String chatRoomName;
        if (selectedNames.size() > 3) {
            chatRoomName = String.join(", ", selectedNames.subList(0, 3)) + "... (" + selectedNames.size() + ")";
        } else {
            chatRoomName = String.join(", ", selectedNames) + " (" + selectedNames.size() + ")";
        }
        return chatRoomName;
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException, ClassNotFoundException {
        if(checkEmptyTo()) return;
        String to = chatList.getSelectionModel().getSelectedItem();
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

        Message message;

        if (IsGroupChat(to)) { //tmp group chat check
            message = new Message(System.currentTimeMillis(), username, formatGroupName(to), txt);
            client.postGroupChat(message);
        } else {
            message = new Message(System.currentTimeMillis(), username, to, txt);
            client.postChat(message);
        }
        chatContentList.getItems().add(message);
        System.out.println(chatContentList.getItems());
//        chatContentList.getItems().clear();
        inputArea.clear();

    }
@FXML
    public void receiveFile() throws IOException, ClassNotFoundException {
    if (checkEmptyTo()) return;
    String to = chatList.getSelectionModel().getSelectedItem();
    FileChooser fileChooser = new FileChooser();
    File init = new File("D:\\SUSTech2023S\\CS209\\CS209-Java2-Assignment2\\files\\"+to);
    if(!init.exists()){
        init.mkdir();
    }
    fileChooser.setInitialDirectory(init);
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
    );
    File selectedFile = fileChooser.showOpenDialog(null);

    if (selectedFile != null) {
        try {
            client.getFile(selectedFile.toPath().getFileName().toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    Message message;

    }

    private boolean checkEmptyTo() {
        String to = chatList.getSelectionModel().getSelectedItem();
        if (to == null) {
            // Display warning message
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No chat selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a chat to send a message.");
            alert.showAndWait();
            return true;
        }
        return false;
    }

    @FXML
    public void doSendFile() throws IOException, ClassNotFoundException {
        if(checkEmptyTo()){
            return;
        }
        String to = chatList.getSelectionModel().getSelectedItem();
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(null);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        if (selectedFile != null) {
            try {
                client.sendFile(selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        Message message;

        if (IsGroupChat(to)) { //tmp group chat check
            message = new Message(System.currentTimeMillis(), username, formatGroupName(to), "sending a file");
            client.postGroupChat(message);
        } else {
            message = new Message(System.currentTimeMillis(), username, to, "Sending a file");
            client.postChat(message);
        }
        chatContentList.getItems().add(message);
        System.out.println(chatContentList.getItems());
//        chatContentList.getItems().clear();
        inputArea.clear();
    }

    private static boolean IsGroupChat(String to) {
        return to.endsWith(")");
    }

    public String formatGroupName(String groupName) {
        return groupName.replaceAll("\\(\\d+\\)$", "").trim();
    }

    public void receiveMessage(Message message) {
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

    private static class UpdateCheckService extends ScheduledService<Boolean> {

        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    updateMessage("Checking for updates");
                    return false;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    Boolean result = getValue();
                    Platform.runLater(() -> {

                    });
                }
            };
        }
    }
}
