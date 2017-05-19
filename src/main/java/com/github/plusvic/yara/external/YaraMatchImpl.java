package com.github.plusvic.yara.external;

import com.github.plusvic.yara.YaraMatch;

public class YaraMatchImpl implements YaraMatch {
    private String value;
    private long offset;

    public YaraMatchImpl(long offset, String value) {
        this.offset = offset;
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Raw bytes of match value.
     * 
     * @return
     */
    public byte[] getValueBytes() {
        return getValue().getBytes();
    }

    @Override
    public long getOffset() {
        return offset;
    }
}
