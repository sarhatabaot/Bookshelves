package org.inventivetalent.bookshelves.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.inventivetalent.bookshelves.Bookshelves;
import org.inventivetalent.bookshelves.utils.WorldGuardUtils;
import org.jetbrains.annotations.NotNull;

public class ShelfListener implements Listener {

    private final Bookshelves plugin;

    public ShelfListener(Bookshelves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (plugin.getDisabledWorlds().contains(player.getWorld().getName())) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (block == null) {
            return;
        }
        if (block.getType() != Material.BOOKSHELF) {
            return;
        }

        if (!player.hasPermission("bookshelf.open")) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }
        if (plugin.isWorldGuardSupport() && !WorldGuardUtils.isAllowedToAccess(player, block)) {
            return;
        }

        Inventory inventory = plugin.getShelf(block);
        if (inventory == null) {
            inventory = plugin.initShelf(block);
        }

        event.setCancelled(true);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (plugin.getDisabledWorlds().contains(player.getWorld().getName())) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }
        if (block == null) {
            return;
        }
        if (block.getType() != Material.BOOKSHELF) {
            return;
        }

        if (block.hasMetadata("BOOKSHELF_INVENTORY")) {
            if (!player.hasPermission("bookshelf.break")) {
                event.setCancelled(true);
                return;
            }
            Inventory inventory = plugin.getShelf(block);
            if (inventory == null) {
                return;
            }
            final ItemStack[] contents = inventory.getContents();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() != Material.BOOKSHELF) {
                    for (ItemStack itemStack : contents) {
                        if (itemStack == null) {
                            continue;
                        }
                        block.getWorld().dropItemNaturally(block.getLocation(), itemStack);
                    }
                    block.removeMetadata("BOOKSHELF_INVENTORY", plugin);
                }
            }, 1);
        }
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        HumanEntity player = event.getWhoClicked();
        Inventory inventory = event.getInventory();
        Inventory clicked = event.getClickedInventory();

        if (plugin.getDisabledWorlds().contains(player.getWorld().getName())) {
            return;
        }

        if (clicked != null && plugin.getInventoryTitle().equals(event.getView().getTitle())) {
            if (!player.hasPermission("bookshelf.modify")) {
                event.setCancelled(true);
            }
            ItemStack item = event.getCursor();
            if (item.getType() != Material.AIR && !plugin.isValidBook(item)) {
                event.setCancelled(true);
            }
        }
        if (event.getClick().isShiftClick()) {
            if (inventory != null && plugin.getInventoryTitle().equals(event.getView().getTitle())) {
                if (!player.hasPermission("bookshelf.modify")) {
                    event.setCancelled(true);
                }
                ItemStack item = event.getCurrentItem();
                if (item.getType() != Material.AIR && !plugin.isValidBook(item)) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
