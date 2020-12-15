package net.dries007.tfc.common.blocks.crop;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import net.minecraftforge.common.Tags;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;

public abstract class ClimbingCropBlock extends DoubleCropBlock
{
    public static final BooleanProperty STICK = TFCBlockStateProperties.STICK;

    public static ClimbingCropBlock create(Properties properties, int singleStages, int doubleStages, Crop crop)
    {
        IntegerProperty property = TFCBlockStateProperties.getAgeProperty(singleStages + doubleStages - 1);
        return new ClimbingCropBlock(properties, singleStages - 1, singleStages + doubleStages - 1, TFCBlocks.DEAD_CROPS.get(crop), TFCItems.CROP_SEEDS.get(crop), crop.getPrimaryNutrient()) {
            @Override
            public IntegerProperty getAgeProperty()
            {
                return property;
            }
        };
    }

    protected ClimbingCropBlock(Properties properties, int maxSingleAge, int maxAge, Supplier<? extends Block> dead, Supplier<? extends Item> seeds, FarmlandTileEntity.NutrientType primaryNutrient)
    {
        super(properties, maxSingleAge, maxAge, dead, seeds, primaryNutrient);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit)
    {
        final ItemStack heldStack = player.getItemInHand(handIn);
        if (!worldIn.isClientSide() && heldStack.getItem().is(Tags.Items.RODS_WOODEN) && !state.getValue(STICK) && pos.getY() < 255)
        {
            // Add a stick
            worldIn.setBlock(pos, state.setValue(STICK, true), 3);
            worldIn.setBlock(pos.above(), state.setValue(STICK, true).setValue(PART, Part.TOP), 3);
            heldStack.shrink(1);
            return ActionResultType.CONSUME;
        }
        return ActionResultType.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(STICK));
    }
}
