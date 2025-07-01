package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WdpCustomItems extends JavaPlugin {

    public NamespacedKey beamSwordKey;
    public NamespacedKey boltKey;
    public NamespacedKey beamDamageKey;
    public NamespacedKey beamColorKey;
    public NamespacedKey beamKnockbackKey;

    public final Map<UUID, Long> cooldowns = new HashMap<>();
    public final Map<UUID, BossBar> cooldownBars = new HashMap<>();
    public final Map<UUID, BossBar> readyBars = new HashMap<>();

    public long cooldownTimeMs;

    @Override
    public void onEnable() {
        ShapelessRecipe recolorRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "recolor_red"),
                new ItemStack(Material.DIAMOND_SWORD)
        );
        recolorRecipe.addIngredient(Material.DIAMOND_SWORD);
        recolorRecipe.addIngredient(Material.RED_DYE);
        getServer().addRecipe(recolorRecipe);

        saveDefaultConfig();

        // Namespaced keys
        beamSwordKey = new NamespacedKey(this, "plugin_sword");
        boltKey = new NamespacedKey(this, "boltkey");
        beamDamageKey = new NamespacedKey(this, "beam_damage");
        beamColorKey = new NamespacedKey(this, "beam_color");
        beamKnockbackKey = new NamespacedKey(this, "beam_knockback");

        cooldownTimeMs = (long) (getConfig().getDouble("cooldown", 5.0) * 1000);

        // Register the recipe here
        registerBoltRecipe();

        getServer().getPluginManager().registerEvents(new BeamHandler(this), this);
        getServer().getPluginManager().registerEvents(new RecolorCraftHandler(this), this);


        Objects.requireNonNull(getCommand("givesword")).setExecutor(new GiveSwordCommand(this));
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor((sender, command, label, args) -> {
            reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        });

        getLogger().info("WDP Custom Items started!");
    }


    @Override
    public void onDisable() {
        getLogger().info("WDP Custom Items disabled.");
    }

    private void registerBoltRecipe() {
        ItemStack bolt = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = bolt.getItemMeta();
        meta.setDisplayName("§bLightning Bolt");
        meta.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        meta.setLore(Collections.singletonList("With this in your inventory, the Epic Sword spawn lightning."));
        bolt.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(boltKey, bolt);
        recipe.shape(
                " E ",
                "ENE",
                " E "
        );
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('N', Material.NETHER_STAR);

        getServer().addRecipe(recipe);
    }


}
