package net.samongi.Inscription.Player.CacheTypes;

import net.samongi.Inscription.Player.CacheData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class CompositeCacheData<Key, CData extends CacheData> implements CacheData {

    //----------------------------------------------------------------------------------------------------------------//
    private Map<Key, CData> m_cacheDatas = new HashMap<>();

    //----------------------------------------------------------------------------------------------------------------//
    @Override public void clear() {
        m_cacheDatas.clear();
    }

    //----------------------------------------------------------------------------------------------------------------//
    public CData getCacheData(Key key) {
        return m_cacheDatas.get(key);
    }

    public CData getCacheData(Key key, Supplier<CData> defaultSupplier) {
        if (m_cacheDatas.containsKey(key)) {
            return getCacheData(key);
        }

        CData newData = defaultSupplier.get();
        setCacheData(key, newData);
        return newData;
    }

    public void setCacheData(Key key, CData value) {
        m_cacheDatas.put(key, value);
    }

    //----------------------------------------------------------------------------------------------------------------//
}
