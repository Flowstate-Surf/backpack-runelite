# Backpack

Backpack turns Old School RuneScape progress into a friendly weekly team race inside RuneLite. Gain XP, earn quest points, land valuable drops, and complete goals to see who is **carrying the team**.

[Plugin overview website](https://backpack-runelite.josh-kyle-bell.chatgpt.site)

## Features

- Create or join a team with an eight-character invite code
- Weekly Carry Points leaderboard with profile progress bars
- 1 point per 10,000 XP, 5 points per quest point, and 5 points per 1 million GP of qualifying drop value
- Leader-assigned skill, quest, and item-drop goals
- Stat-aware generated tasks and group challenges
- Expandable weekly activity breakdowns
- Persistent badges for tasks, challenge wins, points, and weeks spent carrying
- Per-RuneScape-profile team, goal, badge, and progress storage
- Optional cross-client team synchronization
- Optional Discord level-up and valuable-drop announcements, including opt-in screenshots

## Privacy and network features

Backpack is local-first. Cross-client synchronization and Discord posting are separate, disabled-by-default options.

When **Use Backpack server** is enabled, the configured server receives the player's display name and XP, quest-point, and qualifying drop-value deltas. It does not receive RuneScape login credentials, chat, inventory contents, or Jagex session data.

When **Post to Discord** is enabled, the configured Discord webhook receives the player's display name and the relevant level-up or loot details. Drop screenshots are a separate option and can contain chat or other visible game information. Discord mentions are disabled.

## Development

Backpack targets Java 11 and follows the RuneLite example-plugin layout.

```text
./gradlew test
./gradlew run
```

For Jagex accounts, follow RuneLite's [development login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
