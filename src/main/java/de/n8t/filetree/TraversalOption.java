/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package de.n8t.filetree;

import java.nio.file.Files;

/**
 * Defines the file tree traversal options.
 *
 * @since 1.7
 *
 * @see Files#walkFileTree
 */

public enum TraversalOption {
    /**
     * Follow symbolic links.
     */
    FOLLOW_LINKS,
    /**
     * Only traverse tree directory nodes
     */
    ONLY_DIRS,
    /**
     * Don't report symlink entries, this will imply !FOLLOW_LINKS (if both are set, links will not be followed)
     */
    NO_LINK_ENTRIES,
    /**
     * Don't report FIFOs, Character Devices, ...
     */
    NO_EXOTIC_ENTRIES,
    /**
     * If set, will abort traversal on any exception (like the corresponding Java8)
     */
    ON_EXCEPTION_FAIL,
    /**
     * If set, will log any Exception that is encountered, but does not abort
     */
    ON_EXCEPTION_LOG,
    /**
     * If set, directory will be walked recursevly (eventually leading to a filetree walk)
     */
    RECURSE,
    ;
}
