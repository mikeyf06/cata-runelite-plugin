package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JTextArea;

/**
 * A label that actually reflows to whatever width the layout gives it, instead of a JLabel +
 * hardcoded HTML pixel width - which fights FlatLaf's auto-ellipsis and clips mid-word once the
 * real layout width doesn't match the guessed CSS width.
 */
class WrappingLabel extends JTextArea
{
	WrappingLabel(String text, Font font, Color color)
	{
		super(text);
		setEditable(false);
		setFocusable(false);
		setOpaque(false);
		setLineWrap(true);
		setWrapStyleWord(true);
		setBorder(null);
		setMargin(new java.awt.Insets(0, 0, 0, 0));
		setFont(font);
		setForeground(color);
	}
}
