package net.dries007.tfc.common.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;

import net.dries007.tfc.util.tracker.WorldTrackerCapability;

/**
 * A tile entity that tracks ticks. It uses {@link ICalendarTickable} to update post time deltas. Otherwise it can be manually controlled by getting and setting the last tick
 */
public class TickCounterTileEntity extends TFCTileEntity implements ICalendarTickable, ITickableTileEntity
{
    private long lastTick;

    protected TickCounterTileEntity(TileEntityType<?> type)
    {
        super(type);
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

    @Override
    public boolean onTickDelta(long playerTickDelta)
    {
        return true;
    }

    @Override
    public long getLastTick()
    {
        return lastTick;
    }

    @Override
    public void setLastTick(long tick)
    {
        this.lastTick = tick;
        markDirtyFast();
    }

    @Override
    public void tick()
    {
        ICalendarTickable.super.checkForTickDelta();
    }
}
