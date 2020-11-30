package net.dries007.tfc.river;

import net.minecraft.util.Direction;
import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.ICastleTransformer;

public enum RiverExpansionLayer implements ICastleTransformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int north, int east, int south, int west, int center)
    {
        int centerType = RiverLayerUtil.getType(center);
        int centerHeight = RiverLayerUtil.getHeight(center);
        int centerAfterHeight = RiverHeightLayer.INSTANCE.apply(context, north, east, south, west, center);
        if (centerType == RiverLayerUtil.LAND)
        {
            Direction drainDirection = null, sourceDirection = null;
            int drainsFound = 0, sourcesFound = 0;
            boolean sourceNorth = false, sourceEast = false, sourceSouth = false, sourceWest = false;
            int drainHeight = 0;

            // Try and expand any adjacent rivers to this currently empty position
            RiverPixel northPixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(north));
            if (northPixel != null && northPixel.isSourceSouth())
            {
                drainsFound++;
                drainHeight = RiverLayerUtil.getHeight(north);
                drainDirection = Direction.NORTH;
            }
            else if (RiverLayerUtil.getType(north) == RiverLayerUtil.LAND)
            {
                sourceNorth = true;
                sourcesFound++;
                sourceDirection = Direction.NORTH;
            }

            RiverPixel eastPixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(east));
            if (eastPixel != null && eastPixel.isSourceWest())
            {
                drainsFound++;
                if (drainDirection == null || context.nextRandom(drainsFound) == 0)
                {
                    drainHeight = RiverLayerUtil.getHeight(east);
                    drainDirection = Direction.EAST;
                }
            }
            else if (RiverLayerUtil.getType(east) == RiverLayerUtil.LAND)
            {
                sourceEast = true;
                sourcesFound++;
                if (sourceDirection == null || context.nextRandom(sourcesFound) == 0)
                {
                    sourceDirection = Direction.EAST;
                }
            }

            RiverPixel southPixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(south));
            if (southPixel != null && southPixel.isSourceNorth())
            {
                drainsFound++;
                if (drainDirection == null || context.nextRandom(drainsFound) == 0)
                {
                    drainHeight = RiverLayerUtil.getHeight(south);
                    drainDirection = Direction.SOUTH;
                }
            }
            else if (RiverLayerUtil.getType(south) == RiverLayerUtil.LAND)
            {
                sourceSouth = true;
                sourcesFound++;
                if (sourceDirection == null || context.nextRandom(sourcesFound) == 0)
                {
                    sourceDirection = Direction.SOUTH;
                }
            }

            RiverPixel westPixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(west));
            if (westPixel != null && westPixel.isSourceEast())
            {
                drainsFound++;
                if (drainDirection == null || context.nextRandom(drainsFound) == 0)
                {
                    drainHeight = RiverLayerUtil.getHeight(west);
                    drainDirection = Direction.WEST;
                }
            }
            else if (RiverLayerUtil.getType(west) == RiverLayerUtil.LAND)
            {
                sourceWest = true;
                sourcesFound++;
                if (sourceDirection == null || context.nextRandom(sourcesFound) == 0)
                {
                    sourceDirection = Direction.WEST;
                }
            }

            if (drainDirection != null)
            {
                // Valid drain direction. If there's valid sources, create river, otherwise mark this as failed
                if (sourcesFound > 0)
                {
                    // Calculate the actual source booleans based on the branch chance and primary source
                    int branchRandom = context.nextRandom(6);
                    sourceNorth = ((sourceDirection == Direction.NORTH) || (sourceNorth && branchRandom == 0)) && RiverLayerUtil.getHeight(north) > drainHeight - 2;
                    sourceEast = ((sourceDirection == Direction.EAST) || (sourceEast && branchRandom == 1)) && RiverLayerUtil.getHeight(east) > drainHeight - 2;
                    sourceSouth = ((sourceDirection == Direction.SOUTH) || (sourceSouth && branchRandom == 2)) && RiverLayerUtil.getHeight(south) > drainHeight - 2;
                    sourceWest = ((sourceDirection == Direction.WEST) || (sourceWest && branchRandom == 3)) && RiverLayerUtil.getHeight(west) > drainHeight - 2;

                    if (sourceNorth || sourceEast || sourceSouth || sourceWest)
                    {
                        int riverMeta = RiverPixel.byValue(drainDirection, sourceNorth, sourceEast, sourceSouth, sourceWest);
                        return RiverLayerUtil.getId(centerHeight + 1, RiverLayerUtil.RIVER, riverMeta);
                    }
                }
            }
        }
        return centerAfterHeight;
    }
}
