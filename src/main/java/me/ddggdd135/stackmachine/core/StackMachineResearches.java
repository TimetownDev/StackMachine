package me.ddggdd135.stackmachine.core;

import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import me.ddggdd135.stackmachine.StackMachine;
import org.bukkit.NamespacedKey;

public final class StackMachineResearches {
    public static final Research Default =
            new Research(new NamespacedKey(StackMachine.getInstance(), "default_research"), 239182, "&b堆叠机器", 80);
}
