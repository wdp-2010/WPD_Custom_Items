package com.wdpserver.wdpcustomitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DubbleJumpHandler extends JavaPlugin implements Listener {

    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private final Map<UUID, Boolean> canDoubleJump = new HashMap<>();
    private final Map<UUID, Double> lastYVelocity = new HashMap<>();

    private final WdpCustomItems plugin;

    public DubbleJumpHandler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        plugin.jumpBootsKey = new NamespacedKey(this, "double_jump_boots");
    }

    /**
     * Helper to check if boots have the PersistentData key.
     */
    private boolean isDoubleJumpBoots(ItemStack boots) {
        if (boots == null || boots.getType() == Material.AIR) return false;
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(plugin.jumpBootsKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        ItemStack boots = p.getInventory().getBoots();
        boolean hasBoots = isDoubleJumpBoots(boots);

        UUID uuid = p.getUniqueId();
        boolean onGround = p.isOnGround();
        boolean wasGroundBefore = wasOnGround.getOrDefault(uuid, true);
        double currentY = p.getVelocity().getY();
        double lastY = lastYVelocity.getOrDefault(uuid, 0.0);

        // 1. If player just left ground
        if (wasGroundBefore && !onGround && hasBoots) {
            canDoubleJump.put(uuid, true);
            p.sendMessage("you have double jump boots");
        }

        // 2. If player landed, reset
        if (onGround) {
            canDoubleJump.put(uuid, false);
        }

        // 3. Detect midair jump input via Y velocity increase
        if (!onGround && hasBoots && canDoubleJump.getOrDefault(uuid, false)) {
            if (currentY - lastY > 0.2) {
                // Apply double jump boost
                Vector v = p.getLocation().getDirection().multiply(0.5);
                v.setY(1.0);
                p.setVelocity(v);

                canDoubleJump.put(uuid, false);
            }
        }

        // Update tracking
        wasOnGround.put(uuid, onGround);
        lastYVelocity.put(uuid, currentY);
    }
}