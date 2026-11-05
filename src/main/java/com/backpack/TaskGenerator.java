package com.backpack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/** A deliberately conservative, expandable catalogue of useful quest routes. */
final class TaskGenerator
{
	private static final class Route
	{
		final String quest;
		final int combat;
		final Map<Skill, Integer> requirements = new EnumMap<>(Skill.class);
		Route(String quest, int combat, Object... values)
		{
			this.quest = quest; this.combat = combat;
			for (int i = 0; i < values.length; i += 2) requirements.put((Skill) values[i], (Integer) values[i + 1]);
		}
	}

	private static final Route[] ROUTES = {
		new Route("Witch's House", 20),
		new Route("Waterfall Quest", 25),
		new Route("The Knight's Sword", 10, Skill.MINING, 10),
		new Route("Elemental Workshop I", 20, Skill.MINING, 20, Skill.SMITHING, 20, Skill.CRAFTING, 20),
		new Route("Lost City", 40, Skill.CRAFTING, 31, Skill.WOODCUTTING, 36)
	};

	private final Client client;
	private final GoalCatalog catalog;
	@Inject TaskGenerator(Client client, GoalCatalog catalog) { this.client = client; this.catalog = catalog; }

	TaskPlan generate()
	{
		return generate(new Goal[0]);
	}

	TaskPlan generate(Goal... excluded)
	{
		int combat = client.getLocalPlayer() == null ? 3 : client.getLocalPlayer().getCombatLevel();
		List<TaskPlan> exact = new ArrayList<>();
		List<TaskPlan> near = new ArrayList<>();
		for (Route route : ROUTES)
		{
			Quest quest = findQuest(route.quest);
			if (quest == null || quest.getState(client) == QuestState.FINISHED || combat + 5 < route.combat) continue;
			Skill missing = null; int gap = 0; boolean tooFar = false;
			for (Map.Entry<Skill, Integer> requirement : route.requirements.entrySet())
			{
				int difference = requirement.getValue() - client.getRealSkillLevel(requirement.getKey());
				if (difference > 0)
				{
					if (difference > 4 || missing != null) { tooFar = true; break; }
					missing = requirement.getKey(); gap = difference;
				}
			}
			Goal questGoal = new Goal(Goal.Type.QUEST, quest.getName(), 1, quest.name());
			if (!tooFar && missing == null) exact.add(new TaskPlan(questGoal, null));
			else if (!tooFar)
			{
				int targetLevel = client.getRealSkillLevel(missing) + gap;
				long xp = Math.max(1, Experience.getXpForLevel(targetLevel) - client.getSkillExperience(missing));
				near.add(new TaskPlan(new Goal(Goal.Type.SKILL_XP, pretty(missing) + " to level " + targetLevel, xp, missing.name()), questGoal));
			}
		}
		addDropTask(exact, combat, "Hill giant club", "Obor", 45, Skill.SLAYER, 1);
		addDropTask(exact, combat, "Bryophyta's essence", "Bryophyta", 55, Skill.SLAYER, 1);
		addDropTask(exact, combat, "Granite maul", "Gargoyle", 70, Skill.SLAYER, 75);
		addDropTask(exact, combat, "Dragon boots", "Spiritual mage", 85, Skill.SLAYER, 83);
		addDropTask(exact, combat, "Abyssal whip", "Abyssal demon", 95, Skill.SLAYER, 85);
		List<TaskPlan> choices = allowed(near, excluded);
		if (choices.isEmpty()) choices = allowed(exact, excluded);
		if (!choices.isEmpty()) return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
		Skill[] fallbackSkills = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.SLAYER, Skill.PRAYER, Skill.AGILITY, Skill.COOKING};
		List<Skill> fallbacks = new ArrayList<>();
		for (Skill skill : fallbackSkills)
		{
			Goal candidate = new Goal(Goal.Type.SKILL_XP, pretty(skill) + " training", 50_000, skill.name());
			if (!isExcluded(candidate, excluded)) fallbacks.add(skill);
		}
		Skill fallback = fallbacks.isEmpty() ? Skill.ATTACK : fallbacks.get(ThreadLocalRandom.current().nextInt(fallbacks.size()));
		return new TaskPlan(new Goal(Goal.Type.SKILL_XP, pretty(fallback) + " training", 50_000, fallback.name()), null);
	}

	private static List<TaskPlan> allowed(List<TaskPlan> plans, Goal... excluded)
	{
		List<TaskPlan> result = new ArrayList<>();
		for (TaskPlan plan : plans) if (!isExcluded(plan.getCurrent(), excluded)) result.add(plan);
		return result;
	}

	private static boolean isExcluded(Goal candidate, Goal... excluded)
	{
		for (Goal goal : excluded) if (candidate.sameTask(goal)) return true;
		return false;
	}

	private void addDropTask(List<TaskPlan> tasks, int combat, String item, String monster, int requiredCombat, Skill skill, int requiredLevel)
	{
		if (combat < requiredCombat || client.getRealSkillLevel(skill) < requiredLevel) return;
		for (GoalCatalog.Choice choice : catalog.findItems(item))
		{
			if (choice.getName().equalsIgnoreCase(item))
			{
				tasks.add(new TaskPlan(new Goal(Goal.Type.DROP, item + " from " + monster, 1, choice.getReference()), null));
				return;
			}
		}
	}

	private Quest findQuest(String name)
	{
		for (Quest quest : Quest.values()) if (quest.getName().equalsIgnoreCase(name)) return quest;
		return null;
	}

	private static String pretty(Skill skill)
	{
		String value = skill.name().toLowerCase().replace('_', ' ');
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}
}
