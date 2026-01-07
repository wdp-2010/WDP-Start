package com.wdp.start.shop;

import com.wdp.start.WDPStartPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads shop sections and items from YAML configuration files
 */
public class ShopLoader {
    
    private final WDPStartPlugin plugin;
    private final List<ShopSection> sections = new ArrayList<>();
    private final Map<String, List<ShopItem>> itemCache = new HashMap<>();
    
    public ShopLoader(WDPStartPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all sections and items from YAML files
     */
    public void load() {
        sections.clear();
        itemCache.clear();
        
        File sectionsDir = new File(plugin.getDataFolder(), "SkillCoinsShop/sections");
        if (!sectionsDir.exists() || !sectionsDir.isDirectory()) {
            plugin.getLogger().warning("SkillCoinsShop/sections directory not found!");
            return;
        }
        
        File[] sectionFiles = sectionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (sectionFiles == null || sectionFiles.length == 0) {
            plugin.getLogger().warning("No section files found in SkillCoinsShop/sections!");
            return;
        }
        
        for (File file : sectionFiles) {
            try {
                ShopSection section = loadSection(file);
                if (section != null && section.isEnabled()) {
                    sections.add(section);
                    
                    // Load items for this section
                    List<ShopItem> items = loadItemsForSection(section.getId());
                    section.setItems(items);
                    itemCache.put(section.getId().toLowerCase(), items);
                    
                    plugin.debug("[ShopLoader] Loaded section: " + section.getId() + 
                            " with " + items.size() + " items at slot " + section.getSlot());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading section: " + file.getName(), e);
            }
        }
        
        // Sort sections by slot
        sections.sort(Comparator.comparingInt(ShopSection::getSlot));
        
        plugin.getLogger().info("Loaded " + sections.size() + " shop sections with items.");
    }
    
    /**
     * Load a single section from a YAML file
     */
    private ShopSection loadSection(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            boolean enabled = config.getBoolean("enable", true);
            if (!enabled) return null;
            
            String id = file.getName().replace(".yml", "");
            // DON'T use displayname from YAML - it has unparsed legacy color codes
            // Instead use ID which will get proper colors from getDisplayColor()
            String displayName = id;
            
            // Handle slot (some configs use 1-based, we use 0-based internally)
            int slot = config.getInt("slot", -1);
            if (slot > 0) slot--; // Convert to 0-based
            
            String materialName = config.getString("item.material", 
                    config.getString("material", "STONE"));
            Material icon = Material.matchMaterial(materialName);
            if (icon == null) icon = Material.STONE;
            
            // Determine section type
            String lowerName = id.toLowerCase();
            boolean isTokenExchange = lowerName.contains("token_exchange") || lowerName.contains("token exchange");
            boolean isSkillLevels = lowerName.contains("skilllevels") || lowerName.contains("tokens");
            
            return new ShopSection(id, displayName, icon, slot, enabled, isTokenExchange, isSkillLevels);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing section file: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * Load items for a section from the corresponding shop file
     */
    private List<ShopItem> loadItemsForSection(String sectionId) {
        List<ShopItem> items = new ArrayList<>();
        
        File shopsDir = new File(plugin.getDataFolder(), "SkillCoinsShop/shops");
        if (!shopsDir.exists() || !shopsDir.isDirectory()) {
            return getFallbackItems(sectionId);
        }
        
        // Try to find the matching shop file
        File shopFile = findShopFile(shopsDir, sectionId);
        if (shopFile == null || !shopFile.exists()) {
            return getFallbackItems(sectionId);
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
            ConfigurationSection pages = config.getConfigurationSection("pages");
            
            if (pages == null) {
                return getFallbackItems(sectionId);
            }
            
            // Load all pages (not just page1)
            for (String pageKey : pages.getKeys(false)) {
                ConfigurationSection page = pages.getConfigurationSection(pageKey);
                if (page == null) continue;
                
                ConfigurationSection itemsSection = page.getConfigurationSection("items");
                if (itemsSection == null) continue;
                
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemKey);
                    if (itemConfig == null) continue;
                    
                    ShopItem item = parseShopItem(itemConfig);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading items for section: " + sectionId, e);
            return getFallbackItems(sectionId);
        }
        
        if (items.isEmpty()) {
            return getFallbackItems(sectionId);
        }
        
        return items;
    }
    
    /**
     * Parse a single shop item from config
     */
    private ShopItem parseShopItem(ConfigurationSection config) {
        try {
            String materialName = config.getString("material", "STONE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) return null;
            
            double buyPrice = config.getDouble("buy", -1);
            if (buyPrice < 0) return null; // Buy-only shop
            
            String displayName = config.getString("name", null);
            
            // Load enchantments if any
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            if (config.contains("enchants")) {
                ConfigurationSection enchants = config.getConfigurationSection("enchants");
                if (enchants != null) {
                    for (String enchKey : enchants.getKeys(false)) {
                        Enchantment ench = getEnchantment(enchKey);
                        if (ench != null) {
                            enchantments.put(ench, enchants.getInt(enchKey, 1));
                        }
                    }
                }
            }
            
            return new ShopItem(material, displayName, buyPrice, enchantments);
            
        } catch (Exception e) {
            plugin.debug("[ShopLoader] Error parsing item: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get an enchantment by name (handles various naming formats)
     */
    private Enchantment getEnchantment(String name) {
        try {
            // Try direct registry lookup first
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Enchantment ench = Registry.ENCHANTMENT.get(key);
            if (ench != null) return ench;
            
            // Try common aliases
            String normalized = name.toUpperCase().replace("-", "_").replace(" ", "_");
            return Enchantment.getByName(normalized);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Find the shop file for a section (handles various naming conventions)
     */
    private File findShopFile(File shopsDir, String sectionId) {
        // Try exact match first
        File exact = new File(shopsDir, sectionId + ".yml");
        if (exact.exists()) return exact;
        
        // Try case-insensitive match
        File[] files = shopsDir.listFiles((dir, name) -> 
                name.toLowerCase().equals(sectionId.toLowerCase() + ".yml"));
        if (files != null && files.length > 0) return files[0];
        
        // Try with spaces removed
        String noSpaces = sectionId.replace(" ", "");
        File noSpacesFile = new File(shopsDir, noSpaces + ".yml");
        if (noSpacesFile.exists()) return noSpacesFile;
        
        return null;
    }
    
    /**
     * Get fallback items if YAML loading fails
     */
    private List<ShopItem> getFallbackItems(String sectionId) {
        List<ShopItem> items = new ArrayList<>();
        String lower = sectionId.toLowerCase();
        
        if (lower.contains("food")) {
            items.add(new ShopItem(Material.APPLE, "Apple", 10));
            items.add(new ShopItem(Material.BREAD, "Bread", 15));
            items.add(new ShopItem(Material.COOKED_BEEF, "Steak", 25));
            items.add(new ShopItem(Material.COOKED_PORKCHOP, "Cooked Porkchop", 25));
            items.add(new ShopItem(Material.GOLDEN_APPLE, "Golden Apple", 500));
            items.add(new ShopItem(Material.COOKED_CHICKEN, "Cooked Chicken", 20));
            items.add(new ShopItem(Material.BAKED_POTATO, "Baked Potato", 12));
            items.add(new ShopItem(Material.COOKED_MUTTON, "Cooked Mutton", 22));
        } else if (lower.contains("tool")) {
            items.add(new ShopItem(Material.WOODEN_PICKAXE, "Wooden Pickaxe", 20));
            items.add(new ShopItem(Material.STONE_PICKAXE, "Stone Pickaxe", 50));
            items.add(new ShopItem(Material.IRON_PICKAXE, "Iron Pickaxe", 150));
            items.add(new ShopItem(Material.WOODEN_AXE, "Wooden Axe", 20));
            items.add(new ShopItem(Material.STONE_AXE, "Stone Axe", 50));
            items.add(new ShopItem(Material.IRON_AXE, "Iron Axe", 140));
        } else if (lower.contains("resource")) {
            items.add(new ShopItem(Material.OAK_LOG, "Oak Log", 5));
            items.add(new ShopItem(Material.COBBLESTONE, "Cobblestone", 2));
            items.add(new ShopItem(Material.IRON_INGOT, "Iron Ingot", 100));
            items.add(new ShopItem(Material.COAL, "Coal", 10));
            items.add(new ShopItem(Material.COPPER_INGOT, "Copper Ingot", 30));
            items.add(new ShopItem(Material.GOLD_INGOT, "Gold Ingot", 150));
        } else if (lower.contains("block")) {
            items.add(new ShopItem(Material.STONE, "Stone", 3));
            items.add(new ShopItem(Material.OAK_PLANKS, "Oak Planks", 3));
            items.add(new ShopItem(Material.GLASS, "Glass", 10));
            items.add(new ShopItem(Material.TORCH, "Torch", 5));
            items.add(new ShopItem(Material.DIRT, "Dirt", 2));
            items.add(new ShopItem(Material.SAND, "Sand", 4));
        }
        
        return items;
    }
    
    /**
     * Get all loaded sections
     */
    public List<ShopSection> getSections() {
        return new ArrayList<>(sections);
    }
    
    /**
     * Get sections available for a specific quest
     */
    public List<ShopSection> getSectionsForQuest(int questNumber) {
        List<ShopSection> available = new ArrayList<>();
        for (ShopSection section : sections) {
            if (section.isAvailableForQuest(questNumber)) {
                available.add(section);
            }
        }
        return available;
    }
    
    /**
     * Get a specific section by ID
     */
    public ShopSection getSection(String id) {
        for (ShopSection section : sections) {
            if (section.getId().equalsIgnoreCase(id)) {
                return section;
            }
        }
        return null;
    }
    
    /**
     * Get items for a section (cached)
     */
    public List<ShopItem> getItemsForSection(String sectionId) {
        return itemCache.getOrDefault(sectionId.toLowerCase(), Collections.emptyList());
    }
    
    /**
     * Reload all shop data
     */
    public void reload() {
        load();
    }
}
