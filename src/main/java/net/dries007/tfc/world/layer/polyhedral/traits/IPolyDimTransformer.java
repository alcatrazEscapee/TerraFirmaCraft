package net.dries007.tfc.world.layer.polyhedral.traits;

import net.minecraft.world.gen.layer.traits.IDimTransformer;

public interface IPolyDimTransformer extends IDimTransformer
{
    int getParentX(int x);

    int getParentY(int y);

    int getParentZ(int z);
}
