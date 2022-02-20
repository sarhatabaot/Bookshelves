package org.inventivetalent.bookshelves.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.inventivetalent.bookshelves.Bookshelves;
import org.inventivetalent.bookshelves.utils.HopperUtils;
import org.inventivetalent.itembuilder.ItemBuilder;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.FileReader;


public class ScheduledBookLoading extends BukkitRunnable {
    private final Bookshelves plugin;

    public ScheduledBookLoading(final Bookshelves plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getLogger().info("Loading shelves...");
        try {
            JsonElement jsonElement = new JsonParser().parse(new FileReader(plugin.getShelfFile()));
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (JsonElement next : jsonArray) {
                    if (next.isJsonObject()) {
                        JsonObject jsonObject = next.getAsJsonObject();
                        Location location = Bookshelves.jsonToLocation(jsonObject.get("location").getAsJsonObject());
                        if (location.getBlock().getType() != Material.BOOKSHELF) {
                            continue;
                        }
                        Inventory inventory = plugin.initShelf(location.getBlock());
                        if (inventory == null) {
                            inventory = plugin.getShelf(location.getBlock());
                        }
                        if (inventory == null) {
                            continue;
                        }

                        if (jsonObject.has("books")) {
                            JsonElement bookElement = jsonObject.get("books");
                            if (bookElement.isJsonArray()) {// Old file
                                JsonArray bookArray = bookElement.getAsJsonArray();
                                for (final JsonElement element : bookArray) {

                                    JsonObject nextBook = element.getAsJsonObject();
                                    int slot = nextBook.get("slot").getAsInt();
                                    JsonObject jsonItem = nextBook.get("item").getAsJsonObject();

                                    ConfigurationSection yamlItem = Bookshelves.jsonToYaml(jsonItem, new YamlConfiguration());
                                    ItemStack itemStack = new ItemBuilder(Material.STONE).fromConfig(yamlItem).build();

                                    inventory.setItem(slot, itemStack);
                                }
                            } else {
                                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(bookElement.getAsString()));
                                     BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                                    ItemStack[] stacks = new ItemStack[dataInput.readInt()];

                                    for (int i = 0; i < stacks.length; i++) {
                                        stacks[i] = (ItemStack) dataInput.readObject();
                                    }

                                    inventory.setContents(stacks);
                                }

                            }
                        }
                    }
                }
            }

            if (plugin.isHopperSupport()) HopperUtils.initializeHopperSupport();
        } catch (Exception e) {
            e.printStackTrace();
        }
        plugin.getLogger().info("Loaded " + plugin.getShelves().size() + " shelves.");
    }
}
