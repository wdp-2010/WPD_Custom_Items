package com.wdpserver.wdpcustomitems;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * Handles applying a custom Ghast harness item to Ghasts and boosting their speed.
 */
public class GhastHarnessHandler implements Listener {
    private final NamespacedKey harnessItemKey;
    private final NamespacedKey harnessEntityKey;
    private final NamespacedKey speedModifierKey;

    /**
     * @param plugin             your main JavaPlugin instance
     * @param harnessItemKey     key used on the ItemStack to mark the harness
     * @param harnessEntityKey   key used on the Ghast to mark it as harnessed
     */
    public GhastHarnessHandler(JavaPlugin plugin, NamespacedKey harnessItemKey, NamespacedKey harnessEntityKey) {
        this.harnessItemKey = harnessItemKey;
        this.harnessEntityKey = harnessEntityKey;
        this.speedModifierKey = new NamespacedKey(plugin, "happy_ghast_harness_speed");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteractGhast(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Ghast ghast)) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) return;

        ItemMeta meta = inHand.getItemMeta();
        PersistentDataContainer dc = meta.getPersistentDataContainer();
        // Only proceed if this is our harness
        if (!dc.has(harnessItemKey, PersistentDataType.BYTE)) return;

        PersistentDataContainer edc = ghast.getPersistentDataContainer();
        if (edc.has(harnessEntityKey, PersistentDataType.BYTE)) {
            player.sendMessage("§cThis Ghast is already harnessed.");
            return;
        }

        // Mark the Ghast as harnessed
        edc.set(harnessEntityKey, PersistentDataType.BYTE, (byte)1);

        // Apply speed boost using key-based AttributeModifier
        AttributeInstance moveAttr = ghast.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveAttr != null) {
            Collection<AttributeModifier> existing = moveAttr.getModifiers();
            boolean hasMod = existing.stream()
                    .anyMatch(mod -> mod.key().equals(speedModifierKey));
            if (!hasMod) {
                AttributeModifier speedMod = new AttributeModifier(
                        speedModifierKey,
                        0.5,
                        Operation.MULTIPLY_SCALAR_1
                );
                moveAttr.addModifier(speedMod);
            }
        }

        // Consume one harness
        inHand.setAmount(inHand.getAmount() - 1);
        player.getInventory().setItemInMainHand(inHand);
        player.sendMessage("§aYou harnessed the Ghast! It's now happier and faster.");
    }
}
