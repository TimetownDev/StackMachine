package me.ddggdd135.stackmachine.core;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.ddggdd135.stackmachine.StackMachine;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public final class StackMachineItems {
    public static final ItemGroup StackMachineItem = new ItemGroup(
            new NamespacedKey(me.ddggdd135.stackmachine.StackMachine.getInstance(), "stack_machine"),
            new CustomItemStack(Material.FURNACE, "&b堆叠机器", "&e4480000J 可存储", "&e将多个机器放入其中 效率翻倍"));
    public static final SlimefunItemStack StackMachine =
            new SlimefunItemStack("STACKMACHINE", new CustomItemStack(Material.FURNACE, "&b堆叠机器", "&e4480000J 可存储"));

    public static void onSetup(StackMachine plugin) {
        StackMachineImplementation stackMachineImplementation = new StackMachineImplementation(
                StackMachineItem, StackMachine, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    SlimefunItems.ADVANCED_CIRCUIT_BOARD, SlimefunItems.POWER_CRYSTAL,
                            SlimefunItems.ADVANCED_CIRCUIT_BOARD,
                    SlimefunItems.ENHANCED_AUTO_CRAFTER, SlimefunItems.CARGO_MANAGER,
                            SlimefunItems.ENHANCED_AUTO_CRAFTER,
                    SlimefunItems.ANDROID_MEMORY_CORE, SlimefunItems.POWER_CRYSTAL, SlimefunItems.ANDROID_MEMORY_CORE
                });
        stackMachineImplementation.register(plugin);
        StackMachineResearches.Default.addItems(stackMachineImplementation);
        StackMachineResearches.Default.register();
    }
}
