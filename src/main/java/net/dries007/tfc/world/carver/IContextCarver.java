package net.dries007.tfc.world.carver;

import java.util.BitSet;
import javax.annotation.Nullable;

import net.dries007.tfc.world.chunk.RockData;

/**
 * Marker interface for carvers which need additional context
 * Carvers may require this to be called (and thus require TFC-compatible chunk generators) or throw errors upon generation.
 */
public interface IContextCarver
{
    /**
     * @param waterAdjacencyMask The adjacency mask for nearby water. This should be null during air carving, and non-null during liquid carving.
     */
    void setContext(long worldSeed, BitSet airCarvingMask, BitSet liquidCarvingMask, RockData rockData, @Nullable BitSet waterAdjacencyMask);
}
