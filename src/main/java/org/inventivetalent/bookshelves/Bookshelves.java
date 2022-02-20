package org.inventivetalent.bookshelves;

import com.google.gson.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.inventivetalent.bookshelves.listeners.ShelfListener;
import org.inventivetalent.bookshelves.manager.RestrictionManager;
import org.inventivetalent.bookshelves.tasks.ScheduleBookLoading;
import org.inventivetalent.bookshelves.utils.AccessUtil;
import org.inventivetalent.bookshelves.utils.HopperUtils;
import org.inventivetalent.bookshelves.utils.MetaHelper;
import org.inventivetalent.bookshelves.utils.WorldGuardUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Bookshelves extends JavaPlugin {
    public static Bookshelves instance;

    int inventorySize = 18;
    private String inventoryTitle = "Bookshelf";
    private Set<String> disabledWorlds = new HashSet<>();
    boolean onlyBooks = true;
    private boolean worldGuardSupport = false;
    boolean checkRestrictions = false;
    private boolean hopperSupport = false;
    RestrictionManager restrictionManager = null;

    private final Set<Location> shelves = new HashSet<>();
    private final File shelfFile = new File(getDataFolder(), "shelves.json");

    @Override
    public void onLoad() {
        worldGuardSupport = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        if (worldGuardSupport) {
            getLogger().info("Found WorldGuard plugin");
            WorldGuardUtils.registerBookshelfAccessFlag();
        }
    }

    public Set<Location> getShelves() {
        return shelves;
    }

    public String getInventoryTitle() {
        return inventoryTitle;
    }

    public boolean isHopperSupport() {
        return hopperSupport;
    }

    public File getShelfFile() {
        return shelfFile;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean isWorldGuardSupport() {
        return worldGuardSupport;
    }

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(new ShelfListener(this), this);

        // Save configuration & load values
        saveDefaultConfig();
        inventorySize = getConfig().getInt("inventory.size");
        if (inventorySize % 9 != 0) {
            getLogger().warning("Inventory size is not a multiple of 9");
            inventorySize = 18;
        }
        inventoryTitle = ChatColor.translateAlternateColorCodes('&', getConfig().getString("inventory.title")) +/* Unique title */"§B§S";
        if (getConfig().contains("disabledWorlds")) {
            disabledWorlds.addAll(getConfig().getStringList("disabledWorlds"));
        }
        onlyBooks = getConfig().getBoolean("onlyBooks", true);
        checkRestrictions = getConfig().getBoolean("restrictions.enabled");
        hopperSupport = getConfig().getBoolean("hoppers");

        // Initialize restrictions
        if (checkRestrictions) {
            restrictionManager = new RestrictionManager();
        }

        // GriefPrevention compatibility
        // TODO, this should be in a seperate file, as a listener.
        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            getLogger().info("Found GriefPrevention plugin");
            try {
                Class<?> PlayerEventHandler = Class.forName("me.ryanhamshire.GriefPrevention.PlayerEventHandler");
                Object playerEventHandlerInstance = null;

                //PlayerInteractEvent is also handled by PlayerEventHandler
                for (RegisteredListener registeredListener : PlayerInteractEvent.getHandlerList().getRegisteredListeners()) {
                    if (PlayerEventHandler.isAssignableFrom(registeredListener.getListener().getClass())) {
                        playerEventHandlerInstance = registeredListener.getListener();
                        break;
                    }
                }
                if (playerEventHandlerInstance == null) {
                    getLogger().warning("Could not find PlayerEventHandler for GriefPrevention");
                } else {
                    Field inventoryHolderCacheField = AccessUtil.setAccessible(PlayerEventHandler.getDeclaredField("inventoryHolderCache"));
                    ConcurrentHashMap<Material, Boolean> inventoryHolderCache = (ConcurrentHashMap<Material, Boolean>) inventoryHolderCacheField.get(playerEventHandlerInstance);

                    inventoryHolderCache.put(Material.BOOKSHELF, true);
                    getLogger().info("Injected Bookshelf as container type into GriefPrevention");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ShelfFile creation
        if (!shelfFile.exists()) {
            try {
                shelfFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new ScheduleBookLoading(this).runTaskLater(this,40L);
        new Metrics(this, 5131);
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving shelves...");
        try {
            JsonArray shelfArray = new JsonArray();
            for (Location location : shelves) {
                Block block = location.getBlock();
                Inventory inventory = MetaHelper.getMetaValue(block, "BOOKSHELF_INVENTORY", Inventory.class);
                if (inventory == null) {
                    continue;
                }
                JsonObject shelfObject = new JsonObject();
                shelfObject.add("location", LocationToJson(location));
                ItemStack[] contents = inventory.getContents();

                {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                    dataOutput.writeInt(contents.length);

                    for (ItemStack stack : contents) {
                        dataOutput.writeObject(stack);
                    }
                    dataOutput.close();

                    shelfObject.addProperty("books", Base64Coder.encodeLines(outputStream.toByteArray()));
                }

                shelfArray.add(shelfObject);
            }

            try (Writer writer = new FileWriter(shelfFile)) {
                new Gson().toJson(shelfArray, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isValidBook(ItemStack itemStack) {
        if (!onlyBooks) {
            return !checkRestrictions || restrictionManager.isRestricted(itemStack.getType());
        }

        if (itemStack == null) {
            return false;
        }

        switch (itemStack.getType()) {
            case BOOK:
            case WRITABLE_BOOK:
            case ENCHANTED_BOOK:
            case WRITTEN_BOOK:
                return true;
            default: return false;
        }
    }

    public Inventory initShelf(@NotNull Block block) {
        Inventory inventory;
        if (!block.hasMetadata("BOOKSHELF_INVENTORY")) {
            inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);
            MetaHelper.setMetaValue(block, "BOOKSHELF_INVENTORY", inventory);

            shelves.add(block.getLocation());
            if (hopperSupport) HopperUtils.pull(block);

            return inventory;
        } else {
            inventory = getShelf(block);
            if (inventory != null) {
                shelves.add(block.getLocation());
                return inventory;
            }
        }
        return null;
    }

    public Inventory getShelf(@NotNull Block block) {
        if (block.hasMetadata("BOOKSHELF_INVENTORY")) {
            return MetaHelper.getMetaValue(block, "BOOKSHELF_INVENTORY", Inventory.class);
        }
        return null;
    }

    @Contract("_, _ -> param2")
    public static ConfigurationSection JsonToYaml(@NotNull JsonObject json, ConfigurationSection section) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                ConfigurationSection var9 = section.getConfigurationSection(key);
                if (var9 == null) {
                    var9 = section.createSection(key);
                }

                var9 = JsonToYaml((JsonObject) value, var9);
                section.set(key, var9);
            } else if (!value.isJsonArray()) {
                section.set(key, value.getAsString());
            } else {
                ArrayList list = new ArrayList();
                JsonArray array = (JsonArray) value;

                for (int i = 0; i < array.size(); ++i) {
                    list.add(i, array.get(i));
                }

                section.set(key, list);
            }
        }

        return section;
    }

    public static @NotNull JsonObject LocationToJson(@NotNull Location location) {
        JsonObject json = new JsonObject();
        json.addProperty("world", location.getWorld().getName());
        json.addProperty("x", location.getX());
        json.addProperty("y", location.getY());
        json.addProperty("z", location.getZ());
        return json;
    }

    public static @NotNull Location JsonToLocation(@NotNull JsonObject json) {
        String worldName = json.get("world").getAsString();
        double x = json.get("x").getAsDouble();
        double y = json.get("y").getAsDouble();
        double z = json.get("z").getAsDouble();
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

}
