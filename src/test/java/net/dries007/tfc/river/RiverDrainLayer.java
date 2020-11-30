package net.dries007.tfc.river;

import net.minecraft.util.Direction;
import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.ICastleTransformer;

public enum RiverDrainLayer implements ICastleTransformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int north, int east, int south, int west, int center)
    {
        if (center == RiverLayerUtil.LAND)
        {
            Direction drainDirection = null, sourceDirection = null;
            int drainsFound = 0, sourcesFound = 0;

            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final int pixel = RiverLayerUtil.byDirection(direction, north, east, south, west);
                if (RiverLayerUtil.getType(pixel) == RiverLayerUtil.OCEAN)
                {
                    drainsFound++;
                    if (drainDirection == null || context.nextRandom(drainsFound) == 0)
                    {
                        drainDirection = direction;
                    }
                }
                else
                {
                    sourcesFound++;
                    if (sourceDirection == null || context.nextRandom(sourcesFound) == 0)
                    {
                        sourceDirection = direction;
                    }
                }
            }

            if (drainDirection != null && sourceDirection != null && context.nextRandom(3) == 0)
            {
                // One randomly selected drain direction, and at least one valid source direction
                int riverMetadata = RiverPixel.byValue(drainDirection, sourceDirection == Direction.NORTH, sourceDirection == Direction.EAST, sourceDirection == Direction.SOUTH, sourceDirection == Direction.WEST);
                return RiverLayerUtil.getId(0, RiverLayerUtil.RIVER, riverMetadata);
            }
        }
        return RiverHeightLayer.INSTANCE.apply(context, north, east, south, west, center);
    }
}
