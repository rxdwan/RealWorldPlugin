package com.rxdwan.realworld.crime;

import com.rxdwan.realworld.RealWorldPlugin;
import com.rxdwan.realworld.util.MessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;

public class CrimeEventListeners implements Listener {

    private final RealWorldPlugin plugin;

    public CrimeEventListeners(RealWorldPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Handcuffs
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        if (damaged instanceof Player && damager instanceof Player) {
            Player victim = (Player) damaged;
            Player attacker = (Player) damager;
            ItemStack item = attacker.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() &&
                    item.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "handcuffs"), PersistentDataType.BYTE)) {
                if (plugin.getCrimeManager().isWanted(victim.getUniqueId())) {
                    plugin.getJailManager().jailPlayer(victim, attacker);
                    Bukkit.broadcastMessage("§c[!] §e" + attacker.getName() + " §carrested §e" + victim.getName() + " §cand sent them to rot in jail!");
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        attacker.getInventory().setItemInMainHand(null);
                    }
                } else {
                    MessageUtil.sendPrefixedMessage(attacker, MessageUtil.getMessage("cuffs_not_wanted"));
                }
                event.setCancelled(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Completely disable right-click usage for the cuffs
    // -------------------------------------------------------------------------
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer()
                        .has(new NamespacedKey(plugin, "handcuffs"), PersistentDataType.BYTE)) {
            // Cancel any interaction with the item (placing, using, etc.)
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Jail Respawn
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE)) {
            if (plugin.getConfigManager().jailLocation != null) {
                event.setRespawnLocation(plugin.getConfigManager().jailLocation);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Jail Command Blocking
    // -------------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, "jailed"), PersistentDataType.BYTE)) {
            String message = event.getMessage().toLowerCase();
            String command = message.split(" ")[0];

            boolean allowed = false;
            for (String allowedCmd : plugin.getConfigManager().allowedJailCommands) {
                if (command.equalsIgnoreCase(allowedCmd)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                event.setCancelled(true);
                MessageUtil.sendPrefixedMessage(player, MessageUtil.getMessage("jail_command_blocked"));
            }
        }
    }
}
