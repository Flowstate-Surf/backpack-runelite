package com.backpack;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

final class GroupChallenge
{
	static final long COOLDOWN_SECONDS = 3_600;
	private final Goal goal;
	private final long progress;
	private final String winner;
	private final long cooldownUntil;

	GroupChallenge(Goal goal, long progress, String winner, long cooldownUntil)
	{
		this.goal = goal; this.progress = Math.max(0, progress); this.winner = winner; this.cooldownUntil = cooldownUntil;
	}
	Goal getGoal() { return goal; }
	long getProgress() { return progress; }
	String getWinner() { return winner; }
	boolean isCooldown() { return winner != null; }
	long remainingSeconds(Instant now) { return Math.max(0, cooldownUntil - now.getEpochSecond()); }
	boolean cooldownFinished(Instant now) { return isCooldown() && remainingSeconds(now) == 0; }
	GroupChallenge addProgress(long amount) { return new GroupChallenge(goal, progress + Math.max(0, amount), winner, cooldownUntil); }
	GroupChallenge wonBy(String name, Instant now) { return new GroupChallenge(goal, progress, name, now.getEpochSecond() + COOLDOWN_SECONDS); }

	String serialize()
	{
		String encodedGoal = Base64.getUrlEncoder().withoutPadding().encodeToString(goal.serialize().getBytes(StandardCharsets.UTF_8));
		String encodedWinner = winner == null ? "" : Base64.getUrlEncoder().withoutPadding().encodeToString(winner.getBytes(StandardCharsets.UTF_8));
		return encodedGoal + ":" + progress + ":" + encodedWinner + ":" + cooldownUntil;
	}
	static GroupChallenge deserialize(String value)
	{
		if (value == null) return null;
		try
		{
			String[] parts = value.split(":", -1);
			Goal goal = Goal.deserialize(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
			String winner = parts[2].isEmpty() ? null : new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
			return goal == null ? null : new GroupChallenge(goal, Long.parseLong(parts[1]), winner, Long.parseLong(parts[3]));
		}
		catch (RuntimeException ignored) { return null; }
	}
}
