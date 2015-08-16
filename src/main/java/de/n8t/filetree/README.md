# FileTreeTraveller #
## Introduction ##

Java8 only provides one standard Implementation for walking FileTrees.
 
The method `Files.walk(Path p)` in Java8 has the following Properties
  * it always aborts walking, when encountering an checked exception (e.g. Access denied) for a single File or Directory
  * applying a filter on a Stream is done late, so the whole Filetree is actually walked, even subtrees prohibited by the filter

so I wanted
  * a robust implementation, that ignores or reports exceptions encountered with single nodes, but does not fail 
  * to filter directly during walking, preventing the traversal of the prohibited subtrees, symlinks, regular files ... 

Since java.nio.FileIterator and java.nie FileTreeWalker are package private and cannot be overwritten or extended, 
I had to write my own classes based on the regular java.nio.FileIterator.

## Installation ##

*TBD:* Maven maybe

## Usage ##

1. Create a `Directions` Object, with a starting Point and all necessary Options.
2. You can then simply do a `FileTree.travelOn(Directons directions)` to obtain a lazy bound stream object 
(similiar to `Files.walk(Path p)`). On this you can do all the stream magic possible with java8.

For a Example of the usagee see de.uniek.CountNodesExample
