package net.dries007.tfc.common.blocks.crop;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;

public abstract class DefaultCropBlock extends CropBlock
{
    public static DefaultCropBlock create(Properties properties, int stages, Crop crop)
    {
        IntegerProperty property = TFCBlockStateProperties.getAgeProperty(stages - 1);
        return new DefaultCropBlock(properties, stages - 1, TFCBlocks.DEAD_CROPS.get(crop), TFCItems.CROP_SEEDS.get(crop), crop.getPrimaryNutrient()) {
            @Override
            public IntegerProperty getAgeProperty()
            {
                return property;
            }
        };
    }

    protected DefaultCropBlock(Properties properties, int maxAge, Supplier<? extends Block> dead, Supplier<? extends Item> seeds, FarmlandTileEntity.NutrientType primaryNutrient)
    {
        super(properties, maxAge, dead, seeds, primaryNutrient);
    }
}
