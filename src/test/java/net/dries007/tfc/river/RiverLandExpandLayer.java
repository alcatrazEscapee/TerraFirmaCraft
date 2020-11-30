package net.dries007.tfc.river;

import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.ICastleTransformer;

import static net.dries007.tfc.river.RiverLayerUtil.LAND;
import static net.dries007.tfc.river.RiverLayerUtil.OCEAN;

public enum RiverLandExpandLayer implements ICastleTransformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int north, int east, int south, int west, int center)
    {
        if (center == LAND || north == LAND || west == LAND || south == LAND || east == LAND)
        {
            return LAND;
        }
        return OCEAN;
    }
}
