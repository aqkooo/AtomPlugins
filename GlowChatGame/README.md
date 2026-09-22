# 🎯 GlowChatGame

**GlowChatGame** — ультра-оптимизированный, модульный плагин мини-игр и викторин в чате со встроенной статистикой SQLite и поддержкой MiniMessage для серверов Minecraft (**Paper/Spigot 1.16–1.21+**).

Вовлекает игроков интерактивными событиями в чате: решение математических примеров, анаграммы, разгадывание перевёрнутых слов и викторины с моментальной выдачей наград.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-blue)
![Database](https://img.shields.io/badge/Database-SQLite-lightgrey)
![Author](https://img.shields.io/badge/Author-ejyqyl%20(%40aqkooo)-green)

---

## ⚡ Особенности

- **Разнообразные типы игр**:
  - 🔢 **Математика**: примеры со сложением, вычитанием, умножением и скобками разных уровней сложности.
  - 🔤 **Анаграммы**: поиск исходного слова из перемешанных букв.
  - 🔄 **Реверс**: правильное прочтение задом наперёд.
  - ❓ **Викторина (Trivia)**: вопросы с настраиваемыми ответами.
- **Асинхронная статистика SQLite**: Детальный учёт побед каждого игрока, времени реакции (мс) и заработанных наград.
- **Интеграция с Vault**: Автоматическая выдача игровой валюты за победу в раунде.
- **PlaceholderAPI**:
  - `%glowchatgame_wins%` — победы игрока
  - `%glowchatgame_best_time%` — рекордное время ответа игрока

---

## 📋 Команды и права

| Команда | Описание | Алиасы | Право |
| :--- | :--- | :--- | :--- |
| `/chatgame stats` | Посмотреть свою статистику побед | `/cg stats` | `glowchatgame.use` |
| `/chatgame start [тип]` | Принудительно запустить раунд игры | `/cg start` | `glowchatgame.admin` |
| `/chatgame stop` | Остановить текущий раунд | `/cg stop` | `glowchatgame.admin` |
| `/chatgame reload` | Перезагрузить конфигурацию | `/cg reload` | `glowchatgame.admin` |

---

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/aqkooo/GlowChatGame.git
cd GlowChatGame
mvn clean package
```

Скомпилированный `.jar` файл будет доступен в папке `target/GlowChatGame-1.0.0.jar`.
