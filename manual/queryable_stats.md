# `stats` command

With the `stats`/`rstats` commands, you can query every players' personal stats, like they are displayed for them only in the pause menu. This is possible, as the stat data is stored in the world folder server-side.

## command usage:

- `!stats [category] [stat_name] (part_of_player_name)`
    - categories are listed below
    - player name is optional - lists all players when empty
- `!rstats [category] [stat_name] (part_of_player_name)`
    - same as above, just relates stat to hours played in world (`play_time`)
    - playtime for each player is shown in round brackets

## categories:

### `custom`

- `animals_bred`: The number of times the player bred two mobs.
- `clean_armor`: The number of dyed leather armors washed with a cauldron.
- `clean_banner`: The number of banner patterns washed with a cauldron.
- `open_barrel`: The number of times the player has opened a Barrel.
- `bell_ring`: The number of times the player has rung a Bell.
- `eat_cake_slice`: The number of cake slices eaten.
- `fill_cauldron`: The number of times the player filled cauldrons with water buckets.
- `open_chest`: The number of times the player opened chests.
- `damage_absorbed`: The amount of damage the player has absorbed in tenths of ½♥.
- `damage_blocked_by_shield`: The amount of damage the player has blocked with a shield in tenths of ½♥.
- `damage_dealt`: The amount of damage the player has dealt in tenths of 1. Includes only melee attacks.
- `damage_dealt_absorbed`: The amount of damage the player has dealt that were absorbed, in tenths of ½♥.
- `damage_dealt_resisted`: The amount of damage the player has dealt that were resisted, in tenths of ½♥.
- `damage_resisted`: The amount of damage the player has resisted in tenths of ½♥.
- `damage_taken`: The amount of damage the player has taken in tenths of ½♥.
- `inspect_dispenser`: The number of times interacted with dispensers.
- `climb_one_cm`: The total distance traveled up ladders or vines.
- `crouch_one_cm`: The total distance walked while sneaking.
- `fall_one_cm`: The total distance fallen, excluding jumping. If the player falls more than one block, the entire jump is counted.
- `fly_one_cm`: Distance traveled upward and forward at the same time, while more than one block above the ground.
- `sprint_one_cm`: The total distance sprinted.
- `swim_one_cm`: The total distance covered with sprint-swimming.
- `walk_one_cm`: The total distance walked.
- `walk_on_water_one_cm`: The distance covered while bobbing up and down over water.
- `walk_under_water_one_cm`: The total distance you have walked underwater.
- `boat_one_cm`: The total distance traveled by boats.
- `aviate_one_cm`: The total distance traveled by elytra.
- `horse_one_cm`: The total distance traveled by horses.
- `minecart_one_cm`: The total distance traveled by minecarts.
- `pig_one_cm`: The total distance traveled by pigs via saddles.
- `strider_one_cm`: The total distance traveled by striders via saddles.
- `inspect_dropper`: The number of times interacted with droppers.
- `open_enderchest`: The number of times the player opened ender chests.
- `fish_caught`: The number of fish caught.
- `leave_game`: The number of times "Save and quit to title" has been clicked.
- `inspect_hopper`: The number of times interacted with hoppers.
- `interact_with_anvil`: The number of times interacted with anvils.
- `interact_with_beacon`: The number of times interacted with beacons.
- `interact_with_blast_furnace`: The number of times interacted with Blast Furnaces.
- `interact_with_brewingstand`: The number of times interacted with brewing stands.
- `interact_with_campfire`: The number of times interacted with Campfires.
- `interact_with_cartography_table`: The number of times interacted with Cartography Tables.
- `interact_with_crafting_table`: The number of times interacted with crafting tables.
- `interact_with_furnace`: The number of times interacted with furnaces.
- `interact_with_grindstone`: The number of times interacted with Grindstones.
- `interact_with_lectern`: The number of times interacted with Lecterns.
- `interact_with_loom`: The number of times interacted with Looms.
- `interact_with_smithing_table`: The number of times interacted with Smithing Tables.
- `interact_with_smoker`: The number of times interacted with Smokers.
- `interact_with_stonecutter`: The number of times interacted with Stonecutters.
- `drop`: The number of items dropped. This does not include items dropped upon death. If a group of items are dropped together, eg a stack of 64, it only counts as 1.
- `enchant_item`: The number of items enchanted.
- `jump`: The total number of jumps performed.
- `mob_kills`: The number of mobs the player killed.
- `play_record`: The number of music discs played on a jukebox.
- `play_noteblock`: The number of note blocks hit.
- `tune_noteblock`: The number of times interacted with note blocks.
- `deaths`: The number of times the player died.
- `pot_flower`: The number of plants potted onto flower pots.
- `player_kills`: The number of players the player killed (on PvP servers). Indirect kills do not count.
- `raid_trigger`: The number of times the player has triggered a Raid.
- `raid_win`: The number of times the player has won a Raid.
- `clean_shulker_box`: The number of times the player has washed a Shulker Box with a cauldron.
- `open_shulker_box`: The number of times the player has opened a Shulker Box.
- `sneak_time`: The time the player has held down the sneak button (tracked in ticks).
- `talked_to_villager`: The number of times interacted with villagers (opened the trading GUI).
- `target_hit`: The number of times the player has shot a target block.
- `play_time`: The total amount of time played (tracked in ticks).
- `time_since_death`: The time since the player's last death (tracked in ticks).
- `time_since_rest`: The time since the player's last rest (tracked in ticks). If this value is greater than 1.00 h, phantoms can spawn.
- `total_world_time`: The total amount of time the world was opened (tracked in ticks). Unlike Play Time, if the game is paused this number continues to increase, but it does not
  change visually while the statistics menu is open.
- `sleep_in_bed`: The number of times the player has slept in a bed.
- `traded_with_villager`: The number of times traded with villagers.
- `trigger_trapped_chest`: The number of times the player opened trapped chests.
- `use_cauldron`: The number of times the player took water from cauldrons with glass bottles.

### `mined`

- requires block (e.g. `stone`, `light_blue_glazed_terracotta`)

### `broken`

- refers to broken items
- requires item (e.g. `iron_pickaxe`, `fishing_rod`)

### `crafted`

- refers also to smelted items etc.
- requires item (e.g. `oak_planks`, `stone`)

### `used`

Statistics related to the number of block or item used. Players' statistic increases when a player uses a block or item. "Use" is defined as when:

- A shovel, a pickaxe, an axe, flint and steel, shears, a hoe, a bow, or a sword would normally consume durability
    - Players' statistics do not increase when armor consumes durability.
    - Players' statistics increase even if no durability is consumed, like when a torch is destroyed with an item that takes the destroy key to use (e.g. pickaxes).
- For fishing rods and carrots on sticks, the use key is used.
- A block is placed
- A painting, item frame, snowball, egg, spawn egg, any type of minecart or boat, eye of ender, ender pearl, bow, any type of throwable potion, Bottle o' Enchanting, or fishing rod
  spawns an entity
- A cocoa pod is planted on a jungle log, or bone meal is used to grow plants like crops, grass, and saplings
- A potion, bucket of milk, or any food (save cake) is consumed
- An empty map, empty bucket, lava bucket, water bucket, milk bucket, book and quill, or potion turns into a new item
    - Players' statistics do not increase when a bowl becomes mushroom stew or a bucket becomes milk.
- A music disc is placed in a jukebox

Players' statistics do not increase when items are used on mobs—whether to name, tame, feed, breed, saddle, leash, shear, dye, milk, or gather stew from—when armor is equipped
directly with use, when leather armor is washed in a cauldron, and instances mentioned above.

### `dropped` / `picked_up`

- refers to dropped items or blocks (e.g. `stone`, `diamond`)

### `killed` / `killed_by`

- requires entity (e.g. `enderman`, `wither_skeleton`)

(some parts are taken from [Statistics](https://minecraft.fandom.com/wiki/Statistics), accessed July 2023)
