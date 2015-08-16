package de.n8t.filetree;

import java.nio.file.*;

public class Directions {

    private Path start;

    private int maxDepth = Integer.MAX_VALUE;

    private final LinkOption[] linkOptions;
    private boolean followLinks = false;
    private boolean walkDirectoriesOnly = false;
    private boolean noLinkEntries = false;
    private boolean noExoticEntries = false;
    private boolean onExceptionFailFast = false;
    private boolean onExceptionReport = false;
    private boolean recurse = true;

    private DirectoryStream.Filter<? super Path> pathFilter;

    public Directions(Path start, TraversalOption... options) {
        this(start, Integer.MAX_VALUE, options);
    }

    public Directions(Path start, int maxDepth, TraversalOption... options) {
        this.start = start;

        if (maxDepth < 0) throw new IllegalArgumentException("'maxDepth' is negative");
        this.maxDepth = maxDepth;

        Boolean[] optionsAsArray = parseOptions(options);
        if (optionsAsArray[0] != null) followLinks = optionsAsArray[0];
        if (optionsAsArray[1] != null) walkDirectoriesOnly = optionsAsArray[1];
        if (optionsAsArray[2] != null) onExceptionFailFast = optionsAsArray[2];
        if (optionsAsArray[3] != null) noLinkEntries = optionsAsArray[3];
        if (optionsAsArray[4] != null) noExoticEntries = optionsAsArray[4];
        if (optionsAsArray[5] != null) onExceptionReport = optionsAsArray[5];
        if (optionsAsArray[6] != null) recurse = optionsAsArray[6];
        this.linkOptions = (followLinks) ? new LinkOption[0] :
                new LinkOption[] { LinkOption.NOFOLLOW_LINKS };
        setDefaultPathFilter();
    }

    private Boolean[] parseOptions(TraversalOption[] options) {
        Boolean[] result = new Boolean[7];
        for (TraversalOption option: options) {
            switch (option) {
                // will throw NPE if options contains null
                case FOLLOW_LINKS       : result[0] = true; break;
                case ONLY_DIRS          : result[1] = true; break;
                case ON_EXCEPTION_FAIL  : result[2] = true; break;
                case NO_LINK_ENTRIES    : result[3] = true; break;
                case NO_EXOTIC_ENTRIES  : result[4] = true; break;
                case ON_EXCEPTION_LOG   : result[5] = true; break;
                case RECURSE            : result[6] = true; break;
                default:
                    throw new AssertionError("Should not get here");
            }
        }
        return result;
    }

    private void setDefaultPathFilter() {
        pathFilter = p -> {
            if (pathFilteredByDefault(p)) return false;
            return true;
        };

    }

    private boolean pathFilteredByDefault(Path p) {
        if (walkDirectoriesOnly && !Files.isDirectory(p))                       return true;
        if (noLinkEntries && Files.isSymbolicLink(p))                           return true;
        if (noExoticEntries &&!Files.isDirectory(p) && !Files.isRegularFile(p)) return true;
        return false;
    }

    // Modifier for PathFilter

    public Directions blockPaths(final String... prohibitedSubDirs) {
        pathFilter = p -> {
            if (pathFilteredByDefault(p)) return false;
            for(String filterPath: prohibitedSubDirs) {
                if ( p.startsWith( Paths.get(filterPath)) )       return false;
            }
            return true;
        };
        return this;
    }

    public Path start() {
        return start;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public boolean followLinks() {
        return followLinks;
    }

    public boolean walkDirectoriesOnly() {
        return walkDirectoriesOnly;
    }

    public boolean onExceptionFailFast() {
        return onExceptionFailFast;
    }

    public boolean onExceptionReport() { return onExceptionReport; }

    public DirectoryStream.Filter<? super Path> pathFilter() {
        return pathFilter;
    }

    public LinkOption[] linkOptions() {
        return linkOptions;
    }

    public boolean recurse() {
        return recurse;
    }
}
