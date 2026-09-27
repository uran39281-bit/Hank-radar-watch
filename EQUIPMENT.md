# USA equipment — v0.9

`assets/equipment.properties` supplies runtime radar and missile values. `TechTree.java` supplies the USA research catalog, prerequisites and initial prices. The menu offers USA TECH TREE, ECONOMY / REWARDS and EQUIPMENT / STATS.

## Starter battery

| ADS-201 Watchpost | Value |
| --- | ---: |
| Detection range | 30 km |
| Tracking range | 24 km |
| Radar lock range | 16 km |
| Simultaneous tracks | 5 |
| Datalink channels | 2 |
| Scan speed | 1.00 / 10.00 |
| IR guidance / IR lock | N/A |

Scan rating accepts decimals from 1.00 to 10.00. A full sweep takes `11 - scanSpeed` seconds (Watchpost: ten seconds). Aircraft and incoming missiles share track slots. Two observations establish a track if there is capacity and it is within the tracking envelope. Coasting, terrain/clutter, priority refreshes, radar damage, selected display range and uncertain identification still apply. Channels are allocated per locked target; multiple shots can share the target's channel. Both equipped missiles need a maintained target lock. No IR missile is included in the Watchpost loadout.

## USA missiles

| Characteristic | MIM-301 Rampart | FIM-352 Stonebolt |
| --- | ---: | ---: |
| Guidance | Radar / semi-active | Radar / semi-active |
| Guidance time | 35 s | 40 s |
| Maximum speed | 2,400 km/h | 2,700 km/h |
| Maximum G | 14 | 12 |
| Mass | 140 kg | 180 kg |
| Maximum AOA | 12 degrees | 10 degrees |
| Maximum thrust | 18 kN | 22 kN |
| Research cost | Free starter | 600 BP |
| Purchase cost | Free starter | $1,500 |

The supplied speeds are interpreted as km/h and the thrust figures as kN, converted to N internally. Supporting values absent from the request are initial arcade balance: 5s/6s motor burns, 24km/28km flight-path caps, and the existing 13,700m ceiling. Watchpost's 16km radar lock remains the tighter launch limit. Stonebolt is faster and guides longer but has lower G/AOA and more mass; it is not better in every maneuver.

Each weapon property is prefixed `missile.rampart.` or `missile.stonebolt.`; the existing five hostile profiles remain under `kh25`, `kh29`, `kh23`, `kh27`, `kh58`. `Game.weapon` snapshots the owned/equipped missile at mission start and each projectile retains its own profile. MissileMotion applies common thrust/mass acceleration, speed caps, finite turns, coast drag and turn energy loss. This is fictional arcade pursuit, not an operational weapon model.

## Research and ownership

Watchpost and Rampart are free and owned for every new or migrated save. Stonebolt follows Rampart. Select its tree card, apply banked BP (partial progress allowed), buy it with Dollars after research completes, then EQUIP. Research deducts at most the remaining requirement. Purchase alone does not switch the loadout. EQUIP is free and reversible between missions; all three launchers use the chosen missile next mission. Ammunition and repairs remain free.

Research, ownership, loadout and currency are saved in the same preference transaction. Lifetime/mission earnings remain gross earnings, while wallet balances show spendable funds. Old `economy_*` keys and `hawk_best` remain compatible. Schema 2 adds `economy_tech_research`, `economy_tech_owned` and `economy_equipped_missile`. No historical currency is reset. A failed transaction does not change the in-memory wallet/ownership/loadout; the UI reports failure and offers retry. The existing rewards save/retry behavior is retained.

`./test.sh` runs the pure Java suites. TechTreeTest verifies exact requested stats, starter migration, partial research, insufficient funds, duplicate prevention, save failures/retries, persistent equip state, and actual Rampart/Stonebolt interceptions. Desktop Android stubs check the tree UI and preference adapter; real-device installation/playtesting remains unverified.
