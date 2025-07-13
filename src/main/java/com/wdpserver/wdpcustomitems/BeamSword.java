package com.wdpserver.wdpcustomitems;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BeamSword {
    public int damage;
    public Color color;
    public double knockback;
    public double range;
    public long cooldown;
    public boolean isBeamSword;


    public BeamSword(ItemStack item, WdpCustomItems plugin) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        this.damage = container.getOrDefault(plugin.beamDamageKey, PersistentDataType.INTEGER, 5);

        String colorName = container.getOrDefault(plugin.beamColorKey, PersistentDataType.STRING, "RED");
        try {
            this.color = (Color) Color.class.getField(colorName).get(null);
        } catch (Exception e) {
            this.color = Color.RED;
        }

        this.knockback = container.getOrDefault(plugin.beamKnockbackKey, PersistentDataType.DOUBLE, 1.5);

        this.range = container.getOrDefault(plugin.beamRangeKey, PersistentDataType.DOUBLE, 50.0);

        this.cooldown = container.getOrDefault(plugin.beamCooldownKey, PersistentDataType.LONG, (long) 5);

        if (item.getType() == Material.DIAMOND_SWORD || item.getType() == Material.NETHERITE_SWORD) {
            if (item.getItemMeta().getPersistentDataContainer().has(plugin.netheriteBeamSwordKey) || item.getItemMeta().getPersistentDataContainer().has(plugin.diaBeamSwordKey)) {
                isBeamSword = true;
            } else isBeamSword = false;
        } else isBeamSword = false;
    }
}
