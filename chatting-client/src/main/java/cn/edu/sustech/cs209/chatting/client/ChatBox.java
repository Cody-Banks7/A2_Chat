package cn.edu.sustech.cs209.chatting.client;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.MalformedURLException;

public class ChatBox extends HBox {
    Label headline;
    Label timeStamp;
    int chatType;

    public ChatBox(String headline, String timeStamp, int chatType) throws MalformedURLException {
        super(10); // spacing between children
        this.chatType = chatType;
        this.headline = new Label(headline);
        this.headline.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        this.timeStamp = new Label(timeStamp);
        this.timeStamp.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        this.timeStamp.setTextFill(Color.BLUE);

        VBox nameTime = new VBox(5, this.headline, this.timeStamp);
        nameTime.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(nameTime);
    }

    public Label getHeadline() {
        return headline;
    }

    public void setHeadline(Label headline) {
        this.headline = headline;
    }

    public Label getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Label timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }
}
