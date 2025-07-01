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

            if (item.getType() == Material.DIAMOND_SWORD) {
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.beamSwordKey, PersistentDataType.BYTE)) {
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

        // Clone the sword to preserve NBT
        ItemStack newSword = sword.clone();
        ItemMeta meta = newSword.getItemMeta();

        String dyeColor = getColorFromDye(dye);

        // Update the color NBT
        meta.getPersistentDataContainer().set(plugin.beamColorKey, PersistentDataType.STRING, dyeColor);

        // Update lore
        meta.setLore(java.util.Arrays.asList(
                "§7Damage: §f" + meta.getPersistentDataContainer().get(plugin.beamDamageKey, PersistentDataType.INTEGER),
                "§7Color: §f" + dyeColor,
                "§7Knockback: §f" + meta.getPersistentDataContainer().get(plugin.beamKnockbackKey, PersistentDataType.DOUBLE)
        ));

        newSword.setItemMeta(meta);

        inv.setResult(newSword);
    }

    private String getColorFromDye(Material dye) {
        switch (dye) {
            case RED_DYE: return "RED";
            case BLUE_DYE: return "BLUE";
            case GREEN_DYE: return "GREEN";
            case YELLOW_DYE: return "YELLOW";
            case PURPLE_DYE: return "PURPLE";
            default: return "WHITE";
        }
    }
}
