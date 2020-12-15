package net.dries007.tfc.common.blocks.crop;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

import net.dries007.tfc.common.tileentity.FarmlandTileEntity;

public enum Crop
{
    // Grains
    BARLEY(FarmlandTileEntity.NutrientType.NITROGEN, self -> DefaultCropBlock.create(props(), 8, self), self -> new DeadCropBlock(props())), // Default, 8
    OAT(FarmlandTileEntity.NutrientType.PHOSPHOROUS, self -> DefaultCropBlock.create(props(), 8, self), self -> new DeadCropBlock(props())), // Default, 8
    RYE(FarmlandTileEntity.NutrientType.PHOSPHOROUS, self -> DefaultCropBlock.create(props(), 8, self), self -> new DeadCropBlock(props())), // Default, 8
    MAIZE(FarmlandTileEntity.NutrientType.PHOSPHOROUS, self -> DoubleCropBlock.create(props(), 3, 3, self), self -> new DoubleDeadCropBlock(props())), // Double, 3 -> 3
    WHEAT(FarmlandTileEntity.NutrientType.PHOSPHOROUS, self -> DefaultCropBlock.create(props(), 8, self), self -> new DeadCropBlock(props())), // Default, 8
    RICE(FarmlandTileEntity.NutrientType.PHOSPHOROUS, self -> FloodedCropBlock.create(props(), 8, self), self -> new FloodedDeadCropBlock(props())), // Default, Waterlogged, 8
    // Vegetables
    BEET(FarmlandTileEntity.NutrientType.POTASSIUM, self -> RootCropBlock.create(props(), 6, self), self -> new DeadCropBlock(props())), // Default, Root, 6
    CABBAGE(FarmlandTileEntity.NutrientType.NITROGEN, self -> DefaultCropBlock.create(props(), 6, self), self -> new DeadCropBlock(props())), // Default, 6
    CARROT(FarmlandTileEntity.NutrientType.POTASSIUM, self -> RootCropBlock.create(props(), 5, self), self -> new DeadCropBlock(props())), // Default, Root, 5
    GARLIC(FarmlandTileEntity.NutrientType.NITROGEN, self -> RootCropBlock.create(props(), 5, self), self -> new DeadCropBlock(props())), // Default, Root, 5
    GREEN_BEAN(FarmlandTileEntity.NutrientType.NITROGEN, self -> ClimbingCropBlock.create(props(), 4, 4, self), self -> new ClimbingDeadCropBlock(props())), // Double, Pickable, Stick, 4 -> 4
    POTATO(FarmlandTileEntity.NutrientType.POTASSIUM, self -> RootCropBlock.create(props(), 7, self), self -> new DeadCropBlock(props())), // Default, Root, 7
    ONION(FarmlandTileEntity.NutrientType.NITROGEN, self -> RootCropBlock.create(props(), 7, self), self -> new DeadCropBlock(props())), // Default, Root, 7
    SOYBEAN(FarmlandTileEntity.NutrientType.NITROGEN, self -> DefaultCropBlock.create(props(), 7, self), self -> new DeadCropBlock(props())), // Default, 7
    SQUASH(FarmlandTileEntity.NutrientType.POTASSIUM, self -> DefaultCropBlock.create(props(), 8, self), self -> new DeadCropBlock(props())), // Default , 8
    SUGARCANE(FarmlandTileEntity.NutrientType.POTASSIUM, self -> DoubleCropBlock.create(props(), 4, 4, self), self -> new DoubleDeadCropBlock(props())), // Double, 4 -> 4
    TOMATO(FarmlandTileEntity.NutrientType.POTASSIUM, self -> ClimbingCropBlock.create(props(), 4, 4, self), self -> new ClimbingDeadCropBlock(props())), // Double, Stick, Pickable, 4 -> 4
    //BELL_PEPPER(), // Default, Pickable, Multiple Grown Stages, ???
    JUTE(FarmlandTileEntity.NutrientType.POTASSIUM, self -> DoubleCropBlock.create(props(), 2, 4, self), self -> new DoubleDeadCropBlock(props())); // Double, 2 -> 4

    private static AbstractBlock.Properties props()
    {
        return AbstractBlock.Properties.of(Material.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP);
    }

    private final FarmlandTileEntity.NutrientType primaryNutrient;
    private final Supplier<Block> factory;
    private final Supplier<Block> deadFactory;

    Crop(FarmlandTileEntity.NutrientType primaryNutrient, Function<Crop, Block> factory, Function<Crop, Block> deadFactory)
    {
        this.primaryNutrient = primaryNutrient;
        this.factory = () -> factory.apply(this);
        this.deadFactory = () -> deadFactory.apply(this);
    }

    public Block create()
    {
        return factory.get();
    }

    public Block createDead()
    {
        return deadFactory.get();
    }

    public FarmlandTileEntity.NutrientType getPrimaryNutrient()
    {
        return primaryNutrient;
    }
}
