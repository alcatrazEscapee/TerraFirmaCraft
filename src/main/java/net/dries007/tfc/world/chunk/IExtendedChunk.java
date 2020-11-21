package net.dries007.tfc.world.chunk;

import javax.annotation.Nullable;

import net.minecraft.world.biome.Biome;

public interface IExtendedChunk
{
    @Nullable
    Info getExtendedInfo();

    void setExtendedInfo(Info info);

    final class Info
    {
        private final Biome[] localBiomes;
        private final int[] surfaceHeightMap;
        private final double[] carvingCenterMap;
        private final double[] carvingHeightMap;

        public Info(Biome[] localBiomes, int[] surfaceHeightMap, double[] carvingCenterMap, double[] carvingHeightMap)
        {
            this.localBiomes = localBiomes;
            this.surfaceHeightMap = surfaceHeightMap;
            this.carvingCenterMap = carvingCenterMap;
            this.carvingHeightMap = carvingHeightMap;
        }

        public int[] getSurfaceHeightMap()
        {
            return surfaceHeightMap;
        }

        public Biome[] getLocalBiomes()
        {
            return localBiomes;
        }

        public double[] getCarvingCenterMap()
        {
            return carvingCenterMap;
        }

        public double[] getCarvingHeightMap()
        {
            return carvingHeightMap;
        }
    }
}
