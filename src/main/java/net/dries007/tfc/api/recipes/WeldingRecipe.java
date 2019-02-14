/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.api.recipes;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

import net.dries007.tfc.api.types.Metal;

/**
 * Welding Recipe
 *
 * This takes two items and produces a single item out
 */
@ParametersAreNonnullByDefault
public class WeldingRecipe extends IForgeRegistryEntry.Impl<WeldingRecipe>
{
    private final Metal.Tier tier;
    private final ItemStack input1;
    private final ItemStack input2;
    private final ItemStack output;

    public WeldingRecipe(ResourceLocation name, ItemStack input1, ItemStack input2, ItemStack output, Metal.Tier tier)
    {
        this.input1 = input1;
        this.input2 = input2;
        this.output = output;
        if (input1.isEmpty() || input2.isEmpty() || output.isEmpty())
            throw new IllegalArgumentException("Input and output are not allowed to be empty");

        this.tier = tier;

        setRegistryName(name);
    }

    @Nonnull
    public Metal.Tier getTier()
    {
        return tier;
    }

    @Nonnull
    public ItemStack getOutput()
    {
        return output.copy();
    }

    public boolean matches(ItemStack input1, ItemStack input2)
    {
        // Need to check both orientations
        return (this.input1.isItemEqual(input1) && this.input2.isItemEqual(input2)) || (this.input1.isItemEqual(input2) && this.input2.isItemEqual(input1));
    }

}
