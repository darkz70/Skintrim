# Skintrim

![Skintrim logo](logo.png)

**Skintrim** is a Fabric mod for Minecraft **26.2 / 26.3** (Java 25) that lets you change your **skin and cape right in game** — no launcher restart, no rejoining the server.

Built with [MossyPlugin](https://github.com/LopyMine/MossyPlugin) (by LopyMine) + Stonecutter, targeting `fabric-26.2` and `fabric-26.3` from a single source tree.

## Opening the editor

- Press **K** (rebindable in Controls, category "Skintrim")
- Or use **ModMenu** → Skintrim

## Features

Each field (skin / cape) accepts:

| Input | What happens |
|---|---|
| Path to a PNG (`C:\skins\my.png`, or `my.png` from `config/skintrim/skins/`) | Loads the texture from disk |
| An `https://...png` URL | Downloads the texture |
| A Minecraft username (`Notch`) | Pulls the skin (and cape, if any) from that player's Mojang profile |
| Drag & drop a PNG onto the game window | Applies it automatically; files with `cape` in the name apply as a cape |

More in the editor window:

- **Model**: Classic / Slim for your skin.
- **Live 2D preview** of the skin and cape.
- **History**: the last 16 skins and 16 capes you applied, with model and date. Click any entry to wear it again. Entries keep a cached PNG copy, so they keep working even if the original file or URL is gone. History can be cleared per tab.
- **Server Sync** toggle (see below).
- **Skins Folder** button opens `config/skintrim/skins/`, a handy place to drop PNGs.

Your choices are saved to `config/skintrim.json` and restored on the next launch.

## Capes without Mojang

Custom capes are **fully independent of Mojang**:

- You do **not** need to own an official Mojang cape, and nothing is uploaded to Mojang for capes (Mojang does not accept custom cape uploads at all).
- Cape source is switchable in the editor: **Custom** (this mod), **Mojang** (your official cape), or **Hidden** (no cape).
- A custom cape works in singleplayer and offline mode, and is also sent through server sync so other Skintrim users can see it.
- Your custom cape texture is also used for the elytra.

## Who can see your new look?

Three levels of visibility, honestly:

1. **You — instantly.** The local override works in singleplayer and on any server, nothing else required.
2. **Other Skintrim users on a server that also runs Skintrim** — via sync: your client sends the skin/cape PNGs to the server (`skintrim:skin_sync`), the server broadcasts them to everyone (`skintrim:skin_broadcast`). The same jar works on a dedicated server; no client code is loaded there. Custom capes are visible this way.
3. **Everyone, including vanilla clients** — the **Upload to Mojang** button sends the skin to your official profile via `api.minecraftservices.com` (requires a licensed account in online mode). Note: this only works for skins — Mojang cannot accept custom capes — and propagation to other players may take a few minutes or a rejoin.

## Requirements

- Minecraft 26.2 or 26.3, Fabric Loader ≥ 0.19.3, Fabric API, Java 25
- ModMenu is optional (adds a settings button)

## Building

```bash
./gradlew buildAndCollect+fabric+All -Pci_loader=fabric
```

Built jars land in `libs/`. The active version for running from the IDE is set in `versions/active.txt` (`fabric-26.3`) and switched via Stonecutter.

> Before publishing on Modrinth/CurseForge, replace `modrinth_id` / `curseforge_id` in `gradle.properties` with the real Skintrim project IDs. A ready-made store page text lives in [PUBLISHING.md](PUBLISHING.md).

## License

CC BY-ND 4.0 — see `LICENSE`.
