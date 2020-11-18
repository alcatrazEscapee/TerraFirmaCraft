package net.dries007.tfc.world.layer.polyhedral;

import net.minecraft.util.FastRandom;

import net.dries007.tfc.world.layer.polyhedral.traits.IPolyAreaTransformer0;
import net.dries007.tfc.world.layer.polyhedral.traits.PolyAreaLayerContext;
import net.dries007.tfc.world.noise.NoiseUtil;
import net.dries007.tfc.world.noise.Vec3;

public class LocalWaterRegionLayer implements IPolyAreaTransformer0
{
    private final int searchRadius = 1;
    private final int normalizeFactor = 1;
    private final long seed;

    public LocalWaterRegionLayer(long seed)
    {
        this.seed = seed;
    }


    @Override
    public int apply(PolyAreaLayerContext context, int x, int y, int z)
    {
        float fx = x * 0.02f;
        float fy = y * 0.06f;
        float fz = z * 0.02f;

        int startX = NoiseUtil.fastFloor(fx);
        int startY = NoiseUtil.fastFloor(fy);
        int startZ = NoiseUtil.fastFloor(fz);

        float centerY = 0;
        float waterLevelY = 0;
        float distance = Float.MAX_VALUE;
        int positionHash = 0;

        for (int cellX = startX - searchRadius; cellX <= startX + searchRadius; cellX++)
        {
            for (int cellY = startY - searchRadius; cellY <= startY + searchRadius; cellY++)
            {
                for (int cellZ = startZ - searchRadius; cellZ <= startZ + searchRadius; cellZ++)
                {
                    Vec3 center = NoiseUtil.CELL_3D[NoiseUtil.hash(seed, cellX, cellY, cellZ) & 255];
                    float vecX = cellX - fx + center.x * normalizeFactor;
                    float vecY = cellY - fy + center.y * normalizeFactor;
                    float vecZ = cellZ - fz + center.z * normalizeFactor;
                    float newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ;
                    if (newDistance < distance)
                    {
                        distance = newDistance;
                        centerY = cellY + center.y * normalizeFactor;

                        long waterLevelSeed = FastRandom.next(seed, cellX);
                        waterLevelSeed = FastRandom.next(waterLevelSeed, cellY);
                        waterLevelSeed = FastRandom.next(waterLevelSeed, cellZ);
                        waterLevelSeed = FastRandom.next(waterLevelSeed, cellX);
                        waterLevelSeed = FastRandom.next(waterLevelSeed, cellY);
                        waterLevelSeed = FastRandom.next(waterLevelSeed, cellZ);
                        waterLevelY = (((int) waterLevelSeed) / 2147483648f) * 0.7f - 0.3f; // Bias towards no water

                        positionHash = ((cellX & 0xFF) << 2) | ((cellY & 0xFF) << 10) | ((cellZ & 0xFF) << 18);
                    }
                }
            }
        }

        float localWaterY = centerY + waterLevelY;
        if (fy < localWaterY)
        {
            return LocalWaterLevelLayerUtil.WATER | positionHash;
        }
        else if (fy < localWaterY + 0.1f)
        {
            return LocalWaterLevelLayerUtil.NEAR_WATER | positionHash;
        }
        else
        {
            return LocalWaterLevelLayerUtil.AIR | positionHash;
        }
    }
}
