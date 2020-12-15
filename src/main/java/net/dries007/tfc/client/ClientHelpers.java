/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.client;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Client side methods for proxy use
 */
public class ClientHelpers
{
    @Nullable
    public static World getWorld()
    {
        return Minecraft.getInstance().level;
    }

    @Nullable
    public static BlockPos getTargetedPos()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
        {
            RayTraceResult res = mc.hitResult;
            if (res instanceof BlockRayTraceResult)
            {
                return ((BlockRayTraceResult) res).getBlockPos();
            }
        }
        return null;
    }
}