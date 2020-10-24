package com.anatawa12.mdfServerUtils.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCacheMap<K, V> extends LinkedHashMap<K, V> {
    /**
     * シリアライズバージョン
     */
    private static final long serialVersionUID = 1L;

    /**
     * キャッシュエントリ最大数
     */
    private final int maxSize;

    public LruCacheMap(int maxSize, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
        this.maxSize = maxSize;
    }

    public LruCacheMap(int maxSize, int initialCapacity) {
        this(maxSize, initialCapacity, 0.75f);
    }

    public LruCacheMap(int maxSize) {
        this(maxSize, 16);
    }

    /**
     * エントリの削除要否を判断
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
