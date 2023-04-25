package cn.edu.sustech.cs209.chatting.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class Message {
    private final Long timestamp;

    private final String sentBy;

    private final String sendTo;

    private final String data;

    private final MsgType type;


    public Message(Long timestamp, String sentBy, String sendTo, String data, MsgType type) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public MsgType getType() {
        return type;
    }

    public static Message parseInfo(String message) {
        System.out.println("Receive message: " + message);
        JSONObject info=JSON.parseObject(message);
        Long timestamp = Long.parseLong(info.getString("timestamp"));
        String sentBy = info.getString("sentBy");
        String sendTo = info.getString("sendTo");
        String data = info.getString("data");
        MsgType msgType = MsgType.valueOf(info.getString("type"));
        return new Message(timestamp, sentBy, sendTo, data, msgType);
    }
    public enum MsgType {
        SINGLE_MESSAGE_SENDING,
        MULTIPLE_MESSAGE_SENDING,
        SINGLE_CHAT_DEMANDING,
        MULTIPLE_CHAT_DEMANDING,
        LEAVE_DEMAND,
        JOIN_DEMAND,
        USERNAME_DUPLICATE_ERROR,
        JOIN_PERMISSION,
        EXIT_PERMISSION,
        USER_LIST_UPDATE,
        SINGLE_CHAT_REPLY,
        MULTIPLE_CHAT_REPLY,
        SYSTEM_INFO
    }
}
