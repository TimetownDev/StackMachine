package me.ddggdd135.stackmachine.core;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.infinitylib.machines.AbstractMachineBlock;
import io.github.mooy1.infinityexpansion.infinitylib.machines.MachineBlock;
import io.github.mooy1.infinityexpansion.items.machines.MaterialGenerator;
import io.github.mooy1.infinityexpansion.items.quarries.Quarry;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.stackmachine.utils.RecipeUtils;
import me.ddggdd135.stackmachine.utils.ReflectionUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class NetworkStackMachine extends NetworkObject
        implements EnergyNetComponent, InventoryBlock, MachineProcessHolder<CustomCraftingOperation> {
    private static final boolean QUARRY_ALLOW_NETHER_IN_OVERWORLD =
            InfinityExpansion.config().getBoolean("quarry-options.output-nether-materials-in-overworld");
    private static final int QUARRY_INTERVAL =
            InfinityExpansion.config().getInt("quarry-options.ticks-per-output", 1, 100);
    private final GoldPan goldPan = SlimefunItems.GOLD_PAN.getItem(GoldPan.class);
    private final GoldPan netherGoldPan = SlimefunItems.NETHER_GOLD_PAN.getItem(GoldPan.class);
    private static final int[] BORDER = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 17, 18, 19, 20, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final int[] BORDER_MACHINE = {12, 14, 21, 22, 23};
    private final MachineProcessor<CustomCraftingOperation> processor = new MachineProcessor<>(this);
    private final Map<String, ItemStack> machineCache = new HashMap<>(); // <location, ItemStack>

    public NetworkStackMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.BRIDGE);

        createPreset(this, "&b堆叠机器", this::constructMenu);

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

    protected void constructMenu(BlockMenuPreset preset) {
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
                return item instanceof MachineBlock
                        || item instanceof AContainer
                        || item instanceof MaterialGenerator
                        || item instanceof Quarry
                        || item instanceof CustomMaterialGenerator;
            }

            @Override
            public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                if (cursor == null || cursor.getType().isAir()) return true;
                SlimefunItem item = SlimefunItem.getByItem(cursor);
                return item instanceof MachineBlock
                        || item instanceof AContainer
                        || item instanceof MaterialGenerator
                        || item instanceof Quarry
                        || item instanceof CustomMaterialGenerator;
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
        return new int[] {28, 29, 37, 38, 30, 39};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] {15, 16, 24, 25, 32, 33, 34, 41, 42, 43};
    }

    @Override
    public void createPreset(SlimefunItem item, Consumer<BlockMenuPreset> setup) {
        InventoryBlock.super.createPreset(item, setup);
    }

    @Override
    public void createPreset(SlimefunItem item, String title, Consumer<BlockMenuPreset> setup) {
        InventoryBlock.super.createPreset(item, title, setup);
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                addToRegistry(b);

                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                pushNetwork(inv, getOutputSlots());
                ItemStack item = inv.getInventory().getItem(13);
                inv.replaceExistingItem(31, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
                if (item == null || item.getType().isAir()) return;

                for (int i = 0; i < item.getAmount(); i++) if (!NetworkStackMachine.this.tick(b)) return;
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
    }

    private int getEnergyPerTick(Block b) {
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        ItemStack machineItem = inv.getInventory().getItem(13);
        if (machineItem == null || machineItem.getType().isAir()) return 0;
        return getEnergyOnce(b) * machineItem.getAmount();
    }

    private int getEnergyOnce(Block b) {
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        ItemStack machineItem = inv.getInventory().getItem(13);
        if (machineItem == null || machineItem.getType().isAir()) return 0;
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        try {
            if (machine instanceof AContainer aContainer) return aContainer.getEnergyConsumption();
            else if (machine instanceof AbstractMachineBlock machineBlock) {
                return ReflectionUtils.getField(machineBlock, "energyPerTick");
            } else if (machine instanceof CustomMaterialGenerator customMaterialGenerator) {
                return ReflectionUtils.getField(customMaterialGenerator, "energyPerTick");
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return 32;
    }

    protected boolean tick(Block b) {
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(inv.getLocation());

        if (definition == null || definition.getNode() == null) {
            return false;
        }
        ItemStack machineItem = inv.getInventory().getItem(13);
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        List<MachineRecipe> recipes = new ArrayList<>();
        try {
            if (machine instanceof ElectricGoldPan) {
                MachineRecipe recipe = findNextGoldPanRecipe(inv);
                if (recipe != null) recipes.add(recipe);
            } else if (machine instanceof AContainer aContainer) {
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
                        ItemStack item = itemById(s);
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
                            && b.getWorld().getEnvironment() != World.Environment.NETHER
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
                recipes.add(RecipeUtils.createRecipe(tickRate, new ItemStack[0], new ItemStack[] {output}));
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        String location = b.getLocation().toString();
        if (machineCache.containsKey(location)
                && !SlimefunUtils.isItemSimilar(machineCache.get(location), machineItem, false)) {
            processor.endOperation(b);
            inv.replaceExistingItem(40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        machineCache.put(location, machineItem);

        CustomCraftingOperation currentOperation = processor.getOperation(b);

        if (currentOperation != null) {
            if (takeCharge(b)) {
                if (!currentOperation.isFinished()) {
                    currentOperation.addProgress(1);
                    processor.updateProgressBar(inv, 40, currentOperation);
                } else {
                    for (ItemStack output : currentOperation.getResults()) {
                        inv.pushItem(output.clone(), getOutputSlots());
                    }
                    processor.endOperation(b);
                    inv.replaceExistingItem(40, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
                    return tick(b);
                }
                inv.replaceExistingItem(
                        31,
                        new CustomItemStack(
                                Material.IRON_PICKAXE,
                                "&a工作中 耗电量: " + getEnergyPerTick(b) + "/slimefun tick &e" + machineItem.getAmount()
                                        + "倍速"));
                return true;
            } else {
                inv.replaceExistingItem(31, new CustomItemStack(Material.RED_STAINED_GLASS_PANE, "&c无电力"));
                return false;
            }
        } else {
            MachineRecipe next = findNextRecipe(inv, recipes);
            if (next != null) {
                if (!canTake(inv, getInputSlots(), next.getInput())) return false;
                takeItem(inv, getInputSlots(), next.getInput());
                currentOperation = new CustomCraftingOperation(next);
                processor.startOperation(b, currentOperation);
                processor.updateProgressBar(inv, 40, currentOperation);
                return tick(b);
            }
            return false;
        }
    }

    private boolean takeCharge(Block b) {
        int charge = getCharge(b.getLocation());
        int energyOnce = getEnergyOnce(b);
        if (charge < energyOnce) return false;
        setCharge(b.getLocation(), charge - energyOnce);
        return true;
    }

    @Nullable public static String getId(ItemStack item) {
        if (item instanceof SlimefunItemStack) {
            return ((SlimefunItemStack) item).getItemId();
        } else {
            return item.hasItemMeta() ? getId(item.getItemMeta()) : null;
        }
    }

    @Nullable public static String getId(ItemMeta meta) {
        return meta.getPersistentDataContainer()
                .get(Slimefun.getItemDataService().getKey(), PersistentDataType.STRING);
    }

    @Nonnull
    private static ItemStack itemById(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        return item == null
                ? new ItemStack(Material.valueOf(id))
                : item.getItem().clone();
    }

    private MachineRecipe findNextRecipe(BlockMenu inv, List<MachineRecipe> recipes) {
        Map<Integer, ItemStack> inventory = new HashMap<>();

        for (int slot : getInputSlots()) {
            ItemStack item = inv.getItemInSlot(slot);

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
                if (!InvUtils.fitAll(inv.toInventory(), recipe.getOutput(), getOutputSlots())) {
                    return null;
                }

                return recipe;
            } else {
                found.clear();
            }
        }

        return null;
    }

    private MachineRecipe findNextGoldPanRecipe(@Nonnull BlockMenu inv) {
        ItemStack machineItem = inv.getInventory().getItem(13);
        SlimefunItem machine = SlimefunItem.getByItem(machineItem);
        ElectricGoldPan electricGoldPan = (ElectricGoldPan) machine;
        for (int slot : getInputSlots()) {
            ItemStack item = inv.getItemInSlot(slot);
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

            if (output != null && output.getType() != Material.AIR && inv.fits(output, getOutputSlots())) {
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

    public void takeItem(BlockMenu inv, int[] slots, ItemStack[] itemStacks) {
        if (!canTake(inv, slots, itemStacks)) throw new RuntimeException("There are no enough items");

        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(inv.getLocation());

        if (definition == null || definition.getNode() == null) throw new RuntimeException("Network Error");

        ItemRequest[] requests = createRequests(itemStacks);
        for (ItemRequest request : requests) definition.getNode().getRoot().getItemStack(request);
    }

    public void pushNetwork(BlockMenu inv, int[] slots) {
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(inv.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }
        NetworkRoot networkRoot = definition.getNode().getRoot();
        for (int slot : slots) {
            ItemStack itemStack = inv.getItemInSlot(slot);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            networkRoot.addItemStack(itemStack);
        }
    }

    public boolean canTake(BlockMenu inv, int[] slots, ItemStack[] itemStacks) {
        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(inv.getLocation());

        if (definition == null || definition.getNode() == null) {
            return false;
        }
        NetworkRoot networkRoot = definition.getNode().getRoot();
        return networkRoot.contains(createRequests(itemStacks));
    }

    public Map<ItemStack, Integer> mergeAmounts(ItemStack[] itemStacks) {
        Map<ItemStack, Integer> amounts = new HashMap<>();
        for (ItemStack itemStack : itemStacks) {
            ItemStack item = null; // 在已拿出中找到的
            for (ItemStack i : amounts.keySet()) {
                if (SlimefunUtils.isItemSimilar(i, item, true, false)) {
                    item = i;
                    break;
                }
            }

            if (item != null) {
                amounts.replace(item, amounts.get(item) + itemStack.getAmount());
            } else {
                ItemStack i = itemStack.clone();
                i.setAmount(1);
                amounts.put(i, itemStack.getAmount());
            }
        }
        return amounts;
    }

    public ItemRequest[] createRequests(ItemStack[] itemStacks) {
        List<ItemRequest> requests = new ArrayList<>();
        Map<ItemStack, Integer> amounts = mergeAmounts(itemStacks);
        for (ItemStack itemStack : amounts.keySet()) {
            if (!itemStack.getType().isAir()) requests.add(new ItemRequest(itemStack, amounts.get(itemStack)));
        }
        return requests.toArray(new ItemRequest[0]);
    }
}
