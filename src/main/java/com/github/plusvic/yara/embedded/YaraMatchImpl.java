package com.github.plusvic.yara.embedded;

import com.github.plusvic.yara.YaraMatch;

import static com.github.plusvic.yara.Preconditions.checkArgument;

/**
 * Yara rule match
 */
public class YaraMatchImpl implements YaraMatch {
    private final YaraLibrary library;
    private final long peer;
    private final long stringPeer;

    YaraMatchImpl(YaraLibrary library, long peer, long stringPeer) {
        checkArgument(library != null);
        checkArgument(peer != 0);
        checkArgument(stringPeer != 0);

        this.library = library;
        this.peer = peer;
        this.stringPeer = stringPeer;
    }

    /**
     * Value that was matched
     * @return
     */
    public String getValue() {
        return library.matchValue(peer, stringPeer);
    }

    /**
     * Offset where match was found
     * @return
     */
    public long getOffset() {
        return library.matchOffset(peer);
    }
}
