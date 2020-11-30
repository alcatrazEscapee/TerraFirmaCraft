package net.dries007.tfc.river;

import net.minecraft.util.Direction;
import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.ICastleTransformer;

public enum RiverTrimSourcesLayer implements ICastleTransformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int north, int east, int south, int west, int center)
    {
        // Modify existing rivers, closing off impossible connections
        RiverPixel centerPixel = RiverPixel.byIndex(center);
        if (centerPixel != null)
        {
            boolean sourceNorth = centerPixel.isSourceNorth(), sourceEast = centerPixel.isSourceEast(), sourceSouth = centerPixel.isSourceSouth(), sourceWest = centerPixel.isSourceWest();

            if (sourceNorth)
            {
                RiverPixel northPixel = RiverPixel.byIndex(north);
                if (northPixel != null && northPixel.getDrain() != Direction.SOUTH)
                {
                    sourceNorth = false;
                }
            }

            if (sourceEast)
            {
                RiverPixel eastPixel = RiverPixel.byIndex(east);
                if (eastPixel != null && eastPixel.getDrain() != Direction.WEST)
                {
                    sourceEast = false;
                }
            }

            if (sourceSouth)
            {
                RiverPixel southPixel = RiverPixel.byIndex(south);
                if (southPixel != null && southPixel.getDrain() != Direction.NORTH)
                {
                    sourceSouth = false;
                }
            }

            if (sourceWest)
            {
                RiverPixel westPixel = RiverPixel.byIndex(west);
                if (westPixel != null && westPixel.getDrain() != Direction.EAST)
                {
                    sourceWest = false;
                }
            }
            return RiverPixel.byValue(centerPixel.getDrain(), sourceNorth, sourceEast, sourceSouth, sourceWest);
        }
        return center;
    }
}
