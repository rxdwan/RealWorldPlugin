package com.rxdwan.realworld;

import com.rxdwan.realworld.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RealWorldCommands implements CommandExecutor {

    private final RealWorldPlugin plugin;

    public RealWorldCommands(RealWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("realworld.admin")) {
                MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("no_permission"));
                return true;
            }
            plugin.getConfigManager().reload();
            MessageUtil.sendPrefixedMessage(sender, MessageUtil.getMessage("reload_success"));
            plugin.getLogger().info("Configuration reloaded by " + sender.getName() + ".");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§6§l--- RealWorld ---");
            sender.sendMessage("§eVersion: §f" + RealWorldPlugin.PLUGIN_VERSION);
            sender.sendMessage("§eAuthor: §f" + RealWorldPlugin.PLUGIN_AUTHOR);
            sender.sendMessage("§eDescription: §f" + RealWorldPlugin.PLUGIN_DESCRIPTION);
            sender.sendMessage("§6----------------");
            return true;
        }

        if (sender.hasPermission("realworld.admin")) {
            sender.sendMessage(MessageUtil.getMessage("usage_realworld"));
        } else {
            // If they just type /realworld and aren't admin, show info as default fallback
            sender.sendMessage("§6§l--- RealWorld ---");
            sender.sendMessage("§eVersion: §f" + RealWorldPlugin.PLUGIN_VERSION);
            sender.sendMessage("§eAuthor: §f" + RealWorldPlugin.PLUGIN_AUTHOR);
            sender.sendMessage("§eDescription: §f" + RealWorldPlugin.PLUGIN_DESCRIPTION);
            sender.sendMessage("§6----------------");
        }
        return true;
    }
}
