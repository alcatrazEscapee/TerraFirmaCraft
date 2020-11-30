package net.dries007.tfc.unit;

import java.awt.*;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import net.minecraft.util.Direction;
import net.minecraft.world.gen.LazyAreaLayerContext;
import net.minecraft.world.gen.area.IArea;
import net.minecraft.world.gen.area.IAreaFactory;
import net.minecraft.world.gen.area.LazyArea;
import net.minecraft.world.gen.layer.SmoothLayer;
import net.minecraft.world.gen.layer.ZoomLayer;

import net.dries007.tfc.Artist;
import net.dries007.tfc.river.*;
import net.dries007.tfc.util.IArtist;
import net.dries007.tfc.world.biome.TFCBiomeProvider;
import net.dries007.tfc.world.layer.*;
import net.dries007.tfc.world.layer.traits.FastArea;
import net.dries007.tfc.world.layer.traits.FastAreaContext;
import net.dries007.tfc.world.layer.traits.ITypedAreaFactory;
import net.dries007.tfc.world.layer.traits.TypedAreaContext;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.CellularNoiseType;
import org.junit.jupiter.api.Test;

/**
 * As of right now, {@link TFCLayerUtil} does 7 zoom layers following the point at which rivers branch off
 * The shape assignment layer is an equivalent zoom of 3 layers.
 * It ideally needs 1-2 extra zooms (or possibly application of biome widen layer, modified) after
 * That leaves room for 2-3 zooms that need to be done prior to shape assignment
 */
public class RiverLayerTests
{
    static final Artist.Typed<IAreaFactory<FastArea>, Integer> AREA = Artist.forMap(factory -> Artist.Pixel.coerceInt(factory.make()::get));
    static final Artist.Raw RAW = Artist.raw();
    static final Artist.Colored<IAreaFactory<FastArea>> DETAILED = Artist.forColor(factory -> {
        FastArea area = factory.make();
        return Artist.Pixel.coerceInt((x, z) -> {
            int px = x / 3, pz = z / 3;
            int cx = x % 3, cz = z % 3;
            int value = area.get(px, pz);
            int type = RiverLayerUtil.getType(value);
            if (type == RiverLayerUtil.RIVER)
            {
                RiverPixel pixel = RiverPixel.byIndex(RiverLayerUtil.getMeta(value));
                if (pixel != null)
                {
                    return riverPixelColor(pixel, cx, cz);
                }
            }
            return riverColor(value);
        });
    });

    private static Color riverPixelColor(RiverPixel pixel, int cx, int cz)
    {
        // x/z 0 1 2
        //  0  . N .
        //  1  W + E
        //  2  . S .
        // River
        if ((cx == 0 || cx == 2) && (cz == 0 || cz == 2))
        {
            return new Color(40, 160, 40); // Cornets
        }
        if (cx == 1 && cz == 1)
        {
            return new Color(100, 200, 250); // Center
        }
        if (cx == 0)
        {
            return flowColor(pixel.getDrain() == Direction.WEST, pixel.isSourceWest());
        }
        if (cx == 2)
        {
            return flowColor(pixel.getDrain() == Direction.EAST, pixel.isSourceEast());
        }
        if (cz == 0)
        {
            return flowColor(pixel.getDrain() == Direction.NORTH, pixel.isSourceNorth());
        }
        return flowColor(pixel.getDrain() == Direction.SOUTH, pixel.isSourceSouth());
    }

    private static Color flowColor(boolean drain, boolean source)
    {
        //if (drain || source) return new Color(100, 200, 250); // For uniform river color
        if (drain) return new Color(60, 160, 220);
        if (source) return new Color(140, 240, 250);
        return new Color(40, 160, 40);
    }


    private static Color riverColor(int value)
    {
        int type = RiverLayerUtil.getType(value);
        int height = RiverLayerUtil.getHeight(value);
        if (type == RiverLayerUtil.OCEAN) return new Color(0, 0, 250);
        if (type == RiverLayerUtil.RIVER) return new Color(140, 240, 250);
        if (type == RiverLayerUtil.LAND) return new Color(50, 220 - height * 8, 50);
        return Color.BLACK;
    }

    private static Color biomeColor(int value)
    {
        if (value == TFCLayerUtil.RIVER_MARKER) return new Color(140, 240, 250);
        if (value == TFCLayerUtil.OCEAN) return new Color(0, 0, 250);
        if (value == TFCLayerUtil.NULL_MARKER) return new Color(150, 150, 150);
        return Color.BLACK;
    }

    @Test
    public void testRiverLayers()
    {
        final long seed = System.currentTimeMillis();
        final Random random = new Random(seed);
        final Supplier<FastAreaContext> layerContext = () -> new FastAreaContext(seed, random.nextLong());
        final Supplier<TypedAreaContext<Plate>> plateContext = () -> new TypedAreaContext<>(seed, random.nextLong());
        final TFCBiomeProvider.LayerSettings layerSettings = new TFCBiomeProvider.LayerSettings();

        int baseSize = 20;
        int layerCount = 0;

        AREA.dimensionsSized(baseSize).color(RiverLayerTests::riverColor);
        DETAILED.dimensionsSized(baseSize * 3);

        ITypedAreaFactory<Plate> plateLayer;
        IAreaFactory<FastArea> layer;

        // Initial plate tectonics
        plateLayer = new PlateGenerationLayer(new Cellular2D(random.nextInt(), 1.0f, CellularNoiseType.VALUE).spread(0.2f), layerSettings.getOceanPercent()).apply(plateContext.get());
        plateLayer = TypedZoomLayer.<Plate>fuzzy().run(plateContext.get(), plateLayer);

        layer = PlateBoundaryLayer.INSTANCE.run(layerContext.get(), plateLayer);
        layer = SmoothLayer.INSTANCE.run(layerContext.get(), layer);
        layer = PlateBoundaryModifierLayer.INSTANCE.run(layerContext.get(), layer);

        // Convert to river values
        layer = RiverInitLayer.INSTANCE.run(layerContext.get(), layer);
        AREA.draw("river_layer_" + ++layerCount, layer);

        // Expand the initial land area - ensures that rivers always reach the coast after zoom layer application
        layer = RiverLandExpandLayer.INSTANCE.run(layerContext.get(), layer);
        AREA.draw("river_layer_" + ++layerCount, layer);

        // Drain, and expansion (with controlled branching)
        layer = RiverDrainLayer.INSTANCE.run(layerContext.get(), layer);
        DETAILED.draw("river_layer_" + ++layerCount, layer);

        layer = RiverHeightLayer.INSTANCE.run(layerContext.get(), layer);
        DETAILED.draw("river_layer_" + ++layerCount, layer);

        for (int i = 0; i < 13; i++)
        {
            layer = RiverExpansionLayer.INSTANCE.run(layerContext.get(), layer);
            DETAILED.draw("river_layer_" + ++layerCount, layer);
        }

        for (int i = 0; i < 2; i++)
        {
            layer = RiverZoomLayer.INSTANCE.run(layerContext.get(), layer);
            baseSize *= 2;
            DETAILED.dimensionsSized(baseSize * 3).draw("river_layer_" + ++layerCount, layer);

            // todo: also expand drain -> oceans? due to land cutting off rivers
            layer = RiverExpansionLayer.INSTANCE.run(layerContext.get(), layer);
            DETAILED.draw("river_layer_" + ++layerCount, layer);

            layer = RiverExpansionLayer.INSTANCE.run(layerContext.get(), layer);
            DETAILED.draw("river_layer_" + ++layerCount, layer);
        }

        layer = RiverZoomLayer.INSTANCE.run(layerContext.get(), layer);
        baseSize *= 2;
        DETAILED.dimensionsSized(baseSize * 3).draw("river_layer_" + ++layerCount, layer);

        layer = RiverShapeLayer.INSTANCE.run(layerContext.get(), layer);
        baseSize *= 8;
        AREA.dimensionsSized(baseSize).color(RiverLayerTests::biomeColor).draw("river_layer_" + ++layerCount, layer);

        for (int i = 0; i < 1; i++)
        {
            layer = ZoomLayer.NORMAL.run(layerContext.get(), layer);
            baseSize *= 2;
            AREA.dimensions(baseSize).draw("river_layer_" + ++layerCount, layer);
        }

        FastArea riverLayer = layer.make();
        Mutable<IArea> biomeLayerSansRivers = new MutableObject<>();
        TFCLayerUtil.createOverworldBiomeLayer(seed, layerSettings, IArtist.nope(), (name, index, instance) -> {
            if (name.equals("biomes") && index == 14)
            {
                biomeLayerSansRivers.setValue(instance.make());
            }
        });

        RAW.centerSized(1000).draw("river_layer_" + ++layerCount, Artist.Pixel.coerceInt((x, z) -> {
            int land = biomeLayerSansRivers.getValue().get(x, z);
            int river = riverLayer.get(x, z);
            int value;
            if (TFCLayerUtil.hasRiver(land) && river == TFCLayerUtil.RIVER_MARKER)
            {
                value = TFCLayerUtil.riverFor(land);
            }
            else
            {
                value = land;
            }
            return TFCLayerUtilTests.biomeColor(value);
        }));
    }

    @Test
    public void testRiverZoomLayer()
    {
        // Initial tests of the zoom layer
        RAW.dimensionsSized(8 * 64).draw("river_zoom_layer", Artist.Pixel.coerceInt((x, z) -> {
            int px = x / 8, pz = z / 8;
            int cx = x % 8, cz = z % 8;
            RiverPixel parent = RiverPixel.byIndex(pz);
            if (parent != null)
            {
                if (px == 0)
                {
                    // Parent pixel
                    if (cx > 0 && cz > 0 && cx < 7 && cz < 7)
                    {
                        return riverPixelColor(parent, ((cx - 1) / 2), (cz - 1) / 2);
                    }
                }
                else
                {
                    if (cx > 0 && cz > 0 && cx < 7 && cz < 7)
                    {
                        int mapOffset = px - 1;
                        for (int j = 0; j < 16; j++)
                        {
                            int pixelMapIndex = RiverZoomLayer.INSTANCE.pixelMapIndex(parent, j);
                            int[] mapValues = RiverZoomLayer.INSTANCE.pixelMap[pixelMapIndex];
                            if (0 <= mapOffset && mapOffset < mapValues.length >> 2)
                            {
                                int ux = (cx - 1) % 3, uz = (cz - 1) % 3;
                                int vx = cx >= 4 ? 1 : 0, vz = cz >= 4 ? 1 : 0;
                                int mapLocal = vx | (vz << 1);
                                RiverPixel zoomedPixel = RiverPixel.byIndex(mapValues[(mapOffset << 2) | mapLocal]);
                                if (zoomedPixel != null)
                                {
                                    return riverPixelColor(zoomedPixel, ux, uz);
                                }
                                return new Color(150, 200, 150);
                            }
                            mapOffset -= (mapValues.length >> 2) + 1;
                        }
                    }
                }
                return new Color(150, 150, 150);
            }
            return new Color(180, 180, 180);
        }));
    }
}
