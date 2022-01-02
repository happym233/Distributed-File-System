package rmi;

import java.io.Serializable;

public class MessagePackage<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    private MessageType messageType;
    private T data;

    public MessagePackage(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessagePackage(MessageType messageType, T data) {
        this.messageType = messageType;
        this.data = data;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
