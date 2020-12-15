package net.dries007.tfc.common.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;

public class TickCounterTileEntity extends TFCTileEntity
{
    private long lastTick;

    protected TickCounterTileEntity(TileEntityType<?> type)
    {
        super(type);
    }

    public long getLastTick()
    {
        return lastTick;
    }

    public void setLastTick(long lastTick)
    {
        this.lastTick = lastTick;
        markDirtyFast();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt)
    {
        nbt.putLong("lastTick", lastTick);
        return super.save(nbt);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt)
    {
        lastTick = nbt.getLong("lastTick");
        super.load(state, nbt);
    }
}
