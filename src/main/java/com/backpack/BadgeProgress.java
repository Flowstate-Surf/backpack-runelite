package com.backpack;

final class BadgeProgress
{
	private final int questTasks, skillTasks, dropTasks, groupWins, carrierWeeks;
	private final long bestWeeklyPoints;
	BadgeProgress(int questTasks, int skillTasks, int dropTasks, int groupWins, int carrierWeeks, long bestWeeklyPoints)
	{
		this.questTasks = Math.max(0, questTasks); this.skillTasks = Math.max(0, skillTasks);
		this.dropTasks = Math.max(0, dropTasks); this.groupWins = Math.max(0, groupWins);
		this.carrierWeeks = Math.max(0, carrierWeeks); this.bestWeeklyPoints = Math.max(0, bestWeeklyPoints);
	}
	static BadgeProgress empty() { return new BadgeProgress(0, 0, 0, 0, 0, 0); }
	BadgeProgress complete(Goal.Type type)
	{
		return new BadgeProgress(questTasks + (type == Goal.Type.QUEST ? 1 : 0), skillTasks + (type == Goal.Type.SKILL_XP ? 1 : 0),
			dropTasks + (type == Goal.Type.DROP ? 1 : 0), groupWins, carrierWeeks, bestWeeklyPoints);
	}
	BadgeProgress groupWin() { return new BadgeProgress(questTasks, skillTasks, dropTasks, groupWins + 1, carrierWeeks, bestWeeklyPoints); }
	BadgeProgress carriedWeek() { return new BadgeProgress(questTasks, skillTasks, dropTasks, groupWins, carrierWeeks + 1, bestWeeklyPoints); }
	BadgeProgress recordPoints(long points) { return new BadgeProgress(questTasks, skillTasks, dropTasks, groupWins, carrierWeeks, Math.max(bestWeeklyPoints, points)); }
	int getQuestTasks() { return questTasks; } int getSkillTasks() { return skillTasks; } int getDropTasks() { return dropTasks; }
	int getGroupWins() { return groupWins; } int getCarrierWeeks() { return carrierWeeks; }
	long getBestWeeklyPoints() { return bestWeeklyPoints; }
	int getTotalTasks() { return questTasks + skillTasks + dropTasks; }
	String serialize() { return questTasks + "," + skillTasks + "," + dropTasks + "," + groupWins + "," + carrierWeeks + "," + bestWeeklyPoints; }
	static BadgeProgress deserialize(String value)
	{
		if (value == null) return empty();
		try { String[] p = value.split(","); return new BadgeProgress(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), Long.parseLong(p[5])); }
		catch (RuntimeException ignored) { return empty(); }
	}
}
