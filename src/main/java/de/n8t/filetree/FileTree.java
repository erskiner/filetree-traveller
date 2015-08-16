package de.n8t.filetree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * I copied some Methods from java.nio.Files, to add a different Behaviour to the FileTreeIterating Mechanism,
 * that are impossible to overwrite since the necessary classes are package private.
 *
 **/

public class FileTree {

    public static Stream<Path> travelOn(Directions directions)
            throws IOException
    {
        FileTreeIterator iterator = new FileTreeIterator(directions);
        try {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT), false)
                    .onClose(iterator::close)
                    .map(entry -> entry.file());
        } catch (Error|RuntimeException e) {
            iterator.close();
            throw e;
        }
    }


    public static Stream<Path> getDirFileContent(Path d){
        File[] files = d.toFile().listFiles();
        Stream<Path> parent = Stream.of(d);
        Stream<Path> content = files == null ? Stream.empty() : Arrays.stream(files)
                                                                      .map(f -> f.toPath())
                                                                      .filter(Files::isRegularFile);
        return Stream.concat(parent, content);
    }

    public static Comparator<File> getFileFirstComparator() {
        return (a, b) -> {
          int r = ( a.isDirectory() && b.isDirectory() || a.isFile() && a.isFile() ) ? a.compareTo(b) :
                  ( a.isDirectory() && b.isFile() ) ? 1  :
                  ( a.isFile() && b.isDirectory() ) ? -1 : -999;
            if ( r == -999) throw new ClassCastException("Not comparable!");
            else return r;
        };
    }
}
