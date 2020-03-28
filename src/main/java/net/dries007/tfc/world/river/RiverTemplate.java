/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.river;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.Direction;

import static net.dries007.tfc.world.river.Flow.*;

public class RiverTemplate
{
    public static final RiverTemplate STRAIGHT_1 = new RiverTemplate(Direction.NORTH, 6, 8, Direction.NORTH, 3, 0,
        ___, ___, NNN, NNN, ___, ___, ___, ___,
        ___, ___, NNW, NNW, ___, ___, ___, ___,
        ___, ___, N_W, NNW, N_W, ___, ___, ___,
        ___, ___, ___, N_W, NNW, N_W, ___, ___,
        ___, ___, ___, ___, NNW, N_W, ___, ___,
        ___, ___, ___, ___, N_W, N_W, N_W, ___,
        ___, ___, ___, ___, ___, NNW, NNW, ___,
        ___, ___, ___, ___, ___, NNN, NNN, ___
    );

    public static final RiverTemplate STRAIGHT_2 = new RiverTemplate(Direction.NORTH, 5, 8, Direction.NORTH, 3, 0,
        ___, ___, NNN, NNN, ___, ___, ___, ___,
        ___, ___, N_E, NNE, ___, ___, ___, ___,
        ___, N_E, NNN, ___, ___, ___, ___, ___,
        ___, NNN, N_W, WWW, NWW, ___, ___, ___,
        ___, ___, N_W, N_W, WWW, N_W, ___, ___,
        ___, ___, ___, ___, N_W, NWW, N_W, ___,
        ___, ___, ___, ___, ___, NNN, NNN, ___,
        ___, ___, ___, ___, N_E, N_E, ___, ___
    );

    public static final RiverTemplate CURVE_1 = new RiverTemplate(Direction.WEST, 8, 6, Direction.NORTH, 3, 0,
        ___, ___, NNN, NNN, ___, ___, ___, ___,
        ___, ___, N_W, NNW, ___, ___, ___, ___,
        ___, ___, ___, N_W, N_W, ___, ___, ___,
        ___, ___, ___, ___, N_W, N_W, ___, ___,
        ___, ___, ___, ___, ___, N_W, N_W, ___,
        ___, ___, ___, ___, ___, ___, N_W, NWW,
        ___, ___, ___, ___, ___, ___, ___, N_W,
        ___, ___, ___, ___, ___, ___, ___, ___
    );

    public static final RiverTemplate SOURCE_1 = new RiverTemplate(Direction.NORTH, 0, 0, Direction.NORTH, 3, 0,
        ___, ___, NNN, NNN, ___, ___, ___, ___,
        ___, ___, N_W, NNW, ___, ___, ___, ___,
        ___, ___, ___, N_W, N_W, ___, ___, ___,
        ___, ___, ___, NNN, NWW, N_W, ___, ___,
        ___, ___, NEE, N_E, ___, NNW, ___, ___,
        ___, ___, NNE, ___, ___, N_W, N_W, ___,
        ___, ___, NNN, ___, ___, ___, NNW, ___,
        ___, ___, ___, ___, ___, ___, NNN, ___
    );

    public static final RiverTemplate SOURCE_2 = new RiverTemplate(Direction.NORTH, 0, 0, Direction.NORTH, 3, 0,
        ___, ___, NNN, NNN, ___, ___, ___, ___,
        ___, ___, NNE, NNW, ___, ___, ___, ___,
        ___, N_E, N_E, N_W, N_W, ___, ___, ___,
        ___, NNE, ___, ___, NNW, N_W, ___, ___,
        ___, NNN, ___, N_E, N_E, NNW, ___, ___,
        ___, ___, ___, NNE, ___, N_W, N_W, ___,
        ___, ___, N_E, N_E, ___, ___, NNW, ___,
        ___, ___, NNN, ___, ___, ___, NNN, ___
    );

    public static final RiverTemplate DRAIN_1 = new RiverTemplate(Direction.NORTH, 4, 8, Direction.NORTH, 4, 0,
        ___, ___, N_W, NNN, NNN, NNE, N_E, ___,
        ___, ___, N_W, NNN, NNN, N_E, ___, ___,
        ___, ___, ___, NNW, NNN, NNE, ___, ___,
        ___, ___, N_E, NNE, NNN, NNE, ___, ___,
        ___, ___, NNN, NNN, NNN, N_E, ___, ___,
        ___, ___, N_W, NNW, NNN, ___, ___, ___,
        ___, ___, ___, NNW, NNN, ___, ___, ___,
        ___, ___, ___, NNN, NNN, ___, ___, ___
    );

    public static final RiverTemplate DRAIN_2 = new RiverTemplate(Direction.NORTH, 4, 8, Direction.NORTH, 4, 0,
        ___, ___, NNN, NNN, NNN, NNN, N_E, ___,
        ___, ___, NNN, NNN, NNN, N_E, NNE, ___,
        ___, ___, NNW, NNN, N_W, NNE, N_E, ___,
        ___, ___, NNW, NNN, NNN, NNN, ___, ___,
        ___, ___, N_W, NNW, NNN, NNE, ___, ___,
        ___, ___, ___, NNW, N_E, N_E, ___, ___,
        ___, ___, ___, NNN, NNE, ___, ___, ___,
        ___, ___, ___, NNN, NNN, ___, ___, ___
    );

    public static final int SIZE = 8;

    // These are indexed by their downstream direction, so we can easily search for templates that can be attached to a given piece

    public static final Map<Direction, List<RiverTemplate>> DRAIN_TEMPLATES = makeVariants(DRAIN_1, DRAIN_2);
    public static final Map<Direction, List<RiverTemplate>> SOURCE_TEMPLATES = makeVariants(SOURCE_1, SOURCE_2);
    public static final Map<Direction, List<RiverTemplate>> CONNECTOR_TEMPLATES = makeVariants(STRAIGHT_1, STRAIGHT_2, CURVE_1);

    private static final List<RiverTemplate> ALL_TEMPLATES = Stream.of(DRAIN_TEMPLATES, SOURCE_TEMPLATES, CONNECTOR_TEMPLATES).flatMap(map -> map.values().stream()).flatMap(Collection::stream).collect(Collectors.toList());

    public static RiverTemplate get(int id)
    {
        return ALL_TEMPLATES.get(id);
    }

    public static Map<Direction, List<RiverTemplate>> makeVariants(RiverTemplate... templates)
    {
        Map<Direction, List<RiverTemplate>> map = new HashMap<>();
        for (RiverTemplate template : templates)
        {
            for (int i = 0; i < 4; i++)
            {
                map.computeIfAbsent(template.downstreamDirection, key -> new ArrayList<>()).add(template);
                RiverTemplate mirrored = mirror(template);
                map.computeIfAbsent(mirrored.downstreamDirection, key -> new ArrayList<>()).add(mirrored);
                template = rotateCW(template);
            }
        }
        return map;
    }

    public static RiverTemplate mirror(RiverTemplate template)
    {
        // Mirrors the template across the line x = TEMPLATE_SIZE / 2. Only this mirror is needed as mirrors about x can be created by rotations and this mirror
        // (x, z) -> (t - x, z)
        float newUpstreamX = SIZE - template.upstreamX;
        float newDownstreamX = SIZE - template.downstreamX;
        Direction newUpstreamDirection = template.upstreamDirection.getAxis() == Direction.Axis.X ? template.upstreamDirection.getOpposite() : template.upstreamDirection;
        Direction newDownstreamDirection = template.downstreamDirection.getAxis() == Direction.Axis.X ? template.downstreamDirection.getOpposite() : template.downstreamDirection;
        Flow[] newFlows = new Flow[SIZE * SIZE];
        for (int x = 0; x < SIZE; x++)
        {
            for (int z = 0; z < SIZE; z++)
            {
                newFlows[x + SIZE * z] = Flow.mirrorX(template.flows[(SIZE - 1 - x) + SIZE * z]);
            }
        }
        return new RiverTemplate(newUpstreamDirection, newUpstreamX, template.upstreamZ, newDownstreamDirection, newDownstreamX, template.downstreamZ, newFlows);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static RiverTemplate rotateCW(RiverTemplate template)
    {
        // Shift's all points to the origin, (by -TEMPLATE_SIZE / 2), rotates by (x, z) -> (z, -x), then shifts back
        // (x, z) -> (x - t / 2, z - t / 2) -> (t / 2 - z, x - t / 2) -> (t - z, x)
        // Re-indexes the flow array and rotates all flow array entries

        float newUpstreamX = SIZE - template.upstreamZ;
        float newUpstreamZ = template.upstreamX;
        float newDownstreamX = SIZE - template.downstreamZ;
        float newDownstreamZ = template.downstreamX;
        Direction newUpstreamDirection = template.upstreamDirection.getClockWise();
        Direction newDownstreamDirection = template.downstreamDirection.getClockWise();
        Flow[] newFlows = new Flow[SIZE * SIZE];
        for (int x = 0; x < SIZE; x++)
        {
            for (int z = 0; z < SIZE; z++)
            {
                newFlows[x + SIZE * z] = Flow.rotateCW(template.flows[z + SIZE * (SIZE - 1 - x)]);
            }
        }
        return new RiverTemplate(newUpstreamDirection, newUpstreamX, newUpstreamZ, newDownstreamDirection, newDownstreamX, newDownstreamZ, newFlows);
    }

    private final Direction upstreamDirection;
    private final float upstreamX;
    private final float upstreamZ;
    private final Direction downstreamDirection;
    private final float downstreamX;
    private final float downstreamZ;
    private final Flow[] flows;

    public RiverTemplate(Direction upstreamDirection, float upstreamX, float upstreamZ, Direction downstreamDirection, float downstreamX, float downstreamZ, Flow... flows)
    {
        this.upstreamDirection = upstreamDirection;
        this.upstreamX = upstreamX;
        this.upstreamZ = upstreamZ;
        this.downstreamDirection = downstreamDirection;
        this.downstreamX = downstreamX;
        this.downstreamZ = downstreamZ;
        if (flows.length != SIZE * SIZE)
        {
            throw new IllegalStateException("Flow array must be [TEMPLATE_SIZE] x [TEMPLATE_SIZE]");
        }
        this.flows = flows;
    }

    public Flow getFlow(int x, int z)
    {
        if (x < 0 || z < 0 || x >= SIZE || z >= SIZE)
        {
            throw new IllegalStateException("Tried to get flow with illegal index: x = " + x + ", z = " + z + ", index = " + (x + SIZE * z));
        }
        return flows[x + SIZE * z];
    }

    /**
     * Only for internal access for ease of copy!
     */
    public Flow[] getFlows()
    {
        return flows;
    }

    public Direction getUpstreamDirection()
    {
        return upstreamDirection;
    }

    public float getUpstreamX()
    {
        return upstreamX;
    }

    public float getUpstreamZ()
    {
        return upstreamZ;
    }

    public Direction getDownstreamDirection()
    {
        return downstreamDirection;
    }

    public float getDownstreamX()
    {
        return downstreamX;
    }

    public float getDownstreamZ()
    {
        return downstreamZ;
    }
}
