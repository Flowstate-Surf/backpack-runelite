package com.backpack;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BadgeProgressTest
{
	@Test
	public void tracksAndPersistsAchievements()
	{
		BadgeProgress progress = BadgeProgress.empty().complete(Goal.Type.QUEST).complete(Goal.Type.SKILL_XP)
			.complete(Goal.Type.DROP).groupWin().carriedWeek().recordPoints(125);
		BadgeProgress restored = BadgeProgress.deserialize(progress.serialize());
		assertEquals(1, restored.getQuestTasks());
		assertEquals(3, restored.getTotalTasks());
		assertEquals(1, restored.getGroupWins());
		assertEquals(1, restored.getCarrierWeeks());
		assertEquals(125, restored.getBestWeeklyPoints());
	}
}
