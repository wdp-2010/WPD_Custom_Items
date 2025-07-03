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

        // If on the ground: reset state and disable flight
        if (onGround) {
            player.setAllowFlight(false);
            canDoubleJump.put(uuid, false);
            return;
        }

        // If not on ground, and wearing boots, and hasn't used double jump yet
        if (!onGround && hasBoots && !canDoubleJump.getOrDefault(uuid, false)) {
            // Enable flight so pressing space will trigger the double jump
            player.setAllowFlight(true);
            canDoubleJump.put(uuid, true);
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
            event.setCancelled(true); // Cancel normal flight
            player.setAllowFlight(false); // Disable flight so no further jumps
            canDoubleJump.put(uuid, false); // Consume the double jump

            // Apply boost
            Vector velocity = player.getLocation().getDirection().multiply(0.5);
            velocity.setY(1.0);
            player.setVelocity(velocity);
        }
    }
}
