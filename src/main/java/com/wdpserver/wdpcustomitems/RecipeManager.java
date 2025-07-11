package com.wdpserver.wdpcustomitems;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.Map;

public class RecipeManager {

    public record RecipeInfo(String displayName, ShapedRecipe recipe) {}

    private final Map<NamespacedKey, RecipeInfo> recipes = new HashMap<>();

    public void registerRecipe(String displayName, NamespacedKey key, ShapedRecipe recipe) {
        recipes.put(key, new RecipeInfo(displayName, recipe));
    }

    public Map<NamespacedKey, RecipeInfo> getRecipes() {
        return recipes;
    }
}
