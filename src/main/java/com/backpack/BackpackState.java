package com.backpack;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

final class BackpackState
{
	private final long weekStartEpoch;
	private final long xpGained;
	private final int questsCompleted;
	private final int dropsReceived;
	private final int questPointsGained;
	private final long dropPoints;
	private final long goalBonusPoints;

	BackpackState(long weekStartEpoch, long xpGained, int questsCompleted, int dropsReceived)
	{
		this(weekStartEpoch, xpGained, questsCompleted, dropsReceived, 0, 0, 0);
	}

	BackpackState(long weekStartEpoch, long xpGained, int questsCompleted, int dropsReceived,
		int questPointsGained, long dropPoints, long goalBonusPoints)
	{
		this.weekStartEpoch = weekStartEpoch;
		this.xpGained = Math.max(0, xpGained);
		this.questsCompleted = Math.max(0, questsCompleted);
		this.dropsReceived = Math.max(0, dropsReceived);
		this.questPointsGained = Math.max(0, questPointsGained);
		this.dropPoints = Math.max(0, dropPoints);
		this.goalBonusPoints = Math.max(0, goalBonusPoints);
	}

	static BackpackState emptyForCurrentWeek(Instant now)
	{
		return new BackpackState(startOfWeek(now), 0, 0, 0);
	}

	boolean isCurrentWeek(Instant now)
	{
		return weekStartEpoch == startOfWeek(now);
	}

	BackpackState addXp(long amount)
	{
		return new BackpackState(weekStartEpoch, xpGained + Math.max(0, amount), questsCompleted, dropsReceived, questPointsGained, dropPoints, goalBonusPoints);
	}

	BackpackState addDrop(long value)
	{
		return new BackpackState(weekStartEpoch, xpGained, questsCompleted, dropsReceived + 1, questPointsGained, dropPoints + CarryPoints.forDrop(value), goalBonusPoints);
	}

	BackpackState addQuestPoints(int amount)
	{
		return new BackpackState(weekStartEpoch, xpGained, questsCompleted + 1, dropsReceived, questPointsGained + Math.max(0, amount), dropPoints, goalBonusPoints);
	}

	BackpackState addGoalBonus(long points)
	{
		return new BackpackState(weekStartEpoch, xpGained, questsCompleted, dropsReceived, questPointsGained, dropPoints, goalBonusPoints + Math.max(0, points));
	}

	BackpackState reset()
	{
		return new BackpackState(weekStartEpoch, 0, 0, 0, 0, 0, 0);
	}

	long getWeekStartEpoch() { return weekStartEpoch; }
	long getXpGained() { return xpGained; }
	int getQuestsCompleted() { return questsCompleted; }
	int getDropsReceived() { return dropsReceived; }
	int getQuestPointsGained() { return questPointsGained; }
	long getDropPoints() { return dropPoints; }
	long getGoalBonusPoints() { return goalBonusPoints; }

	private static long startOfWeek(Instant instant)
	{
		ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
		return utc.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay(ZoneOffset.UTC).toEpochSecond();
	}
}
