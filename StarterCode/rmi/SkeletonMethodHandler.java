package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

public class SkeletonMethodHandler<T> {
    private Socket socket;
    private Class<T> c;
    private StatusController controller;
    private T server;


    public SkeletonMethodHandler(Socket socket, Class<T> c, StatusController controller, T server) {
        this.socket = socket;
        this.c = c;
        this.controller = controller;
        this.server = server;
    }



    public void handleMessage() {
        MessagePackage messagePackage;
        MessageType messageType;
        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            while (!controller.isStopping() && (messagePackage = (MessagePackage) input.readObject()) != null) {
                if (controller.isStopping()) {
                    input.close();
                    output.close();
                }
                messageType = messagePackage.getMessageType();
                if (messageType == MessageType.GET_INTERFACE) {
                    output.writeObject(new MessagePackage(MessageType.INTERFACE, c));
                } else if (messageType == MessageType.METHOD_INVOKE) {
                    MessagePackage res = invoke((MethodPack) messagePackage.getData());
                    output.writeObject(res);
                    output.flush();
                } else if (messageType == MessageType.SERVICE_STOP) {
                    break;
                }
            }
            if (controller.isStopping()) {
                output.writeObject(new MessagePackage(MessageType.SERVICE_STOP));
                output.flush();
            }
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {

        }
    }

    public MessagePackage invoke(MethodPack methodPack)  {
        if (methodPack == null) {
            return null;
        }

        try {

            String methodName = methodPack.getMethodName();
            if (methodName == null) {
                return new MessagePackage(MessageType.INVOKE_FAILED, new RMIException("Missing method name"));
            }

            Object[] parameters = methodPack.getParas();
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("Invoked method: " + methodName);
            System.out.println("parameters: ");
            if (parameters == null || parameters.length == 0) {
                System.out.println("null");
            }
            for (Object para:parameters) {
                System.out.println(para);
            }

            Method[] services = this.c.getMethods();
            for (Method service:services) {
                if (service.getName().equals(methodName)) {
                    Class<?>[] serviceParameterTypes = service.getParameterTypes();
                    if ((serviceParameterTypes.length == 0 && parameters == null) || serviceParameterTypes.length == parameters.length) {
                        boolean isParametersSame = true;
                        for (int i = 0; i < serviceParameterTypes.length; i++) {
                            if (parameters[i] == null) {}
                            else {
                                String parameterType = parameters[i].getClass().getSimpleName().toLowerCase();
                                String serviceType = serviceParameterTypes[i].getSimpleName().toLowerCase();

                                if (!parameterType.contains(serviceType)) {
                                    if (parameterType.contains("$proxy")) {
                                        parameters[i] = serviceParameterTypes[i].cast(parameters[i]);
                                    }else {
                                        isParametersSame = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (isParametersSame) {
                            return new MessagePackage(MessageType.INVOKE_SUCCESS, (Serializable) service.invoke(server, parameters));
                        }
                    }
                }
            }
            return new MessagePackage(MessageType.INVOKE_FAILED, new RMIException("No such method ("  + methodName + ") with given parameters"));
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return new MessagePackage(MessageType.INVOKE_FAILED, e.getTargetException());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new MessagePackage(MessageType.INVOKE_FAILED, e);
        }

    }

}
