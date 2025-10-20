# Research Table

A Fabric mod for Minecraft 1.20.1 that introduces the Research Table, a replacement for the vanilla enchanting table. Study enchanted items to unlock and level up enchantments, then apply them through a bespoke three-tab interface.

## Features
- Replace the enchanting table with a research-focused workstation that reuses the vanilla model and textures.
- Track per-player research progress across every enchantment with persistent storage.
- Study enchanted items to unlock enchantments and accumulate progress.
- Apply learned enchantments using a crafting tab that enforces compatibility, lapis costs, and experience level requirements.
- Multiplayer ready — progress is stored per-player on the server.

## Building
The project uses the Gradle wrapper with Fabric Loom so you do not need a system-wide Gradle installation. From the repository root:

```bash
./gradlew build      # macOS/Linux
gradlew.bat build    # Windows
```

> **Note:** If your terminal reports `'gradle' is not recognized`, make sure you are running the wrapper scripts above rather than a `gradle` command.

The wrapper will download the required Gradle version on first use and place the resulting mod jar in `build/libs/`.

## License
This project is released under the MIT License.
