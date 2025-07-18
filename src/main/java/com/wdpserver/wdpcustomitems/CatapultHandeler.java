package com.wdpserver.wdpcustomitems;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.UUID;

public class CatapultHandeler implements Listener {

    private final WdpCustomItems plugin;
    private final NamespacedKey storedBlockKey, playerKey, facingKey;

    public CatapultHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
        this.storedBlockKey = new NamespacedKey(plugin, "stored_block");
        this.playerKey = new NamespacedKey(plugin, "playername");
        this.facingKey = new NamespacedKey(plugin, "facing");
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        ItemMeta meta = bow.getItemMeta();
        if (bow == null) return;

        if (!bow.getItemMeta()
                .getPersistentDataContainer()
                .has(plugin.catapultKey, PersistentDataType.BYTE)) return;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.AIR || !offhand.getType().isBlock()) {
            player.sendMessage("You need a block in offhand!");
            event.setCancelled(true);
            return;
        }

        if (meta instanceof Damageable damageable) {
            int damage = damageable.getDamage();
            int maxDurability = bow.getType().getMaxDurability();

            damage += 2; // increase damage by 2

            if (damage >= maxDurability) {
                player.getInventory().remove(bow);
                player.sendMessage(ChatColor.RED + "Your catapult bow broke!");
            } else {
                damageable.setDamage(damage);
                bow.setItemMeta((ItemMeta) damageable);
            }
        }

        Arrow arrow = (Arrow) event.getProjectile();
        arrow.getPersistentDataContainer().set(storedBlockKey, PersistentDataType.STRING, offhand.getType().name());
        arrow.getPersistentDataContainer().set(playerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        arrow.getPersistentDataContainer().set(facingKey, PersistentDataType.STRING, player.getFacing().name());

        BlockData blockData = offhand.getType().createBlockData();
        BlockDisplay display = player.getWorld().spawn(arrow.getLocation(), BlockDisplay.class);
        display.setBlock(blockData);
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.5f, -0.5f),
                display.getTransformation().getLeftRotation(),
                new Vector3f(1f, 1f, 1f),
                display.getTransformation().getRightRotation()));

        new BukkitRunnable() {
            public void run() {
                if (arrow.isDead() || arrow.isInBlock() || arrow.isOnGround()) {
                    display.remove();
                    cancel();
                } else display.teleport(arrow.getLocation());
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (event.getHitBlock() == null || event.getHitBlockFace() == null) return;

        Block hitBlock = event.getHitBlock();
        BlockFace hitFace = event.getHitBlockFace();

        var dt = arrow.getPersistentDataContainer();
        String blockName = dt.get(storedBlockKey, PersistentDataType.STRING);
        String playerUUIDStr = dt.get(playerKey, PersistentDataType.STRING);
        String facingStr = dt.get(facingKey, PersistentDataType.STRING);
        if (blockName == null || playerUUIDStr == null) return;

        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(playerUUIDStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        Material blockType = Material.matchMaterial(blockName);
        if (blockType == null || !blockType.isBlock()) return;

        Block placeBlock = hitBlock.getRelative(hitFace);
        if (!(placeBlock.getType().isAir() || placeBlock.isReplaceable())) {
            arrow.remove();
            return;
        }

        // --- New logic: if arrow hits side of a stair, copy orientation and half ---
        BlockData hitData = hitBlock.getBlockData();
        if (hitData instanceof Stairs hitStairs) {
            placeBlock.setType(blockType);
            BlockData newData = placeBlock.getBlockData();
            if (newData instanceof Stairs newStairs) {
                newStairs.setFacing(hitStairs.getFacing());
                newStairs.setHalf(hitStairs.getHalf());
                placeBlock.setBlockData(newStairs);
                Bukkit.getLogger().info("Copied stair orientation from existing stair: "
                        + newStairs.getFacing() + " / " + newStairs.getHalf());
            } else {
                placeBlock.setBlockData(newData);
            }
            consumeOffhandBlock(player);
            arrow.remove();
            return;
        }

        // --- Otherwise normal stair & directional placement based on player facing and hit face ---

        placeBlock.setType(blockType);
        BlockData data = placeBlock.getBlockData();

        BlockFace faceToUse = null;
        if (facingStr != null) {
            try {
                faceToUse = BlockFace.valueOf(facingStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (data instanceof Stairs stairs) {
            if (faceToUse != null && stairs.getFaces().contains(faceToUse))
                stairs.setFacing(faceToUse);

            stairs.setHalf(hitFace == BlockFace.DOWN
                    ? Stairs.Half.TOP
                    : Stairs.Half.BOTTOM);

            placeBlock.setBlockData(stairs);
            Bukkit.getLogger().info("Placed stair with facing " + stairs.getFacing() + " and half " + stairs.getHalf());
        } else if (data instanceof Directional directional && faceToUse != null && directional.getFaces().contains(faceToUse)) {
            directional.setFacing(faceToUse);
            placeBlock.setBlockData(directional);
            Bukkit.getLogger().info("Placed directional block facing " + faceToUse);
        } else if (data instanceof Orientable orientable) {
            // Optional: orient logs by axis based on player facing or hit face here if you want
            placeBlock.setBlockData(orientable);
            Bukkit.getLogger().info("Placed orientable block without rotation");
        } else {
            placeBlock.setBlockData(data);
            Bukkit.getLogger().info("Placed block without rotation");
        }

        consumeOffhandBlock(player);
        arrow.remove();
    }

    private void consumeOffhandBlock(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            int amount = offhand.getAmount();
            if (amount > 1) {
                offhand.setAmount(amount - 1);
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }
    }
}
