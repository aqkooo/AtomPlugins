---
name: minecraft-java-plugin-dev
description: Полный справочник по разработке плагинов Minecraft Java (Paper/Purpur/Spigot/Folia): структура проекта, взаимодействие классов, команды, слушатели событий, GUI-меню и кнопки, цвета и текст (MiniMessage/legacy/hex), предметы и PDC, игровые механики, конфиги, БД, интеграции, производительность. Используй при любой задаче по написанию, доработке или ревью плагинов.
---

# MINECRAFT JAVA PLUGIN DEVELOPMENT — MASTER SKILL

Отвечай на языке пользователя. Код — рабочий, компилируемый, без выдуманных методов API. Если не уверен в сигнатуре — прямо скажи и предложи свериться с Javadoc Paper.

---

## 0. РЕЖИМ РАБОТЫ

1. Если не указаны: ядро (Paper/Purpur/Spigot/Folia), версия Minecraft, версия Java, система сборки — принимай по умолчанию **Paper 1.21.x, Java 21, Gradle Kotlin DSL** и явно скажи об этом одной строкой.
2. Сначала дай архитектуру (список классов и кто кого вызывает), потом код.
3. Каждый класс — в отдельном блоке кода с указанием пакета и пути файла.
4. После кода: список того, что нужно прописать в `plugin.yml`, `config.yml`, `messages.yml`.
5. Не переписывай весь проект ради маленькой правки: показывай изменённые методы целиком.
6. Всегда думай про: главный поток, утечки, NPE, дюпы предметов, читерство через GUI, нагрузку на TPS.
7. Не используй устаревшее (`ChatColor`, `setDisplayName(String)`, `Material.LEGACY_*`), если задача не под старое ядро.

### Матрица версий
| Ядро/версия | Текст | Команды | Примечание |
|---|---|---|---|
| Paper 1.21.x | Adventure/MiniMessage | Brigadier или CommandExecutor | Рекомендуется |
| Purpur | как Paper + доп. API | как Paper | Есть свои конфиги и фичи |
| Spigot | BungeeCord Chat API / legacy | CommandExecutor | Нет Adventure нативно |
| Folia | как Paper | как Paper | Только региональные планировщики (см. §8) |
| Paper 1.16.5 | legacy `&` + hex через Bungee | CommandExecutor | Java 8/11/16, API другой — не смешивать код |

---

## 1. СТРУКТУРА ПРОЕКТА

```
src/main/java/com/example/myplugin/
├── MyPlugin.java                 // главный класс, только инициализация
├── command/
│   ├── MyCommand.java
│   └── SubCommand.java
├── listener/
│   ├── PlayerListener.java
│   ├── BlockListener.java
│   ├── CombatListener.java
│   └── MenuListener.java         // единый обработчик всех GUI
├── gui/
│   ├── Menu.java                 // базовый класс меню (InventoryHolder)
│   ├── Button.java               // кнопка = предмет + действие
│   ├── PaginatedMenu.java
│   ├── MenuManager.java
│   └── impl/MainMenu.java
├── manager/
│   ├── ConfigManager.java
│   ├── MessageManager.java
│   ├── CooldownManager.java
│   └── PlayerDataManager.java
├── data/
│   ├── PlayerData.java           // модель данных
│   ├── Storage.java              // интерфейс
│   ├── YamlStorage.java
│   └── SqlStorage.java
├── item/
│   ├── ItemBuilder.java
│   └── ItemKeys.java             // NamespacedKey-константы
├── util/
│   ├── Text.java                 // цвета/MiniMessage
│   ├── Sounds.java
│   └── Scheduler.java            // обёртка под Bukkit/Folia
└── api/                          // публичные события и сервисы
    └── event/MyCustomEvent.java
src/main/resources/
├── plugin.yml
├── config.yml
└── messages.yml
```

### Правила взаимодействия классов
- Главный класс **создаёт** менеджеры и **передаёт** их через конструкторы (DI вручную). Не плодить `static` синглтоны без нужды; допустим `MyPlugin.getInstance()` только для планировщиков/ключей.
- Порядок инициализации в `onEnable`: config → messages → storage → managers → listeners → commands → задачи.
- Порядок в `onDisable`: отменить задачи → закрыть открытые меню → сохранить данные (синхронно) → закрыть пул БД.
- Слушатели не содержат бизнес-логики: они берут данные из события и вызывают менеджер/сервис.
- Менеджеры не знают о командах и GUI. GUI знает о менеджерах, но не о слушателях.
- Между слабо связанными частями используй **собственные события** (`extends Event`, `callEvent`), а не прямые ссылки.
- Модель данных (`PlayerData`) — чистый класс без Bukkit-объектов (только `UUID`, примитивы, строки).

---

## 2. GRADLE / PLUGIN.YML

### build.gradle.kts
```kotlin
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io") // Vault
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT") // подставь свою 1.21.x
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.example.myplugin.libs.hikari")
    }
    build { dependsOn(shadowJar) }
    processResources {
        filesMatching("plugin.yml") { expand("version" to project.version) }
    }
}
```

### plugin.yml
```yaml
name: MyPlugin
version: '${version}'
main: com.example.myplugin.MyPlugin
api-version: '1.21'
authors: [ejyqyl]
description: Example plugin
softdepend: [PlaceholderAPI, Vault, WorldGuard]
# folia-supported: true   # только если весь код совместим с Folia

commands:
  myplugin:
    description: Главная команда
    aliases: [mp]
    usage: /<command> help
    permission: myplugin.use

permissions:
  myplugin.use:
    default: true
  myplugin.admin:
    default: op
    children:
      myplugin.use: true
      myplugin.reload: true
  myplugin.reload:
    default: op
```
Правила: `depend` — плагин не запустится без зависимости; `softdepend` — опциональная (проверяй `getServer().getPluginManager().isPluginEnabled("X")` перед обращением к классам зависимости, иначе `NoClassDefFoundError`). `loadbefore`/`load: STARTUP|POSTWORLD` — только если реально нужно.

---

## 3. ГЛАВНЫЙ КЛАСС

```java
public final class MyPlugin extends JavaPlugin {

    private static MyPlugin instance;
    private ConfigManager configManager;
    private MessageManager messages;
    private PlayerDataManager dataManager;
    private CooldownManager cooldowns;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);

        configManager = new ConfigManager(this);
        messages      = new MessageManager(this);
        dataManager   = new PlayerDataManager(this);
        cooldowns     = new CooldownManager();
        menuManager   = new MenuManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, dataManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(menuManager), this);

        MyCommand cmd = new MyCommand(this);
        getCommand("myplugin").setExecutor(cmd);
        getCommand("myplugin").setTabCompleter(cmd);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MyExpansion(this).register();
        }
        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (menuManager != null) menuManager.closeAll();
        if (dataManager != null) dataManager.saveAllSync();
    }

    public static MyPlugin getInstance() { return instance; }
    // геттеры менеджеров...
}
```

---

## 4. КОМАНДЫ

### Классический вариант (работает везде)
```java
public class MyCommand implements CommandExecutor, TabCompleter {
    private final MyPlugin plugin;
    public MyCommand(MyPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("myplugin.reload")) {
                    plugin.getMessages().send(sender, "no-permission"); return true;
                }
                plugin.reloadConfig();
                plugin.getMessages().reload();
                plugin.getMessages().send(sender, "reloaded");
            }
            case "menu" -> {
                if (!(sender instanceof Player p)) {
                    plugin.getMessages().send(sender, "players-only"); return true;
                }
                plugin.getMenuManager().open(p, new MainMenu(plugin, p));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c,
                                      @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("menu", "reload", "help")
                    .filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
```
Правила команд:
- Всегда проверяй `instanceof Player`, права, длину `args`, парсинг чисел (`try/catch NumberFormatException`).
- Целевого игрока ищи через `Bukkit.getPlayerExact(name)` (не `getOfflinePlayer(name)` в основном потоке — это блокирующий запрос).
- Не выполняй тяжёлую работу в команде — уводи в async.
- Возвращай `true`, если команда обработана (сообщение сам отправляешь); `false` покажет `usage`.

### Brigadier (Paper 1.20.6+)
```java
@Override
public void onEnable() {
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
        Commands c = event.registrar();
        c.register(Commands.literal("hello")
            .requires(src -> src.getSender().hasPermission("myplugin.use"))
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes(ctx -> {
                    Player t = ctx.getArgument("target", PlayerSelectorArgumentResolver.class)
                            .resolve(ctx.getSource()).getFirst();
                    ctx.getSource().getSender().sendMessage(Component.text("Hi " + t.getName()));
                    return Command.SINGLE_SUCCESS;
                }))
            .build());
    });
}
```
Команды Brigadier не нужно объявлять в plugin.yml. Не смешивай два подхода для одной команды.

---

## 5. СЛУШАТЕЛИ СОБЫТИЙ

### Основы
```java
public class PlayerListener implements Listener {
    private final MyPlugin plugin;
    private final PlayerDataManager data;
    // конструктор...

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        data.loadAsync(p.getUniqueId());
        e.joinMessage(null); // убрать стандартное сообщение
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        data.saveAndUnload(e.getPlayer().getUniqueId());
    }
}
```

### Приоритеты (порядок выполнения)
`LOWEST → LOW → NORMAL → HIGH → HIGHEST → MONITOR`
- `LOWEST/LOW` — ты хочешь сработать первым (можешь отменить, чтобы другие не видели).
- `HIGH/HIGHEST` — ты хочешь сработать последним и переопределить решения других плагинов.
- `MONITOR` — **только наблюдение**, событие менять нельзя (логи, статистика).
- `ignoreCancelled = true` — не получать уже отменённые события (почти всегда нужно для защитных/игровых механик).

### Частые события по категориям
**Игрок:** `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerInteractEvent` (приходит дважды: проверяй `e.getHand() == EquipmentSlot.HAND`), `PlayerInteractEntityEvent`, `PlayerMoveEvent` (очень частое — только лёгкие проверки, сравнивай блоки `getFrom().getBlockX()...`), `PlayerTeleportEvent`, `PlayerDeathEvent`, `PlayerRespawnEvent`, `PlayerDropItemEvent`, `PlayerItemHeldEvent`, `PlayerSwapHandItemsEvent`, `PlayerCommandPreprocessEvent`, `AsyncChatEvent` (Paper, async!), `PlayerToggleSneakEvent`, `PlayerItemConsumeEvent`, `PlayerBucketEmptyEvent`.
**Блоки:** `BlockBreakEvent`, `BlockPlaceEvent`, `BlockExplodeEvent`, `BlockBurnEvent`, `BlockFromToEvent` (вода/лава), `BlockPistonExtendEvent/RetractEvent`, `BlockDispenseEvent`, `BlockPhysicsEvent` (крайне частое — избегать).
**Сущности/бой:** `EntityDamageEvent`, `EntityDamageByEntityEvent`, `EntityDeathEvent`, `EntityExplodeEvent`, `EntitySpawnEvent`, `CreatureSpawnEvent`, `ProjectileLaunchEvent`, `ProjectileHitEvent`, `EntityTargetEvent`, `EntityShootBowEvent`.
**Инвентарь:** `InventoryClickEvent`, `InventoryDragEvent`, `InventoryCloseEvent`, `InventoryOpenEvent`, `InventoryMoveItemEvent` (воронки), `CraftItemEvent`, `PrepareItemCraftEvent`, `EnchantItemEvent`.
**Мир/прочее:** `ChunkLoadEvent`, `WorldSaveEvent`, `WeatherChangeEvent`, `ServerLoadEvent`.

### Практические паттерны
- **Урон от игрока по игроку:** получай атакующего с учётом снарядов:
```java
Player attacker = null;
if (e.getDamager() instanceof Player p) attacker = p;
else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
```
- **Взрывы:** менять список `e.blockList()` (удалять блоки, которые нельзя ломать), `e.setYield(0f)` — убрать дроп.
- **PlayerInteractEvent:** проверяй `e.getAction()`, `e.getClickedBlock()` (может быть `null`), `e.hasItem()`.
- **Отмена:** `e.setCancelled(true)`; для `PlayerInteractEvent` дополнительно `e.setUseInteractedBlock(Event.Result.DENY)` при необходимости.
- **Пользовательские события:**
```java
public class RaidStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Player starter;
    public RaidStartEvent(Player starter) { this.starter = starter; }
    public Player getStarter() { return starter; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean c) { cancelled = c; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
// вызов:
RaidStartEvent ev = new RaidStartEvent(p);
Bukkit.getPluginManager().callEvent(ev);
if (ev.isCancelled()) return;
```
- Не регистрируй слушатель повторно (дубли = двойные срабатывания). Не забывай `@EventHandler`.
- В `AsyncChatEvent` нельзя трогать мир/сущности — только вычисления и `Bukkit.getScheduler().runTask(...)` для возврата.

---

## 6. ТЕКСТ И ЦВЕТА

### Три системы цвета
| Система | Пример | Где использовать |
|---|---|---|
| MiniMessage | `<red>Текст</red>`, `<#ff5500>`, `<gradient:red:blue>` | Paper 1.16.5+ с Adventure, **рекомендуется** |
| Legacy `&` | `&aТекст`, `&#RRGGBB` (нужен парсер) | Старые конфиги, совместимость со старыми плагинами |
| Section `§` | `§aТекст` | Внутреннее представление, не пиши руками |

### Таблица legacy-кодов
Цвета: `&0` чёрный, `&1` тёмно-синий, `&2` тёмно-зелёный, `&3` тёмно-бирюзовый, `&4` тёмно-красный, `&5` фиолетовый, `&6` золотой, `&7` серый, `&8` тёмно-серый, `&9` синий, `&a` зелёный, `&b` бирюзовый, `&c` красный, `&d` розовый, `&e` жёлтый, `&f` белый.
Форматирование: `&l` жирный, `&o` курсив, `&n` подчёркнутый, `&m` зачёркнутый, `&k` обфускация, `&r` сброс.

### Универсальный утилитарный класс
```java
public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Text() {}

    /** MiniMessage + плейсхолдеры вида <name> */
    public static Component mm(String s, TagResolver... resolvers) {
        return MM.deserialize(s, resolvers);
    }

    /** Legacy (&, &#RRGGBB) -> Component, без курсива по умолчанию */
    public static Component legacy(String s) {
        return LEGACY.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    /** Автоопределение: если есть '<' и '>' — считаем MiniMessage, иначе legacy */
    public static Component parse(String s, TagResolver... r) {
        if (s == null) return Component.empty();
        boolean looksMini = s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<');
        Component c = looksMini ? mm(s, r) : legacy(s);
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static List<Component> parse(List<String> lines, TagResolver... r) {
        return lines.stream().map(l -> parse(l, r)).toList();
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    public static TagResolver ph(String key, String value) {
        return Placeholder.unparsed(key, value);   // безопасно: игрок не внедрит теги
    }
    public static TagResolver phc(String key, Component value) {
        return Placeholder.component(key, value);
    }
}
```
**Важно:** пользовательский ввод (ники, сообщения игроков) подставляй через `Placeholder.unparsed`, иначе игрок сможет вставить `<click:run_command:...>`. 

### MiniMessage — шпаргалка
- Цвета: `<red>`, `<#RRGGBB>`, `<color:#ff8800>`
- Стили: `<bold>`/`<b>`, `<italic>`/`<i>`, `<underlined>`/`<u>`, `<strikethrough>`/`<st>`, `<obfuscated>`/`<obf>`, `<reset>`
- Градиент: `<gradient:#ff0000:#0000ff>Текст</gradient>`; радуга: `<rainbow>Текст</rainbow>`
- Клик: `<click:run_command:/spawn>`, `<click:suggest_command:/msg >`, `<click:open_url:https://...>`, `<click:copy_to_clipboard:текст>`
- Наведение: `<hover:show_text:'<gray>Подсказка'>`
- Перенос: `<newline>` или `<br>`
- Шрифт/переводы: `<font:minecraft:uniform>`, `<lang:item.minecraft.diamond>`, `<key:key.jump>`
- Закрывающие теги нужны не всегда, но для чётких границ лучше закрывать.

### Отправка
```java
player.sendMessage(Text.mm("<green>Привет, <name>!", Text.ph("name", player.getName())));
player.sendActionBar(Text.mm("<yellow>Кулдаун: 5с"));
player.showTitle(Title.title(Text.mm("<gold>Заголовок"), Text.mm("<gray>Подзаголовок"),
        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
Bukkit.broadcast(Text.mm("<red>Рейд начался!"));
```

### MessageManager (messages.yml)
```yaml
prefix: "<gradient:#ff5555:#ffaa00><b>MyPlugin</b></gradient> <dark_gray>»</dark_gray> "
no-permission: "<prefix><red>Нет прав."
reloaded: "<prefix><green>Конфиг перезагружен."
cooldown: "<prefix><yellow>Подожди <seconds>с."
```
```java
public class MessageManager {
    private final MyPlugin plugin;
    private FileConfiguration cfg;
    public MessageManager(MyPlugin p) { this.plugin = p; reload(); }
    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }
    public void send(CommandSender to, String key, TagResolver... r) {
        String raw = cfg.getString(key, "<red>Missing message: " + key);
        TagResolver prefix = Placeholder.parsed("prefix", cfg.getString("prefix", ""));
        to.sendMessage(Text.mm(raw, combine(prefix, r)));
    }
    private TagResolver combine(TagResolver a, TagResolver[] rest) {
        return TagResolver.resolver(a, TagResolver.resolver(rest));
    }
}
```

### Цвета в GUI/предметах
- Название и lore предметов по умолчанию **курсивные** — всегда `decoration(ITALIC, false)` (метод `Text.parse` уже делает это).
- Для единого стиля создай палитру в конфиге: `colors.primary: "#FF5555"`, `colors.secondary: "#AAAAAA"`, и подставляй через `<primary>` (`TagResolver.resolver("primary", Tag.styling(TextColor.fromHexString(...)))`).
- Цвета в заголовках инвентаря поддерживаются, но длинные градиенты могут выглядеть плохо — тестируй.
- Для цветов брони/зелий/фейерверков используй `org.bukkit.Color.fromRGB(...)`, а не `TextColor`.

---

## 7. ПРЕДМЕТЫ: ItemStack, ItemMeta, PDC

### ItemBuilder
```java
public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material m) { this.item = new ItemStack(m); this.meta = item.getItemMeta(); }
    public ItemBuilder amount(int a) { item.setAmount(Math.max(1, Math.min(a, 99))); return this; }
    public ItemBuilder name(String s, TagResolver... r) { meta.displayName(Text.parse(s, r)); return this; }
    public ItemBuilder lore(List<String> lines, TagResolver... r) { meta.lore(Text.parse(lines, r)); return this; }
    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }
    public ItemBuilder flags(ItemFlag... f) { meta.addItemFlags(f); return this; }
    public ItemBuilder unbreakable() { meta.setUnbreakable(true); meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); return this; }
    public ItemBuilder model(NamespacedKey itemModel) { meta.setItemModel(itemModel); return this; } // 1.21.2+
    public ItemBuilder tag(NamespacedKey k, String v) {
        meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, v); return this;
    }
    public ItemBuilder tag(NamespacedKey k, int v) {
        meta.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, v); return this;
    }
    public ItemStack build() { item.setItemMeta(meta); return item; }
}
```
Замечания:
- В Paper 1.21+ `HIDE_ATTRIBUTES` и часть флагов работают иначе из-за data components; если нужно скрыть атрибуты, проверь актуальный способ на своей версии.
- Enchantment теперь через `Enchantment.UNBREAKING` (не `DURABILITY`) в новых API; названия зависят от версии.
- `ItemStack.clone()` перед выдачей нескольким игрокам.
- `item.getItemMeta()` возвращает копию — не забывай `setItemMeta`.

### PersistentDataContainer (PDC) — правильный способ метить предметы/блоки/сущности
```java
public final class ItemKeys {
    public static NamespacedKey ID, ACTION, OWNER;
    public static void init(Plugin p) {
        ID     = new NamespacedKey(p, "item_id");
        ACTION = new NamespacedKey(p, "gui_action");
        OWNER  = new NamespacedKey(p, "owner");
    }
}

// чтение
String id = item.getItemMeta().getPersistentDataContainer().get(ItemKeys.ID, PersistentDataType.STRING);
// проверка
boolean isMine = item != null && item.hasItemMeta()
        && item.getItemMeta().getPersistentDataContainer().has(ItemKeys.ID, PersistentDataType.STRING);
```
- Никогда не определяй кастомный предмет по названию/lore — только PDC.
- PDC есть у: `ItemMeta`, `Entity`, `Chunk`, `World`, `TileState` (блоки-контейнеры, табличка, спавнер). Для обычных блоков (камень, динамит) храни координаты в своей БД/`Chunk` PDC.
- Тип данных: `STRING`, `INTEGER`, `LONG`, `DOUBLE`, `BYTE`, `BYTE_ARRAY`, `INTEGER_ARRAY`, `TAG_CONTAINER`.

### Головы игроков и текстуры
```java
ItemStack head = new ItemStack(Material.PLAYER_HEAD);
SkullMeta sm = (SkullMeta) head.getItemMeta();
sm.setOwningPlayer(offlinePlayer); // или профиль с текстурой:
PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
profile.setProperty(new ProfileProperty("textures", base64Texture));
sm.setPlayerProfile(profile);
head.setItemMeta(sm);
```

### Выдача предметов
```java
Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
```
Всегда обрабатывай остаток при полном инвентаре.

### Рецепты
```java
NamespacedKey key = new NamespacedKey(plugin, "super_pick");
ShapedRecipe r = new ShapedRecipe(key, resultItem);
r.shape("DDD", " S ", " S ");
r.setIngredient('D', Material.DIAMOND);
r.setIngredient('S', Material.STICK);
Bukkit.addRecipe(r);
// в onDisable: Bukkit.removeRecipe(key);
```
Для проверки кастомных ингредиентов — `PrepareItemCraftEvent`/`CraftItemEvent` и PDC (`RecipeChoice.ExactChoice`).

---

## 8. ПЛАНИРОВЩИК И ПОТОКИ

### Золотые правила
1. **Bukkit/Paper API (миры, блоки, сущности, инвентари, игроки) — только главный поток** (на Folia — поток нужного региона).
2. **БД, файлы, HTTP, тяжёлые вычисления — только async.**
3. Возврат из async в главный: `runTask`.
4. Не блокируй главный поток: никаких `Thread.sleep`, `future.get()`, синхронных запросов.
5. Задачи отменяй: `BukkitTask#cancel()` или в `onDisable` — `cancelTasks(this)`.
6. Не храни `Player`, `World`, `Entity`, `Chunk` в долгоживущих коллекциях — храни `UUID` и получай объект в момент использования (`Bukkit.getPlayer(uuid)` → проверка `null`).

### Паттерны
```java
// однократно, через N тиков (20 тиков = 1 секунда)
Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendMessage("..."), 20L * 5);

// периодически
BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, () -> { ... }, 0L, 20L);

// async -> sync
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    PlayerData d = storage.load(uuid);              // блокирующее
    Bukkit.getScheduler().runTask(plugin, () -> {   // назад в главный поток
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) apply(p, d);
    });
});

// самоотменяющийся таймер (обратный отсчёт)
new BukkitRunnable() {
    int left = 10;
    @Override public void run() {
        if (left-- <= 0) { cancel(); return; }
        // ...
    }
}.runTaskTimer(plugin, 0L, 20L);
```

### CompletableFuture-стиль
```java
CompletableFuture.supplyAsync(() -> storage.load(uuid), asyncExecutor)
    .thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> apply(data)))
    .exceptionally(ex -> { plugin.getLogger().log(Level.SEVERE, "load failed", ex); return null; });
```

### Folia-совместимость (если нужна)
```java
player.getScheduler().run(plugin, task -> { /* действие над игроком */ }, null);
Bukkit.getRegionScheduler().run(plugin, location, task -> { /* действие над регионом */ });
Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> { }, 20L);
Bukkit.getAsyncScheduler().runNow(plugin, task -> { });
```
Оберни в `util/Scheduler`, определяя Folia через `Class.forName("io.papermc.paper.threadedregions.RegionizedServer")`. Не используй `BukkitScheduler` на Folia.

---

## 9. КОНФИГУРАЦИЯ

### config.yml
```yaml
settings:
  language: ru
  debug: false
  save-interval-seconds: 300
gui:
  main:
    title: "<gradient:#ff5555:#ffaa00>Главное меню</gradient>"
    rows: 3
    filler: GRAY_STAINED_GLASS_PANE
    items:
      shop:
        slot: 11
        material: EMERALD
        name: "<green><b>Магазин"
        lore:
          - "<gray>Нажми, чтобы открыть"
        action: "open_menu:shop"
      close:
        slot: 22
        material: BARRIER
        name: "<red>Закрыть"
        action: "close"
mechanics:
  cooldowns:
    ability: 30
  explosion:
    power: 4.0
    break-blocks: true
```

### ConfigManager
```java
public class ConfigManager {
    private final MyPlugin plugin;
    private boolean debug;
    private int saveInterval;

    public ConfigManager(MyPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        debug = c.getBoolean("settings.debug", false);
        saveInterval = Math.max(30, c.getInt("settings.save-interval-seconds", 300));
    }
    // геттеры
}
```
Правила:
- Кэшируй значения в поля при загрузке, не читай `getConfig()` в горячих путях (каждый тик/ивент).
- Всегда давай значения по умолчанию: `getInt(path, def)`.
- Материалы: `Material.matchMaterial(str)`, при `null` → лог-предупреждение и запасной материал.
- Звуки/частицы/эффекты: парсить через `Registry` или `valueOf` в `try/catch`, не падать на ошибке пользователя.
- `saveResource("file.yml", false)` — не перезатирать существующий.
- Миграция конфига: храни `config-version`, при старой версии добавляй недостающие ключи, не удаляя пользовательские.
- Комментарии в YAML при `saveConfig()` могут теряться на старых версиях — на 1.18.1+ Paper сохраняет их лучше, но не гарантированно.

### Собственные YAML-файлы
```java
File f = new File(plugin.getDataFolder(), "data.yml");
YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
y.set("players." + uuid + ".coins", 100);
try { y.save(f); } catch (IOException ex) { plugin.getLogger().log(Level.SEVERE, "save", ex); }
```
Сохранение — в async, копируя данные заранее.

---

## 10. GUI: МЕНЮ, КНОПКИ, ОБРАБОТЧИКИ

### Архитектура
- **Menu** — абстрактный класс, реализует `InventoryHolder`. Хранит `Inventory`, карту `slot → Button`, владельца (`UUID`).
- **Button** — предмет + действия (`onClick`).
- **MenuListener** — один слушатель на все меню: определяет по `InventoryHolder`, что это наше меню, и делегирует.
- **MenuManager** — открытие/закрытие, список открытых, `closeAll()`.

Определять своё меню **только** через `holder instanceof Menu`, а не по названию окна.

### Button
```java
public final class Button {
    private final ItemStack icon;
    private final Consumer<ButtonClick> action;

    public Button(ItemStack icon, Consumer<ButtonClick> action) { this.icon = icon; this.action = action; }
    public Button(ItemStack icon) { this(icon, c -> {}); }

    public ItemStack icon() { return icon; }
    public void click(ButtonClick c) { action.accept(c); }

    public record ButtonClick(Player player, ClickType type, int slot, InventoryClickEvent event) {
        public boolean left()  { return type.isLeftClick(); }
        public boolean right() { return type.isRightClick(); }
        public boolean shift() { return type.isShiftClick(); }
    }
}
```

### Menu (базовый)
```java
public abstract class Menu implements InventoryHolder {
    protected final MyPlugin plugin;
    protected final Player viewer;
    protected Inventory inventory;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private boolean allowPlayerInventoryClicks = false;

    protected Menu(MyPlugin plugin, Player viewer) { this.plugin = plugin; this.viewer = viewer; }

    protected abstract Component title();
    protected abstract int rows();               // 1..6
    protected abstract void build();             // расстановка кнопок

    public void open() {
        inventory = Bukkit.createInventory(this, rows() * 9, title());
        buttons.clear();
        build();
        render();
        viewer.openInventory(inventory);
    }

    protected void set(int slot, Button b) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        buttons.put(slot, b);
    }
    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) buttons.putIfAbsent(i, new Button(filler));
    }
    protected void border(ItemStack filler) {
        int size = inventory.getSize(), rows = size / 9;
        for (int i = 0; i < size; i++) {
            int r = i / 9, c = i % 9;
            if (r == 0 || r == rows - 1 || c == 0 || c == 8) buttons.putIfAbsent(i, new Button(filler));
        }
    }
    public void render() {
        inventory.clear();
        buttons.forEach((slot, b) -> inventory.setItem(slot, b.icon()));
    }
    public void refresh() { buttons.clear(); build(); render(); }

    /** Вызывается MenuListener */
    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true); // по умолчанию блокируем ВСЁ
        if (e.getClickedInventory() == null) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getClickedInventory().equals(inventory)) return;    // клик по инвентарю игрока — игнор
        Button b = buttons.get(e.getSlot());
        if (b != null) b.click(new Button.ButtonClick(p, e.getClick(), e.getSlot(), e));
    }
    public void handleClose(InventoryCloseEvent e) { /* переопределить при необходимости */ }

    @Override public @NotNull Inventory getInventory() { return inventory; }
    public Player viewer() { return viewer; }
}
```

### MenuListener — все нюансы кликов
```java
public class MenuListener implements Listener {
    private final MenuManager manager;
    public MenuListener(MenuManager m) { this.manager = m; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Menu menu)) return;

        // 1) блокируем shift-клик из инвентаря игрока в меню и «сбор» предметов двойным кликом
        if (e.getClickedInventory() != null && !e.getClickedInventory().equals(top)) {
            if (e.isShiftClick() || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) e.setCancelled(true);
            // клики по своему инвентарю без shift можно разрешить, но обычно блокируем:
            e.setCancelled(true);
            return;
        }
        // 2) клавиши 1-9 (NUMBER_KEY), F (SWAP_OFFHAND), Q (DROP) — тоже отменяются в handleClick
        menu.handleClick(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) {
            // блокируем перетаскивание, если оно затрагивает слоты меню
            int size = e.getView().getTopInventory().getSize();
            for (int raw : e.getRawSlots()) if (raw < size) { e.setCancelled(true); return; }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Menu menu) menu.handleClose(e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { manager.forget(e.getPlayer().getUniqueId()); }
}
```
Ключевые нюансы (частые причины дюпов):
- `e.setCancelled(true)` **до** любых действий и по умолчанию для всего меню.
- Блокируй `InventoryDragEvent`, `SHIFT+клик` из нижнего инвентаря, `MOVE_TO_OTHER_INVENTORY`, `COLLECT_TO_CURSOR`, `HOTBAR_SWAP`, `NUMBER_KEY`, `SWAP_OFFHAND`, `DROP_*`.
- Используй `getHolder(false)` (Paper) — не создаёт снапшот блока-контейнера.
- Действие, которое выдаёт/забирает предметы или деньги — выполняй **после** проверок и внутри одного тика; защищайся от повторного клика (флаг `processing` или кулдаун 2–5 тиков).
- Закрытие меню во время клика делай через `Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory())`, а не прямо внутри события.
- Не открывай другой инвентарь прямо внутри `InventoryCloseEvent` без задержки в 1 тик.

### Пример конкретного меню
```java
public class MainMenu extends Menu {
    public MainMenu(MyPlugin plugin, Player viewer) { super(plugin, viewer); }

    @Override protected Component title() { return Text.mm("<gradient:#ff5555:#ffaa00>Главное меню</gradient>"); }
    @Override protected int rows() { return 3; }

    @Override protected void build() {
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("<gray> ").build();

        set(11, new Button(
            new ItemBuilder(Material.EMERALD).name("<green><b>Магазин")
                .lore(List.of("<gray>ЛКМ — открыть", "<gray>ПКМ — информация")).build(),
            c -> {
                if (c.left())  plugin.getMenuManager().open(c.player(), new ShopMenu(plugin, c.player()));
                if (c.right()) c.player().sendMessage(Text.mm("<yellow>Информация о магазине"));
                Sounds.click(c.player());
            }));

        set(15, new Button(
            new ItemBuilder(Material.DIAMOND_SWORD).name("<red><b>Арена").glow().build(),
            c -> { c.player().closeInventory(); /* телепорт и т.д. */ }));

        set(22, new Button(new ItemBuilder(Material.BARRIER).name("<red>Закрыть").build(),
            c -> Bukkit.getScheduler().runTask(plugin, () -> c.player().closeInventory())));

        fill(filler);
    }
}
```

### Пагинация
```java
public abstract class PaginatedMenu<T> extends Menu {
    protected int page = 0;
    protected final List<T> entries;
    protected static final int[] SLOTS = /* 28 слотов внутри рамки для 6 рядов */
        {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};

    protected PaginatedMenu(MyPlugin p, Player v, List<T> entries) { super(p, v); this.entries = entries; }
    @Override protected int rows() { return 6; }
    protected abstract ItemStack iconFor(T entry);
    protected abstract void onEntryClick(T entry, Button.ButtonClick c);

    @Override protected void build() {
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) SLOTS.length));
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && from + i < entries.size(); i++) {
            T e = entries.get(from + i);
            set(SLOTS[i], new Button(iconFor(e), c -> onEntryClick(e, c)));
        }
        if (page > 0) set(48, new Button(new ItemBuilder(Material.ARROW).name("<yellow>← Назад").build(),
                c -> { page--; refresh(); }));
        set(49, new Button(new ItemBuilder(Material.PAPER).name("<gray>Стр. " + (page + 1) + "/" + pages).build()));
        if (page < pages - 1) set(50, new Button(new ItemBuilder(Material.ARROW).name("<yellow>Вперёд →").build(),
                c -> { page++; refresh(); }));
        border(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
    }
}
```

### Динамические кнопки
- Обновление без переоткрытия: `refresh()` → `render()`; для анимаций/таймеров — `BukkitRunnable`, который проверяет, что игрок всё ещё смотрит именно это меню: `p.getOpenInventory().getTopInventory().getHolder(false) == this`. Иначе — `cancel()`.
- Toggle-кнопка: меняй материал (`LIME_DYE` ↔ `GRAY_DYE`) и обновляй через `refresh()`.
- Кнопки-счётчики: ЛКМ +1, ПКМ −1, Shift ×10 — через `ButtonClick`.

### Слоты и раскладка
- Слот = `row * 9 + col` (0-индекс). Размеры: 9, 18, 27, 36, 45, 54.
- Центр 3-рядового меню: слот 13. Центр 6-рядового: 22/31.
- Тип инвентаря не сундук: `Bukkit.createInventory(holder, InventoryType.HOPPER, title)` (5 слотов), `DISPENSER` (9), `BREWING`, `ANVIL` и др. — учитывай их специальное поведение.

### Ввод текста от игрока
- **AnvilGUI** (библиотека) — надёжнее всего для ввода строки.
- Чат-ввод: помести игрока в `Map<UUID, Consumer<String>> awaiting`, слушай `AsyncChatEvent`, отмени событие, обработай в главном потоке; добавь таймаут и слово `cancel`.
- Sign-ввод требует ProtocolLib/пакетов — избегать без необходимости.

### GUI из конфига (как DeluxeMenus)
- Формат пункта: `slot`, `material`, `name`, `lore`, `amount`, `glow`, `custom_model_data`, `action` / `left_click_actions` / `right_click_actions`, `view_requirement`.
- Действия строками: `[player] cmd`, `[console] cmd`, `[message] текст`, `[sound] NAME`, `[close]`, `[open] menu`, `[refresh]`. Разбирай парсером `"[tag] payload"` в enum + строку.
- Требования: `permission`, `money >= N`, `has item`. Проверяй перед выполнением действий, при неудаче — `deny_commands`.
- Плейсхолдеры `%player_name%` — только при `PlaceholderAPI.isEnabled`, иначе подставляй свои `<name>`.

---

## 11. ИГРОВЫЕ МЕХАНИКИ — ПАТТЕРНЫ

### 11.1 Кулдауны
```java
public class CooldownManager {
    private final Map<String, Long> map = new ConcurrentHashMap<>();
    private String key(UUID u, String id) { return u + ":" + id; }

    public boolean isOnCooldown(UUID u, String id) {
        Long until = map.get(key(u, id));
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) { map.remove(key(u, id)); return false; }
        return true;
    }
    public long remainingMs(UUID u, String id) {
        return Math.max(0, map.getOrDefault(key(u, id), 0L) - System.currentTimeMillis());
    }
    public void set(UUID u, String id, long seconds) { map.put(key(u, id), System.currentTimeMillis() + seconds * 1000); }
    public void clear(UUID u) { map.keySet().removeIf(k -> k.startsWith(u.toString())); }
}
```
Чисти при выходе игрока. Время — по `System.currentTimeMillis()` (переживает лаги TPS), а не по счётчику тиков.

### 11.2 Способности/предметы с эффектом
- Триггер: `PlayerInteractEvent` (ПКМ предметом с PDC-меткой).
- Проверки: рука, метка, кулдаун, мана/ресурс, регион (можно ли PvP/строить).
- Эффект: урон, эффекты зелий (`player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*10, 1))`), частицы, звук, снаряд.
- Затем: списать ресурс/прочность, поставить кулдаун, показать actionbar.

### 11.3 Снаряды и рейкасты
```java
// Пользовательский снаряд
Snowball s = player.launchProjectile(Snowball.class, player.getLocation().getDirection().multiply(2));
s.getPersistentDataContainer().set(ItemKeys.ID, PersistentDataType.STRING, "fireball");
// ProjectileHitEvent: проверить PDC, e.getHitBlock()/getHitEntity(), создать взрыв/эффект

// Рейкаст (прицеливание)
RayTraceResult r = player.getWorld().rayTraceEntities(player.getEyeLocation(),
        player.getEyeLocation().getDirection(), 30, 0.3, en -> !en.equals(player) && en instanceof LivingEntity);
RayTraceResult rb = player.rayTraceBlocks(30);
```

### 11.4 Урон и бой
- `EntityDamageByEntityEvent#setDamage`, `setCancelled`. Учитывай броню/защиту: финальный урон — `getFinalDamage()`.
- Комбо/тег боя (combat log): при уроне записывай `lastCombat[UUID] = now`; при выходе в бою — наказание (убить/дроп предметов).
- Новая модель кулдауна атаки 1.9+: `AttributeInstance` `ATTACK_SPEED`; отключение кулдауна — атрибут `Attribute.ATTACK_SPEED` (имя константы зависит от версии).
- Кастомные атрибуты предметов — `ItemMeta#addAttributeModifier(Attribute, AttributeModifier)`; в 1.21+ `AttributeModifier` использует `NamespacedKey`.

### 11.5 Взрывы и разрушаемые блоки (рейды, динамит, усиленные блоки)
- Создание взрыва: `world.createExplosion(loc, power, setFire, breakBlocks, sourceEntity)`. Сила: TNT ≈ 4.0, крипер ≈ 3.0.
- Динамит-предмет: метка PDC → при ПКМ/установке спавни `TNTPrimed` (`world.spawn(loc, TNTPrimed.class, t -> { t.setFuseTicks(60); t.getPersistentDataContainer().set(...); })`).
- Свои правила разрушения: в `EntityExplodeEvent` пройдись по `e.blockList()`, для каждого блока с «прочностью» уменьши счётчик и **убери из списка**, пока прочность > 0.
```java
@EventHandler(ignoreCancelled = true)
public void onExplode(EntityExplodeEvent e) {
    Iterator<Block> it = e.blockList().iterator();
    while (it.hasNext()) {
        Block b = it.next();
        ReinforcedBlock rb = reinforced.get(b.getLocation());   // Map<Location, ReinforcedBlock>, ключ — лёгкий (world+x,y,z)
        if (rb == null) continue;
        if (rb.damage(1) > 0) it.remove();       // ещё держится — блок не ломается
        else reinforced.remove(b.getLocation()); // сломался — забыть
    }
}
```
- Храни усиленные блоки по компактному ключу (`long` из `Block#getBlockKey()` + UUID мира) и сохраняй в БД периодически (async).
- Учитывай также `BlockExplodeEvent` (кровать/якорь), пистоны, воду/лаву, огонь, поршни, эндермена — защита блоков должна покрывать все пути разрушения.
- Регион/рейд-блок: проверка «внутри региона» — по кубоиду/сфере, для скорости используй `BoundingBox` и индекс по чанкам.
- Окно рейда: включай/выключай возможность ломать блоки по расписанию (`LocalTime`, сверка раз в минуту).

### 11.6 Регионы и защита
- Свой минимальный регион: `record Region(UUID world, BoundingBox box, UUID owner, Set<UUID> members)`.
- Проверка в `BlockBreakEvent`/`BlockPlaceEvent`/`PlayerInteractEvent`/`EntityExplodeEvent`/`BlockFromToEvent`/поршнях.
- Индекс по чанкам: `Map<Long chunkKey, List<Region>>` — быстрый поиск O(1) вместо перебора всех регионов.
- Для готового решения — WorldGuard API (softdepend), флаги через `RegionQuery#testState`.

### 11.7 Экономика (Vault)
```java
private Economy eco;
private boolean setupEconomy() {
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) return false;
    eco = rsp.getProvider(); return true;
}
// снять: eco.has(p, cost) && eco.withdrawPlayer(p, cost).transactionSuccess()
```
Сначала проверка `has`, потом `withdraw`, потом выдача товара; при неудаче выдачи — `depositPlayer` (откат).

### 11.8 Визуальные эффекты
```java
world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.02);        // count, offsetXYZ, extra
world.spawnParticle(Particle.DUST, loc, 10, new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.5f));
player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
player.spawnParticle(...) // видно только этому игроку
```
Не спавни сотни частиц за тик; ограничивай радиус видимости и частоту.

### 11.9 Бossbar, скорборд, команды-тимы
```java
BossBar bar = BossBar.bossBar(Text.mm("<red>Рейд"), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
player.showBossBar(bar);  bar.progress(0.5f);  player.hideBossBar(bar);
```
- Scoreboard на игрока: `Bukkit.getScoreboardManager().getNewScoreboard()`, обновляй только изменившиеся строки (иначе мерцание). Для сложных — FastBoard/аналог.
- Team: цвет ника/префикс/friendly fire — `Team#color`, `setAllowFriendlyFire(false)`, `setOption(Option.NAME_TAG_VISIBILITY, ...)`.

### 11.10 Мобы и сущности
- Спавн: `world.spawn(loc, Zombie.class, z -> { z.customName(...); z.setPersistent(true); z.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100); z.setHealth(100); })`.
- Метка PDC на кастомном мобе; в `EntityDeathEvent` — свои дропы (`e.getDrops().clear(); e.getDrops().add(...)`).
- AI: `Mob#setTarget`, `Bukkit.getMobGoals()` (Paper) для кастомных целей.
- Не держи ссылки на сущности; чисти задачи, связанные с ними, при их смерти/выгрузке чанка.

### 11.11 Телепортация
- Paper: `player.teleportAsync(loc)` (возвращает `CompletableFuture<Boolean>`), безопаснее для чанков.
- Задержка с отменой при движении: запомни стартовый блок и сравнивай в `PlayerMoveEvent` (только смена блока) или в таймере.
- Безопасная точка: проверь `isSolid` под ногами и проходимость двух блоков над ней.

### 11.12 Права и ранги
- `player.hasPermission("x.y")`; динамические числовые права (`myplugin.limit.5`) — перебирай `getEffectivePermissions()` **один раз** и кэшируй.
- Интеграция LuckPerms — через его API (softdepend), не через команды консоли.

---

## 12. ХРАНЕНИЕ ДАННЫХ

### Выбор
| Тип | Когда |
|---|---|
| YAML | Малые объёмы, конфиги, редкие записи |
| SQLite | Один сервер, средние объёмы, без внешней БД |
| MySQL/MariaDB (HikariCP) | Сеть серверов (Velocity), большие объёмы |
| Redis | Кросс-серверные кэши, события между серверами |

### Кэш игрока
```java
public class PlayerDataManager {
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Storage storage;

    public void loadAsync(UUID id) {
        CompletableFuture.supplyAsync(() -> storage.load(id))
            .thenAccept(d -> cache.put(id, d != null ? d : new PlayerData(id)));
    }
    public PlayerData get(UUID id) { return cache.get(id); }   // может быть null, пока грузится

    public void saveAndUnload(UUID id) {
        PlayerData d = cache.remove(id);
        if (d != null) CompletableFuture.runAsync(() -> storage.save(d));
    }
    public void saveAllSync() { cache.values().forEach(storage::save); }   // только в onDisable
}
```
Правила:
- Загрузка при заходе — async; допускай ситуацию «данные ещё не загрузились» (не кикай, а ставь заглушку/повтори).
- Периодическое автосохранение async.
- В БД — `PreparedStatement` (никаких склеек строк → SQL-инъекции). Индексы по UUID.
- UUID храни как `CHAR(36)` или `BINARY(16)`.
- Схема с версией и миграциями (`schema_version`).
- Пул Hikari: `maximumPoolSize` 5–10, закрывать в `onDisable`.

---

## 13. ИНТЕГРАЦИИ

### PlaceholderAPI
```java
public class MyExpansion extends PlaceholderExpansion {
    private final MyPlugin plugin;
    public MyExpansion(MyPlugin p) { this.plugin = p; }
    @Override public @NotNull String getIdentifier() { return "myplugin"; }
    @Override public @NotNull String getAuthor() { return "ejyqyl"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onPlaceholderRequest(Player p, @NotNull String params) {
        if (p == null) return "";
        PlayerData d = plugin.getDataManager().get(p.getUniqueId());
        if (d == null) return "0";
        return switch (params) {
            case "coins" -> String.valueOf(d.coins());
            case "kills" -> String.valueOf(d.kills());
            default -> null;
        };
    }
}
```
Разбор плейсхолдеров в тексте: `PlaceholderAPI.setPlaceholders(player, text)` (строка `%...%`), затем в MiniMessage. Делать только при наличии плагина.

### ProtocolLib / PacketEvents
Только когда Bukkit API не хватает (фейковые блоки, скрытие сущностей, кастомные пакеты). Пакетная работа — в правильном потоке, версии пакетов меняются — привязывай к версии ядра.

### Velocity/BungeeCord (плагинные сообщения)
- Канал `BungeeCord`/`bungeecord:main`: `player.sendPluginMessage(plugin, "BungeeCord", bytes)`; регистрируй канал в `onEnable`.
- Для Velocity — отдельный плагин-модуль на стороне прокси (`@Plugin`, `ProxyServer`), `MinecraftChannelIdentifier`.

### Telegram-боты и внешние API
- Только async, с таймаутами, retry и лимитами. Токены — в `config.yml`, не в коде. Не логируй токены.
- Java 11+ `HttpClient`, JSON — Gson (есть в Paper).

---

## 14. МИРЫ, ЧАНКИ, БЛОКИ

- `world.getBlockAt(x,y,z)` подгружает чанк синхронно — избегай в циклах по большим областям. Проверяй `world.isChunkLoaded(cx, cz)`.
- Массовое изменение блоков: разбивай на порции по N блоков за тик (очередь + таймер).
- Данные блока: `BlockData data = block.getBlockData();` приведение к `Directional`, `Ageable`, `Waterlogged`…; `block.setBlockData(data, false)` — без обновления физики.
- TileState (сундук, табличка, спавнер): `if (block.getState() instanceof Chest c) { c.getBlockInventory(); c.update(); }`.
- Ключ блока для карт: `block.getBlockKey()` (long) + UUID мира вместо `Location` (у `Location` тяжёлый `hashCode` и `World`-ссылка).
- Границы мира: `world.getWorldBorder()`.
- Созданиe мира/структур — ждать окончания загрузки чанков, `getChunkAtAsync`.

---

## 15. ПРОИЗВОДИТЕЛЬНОСТЬ

Чек-лист:
- [ ] Нет тяжёлой логики в `PlayerMoveEvent`, `BlockPhysicsEvent`, `EntityMoveEvent`, `InventoryMoveItemEvent`.
- [ ] Нет обращений к БД/диску/сети в главном потоке.
- [ ] Значения конфига кэшированы в полях.
- [ ] Коллекции очищаются при выходе игрока/выгрузке мира/`onDisable`.
- [ ] Нет `Bukkit.getOnlinePlayers()` в цикле внутри цикла (O(n²)).
- [ ] Таймеры не выполняются чаще, чем нужно (1 тик — только если реально необходимо).
- [ ] Частицы/звуки ограничены по количеству и радиусу.
- [ ] Регулярки и `String.format` не в горячих путях; `Pattern` — статические константы.
- [ ] Нет `String +` в больших циклах — `StringBuilder`.
- [ ] Используй `spark` (профайлер) и `/timings`/`/spark profiler` для проверки.
- [ ] События: `ignoreCancelled = true`, ранний `return` по дешёвым проверкам.

---

## 16. БЕЗОПАСНОСТЬ И АНТИ-ЧИТ ВЕЩИ ДЛЯ GUI/МЕХАНИК

- Никогда не доверяй клиенту: проверяй права и ресурсы **на сервере** при каждом действии.
- Дюпы: любые операции «забрать/выдать» — атомарно; проверяй `inventory.firstEmpty()`, остаток от `addItem`.
- Защита от спама кликов: кулдаун на кнопки (2–5 тиков).
- Не выполняй `Bukkit.dispatchCommand(console, cmd)` со строкой, в которой есть ввод игрока без санации (инъекция команд).
- Не давай игроку самому определять пути к файлам (path traversal).
- Плейсхолдеры игрока — `Placeholder.unparsed` (см. §6).
- Логируй административные действия.
- Права по умолчанию — минимальные (`default: op` для админских).

---

## 17. ТИПИЧНЫЕ ОШИБКИ И ИСПРАВЛЕНИЯ

| Симптом | Причина | Решение |
|---|---|---|
| `NoClassDefFoundError` | Нет зависимости на сервере/не прописан depend | `depend`/`softdepend`, проверка `isPluginEnabled`, shade библиотеки |
| `IllegalStateException: Asynchronous ...` | Вызов Bukkit API из async | `runTask` |
| `NullPointerException` в `getConfig()` | Нет `saveDefaultConfig()` / нет ключа | Дефолты, проверка `null` |
| Событие срабатывает 2 раза | `PlayerInteractEvent` для обеих рук / двойная регистрация | Фильтр `getHand()`, единственная регистрация |
| Курсивное название предмета | Adventure по умолчанию | `decoration(ITALIC, false)` |
| Цвета не работают в 1.16.5 | Старое API | Hex через `net.md_5.bungee.api.ChatColor.of("#RRGGBB")` |
| Меню можно «растащить» | Нет отмены `InventoryDragEvent`/shift-клика | См. §10 |
| Утечка памяти | `Map` с `Player`/`Location` без очистки | UUID, `remove` на quit, weak/expiring cache |
| `ConcurrentModificationException` | Изменение коллекции при итерации | `Iterator.remove()`, копия, `ConcurrentHashMap` |
| Плагин не видит команду | Нет в plugin.yml / не тот `main` | Сверить `plugin.yml` |
| `UnsupportedClassVersionError` | Java на сервере старее, чем target | Выровнять Java |
| Кастомные предметы теряют метку | Клонирование без meta / `setItemMeta` не вызван | Проверить цепочку `getItemMeta → изменения → setItemMeta` |

### Порядок разбора стектрейса
1. Найти первую строку `Caused by:` на самом глубоком уровне.
2. Найти первую строку, содержащую пакет плагина.
3. Определить, событие/команда/таск, откуда вызов.
4. Предложить причину → исправление → как проверить.

---

## 18. ШАБЛОНЫ ОТВЕТОВ МОДЕЛИ

**Запрос «сделай плагин X»:**
1. Уточни/зафиксируй версию, ядро, Java (или примени значения по умолчанию).
2. Дерево классов + краткая роль каждого.
3. `plugin.yml`, `config.yml`, `messages.yml`.
4. Код классов по порядку зависимостей (модель → менеджеры → GUI → слушатели → команды → главный класс).
5. Список прав и команд.
6. Что протестировать (сценарии) и известные ограничения.

**Запрос «исправь ошибку»:** причина → исправленный метод целиком → почему это работает → как проверить.

**Запрос «оптимизируй»:** узкие места по приоритету → конкретные правки → как замерить (spark).

**Запрос «сделай GUI»:** раскладка слотов (таблица), список кнопок и действий, код `Menu`/`Button`/слушателя, конфиг меню (если нужно), защита от дюпов.

---

## 19. ФИНАЛЬНЫЙ ЧЕК-ЛИСТ ПЕРЕД ОТПРАВКОЙ КОДА

- [ ] Все импорты и пакеты указаны/очевидны.
- [ ] Нет вызовов Bukkit API из async.
- [ ] Нет хранения `Player`/`World`/`Entity` в долгоживущих полях.
- [ ] Все `null`-кейсы (`getPlayer`, `getClickedBlock`, `getItemMeta`, `getConfig().getString`) обработаны.
- [ ] Меню определяются через `InventoryHolder`, а не по названию.
- [ ] Клики/drag в меню отменены, дюп-векторы закрыты.
- [ ] Цвета через единый `Text`-утилитарный класс; курсив выключен.
- [ ] Права и сообщения вынесены в `plugin.yml`/`messages.yml`.
- [ ] Ресурсы (задачи, пулы, меню, рецепты) освобождаются в `onDisable`.
- [ ] Нет выдуманных методов; сомнительные места помечены.
