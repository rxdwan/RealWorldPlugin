package com.rxdwan.realworld.util;

import com.rxdwan.realworld.RealWorldPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageUtil {

    private static RealWorldPlugin plugin;

    public static void init(RealWorldPlugin instance) {
        plugin = instance;
    }

    /**
     * Sends a prefixed message to a CommandSender.
     */
    public static void sendPrefixedMessage(CommandSender sender, String message) {
        sender.sendMessage(color("&6[RealWorld] &r" + message));
    }

    /**
     * Sends a non-prefixed message to a CommandSender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    /**
     * Fetches a message from config.yml, replaces placeholders, and colorizes it.
     */
    public static String getMessage(String key, Map<String, String> placeholders) {
        if (plugin == null || plugin.getConfigManager() == null || plugin.getConfigManager().messages == null) {
            return color("&cMissing message: " + key);
        }

        String message = plugin.getConfigManager().messages.get(key);
        if (message == null) {
            return color("&cMissing message: " + key);
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return color(message);
    }

    /**
     * Convenience method to fetch a message without placeholders.
     */
    public static String getMessage(String key) {
        return getMessage(key, null);
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
