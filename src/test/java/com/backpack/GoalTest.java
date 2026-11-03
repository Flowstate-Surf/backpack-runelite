package com.backpack;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GoalTest
{
	@Test
	public void roundTripsGoal()
	{
		Goal original = new Goal(Goal.Type.DROP, "Tumeken's shadow: rare", 1);
		Goal restored = Goal.deserialize(original.serialize());
		assertEquals(Goal.Type.DROP, restored.getType());
		assertEquals("Tumeken's shadow: rare", restored.getDetail());
		assertEquals(1, restored.getTarget());
	}

	@Test
	public void rejectsBrokenGoal()
	{
		assertNull(Goal.deserialize("not-a-goal"));
	}
}
