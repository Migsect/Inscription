package net.samongi.Inscription.Player;

import net.samongi.Inscription.Inscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public interface CacheData {

    /**
     * Will clear the data
     */
    public void clear();

    /**
     * Will return the cachedata type.
     *
     * @return
     */
    public String getType();

    /**
     * Will return the data as a string that can be printed out and is readable
     *
     * @return
     */
    public String getData();

    public static @Nonnull <DataType extends CacheData> DataType getData(@Nonnull Class<? extends DataType> type, @Nonnull String typeId, @Nonnull PlayerData playerData,
        @Nonnull Supplier<DataType> supplier) {
        CacheData cachedData = playerData.getData(typeId);
        if (cachedData == null) {
            cachedData = supplier.get();
        }
        if (!type.isInstance(cachedData)) {
            Inscription.logger.severe("CachedData with id '" + typeId + "' is not castable to its type");
            return null;
        }

        @SuppressWarnings("unchecked") // We did a direct class type check prior.
        DataType castedData = (DataType)cachedData;

        return castedData;
    }
}
