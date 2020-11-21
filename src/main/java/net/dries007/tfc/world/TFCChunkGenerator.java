/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.EmptyBlockReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeGenerationSettings;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.dries007.tfc.mixin.world.gen.ChunkGeneratorAccessor;
import net.dries007.tfc.world.biome.*;
import net.dries007.tfc.world.carver.CarverHelpers;
import net.dries007.tfc.world.chunk.*;
import net.dries007.tfc.world.noise.INoise2D;
import net.dries007.tfc.world.river.Flow;
import net.dries007.tfc.world.river.RiverGenerator;
import net.dries007.tfc.world.river.RiverPiece;
import net.dries007.tfc.world.river.RiverStructure;
import net.dries007.tfc.world.surfacebuilder.IContextSurfaceBuilder;

public class TFCChunkGenerator extends ChunkGenerator implements ITFCChunkGenerator
{
    public static final Codec<TFCChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeProvider.CODEC.fieldOf("biome_source").forGetter(c -> c.biomeSource),
        DimensionSettings.CODEC.fieldOf("settings").forGetter(c -> () -> c.settings),
        Codec.BOOL.fieldOf("flat_bedrock").forGetter(c -> c.flatBedrock),
        Codec.LONG.fieldOf("seed").forGetter(c -> c.seed)
    ).apply(instance, TFCChunkGenerator::new));

    public static final int SEA_LEVEL = 96;
    public static final int SPAWN_HEIGHT = SEA_LEVEL + 16;

    /**
     * This is the default instance used in the TFC preset, both on client and server
     */
    public static TFCChunkGenerator createDefaultPreset(Supplier<DimensionSettings> dimensionSettings, Registry<Biome> biomeRegistry, long seed)
    {
        return new TFCChunkGenerator(new TFCBiomeProvider(seed, 8_000, 0, 0, new TFCBiomeProvider.LayerSettings(), new TFCBiomeProvider.ClimateSettings(), biomeRegistry), dimensionSettings, false, seed);
    }

    // Noise
    private final Map<BiomeVariants, INoise2D> biomeHeightNoise;
    private final Map<BiomeVariants, INoise2D> biomeCarvingCenterNoise;
    private final Map<BiomeVariants, INoise2D> biomeCarvingHeightNoise;
    private final INoiseGenerator surfaceDepthNoise;

    private final ChunkDataProvider chunkDataProvider;
    private final ChunkBlockReplacer blockReplacer;
    private final RiverGenerator riverGenerator;

    // Properties set from codec
    private final TFCBiomeProvider biomeProvider;
    private final DimensionSettings settings;
    private final boolean flatBedrock;
    private final long seed;

    public TFCChunkGenerator(BiomeProvider biomeProvider, Supplier<DimensionSettings> settings, boolean flatBedrock, long seed)
    {
        super(biomeProvider, settings.get().structureSettings());

        if (!(biomeProvider instanceof TFCBiomeProvider))
        {
            throw new IllegalArgumentException("biome provider must extend TFCBiomeProvider");
        }
        this.biomeProvider = (TFCBiomeProvider) biomeProvider;
        this.settings = settings.get();
        this.flatBedrock = flatBedrock;
        this.seed = seed;

        this.biomeHeightNoise = new HashMap<>();
        this.biomeCarvingCenterNoise = new HashMap<>();
        this.biomeCarvingHeightNoise = new HashMap<>();

        final SharedSeedRandom seedGenerator = new SharedSeedRandom(seed);
        final long biomeNoiseSeed = seedGenerator.nextLong();

        TFCBiomes.getVariants().forEach(variant -> {
            biomeHeightNoise.put(variant, variant.createNoiseLayer(biomeNoiseSeed));
            if (variant instanceof CarvingBiomeVariants)
            {
                Pair<INoise2D, INoise2D> carvingNoise = ((CarvingBiomeVariants) variant).createCarvingNoiseLayer(biomeNoiseSeed);
                biomeCarvingCenterNoise.put(variant, carvingNoise.getFirst());
                biomeCarvingHeightNoise.put(variant, carvingNoise.getSecond());
            }
        });
        surfaceDepthNoise = new PerlinNoiseGenerator(seedGenerator, IntStream.rangeClosed(-3, 0)); // From vanilla

        // Generators / Providers
        this.chunkDataProvider = new ChunkDataProvider(new ChunkDataGenerator(seedGenerator, this.biomeProvider.getLayerSettings())); // Chunk data
        this.blockReplacer = new ChunkBlockReplacer(seedGenerator.nextLong()); // Replaces default world gen blocks with TFC variants, after surface generation
        this.biomeProvider.setChunkDataProvider(chunkDataProvider); // Allow biomes to use the chunk data temperature / rainfall variation
        this.riverGenerator = new RiverGenerator(seedGenerator.nextLong());
    }

    @Override
    public ChunkDataProvider getChunkDataProvider()
    {
        return chunkDataProvider;
    }

    @Override
    protected Codec<TFCChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seedIn)
    {
        return new TFCChunkGenerator(biomeProvider, () -> settings, flatBedrock, seedIn);
    }

    @Override
    public void createBiomes(Registry<Biome> biomeIdRegistry, IChunk chunkIn)
    {
        // No need, as it should already be set during extended chunk info
        // ((ChunkPrimer) chunkIn).setBiomes(new ColumnBiomeContainer(biomeIdRegistry, chunkIn.getPos(), biomeProvider));
    }

    /**
     * Noop - carvers are done at the beginning of feature stage, so the carver is free to check adjacent chunks for information
     */
    @Override
    public void applyCarvers(long worldSeed, BiomeManager biomeManagerIn, IChunk chunkIn, GenerationStage.Carving stage)
    {
        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final BiomeGenerationSettings settings = biomeSource.getNoiseBiome(chunk.getPos().x << 2, 0, chunk.getPos().z << 2).getGenerationSettings();
        final BiomeManager biomeManager = biomeManagerIn.withDifferentSource(this.biomeSource);
        final SharedSeedRandom random = new SharedSeedRandom();

        final BitSet liquidCarvingMask = chunk.getOrCreateCarvingMask(GenerationStage.Carving.LIQUID);
        final BitSet airCarvingMask = chunk.getOrCreateCarvingMask(GenerationStage.Carving.AIR);
        final RockData rockData = chunkDataProvider.get(chunk.getPos(), ChunkData.Status.ROCKS).getRockData();

        if (stage == GenerationStage.Carving.AIR)
        {
            // In vanilla, air carvers fire first. We do water carvers first instead, to catch them with the water adjacency mask later
            // Pass in a null adjacency mask as liquid carvers do not need it
            CarverHelpers.runCarversWithContext(worldSeed, chunk, biomeManager, settings, random, GenerationStage.Carving.LIQUID, airCarvingMask, liquidCarvingMask, rockData, null, getSeaLevel());
        }
        else
        {
            // During liquid carvers, we run air carvers instead.
            // Compute the adjacency mask here
            final BitSet waterAdjacencyMask = CarverHelpers.createWaterAdjacencyMask(chunk, getSeaLevel());
            CarverHelpers.runCarversWithContext(worldSeed, chunk, biomeManager, settings, random, GenerationStage.Carving.AIR, airCarvingMask, liquidCarvingMask, rockData, waterAdjacencyMask, getSeaLevel());
        }
    }

    /**
     * This override just ignores strongholds conditionally as by default TFC does not generate them, but  {@link ChunkGenerator} hard codes them to generate.
     */
    @Override
    public void createStructures(DynamicRegistries dynamicRegistry, StructureManager structureManager, IChunk chunkIn, TemplateManager templateManager, long seed)
    {
        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final ChunkPos chunkPos = chunk.getPos();
        final Biome biome = this.biomeSource.getNoiseBiome((chunkPos.x << 2) + 2, 0, (chunkPos.z << 2) + 2);
        for (Supplier<StructureFeature<?, ?>> supplier : biome.getGenerationSettings().structures())
        {
            ((ChunkGeneratorAccessor) this).invoke$createStructure(supplier.get(), dynamicRegistry, structureManager, chunk, templateManager, seed, chunkPos, biome);
        }
    }

    @Override
    public void createReferences(ISeedReader worldIn, StructureManager structureManager, IChunk chunkIn)
    {
        super.createReferences(worldIn, structureManager, chunkIn);

        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final ChunkPos chunkPos = chunk.getPos();
        final IWorld world = new WorldGenRegion(ServerLifecycleHooks.getCurrentServer().overworld(), Collections.singletonList(chunk));

        // Early chunk setup
        chunk.setBiomes(new ColumnBiomeContainer(worldIn.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), chunkPos, biomeProvider));
        sampleInitialChunkNoise(world, chunk);
    }

    /**
     * Surface is done in make base, bedrock is added here then block replacements are ran.
     */
    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion worldGenRegion, IChunk chunkIn)
    {
        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final ChunkPos chunkPos = chunk.getPos();
        final SharedSeedRandom random = new SharedSeedRandom();

        random.setBaseChunkSeed(chunkPos.x, chunkPos.z);
        makeBedrock(chunk, random);

        final ChunkData chunkData = chunkDataProvider.get(chunkPos, ChunkData.Status.ROCKS);
        blockReplacer.replace(chunk, chunkData);
    }

    @Override
    public int getSpawnHeight()
    {
        return SPAWN_HEIGHT;
    }

    @Override
    public TFCBiomeProvider getBiomeSource()
    {
        return biomeProvider;
    }

    /**
     * This runs after biome generation. In order to do accurate surface placement, we don't use the already generated biome container, as the biome magnifier really sucks for definition on cliffs.
     */
    @Override
    public void fillFromNoise(IWorld world, StructureManager structureManager, IChunk chunkIn)
    {
        // Initialization
        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final ChunkPos chunkPos = chunk.getPos();
        final SharedSeedRandom random = new SharedSeedRandom();
        final BitSet airCarvingMask = chunk.getOrCreateCarvingMask(GenerationStage.Carving.AIR);
        final BitSet liquidCarvingMask = chunk.getOrCreateCarvingMask(GenerationStage.Carving.LIQUID);
        final IExtendedChunk.Info extendedChunkInfo = Objects.requireNonNull(((IExtendedChunk) chunk).getExtendedInfo(), "Chunk must have extended info set!");

        random.setBaseChunkSeed(chunkPos.x, chunkPos.z);

        fillInitialChunkBlocks(chunk, extendedChunkInfo.getSurfaceHeightMap());
        updateInitialChunkHeightmaps(chunk, extendedChunkInfo.getSurfaceHeightMap());
        carveInitialChunkBlocks(chunk, extendedChunkInfo.getCarvingCenterMap(), extendedChunkInfo.getCarvingHeightMap(), airCarvingMask, liquidCarvingMask);
        buildHeightDependentRivers(chunk, chunkPos);
        buildAccurateSurface(chunk, extendedChunkInfo.getLocalBiomes(), random);
    }

    @Override
    public int getSeaLevel()
    {
        return SEA_LEVEL;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Type heightMapType)
    {
        return SEA_LEVEL;
    }

    @Override
    public IBlockReader getBaseColumn(int x, int z)
    {
        return EmptyBlockReader.INSTANCE;
    }

    protected void sampleInitialChunkNoise(IWorld world, IChunk chunkIn)
    {
        // Initialization
        final ChunkPrimer chunk = (ChunkPrimer) chunkIn;
        final ChunkPos chunkPos = chunk.getPos();
        final SharedSeedRandom random = new SharedSeedRandom();
        final int chunkX = chunkPos.getMinBlockX(), chunkZ = chunkPos.getMinBlockZ();
        final BlockPos.Mutable pos = new BlockPos.Mutable();

        random.setBaseChunkSeed(chunkPos.x, chunkPos.z);

        // The accurate version of biomes which we use for surface building
        // These are calculated during height generation in order to generate cliffs with harsh borders between biomes
        final Biome[] localBiomes = new Biome[16 * 16];

        // Height maps, computed initially for each position in the chunk
        final int[] surfaceHeightMap = new int[16 * 16];
        final double[] carvingCenterMap = new double[16 * 16];
        final double[] carvingHeightMap = new double[16 * 16];

        // The biome weights at different distance intervals
        final Object2DoubleMap<Biome> weightMap16 = new Object2DoubleOpenHashMap<>(4), weightMap4 = new Object2DoubleOpenHashMap<>(4), weightMap1 = new Object2DoubleOpenHashMap<>(4), carvingWeightMap1 = new Object2DoubleOpenHashMap<>(4);

        // Faster than vanilla (only does 2d interpolation) and uses the already generated biomes by the chunk where possible
        final ChunkArraySampler.CoordinateAccessor<Biome> biomeAccessor = (x, z) -> (Biome) SmoothColumnBiomeMagnifier.SMOOTH.getBiome(seed, chunkX + x, 0, chunkZ + z, world);
        final Function<Biome, BiomeVariants> variantAccessor = biome -> TFCBiomes.getExtensionOrThrow(world, biome).getVariants();

        final Biome[] sampledBiomes16 = ChunkArraySampler.fillSampledArray(new Biome[10 * 10], biomeAccessor, 4);
        final Biome[] sampledBiomes4 = ChunkArraySampler.fillSampledArray(new Biome[13 * 13], biomeAccessor, 2);
        final Biome[] sampledBiomes1 = ChunkArraySampler.fillSampledArray(new Biome[24 * 24], biomeAccessor);

        final Mutable<Biome> mutableBiome = new MutableObject<>();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                pos.set(chunkX + x, 0, chunkZ + z);

                // Sample biome weights at different distances
                ChunkArraySampler.fillSampledWeightMap(sampledBiomes16, weightMap16, 4, x, z);
                ChunkArraySampler.fillSampledWeightMap(sampledBiomes4, weightMap4, 2, x, z);
                ChunkArraySampler.fillSampledWeightMap(sampledBiomes1, weightMap1, x, z);

                // Group biomes at different distances. This has the effect of making some biome transitions happen over larger distances than others.
                // This is used to make most land biomes blend at maximum distance, while allowing biomes such as rivers to blend at short distances, creating better cliffs as river biomes are smaller width than other biomes.
                ChunkArraySampler.reduceGroupedWeightMap(weightMap4, weightMap16, variantAccessor.andThen(BiomeVariants::getLargeGroup), BiomeVariants.LargeGroup.SIZE);
                ChunkArraySampler.reduceGroupedWeightMap(weightMap1, weightMap4, variantAccessor.andThen(BiomeVariants::getSmallGroup), BiomeVariants.SmallGroup.SIZE);

                // First, always calculate the center height, by ignoring any possibility of carving biomes
                // The variant accessor is called for each possible biome - if we detect a carving variant, then mark it as found
                MutableBoolean hasCarvingBiomes = new MutableBoolean();
                double actualHeight = calculateNoiseColumn(weightMap1, variantAccessor, v -> {
                    if (v instanceof CarvingBiomeVariants)
                    {
                        hasCarvingBiomes.setTrue();
                        return ((CarvingBiomeVariants) v).getParent();
                    }
                    return v;
                }, chunkX + x, chunkZ + z, mutableBiome);

                double carvingCenter = 0, carvingHeight = 0, carvingWeight = 0;
                if (hasCarvingBiomes.booleanValue())
                {
                    // Calculate the carving weight map, only using local influences from carving biomes
                    ChunkArraySampler.fillSampledWeightMap(sampledBiomes1, carvingWeightMap1, x, z);

                    // Calculate the weighted carving height and center, using the modified weight map
                    for (Object2DoubleMap.Entry<Biome> entry : carvingWeightMap1.object2DoubleEntrySet())
                    {
                        final BiomeVariants variants = variantAccessor.apply(entry.getKey());
                        if (variants instanceof CarvingBiomeVariants)
                        {
                            final double weight = entry.getDoubleValue();
                            carvingWeight += weight;
                            carvingCenter += weight * biomeCarvingCenterNoise.get(variants).noise(chunkX + x, chunkZ + z);
                            carvingHeight += weight * biomeCarvingHeightNoise.get(variants).noise(chunkX + x, chunkZ + z);
                        }
                    }
                }

                // Adjust carving center towards sea level, to fill out the total weight (height defaults weight to zero so it does not need to change
                carvingCenter += SEA_LEVEL * (1 - carvingWeight);

                // Record the local (accurate) biome.
                localBiomes[x + 16 * z] = mutableBiome.getValue();

                // Record height maps
                surfaceHeightMap[x + 16 * z] = (int) actualHeight;
                carvingCenterMap[x + 16 * z] = (int) carvingCenter;
                carvingHeightMap[x + 16 * z] = (int) carvingHeight;
            }
        }

        // Record info to be accessed later
        IExtendedChunk extendedChunk = (IExtendedChunk) chunk;
        extendedChunk.setExtendedInfo(new IExtendedChunk.Info(localBiomes, surfaceHeightMap, carvingCenterMap, carvingHeightMap));
    }

    protected double calculateNoiseColumn(Object2DoubleMap<Biome> weightMap, Function<Biome, BiomeVariants> variantsAccessor, Function<BiomeVariants, BiomeVariants> variantsFilter, int x, int z, Mutable<Biome> mutableBiome)
    {
        double totalHeight = 0, riverHeight = 0, shoreHeight = 0;
        double riverWeight = 0, shoreWeight = 0;
        Biome biomeAt = null, normalBiomeAt = null, riverBiomeAt = null, shoreBiomeAt = null;
        double maxNormalWeight = 0, maxRiverWeight = 0, maxShoreWeight = 0;
        for (Object2DoubleMap.Entry<Biome> entry : weightMap.object2DoubleEntrySet())
        {
            double weight = entry.getDoubleValue();
            BiomeVariants variants = variantsAccessor.apply(entry.getKey());
            double height = weight * biomeHeightNoise.get(variantsFilter.apply(variants)).noise(x, z);
            totalHeight += height;
            if (variants == TFCBiomes.RIVER)
            {
                riverHeight += height;
                riverWeight += weight;
                if (maxRiverWeight < weight)
                {
                    riverBiomeAt = entry.getKey();
                    maxRiverWeight = weight;
                }
            }
            else if (variants == TFCBiomes.SHORE)
            {
                shoreHeight += height;
                shoreWeight += weight;
                if (maxShoreWeight < weight)
                {
                    shoreBiomeAt = entry.getKey();
                    maxShoreWeight = weight;
                }
            }
            else if (maxNormalWeight < weight)
            {
                normalBiomeAt = entry.getKey();
                maxNormalWeight = weight;
            }
        }

        double actualHeight = totalHeight;
        if (riverWeight > 0.6 && riverBiomeAt != null)
        {
            // River bottom / shore
            double aboveWaterDelta = actualHeight - riverHeight / riverWeight;
            if (aboveWaterDelta > 0)
            {
                if (aboveWaterDelta > 20)
                {
                    aboveWaterDelta = 20;
                }
                double adjustedAboveWaterDelta = 0.02 * aboveWaterDelta * (40 - aboveWaterDelta) - 0.48;
                actualHeight = riverHeight / riverWeight + adjustedAboveWaterDelta;
            }
            biomeAt = riverBiomeAt; // Use river surface for the bottom of the river + small shore beneath cliffs
        }
        else if (riverWeight > 0 && normalBiomeAt != null)
        {
            double adjustedRiverWeight = 0.6 * riverWeight;
            actualHeight = (totalHeight - riverHeight) * ((1 - adjustedRiverWeight) / (1 - riverWeight)) + riverHeight * (adjustedRiverWeight / riverWeight);

            biomeAt = normalBiomeAt;
        }
        else if (normalBiomeAt != null)
        {
            biomeAt = normalBiomeAt;
        }

        if ((shoreWeight > 0.6 || maxShoreWeight > maxNormalWeight) && shoreBiomeAt != null)
        {
            // Flatten beaches above a threshold, creates cliffs where the beach ends
            double aboveWaterDelta = actualHeight - shoreHeight / shoreWeight;
            if (aboveWaterDelta > 0)
            {
                if (aboveWaterDelta > 20)
                {
                    aboveWaterDelta = 20;
                }
                double adjustedAboveWaterDelta = 0.02 * aboveWaterDelta * (40 - aboveWaterDelta) - 0.48;
                actualHeight = shoreHeight / shoreWeight + adjustedAboveWaterDelta;
            }
            biomeAt = shoreBiomeAt;
        }

        mutableBiome.setValue(Objects.requireNonNull(biomeAt, "Biome should not be null!"));
        return actualHeight;
    }

    /**
     * Fills the initial chunk based on the surface height noise
     * Batches block state modifications by chunk section
     */
    protected void fillInitialChunkBlocks(ChunkPrimer chunk, int[] surfaceHeightMap)
    {
        final BlockState fillerBlock = settings.getDefaultBlock();
        final BlockState fillerFluid = settings.getDefaultFluid();

        for (int sectionY = 0; sectionY < 16; sectionY++)
        {
            final ChunkSection section = chunk.getOrCreateSection(sectionY);
            for (int localY = 0; localY < 16; localY++)
            {
                final int y = (sectionY << 4) | localY;
                boolean filledAny = false;
                for (int x = 0; x < 16; x++)
                {
                    for (int z = 0; z < 16; z++)
                    {
                        if (y <= surfaceHeightMap[x + 16 * z])
                        {
                            section.setBlockState(x, localY, z, fillerBlock, false);
                            filledAny = true;
                        }
                        else if (y <= SEA_LEVEL)
                        {
                            section.setBlockState(x, localY, z, fillerFluid, false);
                            filledAny = true;
                        }
                    }
                }

                if (!filledAny)
                {
                    // Nothing was filled at this y level - exit early
                    return;
                }
            }
        }
    }

    /**
     * Updates chunk height maps based on the initial surface height.
     * This is split off of {@link TFCChunkGenerator#fillInitialChunkBlocks(ChunkPrimer, int[])} as that method exits early whenever it reaches the top layer.
     */
    protected void updateInitialChunkHeightmaps(ChunkPrimer chunk, int[] surfaceHeightMap)
    {
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.WORLD_SURFACE_WG);

        final BlockState fillerBlock = settings.getDefaultBlock();
        final BlockState fillerFluid = settings.getDefaultFluid();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                final int landHeight = surfaceHeightMap[x + 16 * z];
                if (landHeight >= SEA_LEVEL)
                {
                    worldSurface.update(x, landHeight, z, fillerBlock);
                    oceanFloor.update(x, landHeight, z, fillerBlock);
                }
                else
                {
                    worldSurface.update(x, landHeight, z, fillerBlock);
                    oceanFloor.update(x, SEA_LEVEL, z, fillerFluid);
                }
            }
        }
    }

    /**
     * Applies noise level carvers to the initial chunk blocks.
     */
    protected void carveInitialChunkBlocks(ChunkPrimer chunk, double[] carvingCenterMap, double[] carvingHeightMap, BitSet airCarvingMask, BitSet liquidCarvingMask)
    {
        final Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.OCEAN_FLOOR_WG);
        final Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Type.WORLD_SURFACE_WG);

        final BlockState caveFluid = settings.getDefaultFluid();
        final BlockState caveAir = Blocks.CAVE_AIR.defaultBlockState();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                final double carvingCenter = carvingCenterMap[x + 16 * z];
                final double carvingHeight = carvingHeightMap[x + 16 * z];
                if (carvingHeight > 2f)
                {
                    // Apply carving
                    final int bottomHeight = (int) (carvingCenter - carvingHeight * 0.5f);
                    final int topHeight = (int) (carvingCenter + carvingHeight * 0.5f);

                    ChunkSection section = chunk.getOrCreateSection(bottomHeight >> 4);
                    int sectionY = bottomHeight >> 4;

                    for (int y = bottomHeight; y <= topHeight; y++)
                    {
                        final int carvingMaskIndex = CarverHelpers.maskIndex(x, y, z);

                        BlockState stateAt;
                        if (y <= SEA_LEVEL)
                        {
                            stateAt = caveFluid;
                            liquidCarvingMask.set(carvingMaskIndex, true);
                        }
                        else
                        {
                            stateAt = caveAir;
                            airCarvingMask.set(carvingMaskIndex, true);
                        }

                        // More optimizations for early chunk generation - directly access the chunk section's set block state and skip locks
                        final int currentSectionY = y >> 4;
                        if (currentSectionY != sectionY)
                        {
                            section = chunk.getOrCreateSection(currentSectionY);
                            sectionY = currentSectionY;
                        }
                        section.setBlockState(x, y & 15, z, stateAt, false);
                        worldSurface.update(x, y, z, stateAt);
                        oceanFloor.update(x, y, z, stateAt);
                    }
                }
            }
        }
    }

    protected void buildHeightDependentRivers(ChunkPrimer chunk, ChunkPos chunkPos)
    {
        for (RiverStructure river : riverGenerator.generateRiversAroundChunk(chunkPos))
        {
            for (RiverPiece piece : river.getPieces())
            {
                int xPos = (int) piece.getX();
                int zPos = (int) piece.getZ();
                for (int x = 0; x < piece.getWidth(); x++)
                {
                    for (int z = 0; z < piece.getWidth(); z++)
                    {
                        if ((xPos + x) >> 4 == chunkPos.x && (zPos + z) >> 4 == chunkPos.z)
                        {
                            // Intersect the chunk at hand
                            Flow flow = piece.getFlow(x, z);
                            if (flow != Flow.NONE)
                            {
                                chunk.setBlockState(new BlockPos(xPos + x, 150, zPos + z), Blocks.RED_STAINED_GLASS.defaultBlockState(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds surface, but based on a (older style) biome array as opposed to the noise based biome sampling used in vanilla
     */
    protected void buildAccurateSurface(IChunk chunk, Biome[] accurateChunkBiomes, Random random)
    {
        final ChunkPos chunkPos = chunk.getPos();
        final ChunkData chunkData = chunkDataProvider.get(chunkPos, ChunkData.Status.EMPTY);
        for (int x = 0; x < 16; ++x)
        {
            for (int z = 0; z < 16; ++z)
            {
                final int posX = chunkPos.getMinBlockX() + x;
                final int posZ = chunkPos.getMinBlockZ() + z;
                final int posY = chunk.getHeight(Heightmap.Type.WORLD_SURFACE_WG, x, z) + 1;
                final double noise = surfaceDepthNoise.getSurfaceNoiseValue(posX * 0.0625, posZ * 0.0625, 0.0625, x * 0.0625) * 15;

                final Biome biome = accurateChunkBiomes[x + 16 * z];
                IContextSurfaceBuilder.applyIfPresent(biome.getGenerationSettings().getSurfaceBuilder().get(), random, chunkData, chunk, biome, posX, posZ, posY, noise, seed, settings.getDefaultBlock(), settings.getDefaultFluid(), getSeaLevel());
            }
        }
    }

    /**
     * Builds either a single flat layer of bedrock, or natural vanilla bedrock
     * Writes directly to the bottom chunk section for better efficiency
     */
    protected void makeBedrock(ChunkPrimer chunk, Random random)
    {
        final ChunkSection bottomSection = chunk.getOrCreateSection(0);
        final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int yMax = flatBedrock ? 0 : random.nextInt(5);
                for (int y = 0; y < yMax; y++)
                {
                    bottomSection.setBlockState(x, y, z, bedrock, false);
                }
            }
        }
    }
}