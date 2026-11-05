package com.backpack;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Scrollable;
import javax.swing.border.BevelBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

final class BackpackPanel extends PluginPanel
{
	interface Actions
	{
		void createTeam(String name);
		void joinTeam(String code);
		void leaveTeam();
		void resetWeek();
		void setGoal(Goal goal);
		void generateTask();
		void clearGoal();
	}

	private static final Color GOLD = new Color(201, 169, 110);
	private static final Color OSRS_ORANGE = new Color(217, 167, 90);
	private static final Color MAIN = new Color(30, 30, 30);
	private static final Color INSET = new Color(37, 37, 37);
	private static final Color BORDER_LIGHT = new Color(58, 58, 58);
	private static final Color BORDER_DARK = new Color(10, 10, 10);
	private static final Color MUTED = new Color(136, 136, 136);
	private static final Font UI_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	private static final Font UI_FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private final Actions actions;
	private final GoalCatalog catalog;
	private final Set<String> expandedPlayers = new HashSet<>();
	private final JLabel teamName = new JLabel("BACKPACK", SwingConstants.CENTER);
	private final JLabel inviteCode = new JLabel("No team yet", SwingConstants.CENTER);
	private final JLabel carrier = new JLabel("Create or join a team to begin", SwingConstants.CENTER);
	private final JPanel raceContainer = new JPanel();
	private final JPanel goalContainer = new JPanel(new BorderLayout());
	private final JPanel groupGoalContainer = new JPanel(new BorderLayout());
	private final JPanel badgeList = new JPanel();
	private final JButton copyButton = new JButton("Copy invite");
	private final JButton leaveButton = new JButton("Leave");
	private final JButton setGoalButton = new JButton("Set player goal");
	private JPanel createJoinRow;
	private String currentInviteCode;
	private String renderedBadgeProgress;
	private BadgeProgress currentBadgeProgress = BadgeProgress.empty();
	private final JButton badgeFilterButton = new JButton("EARNED ONLY");
	private boolean showLocked = true;

	BackpackPanel(Actions actions, GoalCatalog catalog)
	{
		this.actions = actions;
		this.catalog = catalog;
		setLayout(new BorderLayout());
		setBackground(MAIN);
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(MAIN);
		content.setBorder(BorderFactory.createEmptyBorder(14, 8, 12, 8));

		teamName.setFont(UI_FONT_BOLD.deriveFont(18f));
		teamName.setForeground(GOLD);
		teamName.setAlignmentX(CENTER_ALIGNMENT);
		inviteCode.setForeground(MUTED);
		inviteCode.setFont(UI_FONT.deriveFont(12f));
		inviteCode.setAlignmentX(CENTER_ALIGNMENT);
		carrier.setForeground(Color.WHITE);
		carrier.setFont(UI_FONT_BOLD.deriveFont(12f));
		carrier.setAlignmentX(CENTER_ALIGNMENT);
		content.add(teamName);
		content.add(inviteCode);
		content.add(Box.createRigidArea(new Dimension(0, 8)));
		content.add(carrier);
		content.add(Box.createRigidArea(new Dimension(0, 16)));
		content.add(sectionTitle("WEEKLY XP RACE"));
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		raceContainer.setLayout(new BoxLayout(raceContainer, BoxLayout.Y_AXIS));
		raceContainer.setBackground(MAIN);
		raceContainer.setBorder(BorderFactory.createEmptyBorder());
		content.add(raceContainer);
		content.add(Box.createRigidArea(new Dimension(0, 16)));
		content.add(sectionTitle("ASSIGNED GOAL"));
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		goalContainer.setBackground(INSET);
		goalContainer.setBorder(cardBorder(10));
		content.add(goalContainer);
		content.add(Box.createRigidArea(new Dimension(0, 16)));
		content.add(sectionTitle("GROUP CHALLENGE"));
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		groupGoalContainer.setBackground(INSET);
		groupGoalContainer.setBorder(cardBorder(10));
		content.add(groupGoalContainer);
		content.add(Box.createRigidArea(new Dimension(0, 7)));
		JButton newTaskButton = new JButton("New task");
		styleButton(setGoalButton, false);
		styleButton(newTaskButton, false);
		setGoalButton.setAlignmentX(CENTER_ALIGNMENT);
		content.add(buttonRow(setGoalButton, newTaskButton));
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		JButton createButton = new JButton("Create team");
		JButton joinButton = new JButton("Join team");
		styleButton(createButton, false);
		styleButton(joinButton, false);
		styleButton(copyButton, false);
		styleButton(leaveButton, false);
		createJoinRow = buttonRow(createButton, joinButton);
		content.add(createJoinRow);
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		content.add(buttonRow(copyButton, leaveButton));
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		JButton resetButton = new JButton("Reset this week's progress");
		styleButton(resetButton, true);
		resetButton.setAlignmentX(CENTER_ALIGNMENT);
		resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		content.add(resetButton);
		content.add(Box.createRigidArea(new Dimension(0, 12)));
		JLabel note = new JLabel("<html><center>Team is saved locally.<br>Cross-client sync needs the Backpack server.</center></html>", SwingConstants.CENTER);
		note.setForeground(MUTED);
		note.setFont(UI_FONT.deriveFont(11f));
		note.setAlignmentX(CENTER_ALIGNMENT);
		content.add(note);
		JPanel dashboardPage = new WidthTrackingPanel(new BorderLayout());
		dashboardPage.setBackground(MAIN);
		dashboardPage.add(content, BorderLayout.NORTH);
		JScrollPane dashboardScroll = new JScrollPane(dashboardPage, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dashboardScroll.setBorder(null);
		dashboardScroll.getViewport().setBackground(MAIN);
		dashboardScroll.getVerticalScrollBar().setUnitIncrement(16);
		CardLayout pageLayout = new CardLayout();
		JPanel pages = new JPanel(pageLayout);
		pages.setBackground(MAIN);
		pages.add(dashboardScroll, "dashboard");
		pages.add(buildInfoPage(), "info");
		JButton dashboardTab = new JButton("DASHBOARD");
		JButton infoTab = new JButton("BADGES & INFO");
		styleTab(dashboardTab, true);
		styleTab(infoTab, false);
		JPanel tabs = buttonRow(dashboardTab, infoTab);
		tabs.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));
		add(tabs, BorderLayout.NORTH);
		add(pages, BorderLayout.CENTER);
		dashboardTab.addActionListener(event -> {
			pageLayout.show(pages, "dashboard"); styleTab(dashboardTab, true); styleTab(infoTab, false);
		});
		infoTab.addActionListener(event -> {
			pageLayout.show(pages, "info");
			renderedBadgeProgress = null;
			refreshBadges(currentBadgeProgress);
			styleTab(dashboardTab, false); styleTab(infoTab, true);
			pages.revalidate(); pages.repaint();
		});

		createButton.addActionListener(event -> promptCreate());
		joinButton.addActionListener(event -> promptJoin());
		copyButton.addActionListener(event -> copyInvite());
		leaveButton.addActionListener(event -> actions.leaveTeam());
		resetButton.addActionListener(event -> {
			if (JOptionPane.showConfirmDialog(this, "Reset your Backpack progress for this week?", "Reset week", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{
				actions.resetWeek();
			}
		});
		setGoalButton.addActionListener(event -> promptGoal());
		newTaskButton.addActionListener(event -> actions.generateTask());
	}

	void update(Team team, String username, BackpackState state, int goal, Goal personalGoal, Goal nextGoal, long goalProgress,
		boolean goalCompleted, GroupChallenge groupChallenge, int groupWins, int combatLevel, BadgeProgress badges)
	{
		refreshBadges(badges);
		boolean hasTeam = team != null;
		teamName.setText(hasTeam ? team.getName() : "BACKPACK");
		currentInviteCode = hasTeam ? team.getCode() : null;
		inviteCode.setText(hasTeam ? "Invite: " + currentInviteCode : "No team yet");
		String role = (hasTeam && team.isLeader() ? " 👑" : "") + (groupWins > 0 ? " 🏆" : "")
			+ (state.getXpGained() > 0 ? " 🎒" : "");
		String carrierText = state.getXpGained() > 0 ? safeName(username) + role + " is carrying the pack 🎒" : (hasTeam && team.isLeader() ? "You created this team · Leader 👑" : "The race starts with your first XP");
		carrier.setText("<html><div style='width:190px;text-align:center'>" + escapeHtml(carrierText) + "</div></html>");
		copyButton.setEnabled(hasTeam);
		leaveButton.setEnabled(hasTeam);
		setGoalButton.setVisible(hasTeam && team.isLeader());
		createJoinRow.setVisible(!hasTeam);
		raceContainer.removeAll();
		long teamPointGoal = Math.max(1, CarryPoints.forXp(goal));
		java.util.List<Image> playerBadges = new java.util.ArrayList<>();
		if (hasTeam && team.isLeader()) playerBadges.add(catalog.badgeIcon("leader", true));
		if (groupWins > 0) playerBadges.add(catalog.badgeIcon("group1", true));
		if (state.getXpGained() > 0) playerBadges.add(catalog.badgeIcon("carrier1", true));
		if (goalCompleted && personalGoal != null) playerBadges.add(catalog.icon(personalGoal));
		String playerKey = safeName(username);
		raceContainer.add(new RaceLane(playerKey, playerKey, combatLevel, state.getXpGained(), teamPointGoal, CarryPoints.total(state), true, playerBadges,
			state, personalGoal, goalCompleted, expandedPlayers.contains(playerKey)));
		if (hasTeam && team.getMemberCount() > 1)
		{
			for (int i = 1; i < team.getMemberCount(); i++)
			{
				raceContainer.add(Box.createRigidArea(new Dimension(0, 5)));
				String waiting = "Waiting for member " + (i + 1);
				raceContainer.add(new RaceLane(waiting, waiting, 0, 0, teamPointGoal, 0, false, java.util.Collections.emptyList(), null, null, false, expandedPlayers.contains(waiting)));
			}
		}
		raceContainer.revalidate();
		raceContainer.repaint();
		goalContainer.removeAll();
		if (personalGoal == null)
		{
			JLabel empty = new JLabel("No goal assigned", SwingConstants.CENTER);
			empty.setForeground(MUTED);
			empty.setFont(UI_FONT);
			goalContainer.add(empty);
		}
		else
		{
			Image goalImage = catalog.icon(personalGoal);
			JLabel icon = new JLabel(goalImage == null ? null : new ImageIcon(goalImage.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
			String status = goalCompleted ? "<font color='#7bd88f'>✓ Complete · bonus awarded</font>" : "<font color='#aaaaaa'>Target: " + format(personalGoal.getTarget()) + "</font>";
			if (nextGoal != null) status += "<br><font color='#ffb84d'>Next: " + escapeHtml(nextGoal.getDetail()) + "</font>";
			JLabel text = new JLabel("<html><div style='width:145px'><b>" + escapeHtml(personalGoal.getDetail()) + "</b><br>" + status + "</div></html>");
			text.setFont(UI_FONT);
			JButton clear = new JButton("×");
			clear.setToolTipText("Clear goal");
			clear.setForeground(MUTED);
			clear.setBackground(INSET);
			clear.setFocusPainted(false);
			clear.setBorder(new RoundedBorder(BORDER_LIGHT, 4, new Insets(1, 5, 1, 5)));
			clear.addActionListener(event -> actions.clearGoal());
			goalContainer.add(icon, BorderLayout.WEST);
			if (personalGoal.getType() == Goal.Type.SKILL_XP)
			{
				JPanel details = new JPanel(new BorderLayout(0, 4));
				details.setOpaque(false);
				details.add(text, BorderLayout.CENTER);
				details.add(xpProgressBar(goalCompleted ? personalGoal.getTarget() : goalProgress, personalGoal.getTarget()), BorderLayout.SOUTH);
				goalContainer.add(details, BorderLayout.CENTER);
			}
			else
			{
				goalContainer.add(text, BorderLayout.CENTER);
			}
			goalContainer.add(clear, BorderLayout.EAST);
		}
		goalContainer.revalidate();
		goalContainer.repaint();
		groupGoalContainer.removeAll();
		if (!hasTeam || groupChallenge == null)
		{
			JLabel waiting = new JLabel(hasTeam ? "Waiting for the next group challenge" : "Join a team to start a challenge", SwingConstants.CENTER);
			waiting.setFont(UI_FONT);
			waiting.setForeground(MUTED); groupGoalContainer.add(waiting);
		}
		else if (groupChallenge.isCooldown())
		{
			long seconds = groupChallenge.cooldownRemainingSeconds(java.time.Instant.now());
			JLabel cooldown = new JLabel("<html><center>🏆 " + escapeHtml(groupChallenge.getWinner()) + " finished first!<br>Next challenge in "
				+ String.format("%02d:%02d", seconds / 60, seconds % 60) + "</center></html>", SwingConstants.CENTER);
			cooldown.setFont(UI_FONT);
			cooldown.setForeground(GOLD); groupGoalContainer.add(cooldown);
		}
		else
		{
			Goal challengeGoal = groupChallenge.getGoal();
			Image image = catalog.icon(challengeGoal);
			JLabel icon = new JLabel(image == null ? null : new ImageIcon(image.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
			long target = challengeGoal.getTarget();
			long progress = Math.min(target, groupChallenge.getProgress());
			long seconds = groupChallenge.activeRemainingSeconds(java.time.Instant.now());
			JLabel text = new JLabel("<html><div style='width:150px'><b>First to finish: " + escapeHtml(challengeGoal.getDetail()) + "</b><br><font color='#aaaaaa'>"
				+ format(progress) + " / " + format(target) + " · Winner earns 30 pts</font><br><font color='#ffb84d'>Rotates in "
				+ formatDuration(seconds) + "</font></div></html>");
			text.setFont(UI_FONT);
			groupGoalContainer.add(icon, BorderLayout.WEST);
			if (challengeGoal.getType() == Goal.Type.SKILL_XP)
			{
				JPanel details = new JPanel(new BorderLayout(0, 4));
				details.setOpaque(false);
				details.add(text, BorderLayout.CENTER);
				details.add(xpProgressBar(progress, target), BorderLayout.SOUTH);
				groupGoalContainer.add(details, BorderLayout.CENTER);
			}
			else
			{
				groupGoalContainer.add(text, BorderLayout.CENTER);
			}
		}
		groupGoalContainer.revalidate(); groupGoalContainer.repaint();
	}

	private static String formatDuration(long totalSeconds)
	{
		long hours = totalSeconds / 3_600;
		long minutes = totalSeconds % 3_600 / 60;
		long seconds = totalSeconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	private static JProgressBar xpProgressBar(long progress, long target)
	{
		long safeTarget = Math.max(1, target);
		long safeProgress = Math.max(0, Math.min(progress, safeTarget));
		double percent = safeProgress * 100d / safeTarget;
		JProgressBar bar = new JProgressBar(0, 10_000);
		bar.setValue((int) Math.round(percent * 100));
		bar.setStringPainted(true);
		bar.setString(String.format("%.2f%% · %s / %s XP", percent, format(safeProgress), format(safeTarget)));
		bar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		bar.setForeground(new Color(55, 145, 45));
		bar.setBackground(new Color(16, 16, 16));
		bar.setBorder(BorderFactory.createLineBorder(BORDER_DARK));
		bar.setPreferredSize(new Dimension(150, 17));
		return bar;
	}

	void showError(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Backpack", JOptionPane.ERROR_MESSAGE);
	}

	private void promptGoal()
	{
		Goal.Type type = (Goal.Type) JOptionPane.showInputDialog(this, "Goal type", "Assign player goal", JOptionPane.PLAIN_MESSAGE, null, Goal.Type.values(), Goal.Type.SKILL_XP);
		if (type == null) return;
		GoalCatalog.Choice choice;
		if (type == Goal.Type.SKILL_XP)
		{
			choice = chooseFrom("Select a skill", catalog.skills());
		}
		else if (type == Goal.Type.QUEST)
		{
			choice = chooseFrom("Select a quest", catalog.quests());
		}
		else
		{
			choice = chooseItem();
		}
		if (choice == null) return;
		String amount = JOptionPane.showInputDialog(this, type == Goal.Type.SKILL_XP ? "Target XP" : "Target amount", type == Goal.Type.SKILL_XP ? "100000" : "1");
		if (amount == null) return;
		try
		{
			actions.setGoal(new Goal(type, choice.getName(), Long.parseLong(amount.replace(",", "").trim()), choice.getReference()));
		}
		catch (NumberFormatException ignored)
		{
			JOptionPane.showMessageDialog(this, "Enter a whole-number target.", "Invalid target", JOptionPane.ERROR_MESSAGE);
		}
	}

	private GoalCatalog.Choice chooseFrom(String title, java.util.List<GoalCatalog.Choice> choices)
	{
		JComboBox<GoalCatalog.Choice> combo = new JComboBox<>(choices.toArray(new GoalCatalog.Choice[0]));
		combo.setEditable(true);
		int result = JOptionPane.showConfirmDialog(this, combo, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		Object selected = combo.getSelectedItem();
		if (result != JOptionPane.OK_OPTION || selected == null) return null;
		if (selected instanceof GoalCatalog.Choice) return (GoalCatalog.Choice) selected;
		String text = selected.toString();
		return choices.stream().filter(choice -> choice.getName().equalsIgnoreCase(text)).findFirst().orElse(null);
	}

	private GoalCatalog.Choice chooseItem()
	{
		JTextField search = new JTextField();
		DefaultListModel<GoalCatalog.Choice> model = new DefaultListModel<>();
		JList<GoalCatalog.Choice> results = new JList<>(model);
		Runnable refresh = () -> {
			model.clear();
			for (GoalCatalog.Choice choice : catalog.findItems(search.getText())) model.addElement(choice);
			if (!model.isEmpty()) results.setSelectedIndex(0);
		};
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(DocumentEvent event) { refresh.run(); }
			public void removeUpdate(DocumentEvent event) { refresh.run(); }
			public void changedUpdate(DocumentEvent event) { refresh.run(); }
		});
		refresh.run();
		JPanel picker = new JPanel(new BorderLayout(0, 6));
		picker.add(search, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(results);
		scroll.setPreferredSize(new Dimension(300, 220));
		picker.add(scroll, BorderLayout.CENTER);
		int result = JOptionPane.showConfirmDialog(this, picker, "Search game items", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		return result == JOptionPane.OK_OPTION ? results.getSelectedValue() : null;
	}

	private static String escapeHtml(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void promptCreate()
	{
		String value = JOptionPane.showInputDialog(this, "Team name", "Create Backpack team", JOptionPane.PLAIN_MESSAGE);
		if (value != null && !value.trim().isEmpty()) actions.createTeam(value.trim());
	}

	private void promptJoin()
	{
		String value = JOptionPane.showInputDialog(this, "Enter the 8-character invite code", "Join Backpack team", JOptionPane.PLAIN_MESSAGE);
		if (value == null || value.trim().isEmpty()) return;
		String normalized = value.toUpperCase().replaceAll("[^A-Z0-9]", "");
		if (normalized.length() != 8)
		{
			JOptionPane.showMessageDialog(this, "Invite codes contain exactly 8 letters or numbers.", "Invalid invite", JOptionPane.ERROR_MESSAGE);
			return;
		}
		actions.joinTeam(normalized);
	}

	private void copyInvite()
	{
		if (currentInviteCode != null)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(currentInviteCode), null);
		}
	}

	private static JPanel buttonRow(JButton first, JButton second)
	{
		JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
		panel.setBackground(MAIN);
		panel.setAlignmentX(CENTER_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		panel.add(first);
		panel.add(second);
		return panel;
	}

	private static void styleButton(JButton button, boolean danger)
	{
		button.setFocusPainted(false);
		button.setForeground(Color.WHITE);
		button.setBackground(danger ? new Color(139, 58, 58) : new Color(51, 51, 51));
		button.setFont(UI_FONT_BOLD);
		button.setBorder(new RoundedBorder(danger ? new Color(90, 26, 26) : BORDER_DARK, 4, new Insets(7, 7, 7, 7)));
	}

	private static void styleTab(JButton button, boolean active)
	{
		button.setFocusPainted(false);
		button.setFont(UI_FONT_BOLD.deriveFont(11f));
		button.setForeground(active ? GOLD : MUTED);
		button.setBackground(active ? INSET : MAIN);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, active ? 2 : 0, 0, active ? OSRS_ORANGE : BORDER_LIGHT),
			BorderFactory.createEmptyBorder(9, 4, 8, 4)));
	}

	private JPanel buildInfoPage()
	{
		JPanel page = new JPanel();
		page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
		page.setBackground(new Color(43, 43, 43));
		page.setBorder(BorderFactory.createEmptyBorder(5, 6, 8, 6));
		badgeList.setLayout(new BoxLayout(badgeList, BoxLayout.Y_AXIS));
		badgeList.setBackground(new Color(43, 43, 43));
		page.add(badgeList);
		JScrollPane scroll = new JScrollPane(page, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null); scroll.getViewport().setBackground(new Color(43, 43, 43)); scroll.getVerticalScrollBar().setUnitIncrement(16);
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(43, 43, 43));
		header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(17, 17, 17)), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		JLabel title = new JLabel("ACHIEVEMENTS"); title.setForeground(new Color(201, 169, 110)); title.setFont(UI_FONT_BOLD.deriveFont(14f));
		badgeFilterButton.setFocusPainted(false); badgeFilterButton.setFont(UI_FONT_BOLD.deriveFont(10f));
		badgeFilterButton.setForeground(MUTED); badgeFilterButton.setBackground(new Color(30, 30, 30));
		badgeFilterButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(17, 17, 17)), BorderFactory.createEmptyBorder(3, 6, 3, 6)));
		badgeFilterButton.addActionListener(event -> {
			showLocked = !showLocked; badgeFilterButton.setText(showLocked ? "EARNED ONLY" : "SHOW LOCKED");
			renderedBadgeProgress = null; refreshBadges(currentBadgeProgress);
		});
		header.add(title, BorderLayout.WEST); header.add(badgeFilterButton, BorderLayout.EAST);
		JLabel footer = new JLabel("<html><center>Earn points through XP, quests and drops.<br>1 pt/10k XP · 5 pts/quest point · 5 pts/1m GP</center></html>", SwingConstants.CENTER);
		footer.setForeground(MUTED); footer.setFont(UI_FONT.deriveFont(11f)); footer.setBackground(new Color(30, 30, 30)); footer.setOpaque(true);
		footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT), BorderFactory.createEmptyBorder(7, 5, 7, 5)));
		JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setBackground(new Color(43, 43, 43));
		wrapper.add(header, BorderLayout.NORTH); wrapper.add(scroll, BorderLayout.CENTER); wrapper.add(footer, BorderLayout.SOUTH);
		return wrapper;
	}

	private void refreshBadges(BadgeProgress progress)
	{
		if (progress == null) progress = BadgeProgress.empty();
		currentBadgeProgress = progress;
		String snapshot = progress.serialize();
		if (snapshot.equals(renderedBadgeProgress)) return;
		renderedBadgeProgress = snapshot;
		badgeList.removeAll();
		addBadge("quest1", "Quest Beginner", "Complete 1 quest task", progress.getQuestTasks(), 1);
		addBadge("quest5", "Quest Veteran", "Complete 5 quest tasks", progress.getQuestTasks(), 5);
		addBadge("quest20", "Quest Master", "Complete 20 quest tasks", progress.getQuestTasks(), 20);
		addBadge("skill1", "Training Day", "Complete 1 skill task", progress.getSkillTasks(), 1);
		addBadge("skill5", "Skill Specialist", "Complete 5 skill tasks", progress.getSkillTasks(), 5);
		addBadge("skill20", "Master Trainer", "Complete 20 skill tasks", progress.getSkillTasks(), 20);
		addBadge("drop1", "First Find", "Complete 1 drop task", progress.getDropTasks(), 1);
		addBadge("drop5", "Treasure Hunter", "Complete 5 drop tasks", progress.getDropTasks(), 5);
		addBadge("drop20", "Loot Legend", "Complete 20 drop tasks", progress.getDropTasks(), 20);
		addBadge("goal5", "Goal Getter", "Complete 5 personal tasks", progress.getTotalTasks(), 5);
		addBadge("goal25", "Task Machine", "Complete 25 personal tasks", progress.getTotalTasks(), 25);
		addBadge("group1", "Challenge Winner", "Win 1 group challenge", progress.getGroupWins(), 1);
		addBadge("group5", "Arena Champion", "Win 5 group challenges", progress.getGroupWins(), 5);
		addBadge("carrier1", "Pack Carrier", "Carry the team for 1 completed week", progress.getCarrierWeeks(), 1);
		addBadge("carrier5", "Expedition Leader", "Carry the team for 5 weeks", progress.getCarrierWeeks(), 5);
		addBadge("points100", "Century Club", "Earn 100 Carry Points in one week", progress.getBestWeeklyPoints(), 100);
		addBadge("points500", "Point Royalty", "Earn 500 Carry Points in one week", progress.getBestWeeklyPoints(), 500);
		if (badgeList.getComponentCount() == 0)
		{
			JLabel empty = new JLabel("No achievements earned yet", SwingConstants.CENTER);
			empty.setForeground(MUTED); empty.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5)); badgeList.add(empty);
		}
		badgeList.revalidate(); badgeList.repaint();
	}

	private void addBadge(String iconKey, String name, String description, long current, long target)
	{
		if (!showLocked && current < target) return;
		if (badgeList.getComponentCount() > 0) badgeList.add(Box.createRigidArea(new Dimension(0, 6)));
		badgeList.add(badgeRow(catalog.badgeIcon(iconKey), name, description, current, target));
	}

	private static JPanel badgeRow(Image badgeImage, String name, String description, long current, long target)
	{
		boolean earned = current >= target;
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(new Color(37, 37, 37));
		row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(17, 17, 17)), BorderFactory.createEmptyBorder(5, 6, 5, 6)));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		Image shown = badgeImage == null ? null : badgeImage.getScaledInstance(26, 26, Image.SCALE_SMOOTH);
		if (!earned && shown != null) shown = javax.swing.GrayFilter.createDisabledImage(shown);
		JLabel icon = new JLabel(shown == null ? "?" : null, shown == null ? null : new ImageIcon(shown), SwingConstants.CENTER);
		icon.setOpaque(true); icon.setBackground(new Color(17, 17, 17)); icon.setBorder(BorderFactory.createLineBorder(new Color(58, 58, 58)));
		icon.setForeground(MUTED); icon.setPreferredSize(new Dimension(32, 32));
		JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		JLabel nameLabel = new JLabel((earned ? "✓ " : "🔒 ") + name); nameLabel.setForeground(earned ? GOLD : new Color(200, 200, 200));
		nameLabel.setFont(UI_FONT_BOLD);
		JLabel desc = new JLabel(description); desc.setForeground(new Color(136, 136, 136)); desc.setFont(UI_FONT.deriveFont(11f));
		JPanel progressLine = new JPanel(new BorderLayout(5, 0)); progressLine.setOpaque(false);
		JProgressBar progress = new JProgressBar(0, 100); progress.setValue((int) Math.min(100, target == 0 ? 100 : current * 100 / target));
		progress.setPreferredSize(new Dimension(80, 5)); progress.setForeground(new Color(201, 169, 110)); progress.setBackground(new Color(17, 17, 17)); progress.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		JLabel count = new JLabel(Math.min(current, target) + "/" + target); count.setForeground(MUTED); count.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		progressLine.add(progress, BorderLayout.CENTER); progressLine.add(count, BorderLayout.EAST);
		content.add(nameLabel); content.add(desc); content.add(progressLine);
		row.add(icon, BorderLayout.WEST); row.add(content, BorderLayout.CENTER);
		return row;
	}

	private static JPanel infoCard(String html)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(INSET); card.setBorder(cardBorder(10)); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
		JLabel label = new JLabel("<html><font color='#ede6da'>" + html + "</font></html>");
		label.setFont(UI_FONT); card.add(label, BorderLayout.CENTER); return card;
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setForeground(MUTED);
		label.setFont(UI_FONT_BOLD.deriveFont(11f));
		label.setAlignmentX(CENTER_ALIGNMENT);
		label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		return label;
	}

	private static String safeName(String value)
	{
		return value == null || value.isEmpty() ? "You" : value;
	}

	private static String format(long value)
	{
		if (value >= 1_000_000) return String.format("%.1fm", value / 1_000_000d);
		if (value >= 1_000) return String.format("%.1fk", value / 1_000d);
		return Long.toString(value);
	}

	private final class RaceLane extends JPanel
	{
		private final String key;
		private final String name;
		private final int combatLevel;
		private final long xp;
		private final long pointsGoal;
		private final long points;
		private final boolean active;
		private final java.util.List<Image> badges;
		private final BackpackState state;
		private final Goal assignedGoal;
		private final boolean goalCompleted;
		private boolean expanded;

		private RaceLane(String key, String name, int combatLevel, long xp, long pointsGoal, long points, boolean active, java.util.List<Image> badges,
			BackpackState state, Goal assignedGoal, boolean goalCompleted, boolean expanded)
		{
			this.key = key;
			this.name = name;
			this.combatLevel = combatLevel;
			this.xp = Math.max(0, xp);
			this.pointsGoal = Math.max(1, pointsGoal);
			this.points = Math.max(0, points);
			this.active = active;
			this.badges = badges;
			this.state = state;
			this.assignedGoal = assignedGoal;
			this.goalCompleted = goalCompleted;
			this.expanded = expanded;
			setFont(UI_FONT);
			setOpaque(false);
			updateSize();
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent event)
				{
					RaceLane.this.expanded = !RaceLane.this.expanded;
					if (RaceLane.this.expanded) expandedPlayers.add(RaceLane.this.key);
					else expandedPlayers.remove(RaceLane.this.key);
					updateSize();
					revalidate();
					if (getParent() != null) getParent().revalidate();
					repaint();
				}
			});
		}

		private void updateSize()
		{
			int height = expanded ? 180 : 82;
			setPreferredSize(new Dimension(200, height));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
			setToolTipText("Click to " + (expanded ? "hide" : "show") + " weekly achievements");
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(INSET);
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			g.setColor(BORDER_DARK);
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
			int left = 10;
			int right = getWidth() - 11;
			int trackY = 57;
			g.setColor(BORDER_DARK);
			g.fillRoundRect(left, 8, 34, 23, 4, 4);
			g.setColor(BORDER_LIGHT);
			g.drawRoundRect(left, 8, 34, 23, 4, 4);
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			g.setColor(GOLD);
			String combat = combatLevel > 0 ? Integer.toString(combatLevel) : "--";
			g.drawString(combat, left + (34 - g.getFontMetrics().stringWidth(combat)) / 2, 24);
			g.setFont(getFont().deriveFont(Font.BOLD, 13f));
			g.setColor(active ? Color.WHITE : MUTED);
			g.drawString(name, left + 44, 24);
			int badgeX = right - 22;
			for (Image badge : badges)
			{
				if (badge == null) continue;
				g.setColor(BORDER_DARK);
				g.fillOval(badgeX, 8, 22, 22);
				g.setColor(BORDER_LIGHT);
				g.drawOval(badgeX, 8, 22, 22);
				g.drawImage(badge, badgeX + 4, 12, 14, 14, null);
				badgeX -= 25;
			}
			String score = points + " pts  ·  " + format(xp) + " XP";
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			g.setColor(active ? GOLD : MUTED);
			g.drawString(score, left, 44);
			g.setColor(MUTED);
			g.drawString(expanded ? "▾" : "▸", right - 4, 44);
			double progress = Math.min(1d, points / (double) pointsGoal);
			int barWidth = right - left;
			int barHeight = 16;
			g.setColor(new Color(16, 16, 16));
			g.fillRect(left, trackY, barWidth, barHeight);
			if (progress > 0)
			{
				g.setColor(active ? new Color(44, 139, 45) : new Color(72, 92, 72));
				g.fillRect(left + 1, trackY + 1, Math.max(1, (int) ((barWidth - 2) * progress)), barHeight - 2);
			}
			g.setColor(new Color(6, 6, 6));
			g.drawRect(left, trackY, barWidth, barHeight);
			g.setFont(UI_FONT_BOLD.deriveFont(10f));
			g.setColor(Color.WHITE);
			String currentLabel = points + " pts";
			String percentLabel = String.format("%.2f%%", progress * 100d);
			String targetLabel = pointsGoal + " pts";
			int textY = trackY + 12;
			g.drawString(currentLabel, left + 4, textY);
			g.drawString(percentLabel, left + (barWidth - g.getFontMetrics().stringWidth(percentLabel)) / 2, textY);
			g.drawString(targetLabel, right - g.getFontMetrics().stringWidth(targetLabel) - 3, textY);
			if (expanded) paintBreakdown(g, left, trackY + 39);
			g.dispose();
		}

		private void paintBreakdown(Graphics2D g, int x, int y)
		{
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			if (state == null)
			{
				g.setColor(MUTED);
				g.drawString("No weekly activity received yet", x, y);
				return;
			}
			paintLine(g, x, y, "XP gained", format(state.getXpGained()) + " · " + CarryPoints.forXp(state.getXpGained()) + " pts", GOLD);
			paintLine(g, x, y + 19, "Quest points", "+" + state.getQuestPointsGained() + " · " + CarryPoints.forQuestPoints(state.getQuestPointsGained()) + " pts", GOLD);
			paintLine(g, x, y + 38, "Drops", state.getDropsReceived() + " · " + state.getDropPoints() + " pts", GOLD);
			String goalText = goalCompleted ? "+" + state.getGoalBonusPoints() + " pts · " + (assignedGoal == null ? "Complete" : assignedGoal.getDetail()) : "Not completed";
			paintLine(g, x, y + 57, "Goal bonus", goalText, goalCompleted ? new Color(123, 216, 143) : MUTED);
		}

		private void paintLine(Graphics2D g, int x, int y, String label, String value, Color valueColor)
		{
			g.setColor(new Color(210, 196, 174));
			g.drawString(label, x, y);
			g.setColor(valueColor);
			g.drawString(value, x + 92, y);
		}
	}

	private static javax.swing.border.Border cardBorder(int padding)
	{
		return BorderFactory.createCompoundBorder(new RoundedBorder(BORDER_DARK, 5, new Insets(0, 0, 0, 0)),
			BorderFactory.createEmptyBorder(padding, padding, padding, padding));
	}

	private static final class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel(BorderLayout layout)
		{
			super(layout);
		}

		@Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
		@Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
		@Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(16, visible.height - 16); }
		@Override public boolean getScrollableTracksViewportWidth() { return true; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	private static final class RoundedBorder extends AbstractBorder
	{
		private final Color color;
		private final int radius;
		private final Insets insets;
		RoundedBorder(Color color, int radius, Insets insets) { this.color = color; this.radius = radius; this.insets = insets; }
		@Override public Insets getBorderInsets(Component component) { return insets; }
		@Override public Insets getBorderInsets(Component component, Insets target)
		{
			target.set(insets.top, insets.left, insets.bottom, insets.right); return target;
		}
		@Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(color); g.drawRoundRect(x, y, width - 1, height - 1, radius * 2, radius * 2); g.dispose();
		}
	}
}
