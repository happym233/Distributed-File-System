package rmi;

import java.io.*;
import java.net.Socket;

public class SkeletonService<T> extends Thread{

    private Socket client_socket;
    private Class<T> c;
    private StatusController controller;
    private T server;

    public SkeletonService(Socket socket, Class<T> c, StatusController controller, T server) {
        this.client_socket = socket;
        this.c = c;
        this.controller = controller;
        this.server = server;
    }

    @Override
    public void run(){
            SkeletonMethodHandler<T> skeletonMethodHandler = new SkeletonMethodHandler<>(client_socket, c, controller, server);
            skeletonMethodHandler.handleMessage();

    }

    public Socket getClient_socket() {
        return client_socket;
    }


}
