package net.dries007.tfc.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.util.Direction;
import net.minecraft.util.Util;

public class RiverPixel
{
    private static final RiverPixel[] VALUES = Util.make(new RiverPixel[64], values -> {
        for (int i = 0; i < 8; i++)
        {
            boolean first = (i & 1) == 0, second = (i & 2) == 0, third = (i & 4) == 0;
            set(values, new RiverPixel(Direction.NORTH, false, first, second, third));
            set(values, new RiverPixel(Direction.EAST, first, false, second, third));
            set(values, new RiverPixel(Direction.SOUTH, first, second, false, third));
            set(values, new RiverPixel(Direction.WEST, first, second, third, false));
        }
    });

    public static RiverPixel[] values()
    {
        return VALUES;
    }

    @Nullable
    public static RiverPixel byIndex(int id)
    {
        return id >= 0 && id <= VALUES.length ? VALUES[id] : null;
    }

    public static int byValue(Direction drainDirection, Direction[] sourceDirections)
    {
        int value = drainDirection.get2DDataValue();
        for (Direction sourceDirection : sourceDirections)
        {
            value |= RiverLayerUtil.byDirection(sourceDirection, 4, 8, 16, 32);
        }
        return value;
    }

    public static int byValue(Direction drainDirection, boolean sourceNorth, boolean sourceEast, boolean sourceSouth, boolean sourceWest)
    {
        return drainDirection.get2DDataValue() | (sourceNorth ? 4 : 0) | (sourceEast ? 8 : 0) | (sourceSouth ? 16 : 0) | (sourceWest ? 32 : 0);
    }

    private static void set(RiverPixel[] values, RiverPixel pixel)
    {
        values[pixel.index] = pixel;
    }

    private final Direction drainDirection;
    private final Direction[] sourceDirections;
    private final boolean sourceNorth, sourceEast, sourceSouth, sourceWest;
    private final int index;

    private RiverPixel(Direction drainDirection, boolean sourceNorth, boolean sourceEast, boolean sourceSouth, boolean sourceWest)
    {
        this.drainDirection = drainDirection;
        this.sourceNorth = sourceNorth;
        this.sourceEast = sourceEast;
        this.sourceSouth = sourceSouth;
        this.sourceWest = sourceWest;
        this.index = byValue(drainDirection, sourceNorth, sourceEast, sourceSouth, sourceWest);

        List<Direction> sources = new ArrayList<>();
        if (sourceNorth)
        {
            sources.add(Direction.NORTH);
        }
        if (sourceEast)
        {
            sources.add(Direction.EAST);
        }
        if (sourceSouth)
        {
            sources.add(Direction.SOUTH);
        }
        if (sourceWest)
        {
            sources.add(Direction.WEST);
        }

        this.sourceDirections = sources.toArray(new Direction[0]);
    }

    public Direction getDrain()
    {
        return drainDirection;
    }

    public Direction[] getSources()
    {
        return sourceDirections;
    }

    public int getIndex()
    {
        return index;
    }

    public boolean isSourceNorth()
    {
        return sourceNorth;
    }

    public boolean isSourceEast()
    {
        return sourceEast;
    }

    public boolean isSourceSouth()
    {
        return sourceSouth;
    }

    public boolean isSourceWest()
    {
        return sourceWest;
    }

    @Override
    public String toString()
    {
        return "RiverPixel{drain=" + drainDirection + ", sources=" + Arrays.toString(sourceDirections) + ", index=" + index + '}';
    }
}
