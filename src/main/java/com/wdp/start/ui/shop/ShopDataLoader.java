package com.wdp.start.ui.shop;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.wdp.start.ui.menu.MenuUtils.prettifyMaterialName;

/**
 * Loads and caches shop data from SkillCoinsShop configuration files.
 * 
 * Directory structure:
 * - SkillCoinsShop/
 *   - sections/     (category definitions)
 *   - shops/        (item listings per category)
 */
public class ShopDataLoader {
    
    private final WDPStartPlugin plugin;
    private final File shopBaseDir;
    private final File sectionsDir;
    private final File shopsDir;
    
    // Cached section data
    private List<ShopSection> cachedSections;
    private long lastSectionLoad = 0;
    
    // Cached shop items per category
    private final Map<String, List<ShopItemData>> cachedItems = new HashMap<>();
    
    public ShopDataLoader(WDPStartPlugin plugin) {
        this.plugin = plugin;
        this.shopBaseDir = new File(plugin.getDataFolder(), "SkillCoinsShop");
        this.sectionsDir = new File(shopBaseDir, "sections");
        this.shopsDir = new File(shopBaseDir, "shops");
    }
    
    /**
     * Check if shop directory exists
     */
    public boolean isAvailable() {
        return shopBaseDir.exists() && shopBaseDir.isDirectory();
    }
    
    /**
     * Reload all cached data
     */
    public void reload() {
        cachedSections = null;
        cachedItems.clear();
        lastSectionLoad = 0;
    }
    
    // ==================== SECTIONS ====================
    
    /**
     * Get all shop sections, sorted by slot
     */
    public List<ShopSection> getSections() {
        // Refresh cache every 60 seconds
        if (cachedSections != null && System.currentTimeMillis() - lastSectionLoad < 60000) {
            return cachedSections;
        }
        
        cachedSections = loadSections();
        lastSectionLoad = System.currentTimeMillis();
        return cachedSections;
    }
    
    /**
     * Load sections from files
     */
    private List<ShopSection> loadSections() {
        List<ShopSection> sections = new ArrayList<>();
        
        if (!sectionsDir.exists() || !sectionsDir.isDirectory()) {
            plugin.debug("Sections directory not found: " + sectionsDir.getPath());
            return sections;
        }
        
        File[] files = sectionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return sections;
        
        for (File file : files) {
            try {
                ShopSection section = loadSection(file);
                if (section != null && section.enabled()) {
                    sections.add(section);
                }
            } catch (Exception e) {
                plugin.debug("Failed to load section: " + file.getName() + " - " + e.getMessage());
            }
        }
        
        // Sort by slot
        sections.sort(Comparator.comparingInt(ShopSection::slot));
        
        return sections;
    }
    
    /**
     * Load a single section from file
     */
    private ShopSection loadSection(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        
        String id = file.getName().replace(".yml", "");
        boolean enabled = cfg.getBoolean("enable", true);
        String displayName = cfg.getString("displayname", id);
        int slot = cfg.getInt("slot", -1);
        String materialStr = cfg.getString("material", "STONE");
        
        Material icon = Material.matchMaterial(materialStr.toUpperCase());
        if (icon == null) icon = Material.STONE;
        
        return new ShopSection(id, displayName, slot, icon, enabled);
    }
    
    /**
     * Get section by ID
     */
    public ShopSection getSection(String id) {
        for (ShopSection section : getSections()) {
            if (section.id().equalsIgnoreCase(id)) {
                return section;
            }
        }
        return null;
    }
    
    /**
     * Get section by slot
     */
    public ShopSection getSectionBySlot(int slot) {
        for (ShopSection section : getSections()) {
            if (section.slot() == slot) {
                return section;
            }
        }
        return null;
    }
    
    // ==================== ITEMS ====================
    
    /**
     * Get items for a category
     */
    public List<ShopItemData> getItems(String category) {
        // Check cache first
        String cacheKey = category.toLowerCase();
        if (cachedItems.containsKey(cacheKey)) {
            return cachedItems.get(cacheKey);
        }
        
        // Try to load from shop file
        List<ShopItemData> items = loadShopItems(category);
        
        // If no items from file, use fallback
        if (items.isEmpty()) {
            items = getFallbackItems(category);
        }
        
        cachedItems.put(cacheKey, items);
        return items;
    }
    
    /**
     * Load items from shop file
     */
    private List<ShopItemData> loadShopItems(String category) {
        File shopFile = findShopFile(category);
        if (shopFile == null || !shopFile.exists()) {
            return Collections.emptyList();
        }
        
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        if (!cfg.contains("pages")) {
            return Collections.emptyList();
        }
        
        // Find first page with items
        String firstPage = findFirstPage(cfg);
        if (firstPage == null || !cfg.contains("pages." + firstPage + ".items")) {
            return Collections.emptyList();
        }
        
        List<ShopItemData> items = new ArrayList<>();
        var itemsSection = cfg.getConfigurationSection("pages." + firstPage + ".items");
        if (itemsSection == null) return items;
        
        // Sort keys numerically
        List<String> keys = new ArrayList<>(itemsSection.getKeys(false));
        keys.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        
        for (String key : keys) {
            var itemCfg = itemsSection.getConfigurationSection(key);
            if (itemCfg == null) continue;
            
            String materialStr = itemCfg.getString("material");
            if (materialStr == null) continue;
            
            Material mat = Material.getMaterial(materialStr.toUpperCase());
            if (mat == null) continue;
            
            double buy = itemCfg.getDouble("buy", 0);
            double sell = itemCfg.getDouble("sell", 0);
            String name = prettifyMaterialName(mat);
            
            items.add(new ShopItemData(mat, name, buy, sell, sell > 0));
        }
        
        return items;
    }
    
    /**
     * Find the shop file for a category (handles name variations)
     */
    private File findShopFile(String category) {
        if (!shopsDir.exists() || !shopsDir.isDirectory()) {
            return null;
        }
        
        // Try exact match first
        File file = new File(shopsDir, category + ".yml");
        if (file.exists()) return file;
        
        // Try common variations
        List<String> variants = Arrays.asList(
            category,
            category.replace(" ", ""),
            category.toLowerCase(),
            category.substring(0, 1).toUpperCase() + category.substring(1),
            category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase()
        );
        
        for (String variant : variants) {
            file = new File(shopsDir, variant + ".yml");
            if (file.exists()) return file;
        }
        
        return null;
    }
    
    /**
     * Find the first page key in a shop config
     */
    private String findFirstPage(YamlConfiguration cfg) {
        if (cfg.contains("pages.page1.items")) {
            return "page1";
        }
        
        var pagesSection = cfg.getConfigurationSection("pages");
        if (pagesSection == null) return null;
        
        for (String key : pagesSection.getKeys(false)) {
            if (cfg.contains("pages." + key + ".items")) {
                return key;
            }
        }
        
        return null;
    }
    
    /**
     * Get fallback items for common categories
     */
    private List<ShopItemData> getFallbackItems(String category) {
        List<ShopItemData> items = new ArrayList<>();
        
        switch (category.toLowerCase()) {
            case "food":
                items.add(ShopItemData.buyAndSell(Material.APPLE, "Apple", 10, 5));
                items.add(ShopItemData.buyAndSell(Material.BREAD, "Bread", 15, 8));
                items.add(ShopItemData.buyAndSell(Material.COOKED_BEEF, "Steak", 25, 12));
                items.add(ShopItemData.buyAndSell(Material.COOKED_PORKCHOP, "Cooked Porkchop", 25, 12));
                items.add(ShopItemData.buyAndSell(Material.GOLDEN_APPLE, "Golden Apple", 500, 250));
                items.add(ShopItemData.buyAndSell(Material.COOKED_CHICKEN, "Cooked Chicken", 20, 10));
                items.add(ShopItemData.buyAndSell(Material.BAKED_POTATO, "Baked Potato", 12, 6));
                items.add(ShopItemData.buyAndSell(Material.COOKED_MUTTON, "Cooked Mutton", 22, 11));
                break;
                
            case "tools":
                items.add(ShopItemData.buyAndSell(Material.WOODEN_PICKAXE, "Wooden Pickaxe", 20, 5));
                items.add(ShopItemData.buyAndSell(Material.STONE_PICKAXE, "Stone Pickaxe", 50, 15));
                items.add(ShopItemData.buyAndSell(Material.IRON_PICKAXE, "Iron Pickaxe", 150, 50));
                items.add(ShopItemData.buyAndSell(Material.WOODEN_AXE, "Wooden Axe", 20, 5));
                items.add(ShopItemData.buyAndSell(Material.STONE_AXE, "Stone Axe", 50, 15));
                items.add(ShopItemData.buyAndSell(Material.IRON_AXE, "Iron Axe", 140, 45));
                items.add(ShopItemData.buyAndSell(Material.WOODEN_SHOVEL, "Wooden Shovel", 18, 4));
                items.add(ShopItemData.buyAndSell(Material.STONE_SHOVEL, "Stone Shovel", 45, 12));
                items.add(ShopItemData.buyAndSell(Material.IRON_SHOVEL, "Iron Shovel", 130, 40));
                break;
                
            case "resources":
                items.add(ShopItemData.buyAndSell(Material.OAK_LOG, "Oak Log", 5, 2));
                items.add(ShopItemData.buyAndSell(Material.COBBLESTONE, "Cobblestone", 2, 1));
                items.add(ShopItemData.buyAndSell(Material.IRON_INGOT, "Iron Ingot", 100, 50));
                items.add(ShopItemData.buyAndSell(Material.COAL, "Coal", 10, 5));
                items.add(ShopItemData.buyAndSell(Material.COPPER_INGOT, "Copper Ingot", 30, 15));
                items.add(ShopItemData.buyAndSell(Material.GOLD_INGOT, "Gold Ingot", 150, 75));
                items.add(ShopItemData.buyAndSell(Material.REDSTONE, "Redstone", 20, 10));
                items.add(ShopItemData.buyAndSell(Material.LAPIS_LAZULI, "Lapis Lazuli", 25, 12));
                break;
                
            case "blocks":
                items.add(ShopItemData.buyAndSell(Material.STONE, "Stone", 3, 1));
                items.add(ShopItemData.buyAndSell(Material.OAK_PLANKS, "Oak Planks", 3, 1));
                items.add(ShopItemData.buyAndSell(Material.GLASS, "Glass", 10, 5));
                items.add(ShopItemData.buyAndSell(Material.TORCH, "Torch", 5, 2));
                items.add(ShopItemData.buyAndSell(Material.DIRT, "Dirt", 2, 1));
                items.add(ShopItemData.buyAndSell(Material.SAND, "Sand", 4, 2));
                items.add(ShopItemData.buyAndSell(Material.GRAVEL, "Gravel", 4, 2));
                items.add(ShopItemData.buyAndSell(Material.BRICK, "Brick", 8, 4));
                break;
        }
        
        return items;
    }
    
    /**
     * Get item count for a category
     */
    public int getItemCount(String category) {
        return getItems(category).size();
    }
    
    // ==================== SECTION RECORD ====================
    
    /**
     * Represents a shop section/category
     */
    public record ShopSection(
        String id,
        String displayName,
        int slot,
        Material icon,
        boolean enabled
    ) {
        /**
         * Check if this is a special section (tokens, skilllevels)
         */
        public boolean isSpecial() {
            String lower = id.toLowerCase();
            return lower.contains("skilllevels") || 
                   lower.contains("tokens") ||
                   lower.contains("token_exchange");
        }
        
        /**
         * Check if this is the token exchange section
         */
        public boolean isTokenExchange() {
            String lower = id.toLowerCase();
            return lower.contains("token_exchange") || lower.contains("token exchange");
        }
    }
}
