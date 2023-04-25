package cn.edu.sustech.cs209.chatting.client;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.net.MalformedURLException;

public class ChatBox extends HBox {
    Label title;
    Label timeStamp;
    int chatType;

    public ChatBox(String title, String timeStamp, int chatType) throws MalformedURLException {
        super(10); // spacing between children
        this.chatType = chatType;
        this.title = new Label(title);
        this.title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        this.timeStamp = new Label(timeStamp);
        this.timeStamp.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        this.timeStamp.setTextFill(Color.GRAY);

        VBox nameTime = new VBox(5, this.title, this.timeStamp);
        nameTime.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(nameTime);
    }

    public Label getTitle() {
        return title;
    }

    public void setTitle(Label title) {
        this.title = title;
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
