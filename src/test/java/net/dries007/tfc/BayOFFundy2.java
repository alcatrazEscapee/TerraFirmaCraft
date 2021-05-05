package net.dries007.tfc;

import net.dries007.tfc.world.noise.INoise2D;
import net.dries007.tfc.world.noise.INoise3D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import org.junit.jupiter.api.Test;


public class BayOFFundy2
{
    int SEA_LEVEL = 96;

    @Test
    public void test()
    {
        int rowH = 100, rows = 6, rowW = rows * rowH, rowY = 60;
        Artist.Noise<INoise3D> profile = Artist.<INoise3D>forNoise(noise3 -> Artist.NoisePixel.coerceInt((x, y) -> {
            int my = rowY + rowH - (y % rowH);
            if (y % rowH == 0) return 0.5f;
            int mx = x + (y / rowH) * rowW;
            return noise3.noise(mx, my, 0);
        })).dimensions(rowW).size(rowW);
        Artist.Noise<INoise2D> flat = Artist.<INoise2D>forNoise(noise2 -> Artist.NoisePixel.coerceFloat(noise2::noise)).dimensions(rowW).size(rowW);

        long seed = 1927384917231L;

        INoise2D blobs = new OpenSimplex2D(seed).spread(0.03f).abs();

        float t1 = 0.65f;
        float t2 = 0.5f;

        flat.draw("bof_1", blobs.map(t -> {
            if (t > t1) return 1;
            else if (t > t2) return 0.5f;
            else return 0;
        }));

        INoise2D plateau = new OpenSimplex2D(seed).octaves(4).spread(0.2f).scaled(SEA_LEVEL + 14, SEA_LEVEL + 30);

        final INoise2D warpX = new OpenSimplex2D(seed).octaves(2).spread(0.015f).scaled(-30, 30);
        final INoise2D warpZ = new OpenSimplex2D(seed + 1).octaves(2).spread(0.015f).scaled(-30, 30);
        INoise2D ocean = new OpenSimplex2D(seed + 2).octaves(4).spread(0.11f).warped(warpX, warpZ).scaled(SEA_LEVEL + -16, SEA_LEVEL + -4);

        INoise2D center = new OpenSimplex2D(seed + 9172834791L).scaled(-2, 2);

        flat.draw("bof_2", plateau);
        flat.draw("bof_3", ocean);

        flat.draw("bof_4", (x, z) -> {
            float b = blobs.noise(x, z);
            if (b > t2) return plateau.noise(x, z);
            else return ocean.noise(x, z);
        });

        profile.draw("bof_5", adjust((x, y, z) -> {
            float b = blobs.noise(x, z);
            float p = plateau.noise(x, z);
            float o = ocean.noise(x, z);
            float po = 0.7f * p + 0.3f * o + center.noise(x, z);
            if (b > t1)
            {
                return y > p ? 1 : 0;
            }
            else if (b > t2)
            {
                float t = (b - t2) / (t1 - t2);
                t = 1 - t;
                t *= 1.2f;
                float delta = Math.abs(po - y);
                if (y > p) return 1;
                if (delta < 15 * t) return 1;
                return 0;
            }
            else
            {
                return y > o ? 1 : 0;
            }
        }));
    }

    INoise3D adjust(INoise3D in)
    {
        return (x, y, z) -> {
            float t = in.noise(x, y, z);
            if (t == 1 && y < SEA_LEVEL)
            {
                return 0.7f;
            }
            return t;
        };
    }
}
