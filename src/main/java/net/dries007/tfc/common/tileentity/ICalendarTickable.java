package net.dries007.tfc.common.tileentity;

import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;

import net.dries007.tfc.util.calendar.Calendars;

/**
 * This is implemented on TileEntities that need to receive updates whenever the calendar changes drastically
 * Note: the default {@code update()} casts the implementor to {@link TileEntity}
 *
 * @see CalendarTFC#runTransaction(long, long, Runnable)
 */
public interface ICalendarTickable
{
    default TileEntity asTileEntity()
    {
        return (TileEntity) this;
    }

    /**
     * Here we check every tick for a calendar discrepancy. This only checks for differences in player time, and calls {@link ICalendarTickable#onTickDelta(long playerTickDelta)} as necessary
     *
     * This SHOULD be called through {@link ITickableTileEntity#tick()}
     */
    default void checkForTickDelta()
    {
        TileEntity te = asTileEntity();
        if (te.getLevel() != null && !te.getLevel().isClientSide())
        {
            long playerTick = Calendars.SERVER.getTicks();
            long tickDelta = playerTick - getLastTick();
            if (tickDelta > 1 && onTickDelta(tickDelta - 1)) // Expect 1 tick
            {
                setLastTick(playerTick);
            }
        }
    }

    /**
     * Called through {@link ICalendarTickable#checkForTickDelta()} if there's a unusual tick delta (for instance, if the chunk was unloaded for a while, or a command was used to advance time
     *
     * @param playerTickDelta the difference in player ticks observed between last tick and this tick
     * @return If the tick delta was successfully handled.
     */
    boolean onTickDelta(long playerTickDelta);

    /**
     * Gets the last update tick.
     */
    long getLastTick();

    /**
     * Sets the last update tick
     */
    void setLastTick(long tick);
}
