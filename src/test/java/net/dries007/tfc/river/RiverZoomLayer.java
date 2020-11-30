package net.dries007.tfc.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Direction;
import net.minecraft.world.gen.IExtendedNoiseRandom;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.layer.ZoomLayer;
import net.minecraft.world.gen.layer.traits.IAreaTransformer1;

public enum RiverZoomLayer implements IAreaTransformer1
{
    INSTANCE;

    public final int[][] pixelMap;
    private final int clockwise, anticlockwise;

    RiverZoomLayer()
    {
        clockwise = 0;
        anticlockwise = 1;
        pixelMap = calculatePixelMap();
    }

    @VisibleForTesting
    public int pixelMapIndex(RiverPixel pixel, int mask)
    {
        return pixel.getIndex() | (mask << 6);
    }

    @Override
    public int getParentX(int x)
    {
        return x >> 1;
    }

    @Override
    public int getParentY(int y)
    {
        return y >> 1;
    }

    @Override
    public int applyPixel(IExtendedNoiseRandom<?> context, IArea area, int x, int z)
    {
        final int parentX = getParentX(x);
        final int parentZ = getParentY(z);
        final int parent = area.get(parentX, parentZ);
        if (RiverLayerUtil.getType(parent) == RiverLayerUtil.RIVER)
        {
            RiverPixel pixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(parent));
            if (pixel != null)
            {
                // Compute the mask using seeds for the edges at this parent position
                context.initRandom(parentX, parentZ);
                int maskNorth = context.nextRandom(2);
                context.initRandom(parentX + 1957382L, parentZ + 96838271L);
                int maskWest = context.nextRandom(2);
                context.initRandom(parentX, parentZ + 1);
                int maskSouth = context.nextRandom(2);
                context.initRandom(parentX + 1957382L + 1, parentZ + 96838271L);
                int maskEast = context.nextRandom(2);

                // Get the map values from the pixel map
                int mask = maskNorth | (maskEast << 1) | (maskSouth << 2) | (maskWest << 3);
                int mapIndex = pixelMapIndex(pixel, mask);
                int[] mapValues = pixelMap[mapIndex];

                // Calculate a random pseudo-index into the map values, using the parent seeding
                context.initRandom(parentX + 69283953L, parentZ + 20834738L);
                int mapValuesPseudoIndex = context.nextRandom(mapValues.length >> 2);

                // Index the map values based on the local position of this zoom
                int localX = x & 1;
                int localZ = z & 1;
                int mapValuesLocalIndex = localX | (localZ << 1);

                // Return the river pixel for the given map value
                int localValue = mapValues[(mapValuesPseudoIndex << 2) | mapValuesLocalIndex];
                if (localValue != RiverLayerUtil.EMPTY)
                {
                    return RiverLayerUtil.getId(0, RiverLayerUtil.RIVER, localValue);
                }
            }
        }
        return ZoomLayer.NORMAL.applyPixel(context, (xIn, zIn) -> {
            final int parentIn = area.get(xIn, zIn);
            final int parentType = RiverLayerUtil.getType(parentIn);
            if (parentType == RiverLayerUtil.RIVER)
            {
                return RiverLayerUtil.LAND;
            }
            return parentType;
        }, x, z);
    }

    private int[][] calculatePixelMap()
    {
        int[][] pixelMap = new int[1024][]; // 64 RiverPixel indexes (at 50% density), plus 16 mask bits (4 booleans)
        for (RiverPixel pixel : RiverPixel.values()) // For each (parent) river pixel
        {
            if (pixel != null)
            {
                for (int i = 0; i < 16; i++) // For each edge mask (0b0000 to 0b1111)
                {
                    pixelMap[pixelMapIndex(pixel, i)] = calculateZoomedPixelValues(pixel, i);
                }
            }
        }
        return pixelMap;
    }

    private int[] calculateZoomedPixelValues(RiverPixel parent, int mask)
    {
        // Cardinal Directions:
        //   N
        // W + E -> +x
        //   S
        //   -> +z
        // Positions:
        // 0 | 1
        // 2 | 3
        // Edges:
        // x 0 x
        // 1 x 2
        // x 3 x
        // Notations for Each Edge:
        // 0 = Clockwise
        // 1 = Anticlockwise
        // 2 = No connection

        List<Integer> results = new ArrayList<>();
        boolean maskNorth = (mask & 1) == 0, maskEast = (mask & 2) == 0, maskSouth = (mask & 4) == 0, maskWest = (mask & 8) == 0;

        // Drain positions
        Direction drainDirection = parent.getDrain();
        int drainPos = calculatePositionFromSide(drainDirection, maskNorth, maskEast, maskSouth, maskWest);

        // Source positions
        Direction[] sourceDirections = parent.getSources();
        int[] sourcePos = new int[sourceDirections.length];
        for (int i = 0; i < sourcePos.length; i++)
        {
            sourcePos[i] = calculatePositionFromSide(sourceDirections[i], maskNorth, maskEast, maskSouth, maskWest);
        }

        boolean hasSources = sourcePos.length > 0;

        // Iterate through all possible interior positions
        for (int i = 0; i < 81; i++)
        {
            int edge0 = i % 3;
            int edge1 = (i / 3) % 3;
            int edge2 = (i / 9) % 3;
            int edge3 = (i / 27) % 3;

            if (
                isPixelValid(0, drainPos, sourcePos, edge0, edge1, hasSources) &&
                    isPixelValid(1, drainPos, sourcePos, edge2, edge0, hasSources) &&
                    isPixelValid(2, drainPos, sourcePos, edge1, edge3, hasSources) &&
                    isPixelValid(3, drainPos, sourcePos, edge3, edge2, hasSources)
            )
            {
                {
                    // Pos 0
                    Direction drain = firstTrueThen(drainPos == 0, drainDirection, edge0 == clockwise, Direction.EAST, edge1 == anticlockwise, Direction.SOUTH);
                    int pixel = RiverLayerUtil.EMPTY;
                    if (drain != null)
                    {
                        pixel = RiverPixel.byValue(drain, containsParallel(0, Direction.NORTH, sourcePos, sourceDirections), edge0 == anticlockwise, edge1 == clockwise, containsParallel(0, Direction.WEST, sourcePos, sourceDirections));
                        if (RiverPixel.byIndex(pixel) == null)
                        {
                            throw new IllegalStateException("Parent=" + parent + ", mask=" + mask + ", edgeMask=" + i + ", edges=[" + edge0 + ", " + edge1 + ", " + edge2 + ", " + edge3 + "], drainDirection=" + drainDirection + ", drainPos=" + drainPos + ", sourceDirections=" + Arrays.toString(sourcePos) + ", sourcePos=" + Arrays.toString(sourceDirections) + ", pixel=" + pixel + ", drain=" + drain);
                        }
                    }
                    results.add(pixel);
                }

                {
                    // Pos 1
                    Direction drain = firstTrueThen(drainPos == 1, drainDirection, edge0 == anticlockwise, Direction.WEST, edge2 == clockwise, Direction.SOUTH);
                    int pixel = RiverLayerUtil.EMPTY;
                    if (drain != null)
                    {
                        pixel = RiverPixel.byValue(drain, containsParallel(1, Direction.NORTH, sourcePos, sourceDirections), containsParallel(1, Direction.EAST, sourcePos, sourceDirections), edge2 == anticlockwise, edge0 == clockwise);
                        if (RiverPixel.byIndex(pixel) == null)
                        {
                            throw new IllegalStateException("Parent=" + parent + ", mask=" + mask + ", edgeMask=" + i + ", edges=[" + edge0 + ", " + edge1 + ", " + edge2 + ", " + edge3 + "], drainDirection=" + drainDirection + ", drainPos=" + drainPos + ", sourceDirections=" + Arrays.toString(sourcePos) + ", sourcePos=" + Arrays.toString(sourceDirections) + ", pixel=" + pixel + ", drain=" + drain);
                        }
                    }
                    results.add(pixel);
                }

                {
                    // Pos 2
                    Direction drain = firstTrueThen(drainPos == 2, drainDirection, edge1 == clockwise, Direction.NORTH, edge3 == anticlockwise, Direction.EAST);
                    int pixel = RiverLayerUtil.EMPTY;
                    if (drain != null)
                    {
                        pixel = RiverPixel.byValue(drain, edge1 == anticlockwise, edge3 == clockwise, containsParallel(2, Direction.SOUTH, sourcePos, sourceDirections), containsParallel(2, Direction.WEST, sourcePos, sourceDirections));
                        if (RiverPixel.byIndex(pixel) == null)
                        {
                            throw new IllegalStateException("Parent=" + parent + ", mask=" + mask + ", edgeMask=" + i + ", edges=[" + edge0 + ", " + edge1 + ", " + edge2 + ", " + edge3 + "], drainDirection=" + drainDirection + ", drainPos=" + drainPos + ", sourceDirections=" + Arrays.toString(sourcePos) + ", sourcePos=" + Arrays.toString(sourceDirections) + ", pixel=" + pixel + ", drain=" + drain);
                        }
                    }
                    results.add(pixel);
                }

                {
                    // Pos 3
                    Direction drain = firstTrueThen(drainPos == 3, drainDirection, edge2 == anticlockwise, Direction.NORTH, edge3 == clockwise, Direction.WEST);
                    int pixel = RiverLayerUtil.EMPTY;
                    if (drain != null)
                    {
                        pixel = RiverPixel.byValue(drain, edge2 == clockwise, containsParallel(3, Direction.EAST, sourcePos, sourceDirections), containsParallel(3, Direction.SOUTH, sourcePos, sourceDirections), edge3 == anticlockwise);
                        if (RiverPixel.byIndex(pixel) == null)
                        {
                            throw new IllegalStateException("Parent=" + parent + ", mask=" + mask + ", edgeMask=" + i + ", edges=[" + edge0 + ", " + edge1 + ", " + edge2 + ", " + edge3 + "], drainDirection=" + drainDirection + ", drainPos=" + drainPos + ", sourceDirections=" + Arrays.toString(sourcePos) + ", sourcePos=" + Arrays.toString(sourceDirections) + ", pixel=" + pixel + ", drain=" + drain);
                        }
                    }
                    results.add(pixel);
                }
            }
        }
        if (results.isEmpty())
        {
            throw new IllegalStateException("Parent=" + parent + ", mask=" + mask + ", drainDirection=" + drainDirection + ", drainPos=" + drainPos + ", sourceDirections=" + Arrays.toString(sourcePos) + ", sourcePos=" + Arrays.toString(sourceDirections));
        }
        return results.stream().mapToInt(i -> i).toArray();
    }

    private int calculatePositionFromSide(Direction direction, boolean north, boolean east, boolean south, boolean west)
    {
        switch (direction)
        {
            case NORTH:
                return north ? 0 : 1;
            case EAST:
                return east ? 1 : 3;
            case SOUTH:
                return south ? 2 : 3;
            case WEST:
                return west ? 0 : 2;
            default:
                throw new IllegalStateException("Non-horizontal direction: " + direction);
        }
    }

    private boolean isPixelValid(int pos, int drainPos, int[] sourcePos, int clockwiseEdge, int anticlockwiseEdge, boolean parentHasSources)
    {
        // 1. Each position with an incoming flow (source), must have an outgoing flow (drain)
        if (contains(sourcePos, pos) || clockwiseEdge == anticlockwise || anticlockwiseEdge == clockwise)
        {
            if (clockwiseEdge != clockwise && anticlockwiseEdge != anticlockwise && drainPos != pos)
            {
                return false;
            }
        }

        // 2. Each position must only have at maximum, one outgoing flow (drain)
        if (moreThanOne(drainPos == pos, clockwiseEdge == clockwise, anticlockwiseEdge == anticlockwise))
        {
            return false;
        }

        // 3. If the parent tile has sources, Each position with an outgoing flow (drain), must have at least one incoming flow (source)
        if (parentHasSources && (drainPos == pos || clockwiseEdge == clockwise || anticlockwiseEdge == anticlockwise))
        {
            return contains(sourcePos, pos) || clockwiseEdge == anticlockwise || anticlockwiseEdge == clockwise;
        }
        return true;
    }

    private boolean moreThanOne(boolean first, boolean second, boolean third)
    {
        return (first ? 1 : 0) + (second ? 1 : 0) + (third ? 1 : 0) > 1;
    }

    @Nullable
    private <T> T firstTrueThen(boolean first, T firstValue, boolean second, T secondValue, boolean third, T thirdValue)
    {
        if (first)
        {
            return firstValue;
        }
        if (second)
        {
            return secondValue;
        }
        if (third)
        {
            return thirdValue;
        }
        return null;
    }

    private boolean contains(int[] array, int value)
    {
        for (int target : array)
        {
            if (target == value)
            {
                return true;
            }
        }
        return false;
    }

    private <T> boolean containsParallel(int pos, T expected, int[] positions, T[] values)
    {
        for (int i = 0; i < positions.length; i++)
        {
            if (positions[i] == pos && values[i] == expected)
            {
                return true;
            }
        }
        return false;
    }
}
