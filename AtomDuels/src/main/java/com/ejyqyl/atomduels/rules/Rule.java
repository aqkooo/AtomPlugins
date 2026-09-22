package com.ejyqyl.atomduels.rules;

import org.bukkit.Material;

/**
 * 20+ configurable combat and arena rules for AtomDuels.
 * Authored by ejyqyl.
 */
public enum Rule {
    BOW("Лук", "Использование луков и стрел", Material.BOW, true),
    CROSSBOW("Арбалет", "Использование арбалетов и фейерверков", Material.CROSSBOW, true),
    SWORD("Меч", "Использование мечей", Material.DIAMOND_SWORD, true),
    AXE("Топор", "Использование боевых топоров", Material.DIAMOND_AXE, true),
    MACE("Булава", "Использование булавы (1.21+)", Material.MACE, true),
    POTIONS("Зелья", "Использование любых зелий", Material.POTION, true),
    SPLASH_POTIONS("Взрывные зелья", "Бросание взрывных зелий", Material.SPLASH_POTION, true),
    TOTEM("Тотем бессмертия", "Срабатывание тотемов бессмертия", Material.TOTEM_OF_UNDYING, true),
    SHIELD("Щит", "Блокирование щитом", Material.SHIELD, true),
    GOLDEN_APPLE("Золотые яблоки", "Обычные и зачарованные золотые яблоки", Material.GOLDEN_APPLE, true),
    ENDER_PEARL("Жемчуг Края", "Телепортация жемчугом Края", Material.ENDER_PEARL, true),
    ROD("Удочка", "Использование удочки для комбо", Material.FISHING_ROD, true),
    ELYTRA("Элитры", "Полёты на элитрах", Material.ELYTRA, false),
    ARMOR("Броня", "Ношение элементов брони", Material.DIAMOND_CHESTPLATE, true),
    NATURAL_REGEN("Естественная регенерация", "Регенерация здоровья от сытости", Material.REDSTONE, true),
    HUNGER("Голод", "Трата шкалы голода во время боя", Material.COOKED_BEEF, true),
    BLOCK_PLACE("Установка блоков", "Размещение блоков на арене", Material.COBBLESTONE, false),
    BLOCK_BREAK("Разрушение блоков", "Ломание блоков на арене", Material.IRON_PICKAXE, false),
    SOUP("Супы", "Мгновенное лечение грибными супами", Material.MUSHROOM_STEW, false),
    TRIDENT("Трезубец", "Использование трезубцев и тягуна", Material.TRIDENT, true),
    CRYSTAL_PVP("Кристаллы", "Использование кристаллов Края и якорей", Material.END_CRYSTAL, false),
    PROJECTILE_DAMAGE("Урон от снарядов", "Получение урона от любых стрел и снарядов", Material.ARROW, true);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final boolean defaultEnabled;

    Rule(String displayName, String description, Material icon, boolean defaultEnabled) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.defaultEnabled = defaultEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
}
