package net.dries007.tfc.common.types;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;

/**
 * Describes an item as having a specific fertilizer (NPK) setup
 */
public class Fertilizer
{
    public static Fertilizer read(ResourceLocation id, JsonObject json)
    {
        Ingredient ingredient = CraftingHelper.getIngredient(JSONUtils.getAsJsonObject(json, "ingredient"));
        float nitrogen = JSONUtils.getAsFloat(json, "nitrogen", 0);
        float phosphorus = JSONUtils.getAsFloat(json, "phosphorus", 0);
        float potassium = JSONUtils.getAsFloat(json, "potassium", 0);
        return new Fertilizer(id, ingredient, nitrogen, phosphorus, potassium);
    }

    protected final Ingredient ingredient;
    protected final float nitrogen, phosphorus, potassium;
    private final ResourceLocation id;

    public Fertilizer(ResourceLocation id, Ingredient ingredient, float nitrogen, float phosphorus, float potassium)
    {
        this.id = id;
        this.ingredient = ingredient;
        this.nitrogen = nitrogen;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
    }

    public Collection<Item> getValidItems()
    {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::getItem).collect(Collectors.toSet());
    }

    public boolean isValid(ItemStack stack)
    {
        return ingredient.test(stack);
    }

    public float getNitrogen()
    {
        return nitrogen;
    }

    public float getPhosphorus()
    {
        return phosphorus;
    }

    public float getPotassium()
    {
        return potassium;
    }

    public ResourceLocation getId()
    {
        return id;
    }
}
