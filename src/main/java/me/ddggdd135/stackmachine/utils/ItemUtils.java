package me.ddggdd135.stackmachine.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    public static void takeItem(@Nonnull BlockMenu blockMenu, @Nonnull int[] slots, @Nonnull ItemStack[] itemStacks) {
        if (!canTake(blockMenu, slots, itemStacks)) throw new RuntimeException("There are no enough items");
        Map<ItemStack, Integer> amounts = mergeAmounts(itemStacks);

        for (ItemStack itemStack : amounts.keySet()) {
            for (int slot : slots) {
                ItemStack item = blockMenu.getItemInSlot(slot);
                if (item == null || item.getType().isAir()) continue;
                if (SlimefunUtils.isItemSimilar(item, itemStack, true, false)) {
                    if (item.getAmount() > amounts.get(itemStack)) {
                        item.setAmount(item.getAmount() - amounts.get(itemStack));
                        break;
                    } else {
                        blockMenu.replaceExistingItem(slot, new ItemStack(Material.AIR));
                        int rest = amounts.get(itemStack) - item.getAmount();
                        if (rest != 0) amounts.put(itemStack, rest);
                        else {
                            amounts.remove(itemStack);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static boolean canTake(@Nonnull BlockMenu blockMenu, @Nonnull int[] slots, @Nonnull ItemStack[] itemStacks) {
        Map<ItemStack, Integer> toTake = mergeAmounts(itemStacks);

        for (ItemStack itemStack : toTake.keySet()) {
            if (toTake.get(itemStack) > getItemAmount(blockMenu, slots, itemStack)) {
                return false;
            }
        }
        return true;
    }

    public static int getItemAmount(@Nonnull BlockMenu blockMenu, @Nonnull int[] slots, @Nonnull ItemStack itemStack) {
        int founded = 0;
        for (int slot : slots) {
            ItemStack item = blockMenu.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) continue;
            if (SlimefunUtils.isItemSimilar(item, itemStack, true, false)) {
                founded += item.getAmount();
            }
        }
        return founded;
    }

    public static Map<ItemStack, Integer> mergeAmounts(@Nonnull ItemStack[] itemStacks) {
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

    @Nullable public static String getId(@Nonnull ItemStack item) {
        if (item instanceof SlimefunItemStack) {
            return ((SlimefunItemStack) item).getItemId();
        } else {
            return item.hasItemMeta() ? getId(item.getItemMeta()) : null;
        }
    }

    @Nullable public static String getId(@Nonnull ItemMeta meta) {
        return meta.getPersistentDataContainer()
                .get(Slimefun.getItemDataService().getKey(), PersistentDataType.STRING);
    }

    @Nonnull
    public static ItemStack itemById(@Nonnull String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        return item == null
                ? new ItemStack(Material.valueOf(id))
                : item.getItem().clone();
    }
}
