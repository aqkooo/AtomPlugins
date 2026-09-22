package ru.glowdevv.glowcustomloot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.glowdevv.glowcustomloot.GlowCustomLootPlugin;
import ru.glowdevv.glowcustomloot.model.DimensionType;
import ru.glowdevv.glowcustomloot.model.LootItem;
import ru.glowdevv.glowcustomloot.model.StructureLootTable;
import ru.glowdevv.glowcustomloot.util.TextUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GclCommand implements CommandExecutor, TabCompleter {
    private final GlowCustomLootPlugin plugin;

    public GclCommand(@NotNull GlowCustomLootPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcustomloot.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.getConfigManager().reload();
                plugin.getLootTableManager().loadAll();
                int count = plugin.getLootTableManager().getTablesForDimension(DimensionType.OVERWORLD).size()
                        + plugin.getLootTableManager().getTablesForDimension(DimensionType.NETHER).size()
                        + plugin.getLootTableManager().getTablesForDimension(DimensionType.THE_END).size();
                sender.sendMessage(plugin.getConfigManager().getMessage("reload-success", "{count}", String.valueOf(count)));
            } catch (Exception ex) {
                sender.sendMessage(plugin.getConfigManager().getMessage("reload-fail"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                return true;
            }

            if (args.length < 4) {
                player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                        + "<yellow>Использование: /gcl add <измерение> <данж> <шанс></yellow>"));
                return true;
            }

            DimensionType dim = DimensionType.fromString(args[1]);
            if (dim == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("dimension-not-found", "{dimension}", args[1]));
                return true;
            }

            StructureLootTable table = plugin.getLootTableManager().getTableById(args[2]);
            if (table == null) {
                for (StructureLootTable t : plugin.getLootTableManager().getTablesForDimension(dim)) {
                    if (t.getId().equalsIgnoreCase(args[2]) || t.getName().equalsIgnoreCase(args[2])) {
                        table = t;
                        break;
                    }
                }
            }

            if (table == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("structure-not-found",
                        "{structure}", args[2],
                        "{dimension}", dim.getId()));
                return true;
            }

            double chance;
            try {
                chance = Double.parseDouble(args[3]);
                if (chance < 0.1 || chance > 100.0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-chance"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-chance"));
                return true;
            }

            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getType() == org.bukkit.Material.AIR || handItem.getAmount() <= 0) {
                player.sendMessage(plugin.getConfigManager().getMessage("item-add-air"));
                return true;
            }

            String provider = plugin.getItemProviderRegistry().detectProvider(handItem);
            String newId = table.nextAvailableId();
            int maxAmount = Math.max(1, handItem.getAmount());

            LootItem lootItem = new LootItem(newId, provider, chance, 1, maxAmount, handItem);
            table.addItem(lootItem);
            plugin.getLootTableManager().saveTableAsync(table);

            player.sendMessage(plugin.getConfigManager().getMessage("item-added",
                    "{structure}", table.getName(),
                    "{chance}", String.valueOf(chance)));
            return true;
        }

        if (args[0].equalsIgnoreCase("test") || args[0].equalsIgnoreCase("roll")) {
            if (args.length < 2) {
                sender.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                        + "<yellow>Использование: /gcl test <данж></yellow>"));
                return true;
            }

            StructureLootTable table = plugin.getLootTableManager().getTableById(args[1]);
            if (table == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("structure-not-found-simple", "{structure}", args[1]));
                return true;
            }

            int cap = plugin.getConfigManager().getMaxItemsPerChest();
            List<ItemStack> rolled = table.generateLoot(java.util.concurrent.ThreadLocalRandom.current(), cap);

            sender.sendMessage(plugin.getConfigManager().getMessage("test-header",
                    "{structure}", table.getName(),
                    "{mode}", table.getMode().name(),
                    "{min_rolls}", String.valueOf(table.getMinRolls()),
                    "{max_rolls}", String.valueOf(table.getMaxRolls())));

            if (rolled.isEmpty()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("test-empty"));
            } else {
                for (ItemStack item : rolled) {
                    String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                            ? item.getItemMeta().getDisplayName()
                            : item.getType().name();
                    sender.sendMessage(plugin.getConfigManager().getMessage("test-item",
                            "{item}", itemName,
                            "{amount}", String.valueOf(item.getAmount())));
                }
            }
            sender.sendMessage(plugin.getConfigManager().getMessage("test-footer",
                    "{count}", String.valueOf(rolled.size())));
            return true;
        }

        if (args[0].equalsIgnoreCase("fill")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                        + "<yellow>Использование: /gcl fill <данж></yellow>"));
                return true;
            }

            StructureLootTable table = plugin.getLootTableManager().getTableById(args[1]);
            if (table == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("structure-not-found-simple", "{structure}", args[1]));
                return true;
            }

            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !(targetBlock.getState() instanceof org.bukkit.block.Container container)) {
                player.sendMessage(plugin.getConfigManager().getMessage("fill-no-target"));
                return true;
            }

            org.bukkit.inventory.Inventory inv = container.getInventory();
            List<ItemStack> generated = table.generateLoot(java.util.concurrent.ThreadLocalRandom.current(), inv.getSize());

            if (table.getMode() == ru.glowdevv.glowcustomloot.model.LootMode.REPLACE) {
                inv.clear();
            }

            List<Integer> availableSlots = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack cur = inv.getItem(i);
                if (cur == null || cur.getType() == org.bukkit.Material.AIR || cur.getAmount() <= 0) {
                    availableSlots.add(i);
                }
            }
            Collections.shuffle(availableSlots, java.util.concurrent.ThreadLocalRandom.current());

            int count = 0;
            for (ItemStack item : generated) {
                if (availableSlots.isEmpty()) {
                    inv.addItem(item);
                } else {
                    int slot = availableSlots.remove(0);
                    inv.setItem(slot, item);
                }
                count++;
            }

            player.sendMessage(plugin.getConfigManager().getMessage("fill-success",
                    "{structure}", table.getName(),
                    "{count}", String.valueOf(count),
                    "{mode}", table.getMode().name()));
            return true;
        }

        if (args[0].equalsIgnoreCase("spawnchest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(TextUtil.parse(plugin.getConfigManager().getPrefix()
                        + "<yellow>Использование: /gcl spawnchest <данж></yellow>"));
                return true;
            }

            StructureLootTable table = plugin.getLootTableManager().getTableById(args[1]);
            if (table == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("structure-not-found-simple", "{structure}", args[1]));
                return true;
            }

            org.bukkit.block.Block block = player.getTargetBlockExact(5);
            if (block == null) {
                block = player.getLocation().getBlock();
            } else if (block.getType() != org.bukkit.Material.AIR && !block.isLiquid()) {
                block = block.getRelative(org.bukkit.block.BlockFace.UP);
            }

            block.setType(org.bukkit.Material.CHEST);
            if (block.getState() instanceof org.bukkit.block.Chest chest) {
                org.bukkit.NamespacedKey key = null;
                for (String k : table.getLootTableKeys()) {
                    if (!k.contains("*")) {
                        org.bukkit.NamespacedKey nsk = org.bukkit.NamespacedKey.fromString(k);
                        if (nsk != null && org.bukkit.Bukkit.getLootTable(nsk) != null) {
                            key = nsk;
                            break;
                        }
                    }
                }
                if (key == null) {
                    key = org.bukkit.NamespacedKey.fromString(table.getLootTableKey());
                }

                org.bukkit.loot.LootTable lt = key != null ? org.bukkit.Bukkit.getLootTable(key) : null;
                if (lt != null) {
                    chest.setLootTable(lt);
                    chest.update(true, false);
                    player.sendMessage(plugin.getConfigManager().getMessage("spawnchest-success",
                            "{table_key}", key.toString(),
                            "{structure}", table.getName()));
                    return true;
                }
            }

            player.sendMessage(plugin.getConfigManager().getMessage("spawnchest-fail"));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(@NotNull CommandSender sender) {
        List<String> lines = plugin.getConfigManager().getMessages().getStringList("help");
        for (String line : lines) {
            sender.sendMessage(TextUtil.parse(line));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowcustomloot.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(Arrays.asList("reload", "test", "fill", "spawnchest", "add", "help"), args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("test") || args[0].equalsIgnoreCase("roll") || args[0].equalsIgnoreCase("fill") || args[0].equalsIgnoreCase("spawnchest"))) {
            List<String> list = new ArrayList<>();
            for (DimensionType dim : DimensionType.values()) {
                for (StructureLootTable table : plugin.getLootTableManager().getTablesForDimension(dim)) {
                    list.add(table.getId());
                }
            }
            return filter(list, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return filter(Arrays.asList("overworld", "nether", "the_end"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            DimensionType dim = DimensionType.fromString(args[1]);
            if (dim != null) {
                List<String> list = new ArrayList<>();
                for (StructureLootTable table : plugin.getLootTableManager().getTablesForDimension(dim)) {
                    list.add(table.getId());
                }
                return filter(list, args[2]);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            return filter(Arrays.asList("10", "25", "50", "75", "100"), args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String current) {
        List<String> result = new ArrayList<>();
        String lower = current.toLowerCase();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }
}
