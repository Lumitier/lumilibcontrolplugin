# LumiLibControlPlugin

Paper plugin that lets server admins switch Lumitier's client-side mods on or off for everyone on the server.

Changes apply instantly to every connected player – no kick, no rejoin. Players' own mod settings are never changed: a mod disabled by the server simply pauses while they're connected and works as usual once they leave.

## Controllable mods

| Mod | ID |
|---|---|
| Shadow Tweaks | `shadowtweaks` |
| Perpetual Snow | `perpetualsnow` |
| Item Drop Tweaks | `itemdroptweaks` |
| Experience Orb Tweaks | `xporbtweaks` |

IP Address Hider is intentionally **not** controllable. It protects the player's privacy, and a server that could switch it off would defeat its purpose.

## Three ways to change the rules

1. **Config file** – `plugins/LumiLibControlPlugin/config.yml`, then `/lumimod reload`
2. **Command** – in the console or in game
3. **In-game menu** – with the [LumiLibControl](https://github.com/Lumitier/lumilibcontrol) client mod, from the LumiLib hub

## Commands

| Command | Who |
|---|---|
| `/lumimod list` | everyone |
| `/lumimod enable <mod>` | `lumimod.admin` |
| `/lumimod disable <mod>` | `lumimod.admin` |
| `/lumimod reload` | `lumimod.admin` |

## Permissions

`lumimod.admin` – change the rules. Operators have it by default.

With LuckPerms, grant it like any other permission:

```
/lp user <name> permission set lumimod.admin true
/lp group <group> permission set lumimod.admin true
```

Permission changes are picked up within a second – players don't need to rejoin.

## Good to know

- **Only affects players with [LumiLib](https://modrinth.com/mod/lumilib) 1.3.1+ installed.** Players without it aren't affected; the server can't see or change their mods.
- **This is not anti-cheat.** The server sends a request that the official mods follow. A modified client can ignore it.
- Every change from the menu is checked on the server against `lumimod.admin` – a client claiming to be an admin can't change anything.

## Requirements

- Paper 1.21.x (built against 1.21.11)
- Java 21+

## License

Apache-2.0
