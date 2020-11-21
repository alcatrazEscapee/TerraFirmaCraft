package net.dries007.tfc.mixin.world.chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.chunk.ChunkPrimer;

import net.dries007.tfc.world.chunk.IExtendedChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Extend {@link net.minecraft.world.chunk.ChunkPrimer} to allow for additional data to be generated, and saved during world gen
 */
@Mixin(ChunkPrimer.class)
public class ChunkPrimerMixin implements IExtendedChunk
{
    @Unique
    private IExtendedChunk.Info extendedInfo;

    @Nullable
    @Override
    public IExtendedChunk.Info getExtendedInfo()
    {
        return extendedInfo;
    }

    @Override
    public void setExtendedInfo(@Nonnull IExtendedChunk.Info extendedInfo)
    {
        this.extendedInfo = extendedInfo;
    }
}
