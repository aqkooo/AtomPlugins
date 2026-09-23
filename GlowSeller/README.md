# 🌟 GlowSeller

**GlowSeller** — современный и высокопроизводительный плагин скупщика предметов нового поколения для Paper/Spigot 1.16.5 – 1.21.x (`api-version: 1.16`).

В **GlowSeller** внедрена гибкая механика **магазина за очки** с временными бустерами монет, предметами и командами.

---

## 🎯 Основная концепция

- **Разделение валют:** При продаже предметов игрок одновременно получает **монеты** (серверная экономика Vault / CoinsEngine / PlayerPoints) и **очки** (`points` — внутренняя валюта плагина).
- **Магазин за очки (`/shop`):**
  - **Временные бустеры монет:** увеличивают доход от продажи ресурсов в скупщике на заданный множитель (`+15%`, `+25%`, `x2`, `x3`) на определенное время (например, 30 минут, 1 час).
  - **Кастомные товары и команды:** выдача предметов, ключей от кейсов, команд и привилегий.
  - **Система лимитов:** покупка один раз (`limit: once`), раз в сутки (`limit: daily`) или без ограничений (`limit: unlimited`).
- **Скупщик предметов (`/sell`):** Интерактивный GUI: перенос предметов, кнопка быстрой продажи всего инвентаря в один клик, отображение расчетной прибыли, текущих очков, множителя и времени активного бустера.

---

## ⚡ Система бустеров и математика

1. **Формула расчета множителя:**
   $$\text{Итоговый множитель} = \text{Перманентный множитель} + \text{Купленный множитель} - 1.0$$
   *Пример:* если у игрока есть донат-привилегия с бустером `1.5x` и он купил временный бустер `2.0x`:
   $$1.5 + 2.0 - 1.0 = 2.5x$$
   *(исключает нечестное двойное перемножение!)*

2. **Режимы стэкинга (`boosters.stacking` в `config.yml`):**
   - `EXTEND` — продлевает время действия текущего бустера;
   - `REPLACE` — заменяет текущий бустер новым;
   - `DENY` — запрещает покупку нового бустера, пока не истек старый.

3. **Фоновая задача (`BoosterExpireTask`):**
   - Каждую секунду проверяет онлайн-игроков.
   - По истечении бустера отправляет уведомление в чат/Actionbar и воспроизводит настраиваемый звук.

---

## 📂 Структура проекта

```
glowseller/
├── Main.java                          // Главный класс инициализации и завершения
├── configs/
│   ├── CustomConfig.java              // Базовый загрузчик YAML с поддержкой локалей
│   └── impl/
│       ├── MainConfig.java            // config.yml
│       ├── ItemsConfig.java           // items.yml (предметы скупщика)
│       ├── ShopConfig.java            // shop.yml (магазин товаров и бустеров)
│       ├── MessageConfig.java         // messages.yml (все сообщения и заголовки)
│       └── DataBaseConfig.java        // database.yml (HikariCP, SQLite/MySQL/PostgreSQL)
├── database/
│   ├── DataBaseManager.java           // Пул подключений HikariCP
│   └── repositories/
│       ├── PlayerRepository.java      // CRUD player_data
│       └── PurchaseLogRepository.java // Лог покупок purchase_log
├── cache/
│   └── PlayerDataCache.java           // ConcurrentHashMap кэш игроков
├── economy/
│   ├── EconomyProvider.java           // Интерфейс экономики
│   ├── VaultEconomyProvider.java      // Vault
│   ├── CoinsEngineEconomyProvider.java// CoinsEngine
│   └── PlayerPointsEconomyProvider.java// PlayerPoints
├── managers/
│   ├── SellManager.java               // Логика продажи и начисления валют
│   ├── ItemManager.java               // Сопоставление предметов (материал, model data)
│   ├── ShopManager.java               // Покупка товаров за очки
│   ├── BoosterManager.java            // Множители и форматирование времени
│   └── NumberFormatManager.java       // Форматирование чисел (1,000, 1.5M, 2.5B)
├── menu/
│   ├── AbstractMenu.java              // Защищенный InventoryHolder
│   ├── MainMenu.java                  // Главное меню навигации
│   ├── SellMenu.java                  // Меню скупщика с кнопкой "Продать всё"
│   ├── ShopMenu.java                  // Главное меню магазина (категории)
│   └── ShopCategoryMenu.java          // Меню товаров категории
├── models/
│   ├── PlayerData.java                // Модель данных игрока
│   ├── ShopItem.java                  // Модель товара магазина
│   ├── ActiveBooster.java             // Модель активного бустера
│   └── item/Item.java                 // Модель предмета скупщика
├── events/
│   ├── SellEvent.java                 // Cancellable событие продажи
│   ├── ShopPurchaseEvent.java         // Cancellable событие покупки
│   └── PointsUpdateEvent.java         // Событие обновления очков
├── listeners/
│   ├── PlayerListener.java            // Async load/save при входе/выходе
│   └── MenuListener.java              // Защита от дюпов и обработка кликов
├── commands/
│   ├── SellCommand.java               // /sell, /buyer, /seller
│   ├── ShopCommand.java               // /shop, /gshop
│   └── GlowSellerAdminCommand.java    // /glowseller admin/reload
├── tasks/
│   ├── SavePlayerDataCacheTask.java   // Async периодическое автосохранение
│   └── BoosterExpireTask.java         // Проверка истечения временных бустеров
└── utils/
    ├── Colorizer.java                 // Поддержка hex (&#RRGGBB) и legacy (&)
    ├── HeadUtil.java                  // Текстуры голов (basehead-<base64>)
    ├── ModelDataUtil.java             // CustomModelData
    ├── SyntaxParser.java              // Парсер экшенов GUI ([opengui], [buy], [command])
    └── PlaceholderHook.java           // PlaceholderAPI экспансия
```

---

## 📜 Команды и права

| Команда | Алиасы | Описание | Право |
|---|---|---|---|
| `/sell` | `/buyer`, `/seller` | Открыть меню скупщика предметов | `glowseller.use` (default: true) |
| `/shop` | `/gshop` | Открыть магазин за очки | `glowseller.shop` (default: true) |
| `/glowseller reload` | | Перезагрузить конфиги и локализацию | `glowseller.admin` (default: op) |
| `/glowseller admin give <игрок> <очки>` | | Выдать очки игроку | `glowseller.admin` (default: op) |
| `/glowseller admin take <игрок> <очки>` | | Списать очки у игрока | `glowseller.admin` (default: op) |
| `/glowseller admin booster give <игрок> <ключ> [сек]` | | Выдать временный бустер | `glowseller.admin` (default: op) |

---

## 🧩 PlaceholderAPI

- `%glowseller_points%` — баланс очков с форматированием (например, `1,250`)
- `%glowseller_points_raw%` — баланс очков числом (например, `1250`)
- `%glowseller_booster_active%` — активен ли временный бустер (`true` / `false`)
- `%glowseller_booster_multiplier%` — множитель купленного бустера (например, `2.0`)
- `%glowseller_booster_time_left%` — остаток времени бустера в секундах (например, `1800`)
- `%glowseller_booster_time_left_formatted%` — форматированное время (например, `30м` или `1ч 15м`)
- `%glowseller_total_multiplier%` — полный множитель продажи с учетом привилегий и бустера

---

## 🛡️ Безопасность и оптимизация

- **Полная защита от дюпов:** в GUI меню заблокированы все типы несанкционированных действий (`SHIFT-click` из нижнего инвентаря в меню, `DRAG`, смена через цифры 1-9, сборка на курсор, выброс на `Q`).
- **Синхронизация экономики:** операции со списанием и начислением атомарны; в случае сбоя депозита экономика отменяет операцию и предметы не пропадают.
- **HikariCP и Асинхронность:** все чтения и записи в базу данных производятся в отдельных асинхронных потоках; при выключении сервера (`onDisable`) данные сбрасываются синхронно перед закрытием пула.
- **Кэш игроков:** данные хранятся в памяти в `PlayerDataCache` с флагом `dirty`, что сводит нагрузку на диск к минимуму.
