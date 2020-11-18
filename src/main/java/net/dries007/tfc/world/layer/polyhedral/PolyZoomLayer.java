package net.dries007.tfc.world.layer.polyhedral;

import net.dries007.tfc.world.layer.polyhedral.traits.IPolyArea;
import net.dries007.tfc.world.layer.polyhedral.traits.IPolyAreaTransformer1;
import net.dries007.tfc.world.layer.polyhedral.traits.PolyAreaLayerContext;

/**
 * @see net.minecraft.world.gen.layer.ZoomLayer
 */
public enum PolyZoomLayer implements IPolyAreaTransformer1
{
    INSTANCE;

    @Override
    public int apply(PolyAreaLayerContext context, IPolyArea area, int x, int y, int z)
    {
        final int px = getParentX(x);
        final int py = getParentY(y);
        final int pz = getParentZ(z);
        context.initRandom(px, py, pz);
        final int parent = area.get(px, py, pz);
        switch ((x & 1) | ((y & 1) << 1) | ((z & 1) << 2)) // (Z Y X)
        {
            case 0b000:
                return parent;
            case 0b001:
                return choose2(context, parent, area.get(px + 1, py, pz));
            case 0b010:
                return choose2(context, parent, area.get(px, py + 1, pz));
            case 0b011:
                return choose4(context, parent, area.get(px + 1, py, pz), area.get(px, py + 1, pz), area.get(px + 1, py + 1, pz));
            case 0b100:
                return choose2(context, parent, area.get(px, py, pz + 1));
            case 0b101:
                return choose4(context, parent, area.get(px + 1, py, pz), area.get(px, py, pz + 1), area.get(px + 1, py, pz + 1));
            case 0b110:
                return choose4(context, parent, area.get(px, py + 1, pz), area.get(px, py, pz + 1), area.get(px, py + 1, pz + 1));
            case 0b111:
            {
                // This is fancy stupid random because I can't be arsed to write a mode for eight inputs
                int choice = context.nextRandom(8);
                if (choice == 0)
                {
                    return parent;
                }
                return area.get(px + (choice & 1), py + ((choice >> 1) & 1), pz + ((choice >> 2) & 1));
            }
        }
        throw new IllegalStateException("Something went badly wrong");
    }

    @Override
    public int getParentX(int x)
    {
        return x >> 1;
    }

    @Override
    public int getParentY(int y)
    {
        return y >> 1;
    }

    @Override
    public int getParentZ(int z)
    {
        return z >> 1;
    }

    private int choose2(PolyAreaLayerContext context, int a, int b)
    {
        return context.nextRandom(2) == 0 ? a : b;
    }

    private int choose4(PolyAreaLayerContext context, int first, int second, int third, int fourth)
    {
        if (second == third && third == fourth)
        {
            return second;
        }
        else if (first == second && first == third)
        {
            return first;
        }
        else if (first == second && first == fourth)
        {
            return first;
        }
        else if (first == third && first == fourth)
        {
            return first;
        }
        else if (first == second && third != fourth)
        {
            return first;
        }
        else if (first == third && second != fourth)
        {
            return first;
        }
        else if (first == fourth && second != third)
        {
            return first;
        }
        else if (second == third && first != fourth)
        {
            return second;
        }
        else if (second == fourth && first != third)
        {
            return second;
        }
        else if (third == fourth && first != second)
        {
            return third;
        }
        else
        {
            int value = context.nextRandom(4);
            if (value == 0)
            {
                return first;
            }
            else if (value == 1)
            {
                return second;
            }
            else
            {
                return value == 2 ? third : fourth;
            }
        }
    }
}
