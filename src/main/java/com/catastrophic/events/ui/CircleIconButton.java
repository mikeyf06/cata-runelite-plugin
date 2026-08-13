package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

/**
 * A small circular button for the footer icon row, showing either a text glyph (drawn with a
 * plain system font, not RuneLite's bitmap RuneScape font, so coverage is never a gamble) or a
 * bundled image (e.g. an official brand mark).
 */
class CircleIconButton extends JButton
{
	private static final int DIAMETER = 40;

	private final Color fill;
	private final String glyph;
	private final BufferedImage image;

	CircleIconButton(String glyph, Color fill, String tooltip)
	{
		this(glyph, null, fill, tooltip);
	}

	CircleIconButton(BufferedImage image, Color fill, String tooltip)
	{
		this(null, image, fill, tooltip);
	}

	private CircleIconButton(String glyph, BufferedImage image, Color fill, String tooltip)
	{
		this.glyph = glyph;
		this.image = image;
		this.fill = fill;
		setToolTipText(tooltip);
		setPreferredSize(new Dimension(DIAMETER, DIAMETER));
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		Color bg = getModel().isPressed() ? fill.darker() : getModel().isRollover() ? fill.brighter() : fill;
		g2.setColor(bg);
		g2.fill(new Ellipse2D.Float(0, 0, DIAMETER, DIAMETER));
		g2.setColor(CatastrophicTheme.CARD_BORDER);
		g2.draw(new Ellipse2D.Float(0, 0, DIAMETER - 1, DIAMETER - 1));

		if (image != null)
		{
			int w = image.getWidth();
			int h = image.getHeight();
			g2.drawImage(image, (DIAMETER - w) / 2, (DIAMETER - h) / 2, w, h, null);
		}
		else
		{
			g2.setColor(CatastrophicTheme.TEXT);
			g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
			int textWidth = g2.getFontMetrics().stringWidth(glyph);
			int textHeight = g2.getFontMetrics().getAscent();
			g2.drawString(glyph, (DIAMETER - textWidth) / 2f, (DIAMETER + textHeight) / 2f - 3);
		}

		g2.dispose();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(DIAMETER, DIAMETER);
	}
}
