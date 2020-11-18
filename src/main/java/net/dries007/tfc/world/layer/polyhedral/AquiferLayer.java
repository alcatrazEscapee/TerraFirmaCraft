package net.dries007.tfc.world.layer.polyhedral;


import net.dries007.tfc.world.layer.polyhedral.traits.IPolyCastleTransformer;
import net.dries007.tfc.world.layer.polyhedral.traits.PolyAreaLayerContext;

import static net.dries007.tfc.world.layer.polyhedral.LocalWaterLevelLayerUtil.*;

public enum AquiferLayer implements IPolyCastleTransformer
{
    INSTANCE;

    @Override
    public int apply(PolyAreaLayerContext context, int[] adjacent, int center)
    {
        int waterStatus = center & 3;
        for (int i : adjacent)
        {
            // If neighboring cells, and the water status don't match
            if (i >> 2 != center >> 2 && ((waterStatus == AIR && (i & 3) != AIR) || (waterStatus != AIR && (i & 3) == AIR)))
            {
                return AQUIFER;
            }
        }
        if (waterStatus == AIR)
        {
            return AIR;
        }
        return WATER; // Remove any near water
    }
}
