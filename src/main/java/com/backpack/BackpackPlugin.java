package com.backpack;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Backpack",
	description = "See who is carrying your team toward its weekly goals",
	tags = {"team", "goals", "xp", "leaderboard"}
)
public class BackpackPlugin extends Plugin implements BackpackPanel.Actions
{
	private static final String WEEK_START = "weekStart";
	private static final String XP_GAINED = "xpGained";
	private static final String DROPS_RECEIVED = "dropsReceived";
	private static final String TEAM_NAME = "teamName";
	private static final String TEAM_CODE = "teamCode";
	private static final String TEAM_LEADER = "teamLeader";
	private static final String PERSONAL_GOAL = "personalGoal";
	private static final String GOAL_COMPLETED = "goalCompleted";
	private static final String QUEST_POINTS_GAINED = "questPointsGained";
	private static final String DROP_POINTS = "dropPoints";
	private static final String GOAL_BONUS_POINTS = "goalBonusPoints";
	private static final String GOAL_PROGRESS = "goalProgress";
	private static final String NEXT_GOAL = "nextGoal";
	private static final String GROUP_CHALLENGE = "groupChallenge";
	private static final String GROUP_WINS = "groupWins";
	private static final String BADGE_PROGRESS = "badgeProgress";
	private static final String SERVER_TOKEN = "serverToken";
	private static final String SERVER_MEMBER_ID = "serverMemberId";
	private static final String SERVER_SEQUENCE = "serverSequence";
	private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Pattern COLLECTION_LOG_ITEM_PATTERN = Pattern.compile("New item added to your collection log: (.*)");

	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ConfigManager configManager;
	@Inject private BackpackConfig config;
	@Inject private DiscordWebhookClient discord;
	@Inject private ItemManager itemManager;
	@Inject private DrawManager drawManager;
	@Inject private ScheduledExecutorService executor;
	@Inject private GoalCatalog goalCatalog;
	@Inject private TaskGenerator taskGenerator;
	@Inject private ClientThread clientThread;
	@Inject private BackpackApiClient api;

	private BackpackPanel panel;
	private NavigationButton navigationButton;
	private BackpackState state;
	private long lastOverallXp = -1;
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> skillXp = new EnumMap<>(Skill.class);
	private Team team;
	private int initializingTicks;
	private Goal personalGoal;
	private boolean goalCompleted;
	private int lastQuestPoints = -1;
	private long goalProgress;
	private Goal nextGoal;
	private GroupChallenge groupChallenge;
	private int groupWins;
	private BadgeProgress badgeProgress;
	private String serverToken;
	private long serverSequence;

	@Override
	protected void startUp()
	{
		loadProfileState();
		panel = new BackpackPanel(this, goalCatalog);
		navigationButton = NavigationButton.builder()
			.tooltip("Backpack")
			.priority(7)
			.icon(createIcon())
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		saveState();
		clientToolbar.removeNavigation(navigationButton);
		lastOverallXp = -1;
		panel = null;
		navigationButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loadProfileState();
			resetRuntimeTracking();
			refreshPanel();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastOverallXp = -1;
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		loadProfileState();
		resetRuntimeTracking();
		refreshPanel();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Integer previousLevel = levels.put(event.getSkill(), event.getLevel());
		Integer previousSkillXp = skillXp.put(event.getSkill(), event.getXp());
		long overall = client.getOverallExperience();
		if (initializingTicks > 0)
		{
			return;
		}
		if (previousLevel != null && event.getLevel() > previousLevel && config.discordEnabled()
			&& shouldAnnounceLevel(config.levelNotifications(), event.getLevel()))
		{
			discord.send(config.discordWebhookUrl(), displayName() + " reached level " + event.getLevel() + " " + pretty(event.getSkill()) + "! 🎉");
		}
		if (!goalCompleted && personalGoal != null && personalGoal.getType() == Goal.Type.SKILL_XP
			&& event.getSkill().name().equals(personalGoal.getReference()) && previousSkillXp != null && event.getXp() > previousSkillXp)
		{
			goalProgress += event.getXp() - previousSkillXp;
			configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_PROGRESS, goalProgress);
		}
		if (groupChallenge != null && !groupChallenge.isCooldown() && groupChallenge.getGoal().getType() == Goal.Type.SKILL_XP
			&& event.getSkill().name().equals(groupChallenge.getGoal().getReference()) && previousSkillXp != null && event.getXp() > previousSkillXp)
		{
			groupChallenge = groupChallenge.addProgress(event.getXp() - previousSkillXp);
			checkGroupChallenge();
			saveGroupChallenge();
		}
		if (lastOverallXp >= 0 && overall > lastOverallXp)
		{
			ensureCurrentWeek();
			state = state.addXp(overall - lastOverallXp);
			syncEvent("XP", overall - lastOverallXp);
			checkXpGoal();
			saveState();
			refreshPanel();
		}
		lastOverallXp = overall;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.discordEnabled() || !config.discordCollectionLog() || event.getType() != ChatMessageType.GAMEMESSAGE) return;
		Matcher matcher = COLLECTION_LOG_ITEM_PATTERN.matcher(Text.removeTags(event.getMessage()));
		if (!matcher.find()) return;
		String message = displayName() + " added " + matcher.group(1) + " to their Collection Log! 📚";
		if (config.discordDropScreenshot())
		{
			drawManager.requestNextFrameListener(image -> executor.execute(() ->
				discord.send(config.discordWebhookUrl(), message, ImageUtil.bufferedImageFromImage(image))));
		}
		else
		{
			discord.send(config.discordWebhookUrl(), message);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (state != null && !state.isCurrentWeek(Instant.now()))
		{
			if (CarryPoints.total(state) > 0) badgeProgress = badgeProgress.carriedWeek();
			state = BackpackState.emptyForCurrentWeek(Instant.now());
			saveBadgeProgress(); saveState();
		}
		if (initializingTicks > 0 && --initializingTicks == 0)
		{
			lastOverallXp = client.getOverallExperience();
			lastQuestPoints = client.getVarpValue(VarPlayerID.QP);
			for (Skill skill : Skill.values())
			{
				if (skill != Skill.OVERALL)
				{
					levels.put(skill, client.getRealSkillLevel(skill));
					skillXp.put(skill, client.getSkillExperience(skill));
				}
			}
			refreshPanel();
		}
		if (initializingTicks == 0 && team != null)
		{
			if (groupChallenge == null || groupChallenge.cooldownFinished(Instant.now()) || groupChallenge.activeExpired(Instant.now())) createGroupChallenge();
			refreshPanel();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.QP || initializingTicks > 0) return;
		int current = event.getValue();
		if (lastQuestPoints >= 0 && current > lastQuestPoints)
		{
			state = state.addQuestPoints(current - lastQuestPoints);
			syncEvent("QUEST_POINTS", current - lastQuestPoints);
			if (!goalCompleted && personalGoal != null && personalGoal.getType() == Goal.Type.QUEST
				&& isSelectedQuestComplete())
			{
				completeGoal(CarryPoints.forQuestPoints((int) personalGoal.getTarget()));
			}
			if (groupChallenge != null && !groupChallenge.isCooldown() && groupChallenge.getGoal().getType() == Goal.Type.QUEST
				&& isQuestComplete(groupChallenge.getGoal()))
			{
				groupChallenge = groupChallenge.addProgress(groupChallenge.getGoal().getTarget());
				checkGroupChallenge(); saveGroupChallenge();
			}
			saveState();
			refreshPanel();
		}
		lastQuestPoints = current;
	}

	@Subscribe
	public void onServerNpcLoot(ServerNpcLoot event)
	{
		ItemStack best = null;
		long bestValue = 0;
		for (ItemStack item : event.getItems())
		{
			long value = itemManager.getItemPrice(item.getId()) * item.getQuantity();
			if (value > bestValue)
			{
				best = item;
				bestValue = value;
			}
		}
		if (best == null) return;
		if (groupChallenge != null && !groupChallenge.isCooldown() && groupChallenge.getGoal().getType() == Goal.Type.DROP
			&& Integer.toString(best.getId()).equals(groupChallenge.getGoal().getReference()))
		{
			groupChallenge = groupChallenge.addProgress(best.getQuantity());
			checkGroupChallenge(); saveGroupChallenge(); refreshPanel();
		}
		if (bestValue < config.discordDropValue()) return;

		ensureCurrentWeek();
		state = state.addDrop(bestValue);
		syncEvent("DROP", bestValue);
		String droppedItemName = itemManager.getItemComposition(best.getId()).getMembersName();
		if (!goalCompleted && personalGoal != null && personalGoal.getType() == Goal.Type.DROP
			&& Integer.toString(best.getId()).equals(personalGoal.getReference()) && best.getQuantity() >= personalGoal.getTarget())
		{
			completeGoal(CarryPoints.forDrop(bestValue));
		}
		saveState();
		refreshPanel();
		if (config.discordEnabled())
		{
			String itemName = itemManager.getItemComposition(best.getId()).getMembersName();
			String npcName = event.getComposition().getName();
			String message = displayName() + " got " + best.getQuantity() + " × " + itemName + " from " + npcName + " (" + bestValue + " gp)! 💰";
			if (config.discordDropScreenshot())
			{
				drawManager.requestNextFrameListener(image -> executor.execute(() ->
					discord.send(config.discordWebhookUrl(), message, ImageUtil.bufferedImageFromImage(image))));
			}
			else
			{
				discord.send(config.discordWebhookUrl(), message);
			}
		}
	}

	private void ensureCurrentWeek()
	{
		if (!state.isCurrentWeek(Instant.now()))
		{
			state = BackpackState.emptyForCurrentWeek(Instant.now());
		}
	}

	private void loadProfileState()
	{
		state = loadState();
		team = loadTeam();
		personalGoal = loadGoal();
		goalCompleted = loadGoalCompleted();
		goalProgress = loadGoalProgress();
		nextGoal = loadNextGoal();
		loadGroupChallenge();
		badgeProgress = loadBadgeProgress();
		serverToken = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, SERVER_TOKEN);
		Long sequence = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, SERVER_SEQUENCE, Long.class);
		serverSequence = sequence == null ? 0 : sequence;
	}

	private void resetRuntimeTracking()
	{
		lastOverallXp = -1;
		lastQuestPoints = -1;
		initializingTicks = client.getGameState() == GameState.LOGGED_IN ? 2 : 0;
		levels.clear();
		skillXp.clear();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			for (Skill skill : Skill.values())
			{
				if (skill != Skill.OVERALL)
				{
					levels.put(skill, client.getRealSkillLevel(skill));
					skillXp.put(skill, client.getSkillExperience(skill));
				}
			}
		}
	}

	private BackpackState loadState()
	{
		Long week = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, WEEK_START, Long.class);
		Long xp = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, XP_GAINED, Long.class);
		Integer drops = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, DROPS_RECEIVED, Integer.class);
		Integer questPoints = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, QUEST_POINTS_GAINED, Integer.class);
		Long dropPoints = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, DROP_POINTS, Long.class);
		Long goalBonus = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, GOAL_BONUS_POINTS, Long.class);
		BackpackState loaded = new BackpackState(week == null ? 0 : week, xp == null ? 0 : xp, 0, drops == null ? 0 : drops,
			questPoints == null ? 0 : questPoints, dropPoints == null ? 0 : dropPoints, goalBonus == null ? 0 : goalBonus);
		return loaded.isCurrentWeek(Instant.now()) ? loaded : BackpackState.emptyForCurrentWeek(Instant.now());
	}

	private void saveState()
	{
		if (state == null) return;
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, WEEK_START, state.getWeekStartEpoch());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, XP_GAINED, state.getXpGained());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, DROPS_RECEIVED, state.getDropsReceived());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, QUEST_POINTS_GAINED, state.getQuestPointsGained());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, DROP_POINTS, state.getDropPoints());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_BONUS_POINTS, state.getGoalBonusPoints());
		if (badgeProgress != null)
		{
			badgeProgress = badgeProgress.recordPoints(CarryPoints.total(state));
			saveBadgeProgress();
		}
	}

	private String displayName()
	{
		return client.getLocalPlayer() == null ? "A teammate" : client.getLocalPlayer().getName();
	}

	private static String pretty(Skill skill)
	{
		String lower = skill.name().toLowerCase().replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	static boolean shouldAnnounceLevel(BackpackConfig.LevelNotifications mode, int level)
	{
		return mode == BackpackConfig.LevelNotifications.EVERY_LEVEL
			|| level == 99 || (level >= 20 && level <= 90 && level % 10 == 0);
	}

	private void refreshPanel()
	{
		if (panel == null || state == null) return;
		String username = client.getLocalPlayer() == null ? "You" : client.getLocalPlayer().getName();
		int combatLevel = client.getLocalPlayer() == null ? 0 : client.getLocalPlayer().getCombatLevel();
		SwingUtilities.invokeLater(() -> panel.update(team, username, state, config.weeklyXpGoal(), personalGoal, nextGoal, goalProgress,
			goalCompleted, groupChallenge, groupWins, combatLevel, badgeProgress));
	}

	@Override
	public void createTeam(String name)
	{
		if (config.serverEnabled())
		{
			api.create(config.serverUrl(), name, displayName(), this::applyServerSession, error -> panel.showError(error));
			return;
		}
		team = new Team(name, newInviteCode(), 1, true);
		saveTeam();
		clientThread.invokeLater(this::createGroupChallenge);
		refreshPanel();
	}

	@Override
	public void joinTeam(String code)
	{
		String normalized = code.toUpperCase().replaceAll("[^A-Z0-9]", "");
		if (normalized.length() != 8) return;
		if (config.serverEnabled())
		{
			api.join(config.serverUrl(), normalized, displayName(), this::applyServerSession, error -> panel.showError(error));
			return;
		}
		team = new Team("Joined team", normalized, 1, false);
		saveTeam();
		clientThread.invokeLater(this::createGroupChallenge);
		refreshPanel();
	}

	private void applyServerSession(BackpackApiClient.Session session)
	{
		if (session == null || session.team == null || session.token == null) { panel.showError("Invalid server session"); return; }
		BackpackApiClient.RemoteMember me = session.team.members == null ? null : session.team.members.get(session.memberId);
		team = new Team(session.team.name, session.team.code, session.team.members == null ? 1 : session.team.members.size(), me != null && "LEADER".equals(me.role));
		serverToken = session.token; serverSequence = 0;
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, SERVER_TOKEN, serverToken);
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, SERVER_MEMBER_ID, session.memberId);
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, SERVER_SEQUENCE, 0L);
		saveTeam(); refreshPanel(); clientThread.invokeLater(this::createGroupChallenge);
	}

	private void syncEvent(String type, long amount)
	{
		if (!config.serverEnabled() || serverToken == null || amount <= 0) return;
		serverSequence++;
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, SERVER_SEQUENCE, serverSequence);
		api.event(config.serverUrl(), serverToken, type, amount, serverSequence);
	}

	@Override
	public void leaveTeam()
	{
		team = null;
		groupChallenge = null;
		serverToken = null;
		serverSequence = 0;
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, TEAM_NAME);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, TEAM_CODE);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, TEAM_LEADER);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, GROUP_CHALLENGE);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, SERVER_TOKEN);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, SERVER_MEMBER_ID);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, SERVER_SEQUENCE);
		refreshPanel();
	}

	@Override
	public void resetWeek()
	{
		state = state.reset();
		goalCompleted = false;
		goalProgress = 0;
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_COMPLETED, false);
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_PROGRESS, 0);
		saveState();
		refreshPanel();
	}

	@Override
	public void setGoal(Goal goal)
	{
		nextGoal = null;
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL);
		startGoal(goal);
	}

	private void startGoal(Goal goal)
	{
		personalGoal = goal;
		goalCompleted = false;
		goalProgress = 0;
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, PERSONAL_GOAL, goal.serialize());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_COMPLETED, false);
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_PROGRESS, 0);
		refreshPanel();
	}

	@Override
	public void generateTask()
	{
		clientThread.invokeLater(() -> {
			TaskPlan plan = taskGenerator.generate();
			nextGoal = plan.getNext();
			if (nextGoal == null) configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL);
			else configManager.setRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL, nextGoal.serialize());
			startGoal(plan.getCurrent());
		});
	}

	@Override
	public void clearGoal()
	{
		personalGoal = null;
		nextGoal = null;
		goalCompleted = false;
		goalProgress = 0;
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, PERSONAL_GOAL);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, GOAL_COMPLETED);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, GOAL_PROGRESS);
		configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL);
		refreshPanel();
	}

	private Goal loadGoal()
	{
		return Goal.deserialize(configManager.getRSProfileConfiguration(BackpackConfig.GROUP, PERSONAL_GOAL));
	}

	private boolean loadGoalCompleted()
	{
		Boolean value = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, GOAL_COMPLETED, Boolean.class);
		return value != null && value;
	}

	private long loadGoalProgress()
	{
		Long value = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, GOAL_PROGRESS, Long.class);
		return value == null ? 0 : value;
	}

	private Goal loadNextGoal()
	{
		return Goal.deserialize(configManager.getRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL));
	}

	private boolean isSelectedQuestComplete()
	{
		return isQuestComplete(personalGoal);
	}

	private boolean isQuestComplete(Goal goal)
	{
		try { return net.runelite.api.Quest.valueOf(goal.getReference()).getState(client) == net.runelite.api.QuestState.FINISHED; }
		catch (RuntimeException ignored) { return false; }
	}

	private void createGroupChallenge()
	{
		if (team == null || client.getGameState() != GameState.LOGGED_IN) return;
		Goal goal = taskGenerator.generate().getCurrent();
		groupChallenge = GroupChallenge.active(goal, Instant.now());
		saveGroupChallenge(); refreshPanel();
	}

	private void checkGroupChallenge()
	{
		if (groupChallenge == null || groupChallenge.isCooldown() || groupChallenge.getProgress() < groupChallenge.getGoal().getTarget()) return;
		groupChallenge = groupChallenge.wonBy(displayName(), Instant.now());
		groupWins++;
		badgeProgress = badgeProgress.groupWin();
		state = state.addGoalBonus(30);
		saveState();
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GROUP_WINS, groupWins);
		saveBadgeProgress();
	}

	private void saveGroupChallenge()
	{
		if (groupChallenge != null) configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GROUP_CHALLENGE, groupChallenge.serialize());
	}

	private void loadGroupChallenge()
	{
		groupChallenge = GroupChallenge.deserialize(configManager.getRSProfileConfiguration(BackpackConfig.GROUP, GROUP_CHALLENGE));
		Integer wins = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, GROUP_WINS, Integer.class);
		groupWins = wins == null ? 0 : wins;
	}

	private void checkXpGoal()
	{
		if (!goalCompleted && personalGoal != null && personalGoal.getType() == Goal.Type.SKILL_XP
			&& goalProgress >= personalGoal.getTarget())
		{
			completeGoal(CarryPoints.forXp(personalGoal.getTarget()));
		}
	}

	private void completeGoal(long basePoints)
	{
		Goal.Type completedType = personalGoal.getType();
		state = state.addGoalBonus(CarryPoints.goalBonus(basePoints));
		badgeProgress = badgeProgress.complete(completedType).recordPoints(CarryPoints.total(state));
		saveBadgeProgress();
		if (nextGoal != null)
		{
			Goal followUp = nextGoal;
			nextGoal = null;
			configManager.unsetRSProfileConfiguration(BackpackConfig.GROUP, NEXT_GOAL);
			startGoal(followUp);
		}
		else
		{
			goalCompleted = true;
			configManager.setRSProfileConfiguration(BackpackConfig.GROUP, GOAL_COMPLETED, true);
		}
	}

	private BadgeProgress loadBadgeProgress()
	{
		return BadgeProgress.deserialize(configManager.getRSProfileConfiguration(BackpackConfig.GROUP, BADGE_PROGRESS));
	}

	private void saveBadgeProgress()
	{
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, BADGE_PROGRESS, badgeProgress.serialize());
	}

	private Team loadTeam()
	{
		String name = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, TEAM_NAME);
		String code = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, TEAM_CODE);
		Boolean leader = configManager.getRSProfileConfiguration(BackpackConfig.GROUP, TEAM_LEADER, Boolean.class);
		return name == null || code == null ? null : new Team(name, code, 1, leader != null && leader);
	}

	private void saveTeam()
	{
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, TEAM_NAME, team.getName());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, TEAM_CODE, team.getCode());
		configManager.setRSProfileConfiguration(BackpackConfig.GROUP, TEAM_LEADER, team.isLeader());
	}

	private static String newInviteCode()
	{
		StringBuilder value = new StringBuilder(8);
		for (int i = 0; i < 8; i++) value.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
		return value.toString();
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(126, 77, 42));
		g.fillRoundRect(3, 4, 10, 11, 3, 3);
		g.setColor(new Color(255, 184, 77));
		g.drawArc(5, 0, 6, 8, 0, 180);
		g.drawLine(3, 8, 13, 8);
		g.dispose();
		return image;
	}

	@Provides
	BackpackConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(BackpackConfig.class);
	}
}
