package me.ddggdd135.stackmachine.core;

import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.libraries.commons.lang.Validate;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.bukkit.inventory.ItemStack;

public class CustomCraftingOperation implements MachineOperation {
    private final ItemStack[] ingredients;
    private final ItemStack[] results;
    private final int totalTicks;
    private int currentTicks;

    public CustomCraftingOperation(@Nonnull MachineRecipe recipe) {
        this(recipe.getInput(), recipe.getOutput(), recipe.getTicks());
    }

    public CustomCraftingOperation(@Nonnull ItemStack[] ingredients, @Nonnull ItemStack[] results, int totalTicks) {
        this.currentTicks = 0;
        Validate.notEmpty(results, "The results array cannot be empty or null");
        Validate.isTrue(
                totalTicks >= 0,
                "The amount of total ticks must be a positive integer or zero, received: " + totalTicks);
        this.ingredients = ingredients;
        this.results = results;
        this.totalTicks = totalTicks;
    }

    public void addProgress(int num) {
        Validate.isTrue(num > 0, "Progress must be positive.");
        this.currentTicks += num;
    }

    @Nonnull
    public ItemStack[] getIngredients() {
        return this.ingredients;
    }

    @Nonnull
    public ItemStack[] getResults() {
        return this.results;
    }

    public int getProgress() {
        return this.currentTicks;
    }

    public int getTotalTicks() {
        return this.totalTicks;
    }
}
