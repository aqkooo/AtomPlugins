package com.ejyqyl.glowcmd;

import com.ejyqyl.glowcmd.config.MessageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MessageManagerTest {

    @TempDir
    Path tempDir;

    private GlowCMD plugin;
    private File messagesFile;

    @BeforeEach
    void setup() {
        plugin = Mockito.mock(GlowCMD.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("GlowCMDTest"));

        messagesFile = new File(tempDir.toFile(), "messages.yml");
    }

    private String getSampleEmbeddedMessages() {
        return """
                prefix: "&8[&bGlowCMD&8]"
                no-permission: "{prefix} &cNo permission."
                spawn:
                  set: "{prefix} &aSpawn set."
                  not-set: "{prefix} &cSpawn not set."
                """;
    }

    @Test
    void testInitialCreationAndLoading() {
        when(plugin.getResource("messages.yml")).thenAnswer(inv ->
                new ByteArrayInputStream(getSampleEmbeddedMessages().getBytes(StandardCharsets.UTF_8)));

        doAnswer(inv -> {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(new ByteArrayInputStream(getSampleEmbeddedMessages().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            def.save(messagesFile);
            return null;
        }).when(plugin).saveResource("messages.yml", false);

        MessageManager messageManager = new MessageManager(plugin);

        assertEquals("&8[&bGlowCMD&8]", messageManager.getPrefix());
        assertEquals("{prefix} &aSpawn set.", messageManager.getRawMessage("spawn.set"));
        assertEquals("{prefix} &cSpawn not set.", messageManager.getRawMessage("spawn.not-set"));
    }

    @Test
    void testEmptyMessageSuppression() throws IOException {
        when(plugin.getResource("messages.yml")).thenAnswer(inv ->
                new ByteArrayInputStream(getSampleEmbeddedMessages().getBytes(StandardCharsets.UTF_8)));

        // User intentionally disables a message with ""
        YamlConfiguration userConfig = new YamlConfiguration();
        userConfig.set("prefix", "&8[&bGlowCMD&8]");
        userConfig.set("spawn.set", "");
        userConfig.save(messagesFile);

        MessageManager messageManager = new MessageManager(plugin);

        CommandSender sender = Mockito.mock(CommandSender.class);
        messageManager.send(sender, "spawn.set");

        // Verify sendMessage is NEVER called for empty string messages
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    void testNonDestructiveAutoRecoveryOfMissingKeys() throws IOException {
        when(plugin.getResource("messages.yml")).thenAnswer(inv ->
                new ByteArrayInputStream(getSampleEmbeddedMessages().getBytes(StandardCharsets.UTF_8)));

        // User customized prefix and spawn.set, but deleted spawn.not-set
        YamlConfiguration userConfig = new YamlConfiguration();
        userConfig.set("prefix", "&6[CustomPrefix]");
        userConfig.set("spawn.set", "&eMy custom spawn set message!");
        userConfig.save(messagesFile);

        MessageManager messageManager = new MessageManager(plugin);

        // Verify custom message was preserved
        assertEquals("&eMy custom spawn set message!", messageManager.getRawMessage("spawn.set"));
        assertEquals("&6[CustomPrefix]", messageManager.getPrefix());

        // Verify missing key was safely restored
        assertEquals("{prefix} &cSpawn not set.", messageManager.getRawMessage("spawn.not-set"));

        // Verify file on disk retained user's custom settings
        YamlConfiguration savedDisk = YamlConfiguration.loadConfiguration(messagesFile);
        assertEquals("&6[CustomPrefix]", savedDisk.getString("prefix"));
        assertEquals("&eMy custom spawn set message!", savedDisk.getString("spawn.set"));
        assertEquals("{prefix} &cSpawn not set.", savedDisk.getString("spawn.not-set"));
    }
}
