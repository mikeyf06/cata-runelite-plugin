package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** A JPanel that paints itself as a subtly-gradiented rounded rectangle with an optional border. */
class RoundedPanel extends JPanel
{
	private final int arc;
	private final Color fillTop;
	private final Color fillBottom;
	private final Color border;

	RoundedPanel(int arc, Color fill, Color border)
	{
		this.arc = arc;
		this.fillTop = lighten(fill, 10);
		this.fillBottom = fill;
		this.border = border;
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setPaint(new GradientPaint(0, 0, fillTop, 0, getHeight(), fillBottom));
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
		if (border != null)
		{
			g2.setColor(border);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
		}
		g2.dispose();
		super.paintComponent(g);
	}

	private static Color lighten(Color c, int delta)
	{
		return new Color(
			Math.min(255, c.getRed() + delta),
			Math.min(255, c.getGreen() + delta),
			Math.min(255, c.getBlue() + delta));
	}
}
