# DisablePvP
Toggle PvP for yourself. Also allows you to toggle PvP in GriefPrevention claims. Yes, this is a Bukkit plugin.

If you decide to turn PvP off for yourself, you will be unable to receive nor give damage to/from other players.

### Commands
Note: square brackets `[]` indicate __optional__ arguments. The pipe `|` indicates different options for that argument.
- `/pvp [on|off]` - Toggles PvP for yourself. Default on (PvP is allowed)
- ~~`claimpvp` - Toggles PvP in the GriefPrevention claim you are standing in. By default, GriefPrevention disables PvP in claims - this toggles that protection.~~ I've removed this feature in 3.0 and thus its dependence on GriefPrevention.

Both toggles are persistent and are saved in the DisablePvP folder.

A player is "PvP-enabled" if they are not in a claim/in a claim which allows PvP, **and** the player allows PvP. (Deny supercedes allow.)
