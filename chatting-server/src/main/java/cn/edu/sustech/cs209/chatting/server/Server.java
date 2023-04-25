package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Server {
    private int port = 9876;
    public ArrayList<UserThread> userList = new ArrayList<>();
    public HashMap<String, ArrayList<Message>> singleChatHistory = new HashMap<>();
    public HashMap<String, ArrayList<Message>> multipleChatHistory = new HashMap<>();
    public HashMap<String, ArrayList<String>> multipleMemberList = new HashMap<>();
    public void startServer() throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Start server: port = " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                UserThread currentUser = new UserThread(socket,this);
                boolean judge = true;
                for (UserThread user: userList) {
                    if (user.getUserID().equals(currentUser.getUserID())) {
                        judge = false;
                        break;
                    }
                }
                if (!judge) {
                    Message errorMsg = new Message(System.currentTimeMillis(), "SERVER", currentUser.getUserID(),"",Message.MsgType.ERROR_DUPLICATE_USERNAME);
                    currentUser.serverSendMsg(errorMsg.toString());
                    continue;
                }
                Message successMsg = new Message(System.currentTimeMillis(), "SERVER", currentUser.getUserID(), "", Message.MsgType.ALLOW_TO_JOIN);
                currentUser.serverSendMsg(JSONObject.toJSON(successMsg).toString());
                userList.add(currentUser);
                currentUser.start();

                // 通知每个用户新增了client, 要求更新client列表
                String userListPacket = userList.stream().map(UserThread::getUserID).collect(Collectors.joining(","));
                broadcast(Message.MsgType.UPDATE_CLIENT_LIST, userListPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void broadcast(Message.MsgType command, String data) {
        for (UserThread user : userList) {
            Message message = new Message(System.currentTimeMillis(),"SERVER", user.getUserID(), data, command);
            user.serverSendMsg(JSONObject.toJSON(message).toString());
        }
    }
    synchronized void removeUser(UserThread userThread) { userList.remove(userThread); }
}
