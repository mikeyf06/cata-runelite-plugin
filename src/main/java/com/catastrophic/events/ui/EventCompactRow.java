package com.catastrophic.events.ui;

import com.catastrophic.events.api.dto.EventDto;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

class EventCompactRow extends RoundedPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

	EventCompactRow(EventDto event, Consumer<JButton> onJoinClicked)
	{
		super(10, CatastrophicTheme.CARD_BACKGROUND, CatastrophicTheme.CARD_BORDER);
		setLayout(new BorderLayout(CatastrophicTheme.SPACE_SM, 0));
		setBorder(BorderFactory.createEmptyBorder(
			CatastrophicTheme.SPACE_MD, CatastrophicTheme.SPACE_MD, CatastrophicTheme.SPACE_MD, CatastrophicTheme.SPACE_SM));

		JLabel avatar = new JLabel(new AvatarIcon(event.getTitle(), 40));
		add(avatar, BorderLayout.WEST);

		WrappingLabel title = new WrappingLabel(event.getTitle(), CatastrophicTheme.boldFont(), CatastrophicTheme.TEXT);
		title.setSize(new Dimension(70, Short.MAX_VALUE));

		JLabel subtitle = new JLabel(subtitleText(event));
		subtitle.setFont(CatastrophicTheme.smallFont());
		subtitle.setForeground(CatastrophicTheme.TEXT_DIM);
		subtitle.setBorder(BorderFactory.createEmptyBorder(CatastrophicTheme.SPACE_XS, 0, 0, 0));

		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.setOpaque(false);
		textPanel.add(title);
		textPanel.add(subtitle);
		add(textPanel, BorderLayout.CENTER);

		JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonWrap.setOpaque(false);
		boolean signedUp = event.isSignedUp();
		StyledButton joinButton = signedUp
			? StyledButton.outlined("Joined", CatastrophicTheme.GREEN, CatastrophicTheme.GREEN, 999)
			: StyledButton.pill("Join", CatastrophicTheme.GOLD, CatastrophicTheme.BACKGROUND);
		joinButton.setEnabled(!signedUp);
		joinButton.addActionListener(e -> onJoinClicked.accept(joinButton));
		buttonWrap.add(joinButton);
		add(buttonWrap, BorderLayout.EAST);
	}

	private static String subtitleText(EventDto event)
	{
		// No host field in the events API yet - once catabot-web exposes host_discord_id, prefix it here.
		return formatTime(event);
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
			return "time unknown";
		}
	}
}
