package com.rxdwan.realworld;

import com.rxdwan.realworld.config.ConfigManager;
import com.rxdwan.realworld.crime.CrimeManager;
import com.rxdwan.realworld.crime.JailManager;
import com.rxdwan.realworld.util.LogManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RealWorldPlugin extends JavaPlugin {

    public static final String PLUGIN_NAME = "RealWorld";
    public static final String PLUGIN_VERSION = "1.0";
    public static final String PLUGIN_AUTHOR = "rxdwan";
    public static final String PLUGIN_DESCRIPTION = "A manual crime, bounty, and jail system.";

    private ConfigManager configManager;
    private CrimeManager crimeManager;
    private JailManager jailManager;
    private LogManager logManager;

    private Economy econ = null;

    @Override
    public void onEnable() {
        // Load Configuration
        configManager = new ConfigManager(this);
        
        // Initialize Messages
        com.rxdwan.realworld.util.MessageUtil.init(this);

        // Initialize LogManager early so other managers can use it
        logManager = new LogManager(this);

        // Vault Setup
        // Audit: RegisteredServiceProvider is still the standard and correct way to hook Vault API in Paper 1.21.
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Managers
        jailManager = new JailManager(this);
        crimeManager = new CrimeManager(this);

        // Load Data
        crimeManager.loadData();
        jailManager.loadData();

        // Register Events
        getServer().getPluginManager().registerEvents(new com.rxdwan.realworld.crime.CrimeEventListeners(this), this);

        // Register Commands
        com.rxdwan.realworld.crime.CrimeCommands crimeCmds = new com.rxdwan.realworld.crime.CrimeCommands(this);
        getCommand("wantedlist").setExecutor(crimeCmds);
        getCommand("chargecrime").setExecutor(crimeCmds);
        getCommand("chargecrime").setTabCompleter(crimeCmds);
        getCommand("pardon").setExecutor(crimeCmds);
        getCommand("cuffs").setExecutor(crimeCmds);
        getCommand("bail").setExecutor(crimeCmds);

        getCommand("realworld").setExecutor(new RealWorldCommands(this));

        getLogger().info("RealWorld Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        if (crimeManager != null) crimeManager.saveData();
        if (jailManager != null) jailManager.saveData();

        getLogger().info("RealWorld Plugin Disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CrimeManager getCrimeManager() {
        return crimeManager;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public Economy getEconomy() {
        return econ;
    }
}
