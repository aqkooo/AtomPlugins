package ru.glowdevv.glowcustomloot.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LootItem {
    private final String id;
    private String provider;
    private double chance;
    private int minAmount;
    private int maxAmount;
    private ItemStack template;
    private final Map<String, Object> properties = new HashMap<>();

    public LootItem(@NotNull String id, @NotNull String provider, double chance, int minAmount, int maxAmount, @NotNull ItemStack template) {
        this.id = id;
        this.provider = provider;
        this.chance = round(Math.clamp(chance, 0.1, 100.0));
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
        this.template = template.clone();
        this.template.setAmount(1);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getProvider() {
        return provider;
    }

    public void setProvider(@NotNull String provider) {
        this.provider = provider;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = round(Math.clamp(chance, 0.1, 100.0));
    }

    public void addChance(double delta) {
        setChance(this.chance + delta);
    }

    public int getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(int minAmount) {
        this.minAmount = Math.max(1, minAmount);
        if (this.maxAmount < this.minAmount) {
            this.maxAmount = this.minAmount;
        }
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = Math.max(this.minAmount, maxAmount);
    }

    @NotNull
    public ItemStack getTemplate() {
        return template.clone();
    }

    public void setTemplate(@NotNull ItemStack template) {
        this.template = template.clone();
        this.template.setAmount(1);
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Nullable
    public ItemStack roll(@NotNull Random random) {
        double roll = random.nextDouble() * 100.0;
        if (roll <= chance) {
            int amount = minAmount == maxAmount
                    ? minAmount
                    : minAmount + random.nextInt(maxAmount - minAmount + 1);
            ItemStack result = template.clone();
            int maxStack = Math.max(1, result.getMaxStackSize());
            result.setAmount(Math.clamp(amount, 1, maxStack));
            return result;
        }
        return null;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
