# 👑 AtomTituls

**AtomTituls** — современная, полностью асинхронная и оптимизированная система титулов и суффиксов для серверов Minecraft (**Paper 1.16–1.21+**).

Позволяет игрокам выбирать и устанавливать уникальные титулы через визуальное инвентарное меню (GUI), отображать их в чате, табе и скорбордах через PlaceholderAPI.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%201.20+-blue)
![PlaceholderAPI](https://img.shields.io/badge/Support-PlaceholderAPI-purple)
![Author](https://img.shields.io/badge/Author-ejyqyl%20(%40aqkooo)-green)

---

## ⚡ Особенности

- **Интуитивный GUI**: Удобная пагинация, фильтрация доступных и заблокированных титулов, предпросмотр префикса перед установкой.
- **Асинхронная база данных**: Сохранение и загрузка данных игроков производятся в фоновых потоках без задержек основного тика.
- **Интеграция с PlaceholderAPI**:
  - `%atomtituls_active%` — текущий активный титул игрока
  - `%atomtituls_total%` — общее количество титулов
  - `%atomtituls_unlocked%` — количество разблокированных титулов
- **Гибкая выдача прав**: Доступ к титулам регулируется стандартными пермишенами (`atomtituls.titul.<id>`) или командами выдачи.
- **Совместимость с legacy-командами**: Поддержка алиасов `/titles`, `/stickhwtituls`.

---

## 📋 Команды и права

| Команда | Описание | Право |
| :--- | :--- | :--- |
| `/tituls` | Открыть визуальное меню титулов | Для всех игроков |
| `/tituls give <игрок> <титул>` | Выдать титул игроку | `atomtituls.give` |
| `/tituls take <игрок> <титул>` | Забрать титул у игрока | `atomtituls.take` |
| `/tituls reload` | Перезагрузить конфигурации и титулы | `atomtituls.reload` |

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/aqkooo/AtomTituls.git
cd AtomTituls
mvn clean package
```

Готовый файл `AtomTituls-2.0.0.jar` будет в папке `target/`.
