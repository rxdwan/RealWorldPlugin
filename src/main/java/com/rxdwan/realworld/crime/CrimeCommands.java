package com.rxdwan.realworld.crime;

import com.rxdwan.realworld.RealWorldPlugin;
import com.rxdwan.realworld.util.MessageUtil;
import com.rxdwan.realworld.util.TableUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CrimeCommands implements CommandExecutor, TabCompleter {

    private final RealWorldPlugin plugin;

    public CrimeCommands(RealWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // ---------------------------------------------------------------
        // /wantedlist — all players can use
        // ---------------------------------------------------------------
        if (cmdName.equals("wantedlist")) {
            Collection<WantedEntry> wanted = plugin.getCrimeManager().getAllWanted();
            if (wanted.isEmpty()) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("wantedlist_empty"));
                return true;
            }
            
            List<String> header = List.of("Player", "Bounty", "Crimes");
            List<List<String>> rows = new ArrayList<>();
            for (WantedEntry w : wanted) {
                Map<String, Integer> counts = new HashMap<>();
                for (String c : w.getCrimes()) {
                    counts.put(c, counts.getOrDefault(c, 0) + 1);
                }
                List<String> formattedCrimes = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > 1) {
                        formattedCrimes.add(entry.getKey() + " (" + entry.getValue() + ")");
                    } else {
                        formattedCrimes.add(entry.getKey());
                    }
                }
                String crimesStr = String.join(", ", formattedCrimes);
                rows.add(List.of("&c" + w.getPlayerName(), "&e$" + w.getBounty(), "&f" + crimesStr));
            }
            
            List<String> table = TableUtil.tabulize(header, rows);
            sender.sendMessage(MessageUtil.color("&6=== Wanted Criminals ==="));
            for (String line : table) {
                sender.sendMessage(MessageUtil.color("&7" + line));
            }
            return true;
        }

        // ---------------------------------------------------------------
        // /inmates — all players can use
        // ---------------------------------------------------------------
        if (cmdName.equals("inmates")) {
            Collection<JailManager.JailRecord> inmates = plugin.getJailManager().getAllInmates();
            if (inmates.isEmpty()) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("inmates_empty"));
                return true;
            }
            
            List<String> header = List.of("Inmate", "Bail", "Crimes");
            List<List<String>> rows = new ArrayList<>();
            for (JailManager.JailRecord record : inmates) {
                String name = record.playerName != null ? record.playerName : "Unknown";
                String bail = String.format("$%.2f", record.bailAmount);
                
                String crimesStr = "Unknown";
                if (record.crimes != null && !record.crimes.isEmpty()) {
                    Map<String, Integer> counts = new HashMap<>();
                    for (String c : record.crimes) {
                        counts.put(c, counts.getOrDefault(c, 0) + 1);
                    }
                    List<String> formattedCrimes = new ArrayList<>();
                    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                        if (entry.getValue() > 1) {
                            formattedCrimes.add(entry.getKey() + " (" + entry.getValue() + ")");
                        } else {
                            formattedCrimes.add(entry.getKey());
                        }
                    }
                    crimesStr = String.join(", ", formattedCrimes);
                }
                
                rows.add(List.of("&c" + name, "&e" + bail, "&f" + crimesStr));
            }
            
            List<String> table = TableUtil.tabulize(header, rows);
            sender.sendMessage(MessageUtil.color("&6=== Current Inmates ==="));
            for (String line : table) {
                sender.sendMessage(MessageUtil.color("&7" + line));
            }
            return true;
        }

        // ---------------------------------------------------------------
        // /crimelog [limit] — admin only
        // ---------------------------------------------------------------
        if (cmdName.equals("crimelog")) {
            if (!sender.hasPermission("realworld.admin")) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("no_permission"));
                return true;
            }
            
            int limit = 20;
            if (args.length > 0) {
                try {
                    limit = Integer.parseInt(args[0]);
                } catch (NumberFormatException ignored) {}
            }
            
            File logFile = new File(plugin.getDataFolder(), "logs/crime_log.txt");
            if (!logFile.exists() || logFile.length() == 0) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("crimelog_empty"));
                return true;
            }
            
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                sender.sendMessage(MessageUtil.getMessage("crimelog_error"));
                return true;
            }
            
            if (lines.isEmpty()) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("crimelog_empty"));
                return true;
            }
            
            int start = Math.max(0, lines.size() - limit);
            List<String> header = List.of("Timestamp", "Action", "Player", "Crime", "Bounty", "Extra");
            List<List<String>> rows = new ArrayList<>();
            
            for (int i = start; i < lines.size(); i++) {
                String line = lines.get(i);
                try {
                    String timestamp = line.substring(1, line.indexOf("]"));
                    String rest1 = line.substring(line.indexOf("]") + 2);
                    String[] parts = rest1.split(" \\| ");
                    String action = parts[0].trim();
                    String playerStr = parts[1].replace("Player: ", "").split(" \\(")[0].trim();
                    String crime = parts[2].replace("Crime: ", "").trim();
                    String bounty = parts[3].replace("Bounty: ", "").trim();
                    String extra = parts[4].replace("Admin: ", "").trim();
                    
                    rows.add(List.of(timestamp, action, playerStr, crime, bounty, extra));
                } catch (Exception e) {
                    continue;
                }
            }
            
            sender.sendMessage(MessageUtil.getMessage("crimelog_header"));
            List<String> table = TableUtil.tabulize(header, rows);
            for (String line : table) {
                sender.sendMessage(MessageUtil.color("&7" + line));
            }
            return true;
        }

        // ---------------------------------------------------------------
        // /chargecrime <player> <crime> — admin only
        // ---------------------------------------------------------------
        if (cmdName.equals("chargecrime")) {
            if (!sender.hasPermission("realworld.admin")) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("no_permission"));
                return true;
            }
            if (args.length < 2) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("usage_chargecrime"));
                return true;
            }

            String crimeKey = args[1];
            if (!plugin.getConfigManager().crimes.containsKey(crimeKey)
                    || !plugin.getConfigManager().crimes.get(crimeKey).enabled) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("crime_not_found", Map.of("crime", crimeKey)));
                return true;
            }

            // Try online player first
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                plugin.getCrimeManager().chargeCrime(target, crimeKey, sender.getName());
                return true;
            }

            // Fall back to offline player
            org.bukkit.OfflinePlayer offTarget = Bukkit.getOfflinePlayer(args[0]);
            if (!offTarget.hasPlayedBefore() && !offTarget.isOnline()) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("player_not_found", Map.of("player", args[0])));
                return true;
            }

            plugin.getCrimeManager().chargeCrime(offTarget, crimeKey, sender.getName());
            return true;
        }

        // ---------------------------------------------------------------
        // /pardon <player> [crimes...] — admin only
        // ---------------------------------------------------------------
        if (cmdName.equals("pardon")) {
            if (!sender.hasPermission("realworld.admin")) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("no_permission"));
                return true;
            }
            if (args.length < 1) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("usage_pardon"));
                return true;
            }
            org.bukkit.OfflinePlayer offTarget = Bukkit.getOfflinePlayer(args[0]);

            if (args.length == 1) {
                plugin.getCrimeManager().pardonPlayer(offTarget.getUniqueId());
                plugin.getLogManager().logCrime("PARDONED", offTarget.getName(), offTarget.getUniqueId().toString(),
                        "N/A", 0, sender.getName());

                if (offTarget.isOnline() && offTarget.getPlayer() != null) {
                    Player onlineTarget = offTarget.getPlayer();
                    if (onlineTarget.getPersistentDataContainer().has(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE)) {
                        plugin.getJailManager().releasePlayer(onlineTarget, true);
                    }
                    MessageUtil.sendPrefixedMessage(onlineTarget, MessageUtil.getMessage("pardon_full"));
                }
            } else {
                List<String> toRemove = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                List<String> successfullyRemoved = new ArrayList<>();
                boolean fullyPardoned = plugin.getCrimeManager().pardonPlayerPartial(offTarget.getUniqueId(), toRemove, successfullyRemoved);
                
                // Notify about missing charges
                List<String> tempRemoved = new ArrayList<>(successfullyRemoved);
                for (String requested : toRemove) {
                    if (tempRemoved.contains(requested)) {
                        tempRemoved.remove(requested);
                    } else {
                        MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("pardon_no_charge", Map.of("crime", requested)));
                    }
                }
                
                if (fullyPardoned) {
                    plugin.getLogManager().logCrime("PARDONED", offTarget.getName(), offTarget.getUniqueId().toString(), "N/A", 0, sender.getName());
                    if (offTarget.isOnline() && offTarget.getPlayer() != null) {
                        Player onlineTarget = offTarget.getPlayer();
                        if (onlineTarget.getPersistentDataContainer().has(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE)) {
                            plugin.getJailManager().releasePlayer(onlineTarget, true);
                        }
                        MessageUtil.sendPrefixedMessage(onlineTarget, MessageUtil.getMessage("pardon_full"));
                    }
                } else {
                    WantedEntry currentEntry = plugin.getCrimeManager().getWantedEntry(offTarget.getUniqueId());
                    double currentBounty = currentEntry != null ? currentEntry.getBounty() : 0.0;
                    plugin.getLogManager().logCrime("PARDONED_PARTIAL", offTarget.getName(), offTarget.getUniqueId().toString(), String.join(",", successfullyRemoved), currentBounty, sender.getName());
                    if (offTarget.isOnline() && offTarget.getPlayer() != null) {
                        MessageUtil.sendPrefixedMessage(offTarget.getPlayer(), MessageUtil.getMessage("pardon_partial", Map.of("crimes", String.join(", ", successfullyRemoved))));
                    }
                }
            }
            
            MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("pardon_admin_success", Map.of("player", offTarget.getName())));
            return true;
        }

        // ---------------------------------------------------------------
        // /cuffs — player command
        // ---------------------------------------------------------------
        if (cmdName.equals("cuffs")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (plugin.getCrimeManager().isWanted(player.getUniqueId())) {
                MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("cuffs_cannot_use"));
                return true;
            }

            ItemStack cuffs = new ItemStack(Material.IRON_CHAIN);
            ItemMeta meta = cuffs.getItemMeta();
            // Audit: setDisplayName, setLore, and setCustomModelData are deprecated in newer Paper versions 
            // in favor of Adventure Components and DataComponent API, but remain functional.
            meta.setDisplayName(MessageUtil.color("&bHandcuffs"));
            meta.setLore(List.of(MessageUtil.getMessage("cuffs_lore").split("\n")));
            meta.setUnbreakable(true);
            meta.setCustomModelData(773100);
            meta.getPersistentDataContainer().set(// changed arrest_cuffs to handcuffs in PDC
                    new NamespacedKey(plugin, "handcuffs"), PersistentDataType.BYTE, (byte) 1);
            cuffs.setItemMeta(meta);

            player.getInventory().addItem(cuffs);
            MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("cuffs_received"));
            return true;
        }

        // ---------------------------------------------------------------
        // /bail [playerName] — player command
        // ---------------------------------------------------------------
        if (cmdName.equals("bail")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            Player target = player;
            if (args.length == 1) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("player_not_found", Map.of("player", args[0])));
                    return true;
                }
            }

            // Check if target is actually jailed
            if (!target.getPersistentDataContainer().has(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE)) {
                if (target.equals(player)) {
                    MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("not_in_jail_self"));
                } else {
                    MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("not_in_jail_other"));
                }
                return true;
            }

            double bailAmount = plugin.getConfigManager().bailAmount;
            if (plugin.getEconomy().getBalance(player) < bailAmount) {
                MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("bail_not_enough_money", Map.of("amount", String.valueOf(bailAmount))));
                return true;
            }

            EconomyResponse r = plugin.getEconomy().withdrawPlayer(player, bailAmount);
            if (r.transactionSuccess()) {
                plugin.getJailManager().releasePlayer(target, false);
                
                // Broadcast on success
                if (player.equals(target)) {
                    String bc = MessageUtil.getMessage("bail_broadcast_self", Map.of("criminal", target.getName(), "amount", String.valueOf(bailAmount)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        MessageUtil.sendPrefixedMessage(p, bc);
                    }
                } else {
                    String bc = MessageUtil.getMessage("bail_broadcast", Map.of("payer", player.getName(), "criminal", target.getName(), "amount", String.valueOf(bailAmount)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        MessageUtil.sendPrefixedMessage(p, bc);
                    }
                }
            } else {
                MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("bail_transaction_failed"));
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("chargecrime") && args.length == 2) {
            completions.addAll(plugin.getConfigManager().crimes.keySet());
        }
        return completions;
    }
}
