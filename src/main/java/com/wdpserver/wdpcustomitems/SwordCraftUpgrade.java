package com.wdpserver.wdpcustomitems;


import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class SwordCraftUpgrade implements Listener {
    private final WdpCustomItems plugin;

    public SwordCraftUpgrade(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareUpgradeCraft(PrepareItemCraftEvent event ) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] content = inv.getMatrix();

        ItemStack sword = null;
        Material template = null;
        Material netherite = null;

        for (ItemStack item : content) {
            if (item == null) continue;
            if (item.getType() == Material.DIAMOND_SWORD){
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.diaBeamSwordKey, PersistentDataType.BYTE)){
                    sword = item;

                }
            } else if (item.getType() == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
                template = item.getType();
            } else if (item.getType() == Material.NETHERITE_INGOT) {
                netherite = item.getType();
            }
        }

        if (sword == null || template == null || netherite == null) return;

        int damage = plugin.getConfig().getInt("netherite-beam-sword.damage", 20);
        String color = plugin.getConfig().getString("netherite-beam-sword.color", "DARK_PURPLE");
        double knockback = plugin.getConfig().getDouble("netherite-beam-sword.knockback", 5);
        long cooldown = plugin.getConfig().getLong("netherite-beam-sword.cooldown", 4);
        double range = plugin.getConfig().getDouble("netherite-beam-sword.range", 60);


        ItemStack upgradedSword = plugin.createCustomBeamSword(damage, color, knockback, cooldown, range, Material.NETHERITE_SWORD);
        ItemMeta meta = upgradedSword.getItemMeta();
        // 1. Get the enchantment map from old item
        Map<Enchantment, Integer> oldEnchants = sword.getItemMeta().getEnchants();


        oldEnchants.forEach((enchant, level) -> {
            meta.addEnchant(enchant, level, true); // true = ignore level limits if needed
        });

        upgradedSword.setItemMeta(meta);

        inv.setResult(upgradedSword);
    }
}
