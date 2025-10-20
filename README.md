# Research Table

A Fabric mod for Minecraft 1.20.1 that introduces the Research Table, a replacement for the vanilla enchanting table. Study enchanted items to unlock and level up enchantments, then apply them through a bespoke three-tab interface.

## Features
- Replace the enchanting table with a research-focused workstation that reuses the vanilla model and textures.
- Track per-player research progress across every enchantment with persistent storage.
- Study enchanted items to unlock enchantments and accumulate progress.
- Apply learned enchantments using a crafting tab that enforces compatibility, lapis costs, and experience level requirements.
- Multiplayer ready — progress is stored per-player on the server.

## Building
The project uses Gradle with Fabric Loom. From the repository root:

```bash
./gradlew build
```

If the Gradle wrapper jar has not been bootstrapped yet, the helper script falls back to any Gradle installation on your PATH. The resulting mod jar will be placed in `build/libs/`.

## License
This project is released under the MIT License.
