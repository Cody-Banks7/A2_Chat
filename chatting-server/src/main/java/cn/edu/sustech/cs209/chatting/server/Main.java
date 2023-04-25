package cn.edu.sustech.cs209.chatting.server;

import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        System.out.println("Start server: ");
        try {
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
