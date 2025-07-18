package com.wdpserver.wdpcustomitems;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoubleJumpHandler implements Listener {

    private final Map<UUID, Boolean> canDoubleJump = new HashMap<>();

    private final WdpCustomItems plugin;

    public DoubleJumpHandler(WdpCustomItems plugin) {
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

        if (player.getGameMode().equals(GameMode.CREATIVE)) return;

        ItemStack boots = player.getInventory().getBoots();
        boolean hasBoots = isDoubleJumpBoots(boots);
        boolean onGround = player.isOnGround();
        boolean wasGround = canDoubleJump.containsKey(uuid) && canDoubleJump.get(uuid) != null;

        if (onGround) {
            // On the ground: reset everything
            player.setAllowFlight(false);
            canDoubleJump.put(uuid, true);
            return;
        }

        // If in the air, and wearing boots, and still allowed to double jump
        if (!onGround && hasBoots && canDoubleJump.getOrDefault(uuid, false)) {
            player.setAllowFlight(true);
        }
    }
    @EventHandler
    public void onPlayerCreative(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) player.setAllowFlight(true);

    }


    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ItemStack boots = player.getInventory().getBoots();
        boolean hasBoots = isDoubleJumpBoots(boots);
        boolean canJump = canDoubleJump.getOrDefault(uuid, false);

        if (player.getGameMode().equals(GameMode.CREATIVE)) return;

        if (hasBoots && canJump) {
            event.setCancelled(true); // Cancel normal flying
            player.setAllowFlight(false); // Disable flight immediately
            canDoubleJump.put(uuid, false); // Mark that jump was used


            double velocityY = plugin.getConfig().getDouble("double-jump-boots.velocity", 1);
            Vector velocity = player.getLocation().getDirection().multiply(0.5);
            velocity.setY(velocityY);
            player.setVelocity(velocity);
        }
    }

}
