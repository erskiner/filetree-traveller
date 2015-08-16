package de.n8t.filetree;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static de.n8t.filetree.FileTreeTraveller.EventType.END_DIRECTORY;
import static de.n8t.filetree.FileTreeTraveller.EventType.ENTRY;

class FileTreeIterator implements Iterator<FileTreeTraveller.Event>, Closeable {

    /**
     * An {@code Iterator to iterate over the nodes of a file tree.
     *
     * <pre>{@code
     *     try (FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options)) {
     *         while (iterator.hasNext()) {
     *             Event ev = iterator.next();
     *             Path path = ev.file();
     *             BasicFileAttributes attrs = ev.attributes();
     *         }
     *     }
     * }</pre>
     */

    private FileTreeTraveller walker = null;
    private FileTreeTraveller.Event next;
    private final Directions directions;

    /**
     * Creates a new iterator to travelOn the file tree starting at the given file.
     *
     * @throws  IllegalArgumentException
     *          if {@code maxDepth} is negative
     * @throws IOException
     *          if an I/O errors occurs opening the starting file
     * @throws  SecurityException
     *          if the security manager denies access to the starting file
     * @throws  NullPointerException
     *          if {@code start} or {@code directions} is {@ocde null} or
     *          the directions array contains a {@code null} element
     */
    FileTreeIterator(Directions directions)
            throws IOException
    {
        this.directions = directions;

        // Build FileTreeTraveller, and start walking
        if(directions.recurse()) {
            this.walker = new FileTreeTraveller(directions);
            this.next = walker.walk(directions.start());
            assert next.type() == ENTRY ||
                    next.type() == FileTreeTraveller.EventType.START_DIRECTORY;

            // IOException if there a problem accessing the starting file
            IOException ioe = next.ioeException();
            if (ioe != null)
                throw ioe;
        } else {
            Files.newDirectoryStream(directions.start());
        }
    }


    private void fetchNextIfNeeded() {
        if (next == null) {
            FileTreeTraveller.Event ev = walker.next();
            while (ev != null) {
                IOException ioe = ev.ioeException();
                if (ioe != null) {
                    if(directions.onExceptionReport())
                        System.err.println(getExceptionType(ioe) + ": " + ev.file() );
                    if (directions.onExceptionFailFast())
                        throw new UncheckedIOException(ioe);
                }

                // END_DIRECTORY events are ignored
                if ( !(ev.type() == END_DIRECTORY) ) {
                    next = ev;
                    return;
                }
                ev = walker.next();
            }
        }
    }

    private String getExceptionType(IOException ioe) {
        return ioe.getClass().getSimpleName().replaceAll("Exception", "");
    }

    @Override
    public boolean hasNext() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        return next != null;
    }

    @Override
    public FileTreeTraveller.Event next() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        if (next == null)
            throw new NoSuchElementException();
        FileTreeTraveller.Event result = next;
        next = null;
        return result;
    }

    @Override
    public void close() {
        walker.close();
    }
}
