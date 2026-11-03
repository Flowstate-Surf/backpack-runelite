package com.backpack;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(BackpackConfig.GROUP)
public interface BackpackConfig extends Config
{
	String GROUP = "backpack";
	enum LevelNotifications
	{
		EVERY_LEVEL("Every level"), EVERY_TEN_LEVELS("Every 10 levels (20-90 and 99)");
		private final String label;
		LevelNotifications(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}

	@Range(min = 1)
	@ConfigItem(
		keyName = "weeklyXpGoal",
		name = "Weekly team XP goal",
		description = "XP the team is trying to gain this week"
	)
	default int weeklyXpGoal()
	{
		return 500_000;
	}

	@ConfigItem(keyName = "serverEnabled", name = "Use Backpack server", description = "Sends your display name and XP, quest-point, and qualifying drop-value deltas to the configured Backpack server", warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers")
	default boolean serverEnabled() { return false; }

	@ConfigItem(keyName = "serverUrl", name = "Backpack server URL", description = "HTTPS URL of the Backpack server (localhost HTTP is allowed for development)")
	default String serverUrl() { return "https://flow-api-tmzl4.ondigitalocean.app"; }

	@ConfigItem(
		keyName = "discordEnabled",
		name = "Post to Discord",
		description = "Sends your display name and level-up or qualifying loot details to a Discord webhook",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean discordEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "levelNotifications",
		name = "Level-up notifications",
		description = "Post every level-up or only levels 20, 30, 40, 50, 60, 70, 80, 90, and 99"
	)
	default LevelNotifications levelNotifications() { return LevelNotifications.EVERY_LEVEL; }

	@ConfigItem(
		keyName = "discordCollectionLog",
		name = "Collection Log notifications",
		description = "Post new Collection Log items to Discord"
	)
	default boolean discordCollectionLog() { return true; }

	@ConfigItem(
		keyName = "discordWebhookUrl",
		name = "Discord webhook URL",
		description = "A Discord channel webhook URL. It is stored in your RuneLite settings."
	)
	default String discordWebhookUrl()
	{
		return "";
	}

	@Range(min = 0)
	@ConfigItem(
		keyName = "discordDropValue",
		name = "Minimum drop value",
		description = "Only announce NPC loot worth at least this many coins"
	)
	default int discordDropValue()
	{
		return 1_000;
	}

	@ConfigItem(
		keyName = "discordDropScreenshot",
		name = "Attach Discord screenshots",
		description = "Attach a screenshot of the game frame to valuable-drop and Collection Log posts",
		warning = "Screenshots can contain chat messages or other private information"
	)
	default boolean discordDropScreenshot()
	{
		return true;
	}
}
