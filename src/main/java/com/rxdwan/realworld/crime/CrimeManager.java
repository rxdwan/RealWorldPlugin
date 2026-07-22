package com.rxdwan.realworld.crime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.rxdwan.realworld.RealWorldPlugin;
import com.rxdwan.realworld.config.ConfigManager.CrimeConfig;
import com.rxdwan.realworld.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class CrimeManager {

    private final RealWorldPlugin plugin;
    private final Gson gson;
    private final File wantedFile;
    private final Map<UUID, WantedEntry> wantedPlayers = new HashMap<>();

    public CrimeManager(RealWorldPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.wantedFile = new File(plugin.getDataFolder(), "wanted.json");
    }

    public void loadData() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (wantedFile.exists()) {
            try (FileReader reader = new FileReader(wantedFile)) {
                Type listType = new TypeToken<ArrayList<WantedEntry>>(){}.getType();
                List<WantedEntry> list = gson.fromJson(reader, listType);
                if (list != null) {
                    for (WantedEntry e : list) {
                        wantedPlayers.put(e.getPlayerUUID(), e);
                    }
                }
            } catch (JsonSyntaxException e) {
                plugin.getLogger().warning("wanted.json is corrupted or in an old format. Resetting it. (" + e.getMessage() + ")");
                // Back up the bad file and start fresh
                wantedFile.renameTo(new File(plugin.getDataFolder(), "wanted.json.bak"));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not load wanted.json: " + e.getMessage());
            }
        }
    }

    public void saveData() {
        try (FileWriter writer = new FileWriter(wantedFile)) {
            gson.toJson(new ArrayList<>(wantedPlayers.values()), writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save wanted.json: " + e.getMessage());
        }
    }

    public boolean isWanted(UUID uuid) {
        return wantedPlayers.containsKey(uuid);
    }

    public WantedEntry getWantedEntry(UUID uuid) {
        return wantedPlayers.get(uuid);
    }

    public Collection<WantedEntry> getAllWanted() {
        return wantedPlayers.values();
    }

    public void pardonPlayer(UUID uuid) {
        wantedPlayers.remove(uuid);
        saveData();
    }

    /**
     * Partially pardons a player by removing specific crimes.
     * @return true if the player is now fully pardoned (no crimes left), false otherwise.
     */
    public boolean pardonPlayerPartial(UUID uuid, List<String> crimesToRemove, List<String> successfullyRemoved) {
        WantedEntry entry = wantedPlayers.get(uuid);
        if (entry == null) return true;

        List<String> currentCrimes = entry.getCrimes();

        for (String crimeToRemove : crimesToRemove) {
            if (currentCrimes.remove(crimeToRemove)) {
                successfullyRemoved.add(crimeToRemove);
            }
        }

        if (currentCrimes.isEmpty()) {
            pardonPlayer(uuid);
            return true;
        }

        // Recompute bounty
        double newBounty = 0.0;
        CrimeConfig firstCfg = plugin.getConfigManager().crimes.get(currentCrimes.get(0));
        if (firstCfg != null) {
            newBounty = firstCfg.bounty;
        }
        if (currentCrimes.size() > 1) {
            newBounty += plugin.getConfigManager().bountyIncrement * (currentCrimes.size() - 1);
        }
        
        entry.setBounty(newBounty);
        saveData();
        return false;
    }

    // -------------------------------------------------------------------------
    // Charge a crime manually.
    // -------------------------------------------------------------------------
    public void chargeCrime(org.bukkit.OfflinePlayer player, String crimeKey, String adminName) {
        CrimeConfig cfg = plugin.getConfigManager().crimes.get(crimeKey);
        if (cfg == null || !cfg.enabled) return;

        boolean wasWanted = wantedPlayers.containsKey(player.getUniqueId());
        String pName = player.getName() != null ? player.getName() : "Unknown";
        WantedEntry entry = wantedPlayers.computeIfAbsent(
                player.getUniqueId(),
                k -> new WantedEntry(player.getUniqueId(), pName, 0, crimeKey)
        );

        if (!wasWanted) {
            entry.setBounty(cfg.bounty);
            String msg = MessageUtil.getMessage("newly_wanted", Map.of(
                    "player", pName,
                    "bounty", String.valueOf(entry.getBounty())
            ));
            MessageUtil.sendMessage(Bukkit.getConsoleSender(), msg);
            for (Player p : Bukkit.getOnlinePlayers()) {
                MessageUtil.sendMessage(p, msg);
            }
            plugin.getLogManager().logCrime("WANTED", pName, player.getUniqueId().toString(),
                    crimeKey, entry.getBounty(), adminName);
        } else {
            entry.addCrime(crimeKey);
            entry.setBounty(entry.getBounty() + plugin.getConfigManager().bountyIncrement);
            String msg = MessageUtil.getMessage("already_wanted_increase", Map.of(
                    "player", pName,
                    "bounty", String.valueOf(entry.getBounty())
            ));
            MessageUtil.sendMessage(Bukkit.getConsoleSender(), msg);
            for (Player p : Bukkit.getOnlinePlayers()) {
                MessageUtil.sendMessage(p, msg);
            }
            plugin.getLogManager().logCrime("BOUNTY_INCREASE", pName, player.getUniqueId().toString(),
                    crimeKey, entry.getBounty(), adminName);
        }

        saveData();
    }
}
