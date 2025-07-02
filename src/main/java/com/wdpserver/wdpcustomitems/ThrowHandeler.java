package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ThrowHandeler implements Listener {
    private final WdpCustomItems plugin;

    public ThrowHandeler(WdpCustomItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            ItemMeta meta = item.getItemMeta();

            if (item == null) return;

            if (!item.hasItemMeta()) return ;

            if (meta.getPersistentDataContainer().has(plugin.throwStoneKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);

                item.setAmount(item.getAmount() - 1);

                Snowball snowball = player.launchProjectile(Snowball.class);

                snowball.setVelocity(player.getEyeLocation().getDirection().multiply(1.5));

                player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1, 1);
            }
        }
    }
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof  Snowball)) return;

        Snowball snowball = (Snowball) event.getEntity();

        if (snowball.getPersistentDataContainer().has(plugin.throwStoneKey, PersistentDataType.BYTE)) {
            if (event.getHitEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getHitEntity();

                target.damage(3);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1, 1);
            }
            snowball.getWorld().spawnParticle(Particle.CRIT, snowball.getLocation(), 10);
        }
    }
}

