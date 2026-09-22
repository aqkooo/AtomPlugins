# 📢 AtomMessage

**AtomMessage** — универсальная, высокопроизводительная система интерактивных чат-рассылок и ротации сообщений в TAB для серверов Minecraft (**Paper / Purpur / Spigot 1.20–1.21+**).

Плагин поддерживает **ВСЕ цветовые форматы**, плавные градиенты, эффекты MiniMessage, кликабельные ссылки, интерактивные кнопки с командами, копирование в буфер обмена, звуковые эффекты, отправку Title, ActionBar и BossBar при анонсах.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%201.20--1.21+-blue)
![PlaceholderAPI](https://img.shields.io/badge/Support-PlaceholderAPI-purple)
![Version](https://img.shields.io/badge/Version-1.1.0-brightgreen)
![Author](https://img.shields.io/badge/Author-ejyqyl%20(%40aqkooo)-green)

---

## ⚡ Ключевые возможности

### 🎨 Поддержка ВСЕХ форматов цветов
- **Классические коды Minecraft**: `&0`-`&9`, `&a`-`&f`, а также с символом параграфа `§`.
- **Стили текста**: `&l` (жирный), `&o` (курсив), `&n` (подчёркнутый), `&m` (зачёркнутый), `&k` (магический), `&r` (сброс).
- **Spigot HEX**: `&#RRGGBB` (например, `&#FF5D00`).
- **CMI / EssentialsX HEX**: `{#RRGGBB}` (например, `{#00C6FF}`).
- **MiniMessage HEX**: `<#RRGGBB>` (например, `<#5865F2>`).
- **Section & Ampersand X**: `§x§r§r§g§g§b§b` и `&x&r&r&g&g&b&b`.
- **Adventure MiniMessage градиенты и эффекты**:
  - Двухцветные и многоцветные градиенты: `<gradient:#FF512F:#DD2476>текст</gradient>`
  - Радуга: `<rainbow>радужный текст</rainbow>`
  - Тени и переходы: `<shadow:#736855:1>текст</shadow>`
  - Теги форматирования: `<b>`, `<i>`, `<u>`, `<st>`, `<obf>`, `<reset>`

---

### 🔗 Интерактивные ссылки и кнопки (Links & Commands)

AtomMessage поддерживает как нативные теги MiniMessage, так и удобный теговый синтаксис:

1. **Кликабельные ссылки**:
   - `[url=https://mysite.fun hover="Нажмите для перехода"]Наш Сайт[/url]`
   - `<click:open_url:'https://mysite.fun'><hover:show_text:'Перейти'>Наш Сайт</hover></click>`
2. **Выполнение команд по клику**:
   - `[cmd=/rules hover="Открыть правила"]📜 ПРАВИЛА[/cmd]`
   - `[cmd=/menu hover="Открыть главное меню"]🧭 МЕНЮ[/cmd]`
3. **Подстановка команды в чат**:
   - `[suggest=/pay %player_name% 100 hover="Перевести монеты"]Отправить 100$[/suggest]`
4. **Копирование текста в буфер обмена**:
   - `[copy=ATOMIC2026 hover="Нажмите, чтобы скопировать промокод"]ATOMIC2026[/copy]`
5. **Всплывающие подсказки (Tooltips)**:
   - `[hover="Скидки действуют до конца недели!"]🔥 СКИДКА 25%[/hover]`

---

### 📢 Модуль Чат-Анонсов (Chat Announcements)
- Периодические автоматические рассылки сообщений в чат с настраиваемым интервалом (`interval: 60`).
- Режимы ротации: `SEQUENTIAL` (последовательно) и `RANDOM` (случайно).
- **Звуковые эффекты**: воспроизведение звука при отправке (например, `ENTITY_PLAYER_LEVELUP`).
- **Экранные уведомления**:
  - **Title & Subtitle**: показ красивого заголовка на экране игрока с настройкой плавного появления (`fadeIn`, `stay`, `fadeOut`).
  - **ActionBar**: текст прямо над панелью предметов.
  - **BossBar**: отображение полосы босса сверху с настраиваемым цветом, стилем и длительностью.
- **Таргетинг и фильтрация**:
  - Фильтрация по правам (`permission: "atommessage.vip"` — показ только донатерам).
  - Фильтрация по мирам (`worlds: ["world", "world_nether"]`).

---

### 📑 Ротация в TAB (TabList Messages)
- Ротация сообщений в шапке (header) или подвале (footer) списка игроков (TAB).
- Прямой режим (`direct-tablist.enabled: true`) или работа через плейсхолдер `%atommessage_tab%` в сторонних плагинах (TAB by NEZNALY и др.).

---

## 📋 Команды и права

Основная команда: `/atommessage` (алиасы: `/am`, `/atommsg`)

| Команда | Описание | Право |
| :--- | :--- | :--- |
| `/atommessage broadcast <id>` | Моментально отправить конкретное объявление в чат | `atommessage.admin` |
| `/atommessage test <текст>` | Протестировать форматирование (цвета, ссылки, кнопки) | `atommessage.admin` |
| `/atommessage list` | Список всех настроенных сообщений (ТАБ и ЧАТ) | `atommessage.admin` |
| `/atommessage next` | Переключить сообщение в ТАБе на следующее | `atommessage.admin` |
| `/atommessage info` | Информация о текущих таймерах и статусе | `atommessage.admin` |
| `/atommessage reload` | Перезагрузить конфигурацию плагина | `atommessage.admin` |

---

## 🔌 Интеграция с PlaceholderAPI

| Плейсхолдер | Описание |
| :--- | :--- |
| `%atommessage_tab%` | Текущее форматированное сообщение для таба |
| `%atommessage_tab_current%` | ID текущего сообщения в табе |
| `%atommessage_tab_total%` | Общее количество сообщений в табе |
| `%atommessage_tab_time_left%` | Секунд до следующей смены таба |
| `%atommessage_chat_current%` | ID последнего отправленного анонса в чат |
| `%atommessage_chat_next_seconds%` | Секунд до следующего объявления в чате |
| `%atommessage_chat_total%` | Общее количество чат-анонсов |

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/aqkooo/AtomMessage.git
cd AtomMessage
mvn clean package
```

Готовый файл `AtomMessage-1.1.0.jar` будет в папке `target/`.
