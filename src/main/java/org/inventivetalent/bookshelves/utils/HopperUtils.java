package org.inventivetalent.bookshelves.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.inventivetalent.bookshelves.Bookshelves;
import org.inventivetalent.bookshelves.tasks.ScheduledItemTransfer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HopperUtils {
    private HopperUtils() {
        throw new UnsupportedOperationException();
    }

    public static void pull(Block block) {
        Inventory shelf = Bookshelves.instance.getShelf(block);
        List<Hopper> hoppers = getHoppers(block, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
        int seconds = 1;

        if (shelf == null)
            return;

        if (shelf.firstEmpty() > -1) {
            for (Hopper hopper : hoppers) {
                for (ItemStack itemStack : hopper.getInventory().getContents()) {
                    if (!Bookshelves.instance.isValidBook(itemStack)) continue;
                    new ScheduledItemTransfer(hopper.getInventory(), shelf, itemStack, seconds);
                    seconds++;
                }
            }
        }
        scheduleNextCheck(block, seconds + 1);
        push(block);
    }

    public static void push(Block block) {
        List<Hopper> hoppers = getHoppers(block, BlockFace.DOWN).stream().filter(hopper -> !hopper.isLocked() && hopper.getInventory().firstEmpty() > -1).collect(Collectors.toCollection(ArrayList::new));
        if (hoppers.isEmpty())
            return;


        int seconds = 1;
        Hopper hopper = hoppers.get(0);
        Inventory shelf = Bookshelves.instance.getShelf(block);

        if (shelf == null)
            return;

        for (ItemStack itemStack : shelf) {
            if (!Bookshelves.instance.isValidBook(itemStack))
                continue;
            new ScheduledItemTransfer(shelf, hopper.getInventory(), itemStack, seconds);
            seconds++;
        }

    }

    public static @NotNull List<Hopper> getHoppers(Block block, BlockFace @NotNull ... faces) {
        List<Hopper> hoppers = new ArrayList<>();
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.HOPPER) {
                if (face == BlockFace.DOWN || relative.getRelative(((org.bukkit.block.data.type.Hopper) relative.getBlockData()).getFacing()).equals(block)) {
                    hoppers.add((Hopper) relative.getState());
                }
            }
        }
        return hoppers;
    }

    public static void scheduleNextCheck(Block block, int seconds) {
        Bukkit.getScheduler().runTaskLater(Bookshelves.instance, () -> pull(block), 20L * seconds);
    }

    public static void initializeHopperSupport() {
        Bookshelves.instance.getLogger().info("Hopper support enabled!");
        Bookshelves.instance.getShelves().forEach(shelfLocation -> pull(shelfLocation.getBlock()));
    }

}
