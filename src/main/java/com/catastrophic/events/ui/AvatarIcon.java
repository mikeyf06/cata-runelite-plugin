package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;

/**
 * Placeholder circular "portrait" for an event, until real per-boss artwork is supplied.
 * Color and letter are derived deterministically from the event title.
 */
class AvatarIcon implements Icon
{
	private static final Color[] PALETTE = {
		new Color(0x6b, 0x2e, 0x2e), new Color(0x2e, 0x4a, 0x6b), new Color(0x3d, 0x5c, 0x30),
		new Color(0x5c, 0x3d, 0x6b), new Color(0x6b, 0x54, 0x2e), new Color(0x2e, 0x62, 0x63),
	};

	private final int size;
	private final Color fill;
	private final String letter;

	AvatarIcon(String seed, int size)
	{
		this.size = size;
		String text = seed == null || seed.isEmpty() ? "?" : seed;
		this.fill = PALETTE[Math.floorMod(text.hashCode(), PALETTE.length)];
		this.letter = text.substring(0, 1).toUpperCase();
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(fill);
		g2.fill(new Ellipse2D.Float(x, y, size, size));
		g2.setColor(CatastrophicTheme.GOLD_DIM);
		g2.draw(new Ellipse2D.Float(x, y, size - 1, size - 1));

		g2.setColor(CatastrophicTheme.TEXT);
		g2.setFont(CatastrophicTheme.boldFont().deriveFont(Font.BOLD, size * 0.45f));
		int textWidth = g2.getFontMetrics().stringWidth(letter);
		int textHeight = g2.getFontMetrics().getAscent();
		g2.drawString(letter, x + (size - textWidth) / 2f, y + (size + textHeight) / 2f - 2);
		g2.dispose();
	}

	@Override
	public int getIconWidth()
	{
		return size;
	}

	@Override
	public int getIconHeight()
	{
		return size;
	}
}
