package net.dries007.tfc.world.layer.polyhedral.traits;

import net.minecraft.util.FastRandom;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/**
 * @see net.minecraft.world.gen.LazyAreaLayerContext
 */
public class PolyAreaLayerContext
{
    private final Long2IntLinkedOpenHashMap cache;
    private final int maxCache;

    private final long seed;
    private long lastValue;

    public PolyAreaLayerContext(long seed, int maxCacheSizeIn)
    {
        this.seed = seed;
        this.cache = new Long2IntLinkedOpenHashMap(16, 0.25F);
        this.cache.defaultReturnValue(Integer.MIN_VALUE);
        this.maxCache = maxCacheSizeIn;
    }

    public LazyPolyArea createResult(IPolyArea area)
    {
        return new LazyPolyArea(area, cache, maxCache);
    }

    public LazyPolyArea createResult(IPolyArea area, int prevCache)
    {
        return new LazyPolyArea(area, cache, Math.min(1024, 4 * prevCache));
    }

    public void initRandom(long x, long y, long z)
    {
        long value = this.seed;
        value = FastRandom.next(value, x);
        value = FastRandom.next(value, y);
        value = FastRandom.next(value, z);
        value = FastRandom.next(value, x);
        value = FastRandom.next(value, y);
        value = FastRandom.next(value, z);
        this.lastValue = value;
    }

    public int nextRandom(int bound)
    {
        int value = (int) Math.floorMod(lastValue >> 24, bound);
        this.lastValue = FastRandom.next(lastValue, this.seed);
        return value;
    }
}
