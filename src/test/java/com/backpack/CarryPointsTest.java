package com.backpack;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CarryPointsTest
{
	@Test
	public void scoresBaseActivities()
	{
		assertEquals(50, CarryPoints.forXp(500_000));
		assertEquals(10, CarryPoints.forQuestPoints(2));
		assertEquals(50, CarryPoints.forDrop(10_000_000));
	}

	@Test
	public void clampsGoalBonus()
	{
		assertEquals(5, CarryPoints.goalBonus(1));
		assertEquals(10, CarryPoints.goalBonus(50));
		assertEquals(50, CarryPoints.goalBonus(10_000));
	}
}
