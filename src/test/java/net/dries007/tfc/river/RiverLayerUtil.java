package net.dries007.tfc.river;

import net.minecraft.util.Direction;

public class RiverLayerUtil
{
    public static final int EMPTY = -1;
    public static final int OCEAN = 0;
    public static final int LAND = 1;
    public static final int RIVER = 2;

    public static final int MAX_HEIGHT = (1 << 4) - 1; // 0b1111 = 15

    public static int getType(int value)
    {
        return value & 0b11;
    }

    public static int getHeight(int value)
    {
        return (value >> 2) & 0b1111;
    }

    public static int getActualHeight(int value)
    {
        int type = getType(value);
        return type == OCEAN ? -1 : getHeight(value);
    }

    public static int withHeight(int value, int height)
    {
        return getId(height, getType(value), getMeta(value));
    }

    public static int getMeta(int value)
    {
        return (value >> 6);
    }

    public static int getId(int height, int type)
    {
        return getId(height, type, 0);
    }

    public static int getId(int height, int type, int metadata)
    {
        return type | (height << 2) | (metadata << 6);
    }

    public static int byDirection(Direction direction, int north, int east, int south, int west)
    {
        switch (direction)
        {
            case NORTH:
                return north;
            case EAST:
                return east;
            case SOUTH:
                return south;
            case WEST:
                return west;
        }
        throw new IllegalStateException("Not a valid direction: " + direction);
    }
}
