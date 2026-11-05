package com.backpack;

import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GroupChallengeTest
{
	private static final Instant START = Instant.ofEpochSecond(1_000_000);

	@Test
	public void activeChallengeExpiresAfterTwentyFourHours()
	{
		GroupChallenge challenge = GroupChallenge.active(new Goal(Goal.Type.SKILL_XP, "Attack", 10_000, "ATTACK"), START);
		assertEquals(GroupChallenge.ACTIVE_SECONDS, challenge.activeRemainingSeconds(START));
		assertFalse(challenge.activeExpired(START.plusSeconds(GroupChallenge.ACTIVE_SECONDS - 1)));
		assertTrue(challenge.activeExpired(START.plusSeconds(GroupChallenge.ACTIVE_SECONDS)));
	}

	@Test
	public void deadlineSurvivesSerialization()
	{
		GroupChallenge original = GroupChallenge.active(new Goal(Goal.Type.DROP, "Rune scimitar", 1, "1333"), START).addProgress(1);
		GroupChallenge restored = GroupChallenge.deserialize(original.serialize(), START.plusSeconds(123));
		assertNotNull(restored);
		assertEquals(GroupChallenge.ACTIVE_SECONDS - 123, restored.activeRemainingSeconds(START.plusSeconds(123)));
		assertEquals(1, restored.getProgress());
	}

	@Test
	public void oldSavedChallengeReceivesFreshDeadline()
	{
		Goal goal = new Goal(Goal.Type.QUEST, "Cook's Assistant", 1, "COOKS_ASSISTANT");
		String encodedGoal = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(goal.serialize().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		GroupChallenge restored = GroupChallenge.deserialize(encodedGoal + ":0::0", START);
		assertNotNull(restored);
		assertEquals(GroupChallenge.ACTIVE_SECONDS, restored.activeRemainingSeconds(START));
	}
}
