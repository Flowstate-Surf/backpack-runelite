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
		private final String role;
		private final long points;
		private final long xp;

		Member(String name, String role, long points, long xp)
		{
			this.name = name; this.role = role; this.points = Math.max(0, points); this.xp = Math.max(0, xp);
		}
		String getName() { return name; }
		boolean isLeader() { return "LEADER".equals(role); }
		long getPoints() { return points; }
		long getXp() { return xp; }
	}
}
