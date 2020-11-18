package net.dries007.tfc.world.carver;

import java.util.BitSet;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;

import net.dries007.tfc.world.chunkdata.RockData;
import net.dries007.tfc.world.layer.polyhedral.AquiferLayer;
import net.dries007.tfc.world.layer.polyhedral.LocalWaterRegionLayer;
import net.dries007.tfc.world.layer.polyhedral.traits.IPolyArea;
import net.dries007.tfc.world.layer.polyhedral.traits.IPolyAreaFactory;
import net.dries007.tfc.world.layer.polyhedral.traits.PolyAreaLayerContext;

import static net.dries007.tfc.world.layer.polyhedral.LocalWaterLevelLayerUtil.AIR;
import static net.dries007.tfc.world.layer.polyhedral.LocalWaterLevelLayerUtil.WATER;

public class LocalWaterLevelCarver extends BlockCarver
{
    private final AirBlockCarver air;
    private final UnderwaterBlockCarver water;
    private final IPolyArea aquifers;

    private BitSet waterAdjacencyMask;

    public LocalWaterLevelCarver()
    {
        this.air = new AirBlockCarver();
        this.water = new UnderwaterBlockCarver();

        long seed = 42341;
        Random contextSeed = new Random(seed);
        Supplier<PolyAreaLayerContext> contextFactory = () -> new PolyAreaLayerContext(contextSeed.nextLong(), 25);

        IPolyAreaFactory area = new LocalWaterRegionLayer(seed).run(contextFactory.get());
        area = AquiferLayer.INSTANCE.run(contextFactory.get(), area);
        //for (int i = 0; i < 2; i++)
        //{
        //    area = PolyZoomLayer.INSTANCE.run(contextFactory.get(), area);
        //}

        this.aquifers = area.make();
    }

    @Override
    public boolean carve(IChunk chunk, BlockPos pos, Random random, int seaLevel)
    {
        final int maskIndex = CarverHelpers.maskIndex(pos);
        if (!liquidCarvingMask.get(maskIndex) && !airCarvingMask.get(maskIndex))
        {
            final int aquiferValue = aquifers.get(pos.getX(), pos.getY(), pos.getZ());
            final BlockPos posUp = pos.above();
            final BlockState stateAt = chunk.getBlockState(pos);
            final BlockState stateAbove = chunk.getBlockState(posUp);

            boolean canAirCarve = (pos.getY() > seaLevel || !waterAdjacencyMask.get(maskIndex));

            if (isCarvable(stateAt) && (canAirCarve || aquiferValue == WATER))
            {
                if (aquiferValue == AIR)
                {
                    if (pos.getY() < 11)
                    {
                        chunk.setBlockState(pos, Blocks.LAVA.defaultBlockState(), false);
                    }
                    else
                    {
                        chunk.setBlockState(pos, Blocks.CAVE_AIR.defaultBlockState(), false);
                    }
                }
                else if (aquiferValue == WATER)
                {
                    if (pos.getY() == 10)
                    {
                        // Top of lava level - create obsidian and magma
                        if (random.nextFloat() < 0.25f)
                        {
                            chunk.setBlockState(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), false);
                            chunk.getBlockTicks().scheduleTick(pos, Blocks.MAGMA_BLOCK, 0);
                        }
                        else
                        {
                            chunk.setBlockState(pos, Blocks.OBSIDIAN.defaultBlockState(), false);
                        }
                    }
                    else if (pos.getY() < 10)
                    {
                        // Underneath lava level, fill with lava
                        chunk.setBlockState(pos, Blocks.LAVA.defaultBlockState(), false);
                    }
                    else
                    {
                        // Below sea level, fill with water
                        chunk.setBlockState(pos, Fluids.WATER.defaultFluidState().createLegacyBlock(), false);
                    }
                }

                // Support adjacent blocks
                // Adjust above and below blocks
                setSupported(chunk, posUp, stateAbove, rockData);

                // Check below state for replacements
                BlockPos posDown = pos.below();
                BlockState stateBelow = chunk.getBlockState(posDown);
                if (exposedBlockReplacements.containsKey(stateBelow.getBlock()))
                {
                    chunk.setBlockState(posDown, exposedBlockReplacements.get(stateBelow.getBlock()).defaultBlockState(), false);
                }
                return true;

            }
        }
        return false;
    }

    @Override
    public void setContext(long worldSeed, BitSet airCarvingMask, BitSet liquidCarvingMask, RockData rockData, @Nullable BitSet waterAdjacencyMask)
    {
        this.waterAdjacencyMask = Objects.requireNonNull(waterAdjacencyMask, "LWL block carver was supplied with a null waterAdjacencyMask - this is not allowed!");
        super.setContext(worldSeed, airCarvingMask, liquidCarvingMask, rockData, waterAdjacencyMask);
    }
}
