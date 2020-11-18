package net.dries007.tfc.world.layer.polyhedral.traits;

import net.minecraft.util.math.BlockPos;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/**
 * @see net.minecraft.world.gen.LazyAreaLayerContext
 */
public class LazyPolyArea implements IPolyArea
{
    private final IPolyArea layer;
    private final Long2IntLinkedOpenHashMap cache;
    private final int maxCache;

    public LazyPolyArea(IPolyArea layer, Long2IntLinkedOpenHashMap cache, int maxCache)
    {
        this.layer = layer;
        this.cache = cache;
        this.maxCache = maxCache;
    }

    @Override
    public int get(int x, int y, int z)
    {
        long key = BlockPos.asLong(x, y, z);
        synchronized (this.cache)
        {
            final int cachedValue = cache.get(key);
            if (cachedValue != Integer.MIN_VALUE)
            {
                return cachedValue;
            }
            else
            {
                final int newValue = layer.get(x, y, z);
                cache.put(key, newValue);
                if (cache.size() > maxCache)
                {
                    // Clean cache
                    for (int i = 0; i < maxCache / 16; ++i)
                    {
                        cache.removeFirstInt();
                    }
                }
                return newValue;
            }
        }
    }

    public int getMaxCache()
    {
        return maxCache;
    }
}
