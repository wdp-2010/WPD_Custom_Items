package com.wdpserver.wdpcustomitems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WdpCustomItems extends JavaPlugin {
    public NamespacedKey diaBeamSwordKey;
    public NamespacedKey netheriteBeamSwordKey;
    public NamespacedKey boltKey;
    public NamespacedKey beamDamageKey;
    public NamespacedKey beamColorKey;
    public NamespacedKey beamKnockbackKey;
    public NamespacedKey beamStoneKey;
    public NamespacedKey throwStoneKey;
    public NamespacedKey jumpBootsKey;
    public NamespacedKey grapplingKey;
    public NamespacedKey beamRangeKey;
    public NamespacedKey beamCooldownKey;
    public NamespacedKey catapultKey;

    public final Map<UUID, Boolean> hasBeam = new HashMap<>();
    public final Map<UUID, Long> cooldowns = new HashMap<>();
    public final Map<UUID, BossBar> cooldownBars = new HashMap<>();
    public final Map<UUID, BossBar> readyBars = new HashMap<>();

    public Map<UUID, Long> beamCooldownDurations = new HashMap<>();
    public Map<UUID, Boolean> hasGrappling = new HashMap<>();

    public long shortCooldownTimeMs;
    public long longCooldownTimeMsGrappling;

    private RecipeManager recipeManager;


    @Override
    public void onEnable() {
        ShapelessRecipe recolorRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "recolor_red"),
                new ItemStack(Material.DIAMOND_SWORD)
        );

        this.recipeManager = new RecipeManager();

        RecipeCommand recipeCommand = new RecipeCommand(recipeManager);

        recolorRecipe.addIngredient(Material.DIAMOND_SWORD);
        recolorRecipe.addIngredient(Material.RED_DYE);
        getServer().addRecipe(recolorRecipe);

        saveDefaultConfig();

        // Namespaced keys
        diaBeamSwordKey = new NamespacedKey(this, "diaBeamSword");
        netheriteBeamSwordKey = new NamespacedKey(this, "netheriteBeamSword");
        boltKey = new NamespacedKey(this, "boltkey");
        beamDamageKey = new NamespacedKey(this, "beam_damage");
        beamColorKey = new NamespacedKey(this, "beam_color");
        beamKnockbackKey = new NamespacedKey(this, "beam_knockback");
        beamCooldownKey = new NamespacedKey(this, "beamdefcooldown");
        beamRangeKey= new NamespacedKey(this, "beamrange");
        beamStoneKey = new NamespacedKey(this, "beamstone");
        throwStoneKey = new NamespacedKey(this, "throwstone");
        jumpBootsKey = new NamespacedKey(this, "doublejumpboots");
        grapplingKey = new NamespacedKey(this, "grapplinghook");
        catapultKey = new NamespacedKey(this, "catapult");

        shortCooldownTimeMs = (long) (getConfig().getDouble("diamondbeam-sword.short-cooldown", 1.0) * 1000);
        longCooldownTimeMsGrappling = (long) (getConfig().getDouble("grappling-hook.cooldown", 5.0) * 1000);

        // Register the recipe here
        registerBoltRecipe();
        registerDiaBeamSwordRecipe();
        registerBeamStoneRecipe();
        registerThrowStoneRecipe();
        registerJumpBootsRecipe();
        registerGrapplingHookRecipe();
        registerCatapultRecipe();

        getServer().getPluginManager().registerEvents(new BeamHandler(this), this);
        getServer().getPluginManager().registerEvents(new RecolorCraftHandler(this), this);
        getServer().getPluginManager().registerEvents(new ThrowHandeler(this), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpHandler(this), this);
        getServer().getPluginManager().registerEvents(new GraplingHandeler(this), this);
        getServer().getPluginManager().registerEvents(new SwordCraftUpgrade(this), this);
        getServer().getPluginManager().registerEvents(new CatapultHandeler(this), this);
        getServer().getPluginManager().registerEvents(recipeCommand, this);


        Objects.requireNonNull(getCommand("givesword")).setExecutor(new GiveSwordCommand(this));
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor((sender, command, label, args) -> {
            reloadConfig();
            sender.sendMessage("§aConfig reloaded.");
            return true;
        });
        getCommand("recipes").setExecutor(recipeCommand);

        getLogger().info("WDP Custom Items started!");
    }


    @Override
    public void onDisable() {
        getLogger().info("WDP Custom Items disabled.");
    }

    public ItemStack createCustomBeamSword(int damage, String color, double knockback, long cooldown, double range, Material material) {
        ItemStack sword = new ItemStack(material);
        ItemMeta meta = sword.getItemMeta();


        if (material == Material.NETHERITE_SWORD){
            meta.getPersistentDataContainer().set(netheriteBeamSwordKey, PersistentDataType.BYTE, (byte) 1);
            meta.setItemModel(new NamespacedKey("wdpserver","netherite_beam_sword"));
            meta.setDisplayName("§5Netherite Beam Sword");
        } else {
            meta.getPersistentDataContainer().set(diaBeamSwordKey, PersistentDataType.BYTE, (byte) 1);
            meta.setItemModel(new NamespacedKey("wdpserver","beam_sword"));
            meta.setDisplayName("§bBeam Sword");
        }
        meta.getPersistentDataContainer().set(beamDamageKey, PersistentDataType.INTEGER, damage);
        meta.getPersistentDataContainer().set(beamColorKey, PersistentDataType.STRING, color);
        meta.getPersistentDataContainer().set(beamKnockbackKey, PersistentDataType.DOUBLE, knockback);
        meta.getPersistentDataContainer().set(beamRangeKey, PersistentDataType.DOUBLE, range);
        meta.getPersistentDataContainer().set(beamCooldownKey, PersistentDataType.LONG, cooldown);

        meta.setLore(Arrays.asList(
                "§fBeam:",
                "§7Damage: §f" + damage,
                "§7Color: §f" + color,
                "§7Knockback: §f" + knockback,
                "§7Cooldown: §f" + cooldown + " s",
                "§7Range: §f" + range + " Blocks"
        ));

        sword.setItemMeta(meta);
        return sword;
    }
    public ItemStack createCatapult() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        meta.setMaxStackSize(32);
        meta.setDisplayName("§bCatapult");
        meta.setLore(Arrays.asList("§fLaunch a block", "§fPlace the block where it lands"));
        meta.getPersistentDataContainer().set(catapultKey, PersistentDataType.BYTE, (byte) 1);

        bow.setItemMeta(meta);
        return bow;

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
        meta.setDisplayName("§bDouble Jump Boots");
        meta.setLore(Arrays.asList("§fJump once on air","§fpress space twice to duoule jump"));

        jumpBoots.setItemMeta(meta);
        return jumpBoots;
    }
    public ItemStack createGrapplingHook() {
        ItemStack jumpBoots = new ItemStack(Material.TRIDENT);
        ItemMeta meta = jumpBoots.getItemMeta();

        meta.getPersistentDataContainer().set(grapplingKey, PersistentDataType.BYTE, (byte) 1);
        meta.setDisplayName("§bGrappling Hook");
        meta.setLore(Collections.singletonList("§fGrab an entity"));

        jumpBoots.setItemMeta(meta);
        return jumpBoots;
    }


    private void registerBoltRecipe() {
        ItemStack bolt = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = bolt.getItemMeta();

        NamespacedKey key = this.boltKey;

        meta.setDisplayName("§bLightning Bolt");
        meta.getPersistentDataContainer().set(boltKey, PersistentDataType.BYTE, (byte) 1);
        meta.setLore(Collections.singletonList("§fWith this in your inventory, the Epic Sword spawn lightning."));
        meta.setItemModel(new NamespacedKey("wdpserver","bolt"));

        bolt.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(key, bolt);
        recipe.shape(
                "  D" ,
                " N ",
                "D  "
        );
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);

        recipeManager.registerRecipe("Bolt Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerDiaBeamSwordRecipe() {
        int damage = getConfig().getInt("diamond-beam-sword.damage", 12);
        String color = getConfig().getString("diamond-beam-sword.color", "RED");
        int knockback = getConfig().getInt("diamond-beam-sword.knockback", 3);
        long cooldown = getConfig().getLong("diamond-beam-sword.cooldown", 5);
        double range = getConfig().getDouble("diamond-beam-sword.range", 50);

        NamespacedKey key = this.diaBeamSwordKey;

        ItemStack result = createCustomBeamSword(damage, color, knockback, cooldown, range, Material.DIAMOND_SWORD);  //  config
        ItemStack beamStone = createBeamStone();
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DSD",
                " D "
        );

        recipe.setIngredient('N', new RecipeChoice.ExactChoice(beamStone));
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        recipeManager.registerRecipe("Beam Sword", key, recipe);

        getServer().addRecipe(recipe);
    }

    public void registerCatapultRecipe() {
        ItemStack result = createCatapult();

        NamespacedKey key = this.catapultKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                "DDD",
                "DBD",
                "DDD"
        );

        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('B', Material.BOW);

        recipeManager.registerRecipe("Catapult", key, recipe);

        getServer().addRecipe(recipe);
    }

    public void registerBeamStoneRecipe() {
        ItemStack result = createBeamStone();

        NamespacedKey key = this.beamStoneKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Define shape: example shape, you can adjust (DONE)
        recipe.shape(
                " N ",
                "DFD",
                " D "
        );

        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('F', Material.SUNFLOWER);

        recipeManager.registerRecipe("Beam Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerThrowStoneRecipe() {
        ItemStack result = createThrowRock();
        result.setAmount(9);

        NamespacedKey key = this.throwStoneKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                "CCC",
                "CCC",
                "CCC"
        );

        recipe.setIngredient('C', Material.COBBLESTONE);

        recipeManager.registerRecipe("Throw Stone", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerJumpBootsRecipe() {
        ItemStack result = createJumpBoots();

        NamespacedKey key = this.jumpBootsKey;

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                "   ",
                "I I",
                "W W"
        );

        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('W', Material.WIND_CHARGE);

        recipeManager.registerRecipe("Double Jump Boots", key, recipe);

        getServer().addRecipe(recipe);
    }
    public void registerGrapplingHookRecipe() {
        ItemStack result = createGrapplingHook();
        NamespacedKey key = this.grapplingKey;
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
                " T ",
                " F ",
                "   "
        );

        recipe.setIngredient('T', Material.TRIDENT);
        recipe.setIngredient('F', Material.FISHING_ROD);

        recipeManager.registerRecipe("Grappling Hook", key, recipe);

        getServer().addRecipe(recipe);
    }



}
