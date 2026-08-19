package com.catastrophic.events.ui;

import com.catastrophic.events.CatastrophicEventsConfig;
import com.catastrophic.events.CatastrophicEventsPlugin;
import com.catastrophic.events.api.dto.EventDto;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

public class CatastrophicEventsPanel extends PluginPanel
{
	private enum Tab
	{
		ALL, MINE
	}

	private enum State
	{
		NOT_LINKED, CONNECTION_ERROR, EVENTS
	}

	private final BiConsumer<EventDto, JButton> joinHandler;
	private final ConfigManager configManager;
	private final CatastrophicEventsConfig config;
	private final Runnable onSettingsSaved;

	private final JPanel body = new JPanel();
	private final javax.swing.Timer ticker;

	private Tab activeTab = Tab.ALL;
	private State state = State.NOT_LINKED;
	private List<EventDto> events = List.of();
	private boolean showingSettings = false;

	public CatastrophicEventsPanel(BiConsumer<EventDto, JButton> joinHandler, ConfigManager configManager,
		CatastrophicEventsConfig config, Runnable onSettingsSaved)
	{
		this.joinHandler = joinHandler;
		this.configManager = configManager;
		this.config = config;
		this.onSettingsSaved = onSettingsSaved;

		setLayout(new BorderLayout());
		setBackground(CatastrophicTheme.BACKGROUND);

		add(header(), BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		add(body, BorderLayout.CENTER);

		render();

		// Re-renders countdown text between polls, purely local - no network call.
		ticker = new javax.swing.Timer(30_000, e ->
		{
			if (state == State.EVENTS && !showingSettings)
			{
				render();
			}
		});
		ticker.start();
	}

	/** Stops the local re-render ticker; call from the plugin's shutDown(). */
	public void destroy()
	{
		ticker.stop();
	}

	public void showNotLinked()
	{
		state = State.NOT_LINKED;
		render();
	}

	public void showConnectionError()
	{
		state = State.CONNECTION_ERROR;
		render();
	}

	public void showEvents(List<EventDto> events)
	{
		state = State.EVENTS;
		this.events = events;
		render();
	}

	private JPanel header()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(
			CatastrophicTheme.SPACE_MD, CatastrophicTheme.SPACE_XS, CatastrophicTheme.SPACE_MD, CatastrophicTheme.SPACE_XS));

		BufferedImage logoImage = ImageUtil.loadImageResource(com.catastrophic.events.CatastrophicEventsPlugin.class, "icon.png");
		JLabel logo = new JLabel(new ImageIcon(logoImage));
		logo.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(logo);
		header.add(Box.createRigidArea(new Dimension(0, CatastrophicTheme.SPACE_XS)));

		JLabel title = new JLabel("Catastrophic Events");
		title.setFont(CatastrophicTheme.titleFont());
		title.setForeground(CatastrophicTheme.GOLD);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(title);

		JLabel subtitle = new JLabel("Join boss mass events & claim drops");
		subtitle.setFont(CatastrophicTheme.smallFont());
		subtitle.setForeground(CatastrophicTheme.TEXT_DIM);
		subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(subtitle);

		return header;
	}

	private void render()
	{
		body.removeAll();

		if (showingSettings)
		{
			body.add(sectionLabel("Event Setup"));
			body.add(new SettingsView(configManager, this::onSettingsSavedInternal));
		}
		else if (state == State.NOT_LINKED)
		{
			body.add(message("Not linked",
				"Run !link in Discord, then paste the token it DMs you here via Event Setup below."));
		}
		else if (state == State.CONNECTION_ERROR)
		{
			body.add(message("Can't reach server", "Retrying in the background..."));
		}
		else
		{
			body.add(tabBar());
			body.add(Box.createRigidArea(new Dimension(0, CatastrophicTheme.SPACE_SM)));
			body.add(activeTab == Tab.ALL ? allEventsView() : myEventsView());
		}

		body.add(Box.createRigidArea(new Dimension(0, CatastrophicTheme.SPACE_MD)));
		body.add(footer());

		revalidate();
		repaint();
	}

	private void onSettingsSavedInternal()
	{
		showingSettings = false;
		onSettingsSaved.run();
		render();
	}

	private JPanel tabBar()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		row.add(tabButton("All Events", Tab.ALL));
		row.add(tabButton("My Events", Tab.MINE));
		return row;
	}

	private StyledButton tabButton(String text, Tab tab)
	{
		boolean active = activeTab == tab;
		StyledButton button = StyledButton.outlined(text, active ? CatastrophicTheme.GOLD : CatastrophicTheme.TEXT_DIM,
			active ? CatastrophicTheme.GOLD : CatastrophicTheme.CARD_BORDER, 8);
		button.setFont(CatastrophicTheme.smallFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
		button.addActionListener(e -> switchTab(tab));
		return button;
	}

	private void switchTab(Tab tab)
	{
		activeTab = tab;
		render();
	}

	private JPanel allEventsView()
	{
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		column.setAlignmentX(Component.LEFT_ALIGNMENT);

		List<EventDto> sorted = sortedByStart(events);
		if (sorted.isEmpty())
		{
			column.add(message("No upcoming events", null));
			return column;
		}

		for (EventDto event : sorted)
		{
			EventCompactRow row = new EventCompactRow(event, button -> joinHandler.accept(event, button));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			column.add(row);
			column.add(Box.createRigidArea(new Dimension(0, CatastrophicTheme.SPACE_SM)));
		}
		return column;
	}

	private JPanel myEventsView()
	{
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		column.setAlignmentX(Component.LEFT_ALIGNMENT);

		List<EventDto> joined = sortedByStart(events).stream()
			.filter(EventDto::isSignedUp)
			.collect(Collectors.toList());

		if (joined.isEmpty())
		{
			column.add(message("No joined events", "Switch to All Events to find something to join."));
			return column;
		}

		EventDto featured = joined.get(0);
		EventHeroCard hero = new EventHeroCard(featured, button -> joinHandler.accept(featured, button));
		hero.setAlignmentX(Component.LEFT_ALIGNMENT);
		column.add(hero);

		for (int i = 1; i < joined.size(); i++)
		{
			EventDto event = joined.get(i);
			column.add(Box.createRigidArea(new Dimension(0, CatastrophicTheme.SPACE_SM)));
			EventCompactRow row = new EventCompactRow(event, button -> joinHandler.accept(event, button));
			row.setAlignmentX(Component.LEFT_ALIGNMENT);
			column.add(row);
		}
		return column;
	}

	private static List<EventDto> sortedByStart(List<EventDto> events)
	{
		List<EventDto> copy = new ArrayList<>(events);
		copy.sort(Comparator.comparing(EventDto::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())));
		return copy;
	}

	private JPanel footer()
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, CatastrophicTheme.SPACE_LG, CatastrophicTheme.SPACE_XS));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		BufferedImage discordIcon = ImageUtil.loadImageResource(com.catastrophic.events.CatastrophicEventsPlugin.class, "discord-icon.png");
		CircleIconButton discord = new CircleIconButton(discordIcon, CatastrophicTheme.DISCORD_BLURPLE, "Open Discord");
		discord.addActionListener(e -> openDiscordServer());
		row.add(discord);

		CircleIconButton settings = new CircleIconButton("⚙", CatastrophicTheme.CARD_BACKGROUND,
			showingSettings ? "Close Setup" : "Event Setup");
		settings.addActionListener(e -> toggleSettings());
		row.add(settings);

		return row;
	}

	private void toggleSettings()
	{
		showingSettings = !showingSettings;
		render();
	}

	private void openDiscordServer()
	{
		LinkBrowser.browse("https://discord.com/channels/" + CatastrophicEventsPlugin.DISCORD_GUILD_ID);
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(CatastrophicTheme.boldFont());
		label.setForeground(CatastrophicTheme.GOLD);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		return label;
	}

	private static JPanel message(String title, String body)
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new GridLayout(body == null ? 1 : 2, 1, 0, 4));
		wrapper.setOpaque(false);
		wrapper.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(CatastrophicTheme.TEXT);
		titleLabel.setFont(CatastrophicTheme.boldFont());
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		wrapper.add(titleLabel);

		if (body != null)
		{
			JLabel bodyLabel = new JLabel("<html><div style='text-align:center'>" + body + "</div></html>");
			bodyLabel.setForeground(CatastrophicTheme.TEXT_DIM);
			bodyLabel.setFont(CatastrophicTheme.smallFont());
			bodyLabel.setHorizontalAlignment(SwingConstants.CENTER);
			wrapper.add(bodyLabel);
		}

		return wrapper;
	}
}
