package net.dries007.tfc.river;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.util.Direction;

import static net.dries007.tfc.river.Flow.*;

public class RiverShape
{
    public static final RiverShape STRAIGHT_1 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNW, ___, ___, ___,
        ___, ___, ___, NNW, N_W, WWW, ___, ___,
        ___, ___, ___, ___, N_W, NWW, N_W, ___,
        ___, ___, ___, ___, ___, NNW, NNW, ___,
        ___, ___, ___, ___, N_E, NNE, NNN, ___,
        ___, ___, ___, N_E, NNE, N_E, ___, ___,
        ___, ___, ___, NNN, N_E, ___, ___, ___
    }, Direction.SOUTH);

    public static final RiverShape STRAIGHT_2 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNW, ___, ___, ___,
        ___, ___, ___, NNW, N_W, NWW, ___, ___,
        ___, ___, ___, ___, NNW, N_W, ___, ___,
        ___, ___, ___, ___, NNN, NNW, ___, ___,
        ___, ___, ___, ___, NNN, NNN, ___, ___,
        ___, ___, ___, ___, NNE, NNN, ___, ___,
        ___, ___, ___, NNE, N_E, NNE, ___, ___
    }, Direction.SOUTH);

    public static final RiverShape STRAIGHT_3 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, NEE, NNE, NNN, ___, ___, ___,
        ___, ___, NNE, NNN, NNN, ___, ___, ___,
        ___, ___, NNN, NNW, NNN, ___, ___, ___,
        ___, ___, N_W, N_W, NNN, ___, ___, ___,
        ___, ___, ___, NNW, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNN, ___, ___, ___
    }, Direction.SOUTH);

    public static final RiverShape STRAIGHT_4 = new RiverShape(new Flow[] {
        ___, ___, NEE, NNE, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNN, ___, ___, ___,
        ___, ___, NNN, NNN, NNW, ___, ___, ___,
        ___, ___, NNW, NNW, N_W, NWW, ___, ___,
        ___, ___, ___, NNW, NNN, N_W, ___, ___,
        ___, ___, ___, NNW, NNN, NNN, ___, ___,
        ___, ___, ___, NNN, NNE, NNE, ___, ___,
        ___, ___, ___, NNN, N_E, ___, ___, ___
    }, Direction.SOUTH);

    public static final RiverShape CURVE_1 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNW, N_W, ___, ___,
        ___, ___, ___, NNW, NNW, N_W, N_W, ___,
        ___, ___, ___, ___, N_W, NWW, N_W, NWW,
        ___, ___, ___, ___, ___, ___, N_W, WWW,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.EAST);

    public static final RiverShape CURVE_2 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNW, ___, ___, ___,
        ___, ___, ___, NNN, NNW, NWW, ___, ___,
        ___, ___, ___, NNW, N_W, WWW, WWW, WWW,
        ___, ___, ___, NNN, N_W, SWW, WWW, WWW,
        ___, ___, ___, ___, NWW, SWW, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.EAST);

    public static final RiverShape CURVE_3 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNN, ___, ___, ___,
        ___, ___, NNE, NNN, ___, ___, ___, ___,
        ___, ___, NNN, NNW, N_W, ___, SWW, WWW,
        ___, ___, N_W, NNW, NWW, WWW, S_W, SWW,
        ___, ___, ___, N_W, WWW, WWW, WWW, ___,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.EAST);

    public static final RiverShape CURVE_4 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNE, ___, ___, ___,
        ___, N_E, NNE, N_E, ___, ___, ___, ___,
        ___, NNN, NNN, NNW, ___, ___, SWW, WWW,
        ___, NNW, NNW, N_W, WWW, NNW, WWW, SWW,
        ___, ___, N_W, N_W, SWW, WWW, WWW, ___,
        ___, ___, ___, NNW, WWW, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.EAST);

    public static final RiverShape T_1 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNN, ___, ___, ___,
        ___, N_E, N_E, N_E, ___, ___, ___, ___,
        EEE, N_E, N_E, NNN, ___, ___, SWW, WWW,
        EEE, NNE, NNN, NNW, WWW, NNW, WWW, SWW,
        ___, ___, N_W, N_W, SWW, WWW, WWW, ___,
        ___, ___, ___, NNW, WWW, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.WEST, Direction.EAST);

    public static final RiverShape T_2 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNW, N_W, ___, ___,
        ___, ___, N_E, NNN, NNW, N_W, N_W, ___,
        EEE, NNE, NNE, NNE, NNW, NWW, N_W, NWW,
        EEE, EEE, SEE, N_E, NNN, ___, N_W, WWW,
        ___, ___, EEE, N_E, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.WEST, Direction.EAST);

    public static final RiverShape T_3 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNN, NNN, N_W, ___, ___,
        ___, N_E, N_E, NNE, NNW, N_W, N_W, ___,
        NEE, N_E, NNE, NNE, NNW, NWW, N_W, NWW,
        EEE, N_E, ___, ___, ___, ___, N_W, WWW,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___,
        ___, ___, ___, ___, ___, ___, ___, ___
    }, Direction.WEST, Direction.EAST);

    public static final RiverShape J_1 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNN, ___, ___, ___,
        ___, N_E, N_E, N_E, ___, ___, ___, ___,
        EEE, N_E, N_E, NNN, ___, ___, ___, ___,
        EEE, NNE, NNN, NNW, ___, ___, ___, ___,
        ___, ___, N_W, N_W, N_W, ___, ___, ___,
        ___, ___, NNW, NNW, N_W, ___, ___, ___,
        ___, ___, ___, NNN, NNN, ___, ___, ___
    }, Direction.WEST, Direction.SOUTH);

    public static final RiverShape J_2 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, NNE, NNN, NNW, ___, ___, ___,
        ___, N_E, N_E, NNN, NNW, WWW, ___, ___,
        EEE, N_E, N_E, ___, N_W, NWW, N_W, ___,
        EEE, NEE, ___, ___, ___, NNW, NNW, ___,
        ___, ___, ___, ___, N_E, NNE, NNN, ___,
        ___, ___, ___, N_E, NNE, N_E, ___, ___,
        ___, ___, ___, NNN, N_E, ___, ___, ___
    }, Direction.WEST, Direction.SOUTH);

    public static final RiverShape CROSS_1 = new RiverShape(new Flow[] {
        ___, ___, ___, NNN, NNN, ___, ___, ___,
        ___, ___, N_E, NNE, NNN, ___, ___, ___,
        ___, N_E, N_E, N_E, ___, ___, ___, ___,
        EEE, N_E, N_E, NNN, ___, ___, SWW, WWW,
        EEE, NNE, NNN, NNW, WWW, NNW, WWW, SWW,
        ___, ___, N_W, N_W, SWW, SWW, WWW, ___,
        ___, ___, NNW, NNW, WWW, WWW, ___, ___,
        ___, ___, ___, NNW, NNN, ___, ___, ___
    }, Direction.WEST, Direction.SOUTH, Direction.EAST);

    private static final int SIZE = 8;
    private static final RiverShape[][] SHAPES;

    static
    {
        List<List<RiverShape>> shapes = IntStream.range(0, 64).mapToObj(i -> new ArrayList<RiverShape>()).collect(Collectors.toList());
        makeVariants(shapes,
            STRAIGHT_1, STRAIGHT_2, STRAIGHT_3, STRAIGHT_4,
            CURVE_1, CURVE_2, CURVE_3, CURVE_4,
            T_1, T_2, T_3,
            J_1, J_2,
            CROSS_1
        );
        SHAPES = shapes.stream().map(list -> list.toArray(new RiverShape[0])).toArray(RiverShape[][]::new);
    }

    public static RiverShape[] getShape(RiverPixel pixel)
    {
        return Objects.requireNonNull(SHAPES[pixel.getIndex()], "Asking for a pixel: " + pixel);
    }

    private static void makeVariants(List<List<RiverShape>> lists, RiverShape... shapes)
    {
        for (RiverShape shape : shapes)
        {
            for (int i = 0; i < 4; i++)
            {
                lists.get(shape.pixelIndex).add(shape);
                RiverShape mirrored = mirror(shape);
                lists.get(mirrored.pixelIndex).add(mirrored);
                shape = rotateCW(shape);
            }
        }
    }

    /**
     * Mirrors the template across the line x = SIZE / 2. Only this mirror is needed as mirrors about x can be created by rotations and this mirror
     * (x, z) -> (t - x, z)
     */
    private static RiverShape mirror(RiverShape shape)
    {
        Direction newDrainDirection = shape.drainDirection.getAxis() == Direction.Axis.X ? shape.drainDirection.getOpposite() : shape.drainDirection;
        Direction[] newSourceDirections = Arrays.stream(shape.sourceDirections).map(d -> d.getAxis() == Direction.Axis.X ? d.getOpposite() : d).toArray(Direction[]::new);
        Flow[] newPattern = new Flow[SIZE * SIZE];
        for (int x = 0; x < SIZE; x++)
        {
            for (int z = 0; z < SIZE; z++)
            {
                newPattern[x + SIZE * z] = Flow.mirrorX(shape.pattern[(SIZE - 1 - x) + SIZE * z]);
            }
        }
        return new RiverShape(newDrainDirection, newPattern, newSourceDirections);
    }

    /**
     * Shift's all points to the origin, (by -SIZE / 2), rotates by (x, z) -> (z, -x), then shifts back
     * (x, z) -> (x - t / 2, z - t / 2) -> (t / 2 - z, x - t / 2) -> (t - z, x)
     * Re-indexes the flow array and rotates all flow array entries
     */
    private static RiverShape rotateCW(RiverShape shape)
    {
        Direction newDrainDirection = shape.drainDirection.getClockWise();
        Direction[] newSourceDirections = Arrays.stream(shape.sourceDirections).map(Direction::getClockWise).toArray(Direction[]::new);
        Flow[] newPattern = new Flow[SIZE * SIZE];
        for (int x = 0; x < SIZE; x++)
        {
            for (int z = 0; z < SIZE; z++)
            {
                newPattern[x + SIZE * z] = Flow.rotateCW(shape.pattern[z + SIZE * (SIZE - 1 - x)]);
            }
        }
        return new RiverShape(newDrainDirection, newPattern, newSourceDirections);
    }

    private final Flow[] pattern;
    private final Direction drainDirection;
    private final Direction[] sourceDirections;
    private final int pixelIndex;

    private RiverShape(Flow[] pattern, Direction... sourceDirections)
    {
        this(Direction.NORTH, pattern, sourceDirections);
    }

    private RiverShape(Direction drainDirection, Flow[] pattern, Direction... sourceDirections)
    {
        this.pattern = pattern;
        this.drainDirection = drainDirection;
        this.sourceDirections = sourceDirections;
        this.pixelIndex = RiverPixel.byValue(drainDirection, sourceDirections);
    }

    public Flow getFlow(int x, int z)
    {
        if (x >= 0 && z >= 0 && x < 8 && z < 8)
        {
            return pattern[x | (z << 3)];
        }
        throw new IllegalArgumentException("Trying to access flow at x=" + x + ", z=" + z + ", index=" + (x | (z << 3)));
    }
}
