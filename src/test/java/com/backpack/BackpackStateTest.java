package com.backpack;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BackpackStateTest
{
	@Test
	public void resetsAcrossUtcWeeks()
	{
		BackpackState sunday = BackpackState.emptyForCurrentWeek(Instant.parse("2026-11-01T23:59:59Z"));
		assertTrue(sunday.isCurrentWeek(Instant.parse("2026-11-01T12:00:00Z")));
		assertFalse(sunday.isCurrentWeek(Instant.parse("2026-11-02T00:00:00Z")));
	}

	@Test
	public void ignoresNegativeXp()
	{
		BackpackState state = BackpackState.emptyForCurrentWeek(Instant.parse("2026-11-02T12:00:00Z")).addXp(500).addXp(-20);
		assertEquals(500, state.getXpGained());
	}
}
