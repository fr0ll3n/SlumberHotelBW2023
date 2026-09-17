# SlumberHotelCore

Hypixel-style BedWars **Slumber Hotel** progression for Minecraft **1.8.8 / 1.8.9**.

## Requires
- Spigot/Paper 1.8.8–1.8.9
- PlayerPoints
- BedWars2023 (arena) / BW2023Proxy (lobby)
- Citizens (NPCs)
- Optional: PlaceholderAPI, LuckPerms, Vault, DeluxeMenus, cosmetics plugin (Mher one), Citizens

## Install
1. Put jar in `/plugins` on **lobby and arena**
2. Create Citizens NPCs
4. Configure MySQL optional: `storage.type: MYSQL`
5. Restart `/slumber reload`

## Commands
| Command | Description |
|---------|-------------|
| `/slumber quests` | Quest log GUI |
| `/slumber tickets` | Balance |
| `/slumber wallet` | Capacity |
| `/slumber daily` | Daily challenges |
| `/slumber door [id]` | List / unlock doors |
| `/slumber machine` | Ticket machine |
| `/slumber stars` | Hotel stars |
| `/slumber cosmetics` | Cosmetics shop list |
| `/slumber cosmetics buy <id>` | Buy cosmetic with tickets |
| `/slumber give/setwallet/giveitem` | Admin |

## Placeholders (`%slumber_*%`)
tickets, wallet, wallet_tier, stars, needed_entry, needed_wallet, needed_door_<id>, games_played, quests_active, quests_completed, machine_cost, ...

## Progression (high level)
1. Play 5 games → Doorman Dave (25 tickets) → enter
2. Receptionist + Saichi / Daku / John → wallet upgrade
3. Doors, King Flut, Blacksmith hammer, Oasis, Arcade, CEO, Quiz, SkyBlock, Ratman
4. Jimmy tribute + wool cables → staff wallet
5. Owner's Office (40k tickets) → Sandman

## Config sections
- `tickets`, `wallet`, `special-items`, `costs`, `quests`, `npcs`, `doors`
- `daily-quests`, `ticket-machine`, `stars`, `cosmetics-shop`, `storage`

Expand quests by copying a quest block under `quests:` – no code change needed for new TALK/COLLECT/PAY quests.
