package net.dries007.tfc.world.layer.polyhedral.traits;

public interface IPolyAreaTransformer0
{
    default IPolyAreaFactory run(PolyAreaLayerContext context)
    {
        return () -> context.createResult((x, y, z) -> {
            context.initRandom(x, y, z);
            return apply(context, x, y, z);
        });
    }

    int apply(PolyAreaLayerContext context, int x, int y, int z);
}
