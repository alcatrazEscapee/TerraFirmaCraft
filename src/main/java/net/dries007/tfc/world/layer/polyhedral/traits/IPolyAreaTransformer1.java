package net.dries007.tfc.world.layer.polyhedral.traits;

/**
 * @see net.minecraft.world.gen.layer.traits.IAreaTransformer1
 */
public interface IPolyAreaTransformer1 extends IPolyDimTransformer
{
    default IPolyAreaFactory run(PolyAreaLayerContext context, IPolyAreaFactory areaFactory)
    {
        return () -> {
            LazyPolyArea area = areaFactory.make();
            return context.createResult((x, y, z) -> {
                context.initRandom(x, y, z);
                return apply(context, area, x, y, z);
            }, area.getMaxCache());
        };
    }

    int apply(PolyAreaLayerContext context, IPolyArea area, int x, int y, int z);
}
