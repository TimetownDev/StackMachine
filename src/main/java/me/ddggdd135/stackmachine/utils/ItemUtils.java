package me.ddggdd135.stackmachine.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.ddggdd135.guguslimefunlib.api.ItemHashMap;
import me.ddggdd135.guguslimefunlib.items.ItemKey;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtils {
    public static void takeItem(@Nonnull BlockMenu blockMenu, @Nonnull int[] slots, @Nonnull ItemStack[] itemStacks) {
        if (!canTake(blockMenu, slots, itemStacks)) throw new RuntimeException("There are no enough items");
        ItemHashMap<Integer> amounts = me.ddggdd135.guguslimefunlib.utils.ItemUtils.getAmounts(itemStacks);

        for (ItemKey itemKey : amounts.sourceKeySet()) {
            for (int slot : slots) {
                ItemStack item = blockMenu.getItemInSlot(slot);
                if (item == null || item.getType().isAir()) continue;
                if (SlimefunUtils.isItemSimilar(item, itemKey.getItemStack(), true, false)) {
                    if (item.getAmount() > amounts.getKey(itemKey)) {
                        item.setAmount(item.getAmount() - amounts.getKey(itemKey));
                        break;
                    } else {
                        blockMenu.replaceExistingItem(slot, null);
                        int rest = amounts.getKey(itemKey) - item.getAmount();
                        if (rest != 0) amounts.putKey(itemKey, rest);
                        else break;
                    }
                }
            }
        }
    }

    public static boolean canTake(@Nonnull BlockMenu blockMenu, @Nonnull int[] slots, @Nonnull ItemStack[] itemStacks) {
        ItemHashMap<Integer> toTake = me.ddggdd135.guguslimefunlib.utils.ItemUtils.getAmounts(itemStacks);

        for (ItemKey itemKey : toTake.sourceKeySet()) {
            if (toTake.getKey(itemKey) > getItemAmount(blockMenu, slots, itemKey.getItemStack())) {
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
