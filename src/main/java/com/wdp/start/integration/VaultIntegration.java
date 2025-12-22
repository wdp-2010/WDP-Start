package com.wdp.start.integration;

import com.wdp.start.WDPStartPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault economy integration for SkillCoins rewards
 */
public class VaultIntegration {
    
    private final WDPStartPlugin plugin;
    private Economy economy;
    private boolean enabled;
    
    public VaultIntegration(WDPStartPlugin plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            enabled = false;
            plugin.getLogger().warning("Vault found but no economy provider registered!");
            return;
        }
        
        economy = rsp.getProvider();
        enabled = economy != null;
        
        if (enabled) {
            plugin.getLogger().info("Economy provider: " + economy.getName());
        }
    }
    
    /**
     * Check if economy is enabled
     */
    public boolean isEnabled() {
        return enabled && economy != null;
    }
    
    /**
     * Get player balance
     */
    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }
    
    /**
     * Deposit money to player
     */
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) {
            plugin.getLogger().warning("Attempted to deposit " + amount + " to " + player.getName() + " but economy is disabled!");
            return false;
        }
        
        economy.depositPlayer(player, amount);
        plugin.debug("Deposited " + amount + " to " + player.getName());
        return true;
    }
    
    /**
     * Withdraw money from player
     */
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        
        if (economy.getBalance(player) < amount) {
            return false;
        }
        
        economy.withdrawPlayer(player, amount);
        plugin.debug("Withdrew " + amount + " from " + player.getName());
        return true;
    }
    
    /**
     * Check if player has enough money
     */
    public boolean has(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }
    
    /**
     * Get the economy provider
     */
    public Economy getEconomy() {
        return economy;
    }
}
