package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{

    private String pathStr;

    /** Creates a new path which represents the root directory. */
    public Path()
    {
        this.pathStr = "/";
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if (component == null || component.isEmpty()) {
            throw new IllegalArgumentException("String must not be null or empty");
        } else if (component.contains(":")) {
            throw new IllegalArgumentException("Component string cannot contain a colon");
        } else if ( component.contains("/")) {
            throw new IllegalArgumentException("Component string cannot contain a separator.");
        }
        if (path.pathStr == "/") {
            this.pathStr = path.pathStr + component;
        } else {
           this.pathStr = path.pathStr + "/" + component;
        }
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Empty path string");
        } else if (path.charAt(0) != '/') {
            throw new IllegalArgumentException("Input path must start with /");
        } else if (path.contains(":")) {
            throw new IllegalArgumentException("Path contains :");
        }
        if (path == "/") {
            this.pathStr = path;
            return;
        }
        this.pathStr = path;
        int i = 1;
        while (i != this.pathStr.length()) {
            if (this.pathStr.charAt(i) == '/' && this.pathStr.charAt(i-1) == '/') {
                this.pathStr = this.pathStr.substring(0,i) + this.pathStr.substring(i+1);
            } else {
                i++;
            }
        }
        if (this.pathStr.length() > 1 && this.pathStr.charAt(this.pathStr.length()-1) == '/') {
            this.pathStr = this.pathStr.substring(0, this.pathStr.length()-1);
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        return new PathIter();
    }

    private class PathIter implements Iterator<String> {
        private String[] com = pathStr.split("/");
        private Integer i;
        private Integer l = com.length;

        PathIter() {
            i=1;
        }
        @Override
        public boolean hasNext() {
            return i!=l;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Iterator out of boundary");
            }
            return com[i++];
        }
    };

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if(!directory.exists()) {
            throw new FileNotFoundException("The root directory doer not exist.");
        } else if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The input file is not a directory.");
        }
        File[] fileList = directory.listFiles();

        return getFilePathUnderDirectory("/", directory).toArray(new Path[0]);
    }

    private static ArrayList<Path> getFilePathUnderDirectory(String pathStr, File directory) {
        ArrayList<Path> paths = new ArrayList<>();
        File[] fileList = directory.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {
//                System.out.println(fileList[i].getName());
                paths.add(new Path(pathStr + fileList[i].getName()));
            }
            if (fileList[i].isDirectory()) {
                paths.addAll(getFilePathUnderDirectory(pathStr + fileList[i].getName() + "/", fileList[i]));
            }
        }
        return paths;
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.pathStr.strip().equals("/");
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent() throws IllegalArgumentException
    {
        if (this.isRoot()) {
            throw new IllegalArgumentException("Root do not have a parent");
        }
        String[] pathList = this.pathStr.split("/");
//        for (int i = 0; i < pathList.length; i++) {
//            System.out.println(pathList[i]);
//        }
        String parentPathStr = "";
        for (int i = 0; i < pathList.length-1; i++) {
            parentPathStr +="/"+pathList[i];
        }
        return new Path(parentPathStr);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (this.isRoot()) {
            throw new IllegalArgumentException("Root path do not have last component.");
        }
        String[] pathList = this.pathStr.split("/");
        return  pathList[pathList.length-1];
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        if (this.equals(other)) return true;
//        System.out.println(this.pathStr+ "======"+ other.pathStr);
        if (other.isRoot())
            return true;
        int lt = this.pathStr.length();
        int lo = other.pathStr.length();
        if (lt < lo) {
            return false;
        }
        int i = 0;
        for (i = 0; i < lo; i++) {
            if (this.pathStr.charAt(i) != other.pathStr.charAt(i)) {
                return false;
            }
        }
        if (lt > lo && this.pathStr.charAt(lo) != '/') {
            return false;
        }
        return true;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        File file = new File(root.getAbsolutePath() + pathStr);
        return file;
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this==other) return true;
        if (!(other instanceof Path)) return false;
        Path otherPath = (Path) other;
        return this.pathStr.equals(otherPath.pathStr);
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
       return this.pathStr.hashCode();
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        return pathStr;
    }
}
