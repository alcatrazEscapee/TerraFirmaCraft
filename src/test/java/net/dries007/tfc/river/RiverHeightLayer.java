package net.dries007.tfc.river;

import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.ICastleTransformer;

public enum RiverHeightLayer implements ICastleTransformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int north, int east, int south, int west, int center)
    {
        if (RiverLayerUtil.getType(center) == RiverLayerUtil.LAND)
        {
            int height = RiverLayerUtil.getActualHeight(center);
            height = Math.min(height, RiverLayerUtil.getActualHeight(north));
            height = Math.min(height, RiverLayerUtil.getActualHeight(east));
            height = Math.min(height, RiverLayerUtil.getActualHeight(west));
            height = Math.min(height, RiverLayerUtil.getActualHeight(south));
            if (height < RiverLayerUtil.MAX_HEIGHT)
            {
                height++;
            }
            return RiverLayerUtil.withHeight(center, height);
        }
        return center;
    }
}
