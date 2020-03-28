/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.river;

/**
 * todo: this should just be replaced with {@link net.minecraft.util.math.MutableBoundingBox}
 */
public class XZRange
{
    private final float xStart;
    private final float zStart;
    private final float xEnd;
    private final float zEnd;

    public XZRange(float xStart, float zStart, float xSize, float zSize)
    {
        this.xStart = xStart;
        this.zStart = zStart;
        this.xEnd = xStart + xSize;
        this.zEnd = zStart + zSize;
    }

    public boolean intersects(XZRange other)
    {
        // Easier to check contrapositive: they don't intersect if one is on the left, or above the other.
        return !(xStart > other.xEnd || xEnd < other.xStart || zStart > other.zEnd || zEnd < other.zStart);
    }

    /**
     * Is a specific box contained within a larger one
     *
     * @param outer the larger box
     */
    public boolean containedIn(XZRange outer)
    {
        return xStart >= outer.xStart && zStart >= outer.zStart && xEnd <= outer.xEnd && zEnd <= outer.zEnd;
    }

    public XZRange expand(float amount)
    {
        return new XZRange(xStart - amount, zStart - amount, (xEnd - xStart) + amount * 2, (zEnd - zStart) + amount * 2);
    }
}
