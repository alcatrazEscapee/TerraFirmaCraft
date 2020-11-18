package net.dries007.tfc.world.layer.polyhedral.traits;

/**
 * @see net.minecraft.world.gen.layer.traits.ICastleTransformer
 */
public interface IPolyCastleTransformer extends IPolyAreaTransformer1, IPolyDimTransformer
{
    @Override
    @SuppressWarnings("PointlessArithmeticExpression")
    default int apply(PolyAreaLayerContext context, IPolyArea area, int x, int y, int z)
    {
        return apply(context, new int[] {
            area.get(getParentX(x + 1), getParentY(y + 1), getParentZ(z + 0)),
            area.get(getParentX(x + 2), getParentY(y + 1), getParentZ(z + 1)),
            area.get(getParentX(x + 1), getParentY(y + 1), getParentZ(z + 2)),
            area.get(getParentX(x + 0), getParentY(y + 1), getParentZ(z + 1)),
            area.get(getParentX(x + 1), getParentY(y + 0), getParentZ(z + 1)),
            area.get(getParentX(x + 1), getParentY(y + 2), getParentZ(z + 1)),
        }, area.get(getParentX(x + 1), getParentY(y + 1), getParentZ(z + 1)));
    }

    int apply(PolyAreaLayerContext context, int[] adjacent, int center);

    @Override
    default int getParentX(int x)
    {
        return x - 1;
    }

    @Override
    default int getParentY(int y)
    {
        return y - 1;
    }

    @Override
    default int getParentZ(int z)
    {
        return z - 1;
    }
}
