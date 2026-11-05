package com.backpack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Team
{
	private final String name;
	private final String code;
	private final int memberCount;
	private final boolean leader;
	private final List<Member> members;

	Team(String name, String code, int memberCount, boolean leader)
	{
		this(name, code, memberCount, leader, Collections.emptyList());
	}

	Team(String name, String code, int memberCount, boolean leader, List<Member> members)
	{
		this.name = name;
		this.code = code;
		this.memberCount = Math.max(1, memberCount);
		this.leader = leader;
		this.members = Collections.unmodifiableList(new ArrayList<>(members));
	}

	String getName() { return name; }
	String getCode() { return code; }
	int getMemberCount() { return memberCount; }
	boolean isLeader() { return leader; }
	List<Member> getMembers() { return members; }

	static final class Member
	{
		private final String name;
		private final String id;
		private final String role;
		private final long points;
		private final long xp;
		private final int combatLevel;
		private final Goal goal;
		private final long goalProgress;
		private final boolean goalCompleted;

		Member(String id, String name, String role, long points, long xp, int combatLevel, Goal goal, long goalProgress, boolean goalCompleted)
		{
			this.id = id; this.name = name; this.role = role; this.points = Math.max(0, points); this.xp = Math.max(0, xp);
			this.combatLevel = Math.max(0, combatLevel); this.goal = goal; this.goalProgress = Math.max(0, goalProgress); this.goalCompleted = goalCompleted;
		}
		String getId() { return id; }
		String getName() { return name; }
		boolean isLeader() { return "LEADER".equals(role); }
		long getPoints() { return points; }
		long getXp() { return xp; }
		int getCombatLevel() { return combatLevel; }
		Goal getGoal() { return goal; }
		long getGoalProgress() { return goalProgress; }
		boolean isGoalCompleted() { return goalCompleted; }
		@Override public String toString() { return name; }
	}
}
