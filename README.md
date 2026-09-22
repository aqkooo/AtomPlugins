# ⚡ AtomPlugins

Collection of high-performance, production-ready Minecraft (Paper / Purpur / Spigot / Folia) plugins developed by **[ejyqyl](https://github.com/aqkooo)**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-blue)
![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)
![Author](https://img.shields.io/badge/Author-ejyqyl%20(%40aqkooo)-green)

---

## 📦 Список плагинов (Plugins Overview)

### 🔹 Серия Atom (Atom Series)

| Плагин | Версия ядра | Описание |
| :--- | :--- | :--- |
| **[AtomMessage](./AtomMessage)** | `Paper/Spigot 1.16+` | Модуль автоматических объявлений, рассылок и форматированных уведомлений в чате с MiniMessage/HEX. |
| **[AtomReactor](./AtomReactor)** | `Paper/Spigot 1.16+` | Интерактивный чат-реактор: викторины, быстрая реакция на слова и математические примеры с наградами. |
| **[AtomRegen](./AtomRegen)** | `Paper/Spigot 1.16+` | Система автоматической регенерации сломанных блоков (для шахт, автошахт и защиты спавна). |
| **[AtomTituls](./AtomTituls)** | `Paper 1.16 - 1.21+` | GUI-система титулов и префиксов с поддержкой PlaceholderAPI, кастомными цветами и правами. |

### 🔸 Серия Glow (Glow Series)

| Плагин | Версия ядра | Описание |
| :--- | :--- | :--- |
| **[GlowCMD](./GlowCMD)** | `Paper/Spigot 1.16+` | Менеджер кастомных команд, алиасов, кулдаунов и прав с гибкой конфигурацией. |
| **[GlowChatGame](./GlowChatGame)** | `Paper/Spigot 1.16+` | Набор мини-игр в чате: математика, анаграммы, разгадывание слов с системой наград. |
| **[GlowCustomLoot](./GlowCustomLoot)** | `Folia / Paper 1.16+` | Внутриигровой визуальный GUI-редактор лут-таблиц с поддержкой ItemsAdder, Oraxen, MythicMobs. |
| **[GlowSnakeGame](./GlowSnakeGame)** | `Paper/Spigot 1.16+` | Классическая игра «Змейка», работающая прямо внутри сундучного инвентаря (Chest GUI) с управлением WASD. |
| **[GlowTrade](./GlowTrade)** | `Paper/Spigot 1.16+` | Безопасный dupe-proof обмен предметами между игроками в стиле ReallyWorld с подтверждением сделки. |

---

## 🛠️ Сборка проектов (Building from Source)

Все проекты используют систему сборки **Apache Maven** и целевую версию **Java 21**.

Для сборки отдельного плагина перейдите в его директорию и выполните:

```bash
cd GlowTrade
mvn clean package
```

Скомпилированный `.jar` файл плагина будет находиться в директории `target/`.

---

## 👤 Автор (Author)

- **GitHub:** [@aqkooo](https://github.com/aqkooo)
- **Telegram:** `atomicsqd`, `glowdevv`, @ejyqyl
