package net.dries007.tfc.river;

import net.minecraft.world.gen.INoiseRandom;
import net.minecraft.world.gen.layer.traits.IC1Transformer;

import static net.dries007.tfc.world.layer.TFCLayerUtil.*;

public enum RiverInitLayer implements IC1Transformer
{
    INSTANCE;

    @Override
    public int apply(INoiseRandom context, int value)
    {
        switch (value)
        {
            case OCEANIC:
            case OCEAN_CONTINENT_CONVERGING_LOWER:
            case OCEAN_OCEAN_CONVERGING_UPPER:
            case OCEAN_OCEAN_CONVERGING_LOWER:
            case OCEAN_OCEAN_DIVERGING:
            case CONTINENTAL_SHELF:
                return RiverLayerUtil.OCEAN;
            case CONTINENTAL_LOW:
            case CONTINENTAL_HIGH:
            case CONTINENTAL_MID:
            case CONTINENT_CONTINENT_CONVERGING:
            case CONTINENT_CONTINENT_DIVERGING:
            case OCEAN_CONTINENT_CONVERGING_UPPER:
                return RiverLayerUtil.LAND;
        }
        throw new IllegalStateException("What is this: " + value);
    }
}
