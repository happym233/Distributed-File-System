package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    private HashMap<Path, StorageServerManager> fileMap;
    private Skeleton<Registration> registrationSkeleton;
    private Skeleton<Service> serviceSkeleton;
    private ArrayList<Object> clientStubs;
    private ArrayList<Object> commandStubs;
    private NamingServerStatusController controller;
    private Random rn;


    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        this.fileMap = new HashMap<>();
        InetSocketAddress regAddress = new InetSocketAddress("localhost", NamingStubs.REGISTRATION_PORT);
        InetSocketAddress serviceAddress = new InetSocketAddress("localhost", NamingStubs.SERVICE_PORT);
        System.out.println("============================================================");
        System.out.println("Starting registration skeleton: ");
        this.registrationSkeleton = new Skeleton<>(Registration.class, this, regAddress);
        System.out.println("============================================================");

        System.out.println("============================================================");
        this.serviceSkeleton = new Skeleton<>(Service.class, this, serviceAddress);
        System.out.println("Starting registration skeleton: ");
        System.out.println("============================================================");
        this.controller = new NamingServerStatusController();


        this.commandStubs = new ArrayList<>();
        this.clientStubs = new ArrayList<>();
        this.rn = new Random();
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        if (this.controller.isRunning()) {
            System.out.println("The naming server have been started.");
            return;
        } else if (this.controller.errored()) {
            System.out.println("Exception occurred in this server.");
        } else {
            this.controller.start();
            try {
                this.registrationSkeleton.start();
                this.serviceSkeleton.start();
            } catch (RMIException e) {
                this.controller.setHasError(true);
                this.registrationSkeleton.stop();
                this.serviceSkeleton.stop();
                this.controller.stop();
                throw e;
            }
        }
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        System.out.println("Stopping Naming server");
       if (this.controller.isRunning()) {
           this.registrationSkeleton.stop();
           this.serviceSkeleton.stop();
           this.controller.stop();
           this.stopped(new Throwable());
       }
       else {
           System.out.println("This serve have been stopped.");
       }
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
        stop();
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException, NullPointerException
    {
        if (path == null)
            throw new NullPointerException("Input path should not be null.");
        if (path.isRoot()) return true;
        if (this.fileMap.containsKey(path)) {
            return this.fileMap.get(path).isDir();
        }
        else throw new FileNotFoundException("File not found in input path.");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException, NullPointerException
    {
        if (directory == null) {
            throw new NullPointerException("Input directory should not be null.");
        }
        else if (!directory.isRoot() && (!fileMap.containsKey(directory) || !fileMap.get(directory).isDir())) {
            throw new FileNotFoundException("Target directory not found.");
        }

        ArrayList<String> res = new ArrayList<>();

        System.out.println("parent: " + directory);

        for (Path path: fileMap.keySet()) {
            System.out.println("check: " + path + " whose parent: " + path.parent());
            if (path.parent().equals(directory)) {
                res.add(path.last());
            }
        }
        return (String[])res.toArray(new String[res.size()]);
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException, NullPointerException
    {
        if (file == null) throw new NullPointerException("Input file should not be null.");
        else if (file.isRoot()) return false;
        else if (this.fileMap.containsKey(file)) {
            return false;
        } else {
            Path parent = file.parent();
            if (parent.isRoot() && clientStubs.size() > 0) {
                this.fileMap.put(file, new StorageServerManager(commandStubs.get(0), clientStubs.get(0), false));
                return ((Command)commandStubs.get(0)).create(file);
            }
            if (this.fileMap.containsKey(parent) && this.fileMap.get(parent).isDir()) {
                StorageServerManager storageServerManager = this.fileMap.get(parent);
                this.fileMap.put(file, new StorageServerManager(storageServerManager.getCommandStub(), storageServerManager.getClientStub(), false));
                return ((Command)storageServerManager.getCommandStub()).create(file);
            }
            throw new FileNotFoundException("Parent directory of target file not found.");
        }
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException, RMIException, NullPointerException {
        if (directory == null) throw new NullPointerException("Input directory should not be null.");
        if (directory.isRoot()) return false;
        if (this.fileMap.containsKey(directory)) return false;
        else {
            int storageServerNum = clientStubs.size();
            if (storageServerNum == 0) {
                throw new FileNotFoundException("No storage server is registered yet.");
            }
            int randomStorage = rn.nextInt(storageServerNum);
            Path parent = directory.parent();
            if (parent.isRoot()) {
                this.fileMap.put(directory, new StorageServerManager(commandStubs.get(randomStorage), clientStubs.get(randomStorage), true));
                return true;
            }
            if (this.fileMap.containsKey(parent) && this.fileMap.get(parent).isDir()) {
                StorageServerManager storageServerManager =  this.fileMap.get(parent);
                this.fileMap.put(directory, new StorageServerManager(storageServerManager.getCommandStub(), storageServerManager.getClientStub(), true));
                return true;
            }
            throw new FileNotFoundException("Parent of target directory is not found.");
        }
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException, NullPointerException
    {
        if (file == null) throw new NullPointerException("Input file should not be null");
        else if (file.isRoot() || !fileMap.containsKey(file)) throw new FileNotFoundException("Target file not found.");
        else if (fileMap.get(file).isDir()) throw new FileNotFoundException("Target file is directory.");
        else {
            System.out.println("target file: " + file + " is directory: " + fileMap.get(file).isDir());
            return ((Storage) fileMap.get(file).getClientStub());
        }
    }


    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) throws NullPointerException, IllegalStateException
    {
        if (client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException("Input should not be null.");
        }
        if (this.clientStubs.contains(client_stub) || this.commandStubs.contains(command_stub)) {
            throw new IllegalStateException("This stub have been registered");
        }

        ArrayList<Path> pathLists = new ArrayList<>();
        int fileNum = files.length;
        for (int i = 0; i < fileNum; i++) {
            if (files[i].isRoot()) {
                continue;
            } else {
                if (this.fileMap.containsKey(files[i])) pathLists.add(files[i]);
                else {
                    this.fileMap.put(files[i], new StorageServerManager(command_stub, client_stub, false));
                    ArrayList<Path> directories = getDirectoryToFile(files[i]);
                    for (Path directory:directories) {
                        if (!this.fileMap.containsKey(directory)) {
                            this.fileMap.put(directory, new StorageServerManager(command_stub, client_stub, true));
                        }
                    }
                }
            }
        }
        this.clientStubs.add(client_stub);
        this.commandStubs.add(command_stub);
        return (Path[]) pathLists.toArray(new Path[pathLists.size()]);
    }

    public ArrayList<Path> getDirectoryToFile(Path file) {
        ArrayList<Path> directories = new ArrayList<>();
        if (file.isRoot()) {
            return directories;
        }
        Path path = file.parent();
        while (!path.isRoot()) {
            directories.add(path);
            System.out.println("Path: " + path);
            path = path.parent();
        }
        return directories;
    }
}
