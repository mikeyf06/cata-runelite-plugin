package com.catastrophic.events.ui;

import com.catastrophic.events.api.dto.EventDto;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.net.URI;
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
import lombok.extern.slf4j.Slf4j;

/** The expanded "featured" event card - the soonest joined event on the My Events tab. */
@Slf4j
class EventHeroCard extends RoundedPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a z");
	private static final int STARTING_SOON_THRESHOLD_MINUTES = 60;

	EventHeroCard(EventDto event, String websiteBase, Consumer<JButton> onJoinClicked)
	{
		super(14, CatastrophicTheme.CARD_BACKGROUND, CatastrophicTheme.CARD_BORDER);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(
			CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_LG));

		add(header(event));
		add(gap(CatastrophicTheme.SPACE_MD));
		add(countdown(event));
		add(separator());
		add(infoRow("Time", formatTime(event)));
		add(infoRow("World", "Not set by host yet"));
		add(separator());
		add(infoRow("Attendance", attendanceText(event)));
		add(featureRow("Auto check-in", "Coming soon", false));
		add(featureRow("Drop screenshots", "Coming soon", false));
		add(featureRow("Notifications", "Joined events only", true));
		add(gap(CatastrophicTheme.SPACE_MD));
		add(joinButtonRow(event, onJoinClicked));
		add(gap(CatastrophicTheme.SPACE_SM));
		add(featureRow("Discord Voice", "Coming soon", false));
		add(gap(CatastrophicTheme.SPACE_SM));
		add(viewEventButton(event, websiteBase));
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
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel text = new JLabel(countdownText(event));
		text.setFont(CatastrophicTheme.heroCountdownFont());
		text.setForeground(CatastrophicTheme.GOLD);
		row.add(text);
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

	private JPanel viewEventButton(EventDto event, String websiteBase)
	{
		StyledButton button = StyledButton.outlined("View Event", CatastrophicTheme.GOLD, CatastrophicTheme.CARD_BORDER, 8);
		button.setMinimumSize(new Dimension(10, 32));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		button.addActionListener(e -> openEventPage(websiteBase, event.getId()));

		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.add(button, BorderLayout.CENTER);
		return wrap;
	}

	private static void openEventPage(String websiteBase, String eventId)
	{
		try
		{
			Desktop.getDesktop().browse(new URI(websiteBase + "/events/" + eventId));
		}
		catch (Exception ex)
		{
			log.warn("Failed to open event page", ex);
		}
	}

	private static JPanel infoRow(String label, String value)
	{
		return infoRow(label, value, CatastrophicTheme.TEXT);
	}

	private static JPanel infoRow(String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(CatastrophicTheme.SPACE_XS, 0, CatastrophicTheme.SPACE_XS, 0));

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(CatastrophicTheme.smallFont());
		labelComponent.setForeground(CatastrophicTheme.TEXT_DIM);
		row.add(labelComponent, BorderLayout.WEST);

		JLabel valueComponent = new JLabel(value);
		valueComponent.setFont(CatastrophicTheme.smallFont());
		valueComponent.setForeground(valueColor);
		row.add(valueComponent, BorderLayout.EAST);
		return row;
	}

	private static JPanel featureRow(String label, String status, boolean available)
	{
		String display = available ? status + " ✓" : status;
		return infoRow(label, display, available ? CatastrophicTheme.GREEN : CatastrophicTheme.TEXT_DISABLED);
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
