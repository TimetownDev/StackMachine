package me.ddggdd135.stackmachine.utils;

import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.infinitylib.machines.MachineBlock;
import io.github.mooy1.infinityexpansion.items.machines.MaterialGenerator;
import io.github.mooy1.infinityexpansion.items.machines.VoidHarvester;
import io.github.mooy1.infinityexpansion.items.materials.Materials;
import io.github.mooy1.infinityexpansion.items.quarries.Quarry;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.ncbpfluffybear.slimecustomizer.objects.CustomMaterialGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class RecipeUtils {
    private static final boolean QUARRY_ALLOW_NETHER_IN_OVERWORLD =
            InfinityExpansion.config().getBoolean("quarry-options.output-nether-materials-in-overworld");
    private static final int QUARRY_INTERVAL =
            InfinityExpansion.config().getInt("quarry-options.ticks-per-output", 1, 100);

    public static MachineRecipe createRecipe(int ticks, ItemStack[] input, ItemStack[] output) {
        MachineRecipe recipe = new MachineRecipe(1, input, output);
        try {
            ReflectionUtils.setField(recipe, "ticks", ticks);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return recipe;
    }

    @Nonnull
    public static List<MachineRecipe> getRecipes(@Nonnull SlimefunItem machine, @Nonnull Block block)
            throws NoSuchFieldException, IllegalAccessException {
        List<MachineRecipe> recipes = new ArrayList<>();
        if (machine instanceof AContainer aContainer) {
            recipes.addAll(aContainer.getMachineRecipes());
        } else if (machine instanceof MachineBlock machineBlock) {
            List<Object> list = ReflectionUtils.getField(machineBlock, "recipes");
            int ticksPerOutput = ReflectionUtils.getField(machineBlock, "ticksPerOutput");

            for (Object object : list) {
                String[] strings = ReflectionUtils.getField(object, "strings");
                int[] amounts = ReflectionUtils.getField(object, "amounts");
                ItemStack output = ReflectionUtils.getField(object, "output");
                List<ItemStack> in = new ArrayList<>();
                for (int i = 0; i < strings.length; i++) {
                    String s = strings[i];
                    ItemStack item = ItemUtils.itemById(s);
                    item.setAmount(amounts[i]);
                    in.add(item);
                }
                for (int i = 0; i < strings.length; i++) {
                    recipes.add(RecipeUtils.createRecipe(
                            ticksPerOutput, in.toArray(new ItemStack[0]), new ItemStack[] {output}));
                }
            }
        } else if (machine instanceof MaterialGenerator materialGenerator) {
            Material material = ReflectionUtils.getField(materialGenerator, "material");
            int speed = ReflectionUtils.getField(materialGenerator, "speed");
            ItemStack output = new ItemStack(material, speed);
            recipes.add(RecipeUtils.createRecipe(1, new ItemStack[0], new ItemStack[] {output}));
        } else if (machine instanceof Quarry quarry) {
            int speed = ReflectionUtils.getField(quarry, "speed");
            int chance = ReflectionUtils.getField(quarry, "chance");
            Material[] outputs = ReflectionUtils.getField(quarry, "outputs");
            Material outputType = outputs[ThreadLocalRandom.current().nextInt(outputs.length)];
            ItemStack outputItem;
            if (ThreadLocalRandom.current().nextInt(chance) == 0) {
                if (!QUARRY_ALLOW_NETHER_IN_OVERWORLD
                        && block.getWorld().getEnvironment() != World.Environment.NETHER
                        && (outputType == Material.QUARTZ
                                || outputType == Material.NETHERITE_INGOT
                                || outputType == Material.NETHERRACK)) {
                    outputItem = new ItemStack(Material.COBBLESTONE, speed);
                } else {
                    outputItem = new ItemStack(outputType, speed);
                }
            } else {
                outputItem = new ItemStack(Material.COBBLESTONE, speed);
            }
            recipes.add(RecipeUtils.createRecipe(QUARRY_INTERVAL, new ItemStack[0], new ItemStack[] {outputItem}));
        } else if (machine instanceof CustomMaterialGenerator customMaterialGenerator) {
            int tickRate = ReflectionUtils.getField(customMaterialGenerator, "tickRate");
            ItemStack output = ReflectionUtils.getField(customMaterialGenerator, "output");
            recipes.add(RecipeUtils.createRecipe(tickRate - 1, new ItemStack[0], new ItemStack[] {output}));
        } else if (machine instanceof VoidHarvester voidHarvester) {
            int speed = ReflectionUtils.getField(voidHarvester, "speed");
            recipes.add(RecipeUtils.createRecipe(speed, new ItemStack[0], new ItemStack[]{Materials.VOID_BIT}));
        }

        return recipes;
    }
}
