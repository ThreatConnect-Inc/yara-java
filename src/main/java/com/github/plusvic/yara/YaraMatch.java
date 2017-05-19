package com.github.plusvic.yara;

/**
 * Yara match
 */
public interface YaraMatch {
    /**
     * Value that was matched
     *
     * @return
     */
    String getValue();
    
    /**
     * Raw bytes of match value.
     * 
     * @return
     */
    byte[] getValueBytes(); 

    /**
     * Offset where match was found
     *
     * @return
     */
    long getOffset();
}
