package com.wdp.start.path;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

/**
 * A* Pathfinding implementation for finding the shortest walkable path
 * between two locations. Considers terrain, obstacles, and height changes.
 */
public class AStarPathfinder {
    
    // Maximum path distance (to prevent infinite searches)
    private static final int MAX_ITERATIONS = 5000;
    
    // Height tolerance for path finding
    private static final int MAX_FALL_HEIGHT = 3;
    private static final int MAX_JUMP_HEIGHT = 1;
    
    // Directions for neighbors (8-directional + vertical)
    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // Cardinal
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}  // Diagonal
    };
    
    /**
     * Find the shortest path from start to end using A* algorithm
     * @param start Starting location
     * @param end Target location
     * @param maxDistance Maximum path length
     * @return List of locations representing the path, or empty if no path found
     */
    public static List<Location> findPath(Location start, Location end, int maxDistance) {
        if (start == null || end == null || start.getWorld() == null) {
            return Collections.emptyList();
        }
        
        World world = start.getWorld();
        
        // Convert to path nodes
        PathNode startNode = new PathNode(start.getBlockX(), getWalkableY(world, start.getBlockX(), start.getBlockY(), start.getBlockZ()), start.getBlockZ());
        PathNode endNode = new PathNode(end.getBlockX(), getWalkableY(world, end.getBlockX(), end.getBlockY(), end.getBlockZ()), end.getBlockZ());
        
        // Check if already close enough
        double directDistance = Math.sqrt(
            Math.pow(endNode.x - startNode.x, 2) + 
            Math.pow(endNode.z - startNode.z, 2)
        );
        
        if (directDistance < 3) {
            return Collections.emptyList(); // Too close
        }
        
        // A* algorithm
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Long, PathNode> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();
        
        startNode.gCost = 0;
        startNode.hCost = heuristic(startNode, endNode);
        startNode.fCost = startNode.hCost;
        
        openSet.add(startNode);
        allNodes.put(startNode.getKey(), startNode);
        
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            
            PathNode current = openSet.poll();
            
            // Check if we reached destination (within 2 blocks)
            double distToEnd = Math.sqrt(
                Math.pow(endNode.x - current.x, 2) + 
                Math.pow(endNode.z - current.z, 2)
            );
            
            if (distToEnd < 2) {
                return reconstructPath(current, world, maxDistance);
            }
            
            closedSet.add(current.getKey());
            
            // Check all neighbors
            for (int[] dir : DIRECTIONS) {
                int newX = current.x + dir[0];
                int newZ = current.z + dir[1];
                
                // Find walkable Y at this position
                int newY = findWalkableY(world, newX, current.y, newZ);
                if (newY == Integer.MIN_VALUE) {
                    continue; // Not walkable
                }
                
                // Check height difference
                int heightDiff = newY - current.y;
                if (heightDiff > MAX_JUMP_HEIGHT || heightDiff < -MAX_FALL_HEIGHT) {
                    continue; // Too high/low
                }
                
                PathNode neighbor = new PathNode(newX, newY, newZ);
                long neighborKey = neighbor.getKey();
                
                if (closedSet.contains(neighborKey)) {
                    continue;
                }
                
                // Calculate movement cost (diagonal costs more)
                double moveCost = (dir[0] != 0 && dir[1] != 0) ? 1.414 : 1.0;
                moveCost += Math.abs(heightDiff) * 0.5; // Height changes cost more
                
                double tentativeG = current.gCost + moveCost;
                
                PathNode existingNode = allNodes.get(neighborKey);
                
                if (existingNode == null) {
                    neighbor.gCost = tentativeG;
                    neighbor.hCost = heuristic(neighbor, endNode);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    neighbor.parent = current;
                    
                    openSet.add(neighbor);
                    allNodes.put(neighborKey, neighbor);
                } else if (tentativeG < existingNode.gCost) {
                    openSet.remove(existingNode);
                    existingNode.gCost = tentativeG;
                    existingNode.fCost = existingNode.gCost + existingNode.hCost;
                    existingNode.parent = current;
                    openSet.add(existingNode);
                }
            }
        }
        
        // No path found - return partial path to closest point
        PathNode closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (PathNode node : allNodes.values()) {
            double dist = heuristic(node, endNode);
            if (dist < closestDist) {
                closestDist = dist;
                closest = node;
            }
        }
        
        if (closest != null && closest.parent != null) {
            return reconstructPath(closest, world, maxDistance);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Calculate heuristic (estimated cost to target)
     */
    private static double heuristic(PathNode a, PathNode b) {
        // Use Euclidean distance for more accurate estimation
        return Math.sqrt(
            Math.pow(b.x - a.x, 2) + 
            Math.pow(b.y - a.y, 2) * 0.5 + // Reduce Y weight
            Math.pow(b.z - a.z, 2)
        );
    }
    
    /**
     * Find a walkable Y coordinate starting from a given Y
     */
    private static int findWalkableY(World world, int x, int startY, int z) {
        // Search up and down from startY
        for (int offset = 0; offset <= MAX_FALL_HEIGHT + MAX_JUMP_HEIGHT; offset++) {
            // Check below
            int yBelow = startY - offset;
            if (yBelow > world.getMinHeight() && isWalkable(world, x, yBelow, z)) {
                return yBelow;
            }
            
            // Check above
            int yAbove = startY + offset;
            if (yAbove < world.getMaxHeight() - 2 && isWalkable(world, x, yAbove, z)) {
                return yAbove;
            }
        }
        
        return Integer.MIN_VALUE;
    }
    
    /**
     * Get walkable Y from location
     */
    private static int getWalkableY(World world, int x, int y, int z) {
        int result = findWalkableY(world, x, y, z);
        return result == Integer.MIN_VALUE ? y : result;
    }
    
    /**
     * Check if a position is walkable (solid ground with 2 air blocks above)
     */
    private static boolean isWalkable(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y - 1, z);
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        
        // Ground must be solid
        if (!ground.getType().isSolid()) {
            return false;
        }
        
        // Check for dangerous blocks
        if (isDangerous(ground.getType())) {
            return false;
        }
        
        // Feet and head space must be passable
        if (!isPassable(feet.getType()) || !isPassable(head.getType())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a material is passable (can walk through)
     */
    private static boolean isPassable(Material mat) {
        return mat.isAir() || 
               mat == Material.LIGHT ||
               mat == Material.WATER ||
               mat == Material.SHORT_GRASS ||
               mat == Material.TALL_GRASS ||
               mat == Material.FERN ||
               mat == Material.LARGE_FERN ||
               mat == Material.DEAD_BUSH ||
               mat == Material.TORCH ||
               mat == Material.WALL_TORCH ||
               mat == Material.REDSTONE_TORCH ||
               mat == Material.REDSTONE_WALL_TORCH ||
               mat.name().contains("FLOWER") ||
               mat.name().contains("CARPET") ||
               mat.name().contains("PRESSURE_PLATE") ||
               mat.name().contains("SIGN") ||
               mat.name().contains("BANNER");
    }
    
    /**
     * Check if material is dangerous
     */
    private static boolean isDangerous(Material mat) {
        return mat == Material.LAVA ||
               mat == Material.FIRE ||
               mat == Material.SOUL_FIRE ||
               mat == Material.CAMPFIRE ||
               mat == Material.SOUL_CAMPFIRE ||
               mat == Material.MAGMA_BLOCK ||
               mat == Material.CACTUS ||
               mat == Material.SWEET_BERRY_BUSH ||
               mat == Material.WITHER_ROSE ||
               mat == Material.POINTED_DRIPSTONE;
    }
    
    /**
     * Reconstruct the path from end node to start
     */
    private static List<Location> reconstructPath(PathNode end, World world, int maxLength) {
        List<Location> path = new ArrayList<>();
        PathNode current = end;
        
        while (current != null && path.size() < maxLength) {
            path.add(new Location(world, current.x + 0.5, current.y, current.z + 0.5));
            current = current.parent;
        }
        
        Collections.reverse(path);
        return path;
    }
    
    /**
     * Inner class representing a path node
     */
    private static class PathNode {
        final int x, y, z;
        double gCost = Double.MAX_VALUE; // Cost from start
        double hCost = 0; // Heuristic cost to end
        double fCost = Double.MAX_VALUE; // Total cost
        PathNode parent = null;
        
        PathNode(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        long getKey() {
            // Unique key for this position
            return ((long) x & 0xFFFFFFL) | (((long) y & 0xFFFL) << 24) | (((long) z & 0xFFFFFFL) << 36);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathNode)) return false;
            PathNode other = (PathNode) o;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(getKey());
        }
    }
}
