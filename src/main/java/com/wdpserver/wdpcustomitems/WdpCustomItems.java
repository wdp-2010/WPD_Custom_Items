package com.wdpserver.wdpcustomitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
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
    public NamespacedKey beamStoneKey;
    public NamespacedKey throwStoneKey;
    public NamespacedKey jumpBootsKey;

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
        beamStoneKey = new NamespacedKey(this, "beamstone");
        throwStoneKey = new NamespacedKey(this, "throwstone");
        jumpBootsKey = new NamespacedKey(this, "dubllejumpboots");

        cooldownTimeMs = (long) (getConfig().getDouble("cooldown", 5.0) * 1000);

        // Register the recipe here
        registerBoltRecipe();
        registerBeamSwordRecipe();
        registerBeamStoneRecipe();
        registerThrowStonerecipe();
        registerJumpBootsRecipe();

        getServer().getPluginManager().registerEvents(new BeamHandler(this), this);
        getServer().getPluginManager().registerEvents(new RecolorCraftHandler(this), this);
        getServer().getPluginManager().registerEvents(new ThrowHandeler(this), this);
        getServer().getPluginManager().registerEvents(new DubbleJumpHandler(this), this);


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

    public ItemStack createCustomBeamSword(int damage, String color, double knockback) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();

        meta.setDisplayName("§bBeam Sword");
        meta.getPersistentDataContainer().set(beamSwordKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(beamDamageKey, PersistentDataType.INTEGER, damage);
        meta.getPersistentDataContainer().set(beamColorKey, PersistentDataType.STRING, color);
        meta.getPersistentDataContainer().set(beamKnockbackKey, PersistentDataType.DOUBLE, knockback);

        meta.setItemModel(new NamespacedKey("wdpserver","beam_sword"));

        meta.setLore(Arrays.asList(
                "§fBeam:",
                "§7Damage: §f" + damage,
                "§7Color: §f" + color,
                "§7Knockback: §f" + knockback
        ));

        sword.setItemMeta(meta);
        return sword;
    }
    public ItemStack createThrowRock() {
        ItemStack ThrowRock = new ItemStack(Material.COBBLESTONE);
        ItemMeta meta = ThrowRock.getItemMeta();

        meta.setMaxStackSize(32);
        meta.setDisplayName("§bThrowable Stone");
        meta.setLore(Collections.singletonList("§fRight click to trow"));
        meta.getPersistentDataContainer().set(throwStoneKey, PersistentDataType.BYTE, (byte) 1);

        ThrowRock.setItemMeta(meta);
        return ThrowRock;

    }
    public ItemStack createBeamStone() {
        ItemStack beamStone = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = beamStone.getItemMeta();

        meta.setDisplayName("§bBeam Stone");
        meta.getPersistentDataContainer().set(beamStoneKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(new NamespacedKey("wdpserver","beam_stone"));

        beamStone.setItemMeta(meta);
        return beamStone;

    }
    public ItemStack createJumpBoots() {
        ItemStack jumpBoots = new ItemStack(Material.IRON_BOOTS);
        ItemMeta meta = jumpBoots.getItemMeta();

        meta.getPersistentDataContainer().set(jumpBootsKey, PersistentDataType.BYTE, (byte) 1);
        meta.setDisplayName("§bDubble Jump Boots");
        meta.setLore(Arrays.asList("§fJump once on air","§fpress space twice to dubble jump"));

        jumpBoots.setItemMeta(meta);
        return jumpBoots;
    }


    private void registerBoltRecipe() {
        ItemStack bolt = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = bolt.getItemMeta();
        meta.setDisplayName("§bLightning Bolt");
        meta.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        meta.setLore(Collections.singletonList("§fWith this in your inventory, the Epic Sword spawn lightning."));
        meta.setItemModel(new NamespacedKey("wdpserver","bolt"));

        bolt.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(boltKey, bolt);
        recipe.shape(
                "  D" ,
                " N ",
                "D  "
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);

        getServer().addRecipe(recipe);
    }
    public void registerBeamSwordRecipe() {
        ItemStack result = createCustomBeamSword(getConfig().getInt("beam-damage", 5), "RED", getConfig().getDouble("knockback", 1.5));  //  config
        ItemStack beamStone = createBeamStone();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "beam_sword"), result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DSD",
                " D "
        );

        recipe.setIngredient('N', new RecipeChoice.ExactChoice(beamStone));
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        getServer().addRecipe(recipe);
    }
    public void registerBeamStoneRecipe() {
        ItemStack result = createBeamStone();

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "beam_stone"), result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DFD",
                " D "
        );

        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('F', Material.SUNFLOWER);

        getServer().addRecipe(recipe);
    }
    public void registerThrowStonerecipe() {
        ItemStack result = createThrowRock();
        result.setAmount(9);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "throw_stone"), result);

        recipe.shape(
                "CCC",
                "CCC",
                "CCC"
        );

        recipe.setIngredient('C', Material.COBBLESTONE);

        getServer().addRecipe(recipe);
    }
    public void registerJumpBootsRecipe() {
        ItemStack result = createJumpBoots();

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jumpboots"), result);

        recipe.shape(
                "   ",
                "I I",
                "W W"
        );

        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('W', Material.WIND_CHARGE);

        getServer().addRecipe(recipe);
    }



}
