package com.wdpserver.wdpcustomitems;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GhastHarnessHandler implements Listener {
    private final WdpCustomItems plugin;

    public GhastHarnessHandler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractGhast(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("InteractEntityEvent: " + player.getName() + " clicked target " + event.getRightClicked().getType());

        if (!(event.getRightClicked() instanceof HappyGhast ghast)) {
            plugin.getLogger().info("InteractEntityEvent: Target is not a Happy Ghast. Ignoring.");
            return;
        }
        plugin.getLogger().info("InteractEntityEvent: Happy Ghast detected.");

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) {
            plugin.getLogger().info("InteractEntityEvent: No item or no meta in hand.");
            return;
        }

        PersistentDataContainer dc = inHand.getItemMeta().getPersistentDataContainer();
        boolean hasHarnessTag = dc.has(plugin.ghastHarnessKey, PersistentDataType.BYTE);
        plugin.getLogger().info("Item in hand has harness tag? " + hasHarnessTag);
        if (!hasHarnessTag) return;

        PersistentDataContainer edc = ghast.getPersistentDataContainer();
        if (edc.has(plugin.harnessAppliedKey, PersistentDataType.BYTE)) {
            plugin.getLogger().info("Ghast already harnessed. Sending message to player.");
            player.sendMessage("§cThis Ghast is already harnessed.");
            return;
        }

        // Mark harnessed
        edc.set(plugin.harnessAppliedKey, PersistentDataType.BYTE, (byte)1);
        plugin.getLogger().info("Harness tag applied to Ghast.");

        // Set base flying speed to 0.1 (double default)
        AttributeInstance flyAttr = ghast.getAttribute(Attribute.FLYING_SPEED);
        if (flyAttr != null) {
            flyAttr.setBaseValue(0.1);
            plugin.getLogger().info("Ghast base flying speed set to 0.1.");
        } else {
            plugin.getLogger().warning("Unable to retrieve Ghast flying speed attribute.");
        }

        // Consume harness and feedback
        inHand.setAmount(inHand.getAmount() - 1);
        player.getInventory().setItemInMainHand(inHand);
        player.sendMessage("§aYou harnessed the Ghast! It's now happier and faster.");
        plugin.getLogger().info("Harness item consumed and player notified.");
    }
}
