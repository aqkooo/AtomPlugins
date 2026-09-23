package glowseller.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ShopItem {
    public enum Type {
        BOOSTER,
        ITEM,
        COMMAND,
        UNKNOWN
    }

    private final String key;
    private final String category;
    private Material material;
    private String baseHeadTexture;
    private String name;
    private List<String> lore;
    private int slot;
    private long price;
    private Type type;
    private double multiplier;
    private long durationSeconds;
    private int giveAmount;
    private List<String> commands;
    private Integer customModelData;
    private String permission;
    private String limit;

    public ShopItem(String key, String category) {
        this.key = key;
        this.category = category;
        this.material = Material.STONE;
        this.baseHeadTexture = null;
        this.name = key;
        this.lore = new ArrayList<>();
        this.slot = 0;
        this.price = 0;
        this.type = Type.ITEM;
        this.multiplier = 1.0;
        this.durationSeconds = 0;
        this.giveAmount = 1;
        this.commands = new ArrayList<>();
        this.customModelData = null;
        this.permission = null;
        this.limit = "unlimited";
    }

    public String getKey() {
        return key;
    }

    public String getCategory() {
        return category;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getBaseHeadTexture() {
        return baseHeadTexture;
    }

    public void setBaseHeadTexture(String baseHeadTexture) {
        this.baseHeadTexture = baseHeadTexture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getGiveAmount() {
        return giveAmount;
    }

    public void setGiveAmount(int giveAmount) {
        this.giveAmount = giveAmount;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands != null ? commands : new ArrayList<>();
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }
}
