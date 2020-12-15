package net.dries007.tfc.world.chunkdata;

/**
 * Values for plate tectonics
 * todo: FIX
 * These must match the compile time constants in {@link net.dries007.tfc.world.layer.TFCLayerUtil}
 */
public enum PlateTectonicsClassification
{
    OCEANIC,
    CONTINENTAL_LOW,
    CONTINENTAL_MID,
    CONTINENTAL_HIGH,
    OCEAN_OCEAN_DIVERGING,
    OCEAN_OCEAN_CONVERGING_LOWER,
    OCEAN_OCEAN_CONVERGING_UPPER,
    OCEAN_CONTINENT_CONVERGING_LOWER,
    OCEAN_CONTINENT_CONVERGING_UPPER,
    OCEAN_CONTINENT_DIVERGING,
    CONTINENT_CONTINENT_DIVERGING,
    CONTINENT_CONTINENT_CONVERGING,
    CONTINENTAL_SHELF;

    private static final PlateTectonicsClassification[] VALUES = values();

    public static PlateTectonicsClassification valueOf(int i)
    {
        return i >= 0 && i < VALUES.length ? VALUES[i] : OCEANIC;
    }

    private final String translationKey;

    PlateTectonicsClassification()
    {
        this.translationKey = name().toLowerCase();
    }

    public String getTranslationKey()
    {
        return translationKey;
    }
}
