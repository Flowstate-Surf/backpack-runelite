package com.backpack;

final class CarryPoints
{
	private CarryPoints() {}

	static long forXp(long xp)
	{
		return Math.max(0, xp) / 10_000;
	}

	static long forQuestPoints(int questPoints)
	{
		return Math.max(0, questPoints) * 5L;
	}

	static long forDrop(long value)
	{
		return Math.max(0, value) / 1_000_000 * 5L;
	}

	static long goalBonus(long basePoints)
	{
		return Math.min(50, Math.max(5, (long) Math.ceil(Math.max(0, basePoints) * 0.20d)));
	}

	static long total(BackpackState state)
	{
		return forXp(state.getXpGained()) + forQuestPoints(state.getQuestPointsGained())
			+ state.getDropPoints() + state.getGoalBonusPoints();
	}
}
