package com.rxdwan.realworld.crime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rxdwan.realworld.RealWorldPlugin;
import com.rxdwan.realworld.util.MessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JailManager {

    private final RealWorldPlugin plugin;
    private final Gson gson;
    private final File jailFile;
    private final Map<UUID, JailRecord> jailRecords = new HashMap<>();

    public JailManager(RealWorldPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jailFile = new File(plugin.getDataFolder(), "jail_inventory.json");
    }

    public void loadData() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (jailFile.exists()) {
            try (FileReader reader = new FileReader(jailFile)) {
                Type listType = new TypeToken<ArrayList<JailRecord>>(){}.getType();
                List<JailRecord> list = gson.fromJson(reader, listType);
                if (list != null) {
                    for (JailRecord r : list) {
                        jailRecords.put(r.playerUUID, r);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not load jail_inventory.json: " + e.getMessage());
            }
        }
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(jailFile)) {
            gson.toJson(new ArrayList<>(jailRecords.values()), writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save jail_inventory.json: " + e.getMessage());
        }
    }

    /**
     * Jails the criminal.
     * @param criminal  The player being jailed.
     * @param arrester  The citizen arresting player, or null if arrested by police NPC.
     */
    public void jailPlayer(Player criminal, Player arrester) {
        WantedEntry we = plugin.getCrimeManager().getWantedEntry(criminal.getUniqueId());
        List<String> crimes = we != null ? new ArrayList<>(we.getCrimes()) : new ArrayList<>();
        double bail = plugin.getConfigManager().bailAmount;

        // Serialize and store inventory
        String serialized = serializeInventory(criminal.getInventory().getContents());
        jailRecords.put(criminal.getUniqueId(), new JailRecord(criminal.getUniqueId(), criminal.getName(), serialized, crimes, bail));
        saveData();

        // Clear inventory
        criminal.getInventory().clear();

        // Teleport to jail
        if (plugin.getConfigManager().jailLocation != null) {
            criminal.teleport(plugin.getConfigManager().jailLocation);
        }

        // Apply jailed tag
        criminal.getPersistentDataContainer().set(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE, (byte) 1);

        // Bounty deposit if citizen arrest
        double bountyPaid = 0;
        // 'we' is already defined above, we can just reuse it.
        if (we != null) {
            bountyPaid = we.getBounty();
            if (arrester != null) {
                plugin.getEconomy().depositPlayer(arrester, bountyPaid);
                MessageUtil.sendPrefixedMessage(arrester, MessageUtil.getMessage("arrest_success", Map.of(
                        "criminal", criminal.getName(),
                        "bounty", String.valueOf(bountyPaid)
                )));
            }
        }

        // Remove from wanted list now that they are in jail
        plugin.getCrimeManager().pardonPlayer(criminal.getUniqueId());

        MessageUtil.sendPrefixedMessage(criminal, MessageUtil.getMessage("arrest_success_criminal", Map.of("bailAmount", String.valueOf(plugin.getConfigManager().bailAmount))));

        // Log the arrest
        plugin.getLogManager().logCrime("ARREST", criminal.getName(), criminal.getUniqueId().toString(),
                "N/A", bountyPaid, arrester != null ? arrester.getName() : "CITIZEN");
    }

    /**
     * Releases the criminal (bail or pardon). Restores inventory, removes jailed tag, teleports to exit.
     */
    public void releasePlayer(Player criminal, boolean isPardon) {
        JailRecord record = jailRecords.remove(criminal.getUniqueId());
        saveData();

        if (record != null && record.serializedInventory != null) {
            ItemStack[] contents = deserializeInventory(record.serializedInventory);
            if (contents != null) {
                criminal.getInventory().setContents(contents);
            }
        }

        criminal.getPersistentDataContainer().remove(new NamespacedKey(plugin, "jailed"));
        plugin.getCrimeManager().pardonPlayer(criminal.getUniqueId());

        if (plugin.getConfigManager().jailExitLocation != null) {
            criminal.teleport(plugin.getConfigManager().jailExitLocation);
            // Overwrite their respawn point too, in case they slept on a bed
            // inside the jail cell while serving time — otherwise they'd keep
            // respawning back in the cell on every future death.
            criminal.setRespawnLocation(plugin.getConfigManager().jailExitLocation, true);
        }

        if (!isPardon) {
            MessageUtil.sendPrefixedMessage(criminal, MessageUtil.getMessage("released"));
            
            // Log the release (bail event)
            plugin.getLogManager().logCrime("BAIL", criminal.getName(), criminal.getUniqueId().toString(),
                    "N/A", plugin.getConfigManager().bailAmount, null);
        }
    }

    public Collection<JailRecord> getAllInmates() {
        return jailRecords.values();
    }

    // -------------------------------------------------------------------------
    // Inventory serialization
    // -------------------------------------------------------------------------

    private String serializeInventory(ItemStack[] contents) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(outputStream);
            dataOutput.writeInt(contents.length);
            for (ItemStack item : contents) {
                if (item == null || item.getType().isAir()) {
                    dataOutput.writeInt(0);
                } else {
                    byte[] bytes = item.serializeAsBytes();
                    dataOutput.writeInt(bytes.length);
                    dataOutput.write(bytes);
                }
            }
            dataOutput.close();
            return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    private ItemStack[] deserializeInventory(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(data));
            DataInputStream dataInput = new DataInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                int length = dataInput.readInt();
                if (length == 0) {
                    items[i] = null;
                } else {
                    byte[] bytes = new byte[length];
                    dataInput.readFully(bytes);
                    items[i] = ItemStack.deserializeBytes(bytes);
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to decode inventory data: " + e.getMessage());
            return new ItemStack[0];
        }
    }

    public static class JailRecord {
        public UUID playerUUID;
        public String playerName;
        public String serializedInventory;
        public List<String> crimes;
        public double bailAmount;

        public JailRecord() {}
        
        public JailRecord(UUID uuid, String playerName, String data, List<String> crimes, double bailAmount) {
            this.playerUUID = uuid;
            this.playerName = playerName;
            this.serializedInventory = data;
            this.crimes = crimes;
            this.bailAmount = bailAmount;
        }
    }
}
