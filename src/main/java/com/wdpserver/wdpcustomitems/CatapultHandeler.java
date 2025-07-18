package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class CatapultHandeler implements Listener {

    private final WdpCustomItems plugin;
    private final NamespacedKey storedBlockKey;

    public CatapultHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
    }

    @EventHandler
    public void OnShoot(@NotNull EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        ItemMeta meta = bow.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(plugin.catapultKey, PersistentDataType.BYTE)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || !offhand.getType().isBlock()) {
            player.sendMessage("You have no block in your off-hand");
            return;
        }

        // Store block type in arrow's PDC
        Arrow arrow = (Arrow) event.getProjectile();
        arrow.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, offhand.getType().name());

        // Spawn display entity to follow the arrow
        BlockDisplay display = player.getWorld().spawn(player.getEyeLocation(), BlockDisplay.class);
        display.setBlock(offhand.getType().createBlockData());
        display.setTransformation(new Transformation(
                new Vector(0, 0, 0).toVector3f(),
                display.getTransformation().getLeftRotation(),
                new Vector(1, 1, 1).toVector3f(),
                display.getTransformation().getRightRotation()
        ));

        // Track arrow and update display
        WeakReference<Arrow> arrowRef = new WeakReference<>(arrow);
        new BukkitRunnable() {
            @Override
            public void run() {
                Arrow a = arrowRef.get();
                if (a == null || a.isDead() || a.isOnGround() || !a.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }
                display.teleport(a.getLocation());
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler
    public void onArrowHit(@NotNull ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        if (!arrow.getPersistentDataContainer().has(storedBlockKey, PersistentDataType.STRING)) return;

        String blockName = arrow.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING);
        if (blockName == null) return;

        Material blockType = Material.matchMaterial(blockName);
        if (blockType == null || !blockType.isBlock()) return;

        Location hitLoc = arrow.getLocation().getBlock().getLocation();
        Block block = hitLoc.getBlock();
        if (block.getType().isAir() || block.isReplaceable()) {
            block.setType(blockType);
        }

        arrow.remove(); // Clean up arrow
    }
}
