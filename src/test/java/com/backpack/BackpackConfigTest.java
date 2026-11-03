package com.backpack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BackpackConfigTest
{
	private final BackpackConfig config = new BackpackConfig() { };

	@Test
	public void safeDefaultsMatchDashboardSettings()
	{
		assertEquals(500_000, config.weeklyXpGoal());
		assertEquals(1_000, config.discordDropValue());
		assertTrue(config.discordDropScreenshot());
		assertTrue(config.discordCollectionLog());
		assertEquals("", config.discordWebhookUrl());
		assertFalse(config.discordEnabled());
		assertFalse(config.serverEnabled());
	}

	@Test
	public void milestoneModeIncludesTwentyThroughNinetyAndNinetyNine()
	{
		for (int level : new int[] {20, 30, 40, 50, 60, 70, 80, 90, 99})
		{
			assertTrue(BackpackPlugin.shouldAnnounceLevel(BackpackConfig.LevelNotifications.EVERY_TEN_LEVELS, level));
		}
		for (int level : new int[] {1, 10, 19, 21, 89, 91, 98})
		{
			assertFalse(BackpackPlugin.shouldAnnounceLevel(BackpackConfig.LevelNotifications.EVERY_TEN_LEVELS, level));
		}
		assertTrue(BackpackPlugin.shouldAnnounceLevel(BackpackConfig.LevelNotifications.EVERY_LEVEL, 37));
	}
}
