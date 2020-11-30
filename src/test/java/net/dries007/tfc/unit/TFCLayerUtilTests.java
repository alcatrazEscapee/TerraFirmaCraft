/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.unit;

import java.awt.*;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.area.IAreaFactory;

import net.dries007.tfc.Artist;
import net.dries007.tfc.util.IArtist;
import net.dries007.tfc.world.biome.BiomeVariants;
import net.dries007.tfc.world.biome.TFCBiomeProvider;
import net.dries007.tfc.world.biome.VolcanoNoise;
import net.dries007.tfc.world.layer.Plate;
import net.dries007.tfc.world.layer.TFCLayerUtil;
import net.dries007.tfc.world.layer.traits.ITypedAreaFactory;
import net.dries007.tfc.world.layer.traits.TypedArea;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.CellularNoiseType;
import net.dries007.tfc.world.noise.INoise2D;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static net.dries007.tfc.world.layer.TFCLayerUtil.*;

public class TFCLayerUtilTests
{
    // These inner lambdas could be shortened to factory.make()::get, but javac gets confused with the type parameters and fails to compile, even though IDEA thinks it's valid.
    static final Artist.Typed<ITypedAreaFactory<Plate>, Plate> PLATES = Artist.forMap(factory -> {
        final TypedArea<Plate> area = factory.make();
        return Artist.Pixel.coerceInt(area::get);
    });
    static final Artist.Typed<IAreaFactory<? extends IArea>, Integer> AREA = Artist.forMap(factory -> {
        final IArea area = factory.make();
        return Artist.Pixel.coerceInt(area::get);
    });
    static final Artist.Noise<IAreaFactory<? extends IArea>> FLOAT_AREA = AREA.mapNoise(Float::intBitsToFloat);
    static final Artist.Raw RAW = Artist.raw().size(1000);

    @Test
    public void testCreateOverworldBiomeLayer()
    {
        final long seed = System.currentTimeMillis();
        final TFCBiomeProvider.LayerSettings settings = new TFCBiomeProvider.LayerSettings();

        // Drawing is done via callbacks in TFCLayerUtil
        IArtist<ITypedAreaFactory<Plate>> plateArtist = (name, index, instance) -> {
            PLATES.color(TFCLayerUtilTests::plateElevationColor);
            PLATES.centerSized(index == 1 ? 10 : 20).draw(name + '_' + index, instance);
            PLATES.centerSized(index == 1 ? 100 : 200).draw(name + '_' + index + "_wide", instance);
        };

        IArtist<IAreaFactory<? extends IArea>> layerArtist = (name, index, instance) -> {
            switch (name)
            {
                case "plate_boundary":
                    AREA.centerSized(20).color(TFCLayerUtilTests::plateBoundaryColor).draw(name + '_' + index, instance);
                    AREA.centerSized(200).draw(name + '_' + index + "_wide", instance);
                    break;
                case "river":
                {
                    int zoom;
                    if (index <= 5) zoom = index - 1;
                    else if (index <= 7) zoom = 4;
                    else zoom = 5;

                    if (index <= 5)
                        FLOAT_AREA.centerSized((1 << zoom) * 40).color(Artist.Colors.LINEAR_BLUE_RED).draw(name + '_' + index, instance);
                    else
                        AREA.centerSized((1 << zoom) * 10).color(TFCLayerUtilTests::riverColor).draw(name + '_' + index, instance);
                    break;
                }
                case "lake":
                {
                    int zoom;
                    if (index <= 1) zoom = 0;
                    else if (index <= 3) zoom = 1;
                    else if (index <= 5) zoom = 2;
                    else zoom = 3;

                    AREA.centerSized((1 << zoom) * 40).color(TFCLayerUtilTests::lakeColor).draw(name + '_' + index, instance);
                    break;
                }
                case "biomes":
                {
                    int zoom;
                    if (index <= 2) zoom = 0;
                    else if (index <= 5) zoom = 1;
                    else if (index <= 7) zoom = 2;
                    else if (index <= 10) zoom = 3;
                    else zoom = 4;

                    AREA.color(TFCLayerUtilTests::biomeColor).centerSized((1 << zoom) * 20).draw(name + '_' + index, instance);
                    if (index == 18)
                    {
                        AREA.color(TFCLayerUtilTests::biomeColor).center(10_000).size(4_000).draw(name + '_' + index + "_wide", instance);
                    }
                    break;
                }
            }
        };

        TFCLayerUtil.createOverworldBiomeLayer(seed, settings, plateArtist, layerArtist);
    }

    @Test
    public void testOverworldForestLayer()
    {
        final long seed = System.currentTimeMillis();
        final TFCBiomeProvider.LayerSettings settings = new TFCBiomeProvider.LayerSettings();

        IArtist<IAreaFactory<? extends IArea>> artist = (name, index, instance) -> {
            int zoom;
            if (index <= 2) zoom = 1;
            else if (index <= 4) zoom = 2;
            else if (index == 5) zoom = 3;
            else if (index <= 8) zoom = 4;
            else zoom = 5;

            AREA.color(TFCLayerUtilTests::forestColor).centerSized(4 * (1 << zoom));
            AREA.draw(name + '_' + index, instance);
        };

        TFCLayerUtil.createOverworldForestLayer(seed, settings, artist);
    }

    @Test
    @Disabled
    public void testBiomesWithVolcanoes()
    {
        long seed = System.currentTimeMillis();

        Cellular2D volcanoNoise = VolcanoNoise.cellNoise(seed);
        INoise2D volcanoJitterNoise = VolcanoNoise.distanceVariationNoise(seed);

        IArea biomeArea = TFCLayerUtil.createOverworldBiomeLayer(seed, new TFCBiomeProvider.LayerSettings(), IArtist.nope(), IArtist.nope()).make();

        Artist.Pixel<Color> volcanoBiomeMap = Artist.Pixel.coerceFloat((x, z) -> {
            int value = biomeArea.get(((int) x) >> 2, ((int) z) >> 2);
            BiomeVariants biome = TFCLayerUtil.getFromLayerId(value);
            if (biome.isVolcanic())
            {
                float distance = volcanoNoise.noise(x, z) + volcanoJitterNoise.noise(x, z);
                float volcano = VolcanoNoise.calculateEasing(distance);
                float chance = volcanoNoise.noise(x, z, CellularNoiseType.VALUE);
                if (volcano > 0 && chance < biome.getVolcanoChance())
                {
                    return new Color(MathHelper.clamp((int) (155 + 100 * volcano), 0, 255), 30, 30); // Near volcano
                }
            }
            return biomeColor(value);
        });

        RAW.center(20_000).size(1_000); // 40 km image, at 1 pixel = 20 blocks
        RAW.draw("volcano_biome_map", volcanoBiomeMap);
    }

    public static Color plateElevationColor(Plate plate)
    {
        if (plate.isOceanic())
        {
            return new Color(0, MathHelper.clamp((int) (plate.getElevation() * 255), 0, 255), 255);
        }
        else
        {
            return new Color(0, MathHelper.clamp((int) (100 + 155 * plate.getElevation()), 100, 255), 0);
        }
    }

    public static Color plateBoundaryColor(int value)
    {
        if (value == OCEANIC) return new Color(0, 0, 200);
        if (value == CONTINENTAL_LOW) return new Color(50, 200, 50);
        if (value == CONTINENTAL_MID) return new Color(50, 150, 50);
        if (value == CONTINENTAL_HIGH) return new Color(70, 100, 70);
        if (value == OCEAN_OCEAN_DIVERGING) return new Color(150, 0, 255);
        if (value == OCEAN_OCEAN_CONVERGING_LOWER) return new Color(230, 80, 155);
        if (value == OCEAN_OCEAN_CONVERGING_UPPER) return new Color(250, 100, 255);
        if (value == OCEAN_CONTINENT_CONVERGING_LOWER) return new Color(210, 60, 0);
        if (value == OCEAN_CONTINENT_CONVERGING_UPPER) return new Color(250, 130, 0);
        if (value == OCEAN_CONTINENT_DIVERGING) return new Color(250, 200, 0);
        if (value == CONTINENT_CONTINENT_DIVERGING) return new Color(0, 180, 130);
        if (value == CONTINENT_CONTINENT_CONVERGING) return new Color(0, 230, 180);
        if (value == CONTINENTAL_SHELF) return new Color(0, 200, 255);
        return Color.BLACK;
    }

    public static Color biomeColor(int id)
    {
        if (id == DEEP_OCEAN) return new Color(0, 0, 250);
        if (id == OCEAN) return new Color(60, 100, 250);
        if (id == PLAINS) return new Color(0, 150, 0);
        if (id == HILLS) return new Color(30, 130, 30);
        if (id == LOWLANDS) return new Color(20, 200, 180);
        if (id == LOW_CANYONS) return new Color(40, 100, 40);
        if (id == ROLLING_HILLS) return new Color(100, 100, 0);
        if (id == BADLANDS) return new Color(150, 100, 0);
        if (id == PLATEAU) return new Color(200, 100, 0);
        if (id == OLD_MOUNTAINS) return new Color(200, 150, 100);
        if (id == MOUNTAINS) return new Color(200, 200, 200);
        if (id == VOLCANIC_MOUNTAINS) return new Color(255, 150, 150);
        if (id == OCEANIC_MOUNTAINS) return new Color(180, 180, 250);
        if (id == VOLCANIC_OCEANIC_MOUNTAINS) return new Color(255, 140, 200);
        if (id == CANYONS) return new Color(160, 60, 60);
        if (id == SHORE) return new Color(255, 230, 160);
        if (id == LAKE) return new Color(120, 200, 255);
        if (id == RIVER) return new Color(80, 140, 255);
        if (id == OLD_MOUNTAIN_LAKE || id == OCEANIC_MOUNTAIN_LAKE || id == PLATEAU_LAKE || id == MOUNTAIN_LAKE || id == VOLCANIC_MOUNTAIN_LAKE || id == VOLCANIC_OCEANIC_MOUNTAIN_LAKE)
            return new Color(150, 140, 205);
        if (id == OLD_MOUNTAIN_RIVER || id == OCEANIC_MOUNTAIN_RIVER || id == MOUNTAIN_RIVER || id == VOLCANIC_OCEANIC_MOUNTAIN_RIVER || id == VOLCANIC_MOUNTAIN_RIVER)
            return new Color(130, 110, 205);
        if (id == DEEP_OCEAN_TRENCH) return new Color(15, 40, 170);
        if (id == OCEAN_OCEAN_CONVERGING_MARKER) return new Color(160, 160, 255);
        if (id == OCEAN_OCEAN_DIVERGING_MARKER) return new Color(0, 0, 100);
        if (id == OCEAN_REEF_MARKER) return new Color(200, 200, 0);
        if (id == OCEAN_REEF) return new Color(200, 250, 100);
        return Color.BLACK;
    }

    public static Color riverColor(int id)
    {
        if (id == RIVER_MARKER) return new Color(80, 140, 255);
        return Color.BLACK;
    }

    public static Color lakeColor(int id)
    {
        if (id == LAKE_MARKER) return new Color(20, 140, 255);
        if (id == INLAND_MARKER) return new Color(100, 100, 100);
        return Color.BLACK;
    }

    public static Color forestColor(int id)
    {
        if (id == FOREST_NONE) return new Color(140, 140, 140);
        if (id == FOREST_NORMAL) return new Color(50, 200, 50);
        if (id == FOREST_SPARSE) return new Color(100, 180, 100);
        if (id == FOREST_EDGE) return new Color(180, 140, 40);
        if (id == FOREST_OLD) return new Color(0, 80, 0);
        return Color.BLACK;
    }
}
