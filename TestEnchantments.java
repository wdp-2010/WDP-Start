import com.wdp.start.ui.shop.ShopDataLoader;
import com.wdp.start.ui.shop.ShopItemData;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Simple test to verify enchantment loading and display
 */
public class TestEnchantments {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Testing enchantment loading...");
        
        // Create a mock plugin
        JavaPlugin mockPlugin = new JavaPlugin() {
            @Override
            public File getDataFolder() {
                return new File("./target/test-classes");
            }
        };
        
        // Create shop directories
        File shopBaseDir = new File(mockPlugin.getDataFolder(), "SkillCoinsShop");
        File sectionsDir = new File(shopBaseDir, "sections");
        File shopsDir = new File(shopBaseDir, "shops");
        
        sectionsDir.mkdirs();
        shopsDir.mkdirs();
        
        // Copy test files
        copyResource("/SkillCoinsShop/sections/Enchanting.yml", new File(sectionsDir, "Enchanting.yml"));
        copyResource("/SkillCoinsShop/shops/Enchanting.yml", new File(shopsDir, "Enchanting.yml"));
        
        // Create ShopDataLoader
        ShopDataLoader loader = new ShopDataLoader(mockPlugin);
        
        // Test loading enchanting items
        List<ShopItemData> items = loader.getItems("Enchanting");
        
        System.out.println("Loaded " + items.size() + " items from Enchanting category");
        
        // Check for items with enchantments
        int enchantedCount = 0;
        for (ShopItemData item : items) {
            if (item.hasEnchantments()) {
                enchantedCount++;
                System.out.println("\nItem: " + item.name());
                System.out.println("Material: " + item.material());
                System.out.println("Enchantments:");
                for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                    System.out.println("  - " + entry.getKey().getKey() + " " + entry.getValue());
                }
            }
        }
        
        System.out.println("\nFound " + enchantedCount + " items with enchantments");
        
        if (enchantedCount > 0) {
            System.out.println("✅ Enchantment loading test PASSED");
        } else {
            System.out.println("❌ Enchantment loading test FAILED - no enchanted items found");
        }
    }
    
    private static void copyResource(String resourcePath, File destination) throws Exception {
        // Simple file copy for testing
        java.nio.file.Files.copy(
            new java.io.File("./src/main/resources" + resourcePath).toPath(),
            destination.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
    }
}