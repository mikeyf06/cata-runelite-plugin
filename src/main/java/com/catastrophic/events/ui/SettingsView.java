package com.catastrophic.events.ui;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.config.ConfigManager;

/** Inline "Event Setup" form opened from the gear icon, writes straight to ConfigManager on Save. */
class SettingsView extends JPanel
{
	private static final String CONFIG_GROUP = "catastrophicevents";

	SettingsView(ConfigManager configManager, Runnable onSaved)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

		JTextField tokenField = field(configManager.getConfiguration(CONFIG_GROUP, "token"));
		JTextField apiBaseField = field(configManager.getConfiguration(CONFIG_GROUP, "apiBase"));
		JTextField websiteBaseField = field(configManager.getConfiguration(CONFIG_GROUP, "websiteBase"));
		JTextField guildIdField = field(configManager.getConfiguration(CONFIG_GROUP, "guildId"));

		add(fieldRow("Plugin token", tokenField));
		add(fieldRow("Plugin API base URL (bot)", apiBaseField));
		add(fieldRow("Website base URL", websiteBaseField));
		add(fieldRow("Discord server ID", guildIdField));
		add(Box.createRigidArea(new Dimension(0, 6)));

		StyledButton saveButton = StyledButton.pill("Save", CatastrophicTheme.GOLD, CatastrophicTheme.BACKGROUND);
		saveButton.setFont(CatastrophicTheme.boldFont());
		saveButton.setAlignmentX(LEFT_ALIGNMENT);
		saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		saveButton.addActionListener(e ->
		{
			configManager.setConfiguration(CONFIG_GROUP, "token", tokenField.getText().trim());
			configManager.setConfiguration(CONFIG_GROUP, "apiBase", apiBaseField.getText().trim());
			configManager.setConfiguration(CONFIG_GROUP, "websiteBase", websiteBaseField.getText().trim());
			configManager.setConfiguration(CONFIG_GROUP, "guildId", guildIdField.getText().trim());
			onSaved.run();
		});
		add(saveButton);
	}

	private static JPanel fieldRow(String label, JTextField field)
	{
		JPanel row = new JPanel();
		row.setOpaque(false);
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(CatastrophicTheme.smallFont());
		labelComponent.setForeground(CatastrophicTheme.TEXT_DIM);
		labelComponent.setAlignmentX(LEFT_ALIGNMENT);
		row.add(labelComponent);

		field.setAlignmentX(LEFT_ALIGNMENT);
		field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		row.add(field);
		return row;
	}

	private static JTextField field(String initialValue)
	{
		JTextField field = new JTextField(initialValue == null ? "" : initialValue);
		field.setFont(CatastrophicTheme.smallFont().deriveFont(Font.PLAIN));
		field.setBackground(CatastrophicTheme.CARD_BACKGROUND);
		field.setForeground(CatastrophicTheme.TEXT);
		field.setCaretColor(CatastrophicTheme.TEXT);
		field.setBorder(BorderFactory.createLineBorder(CatastrophicTheme.CARD_BORDER));
		return field;
	}
}
