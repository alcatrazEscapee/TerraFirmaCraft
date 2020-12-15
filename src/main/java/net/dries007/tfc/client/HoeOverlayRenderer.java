package net.dries007.tfc.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.soil.TFCFarmlandBlock;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;
import net.dries007.tfc.util.Helpers;

public class HoeOverlayRenderer
{
    public static boolean render(Minecraft minecraft, MainWindow window, MatrixStack stack, ItemStack heldStack)
    {
        // Only render when holding a hoe
        if (TFCTags.Items.HOES.contains(heldStack.getItem()))
        {
            final IWorld world = minecraft.level;
            final BlockPos targetedPos = ClientHelpers.getTargetedPos();
            if (world != null && targetedPos != null)
            {
                final BlockState targetedState = world.getBlockState(targetedPos);
                if (targetedState.getBlock() instanceof IHoeOverlayBlock)
                {
                    final List<ITextComponent> lines = new ArrayList<>();
                    ((IHoeOverlayBlock) targetedState.getBlock()).addHoeOverlayInfo(world, targetedPos, targetedState, lines);
                    if (!lines.isEmpty())
                    {
                        int x = window.getGuiScaledWidth() / 2 + 3;
                        int y = window.getGuiScaledHeight() / 2 + 8;
                        for (ITextComponent line : lines)
                        {
                            drawCenteredText(minecraft, stack, line, x, y);
                            y += 12;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private static void drawCenteredText(Minecraft minecraft, MatrixStack stack, ITextComponent text, int x, int y)
    {
        int textWidth = minecraft.font.width(text);
        minecraft.font.draw(stack, text, x - (textWidth / 2), y, 0xCCCCCC);
    }
}
