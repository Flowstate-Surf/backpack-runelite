package com.backpack;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.http.api.item.ItemPrice;

final class GoalCatalog
{
	static final class Choice
	{
		private final String name;
		private final String reference;
		Choice(String name, String reference) { this.name = name; this.reference = reference; }
		String getName() { return name; }
		String getReference() { return reference; }
		@Override public String toString() { return name; }
	}

	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final SpriteManager spriteManager;
	private List<ItemPrice> items;
	private final Map<String, BufferedImage> badgeIcons = new HashMap<>();

	@Inject
	GoalCatalog(ItemManager itemManager, SkillIconManager skillIconManager, SpriteManager spriteManager)
	{
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.spriteManager = spriteManager;
	}

	List<Choice> skills()
	{
		return Arrays.stream(Skill.values()).filter(skill -> skill != Skill.OVERALL)
			.map(skill -> new Choice(title(skill.name()), skill.name())).collect(Collectors.toList());
	}

	List<Choice> quests()
	{
		return Arrays.stream(Quest.values()).map(quest -> new Choice(quest.getName(), quest.name()))
			.sorted(Comparator.comparing(Choice::getName)).collect(Collectors.toList());
	}

	List<Choice> findItems(String query)
	{
		if (items == null || items.isEmpty()) items = new ArrayList<>(itemManager.search(""));
		String needle = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
		return items.stream().sorted(Comparator.comparingInt(item -> score(item.getName().toLowerCase(Locale.ENGLISH), needle)))
			.limit(30).map(item -> new Choice(item.getName(), Integer.toString(item.getId()))).collect(Collectors.toList());
	}

	BufferedImage icon(Goal goal)
	{
		try
		{
			switch (goal.getType())
			{
				case SKILL_XP: return skillIconManager.getSkillImage(Skill.valueOf(goal.getReference()));
				case DROP: return itemManager.getImage(Integer.parseInt(goal.getReference()));
				case QUEST: return skillIconManager.getSkillImage(Skill.MAGIC);
				default: return null;
			}
		}
		catch (RuntimeException ignored) { return null; }
	}

	BufferedImage badgeIcon(String key)
	{
		return badgeIcon(key, false);
	}

	BufferedImage badgeIcon(String key, boolean small)
	{
		String cacheKey = key + (small ? ":small" : ":large");
		if (badgeIcons.containsKey(cacheKey)) return badgeIcons.get(cacheKey);
		try
		{
			BufferedImage image;
			switch (key)
			{
				case "leader": image = skill(Skill.PRAYER, small); break;
				case "quest1": image = skill(Skill.MAGIC, small); break;
				case "quest5": image = skill(Skill.RUNECRAFT, small); break;
				case "quest20": image = skill(Skill.PRAYER, small); break;
				case "skill1": image = skill(Skill.ATTACK, small); break;
				case "skill5": image = skill(Skill.STRENGTH, small); break;
				case "skill20": image = skill(Skill.CONSTRUCTION, small); break;
				case "drop1": image = skill(Skill.MINING, small); break;
				case "drop5": image = skill(Skill.THIEVING, small); break;
				case "drop20": image = skill(Skill.SLAYER, small); break;
				case "goal5": image = skill(Skill.AGILITY, small); break;
				case "goal25": image = skill(Skill.FIREMAKING, small); break;
				case "group1": image = skill(Skill.SLAYER, small); break;
				case "group5": image = skill(Skill.ATTACK, small); break;
				case "carrier1": image = skill(Skill.AGILITY, small); break;
				case "carrier5": image = skill(Skill.HUNTER, small); break;
				case "points100": image = skill(Skill.RUNECRAFT, small); break;
				case "points500": image = skill(Skill.PRAYER, small); break;
				default: image = null;
			}
			if (image == null) image = fallbackBadgeIcon(key, small);
			if (image != null) badgeIcons.put(cacheKey, image);
			return image;
		}
		catch (RuntimeException ignored) { return fallbackBadgeIcon(key, small); }
	}

	private BufferedImage skill(Skill skill, boolean small)
	{
		return skillIconManager.getSkillImage(skill, small);
	}

	private BufferedImage fallbackBadgeIcon(String key, boolean small)
	{
		if (key.startsWith("drop")) return skill(Skill.MINING, small);
		if (key.startsWith("group")) return skill(Skill.SLAYER, small);
		if (key.startsWith("carrier")) return skill(Skill.AGILITY, small);
		if (key.startsWith("quest")) return skill(Skill.MAGIC, small);
		return skill(Skill.ATTACK, small);
	}

	private BufferedImage itemImage(String name)
	{
		for (Choice choice : findItems(name))
		{
			if (choice.getName().equalsIgnoreCase(name)) return itemManager.getImage(Integer.parseInt(choice.getReference()));
		}
		return null;
	}

	private static int score(String value, String query)
	{
		if (query.isEmpty()) return 0;
		if (value.equals(query)) return 0;
		if (value.startsWith(query)) return 1;
		if (value.contains(query)) return 2 + value.indexOf(query);
		return 100 + levenshtein(value, query);
	}

	private static int levenshtein(String a, String b)
	{
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) costs[j] = j;
		for (int i = 1; i <= a.length(); i++)
		{
			int previous = costs[0]; costs[0] = i;
			for (int j = 1; j <= b.length(); j++)
			{
				int old = costs[j];
				costs[j] = Math.min(Math.min(costs[j] + 1, costs[j - 1] + 1), previous + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
				previous = old;
			}
		}
		return costs[b.length()];
	}

	private static String title(String value)
	{
		String lower = value.toLowerCase(Locale.ENGLISH).replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
