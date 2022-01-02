package naming;

import common.Path;

public class StorageServerManager{
    private Object commandStub;
    private Object clientStub;
    private Boolean isDir;

    public StorageServerManager(Object commandStub, Object clientStub, Boolean isDir) {
        this.commandStub = commandStub;
        this.clientStub = clientStub;
        this.isDir = isDir;
    }

    public Object getCommandStub() {
        return commandStub;
    }

    public void setCommandStub(Object commandStub) {
        this.commandStub = commandStub;
    }

    public Object getClientStub() {
        return clientStub;
    }

    public void setClientStub(Object clientStub) {
        this.clientStub = clientStub;
    }

    public Boolean isDir() {
        return isDir;
    }

    public void setDir(Boolean dir) {
        isDir = dir;
    }
}
