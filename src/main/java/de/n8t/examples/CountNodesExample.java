package de.n8t.examples;

import de.n8t.filetree.Directions;
import de.n8t.filetree.FileTree;
import de.n8t.filetree.TraversalOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

/**
 * Example that counts the different types of filesystem nodes.
 * Try out enabling the different Options to find out how to manipulate the FileTreeTraversal.
 */
public class CountNodesExample
{
    public static void main( String[] args )
    {
        try {
            Directions directions = new Directions(
                      Paths.get("/")
//                    , TraversalOption.FOLLOW_LINKS        // Traversal will take much longer with this option on
//                    , TraversalOption.ONLY_DIRS
//                    , TraversalOption.NO_LINK_ENTRIES
//                    , TraversalOption.NO_EXOTIC_ENTRIES
//                    , TraversalOption.ON_EXCEPTION_FAIL
                    , TraversalOption.ON_EXCEPTION_LOG
            )
                    .blockPaths("/proc", "/tmp", "/home")
            ;

            int[] results =
                FileTree.travelOn(directions)
                        .parallel()
                        .map(p -> Files.isSymbolicLink(p) ? new int[]{1, 0, 0, 0} : // Symbolic Link
                                  Files.isRegularFile(p)  ? new int[]{0, 1, 0, 0} : // File
                                  Files.isDirectory(p)    ? new int[]{0, 0, 1, 0} : // Directory
                                                            new int[]{0, 0, 0, 1} ) // FIFO, etc..
                        .reduce(new int[]{0, 0, 0, 0},
                                (a, b) -> new int[]{a[0]+b[0], a[1]+b[1], a[2]+b[2], a[3]+b[3]})
            ;

            System.out.println( "" );
            System.out.println( "Directories: " + DecimalFormat.getNumberInstance().format(results[2]) );
            System.out.println( "Files: " + DecimalFormat.getNumberInstance().format(results[1]) );
            System.out.println( "Links (Files/Dirs): " + DecimalFormat.getNumberInstance().format(results[0]) );
            System.out.println( "Other (FIFO, etc...): " + DecimalFormat.getNumberInstance().format(results[3]) );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
