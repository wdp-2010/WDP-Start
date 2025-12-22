package com.wdp.start.path;

import com.wdp.start.WDPStartPlugin;
import com.wdp.start.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages particle path guides for Quest 1
 * Shows totem particles along the shortest WALKABLE path to the target using A* pathfinding
 */
public class PathGuideManager {
    
    private final WDPStartPlugin plugin;
    private final Map<UUID, PathSession> activePaths = new HashMap<>();
    private BukkitTask animationTask;
    
    // Cache for path calculations (to avoid recalculating every tick)
    private final Map<UUID, Long> lastPathCalculation = new HashMap<>();
    private static final long PATH_RECALC_COOLDOWN = 2000; // 2 seconds
    
    public PathGuideManager(WDPStartPlugin plugin) {
        this.plugin = plugin;
        startAnimationTask();
    }
    
    /**
     * Start showing path for a player
     */
    public void startPath(Player player) {
        if (!plugin.getConfigManager().isPathEnabled()) {
            plugin.debug("[PathGuide] Path disabled in config, not starting for " + player.getName());
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        plugin.debug("[PathGuide] Starting A* pathfinding for " + player.getName() + " at " + 
            String.format("(%.1f, %.1f, %.1f)", 
                player.getLocation().getX(), 
                player.getLocation().getY(), 
                player.getLocation().getZ()));
        
        // Stop existing path
        stopPath(player);
        
        // Calculate initial path using A* algorithm
        List<Location> path = calculatePathAStar(player.getLocation());
        if (path.isEmpty()) {
            plugin.debug("[PathGuide] A* pathfinding failed for " + player.getName() + " - trying fallback");
            // Fallback to simple path if A* fails
            path = calculateSimplePath(player.getLocation());
        }
        
        if (path.isEmpty()) {
            plugin.debug("[PathGuide] Failed to calculate any path for " + player.getName());
            return;
        }
        
        PathSession session = new PathSession(player, path);
        activePaths.put(uuid, session);
        lastPathCalculation.put(uuid, System.currentTimeMillis());
        
        plugin.debug("[PathGuide] Path started for " + player.getName() + " with " + path.size() + " points (A*)");
    }
    
    /**
     * Stop showing path for a player
     */
    public void stopPath(Player player) {
        PathSession session = activePaths.remove(player.getUniqueId());
        if (session != null) {
            plugin.debug("[PathGuide] Stopped path guide for " + player.getName());
        }
    }
    
    /**
     * Check if player has active path
     */
    public boolean hasActivePath(Player player) {
        return activePaths.containsKey(player.getUniqueId());
    }
    
    /**
     * Calculate shortest WALKABLE path using A* algorithm
     * Always shows the full path to target when show-full-path is enabled
     */
    private List<Location> calculatePathAStar(Location from) {
        Location target = plugin.getConfigManager().getPathTarget();
        if (target == null) {
            plugin.debug("[PathGuide] Cannot calculate A* path - target is null");
            return Collections.emptyList();
        }
        if (from.getWorld() == null) {
            plugin.debug("[PathGuide] Cannot calculate A* path - player world is null");
            return Collections.emptyList();
        }
        
        int pathLength = plugin.getConfigManager().getPathLength();
        boolean showFullPath = plugin.getConfigManager().isShowFullPath();
        
        // If show full path is enabled, calculate distance and use that as max
        if (showFullPath) {
            double distance = from.distance(target);
            pathLength = Math.max(pathLength, (int) (distance * 2)); // Ensure we can reach target
        }
        
        plugin.debug("[PathGuide] Calculating A* path from " + 
            String.format("(%.1f, %.1f, %.1f) to (%.1f, %.1f, %.1f) [full path: %s]",
                from.getX(), from.getY(), from.getZ(),
                target.getX(), target.getY(), target.getZ(),
                showFullPath));
        
        // Use A* pathfinder with larger search radius
        List<Location> path = AStarPathfinder.findPath(from, target, pathLength * 3);
        
        if (path.isEmpty()) {
            plugin.debug("[PathGuide] A* returned empty path");
            return Collections.emptyList();
        }
        
        // Add particle height to all path points - NO length limit if showFullPath
        double particleHeight = plugin.getConfigManager().getParticleHeight();
        List<Location> adjustedPath = new ArrayList<>();
        
        int maxPoints = showFullPath ? path.size() : Math.min(path.size(), pathLength);
        
        for (int i = 0; i < maxPoints; i++) {
            Location loc = path.get(i).clone();
            loc.setY(loc.getY() + particleHeight);
            adjustedPath.add(loc);
        }
        
        plugin.debug("[PathGuide] A* path calculated with " + adjustedPath.size() + " points (full path: " + showFullPath + ")");
        return adjustedPath;
    }
    
    /**
     * Fallback: Calculate simple path (terrain-following straight line)
     * Also respects show-full-path setting
     */
    private List<Location> calculateSimplePath(Location from) {
        Location target = plugin.getConfigManager().getPathTarget();
        if (target == null || from.getWorld() == null) {
            return Collections.emptyList();
        }
        
        World world = from.getWorld();
        double dx = target.getX() - from.getX();
        double dz = target.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance < 3) {
            return Collections.emptyList();
        }
        
        int pathLength = plugin.getConfigManager().getPathLength();
        boolean showFullPath = plugin.getConfigManager().isShowFullPath();
        
        double stepSize = 1.5;
        int maxSteps = showFullPath ? (int) (distance / stepSize) : Math.min((int) (distance / stepSize), pathLength);
        
        dx /= distance;
        dz /= distance;
        
        List<Location> path = new ArrayList<>();
        double particleHeight = plugin.getConfigManager().getParticleHeight();
        
        for (int i = 1; i <= maxSteps; i++) {
            double x = from.getX() + dx * i * stepSize;
            double z = from.getZ() + dz * i * stepSize;
            int y = findGroundLevel(world, (int) x, (int) from.getY(), (int) z);
            
            Location point = new Location(world, x, y + particleHeight, z);
            path.add(point);
        }
        
        return path;
    }
    
    /**
     * Find the ground level at a position
     */
    private int findGroundLevel(World world, int x, int startY, int z) {
        // Search up and down from start Y
        for (int offset = 0; offset < 10; offset++) {
            // Check below
            int yBelow = startY - offset;
            if (yBelow > world.getMinHeight()) {
                Block block = world.getBlockAt(x, yBelow, z);
                Block above = world.getBlockAt(x, yBelow + 1, z);
                if (block.getType().isSolid() && !above.getType().isSolid()) {
                    return yBelow + 1;
                }
            }
            
            // Check above
            int yAbove = startY + offset;
            if (yAbove < world.getMaxHeight()) {
                Block block = world.getBlockAt(x, yAbove, z);
                Block above = world.getBlockAt(x, yAbove + 1, z);
                if (block.getType().isSolid() && !above.getType().isSolid()) {
                    return yAbove + 1;
                }
            }
        }
        
        return startY;
    }
    
    /**
     * Start the animation task
     */
    private void startAnimationTask() {
        int speed = plugin.getConfigManager().getAnimationSpeed();
        
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, PathSession>> iter = activePaths.entrySet().iterator();
            
            while (iter.hasNext()) {
                Map.Entry<UUID, PathSession> entry = iter.next();
                PathSession session = entry.getValue();
                Player player = session.getPlayer();
                
                // Check if player is still valid
                if (!player.isOnline()) {
                    plugin.debug("[PathGuide] Removing path for " + player.getName() + " - player offline");
                    iter.remove();
                    continue;
                }
                
                // Check if still on Quest 1
                PlayerData data = plugin.getPlayerDataManager().getData(player);
                if (!data.isStarted() || data.getCurrentQuest() != 1 || data.isQuestCompleted(1)) {
                    plugin.debug("[PathGuide] Removing path for " + player.getName() + " - no longer on Quest 1");
                    iter.remove();
                    continue;
                }
                
                // Animate and check if path ended
                boolean pathEnded = session.animate();
                
                if (pathEnded) {
                    // Check cooldown before recalculating
                    long now = System.currentTimeMillis();
                    Long lastCalc = lastPathCalculation.get(entry.getKey());
                    
                    if (lastCalc == null || now - lastCalc > PATH_RECALC_COOLDOWN) {
                        plugin.debug("[PathGuide] Path ended for " + player.getName() + ", recalculating with A*...");
                        
                        // Recalculate path using A*
                        List<Location> newPath = calculatePathAStar(player.getLocation());
                        if (newPath.isEmpty()) {
                            newPath = calculateSimplePath(player.getLocation());
                        }
                        
                        if (newPath.isEmpty()) {
                            plugin.debug("[PathGuide] Cannot recalculate path for " + player.getName() + " - removing");
                            iter.remove();
                        } else {
                            plugin.debug("[PathGuide] Path recalculated for " + player.getName() + " with " + newPath.size() + " points (A*)");
                            session.resetPath(newPath);
                            lastPathCalculation.put(entry.getKey(), now);
                        }
                    } else {
                        // Just restart from beginning
                        session.restartAnimation();
                    }
                }
            }
        }, 0L, speed);
    }
    
    /**
     * Shutdown the manager
     */
    public void shutdown() {
        plugin.debug("[PathGuide] Shutting down PathGuideManager, removing " + activePaths.size() + " active paths");
        if (animationTask != null) {
            animationTask.cancel();
        }
        activePaths.clear();
        lastPathCalculation.clear();
    }
    
    /**
     * Inner class to track path animation state
     */
    private class PathSession {
        private final Player player;
        private List<Location> path;
        private int currentIndex;
        
        public PathSession(Player player, List<Location> path) {
            this.player = player;
            this.path = path;
            this.currentIndex = 0;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public void resetPath(List<Location> newPath) {
            this.path = newPath;
            this.currentIndex = 0;
            plugin.debug("[PathGuide] Path reset for " + player.getName() + " with " + newPath.size() + " points");
        }
        
        public void restartAnimation() {
            this.currentIndex = 0;
        }
        
        /**
         * Animate the path particles
         * @return true if path has ended and needs recalculation
         */
        public boolean animate() {
            if (path.isEmpty()) {
                plugin.debug("[PathGuide] Path empty for " + player.getName());
                return true;
            }
            
            // Show particle at current position
            Location loc = path.get(currentIndex);
            
            Particle particle;
            try {
                particle = Particle.valueOf(plugin.getConfigManager().getParticleType());
            } catch (Exception e) {
                plugin.debug("[PathGuide] Invalid particle type '" + plugin.getConfigManager().getParticleType() + "', using TOTEM_OF_UNDYING");
                particle = Particle.TOTEM_OF_UNDYING;
            }
            
            int count = plugin.getConfigManager().getParticleCount();
            
            // Spawn particle
            try {
                player.spawnParticle(particle, loc, count, 0.1, 0.1, 0.1, 0.01);
            } catch (Exception e) {
                plugin.debug("[PathGuide] Failed to spawn particle for " + player.getName() + ": " + e.getMessage());
            }
            
            // Move to next position
            currentIndex++;
            
            // Check if we've reached the end
            if (currentIndex >= path.size()) {
                plugin.debug("[PathGuide] Reached end of path for " + player.getName() + " (" + currentIndex + "/" + path.size() + ")");
                return true; // Path ended, needs recalculation
            }
            
            return false;
        }
    }
}
