package com.backpack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class Goal
{
	enum Type
	{
		SKILL_XP("⚔", "Skill XP"), DROP("💎", "Drop"), QUEST("📜", "Quest");

		private final String icon;
		private final String label;
		Type(String icon, String label) { this.icon = icon; this.label = label; }
		String getIcon() { return icon; }
		@Override public String toString() { return label; }
	}

	private final Type type;
	private final String detail;
	private final long target;
	private final String reference;

	Goal(Type type, String detail, long target)
	{
		this(type, detail, target, detail);
	}

	Goal(Type type, String detail, long target, String reference)
	{
		this.type = type;
		this.detail = detail;
		this.target = Math.max(1, target);
		this.reference = reference;
	}

	Type getType() { return type; }
	String getDetail() { return detail; }
	long getTarget() { return target; }
	String getReference() { return reference; }
	String getIcon() { return type.getIcon(); }

	String serialize()
	{
		return type.name() + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(detail.getBytes(StandardCharsets.UTF_8)) + ":" + target
			+ ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(reference.getBytes(StandardCharsets.UTF_8));
	}

	static Goal deserialize(String value)
	{
		if (value == null) return null;
		try
		{
			String[] parts = value.split(":", 4);
			String detail = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			String reference = parts.length == 4 ? new String(Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8) : detail;
			return new Goal(Type.valueOf(parts[0]), detail, Long.parseLong(parts[2]), reference);
		}
		catch (RuntimeException ignored)
		{
			return null;
		}
	}
}
