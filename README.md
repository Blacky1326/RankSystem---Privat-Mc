# RankSystem Plugin

Neon Rank System für Paper 26.1 mit LuckPerms-Integration.

## Ränge & Farben

| Rang | Farbe | Sortierung |
|------|-------|------------|
| 👑 OWNER | Rot → Orange (Gradient) | 1 |
| ⚡ ADMIN | Orange → Gold (Gradient) | 2 |
| 🛡️ MOD | Blau → Lila (Gradient) | 3 |
| 💬 SUPPORTER | Grün → Türkis (Gradient) | 4 |
| 🎮 STREAMER | Lila → Pink (Gradient) | 5 |
| ⭐ VIP | Gold → Orange (Gradient) | 6 |
| 👤 Mitglied | Grau | 7 |

## Installation

1. **Voraussetzungen:**
   - Paper 1.21+ Server
   - LuckPerms Plugin

2. **Builden:**
   ```bash
   ./gradlew build
   ```
   Die fertige JAR liegt in `build/libs/RankSystem-1.0.0.jar`

3. **JAR in `/plugins/` kopieren** und Server starten

## Befehle

| Befehl | Beschreibung | Permission |
|--------|-------------|------------|
| `/rank set <Spieler> <Rang>` | Rang setzen | `ranksystem.admin` |
| `/rank info [Spieler]` | Rang anzeigen | `ranksystem.admin` |
| `/rank list` | Alle Ränge anzeigen | `ranksystem.admin` |

## Verfügbare Ränge (Befehl)
`OWNER`, `ADMIN`, `MODERATOR`, `SUPPORTER`, `STREAMER`, `VIP`, `MITGLIED`

## Permissions für andere Plugins

```
ranksystem.vip       → VIP-Features freischalten
ranksystem.streamer  → Streamer-Features
ranksystem.mod       → Moderator-Features
ranksystem.support   → Supporter-Features
ranksystem.admin     → Admin-Befehle
```

Diese Permissions werden automatisch durch LuckPerms-Gruppen-Vererbung gesetzt.

## LuckPerms Gruppen-Hierarchie

```
owner
  └── admin
        └── moderator
              └── supporter
                    └── default (Mitglied)

streamer
  └── vip
        └── default (Mitglied)
```

## Tab-Liste Aussehen

```
╔══════════════════════╗
  ✦ Dein Server ✦
  Spieler online: 5
╠══════════════════════╣
  OWNER  OwnerName
  ADMIN  AdminName
  MOD    ModName
  ...
╠══════════════════════╣
  discord.gg/deinserver
╚══════════════════════╝
```
