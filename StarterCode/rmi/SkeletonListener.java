package rmi;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class SkeletonListener<T> extends Thread {
    private Class<T> c;
    private T server;
    private InetSocketAddress address;
    private StatusController controller;
    private Skeleton skeleton;
    ArrayList<SkeletonService> services;

    public SkeletonListener (Class<T> c, T server, InetSocketAddress address, StatusController statusController, Skeleton skeleton) {
        this.c = c;
        this.server = server;
        this.address = address;
        this.controller = statusController;
        this.skeleton = skeleton;
        services = new ArrayList<>();
    }

    @Override
    public void run() {
        if (address == null) {
            System.out.println("Skeleton have not been assigned an address.");
            return;
        }
        try {
            System.out.println("The port of this skeleton: " + address.getPort());
            ServerSocket serverSocket = new ServerSocket(address.getPort());
                while (!controller.isStopping()) {
                    Socket socket = serverSocket.accept();
                    if (controller.isStopping()) {
                        socket.close();
                        break;
                    }
                    socket.setSoLinger(true,2);
                    InetAddress clientInfo = socket.getInetAddress();
                    System.out.println("Connection from "+clientInfo.getHostAddress());

                    SkeletonService service = new SkeletonService(socket, c, controller, server);
                    service.start();

                    services.add(service);
                }
                serverSocket.close();

                if (controller.isStopping()) {
                    int count = 1;
                    while (count != 0) {
                        count = 0;
                        Thread.sleep(1000);
                        for (SkeletonService skeletonService:services) {
                            if (skeletonService.isAlive()) {
                                skeletonService.getClient_socket().close();
                                count++;
                            }
                        }
                        System.out.println("Stopping skeleton: " + (services.size() - count) + "/" + services.size()+ " stopped");
                    }
                    System.out.println("Stopping skeleton: " + (services.size() - count) + "/" + services.size()+ " stopped");
                    synchronized (skeleton) {
                        this.controller.stopped();
                        skeleton.notify();
                    }
                }
        } catch (IOException | InterruptedException e) {
           this.skeleton.stopped(e);
        }
    }
}
