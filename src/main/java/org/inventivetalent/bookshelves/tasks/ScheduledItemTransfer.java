package org.inventivetalent.bookshelves.tasks;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.bookshelves.Bookshelves;

public class ScheduledItemTransfer extends BukkitRunnable {
    private final Inventory source;
    private final Inventory destination;
    private final ItemStack itemStack;

    public ScheduledItemTransfer(Inventory source, Inventory destination, ItemStack itemStack, int seconds) {
        this.source = source;
        this.destination = destination;
        this.itemStack = itemStack;

        runTaskLater(Bookshelves.instance, 20L * seconds);
    }

    @Override
    public void run() {
        if (!source.contains(itemStack)) {
            return;
        }
        if (destination.firstEmpty() <= -1) {
            return;
        }

        if (itemStack.getAmount() <= 2) {
            source.remove(itemStack);
            destination.addItem(itemStack);
            return;
        }

        ItemStack partialStack = itemStack.clone();
        partialStack.setAmount(2);
        itemStack.setAmount(itemStack.getAmount() - 2);
        destination.addItem(partialStack);
    }

}
