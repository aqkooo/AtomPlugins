package glowseller;

import glowseller.Main;
import glowseller.configs.impl.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigInitializationTest {

    @Test
    @DisplayName("Verify all configs instantiate and parse without NPE")
    void testConfigsInitialization(@TempDir File tempFolder) {
        Main mockPlugin = Mockito.mock(Main.class);
        Mockito.when(mockPlugin.getDataFolder()).thenReturn(tempFolder);

        // Mock embedded resources
        String configYaml = """
            locale: "ru"
            economy:
              type: "VAULT"
            boosters:
              stacking: "EXTEND"
              permission_boosters:
                vip:
                  permission: "glowseller.booster.vip"
                  multiplier: 1.2
            """;
        Mockito.when(mockPlugin.getResource("config.yml"))
                .thenAnswer(inv -> new ByteArrayInputStream(configYaml.getBytes(StandardCharsets.UTF_8)));

        String messagesYaml = """
            prefix: "&b[GlowSeller]&f "
            commands:
              reloaded: "Reloaded!"
            """;
        Mockito.when(mockPlugin.getResource("messages.yml"))
                .thenAnswer(inv -> new ByteArrayInputStream(messagesYaml.getBytes(StandardCharsets.UTF_8)));

        String itemsYaml = """
            items:
              DIAMOND:
                material: DIAMOND
                price: 50.0
                points: 2
            """;
        Mockito.when(mockPlugin.getResource("items.yml"))
                .thenAnswer(inv -> new ByteArrayInputStream(itemsYaml.getBytes(StandardCharsets.UTF_8)));

        String shopYaml = """
            categories:
              boosters:
                name: "Бустеры"
                slot: 11
                material: EXPERIENCE_BOTTLE
                items:
                  coin_x2_1h:
                    material: EXPERIENCE_BOTTLE
                    name: "x2 1h"
                    slot: 11
                    price: 500
                    type: BOOSTER
                    multiplier: 2.0
                    duration: 3600
            decorate:
              material: GRAY_STAINED_GLASS_PANE
              slots: ["0-9"]
            """;
        Mockito.when(mockPlugin.getResource("shop.yml"))
                .thenAnswer(inv -> new ByteArrayInputStream(shopYaml.getBytes(StandardCharsets.UTF_8)));

        String databaseYaml = """
            type: "SQLITE"
            sqlite:
              file: "database.db"
            """;
        Mockito.when(mockPlugin.getResource("database.yml"))
                .thenAnswer(inv -> new ByteArrayInputStream(databaseYaml.getBytes(StandardCharsets.UTF_8)));

        // Test instantiations - must NOT throw NPE!
        assertDoesNotThrow(() -> new DataBaseConfig(mockPlugin));
        assertDoesNotThrow(() -> new MainConfig(mockPlugin));
        assertDoesNotThrow(() -> new MessageConfig(mockPlugin));
        assertDoesNotThrow(() -> new ItemsConfig(mockPlugin));
        assertDoesNotThrow(() -> new ShopConfig(mockPlugin));
    }
}
