package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JButton;

/** Fully custom-painted button (no default L&F chrome) so buttons can be true rounded pills, not tinted rectangles. */
class StyledButton extends JButton
{
	private static final int PILL = -1;

	private final Color fill;
	private final Color fillHover;
	private final Color fillPressed;
	private final Color outline;
	private final int arc;

	static StyledButton pill(String text, Color fill, Color textColor)
	{
		return new StyledButton(text, fill, textColor, PILL, null);
	}

	static StyledButton outlined(String text, Color textColor, Color outline, int arc)
	{
		return new StyledButton(text, CatastrophicTheme.CARD_BACKGROUND, textColor, arc, outline);
	}

	StyledButton(String text, Color fill, Color textColor, int arc, Color outline)
	{
		super(text);
		this.fill = fill;
		// Brightening an already-bright fill (e.g. the gold pill buttons) barely reads as feedback,
		// so bright fills darken on hover/press instead - whichever direction actually shows.
		int direction = luminance(fill) > 140 ? -1 : 1;
		this.fillHover = brighten(fill, direction * 24);
		this.fillPressed = brighten(fill, direction * 44);
		this.outline = outline;
		this.arc = arc;

		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setForeground(textColor);
		setFont(CatastrophicTheme.smallFont());
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		// Generous default padding so text never gets tight - callers only need setPreferredSize for something unusual.
		setMargin(new Insets(6, 16, 6, 16));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color bg = !isEnabled() ? CatastrophicTheme.CARD_BACKGROUND
			: getModel().isPressed() ? fillPressed
			: getModel().isRollover() ? fillHover
			: fill;
		int cornerArc = arc == PILL ? getHeight() : arc;

		g2.setColor(bg);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerArc, cornerArc);

		if (outline != null)
		{
			g2.setColor(isEnabled() ? outline : CatastrophicTheme.CARD_BORDER);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerArc, cornerArc);
		}

		g2.dispose();
		super.paintComponent(g);
	}

	private static Color brighten(Color c, int delta)
	{
		return new Color(
			clamp(c.getRed() + delta),
			clamp(c.getGreen() + delta),
			clamp(c.getBlue() + delta));
	}

	private static int clamp(int v)
	{
		return Math.max(0, Math.min(255, v));
	}

	private static int luminance(Color c)
	{
		return (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
	}
}
