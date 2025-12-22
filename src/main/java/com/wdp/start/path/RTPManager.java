package com.wdp.start.path;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * RTP (Random Teleport) Manager for Quest 1 completion
 * 
 * Features:
 * - Teleports player to random location near a tree
 * - Avoids existing bases using WDP-BaseDet API
 * - Respects world border
 * - Ensures safe landing location
 */
public class RTPManager {
    
    private final WDPStartPlugin plugin;
    private final Random random = new Random();
    
    // Tree log materials
    private static final Set<Material> TREE_LOGS = Set.of(
        Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG
    );
    
    // Leaf materials (to verify it's a tree)
    private static final Set<Material> TREE_LEAVES = Set.of(
        Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    );
    
    // Dangerous blocks to avoid
    private static final Set<Material> DANGEROUS_BLOCKS = Set.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
        Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
        Material.POWDER_SNOW
    );
    
    public RTPManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Perform RTP for a player after portal zone entry
     * 
     * @param player The player to teleport
     * @return CompletableFuture that completes when teleport is done
     */
    public CompletableFuture<Boolean> performRTP(Player player) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Check if RTP is enabled
        if (!plugin.getConfig().getBoolean("rtp.enabled", true)) {
            plugin.debug("[RTP] RTP is disabled in config");
            future.complete(false);
            return future;
        }
        
        // Get config values
        String worldName = plugin.getConfig().getString("rtp.world", "world");
        int minDistance = plugin.getConfig().getInt("rtp.min-distance", 1000);
        int maxDistance = plugin.getConfig().getInt("rtp.max-distance", 5000);
        int worldBorderBuffer = plugin.getConfig().getInt("rtp.world-border-buffer", 100);
        int maxAttempts = plugin.getConfig().getInt("rtp.tree-search.max-attempts", 50);
        int minDistanceFromTree = plugin.getConfig().getInt("rtp.tree-search.min-distance-from-tree", 5);
        int maxDistanceFromTree = plugin.getConfig().getInt("rtp.tree-search.max-distance-from-tree", 30);
        int minDistanceFromBase = plugin.getConfig().getInt("rtp.base-detection.min-distance-from-base", 200);
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[RTP] World '" + worldName + "' not found!");
            future.complete(false);
            return future;
        }
        
        // Get world border
        WorldBorder border = world.getWorldBorder();
        double borderSize = border.getSize() / 2 - worldBorderBuffer;
        Location borderCenter = border.getCenter();
        
        // Send searching message
        player.sendMessage(WDPStartPlugin.hex("&#FFD700&l✦ Finding the perfect spot..."));
        
        // Run async to not block main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location rtpLocation = null;
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                // Generate random angle and distance
                double angle = random.nextDouble() * 2 * Math.PI;
                int distance = minDistance + random.nextInt(maxDistance - minDistance);
                
                // Calculate coordinates
                int x = (int) (borderCenter.getX() + Math.cos(angle) * distance);
                int z = (int) (borderCenter.getZ() + Math.sin(angle) * distance);
                
                // Check world border
                if (Math.abs(x - borderCenter.getX()) > borderSize || 
                    Math.abs(z - borderCenter.getZ()) > borderSize) {
                    continue;
                }
                
                // Check if near a base (WDP-BaseDet integration)
                if (isNearBase(world, x, z, minDistanceFromBase)) {
                    plugin.debug("[RTP] Attempt " + attempt + ": Location near base, skipping");
                    continue;
                }
                
                // Find a tree near this location
                Location treeLocation = findNearbyTree(world, x, z, maxDistanceFromTree * 2);
                if (treeLocation == null) {
                    plugin.debug("[RTP] Attempt " + attempt + ": No tree found near (" + x + ", " + z + ")");
                    continue;
                }
                
                // Find safe landing spot near the tree
                rtpLocation = findSafeLandingNearTree(world, treeLocation, minDistanceFromTree, maxDistanceFromTree);
                if (rtpLocation != null) {
                    plugin.debug("[RTP] Found valid location after " + (attempt + 1) + " attempts");
                    break;
                }
            }
            
            final Location finalLocation = rtpLocation;
            
            // Teleport on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalLocation == null) {
                    player.sendMessage(WDPStartPlugin.hex("&#FF5555&l✗ Could not find a safe location!"));
                    player.sendMessage(WDPStartPlugin.hex("&#AAAAAAPlease try again or contact staff."));
                    future.complete(false);
                    return;
                }
                
                // Load the chunk first
                finalLocation.getChunk().load(true);
                
                // Teleport player
                player.teleport(finalLocation);
                
                // Success messages
                player.sendMessage("");
                player.sendMessage(WDPStartPlugin.hex("&#55FF55&l✦ Welcome to your starting area! ✦"));
                player.sendMessage(WDPStartPlugin.hex("&#AAAAAAYou've been teleported near a tree."));
                player.sendMessage(WDPStartPlugin.hex("&#AAAAAAStart gathering wood to begin!"));
                player.sendMessage("");
                
                // Play success sound
                if (plugin.getConfigManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                }
                
                // Spawn particles at location
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, finalLocation, 50, 1, 1, 1, 0.1);
                
                plugin.getLogger().info("[RTP] " + player.getName() + " teleported to " + 
                    String.format("(%.1f, %.1f, %.1f)", finalLocation.getX(), finalLocation.getY(), finalLocation.getZ()));
                
                future.complete(true);
            });
        });
        
        return future;
    }
    
    /**
     * Check if a location is near any detected base using WDP-BaseDet
     */
    private boolean isNearBase(World world, int x, int z, int minDistance) {
        try {
            // Try to get WDP-BaseDet plugin
            org.bukkit.plugin.Plugin baseDetPlugin = Bukkit.getPluginManager().getPlugin("WDP-BaseDet");
            if (baseDetPlugin == null || !baseDetPlugin.isEnabled()) {
                return false; // No base detection available
            }
            
            // Use reflection to call the API
            // This assumes WDP-BaseDet has a method like: isNearBase(World, x, z, distance)
            try {
                Object detectionManager = baseDetPlugin.getClass()
                    .getMethod("getDetectionManager")
                    .invoke(baseDetPlugin);
                
                if (detectionManager != null) {
                    Boolean result = (Boolean) detectionManager.getClass()
                        .getMethod("isLocationNearBase", World.class, int.class, int.class, int.class)
                        .invoke(detectionManager, world, x, z, minDistance);
                    return result != null && result;
                }
            } catch (Exception e) {
                // API method doesn't exist yet, fall back to no detection
                plugin.debug("[RTP] WDP-BaseDet API not available: " + e.getMessage());
            }
            
            return false;
        } catch (Exception e) {
            plugin.debug("[RTP] Error checking base proximity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a nearby tree within the search radius
     */
    private Location findNearbyTree(World world, int centerX, int centerZ, int searchRadius) {
        int minY = plugin.getConfig().getInt("rtp.safety.min-y", 63);
        int maxY = plugin.getConfig().getInt("rtp.safety.max-y", 200);
        
        // Spiral search pattern for efficiency
        for (int radius = 0; radius <= searchRadius; radius += 5) {
            for (int dx = -radius; dx <= radius; dx += 5) {
                for (int dz = -radius; dz <= radius; dz += 5) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue; // Only check border
                    
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    
                    // Get highest block
                    int highestY = world.getHighestBlockYAt(x, z);
                    if (highestY < minY || highestY > maxY) continue;
                    
                    // Check for tree
                    for (int y = highestY; y >= minY; y--) {
                        Block block = world.getBlockAt(x, y, z);
                        if (TREE_LOGS.contains(block.getType())) {
                            // Verify it's actually a tree (has leaves nearby)
                            if (hasLeavesNearby(world, x, y + 2, z)) {
                                return new Location(world, x, y, z);
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if there are leaves near a location (to verify it's a tree)
     */
    private boolean hasLeavesNearby(World world, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (TREE_LEAVES.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Find a safe landing spot near a tree
     */
    private Location findSafeLandingNearTree(World world, Location treeLocation, int minDist, int maxDist) {
        int treeX = treeLocation.getBlockX();
        int treeZ = treeLocation.getBlockZ();
        
        // Try multiple positions around the tree
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            int distance = minDist + random.nextInt(maxDist - minDist);
            
            int x = (int) (treeX + Math.cos(angle) * distance);
            int z = (int) (treeZ + Math.sin(angle) * distance);
            
            Location safeLoc = findSafeYLevel(world, x, z);
            if (safeLoc != null) {
                return safeLoc;
            }
        }
        
        return null;
    }
    
    /**
     * Find a safe Y level at the given X/Z coordinates
     */
    private Location findSafeYLevel(World world, int x, int z) {
        int minY = plugin.getConfig().getInt("rtp.safety.min-y", 63);
        int maxY = plugin.getConfig().getInt("rtp.safety.max-y", 200);
        boolean requireSolid = plugin.getConfig().getBoolean("rtp.safety.require-solid-ground", true);
        boolean avoidLiquids = plugin.getConfig().getBoolean("rtp.safety.avoid-liquids", true);
        
        int highestY = world.getHighestBlockYAt(x, z);
        
        if (highestY < minY || highestY > maxY) {
            return null;
        }
        
        Block groundBlock = world.getBlockAt(x, highestY, z);
        Block feetBlock = world.getBlockAt(x, highestY + 1, z);
        Block headBlock = world.getBlockAt(x, highestY + 2, z);
        
        // Check ground is solid
        if (requireSolid && !groundBlock.getType().isSolid()) {
            return null;
        }
        
        // Check not standing in liquid or dangerous block
        if (avoidLiquids) {
            if (groundBlock.isLiquid() || feetBlock.isLiquid() || headBlock.isLiquid()) {
                return null;
            }
        }
        
        // Check for dangerous blocks
        if (DANGEROUS_BLOCKS.contains(groundBlock.getType()) ||
            DANGEROUS_BLOCKS.contains(feetBlock.getType()) ||
            DANGEROUS_BLOCKS.contains(headBlock.getType())) {
            return null;
        }
        
        // Check space for player (2 blocks high)
        if (!feetBlock.isPassable() || !headBlock.isPassable()) {
            return null;
        }
        
        // Return safe location (center of block, on top of ground)
        return new Location(world, x + 0.5, highestY + 1, z + 0.5);
    }
}
