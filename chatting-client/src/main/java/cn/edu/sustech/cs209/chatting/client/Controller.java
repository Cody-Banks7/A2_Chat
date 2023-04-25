package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    public ListView<ChatBox> chatList;
    public TextArea inputArea;
    public Label currentUsername;
    public Label currentOnlineCnt;
    public Label currentChatTitle;
    String username;
    PrintWriter out;
    int chatType;
    String chatTitle;
    private List<String> clientsList = new ArrayList<>();

    public List<String> getClientsList() {
        return clientsList;
    }


    private void clientSendMsg(Message message) {
        out.println(JSONObject.toJSON(message).toString());
    }
    public void setClientList(List<String> clients) {
        this.clientsList = clients;
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            this.username = input.get();
            this.currentUsername.setText(String.format("Current User: %s", this.username));
            this.inputArea.setWrapText(true);
            this.chatTitle = "";
            try {
                Socket client = new Socket("localhost", Integer.parseInt("9876"));
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                Message requestJoinMsg = new Message(System.currentTimeMillis(), this.username, "SERVER","" ,Message.MsgType.REQUEST_TO_JOIN);
                clientSendMsg(requestJoinMsg);
                String message;
                boolean judge = true;
                while (true) {
                    message = in.readLine();
                    Message parsedMsg = Message.parseInfo(message);
                    if (parsedMsg.getType() == Message.MsgType.ERROR_DUPLICATE_USERNAME) {
                        System.out.println("Duplicate username, username " + this.username + "already exits");
                        judge = false;
                        in.close();
                        out.close();
                        client.close();
                        Platform.exit();
                        break;
                    }
                    else if (parsedMsg.getType() == Message.MsgType.ALLOW_TO_JOIN) {
                        break;
                    }
                }
                if (judge) { new Thread(new Controller.MessageHandler(client)).start(); }
                if (judge) { chatContentList.setCellFactory(new MessageCellFactory()); }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
    }

    private void requestSingleChat(String title){
        Message requestPrivateChatMessage = new Message(System.currentTimeMillis(), this.username, this.chatTitle, title,Message.MsgType.REQUEST_PRIVATE_CHAT);
        clientSendMsg(requestPrivateChatMessage);
    }

    public static String currentTime(){
        DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()));
    }

    @FXML
    public void createPrivateChat() throws MalformedURLException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        stage.setTitle("Start by choosing someone");
        stage.setWidth(400.0);
        ComboBox<String> userSel = new ComboBox<>();
        userSel.setPrefWidth(200.0);
        // FIXME: get the user list from server, the current user's name should be filtered out
        synchronized (this) {
            userSel.getItems().addAll(getClientsList());
        }
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
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
        String selectedUser = user.get();
        if(selectedUser != null && !selectedUser.equals("")){
            this.chatTitle = selectedUser;
            this.currentChatTitle.setText(selectedUser);
            this.chatType = 1;
            boolean chatItemExists = false;
            for (Object item : chatList.getItems()) {
                if (item instanceof ChatBox && ((ChatBox) item).getTitle().getText().equals(selectedUser)) {
                    chatItemExists = true;
                    break;
                }
            }
            if (!chatItemExists) {
                ChatBox newChatItem = new ChatBox(selectedUser, currentTime(), 1);
                newChatItem.setOnMouseClicked(event -> {
                    System.out.println("click mouse");
                    Controller.this.chatType = 1;
                    Controller.this.chatTitle = newChatItem.getTitle().getText();
                    requestSingleChat(Controller.this.chatTitle);
                });
                this.chatList.getItems().add(newChatItem);
            }
            requestSingleChat(this.chatTitle);
        }
    }


    private void requestMultipleChat(String sendTo){
        Message message = new Message(System.currentTimeMillis(), this.username, this.chatTitle, sendTo, Message.MsgType.REQUEST_GROUP_CHAT);
        clientSendMsg(message);
    }

    private String showGroupChatConfigDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Create Group Chat");
        dialog.setHeaderText("Please enter a group chat name and select users");

        ButtonType createButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        TextField groupTitleField = new TextField();

        CheckBox[] userCheckBoxes = new CheckBox[this.clientsList.size()];
        for (int i = 0; i < this.clientsList.size(); i++) {
            userCheckBoxes[i] = new CheckBox(this.clientsList.get(i));
        }
        GridPane contentPane = new GridPane();
        contentPane.setHgap(10);
        contentPane.setVgap(10);
        contentPane.setPadding(new Insets(20));
        contentPane.addRow(0, new Label("Chat title:"), groupTitleField);
        contentPane.addRow(2, new Label("Select users:"));
        for (int i = 0; i < userCheckBoxes.length; i++) {
            contentPane.addRow(i + 3, userCheckBoxes[i]);
        }
        dialog.getDialogPane().setContent(contentPane);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType && groupTitleField.getText() != null && !groupTitleField.getText().equals("")) {
                StringBuilder selectedUsers = new StringBuilder(this.username);
                for (CheckBox checkBox : userCheckBoxes) {
                    if (checkBox.isSelected()) {
                        selectedUsers.append(",").append(checkBox.getText());
                    }
                }
                return selectedUsers + "@" + groupTitleField.getText();
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
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
    public void createGroupChat() throws MalformedURLException {
        String usersAndTitle = showGroupChatConfigDialog();
        if (usersAndTitle != null) {
            String groupTitle = usersAndTitle.split("@")[1];
            String userListStr = usersAndTitle.split("@")[0];
            this.chatTitle = groupTitle;
            this.chatType = 2;
            this.currentChatTitle.setText(groupTitle + "(Group Chat)");
            boolean chatItemExists = false;
            for (Object item : chatList.getItems()) {
                if (item instanceof ChatBox && ((ChatBox) item).getTitle().getText().equals(groupTitle + "(Group Chat)")) {
                    chatItemExists = true;
                    break;
                }
            }
            if (!chatItemExists) {
                ChatBox newChatItem = new ChatBox(groupTitle + "(Group Chat)", currentTime(),  2);
                newChatItem.setOnMouseClicked(event -> {
                    Controller.this.chatType = 2;
                    Controller.this.chatTitle = newChatItem.getTitle().getText().replace("(Group Chat)", "");
                    requestMultipleChat(Controller.this.chatTitle);
                });
                this.chatList.getItems().add(newChatItem);
            }
            requestMultipleChat(userListStr);
        }
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        String content = inputArea.getText();
        Message.MsgType type;
        if (this.chatType == 2){
            type = Message.MsgType.SEND_GROUP_MESSAGE;
        }else{
            type = Message.MsgType.SEND_PRIVATE_MESSAGE;
        }
        if (content.length() > 0){
            Message msg = new Message(System.currentTimeMillis(), this.username, this.chatTitle, content, type);
            clientSendMsg(msg);
            inputArea.setText("");
        }
        else {
            System.out.println("Null message");
        }
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
                        setText(null);
                        setGraphic(null);
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

    public void shutdown() {
        Message leaveMessage = new Message(System.currentTimeMillis(),this.username, "SERVER",  "",Message.MsgType.REQUEST_TO_LEAVE);
        clientSendMsg(leaveMessage);
    }

    private class MessageHandler implements Runnable {
        private final Socket client;

        public MessageHandler(Socket client) {
            this.client = client;
        }
        public boolean chatCheck(String sendBy, String sentTo, String checkA, String checkB){
            return (sendBy.equals(checkA) && sentTo.equals(checkB)) || (sendBy.equals(checkB) && sentTo.equals(checkA));
        }
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String message;
                while (true) {
                    message = in.readLine();
                    if (message == null){
                        break;
                    }
                    Message parsedMsg = Message.parseInfo(message);
                    if (parsedMsg.getType()==Message.MsgType.ALLOW_TO_LEAVE) {
                        break;
                    }
                    else if (parsedMsg.getType()==Message.MsgType.UPDATE_CLIENT_LIST) {
                        List<String> tmp = new ArrayList<>(Arrays.asList(parsedMsg.getData().split(",")));
                        int currentUserCnt = tmp.size();
                        Platform.runLater(() -> Controller.this.currentOnlineCnt.setText("Online: " + currentUserCnt));
                        List<String> usernameTmpList = new ArrayList<>(tmp);
                        usernameTmpList.remove(Controller.this.username);
                        synchronized (Controller.this) {
                            setClientList(usernameTmpList);
                        }
                    }
                    else if (parsedMsg.getType()==Message.MsgType.RESPONSE_PRIVATE_CHAT) {
                        String currentData = parsedMsg.getData();
                        List<Message> msgList = JSONObject.parseArray(currentData, Message.class);
                        String selectedUser;
                        if(msgList.size() > 0 && msgList.get(0).getType()== Message.MsgType.SEND_PRIVATE_MESSAGE) {
                            selectedUser = msgList.get(0).getSentBy();
                            if (selectedUser != null && !selectedUser.equals("") && !selectedUser.equals(username)) {
                                boolean chatItemExists = false;
                                for (Object item : chatList.getItems()) {
                                    if (item instanceof ChatBox && ((ChatBox) item).getTitle().getText().equals(selectedUser)) {
                                        chatItemExists = true;
                                        break;
                                    }
                                }
                                if (!chatItemExists) {
                                    ChatBox newChatItem;
                                    try {
                                        newChatItem = new ChatBox(selectedUser, currentTime(), 1);
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    }
                                    newChatItem.setOnMouseClicked(event -> {
                                        System.out.println("click mouse");
                                        Controller.this.chatType = 1;
//                                        requestSingleChat(Controller.this.chatTitle);  // 加了会死循环，不加没法更新右边，先看看下面为什么没更新
                                        Controller.this.chatTitle = newChatItem.getTitle().getText();
                                        if (msgList.size() > 0 && chatCheck(Controller.this.username,Controller.this.chatTitle,msgList.get(0).getSendTo(),msgList.get(0).getSentBy())) {
                                            Platform.runLater(() -> {
                                                Controller.this.chatContentList.getItems().clear();
                                                Controller.this.chatContentList.getItems().addAll(msgList);
                                            });
                                        }
                                        else if (msgList.size() == 0) {
                                            Controller.this.chatContentList.getItems().clear();
                                        }
                                    });
                                    Platform.runLater(() -> chatList.getItems().add(newChatItem));
                                }
                            }
                        }
                        if (chatType==1) {
                            if (msgList.size() > 0 && chatCheck(Controller.this.username,Controller.this.chatTitle,msgList.get(0).getSendTo(),msgList.get(0).getSentBy())) {
                                Platform.runLater(() -> {
                                    Controller.this.chatContentList.getItems().clear();
                                    Controller.this.chatContentList.getItems().addAll(msgList);
                                });
                            }
                            else if (msgList.size() == 0) {
                                Platform.runLater(() -> Controller.this.chatContentList.getItems().clear());
                            }
                        }
                    }
                    else if (parsedMsg.getType()==Message.MsgType.RESPONSE_GROUP_CHAT) {  //两个问题，能影响别人，切换信息更新不到位
                        String tmp = parsedMsg.getData();
                        List<Message> msgList = JSONObject.parseArray(tmp, Message.class);

                        if (msgList.size() > 0 && msgList.get(0).getType()== Message.MsgType.SEND_GROUP_MESSAGE) {
                            String groupTitle = msgList.get(0).getSendTo();
                            if(msgList.get(0).getSentBy().equals(username)||chatType==0){
                                chatTitle = groupTitle;
                                chatType = 2;
                                Platform.runLater(() -> currentChatTitle.setText(groupTitle + "(Group Chat)"));
                            }
                            boolean chatItemExists = false;
                            for (Object item : chatList.getItems()) {
                                if (item instanceof ChatBox && ((ChatBox) item).getTitle().getText().equals(groupTitle + "(Group Chat)")) {
                                    chatItemExists = true;
                                    break;
                                }
                            }
                            if (!chatItemExists) {
                                ChatBox newChatItem = new ChatBox(groupTitle + "(Group Chat)", currentTime(),  2);
                                newChatItem.setOnMouseClicked(event -> {
                                    Controller.this.chatType = 2;
                                    Controller.this.chatTitle = newChatItem.getTitle().getText().replace("(Group Chat)", "");
//                                    requestMultipleChat(Controller.this.chatTitle);
                                    if (msgList.size() > 0 && Controller.this.chatTitle.equals(msgList.get(0).getSendTo())) {
                                        Platform.runLater(() -> {
                                            Controller.this.chatContentList.getItems().clear();
                                            Controller.this.chatContentList.getItems().addAll(msgList);
                                            requestMultipleChat(Controller.this.chatTitle);
                                        });
                                    }
                                });
                                    Platform.runLater(() -> chatList.getItems().add(newChatItem));
                            }
                        }

                        if (chatType==2) {
                            if (msgList.size() > 0 && Controller.this.chatTitle.equals(msgList.get(0).getSendTo()) ) {
                                Platform.runLater(() -> {
                                    Controller.this.chatContentList.getItems().clear();
                                    Controller.this.chatContentList.getItems().addAll(msgList);
                                });
                            }
                            else if (msgList.size() == 0) {
                                Platform.runLater(() -> Controller.this.chatContentList.getItems().clear());
                            }
                        }
                    }
                    System.out.println(message);
                }
                in.close();
                System.out.println("Controller closed.");
                System.out.println("MessageHandler closed.");
                Platform.exit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
