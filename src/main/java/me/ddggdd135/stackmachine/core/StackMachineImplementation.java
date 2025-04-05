package me.ddggdd135.stackmachine.core;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.mooy1.infinityexpansion.infinitylib.machines.AbstractMachineBlock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricGoldPan;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GoldPan;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import io.ncbpfluffybear.slimecustomizer.objects.CustomMaterialGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.guguslimefunlib.api.abstracts.TickingBlock;
import me.ddggdd135.guguslimefunlib.api.interfaces.InventoryBlock;
import me.ddggdd135.stackmachine.StackMachine;
import me.ddggdd135.stackmachine.utils.ItemUtils;
import me.ddggdd135.stackmachine.utils.RecipeUtils;
import me.ddggdd135.stackmachine.utils.ReflectionUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class StackMachineImplementation extends TickingBlock
        implements EnergyNetComponent, InventoryBlock, MachineProcessHolder<CustomCraftingOperation> {
    private final GoldPan goldPan = SlimefunItems.GOLD_PAN.getItem(GoldPan.class);
    private final GoldPan netherGoldPan = SlimefunItems.NETHER_GOLD_PAN.getItem(GoldPan.class);
    private static final int[] BORDER = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final int[] BORDER_MACHINE = {12, 14, 21, 22, 23};
    private final MachineProcessor<CustomCraftingOperation> processor = new MachineProcessor<>(this);
    private final Map<Location, ItemStack> machineCache = new HashMap<>();
    private final Map<Location, Integer> energyCache = new HashMap<>();

    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    protected void tick(
            @Nonnull Block block, @Nonnull SlimefunItem slimefunItem, @Nonnull SlimefunBlockData slimefunBlockData) {
        BlockMenu blockMenu = StorageCacheUtils.getMenu(block.getLocation());
        ItemStack machineItem = blockMenu.getInventory().getItem(13);
        blockMenu.replaceExistingItem(31, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
        if (machineItem == null || machineItem.getType().isAir()) return;
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        if (machine == null) return;
        List<MachineRecipe> recipes = new ArrayList<>();
        try {
            if (machine instanceof ElectricGoldPan) {
                MachineRecipe recipe = findNextGoldPanRecipe(blockMenu);
                if (recipe != null) recipes.add(recipe);
            } else {
                recipes = RecipeUtils.getRecipes(machine, block);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return;
        }

        MachineRecipe recipeCache = null;

        int ticks = machineItem.getAmount();
        energyCache.put(block.getLocation(), getEnergyOnce(block));

        while (ticks > 0) {
            if (machineCache.containsKey(block.getLocation())
                    && !SlimefunUtils.isItemSimilar(machineCache.get(block.getLocation()), machineItem, false)) {
                processor.endOperation(block);
                blockMenu.replaceExistingItem(40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
            machineCache.put(block.getLocation(), machineItem);

            CustomCraftingOperation currentOperation = processor.getOperation(block);

            if (currentOperation == null) {
                ticks--;
                MachineRecipe next = recipeCache;
                if (next == null || !(InvUtils.fitAll(blockMenu.toInventory(), next.getOutput(), getOutputSlots()))) {
                    next = findNextRecipe(blockMenu, recipes);
                    if (next == null) break;
                    recipeCache = next;
                }
                if (!ItemUtils.canTake(blockMenu, getInputSlots(), next.getInput())) {
                    break;
                }
                ItemUtils.takeItem(blockMenu, getInputSlots(), next.getInput());
                currentOperation = new CustomCraftingOperation(next);
                processor.startOperation(block, currentOperation);
                if (ticks <= 0) break;
            }

            if (takeCharge(block)) {
                if (!currentOperation.isFinished()) {
                    if (ticks > currentOperation.getTotalTicks()) {
                        currentOperation.addProgress(currentOperation.getTotalTicks());
                        ticks -= currentOperation.getTotalTicks();
                    } else {
                        currentOperation.addProgress(ticks);
                        ticks = 0;
                    }
                } else {
                    ticks--;
                    for (ItemStack output : currentOperation.getResults()) {
                        blockMenu.pushItem(output.clone(), getOutputSlots());
                    }
                    processor.endOperation(block);
                }
            } else {
                blockMenu.replaceExistingItem(31, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&c无电力"));
                return;
            }
        }

        CustomCraftingOperation currentOperation = processor.getOperation(block);
        if (currentOperation == null || currentOperation.isFinished()) {
            blockMenu.replaceExistingItem(40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
            return;
        }
        blockMenu.replaceExistingItem(
                31,
                new CustomItemStack(
                        Material.IRON_PICKAXE,
                        "&a工作中 耗电量: " + getEnergyPerTick(block) + "/slimefun tick &e" + machineItem.getAmount()
                                + "倍速"));
        processor.updateProgressBar(blockMenu, 40, currentOperation);
    }

    public StackMachineImplementation(
            ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        createPreset(this);

        addItemHandler(onBlockBreak());

        processor.setProgressBar(new ItemStack(Material.IRON_PICKAXE));
    }

    @Nonnull
    private BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                    inv.dropItems(b.getLocation(), 13);
                }

                machineCache.remove(b.getLocation().toString());
            }
        };
    }

    @Override
    public void init(@Nonnull BlockMenuPreset preset) {
        for (int i : BORDER) {
            preset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addMenuClickHandler(13, new ChestMenu.AdvancedMenuClickHandler() {

            @Override
            public boolean onClick(
                    InventoryClickEvent inventoryClickEvent,
                    Player p,
                    int i,
                    ItemStack cursor,
                    ClickAction clickAction) {
                if (cursor == null || cursor.getType().isAir()) return true;
                SlimefunItem item = SlimefunItem.getByItem(cursor);
                if (item != null) {
                    return RecipeUtils.IsSupport(item);
                }

                return false;
            }

            @Override
            public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                if (cursor == null || cursor.getType().isAir()) return true;
                SlimefunItem item = SlimefunItem.getByItem(cursor);
                if (item != null) {
                    return RecipeUtils.IsSupport(item);
                }

                return false;
            }
        });

        for (int i : BORDER_MACHINE) {
            preset.addItem(
                    i,
                    new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, "&f机器槽位"),
                    ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addItem(
                31, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());

        for (int i : getOutputSlots()) {
            preset.addMenuClickHandler(i, new ChestMenu.AdvancedMenuClickHandler() {

                @Override
                public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                    return false;
                }

                @Override
                public boolean onClick(
                        InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                    if (cursor == null) return true;
                    cursor.getType();
                    return cursor.getType().isAir();
                }
            });
        }
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return 4480000;
    }

    @Override
    public int[] getInputSlots() {
        return new int[] {10, 11, 19, 20, 28, 29, 37, 38, 30, 39};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] {15, 16, 24, 25, 32, 33, 34, 41, 42, 43};
    }

    @Override
    public void newInstance(@Nonnull BlockMenu blockMenu, @Nonnull Block block) {}

    private int getEnergyPerTick(@Nonnull Block block) {
        BlockMenu inv = StorageCacheUtils.getMenu(block.getLocation());
        ItemStack machineItem = inv.getInventory().getItem(13);
        if (machineItem == null || machineItem.getType().isAir()) return 0;
        return getEnergyOnce(block) * machineItem.getAmount();
    }

    private int getEnergyOnce(@Nonnull Block block) {
        BlockMenu inv = StorageCacheUtils.getMenu(block.getLocation());
        ItemStack machineItem = inv.getInventory().getItem(13);
        if (machineItem == null || machineItem.getType().isAir()) return 0;
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        try {
            if (machine instanceof AContainer aContainer) return aContainer.getEnergyConsumption();
            else if (StackMachine.getInstance().InfinityExpansionSupport
                    && machine instanceof AbstractMachineBlock machineBlock) {
                return ReflectionUtils.getField(machineBlock, "energyPerTick");
            } else if (StackMachine.getInstance().SlimeCustomizerSupport
                    && machine instanceof CustomMaterialGenerator customMaterialGenerator) {
                return ReflectionUtils.getField(customMaterialGenerator, "energyPerTick");
            } else if (StackMachine.getInstance().RykenSlimefunCustomizerSupport
                    && machine
                            instanceof
                            org.lins.mmmjjkx.rykenslimefuncustomizer.objects.customs.machine.CustomMaterialGenerator
                            customMaterialGenerator) {
                return ReflectionUtils.getField(customMaterialGenerator, "per");
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return 32;
    }

    private boolean takeCharge(@Nonnull Block block) {
        int charge = getCharge(block.getLocation());
        int energyOnce = energyCache.get(block.getLocation());
        if (charge < energyOnce) return false;
        setCharge(block.getLocation(), charge - energyOnce);
        return true;
    }

    @Nullable private MachineRecipe findNextRecipe(@Nonnull BlockMenu blockMenu, @Nonnull List<MachineRecipe> recipes) {
        Map<Integer, ItemStack> inventory = new HashMap<>();

        for (int slot : getInputSlots()) {
            ItemStack item = blockMenu.getItemInSlot(slot);

            if (item != null) {
                inventory.put(slot, ItemStackWrapper.wrap(item));
            }
        }

        Map<Integer, Integer> found = new HashMap<>();

        for (MachineRecipe recipe : recipes) {
            for (ItemStack input : recipe.getInput()) {
                for (int slot : getInputSlots()) {
                    if (SlimefunUtils.isItemSimilar(inventory.get(slot), input, true)) {
                        found.put(slot, input.getAmount());
                        break;
                    }
                }
            }

            if (found.size() == recipe.getInput().length) {
                if (!InvUtils.fitAll(blockMenu.toInventory(), recipe.getOutput(), getOutputSlots())) {
                    return null;
                }

                return recipe;
            } else {
                found.clear();
            }
        }

        return null;
    }

    @Nullable private MachineRecipe findNextGoldPanRecipe(@Nonnull BlockMenu blockMenu) {
        ItemStack machineItem = blockMenu.getInventory().getItem(13);
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        ElectricGoldPan electricGoldPan = (ElectricGoldPan) machine;
        for (int slot : getInputSlots()) {
            ItemStack item = blockMenu.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) continue;
            MachineRecipe recipe = null;
            ItemStack output = null;
            ItemStack input = item.clone();
            input.setAmount(1);

            if (goldPan.isValidInput(item)) {
                output = goldPan.getRandomOutput();
                recipe = new MachineRecipe(
                        3 / electricGoldPan.getSpeed(), new ItemStack[] {input}, new ItemStack[] {output});
            } else if (netherGoldPan.isValidInput(item)) {
                output = netherGoldPan.getRandomOutput();
                recipe = new MachineRecipe(
                        4 / electricGoldPan.getSpeed(), new ItemStack[] {input}, new ItemStack[] {output});
            }

            if (output != null && output.getType() != Material.AIR && blockMenu.fits(output, getOutputSlots())) {
                return recipe;
            }
        }

        return null;
    }

    @Nonnull
    @Override
    public MachineProcessor<CustomCraftingOperation> getMachineProcessor() {
        return processor;
    }
}
