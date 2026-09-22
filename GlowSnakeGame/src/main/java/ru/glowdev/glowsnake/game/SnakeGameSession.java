package ru.glowdev.glowsnake.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.glowdev.glowsnake.GlowSnakePlugin;
import ru.glowdev.glowsnake.config.ConfigManager;
import ru.glowdev.glowsnake.util.ChatUtil;
import ru.glowdev.glowsnake.util.ItemBuilder;
import ru.glowdev.glowsnake.util.SoundUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SnakeGameSession {

    public static final int INVENTORY_SIZE = 54;
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;

    // Joystick Slots in the bottom-right corner (matching user's screenshot)
    public static final int JOYSTICK_UP = 34;    // Row 3, Col 7
    public static final int JOYSTICK_LEFT = 42;  // Row 4, Col 6
    public static final int JOYSTICK_CENTER = 43;// Row 4, Col 7 (empty)
    public static final int JOYSTICK_RIGHT = 44; // Row 4, Col 8
    public static final int JOYSTICK_DOWN = 52;  // Row 5, Col 7

    // Start slot: Row 2, Col 2 (center of playing area)
    public static final int START_SLOT = 20;

    // Game Over buttons on field
    public static final int GAMEOVER_RESTART = 12;
    public static final int GAMEOVER_SCORE = 13;
    public static final int GAMEOVER_EXIT = 14;

    private final GlowSnakePlugin plugin;
    private final Player player;
    private final Location originLocation;

    private Inventory inventory;
    private BukkitTask gameTask;

    private final Deque<Integer> body = new ArrayDeque<>();
    private Direction currentDirection;
    private Direction nextDirection;

    private int appleSlot = -1;
    private int score = 0;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean isClosed = false;

    public SnakeGameSession(GlowSnakePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.originLocation = player.getLocation().clone();
    }

    public void start() {
        ConfigManager cfg = plugin.getConfigManager();

        this.score = 0;
        this.isGameOver = false;
        this.isVictory = false;
        this.isClosed = false;

        this.currentDirection = Direction.fromString(cfg.getStartDirection());
        this.nextDirection = this.currentDirection;

        // Build Snake body
        body.clear();
        int headSlot = START_SLOT;
        body.add(headSlot);

        // Build initial tail segments opposite to start direction
        int initialLength = cfg.getInitialLength();
        int stepBack = -currentDirection.getSlotOffset();
        int currentSegment = headSlot;
        for (int i = 1; i < initialLength; i++) {
            currentSegment += stepBack;
            if (isPlayable(currentSegment)) {
                body.addLast(currentSegment);
            }
        }

        // Initialize GUI
        String title = cfg.getGuiString("title", "&2Змейка &8| &7Счет: &e{SCORE}")
                .replace("{SCORE}", String.valueOf(score));
        this.inventory = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Spawn first apple
        spawnApple();

        // Initial render
        renderFullField();

        // Open inventory for player
        player.openInventory(inventory);

        // Send game started message
        String startMsg = cfg.getMessage("game-started", "&aЗмейка запущена! &7Управление: &fджойстик&7, &fW, A, S, D &7или &f1-4");
        player.sendMessage(ChatUtil.color(startMsg));

        // Start game loop (18 ticks = 10% faster)
        long tickDelay = cfg.getTickDelay();
        this.gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, tickDelay, tickDelay);
    }

    public synchronized void tick() {
        if (isGameOver || isVictory || isClosed) {
            return;
        }

        // Apply buffered direction
        this.currentDirection = this.nextDirection;

        int currentHead = body.peekFirst();

        // Check if move is valid (within boundaries and not entering joystick area)
        if (!isValidMove(currentHead, currentDirection)) {
            handleGameOver("wall");
            return;
        }

        int nextHead = currentHead + currentDirection.getSlotOffset();

        // Check for self collision
        boolean eating = (nextHead == appleSlot);
        if (eating) {
            if (body.contains(nextHead)) {
                handleGameOver("self");
                return;
            }
        } else {
            int tail = body.peekLast();
            if (nextHead != tail && body.contains(nextHead)) {
                handleGameOver("self");
                return;
            }
        }

        // Move Snake
        body.addFirst(nextHead);

        if (eating) {
            score++;
            ConfigManager cfg = plugin.getConfigManager();
            SoundUtil.playSound(player, cfg.getSoundEat(), cfg.getSoundVolume(), cfg.getSoundPitchEat());

            boolean isRecord = plugin.getScoreManager().updateScore(player.getUniqueId(), player.getName(), score);
            if (isRecord) {
                String recordMsg = cfg.getMessage("new-record", "&aНовый рекорд! &7Вы набрали: &e{SCORE}")
                        .replace("{SCORE}", String.valueOf(score));
                player.sendMessage(ChatUtil.color(recordMsg));
            }

            if (!spawnApple()) {
                handleVictory();
                return;
            }
        } else {
            int oldTail = body.removeLast();
            setSlotItem(oldTail, null); // Clear old tail
        }

        // Visual updates: old head becomes body, new head placed
        setSlotItem(currentHead, createBodyItem());
        setSlotItem(nextHead, createHeadItem());
    }

    public synchronized void changeDirection(Direction requested) {
        if (requested == null || isGameOver || isVictory || isClosed) {
            return;
        }
        // Prevent 180-degree reversal
        if (!requested.isOpposite(currentDirection)) {
            this.nextDirection = requested;
        }
    }

    public void handleNumberKeyClick(int hotbarButton) {
        if (!plugin.getConfigManager().isEnableNumberKeys() || isGameOver || isVictory || isClosed) {
            return;
        }

        Direction requested = null;
        switch (hotbarButton) {
            case 0: // Key 1 -> Left (A)
                requested = Direction.LEFT;
                break;
            case 1: // Key 2 -> Down (S)
                requested = Direction.DOWN;
                break;
            case 2: // Key 3 -> Up (W)
                requested = Direction.UP;
                break;
            case 3: // Key 4 -> Right (D)
                requested = Direction.RIGHT;
                break;
            case 7: // Key 8 -> Up
                requested = Direction.UP;
                break;
            case 5: // Key 6 -> Right
                requested = Direction.RIGHT;
                break;
            default:
                break;
        }

        if (requested != null) {
            changeDirection(requested);
            playClickSound();
        }
    }

    public void handleDirectionClick(int clickedSlot) {
        if (!plugin.getConfigManager().isEnableGuiClicks() || isGameOver || isVictory || isClosed) {
            return;
        }

        // Joystick buttons in bottom-right corner (from user's screenshot)
        if (clickedSlot == JOYSTICK_UP) {
            changeDirection(Direction.UP);
            playClickSound();
            return;
        }
        if (clickedSlot == JOYSTICK_DOWN) {
            changeDirection(Direction.DOWN);
            playClickSound();
            return;
        }
        if (clickedSlot == JOYSTICK_LEFT) {
            changeDirection(Direction.LEFT);
            playClickSound();
            return;
        }
        if (clickedSlot == JOYSTICK_RIGHT) {
            changeDirection(Direction.RIGHT);
            playClickSound();
            return;
        }

        // Clicks on playing field relative to snake head
        if (isPlayable(clickedSlot)) {
            int head = body.peekFirst();
            int headRow = head / COLUMNS;
            int headCol = head % COLUMNS;

            int clickRow = clickedSlot / COLUMNS;
            int clickCol = clickedSlot % COLUMNS;

            int rowDiff = clickRow - headRow;
            int colDiff = clickCol - headCol;

            Direction requested = null;
            if (Math.abs(rowDiff) > Math.abs(colDiff)) {
                if (rowDiff < 0) requested = Direction.UP;
                else if (rowDiff > 0) requested = Direction.DOWN;
            } else if (Math.abs(colDiff) > Math.abs(rowDiff)) {
                if (colDiff < 0) requested = Direction.LEFT;
                else if (colDiff > 0) requested = Direction.RIGHT;
            } else {
                // Diagonals
                if (rowDiff < 0 && currentDirection != Direction.DOWN && currentDirection != Direction.UP) {
                    requested = Direction.UP;
                } else if (rowDiff > 0 && currentDirection != Direction.UP && currentDirection != Direction.DOWN) {
                    requested = Direction.DOWN;
                } else if (colDiff < 0 && currentDirection != Direction.RIGHT && currentDirection != Direction.LEFT) {
                    requested = Direction.LEFT;
                } else if (colDiff > 0 && currentDirection != Direction.LEFT && currentDirection != Direction.RIGHT) {
                    requested = Direction.RIGHT;
                }
            }

            if (requested != null) {
                changeDirection(requested);
                playClickSound();
            }
            return;
        }

        // Bottom inventory clicks (player's inventory, rawSlot 54..89)
        if (clickedSlot >= INVENTORY_SIZE) {
            int bottomSlot = clickedSlot - INVENTORY_SIZE;
            int bottomRow = bottomSlot / COLUMNS; // 0..3
            int bottomCol = bottomSlot % COLUMNS; // 0..8

            Direction requested = null;
            if (bottomRow == 0) {
                requested = Direction.UP;
            } else if (bottomRow == 3) {
                requested = Direction.DOWN;
            } else if (bottomCol <= 3) {
                requested = Direction.LEFT;
            } else if (bottomCol >= 5) {
                requested = Direction.RIGHT;
            }

            if (requested != null) {
                changeDirection(requested);
                playClickSound();
            }
        }
    }

    private void playClickSound() {
        ConfigManager cfg = plugin.getConfigManager();
        SoundUtil.playSound(player, cfg.getSoundClick(), 0.6f, 1.4f);
    }

    private void handleGameOver(String cause) {
        this.isGameOver = true;
        cancelTask();

        ConfigManager cfg = plugin.getConfigManager();
        SoundUtil.playSound(player, cfg.getSoundGameOver(), cfg.getSoundVolume(), cfg.getSoundPitchGameOver());
        SoundUtil.playSound(player, cfg.getSoundGameOverSecondary(), 0.7f, 0.9f);

        plugin.getScoreManager().updateScore(player.getUniqueId(), player.getName(), score);

        String gameOverMsg = cfg.getMessage("game-over", "&cИгра окончена! &7Ваш счет: &e{SCORE}")
                .replace("{SCORE}", String.valueOf(score));
        player.sendMessage(ChatUtil.color(gameOverMsg));

        renderGameOverScreen();
    }

    private void handleVictory() {
        this.isVictory = true;
        cancelTask();

        ConfigManager cfg = plugin.getConfigManager();
        SoundUtil.playSound(player, "UI_TOAST_CHALLENGE_COMPLETE", 1.0f, 1.0f);
        plugin.getScoreManager().updateScore(player.getUniqueId(), player.getName(), score);

        player.sendMessage(ChatUtil.color("&aПобеда! &fВы заполнили всё игровое поле! Счет: &e" + score));
        renderGameOverScreen();
    }

    private boolean spawnApple() {
        List<Integer> freeSlots = new ArrayList<>();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (isPlayable(slot) && !body.contains(slot)) {
                freeSlots.add(slot);
            }
        }

        if (freeSlots.isEmpty()) {
            this.appleSlot = -1;
            return false;
        }

        this.appleSlot = freeSlots.get(ThreadLocalRandom.current().nextInt(freeSlots.size()));
        setSlotItem(appleSlot, createAppleItem());
        return true;
    }

    public void renderFullField() {
        ConfigManager cfg = plugin.getConfigManager();

        // 1. Clear all slots (no red blocks, no glass clutter)
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, null);
        }

        // 2. Render Joystick in bottom-right corner (exactly matching user's screenshot)
        Material joyMat = cfg.getJoystickMaterial();
        setSlotItem(JOYSTICK_UP, ItemBuilder.from(joyMat).name("&a▲ Вверх").lore("&7Нажмите для поворота").hideAllFlags().build());
        setSlotItem(JOYSTICK_LEFT, ItemBuilder.from(joyMat).name("&a◄ Влево").lore("&7Нажмите для поворота").hideAllFlags().build());
        setSlotItem(JOYSTICK_CENTER, null); // Slot 43 is empty as in screenshot!
        setSlotItem(JOYSTICK_RIGHT, ItemBuilder.from(joyMat).name("&a► Вправо").lore("&7Нажмите для поворота").hideAllFlags().build());
        setSlotItem(JOYSTICK_DOWN, ItemBuilder.from(joyMat).name("&a▼ Вниз").lore("&7Нажмите для поворота").hideAllFlags().build());

        // 3. Render Playing Field items (Only Snake and Apple!)
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (isPlayable(slot)) {
                if (slot == body.peekFirst()) {
                    setSlotItem(slot, createHeadItem());
                } else if (body.contains(slot)) {
                    setSlotItem(slot, createBodyItem());
                } else if (slot == appleSlot) {
                    setSlotItem(slot, createAppleItem());
                } else {
                    setSlotItem(slot, null); // Clean empty slot
                }
            }
        }
    }

    public void renderGameOverScreen() {
        ConfigManager cfg = plugin.getConfigManager();
        int bestScore = plugin.getScoreManager().getHighScore(player.getUniqueId());

        // Clear playing field slots
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (isPlayable(i)) {
                inventory.setItem(i, null);
            }
        }

        // Restart button (Slot GAMEOVER_RESTART = 12)
        ItemStack restartButton = ItemBuilder.from(Material.LIME_CONCRETE)
                .name(cfg.getGuiString("restart-button", "&aИграть снова"))
                .lore(cfg.getGuiLore("restart-lore"))
                .hideAllFlags()
                .build();
        inventory.setItem(GAMEOVER_RESTART, restartButton);

        // Score display (Slot GAMEOVER_SCORE = 13)
        List<String> scoreLore = new ArrayList<>();
        for (String line : cfg.getGuiLore("score-lore")) {
            scoreLore.add(line.replace("{SCORE}", String.valueOf(score))
                    .replace("{BEST_SCORE}", String.valueOf(bestScore)));
        }
        ItemStack scoreItem = ItemBuilder.from(Material.NETHER_STAR)
                .name(cfg.getGuiString("score-item", "&eИтоговый счет"))
                .lore(scoreLore)
                .hideAllFlags()
                .build();
        inventory.setItem(GAMEOVER_SCORE, scoreItem);

        // Exit button (Slot GAMEOVER_EXIT = 14)
        ItemStack exitButton = ItemBuilder.from(Material.RED_CONCRETE)
                .name(cfg.getGuiString("exit-button", "&cВыйти"))
                .lore(cfg.getGuiLore("exit-lore"))
                .hideAllFlags()
                .build();
        inventory.setItem(GAMEOVER_EXIT, exitButton);
    }

    public void restart() {
        cancelTask();
        start();
    }

    public void cleanup() {
        this.isClosed = true;
        cancelTask();
    }

    private void cancelTask() {
        if (gameTask != null) {
            try {
                gameTask.cancel();
            } catch (Exception ignored) {
            }
            gameTask = null;
        }
    }

    /**
     * Playable area consists of all 54 slots EXCEPT the bottom-right 3x3 joystick area
     * (Rows 3..5, Columns 6..8: slots 33..35, 42..44, 51..53).
     * Total playable slots: 45.
     */
    public static boolean isPlayable(int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return false;
        }
        int row = slot / COLUMNS;
        int col = slot % COLUMNS;
        // Bottom-right 3x3 is joystick area
        if (row >= 3 && col >= 6) {
            return false;
        }
        return true;
    }

    /**
     * Verifies that moving from currentSlot in direction does not wrap across rows/columns
     * and lands on a playable slot.
     */
    public static boolean isValidMove(int currentSlot, Direction direction) {
        if (!isPlayable(currentSlot) || direction == null) {
            return false;
        }
        int nextSlot = currentSlot + direction.getSlotOffset();
        if (!isPlayable(nextSlot)) {
            return false;
        }
        int curRow = currentSlot / COLUMNS;
        int curCol = currentSlot % COLUMNS;
        int nextRow = nextSlot / COLUMNS;
        int nextCol = nextSlot % COLUMNS;

        switch (direction) {
            case UP:
                return nextRow == curRow - 1 && nextCol == curCol;
            case DOWN:
                return nextRow == curRow + 1 && nextCol == curCol;
            case LEFT:
                return nextCol == curCol - 1 && nextRow == curRow;
            case RIGHT:
                return nextCol == curCol + 1 && nextRow == curRow;
            default:
                return false;
        }
    }

    private void setSlotItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < INVENTORY_SIZE) {
            inventory.setItem(slot, item);
        }
    }

    private ItemStack createHeadItem() {
        ConfigManager cfg = plugin.getConfigManager();
        return ItemBuilder.from(cfg.getSnakeHeadMaterial())
                .name("&aГолова змейки &f" + currentDirection.getSymbol())
                .lore(
                        "&7Направление: &f" + currentDirection.getDisplayName(),
                        "&7Счет: &e" + score
                )
                .hideAllFlags()
                .build();
    }

    private ItemStack createBodyItem() {
        ConfigManager cfg = plugin.getConfigManager();
        return ItemBuilder.from(cfg.getSnakeBodyMaterial())
                .name("&2Тело змейки")
                .hideAllFlags()
                .build();
    }

    private ItemStack createAppleItem() {
        ConfigManager cfg = plugin.getConfigManager();
        return ItemBuilder.from(cfg.getAppleMaterial())
                .name("&cЯблоко &e+1")
                .lore("&7Съешьте яблоко, чтобы вырасти!")
                .hideAllFlags()
                .build();
    }

    public Player getPlayer() {
        return player;
    }

    public Location getOriginLocation() {
        return originLocation;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isVictory() {
        return isVictory;
    }

    public boolean isClosed() {
        return isClosed;
    }
}
