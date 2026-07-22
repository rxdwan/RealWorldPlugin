package com.rxdwan.realworld.config;

import com.rxdwan.realworld.RealWorldPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final RealWorldPlugin plugin;
    private FileConfiguration config;

    // Jail
    public Location jailLocation;
    public Location jailExitLocation;
    public double bailAmount;
    public List<String> allowedJailCommands;

    // Crime
    public double bountyIncrement;
    public final Map<String, CrimeConfig> crimes = new HashMap<>();

    // Messages
    public final Map<String, String> messages = new HashMap<>();

    public ConfigManager(RealWorldPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Jail
        jailLocation = getLocation("jail.location");
        jailExitLocation = getLocation("jail.exit_location");
        bailAmount = config.getDouble("jail.bail_amount", 8000.0);
        allowedJailCommands = config.getStringList("jail.allowed_commands");

        // Crime
        bountyIncrement = config.getDouble("crime.bounty_increment", 1000.0);
        crimes.clear();
        ConfigurationSection crimesSection = config.getConfigurationSection("crime.crimes");
        if (crimesSection != null) {
            Set<String> keys = crimesSection.getKeys(false);
            for (String key : keys) {
                boolean enabled = crimesSection.getBoolean(key + ".enabled", true);
                double bounty = crimesSection.getDouble(key + ".bounty", 1000.0);
                crimes.put(key, new CrimeConfig(key, enabled, bounty));
            }
        }

        // Messages
        messages.clear();
        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, msgSection.getString(key));
            }
        }
    }

    private Location getLocation(String path) {
        String worldName = config.getString(path + ".world", "world");
        double x = config.getDouble(path + ".x", 0.0);
        double y = config.getDouble(path + ".y", 64.0);
        double z = config.getDouble(path + ".z", 0.0);
        float yaw = (float) config.getDouble(path + ".yaw", 0.0);
        float pitch = (float) config.getDouble(path + ".pitch", 0.0);
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for location path '" + path + "'. Returning null.");
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static class CrimeConfig {
        public final String name;
        public final boolean enabled;
        public final double bounty;

        public CrimeConfig(String name, boolean enabled, double bounty) {
            this.name = name;
            this.enabled = enabled;
            this.bounty = bounty;
        }
    }
}
