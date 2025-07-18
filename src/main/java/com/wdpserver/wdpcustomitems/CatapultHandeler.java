package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
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
import org.joml.Vector3f;

public class CatapultHandeler implements Listener {

    private final WdpCustomItems plugin;
    private final NamespacedKey storedBlockKey;

    public CatapultHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
    }

    @EventHandler
    public void onShoot(@NotNull EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null) return;

        ItemMeta meta = bow.getItemMeta();
        if (meta == null) return;

        if (!meta.getPersistentDataContainer().has(plugin.catapultKey, PersistentDataType.BYTE)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.AIR || !offhand.getType().isBlock()) {
            player.sendMessage("You don't have a block in your offhand");
            event.setCancelled(true);
            return;
        }

        Arrow arrow = (Arrow) event.getProjectile();
        arrow.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, offhand.getType().name());

        BlockData blockData = offhand.getType().createBlockData();

        BlockDisplay display = arrow.getWorld().spawn(arrow.getLocation(), BlockDisplay.class);
        display.setBlock(blockData);
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                display.getTransformation().getLeftRotation(),
                new Vector3f(1f, 1f, 1f),
                display.getTransformation().getRightRotation()
        ));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
                    display.remove();
                    cancel();
                    return;
                }

                display.teleport(arrow.getLocation());
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onArrowHit(@NotNull ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (event.getHitBlock() == null || event.getHitBlockFace() == null) return;

        Block hitBlock = event.getHitBlock();
        BlockFace face = event.getHitBlockFace();

        Bukkit.getLogger().info("Arrow hit face: " + face + " at block: " + hitBlock.getType());

        String blockName = arrow.getPersistentDataContainer().get(storedBlockKey, PersistentDataType.STRING);
        if (blockName == null) return;

        Material blockType = Material.matchMaterial(blockName);
        if (blockType == null || !blockType.isBlock()) return;

        Block placeBlock = hitBlock.getRelative(face);

        if (placeBlock.getType().isAir() || placeBlock.isReplaceable()) {
            placeBlock.setType(blockType);
            BlockData data = placeBlock.getBlockData();

            // Try to rotate block toward hit face
            if (data instanceof Directional directional) {
                directional.setFacing(face);
                placeBlock.setBlockData(directional);
                Bukkit.getLogger().info("Placed directional block: " + blockType + " facing " + face);
            } else {
                placeBlock.setBlockData(data);
                Bukkit.getLogger().info("Placed non-directional block: " + blockType);
            }
        } else {
            Bukkit.getLogger().info("Could not place block: " + blockType + " at " + placeBlock.getLocation() + " (blocked)");
        }

        arrow.remove(); // Optional cleanup
    }
}
