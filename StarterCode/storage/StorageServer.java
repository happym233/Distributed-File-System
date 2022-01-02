package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private File root;
    private Path rootPath;
    private String rootPathStr;
    private int storagePort;
    private int commandPort;
    private Skeleton<Command> commandSkeleton;
    private Skeleton<Storage> storageSkeleton;
    private StorageServerStatusController controller;


    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
        if (root == null) {
            throw new NullPointerException("Input should not be null");
        }
        this.root = root;
        this.rootPath = new Path();
        this.rootPathStr =root.getAbsolutePath();
        if (isWindows()) {
            this.rootPathStr = this.rootPathStr.replace('\\', '/');
        }
        this.storagePort = 8000;
        this.commandPort = 8001;
        this.controller = new StorageServerStatusController();
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (this.controller.isRunning()) {
            System.out.println("This storage server is running.");
            return;
        }
        InetSocketAddress commandSocketAddress = new InetSocketAddress(hostname, this.commandPort);
        this.commandSkeleton = new Skeleton<>(Command.class, this, commandSocketAddress);
        InetSocketAddress storageSocketAddress = new InetSocketAddress(hostname, this.storagePort);
        this.storageSkeleton = new Skeleton<>(Storage.class, this, storageSocketAddress);
        Path[] fileList = Path.list(this.root);
        Command commandStub = Stub.create(Command.class, this.commandSkeleton);
        Storage storageStub = Stub.create(Storage.class, this.storageSkeleton);
        Path[] deleteFiles = naming_server.register(storageStub, commandStub, fileList);
        for (Path deleteFile:deleteFiles) {
            File file = new File(this.rootPathStr + deleteFile.toString());
            deleteAll(file);
        }
        clearEmptyDir(root);
        this.controller.start();
        this.commandSkeleton.start();
        this.storageSkeleton.start();
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        System.out.println("Stopping storage server");
        if (this.controller.isRunning()) {
            this.commandSkeleton.stop();
            this.storageSkeleton.stop();
            this.controller.stop();
        } else {
            System.out.println("Storage server already stopped.");
        }
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
        cause.printStackTrace();
    }



    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        if (file == null) throw new NullPointerException("Input should not be null.");
        String localPath = this.rootPathStr + file.toString();
        File targetFile = new File(localPath);
        if (targetFile.exists() && targetFile.isFile()) return targetFile.length();
        else throw new FileNotFoundException("Target file not found");
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        if (file == null) {
            throw new NullPointerException("Input should not be null");
        }
        String localPath = this.rootPathStr + file.toString();
        File targetFile = new File(localPath);
        if (targetFile.exists() && targetFile.isFile()) {
            long totalLength = targetFile.length();
            if (totalLength < offset+length || length < 0) throw new IndexOutOfBoundsException();
            FileInputStream input = new FileInputStream(targetFile);
            byte[] res = new byte[length];
            input.read(res, (int)offset, length);
            input.close();
            return res;
        } else  {
            throw new FileNotFoundException("Target file not found.");
        }
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        if (file == null || data == null) {
            throw new NullPointerException("Input should not be null");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Offset should be greater than 0.");
        }

        String localPath = this.rootPathStr + file.toString();
        File targetFile = new File(localPath);
        if (targetFile.exists() && targetFile.isFile()) {
            RandomAccessFile rTargetFile = new RandomAccessFile(targetFile, "rw");
            rTargetFile.seek(offset);
            rTargetFile.write(data);
            rTargetFile.close();
        } else throw new FileNotFoundException("Target file not found.");
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file == null) {
            throw new NullPointerException("Input should not be null");
        }
        if (file.isRoot()) {
            return false;
        }

        Path parent = file.parent();
        String parentLocalPath = this.rootPathStr + parent.toString();
        String fileLocalPath = this.rootPathStr + file.toString();

        File parentDir = new File(parentLocalPath);
        File targetFile = new File(fileLocalPath);
        parentDir.mkdirs();

        try {
            return targetFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path) throws NullPointerException
    {
        if (path == null) {
            throw new NullPointerException("Input path should not be null.");
        }

        if (path.isRoot()) {
            return false;
        }

        File targetFile = path.toFile(this.root);
        if (targetFile == null) {
            return false;
        }
        return deleteAll(targetFile);
    }

    public boolean deleteAll(File file) {
        try {
            if (file.isFile() || (file.isDirectory() && file.listFiles().length == 0)) {
                return file.delete();
            } else {
                boolean deleted = true;
                for (File f : file.listFiles()) {
                    Boolean deletedAll = deleteAll(f);
                    if (!deletedAll) {
                        deleted = false;
                    }
                }
                return deleted && file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearEmptyDir(File directory) {
        if (directory.listFiles().length == 0) {
            directory.delete();
        } else  {
            for (File dir: directory.listFiles()) {
                System.out.println(dir.toString());
                if (dir.isDirectory()) {
                    clearEmptyDir(dir);
                }
            }
            if (directory.listFiles().length == 0) {
                directory.delete();
            }
        }
    }
}
