package com.backpack;

final class Team
{
	private final String name;
	private final String code;
	private final int memberCount;
	private final boolean leader;

	Team(String name, String code, int memberCount, boolean leader)
	{
		this.name = name;
		this.code = code;
		this.memberCount = Math.max(1, memberCount);
		this.leader = leader;
	}

	String getName() { return name; }
	String getCode() { return code; }
	int getMemberCount() { return memberCount; }
	boolean isLeader() { return leader; }
}
