package com.wdpserver.wdpcustomitems;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BeamSword {
    public int damage;
    public Color color;
    public double knockback;

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
    }
}
