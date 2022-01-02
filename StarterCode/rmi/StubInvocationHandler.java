package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

public class StubInvocationHandler implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;
    private InetSocketAddress ia;
    private Class<?> c;

    public StubInvocationHandler(InetSocketAddress ia, Class<?> c) {
        this.ia = ia;
        this.c = c;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String method_name = method.getName();
        Object[] para = args;
        if(method_name.equals("equals"))
        {
            if(para.length != 1) throw new Error("Wrong number of input parameters for equals method!");
            if(para[0] == null) return false;
            if(!Proxy.isProxyClass(para[0].getClass())) throw new Error("Wrong input for equals method!");
            else
            {
                StubInvocationHandler comp = (StubInvocationHandler)Proxy.getInvocationHandler(para[0]);
                return this.ia.equals(comp.ia) && this.c.equals(comp.c);
            }
        }
        //
        else if(method_name.equals("toString"))
        {
            if(para != null) throw new Error("Wrong number of input para for toString method!");
            return "Interface Name: " + this.c.getName() + " Remote Address: " + this.ia.getHostName() + ":" + this.ia.getPort();
        }
        else if(method_name.equals("hashCode"))
        {
            if(para != null) throw new Error("Wrong number of input para for hashCode method!");
            int hash = 1;
            hash = hash*17 + this.ia.hashCode();
            hash = hash*31 + this.c.hashCode();
            return hash;
        }
        //
        Socket socket;
        MessagePackage inMessagePackage;
        try {
            socket = new Socket();
            System.out.println("Connecting server at " + ia.toString());
            socket.connect(ia);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            String methodName = method.getName();
            Class[] paras_types = method.getParameterTypes();
            Integer l = paras_types.length;
            if ((l != 0 && args==null) && l != args.length) {
                throw new RMIException("Wrong number of parameters");
            }
            Object[] paras = new Object[l];
            for (int i = 0; i < l; i++) {
                if (paras_types[i].isPrimitive()) {
                    paras[i] = args[i];
                } else paras[i] = paras_types[i].cast(args[i]);
            }
            MethodPack methodPack = new MethodPack(methodName, paras);
            MessagePackage outMessagePackage = new MessagePackage(MessageType.METHOD_INVOKE, methodPack);
            System.out.println(methodPack);
            output.writeObject(outMessagePackage);

            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            inMessagePackage = (MessagePackage) input.readObject();
        } catch (IOException e) {
            throw new RMIException("Connection failed!");
        }


        MessageType messageType = inMessagePackage.getMessageType();
        Object data = inMessagePackage.getData();
        System.out.println(data);

        if (messageType.equals(MessageType.INVOKE_FAILED)) {
            System.out.println("Error occurred when invoking the method.");
            socket.close();
            throw  (Throwable) data;
        } else if(messageType.equals(MessageType.INVOKE_SUCCESS)) {
            System.out.println("Invoke successfully.");
            socket.close();
            return data;
        } else {
            socket.close();
            System.out.println("Unknown method");
            return null;
        }
    }

}
