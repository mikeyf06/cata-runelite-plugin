package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** Small rounded-rect status label, e.g. the green "STARTING SOON" badge in the mockup. */
class PillBadge extends JLabel
{
	private final Color bg;

	PillBadge(String text, Color bg, Color fg)
	{
		super(text, SwingConstants.CENTER);
		this.bg = bg;
		setForeground(fg);
		setFont(CatastrophicTheme.smallFont().deriveFont(Font.BOLD));
		setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 10, 3, 10));
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(bg);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
		g2.dispose();
		super.paintComponent(g);
	}
}
