package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DubbleJumpHandler implements Listener {

    private final Map<UUID, Boolean> canDoubleJump = new HashMap<>();

    private final WdpCustomItems plugin;

    public DubbleJumpHandler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    private boolean isDoubleJumpBoots(ItemStack boots) {
        if (boots == null || boots.getType() == Material.AIR) return false;
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(plugin.jumpBootsKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ItemStack boots = player.getInventory().getBoots();
        boolean hasBoots = isDoubleJumpBoots(boots);
        boolean onGround = player.isOnGround();

        if (hasBoots && onGround) {
            // Allow flight to enable double jump detection
            player.setAllowFlight(true);
            canDoubleJump.put(uuid, true);
        } else if (!hasBoots || onGround == false) {
            // Disable flight when not on ground or boots removed
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ItemStack boots = player.getInventory().getBoots();
        boolean hasBoots = isDoubleJumpBoots(boots);
        boolean canJump = canDoubleJump.getOrDefault(uuid, false);

        if (hasBoots && canJump) {
            event.setCancelled(true); // Cancel normal flying
            player.setAllowFlight(false); // Disable flight until next landing
            canDoubleJump.put(uuid, false); // Use up the double jump

            // Apply upward and forward boost
            Vector velocity = player.getLocation().getDirection().multiply(0.5);
            velocity.setY(1.0);
            player.setVelocity(velocity);
        }
    }
}
