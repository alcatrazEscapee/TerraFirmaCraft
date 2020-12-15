package net.dries007.tfc.common.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.soil.TFCFarmlandBlock;
import net.dries007.tfc.common.types.Fertilizer;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.chunkdata.ChunkData;

public class FarmlandTileEntity extends TFCTileEntity
{
    // Nutrients
    private float nitrogen; // N
    private float phosphorous; // P
    private float potassium; // K

    public FarmlandTileEntity()
    {
        this(TFCTileEntities.FARMLAND.get());
    }

    protected FarmlandTileEntity(TileEntityType<?> type)
    {
        super(type);

        nitrogen = phosphorous = potassium = 0;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt)
    {
        nitrogen = nbt.getFloat("nitrogen");
        phosphorous = nbt.getFloat("phosphorous");
        potassium = nbt.getFloat("potassium");

        super.load(state, nbt);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt)
    {
        nbt.putFloat("nitrogen", nitrogen);
        nbt.putFloat("phosphorous", phosphorous);
        nbt.putFloat("potassium", potassium);

        return super.save(nbt);
    }

    public float getNutrient(NutrientType type)
    {
        switch (type)
        {
            case NITROGEN:
                return nitrogen;
            case PHOSPHOROUS:
                return phosphorous;
            case POTASSIUM:
                return potassium;
        }
        throw new IllegalArgumentException("Unknown nutrient type: " + type);
    }

    public void addNutrients(Fertilizer fertilizer)
    {
        addNutrients(fertilizer.getNitrogen(), fertilizer.getPhosphorus(), fertilizer.getPotassium());
    }

    public void addNutrients(float nitrogen, float phosphorous, float potassium)
    {
        this.nitrogen = Math.min(nitrogen, 1);
        this.phosphorous = Math.min(phosphorous, 1);
        this.potassium = Math.min(potassium, 1);
    }

    /**
     * Consumes all nutrients, up to the provided amount, and returns the total nutrients (across all types) consumed.
     * @param amount An amount to consume up to, between [0, 1]
     * @return The sum of all consumed nutrients
     */
    public float consumeAll(float amount)
    {
        float deltaN = Math.min(nitrogen, amount);
        float deltaP = Math.min(phosphorous, amount);
        float deltaK = Math.min(potassium, amount);

        nitrogen -= deltaN;
        phosphorous -= deltaP;
        potassium -= deltaK;

        return deltaN + deltaP + deltaK;
    }

    public float getNitrogen()
    {
        return nitrogen;
    }

    public void setNitrogen(float nitrogen)
    {
        this.nitrogen = nitrogen;
    }

    public float getPhosphorous()
    {
        return phosphorous;
    }

    public void setPhosphorous(float phosphorous)
    {
        this.phosphorous = phosphorous;
    }

    public float getPotassium()
    {
        return potassium;
    }

    public void setPotassium(float potassium)
    {
        this.potassium = potassium;
    }

    public enum NutrientType
    {
        NITROGEN, PHOSPHOROUS, POTASSIUM
    }

}
