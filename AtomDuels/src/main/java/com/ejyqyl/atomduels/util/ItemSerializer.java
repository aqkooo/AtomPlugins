package com.ejyqyl.atomduels.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for serializing ItemStack arrays and lists to Base64 strings.
 * Authored by ejyqyl.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    public static String toBase64(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return toBase64(items.toArray(new ItemStack[0]));
    }

    public static String toBase64(ItemStack[] items) {
        if (items == null || items.length == 0) {
            return "";
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    public static List<ItemStack> fromBase64(String data) {
        List<ItemStack> list = new ArrayList<>();
        if (data == null || data.trim().isEmpty()) {
            return list;
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int length = dataInput.readInt();
            for (int i = 0; i < length; i++) {
                list.add((ItemStack) dataInput.readObject());
            }
        } catch (IOException | ClassNotFoundException ignored) {}
        return list;
    }
}
