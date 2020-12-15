package net.dries007.tfc.common.types.managers;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.dries007.tfc.common.types.Fertilizer;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.collections.IndirectHashCollection;
import net.dries007.tfc.util.data.TypedDataManager;

public class FertilizerManager extends TypedDataManager<Fertilizer>
{
    public static final FertilizerManager INSTANCE = new FertilizerManager(new GsonBuilder().create(), "fertilizer", "fertilizer", true);
    private static final IndirectHashCollection<Item, Fertilizer> CACHE = new IndirectHashCollection<>(Fertilizer::getValidItems);
    private static final ResourceLocation DEFAULT = Helpers.identifier("default");

    @Nullable
    public static Fertilizer get(ItemStack stack)
    {
        for (Fertilizer def : CACHE.getAll(stack.getItem()))
        {
            if (def.isValid(stack))
            {
                return def;
            }
        }
        return null;
    }

    public static void reload()
    {
        CACHE.reload(INSTANCE.getValues());
    }

    private FertilizerManager(Gson gson, String domain, String typeName, boolean allowNone)
    {
        super(gson, domain, typeName, allowNone);

        register(DEFAULT, Fertilizer::read);
    }

    @Override
    protected ResourceLocation getFallbackType()
    {
        return super.getFallbackType();
    }
}
