package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import com.alibaba.fastjson.JSONObject;
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
import javafx.util.Callback;
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
    String currentChatObject;
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
            this.currentChatObject = "";
            try {
                Socket client = new Socket("localhost", Integer.parseInt("9876"));
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
                Message requestJoinMsg = new Message(System.currentTimeMillis(), this.username, "SERVER","" ,Message.MsgType.JOIN_DEMAND);
                clientSendMsg(requestJoinMsg);
                String message;
                boolean judge = true;
                while (true) {
                    message = in.readLine();
                    Message parsedMsg = Message.parseInfo(message);
                    if (parsedMsg.getType() == Message.MsgType.USERNAME_DUPLICATE_ERROR) {
                        System.out.println("Duplicate username, username " + this.username + "already exits");
                        judge = false;
                        in.close();
                        out.close();
                        client.close();
                        Platform.exit();
                        break;
                    }
                    else if (parsedMsg.getType() == Message.MsgType.JOIN_PERMISSION) {
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
        Message requestPrivateChatMessage = new Message(System.currentTimeMillis(), this.username, this.currentChatObject, title,Message.MsgType.SINGLE_CHAT_DEMANDING);
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
            this.currentChatObject = selectedUser;
            this.currentChatTitle.setText(selectedUser);
            this.chatType = 1;
            boolean chatItemExists = false;
            for (Object item : chatList.getItems()) {
                if (item instanceof ChatBox && ((ChatBox) item).getHeadline().getText().equals(selectedUser)) {
                    chatItemExists = true;
                    break;
                }
            }
            if (!chatItemExists) {
                ChatBox newChatItem = new ChatBox(selectedUser, currentTime(), 1);
                newChatItem.setOnMouseClicked(event -> {
                    System.out.println("click mouse");
                    Controller.this.chatType = 1;
                    Controller.this.currentChatObject = newChatItem.getHeadline().getText();
                    requestSingleChat(Controller.this.currentChatObject);
                });
                this.chatList.getItems().add(newChatItem);
            }
            requestSingleChat(this.currentChatObject);
        }
    }


    private void requestMultipleChat(String sendTo){
        Message message = new Message(System.currentTimeMillis(), this.username, this.currentChatObject, sendTo, Message.MsgType.MULTIPLE_CHAT_DEMANDING);
        clientSendMsg(message);
    }

    private String groupChatPopUp() {
        Dialog<String> popUp = new Dialog<>();
        popUp.setTitle("Select To Create Group Chat");
        popUp.setHeaderText("Enter a group name and select users");
        ButtonType createButtonType = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        popUp.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        TextField groupChatHeadline = new TextField();

        CheckBox[] userCheckBoxes = new CheckBox[this.clientsList.size()];
        for (int i = 0; i < this.clientsList.size(); i++) {
            userCheckBoxes[i] = new CheckBox(this.clientsList.get(i));
        }
        GridPane contentPane = new GridPane();
        contentPane.setHgap(5);
        contentPane.setVgap(5);
        contentPane.setPadding(new Insets(10));
        contentPane.addRow(0, new Label("HEADLINE:"), groupChatHeadline);
        contentPane.addRow(3, new Label("Users"));
        for (int i = 0; i < userCheckBoxes.length; i++) {
            contentPane.addRow(i + 4, userCheckBoxes[i]);
        }
        popUp.getDialogPane().setContent(contentPane);

        popUp.setResultConverter(dialogButton -> {
            if (groupChatHeadline.getText() != null && !groupChatHeadline.getText().equals("") && dialogButton == createButtonType) {
                StringBuilder includeUsers = new StringBuilder(this.username);
                for (CheckBox checkBox : userCheckBoxes) {
                    if (checkBox.isSelected()) {
                        includeUsers.append(",").append(checkBox.getText());
                    }
                }
                return includeUsers + "@" + groupChatHeadline.getText();
            }
            return null;
        });
        Optional<String> result = popUp.showAndWait();
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
        String usersAndTitle = groupChatPopUp();
        if (usersAndTitle != null) {
            String groupTitle = usersAndTitle.split("@")[1];
            String userListStr = usersAndTitle.split("@")[0];
            this.currentChatObject = groupTitle;
            this.chatType = 2;
            this.currentChatTitle.setText(groupTitle + "(MultiUserChat)");
            boolean chatItemExists = false;
            for (Object item : chatList.getItems()) {
                if (item instanceof ChatBox && ((ChatBox) item).getHeadline().getText().equals(groupTitle + "(MultiUserChat)")) {
                    chatItemExists = true;
                    break;
                }
            }
            if (!chatItemExists) {
                ChatBox newChatItem = new ChatBox(groupTitle + "(MultiUserChat)", currentTime(),  2);
                newChatItem.setOnMouseClicked(event -> {
                    Controller.this.chatType = 2;
                    Controller.this.currentChatObject = newChatItem.getHeadline().getText().replace("(MultiUserChat)", "");
                    requestMultipleChat(Controller.this.currentChatObject);
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
            type = Message.MsgType.MULTIPLE_MESSAGE_SENDING;
        }else{
            type = Message.MsgType.SINGLE_MESSAGE_SENDING;
        }
        if (content.length() > 0){
            Message msg = new Message(System.currentTimeMillis(), this.username, this.currentChatObject, content, type);
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
        Message leaveMessage = new Message(System.currentTimeMillis(),this.username, "SERVER",  "",Message.MsgType.LEAVE_DEMAND);
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
                    if (parsedMsg.getType()==Message.MsgType.EXIT_PERMISSION) {
                        break;
                    }
                    else if (parsedMsg.getType()==Message.MsgType.USER_LIST_UPDATE) {
                        List<String> tmp = new ArrayList<>(Arrays.asList(parsedMsg.getData().split(",")));
                        int currentUserCnt = tmp.size();
                        Platform.runLater(() -> Controller.this.currentOnlineCnt.setText("Online: " + currentUserCnt));
                        List<String> usernameTmpList = new ArrayList<>(tmp);
                        usernameTmpList.remove(Controller.this.username);
                        synchronized (Controller.this) {
                            setClientList(usernameTmpList);
                        }
                    }
                    else if (parsedMsg.getType()==Message.MsgType.SINGLE_CHAT_REPLY) {
                        String currentData = parsedMsg.getData();
                        List<Message> msgList = JSONObject.parseArray(currentData, Message.class);
                        String selectedUser;
                        if(msgList.size() > 0 && msgList.get(0).getType()== Message.MsgType.SINGLE_MESSAGE_SENDING) {
                            selectedUser = msgList.get(0).getSentBy();
                            if (selectedUser != null && !selectedUser.equals("") && !selectedUser.equals(username)) {
                                boolean chatItemExists = false;
                                for (Object item : chatList.getItems()) {
                                    if (item instanceof ChatBox && ((ChatBox) item).getHeadline().getText().equals(selectedUser)) {
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
                                        Controller.this.currentChatObject = newChatItem.getHeadline().getText();
                                        if (msgList.size() > 0 && chatCheck(Controller.this.username,Controller.this.currentChatObject,msgList.get(0).getSendTo(),msgList.get(0).getSentBy())) {
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
                            if (msgList.size() > 0 && chatCheck(Controller.this.username,Controller.this.currentChatObject,msgList.get(0).getSendTo(),msgList.get(0).getSentBy())) {
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
                    else if (parsedMsg.getType()==Message.MsgType.MULTIPLE_CHAT_REPLY) {  //两个问题，能影响别人，切换信息更新不到位
                        String tmp = parsedMsg.getData();
                        List<Message> msgList = JSONObject.parseArray(tmp, Message.class);

                        if (msgList.size() > 0 && msgList.get(0).getType()== Message.MsgType.MULTIPLE_MESSAGE_SENDING) {
                            String groupTitle = msgList.get(0).getSendTo();
                            if(msgList.get(0).getSentBy().equals(username)||chatType==0){
                                currentChatObject = groupTitle;
                                chatType = 2;
                                Platform.runLater(() -> currentChatTitle.setText(groupTitle + "(MultiUserChat)"));
                            }
                            boolean chatItemExists = false;
                            for (Object item : chatList.getItems()) {
                                if (item instanceof ChatBox && ((ChatBox) item).getHeadline().getText().equals(groupTitle + "(MultiUserChat)")) {
                                    chatItemExists = true;
                                    break;
                                }
                            }
                            if (!chatItemExists) {
                                ChatBox newChatItem = new ChatBox(groupTitle + "(MultiUserChat)", currentTime(),  2);
                                newChatItem.setOnMouseClicked(event -> {
                                    Controller.this.chatType = 2;
                                    Controller.this.currentChatObject = newChatItem.getHeadline().getText().replace("(MultiUserChat)", "");
//                                    requestMultipleChat(Controller.this.chatTitle);
                                    if (msgList.size() > 0 && Controller.this.currentChatObject.equals(msgList.get(0).getSendTo())) {
                                        Platform.runLater(() -> {
                                            Controller.this.chatContentList.getItems().clear();
                                            Controller.this.chatContentList.getItems().addAll(msgList);
                                            requestMultipleChat(Controller.this.currentChatObject);
                                        });
                                    }
                                });
                                    Platform.runLater(() -> chatList.getItems().add(newChatItem));
                            }
                        }

                        if (chatType==2) {
                            if (msgList.size() > 0 && Controller.this.currentChatObject.equals(msgList.get(0).getSendTo()) ) {
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
