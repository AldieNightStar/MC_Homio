package haxidenti.homio;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

public class HomesConfig {
    private YamlConfiguration config;
    private File dataFolder;
    private int counter = 10;

    private static final int maxCounter = 10;

    public HomesConfig(File dataFolder) {
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) {
                throw new RuntimeException("Homio config folder did not created successfully!");
            }
        }
        config = YamlConfiguration.loadConfiguration(new File(dataFolder, "homio.yml"));
        this.dataFolder = dataFolder;
    }

    public void setHomeFor(String name, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        String playerDataPrefix = "homes." + noDots(name) + ".";

        config.set(playerDataPrefix + "world", world.getName());
        config.set(playerDataPrefix + "x", location.getBlockX());
        config.set(playerDataPrefix + "y", location.getBlockY());
        config.set(playerDataPrefix + "z", location.getBlockZ());

        countAndSave();
    }

    public Location getHomeFor(String name) {
        try {
            String playerDataPrefix = "homes." + noDots(name) + ".";

            String world = config.getString(playerDataPrefix + "world");
            if (world == null) return null;

            int x = config.getInt(playerDataPrefix + "x");
            int y = config.getInt(playerDataPrefix + "y");
            int z = config.getInt(playerDataPrefix + "z");

            return new Location(Bukkit.getWorld(world), x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    public void setTPointFor(String playerName, String pointName, Location location) {
        try {
            String dataPrefix = "points." + noDots(playerName) + "." + noDots(pointName) + ".";

            World world = location.getWorld();
            if (world == null) return;

            config.set(dataPrefix + "world", world.getName());
            config.set(dataPrefix + "x", location.getBlockX());
            config.set(dataPrefix + "y", location.getBlockY());
            config.set(dataPrefix + "z", location.getBlockZ());

            countAndSave();
        } catch (Exception ignored) {
        }
    }

    public Location getTPointFor(String playerName, String pointName) {
        try {
            String dataPrefix = "points." + noDots(playerName) + "." + noDots(pointName) + ".";

            String world = config.getString(dataPrefix + "world");
            if (world == null) return null;

            int x = config.getInt(dataPrefix + "x");
            int y = config.getInt(dataPrefix + "y");
            int z = config.getInt(dataPrefix + "z");

            return new Location(Bukkit.getWorld(world), x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    public void removeTPointFor(String playerName, String pointName) {
        try {
            config.set("points." + noDots(playerName) + "." + noDots(pointName), null);
        } catch (Exception ignored) {
        }
    }

    private void countAndSave() {
        counter -= 1;
        if (counter < 0) {
            counter = maxCounter;
            save();
        }
    }

    public void save() {
        try {
            config.save(new File(dataFolder, "homio.yml"));
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Can't save file \"homio.yml\": " + e.getMessage());
        }
    }

    public Set<String> getTPointListOf(String playerName) {
        String dataPrefix = "points." + noDots(playerName);

        return config.getConfigurationSection(dataPrefix).getKeys(false);
    }

    private static String noDots(String s) {
        return s.replace(".", "_");
    }

}
