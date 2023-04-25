package cn.edu.sustech.cs209.chatting.server;
import cn.edu.sustech.cs209.chatting.common.Message;
import com.alibaba.fastjson.JSONObject;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class UserThread extends Thread
{
    private Socket socket;
    private Server server;
    private BufferedReader input;
    private PrintWriter output;
    String userID;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public BufferedReader getInput() {
        return input;
    }

    public void setInput(BufferedReader input) {
        this.input = input;
    }

    public PrintWriter getOutput() {
        return output;
    }

    public void setOutput(PrintWriter output) {
        this.output = output;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public UserThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            userID = Message.parseInfo(input.readLine()).getSentBy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void serverSendMsg(String message) {
        output.println(message);
    }

    private void sendAllSingleMessage(Message msg, String key) {
        String msgList = JSONObject.toJSON(server.singleChatHistory.get(key)).toString();
        Message sendByUpdate = new Message(System.currentTimeMillis(), "SERVER", msg.getSentBy(),msgList ,Message.MsgType.SINGLE_CHAT_REPLY);
        this.serverSendMsg(JSONObject.toJSON(sendByUpdate).toString());
        for (UserThread user: server.userList) {
//            System.out.println(user.getUserID());
            if(user.getUserID().equals(msg.getSendTo())){
                Message sendToUpdate = new Message(System.currentTimeMillis(), "SERVER", msg.getSendTo(),msgList ,Message.MsgType.SINGLE_CHAT_REPLY);
                user.serverSendMsg(JSONObject.toJSON(sendToUpdate).toString());
                break;
            }
        }
    }

    private void sendAllMultipleMessage(String key) {
        String msgList = JSONObject.toJSON(server.multipleChatHistory.get(key)).toString();
        ArrayList<String> memberList = server.multipleMemberList.get(key);
        for (UserThread user : server.userList) {
            if (memberList.contains(user.getUserID())) {
                Message responseToMsgReceiver = new Message(System.currentTimeMillis(), "SERVER", user.getUserID(), msgList, Message.MsgType.MULTIPLE_CHAT_REPLY);
                user.serverSendMsg(JSONObject.toJSON(responseToMsgReceiver).toString());
            }
        }
    }
    public String createEntry(String sendTo, String sendFrom){
        String entry;
        if(sendTo.compareTo(sendFrom) > 0){
            entry = sendTo + "&" + sendFrom;
        }
        else {
            entry = sendFrom + "&" + sendTo;
        }
        return entry;
    }
    @Override
    public void run() {
        System.out.println("Connect to: " + socket.getInetAddress());
        String message;
        try {
            while (true) {
                message = input.readLine();
                if(message == null){
                    break;
                }else {
                    if (message.equals(String.valueOf(Message.MsgType.LEAVE_DEMAND))){
                        serverSendMsg(String.valueOf(Message.MsgType.EXIT_PERMISSION));
                        server.removeUser(this);
                        server.broadcast(Message.MsgType.SYSTEM_INFO, this.getUserID() + " left");
                        String usernameListStr = server.userList.stream().map(UserThread::getUserID).collect(Collectors.joining(","));
                        server.broadcast(Message.MsgType.USER_LIST_UPDATE, usernameListStr);
                        break;
                    }
                    Message parsedMsg = Message.parseInfo(message);
                    if (parsedMsg.getType() == Message.MsgType.SINGLE_CHAT_DEMANDING) {
                        System.out.println(parsedMsg.getSentBy()+parsedMsg.getSendTo());
                        String key = createEntry(parsedMsg.getSentBy(), parsedMsg.getSendTo());
                        if (!server.singleChatHistory.containsKey(key)) {
                            server.singleChatHistory.put(key, new ArrayList<>());
                        }
                        sendAllSingleMessage(parsedMsg, key);
                    }else if (parsedMsg.getType() == Message.MsgType.SINGLE_MESSAGE_SENDING){
                        String key = createEntry(parsedMsg.getSentBy(), parsedMsg.getSendTo());
                        if (!server.singleChatHistory.containsKey(key)) {
                            server.singleChatHistory.put(key, new ArrayList<>());
                        }
                        server.singleChatHistory.get(key).add(parsedMsg);
                        sendAllSingleMessage(parsedMsg, key);
                    }else if (parsedMsg.getType() == Message.MsgType.MULTIPLE_CHAT_DEMANDING){
                        String[] userArray = parsedMsg.getData().split("@")[0].split(",");
                        ArrayList<String> currentUserList = new ArrayList<>(Arrays.asList(userArray));
                        if (!server.multipleChatHistory.containsKey(parsedMsg.getSendTo())) {
                            server.multipleChatHistory.put(parsedMsg.getSendTo(), new ArrayList<>());
                            server.multipleMemberList.put(parsedMsg.getSendTo(), currentUserList);
                        }
                        sendAllMultipleMessage(parsedMsg.getSendTo());
                    }else if (parsedMsg.getType() == Message.MsgType.MULTIPLE_MESSAGE_SENDING){
                        String title = parsedMsg.getSendTo();
                        server.multipleChatHistory.get(title).add(parsedMsg);
                        sendAllMultipleMessage(parsedMsg.getSendTo());
                    }
                }
                }
            } catch (IOException ex) {
             System.out.println("user logout");
        }finally {
            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
