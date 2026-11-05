package com.backpack;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

final class GroupChallenge
{
	static final long COOLDOWN_SECONDS = 3_600;
	static final long ACTIVE_SECONDS = 86_400;
	private final Goal goal;
	private final long progress;
	private final String winner;
	private final long cooldownUntil;
	private final long expiresAt;

	GroupChallenge(Goal goal, long progress, String winner, long cooldownUntil, long expiresAt)
	{
		this.goal = goal; this.progress = Math.max(0, progress); this.winner = winner;
		this.cooldownUntil = cooldownUntil; this.expiresAt = expiresAt;
	}
	static GroupChallenge active(Goal goal, Instant now) { return new GroupChallenge(goal, 0, null, 0, now.getEpochSecond() + ACTIVE_SECONDS); }
	Goal getGoal() { return goal; }
	long getProgress() { return progress; }
	String getWinner() { return winner; }
	boolean isCooldown() { return winner != null; }
	long cooldownRemainingSeconds(Instant now) { return Math.max(0, cooldownUntil - now.getEpochSecond()); }
	long activeRemainingSeconds(Instant now) { return isCooldown() ? 0 : Math.max(0, expiresAt - now.getEpochSecond()); }
	boolean cooldownFinished(Instant now) { return isCooldown() && cooldownRemainingSeconds(now) == 0; }
	boolean activeExpired(Instant now) { return !isCooldown() && activeRemainingSeconds(now) == 0; }
	GroupChallenge addProgress(long amount) { return new GroupChallenge(goal, progress + Math.max(0, amount), winner, cooldownUntil, expiresAt); }
	GroupChallenge wonBy(String name, Instant now) { return new GroupChallenge(goal, progress, name, now.getEpochSecond() + COOLDOWN_SECONDS, expiresAt); }

	String serialize()
	{
		String encodedGoal = Base64.getUrlEncoder().withoutPadding().encodeToString(goal.serialize().getBytes(StandardCharsets.UTF_8));
		String encodedWinner = winner == null ? "" : Base64.getUrlEncoder().withoutPadding().encodeToString(winner.getBytes(StandardCharsets.UTF_8));
		return encodedGoal + ":" + progress + ":" + encodedWinner + ":" + cooldownUntil + ":" + expiresAt;
	}
	static GroupChallenge deserialize(String value)
	{
		return deserialize(value, Instant.now());
	}
	static GroupChallenge deserialize(String value, Instant now)
	{
		if (value == null) return null;
		try
		{
			String[] parts = value.split(":", -1);
			Goal goal = Goal.deserialize(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
			String winner = parts[2].isEmpty() ? null : new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);
			long expiresAt = parts.length >= 5 ? Long.parseLong(parts[4]) : now.getEpochSecond() + ACTIVE_SECONDS;
			return goal == null ? null : new GroupChallenge(goal, Long.parseLong(parts[1]), winner, Long.parseLong(parts[3]), expiresAt);
		}
		catch (RuntimeException ignored) { return null; }
	}
}
