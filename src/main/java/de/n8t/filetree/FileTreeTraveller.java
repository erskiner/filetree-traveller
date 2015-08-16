package de.n8t.filetree;


import sun.nio.fs.BasicFileAttributesHolder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Copied from internal java.nio.file Class:
 *
 * Walks a file tree, generating a sequence of events corresponding to the files
 * in the tree.
 *
 * <pre>{@code
 *     Path top = ...
 *     Set<TraversalOption> directions = ...
 *     int maxDepth = ...
 *
 *     try (FileTreeTraveller walker = new FileTreeTraveller(directions, maxDepth)) {
 *         FileTreeTraveller.Event ev = walker.travelOn(top);
 *         do {
 *             process(ev);
 *             ev = walker.next();
 *         } while (ev != null);
 *     }
 * }</pre>
 *
 * @see Files#walkFileTree
 */

class FileTreeTraveller implements Closeable {
    private final Directions directions;
    private final ArrayDeque<DirectoryNode> stack = new ArrayDeque<>();
    private boolean closed;

    /**
     * The element on the walking stack corresponding to a directory node.
     */
    private static class DirectoryNode {
        private final Path dir;
        private final Object key;
        private final DirectoryStream<Path> stream;
        private final Iterator<Path> iterator;
        private boolean skipped;

        DirectoryNode(Path dir, Object key, DirectoryStream<Path> stream) {
            this.dir = dir;
            this.key = key;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        Path directory() {
            return dir;
        }

        Object key() {
            return key;
        }

        DirectoryStream<Path> stream() {
            return stream;
        }

        Iterator<Path> iterator() {
            return iterator;
        }

        void skip() {
            skipped = true;
        }

        boolean skipped() {
            return skipped;
        }
    }

    /**
     * The event types.
     */
    static enum EventType {
        /**
         * Start of a directory
         */
        START_DIRECTORY,
        /**
         * End of a directory
         */
        END_DIRECTORY,
        /**
         * An entry in a directory
         */
        ENTRY;
    }

    /**
     * Events returned by the {@link #walk} and {@link #next} methods.
     */
    static class Event {
        private final EventType type;
        private final Path file;
        private final BasicFileAttributes attrs;
        private final IOException ioe;

        private Event(EventType type, Path file, BasicFileAttributes attrs, IOException ioe) {
            this.type = type;
            this.file = file;
            this.attrs = attrs;
            this.ioe = ioe;
        }

        Event(EventType type, Path file, BasicFileAttributes attrs) {
            this(type, file, attrs, null);
        }

        Event(EventType type, Path file, IOException ioe) {
            this(type, file, null, ioe);
        }

        EventType type() {
            return type;
        }

        Path file() {
            return file;
        }

        BasicFileAttributes attributes() {
            return attrs;
        }

        IOException ioeException() {
            return ioe;
        }
    }

    /**
     * Creates a {@code FileTreeTraveller}.
     *
     * @throws  IllegalArgumentException
     *          if {@code maxDepth} is negative
     * @throws  ClassCastException
     *          if (@code directions} contains an element that is not a
     *          {@code TraversalOption}
     * @throws  NullPointerException
     *          if {@code directions} is {@ocde null} or the directions
     *          array contains a {@code null} element
     */
    FileTreeTraveller(Directions directions) {
        this.directions = directions;
    }

    /**
     * Returns the attributes of the given file, taking into account whether
     * the travelOn is following sym links is not. The {@code canUseCached}
     * argument determines whether this method can use cached attributes.
     */
    private BasicFileAttributes getAttributes(Path file, boolean canUseCached)
            throws IOException
    {
        // if attributes are cached then use them if possible
        if (canUseCached &&
                (file instanceof BasicFileAttributesHolder) &&
                (System.getSecurityManager() == null))
        {
            BasicFileAttributes cached = ((BasicFileAttributesHolder)file).get();
            if (cached != null && (!directions.followLinks() || !cached.isSymbolicLink())) {
                return cached;
            }
        }

        // attempt to get attributes of file. If fails and we are following
        // links then a link target might not exist so get attributes of link
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(file, BasicFileAttributes.class, directions.linkOptions());
        } catch (IOException ioe) {
            if (!directions.followLinks())
                throw ioe;

            // attempt to get attrmptes without following links
            attrs = Files.readAttributes(file,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
        }
        return attrs;
    }

    /**
     * Returns true if walking into the given directory would result in a
     * file system loop/cycle.
     */
    private boolean wouldLoop(Path dir, Object key) {
        // if this directory and ancestor has a file key then we compare
        // them; otherwise we use less efficient isSameFile test.
        for (DirectoryNode ancestor: stack) {
            Object ancestorKey = ancestor.key();
            if (key != null && ancestorKey != null) {
                if (key.equals(ancestorKey)) {
                    // cycle detected
                    return true;
                }
            } else {
                try {
                    if (Files.isSameFile(dir, ancestor.directory())) {
                        // cycle detected
                        return true;
                    }
                } catch (IOException | SecurityException x) {
                    // ignore
                }
            }
        }
        return false;
    }

    /**
     * Visits the given file, returning the {@code Event} corresponding to that
     * visit.
     *
     * The {@code ignoreSecurityException} parameter determines whether
     * any SecurityException should be ignored or not. If a SecurityException
     * is thrown, and is ignored, then this method returns {@code null} to
     * mean that there is no event corresponding to a visit to the file.
     *
     * The {@code canUseCached} parameter determines whether cached attributes
     * for the file can be used or not.
     */
    private Event visit(Path entry, boolean ignoreSecurityException, boolean canUseCached) {
        // need the file attributes
        BasicFileAttributes attrs;
        try {
            attrs = getAttributes(entry, canUseCached);
        } catch (IOException ioe) {
            return new Event(EventType.ENTRY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException)
                return null;
            throw se;
        }

        // at maximum depth or file is not a directory
        int depth = stack.size();
        if (depth >= directions.maxDepth() || !attrs.isDirectory()) {
            return new Event(EventType.ENTRY, entry, attrs);
        }

        // check for cycles when following links
        if (directions.followLinks() && wouldLoop(entry, attrs.fileKey())) {
            return new Event(EventType.ENTRY, entry,
                    new FileSystemLoopException(entry.toString()));
        }

        // file is a directory, attempt to open it
        DirectoryStream<Path> stream = null;
        try {
            stream = directions.pathFilter() != null ? Files.newDirectoryStream(entry, directions.pathFilter())
                                        : Files.newDirectoryStream(entry);
        } catch (IOException ioe) {
            return new Event(EventType.ENTRY, entry, ioe);
        } catch (SecurityException se) {
            if (ignoreSecurityException)
                return null;
            throw se;
        }

        // push a directory node to the stack and return an event
        stack.push(new DirectoryNode(entry, attrs.fileKey(), stream));
        return new Event(EventType.START_DIRECTORY, entry, attrs);
    }


    /**
     * Start walking from the given file.
     */
    Event walk(Path file) {
        if (closed)
            throw new IllegalStateException("Closed");

        Event ev = visit(file,
                false,   // ignoreSecurityException
                false);  // canUseCached
        assert ev != null;
        return ev;
    }

    /**
     * Returns the next Event or {@code null} if there are no more events or
     * the walker is closed.
     */
    Event next() {
        DirectoryNode top = stack.peek();
        if (top == null)
            return null;      // stack is empty, we are done

        // continue iteration of the directory at the top of the stack
        Event ev;
        do {
            Path entry = null;
            IOException ioe = null;

            // get next entry in the directory
            if (!top.skipped()) {
                Iterator<Path> iterator = top.iterator();
                try {
                    if (iterator.hasNext()) {
                        entry = iterator.next();
                    }
                } catch (DirectoryIteratorException x) {
                    ioe = x.getCause();
                }
            }

            // no next entry so close and pop directory, creating corresponding event
            if (entry == null) {
                try {
                    top.stream().close();
                } catch (IOException e) {
                    if (ioe != null) {
                        ioe = e;
                    } else {
                        ioe.addSuppressed(e);
                    }
                }
                stack.pop();
                return new Event(EventType.END_DIRECTORY, top.directory(), ioe);
            }

            // visit the entry
            ev = visit(entry,
                    true,   // ignoreSecurityException
                    true);  // canUseCached

        } while (ev == null);

        return ev;
    }

    /**
     * Pops the directory node that is the current top of the stack so that
     * there are no more events for the directory (including no END_DIRECTORY)
     * event. This method is a no-op if the stack is empty or the walker is
     * closed.
     */
    void pop() {
        if (!stack.isEmpty()) {
            DirectoryNode node = stack.pop();
            try {
                node.stream().close();
            } catch (IOException ignore) { }
        }
    }

    /**
     * Skips the remaining entries in the directory at the top of the stack.
     * This method is a no-op if the stack is empty or the walker is closed.
     */
    void skipRemainingSiblings() {
        if (!stack.isEmpty()) {
            stack.peek().skip();
        }
    }

    /**
     * Returns {@code true} if the walker is open.
     */
    boolean isOpen() {
        return !closed;
    }

    /**
     * Closes/pops all directories on the stack.
     */
    @Override
    public void close() {
        if (!closed) {
            while (!stack.isEmpty()) {
                pop();
            }
            closed = true;
        }
    }
}
