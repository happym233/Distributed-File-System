package test;

import rmi.Skeleton;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TestMain {

    private interface C{

    }
    private static class S implements C{

    }


    public static void main(String[] args) {
        S s = new S();
        Skeleton skeleton = new Skeleton(C.class,s);
        try {
            skeleton.start();
            InetAddress inetAddress = InetAddress.getByName("localhost");
            Socket socket = new Socket(inetAddress, 8080);
            Socket socket2 = new Socket(inetAddress, 8080);
            Socket socket3 = new Socket(inetAddress, 8080);
            Socket socket4 = new Socket(inetAddress, 8080);
            // System.out.println("11111111111111");
            skeleton.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
