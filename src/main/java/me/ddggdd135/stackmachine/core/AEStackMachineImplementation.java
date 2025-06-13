package me.ddggdd135.stackmachine.core;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricGoldPan;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import me.ddggdd135.guguslimefunlib.api.ItemHashMap;
import me.ddggdd135.slimeae.SlimeAEPlugin;
import me.ddggdd135.slimeae.api.interfaces.IMEObject;
import me.ddggdd135.slimeae.api.interfaces.IStorage;
import me.ddggdd135.slimeae.api.items.ItemRequest;
import me.ddggdd135.slimeae.api.items.ItemStorage;
import me.ddggdd135.slimeae.core.NetworkInfo;
import me.ddggdd135.stackmachine.utils.ItemUtils;
import me.ddggdd135.stackmachine.utils.RecipeUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AEStackMachineImplementation extends StackMachineImplementation implements IMEObject {
    private static final int[] BORDER = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final int[] BORDER_MACHINE = {12, 14, 21, 22, 23};
    private final Map<Location, ItemStorage> tmpStorages = new HashMap<>();

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

        NetworkInfo networkInfo = SlimeAEPlugin.getNetworkData().getNetworkInfo(block.getLocation());
        if (networkInfo == null) return;

        IStorage storage = networkInfo.getStorage();

        int amount = machineItem.getAmount();
        energyCache.put(block.getLocation(), getEnergyOnce(block) * amount);

        if (machineCache.containsKey(block.getLocation())
                && !SlimefunUtils.isItemSimilar(machineCache.get(block.getLocation()), machineItem, false)) {
            processor.endOperation(block);
            blockMenu.replaceExistingItem(40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        machineCache.put(block.getLocation(), machineItem);

        if (takeCharge(block)) {
            CustomCraftingOperation currentOperation = processor.getOperation(block);

            ItemStorage blockStorage = tmpStorages.computeIfAbsent(block.getLocation(), x -> new ItemStorage());

            if (currentOperation == null) {
                if (!blockStorage.getStorageUnsafe().isEmpty()) {
                    storage.pushItem(blockStorage.getStorageUnsafe());
                    me.ddggdd135.slimeae.utils.ItemUtils.trim(blockStorage.getStorageUnsafe());
                    return;
                }

                MachineRecipe next;
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

                next = findNextRecipe(blockMenu, recipes);
                if (next == null) return;

                ItemHashMap<Long> need =
                        ItemUtils.muiItems(me.ddggdd135.slimeae.utils.ItemUtils.getAmounts(next.getInput()), amount);
                ItemRequest[] requests = me.ddggdd135.slimeae.utils.ItemUtils.createRequests(need);

                if (!storage.contains(requests)) return;

                storage.takeItem(requests);
                currentOperation = new CustomCraftingOperation(next);
                processor.startOperation(block, currentOperation);
            } else if (!currentOperation.isFinished()) {
                currentOperation.addProgress(1);
            } else {
                blockStorage.addItem(ItemUtils.muiItems(
                        me.ddggdd135.slimeae.utils.ItemUtils.getAmounts(currentOperation.getResults()), amount));
                processor.endOperation(block);
            }
        } else {
            blockMenu.replaceExistingItem(31, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&c无电力"));
            return;
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

    public AEStackMachineImplementation(
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

    @Override
    public int getCapacity() {
        return 17920000;
    }

    @Override
    public void newInstance(@Nonnull BlockMenu blockMenu, @Nonnull Block block) {
        super.newInstance(blockMenu, block);
    }

    @Override
    public void onNetworkUpdate(Block block, NetworkInfo networkInfo) {}

    @Override
    public void onNetworkTick(Block block, NetworkInfo networkInfo) {}
}
