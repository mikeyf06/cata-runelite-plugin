package com.catastrophic.events.ui;

import com.catastrophic.events.CatastrophicEventsPlugin;
import com.catastrophic.events.api.dto.EventDto;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.runelite.client.util.LinkBrowser;

/** The expanded "featured" event card - the soonest joined event on the My Events tab. */
class EventHeroCard extends RoundedPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a z");
	private static final int STARTING_SOON_THRESHOLD_MINUTES = 60;

	EventHeroCard(EventDto event, Consumer<JButton> onJoinClicked)
	{
		super(14, CatastrophicTheme.CARD_BACKGROUND, CatastrophicTheme.CARD_BORDER);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(
			CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG));

		add(header(event));
		add(gap(CatastrophicTheme.SPACE_MD));
		add(countdown(event));
		add(gap(CatastrophicTheme.SPACE_XS));
		add(separator());
		add(gap(CatastrophicTheme.SPACE_XS));
		add(infoRow(RowIcon.Type.CLOCK, "Time", formatTime(event)));
		add(infoRow(RowIcon.Type.WORLD, "World", worldText(event)));
		add(gap(CatastrophicTheme.SPACE_XS));
		add(separator());
		add(gap(CatastrophicTheme.SPACE_XS));
		add(infoRow(RowIcon.Type.PEOPLE, "Attendance", attendanceText(event)));
		add(featureRow("Auto check-in", "Coming soon", false));
		add(featureRow("Drop screenshots", "Coming soon", false));
		add(featureRow("Notifications", "Joined events only", true));
		add(gap(CatastrophicTheme.SPACE_MD));
		add(joinButtonRow(event, onJoinClicked));
		add(gap(CatastrophicTheme.SPACE_SM));
		add(discordVoiceRow(event));
	}

	private static Component gap(int height)
	{
		return Box.createRigidArea(new Dimension(0, height));
	}

	private JPanel header(EventDto event)
	{
		JPanel row = new JPanel(new BorderLayout(CatastrophicTheme.SPACE_MD, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		row.add(new JLabel(new AvatarIcon(event.getTitle(), 64)), BorderLayout.WEST);

		JPanel titleCol = new JPanel();
		titleCol.setOpaque(false);
		titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));

		WrappingLabel title = new WrappingLabel(event.getTitle(), CatastrophicTheme.titleFont(), CatastrophicTheme.TEXT);
		title.setSize(new Dimension(110, Short.MAX_VALUE));
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		titleCol.add(title);

		Long minutesUntil = safeMinutesUntilStart(event);
		if (minutesUntil != null && minutesUntil >= 0 && minutesUntil <= STARTING_SOON_THRESHOLD_MINUTES)
		{
			PillBadge badge = new PillBadge("STARTING SOON", CatastrophicTheme.GREEN_BADGE_BG, CatastrophicTheme.GREEN);
			badge.setAlignmentX(Component.LEFT_ALIGNMENT);
			titleCol.add(gap(CatastrophicTheme.SPACE_XS));
			titleCol.add(badge);
		}

		row.add(titleCol, BorderLayout.CENTER);
		return row;
	}

	private JPanel countdown(EventDto event)
	{
		JPanel row = new JPanel(new BorderLayout(CatastrophicTheme.SPACE_XS, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel icon = new JLabel(new RowIcon(RowIcon.Type.CLOCK, 20));
		row.add(icon, BorderLayout.WEST);

		WrappingLabel text = new WrappingLabel(countdownText(event), CatastrophicTheme.heroCountdownFont(), CatastrophicTheme.GOLD);
		text.setSize(new Dimension(150, Short.MAX_VALUE));
		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private JPanel joinButtonRow(EventDto event, Consumer<JButton> onJoinClicked)
	{
		boolean signedUp = event.isSignedUp();
		StyledButton joinButton = signedUp
			? StyledButton.outlined("Joined ✓", CatastrophicTheme.GREEN, CatastrophicTheme.GREEN, 8)
			: StyledButton.pill("Join", CatastrophicTheme.GOLD, CatastrophicTheme.BACKGROUND);
		joinButton.setEnabled(!signedUp);
		joinButton.setFont(CatastrophicTheme.boldFont().deriveFont(14f));
		joinButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		joinButton.setMinimumSize(new Dimension(10, 38));
		joinButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
		joinButton.addActionListener(e -> onJoinClicked.accept(joinButton));

		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.add(joinButton, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel discordVoiceRow(EventDto event)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(CatastrophicTheme.SPACE_SM, 0, CatastrophicTheme.SPACE_SM, 0));

		JLabel labelComponent = new JLabel("Discord Voice");
		labelComponent.setFont(CatastrophicTheme.smallFont());
		labelComponent.setForeground(CatastrophicTheme.TEXT_DIM);
		row.add(labelComponent, BorderLayout.WEST);

		String voiceChannelId = event.getDiscordVoiceChannelId();
		boolean canJoin = voiceChannelId != null && !voiceChannelId.isEmpty();

		if (canJoin)
		{
			StyledButton joinVc = StyledButton.outlined("Join VC", CatastrophicTheme.DISCORD_BLURPLE,
				CatastrophicTheme.DISCORD_BLURPLE, 999);
			joinVc.addActionListener(e -> openVoiceChannel(voiceChannelId));
			row.add(joinVc, BorderLayout.EAST);
		}
		else
		{
			JLabel notSet = new JLabel("Not set");
			notSet.setFont(CatastrophicTheme.smallFont());
			notSet.setForeground(CatastrophicTheme.TEXT_DISABLED);
			row.add(notSet, BorderLayout.EAST);
		}

		return row;
	}

	private static void openVoiceChannel(String voiceChannelId)
	{
		LinkBrowser.browse("https://discord.com/channels/" + CatastrophicEventsPlugin.DISCORD_GUILD_ID + "/" + voiceChannelId);
	}

	private static String worldText(EventDto event)
	{
		Integer world = event.getWorld();
		return world == null ? "Not set by host yet" : "W" + world;
	}

	private static JPanel infoRow(RowIcon.Type icon, String label, String value)
	{
		return infoRow(icon, label, value, CatastrophicTheme.TEXT);
	}

	private static JPanel infoRow(RowIcon.Type icon, String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(CatastrophicTheme.SPACE_SM, 0, CatastrophicTheme.SPACE_SM, 0));

		JPanel labelWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, CatastrophicTheme.SPACE_XS, 0));
		labelWrap.setOpaque(false);

		if (icon != null)
		{
			labelWrap.add(new JLabel(new RowIcon(icon, 13)));
		}

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(CatastrophicTheme.smallFont());
		labelComponent.setForeground(CatastrophicTheme.TEXT_DIM);
		labelWrap.add(labelComponent);
		row.add(labelWrap, BorderLayout.WEST);

		JLabel valueComponent = new JLabel(value);
		valueComponent.setFont(CatastrophicTheme.smallFont());
		valueComponent.setForeground(valueColor);
		row.add(valueComponent, BorderLayout.EAST);
		return row;
	}

	private static JPanel featureRow(String label, String status, boolean available)
	{
		String display = available ? status + " ✓" : status;
		return infoRow(null, label, display, available ? CatastrophicTheme.GREEN : CatastrophicTheme.TEXT_DISABLED);
	}

	private static JSeparator separator()
	{
		JSeparator separator = new JSeparator();
		separator.setForeground(CatastrophicTheme.CARD_BORDER);
		separator.setBackground(CatastrophicTheme.CARD_BACKGROUND);
		separator.setAlignmentX(Component.LEFT_ALIGNMENT);
		separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
		return separator;
	}

	private static String attendanceText(EventDto event)
	{
		EventDto.Attendance attendance = event.getAttendance();
		if (attendance == null)
		{
			return "unknown";
		}
		return attendance.getSignedUp() + " signed up · " + attendance.getPresent() + " present";
	}

	private static String countdownText(EventDto event)
	{
		Long minutes = safeMinutesUntilStart(event);
		if (minutes == null)
		{
			return "Start time unknown";
		}
		if (minutes < 0)
		{
			return "Already started";
		}
		if (minutes < 60)
		{
			return "Starts in " + minutes + "m";
		}
		long hours = minutes / 60;
		if (hours < 24)
		{
			return "Starts in " + hours + "h " + (minutes % 60) + "m";
		}
		return "Starts in " + (hours / 24) + "d " + (hours % 24) + "h";
	}

	private static Long safeMinutesUntilStart(EventDto event)
	{
		try
		{
			return event.minutesUntilStart();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private static String formatTime(EventDto event)
	{
		try
		{
			OffsetDateTime start = OffsetDateTime.parse(event.getStartAt());
			return TIME_FORMAT.format(start.atZoneSameInstant(java.time.ZoneId.systemDefault()));
		}
		catch (DateTimeParseException | NullPointerException e)
		{
			return "unknown";
		}
	}
}
