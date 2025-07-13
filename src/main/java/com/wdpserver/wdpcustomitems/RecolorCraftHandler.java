package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RecolorCraftHandler implements Listener {
    private final WdpCustomItems plugin;

    public RecolorCraftHandler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] contents = inv.getMatrix();

        ItemStack sword = null;
        Material dye = null;

        for (ItemStack item : contents) {
            if (item == null) continue;

            if (item.getType() == Material.DIAMOND_SWORD || item.getType() == Material.NETHERITE_SWORD) {
                if (item.hasItemMeta() && (item.getItemMeta().getPersistentDataContainer().has(plugin.diaBeamSwordKey, PersistentDataType.BYTE) || item.getItemMeta().getPersistentDataContainer().has(plugin.netheriteBeamSwordKey, PersistentDataType.BYTE))) {
                    sword = item;
                }
            } else if (item.getType().name().endsWith("_DYE")) {
                dye = item.getType();
            }
        }

        if (sword == null || dye == null) {
            // Not our recipe
            return;
        }
        int damage = plugin.getConfig().getInt("diamond-beam-sword.damage", 12);
        int knockback = plugin.getConfig().getInt("diamond-beam-sword.knockback", 3);
        long cooldown = plugin.getConfig().getLong("diamond-beam-sword.cooldown", 5);
        double range = plugin.getConfig().getDouble("diamond-beam-sword.range", 50);
        String dyeColor = getColorFromDye(dye);

        // Clone the sword to preserve NBT
        ItemStack newSword = plugin.createCustomBeamSword(damage, dyeColor, knockback, cooldown, range, Material.DIAMOND_SWORD);

        inv.setResult(newSword);
    }

    private String getColorFromDye(Material dye) {
        switch (dye) {
            case RED_DYE: return "RED";
            case BLUE_DYE: return "BLUE";
            case GREEN_DYE: return "GREEN";
            case YELLOW_DYE: return "YELLOW";
            case PURPLE_DYE: return "PURPLE";
            default: return "RED";
        }
    }
}
