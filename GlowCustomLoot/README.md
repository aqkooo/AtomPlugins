# 💎 GlowCustomLoot

**GlowCustomLoot** — визуальный внутриигровой GUI-редактор и генератор кастомных лут-таблиц для серверов Minecraft нового поколения (**Paper / Purpur / Folia 1.20–1.21+**).

Позволяет прямо из игры через интуитивный интерфейс настраивать сундуки данжей, структур и мобов во всех трёх измерениях, а также интегрировать предметы из сторонних плагинов.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-blue)
![Folia Ready](https://img.shields.io/badge/Folia-Supported-brightgreen)
![Integrations](https://img.shields.io/badge/Items-ItemsAdder%20%7C%20Oraxen%20%7C%20MythicMobs-purple)
![Author](https://img.shields.io/badge/Author-ejyqyl%20(%40aqkooo)-green)

---

## ⚡ Особенности

- **Внутриигровой GUI-редактор (`/editloot`)**: Настройка шанса выпадения, минимального и максимального количества предметов простым кликом.
- **Полная поддержка Folia**: Асинхронные региональные шедулеры и потокобезопасная генерация лута без крашей.
- **Поддержка всех структур и измерений**:
  - 🌍 **Overworld**: Trial Chambers, Ancient City, Buried Treasure, Woodland Mansion, Stronghold, Mineshaft и др.
  - 🔥 **Nether**: Bastion Remnant, Nether Fortress, Ruined Portal.
  - 🌌 **The End**: End City Treasure.
- **Поддержка кастомных предметов**:
  - Vanilla Minecraft (с учётом NBT, чар, названий и лора).
  - ItemsAdder.
  - Oraxen.
  - MythicMobs / MMOItems.

---

## 📋 Команды и права

| Команда | Описание | Алиасы | Право |
| :--- | :--- | :--- | :--- |
| `/editloot` | Открыть главное меню выбора измерения и структуры для редактирования | — | `glowcustomloot.admin` |
| `/gcl reload` | Перезагрузить все таблицы лута и конфиги | `/glowcustomloot reload` | `glowcustomloot.admin` |
| `/gcl add <шанс>` | Быстро добавить предмет из руки в текущую таблицу | — | `glowcustomloot.admin` |

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/aqkooo/GlowCustomLoot.git
cd GlowCustomLoot
mvn clean package
```

Скомпилированный `.jar` файл будет доступен в папке `target/GlowCustomLoot-1.0.0.jar`.
