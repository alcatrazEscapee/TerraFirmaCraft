package net.dries007.tfc.river;

import net.minecraft.world.gen.IExtendedNoiseRandom;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.layer.traits.IAreaTransformer1;

import net.dries007.tfc.world.layer.TFCLayerUtil;

public enum RiverShapeLayer implements IAreaTransformer1
{
    INSTANCE;

    @Override
    public int applyPixel(IExtendedNoiseRandom<?> context, IArea area, int x, int z)
    {
        final int parentX = getParentX(x);
        final int parentZ = getParentY(z);
        final int parent = area.get(parentX, parentZ);
        final int parentType = RiverLayerUtil.getType(parent);
        if (parentType == RiverLayerUtil.RIVER)
        {
            RiverPixel parentPixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(parent));
            if (parentPixel != null)
            {
                context.initRandom(parentX, parentZ);
                RiverShape[] shapes = RiverShape.getShape(parentPixel);
                RiverShape shape = shapes[context.nextRandom(shapes.length)];
                Flow flow = shape.getFlow(x & 7, z & 7);
                if (flow != Flow.NONE)
                {
                    return TFCLayerUtil.RIVER_MARKER;
                }
            }
        }
        else if (parentType == RiverLayerUtil.OCEAN)
        {
            return TFCLayerUtil.OCEAN;
        }
        return TFCLayerUtil.NULL_MARKER;
    }

    @Override
    public int getParentX(int x)
    {
        return x >> 3;
    }

    @Override
    public int getParentY(int z)
    {
        return z >> 3;
    }
}
