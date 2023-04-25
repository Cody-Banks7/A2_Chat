package cn.edu.sustech.cs209.chatting.common;

import java.util.Arrays;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
        SEND_PRIVATE_MESSAGE,
        SEND_GROUP_MESSAGE,
        REQUEST_PRIVATE_CHAT,
        REQUEST_GROUP_CHAT,
        REQUEST_TO_LEAVE,
        REQUEST_TO_JOIN,
        ERROR_DUPLICATE_USERNAME,
        ALLOW_TO_JOIN,
        ALLOW_TO_LEAVE,
        UPDATE_CLIENT_LIST,
        RESPONSE_PRIVATE_CHAT,
        RESPONSE_GROUP_CHAT,
        SYSTEM_INFO
    }
}
