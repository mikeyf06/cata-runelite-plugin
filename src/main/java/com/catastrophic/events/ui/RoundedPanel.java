package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/** A JPanel that paints itself as a subtly-gradiented rounded rectangle with an optional border. */
class RoundedPanel extends JPanel
{
	private final int arc;
	private final Color fillTop;
	private final Color fillBottom;
	private final Color border;
	private final Color borderHover;

	private boolean hovered;

	RoundedPanel(int arc, Color fill, Color border)
	{
		this(arc, fill, border, false);
	}

	RoundedPanel(int arc, Color fill, Color border, boolean hoverable)
	{
		this.arc = arc;
		this.fillTop = lighten(fill, 10);
		this.fillBottom = fill;
		this.border = border;
		this.borderHover = border == null ? null : lighten(border, 45);
		setOpaque(false);

		if (hoverable && border != null)
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hovered = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					// Moving onto a child (button, label) still fires exited on the parent -
					// only actually clear hover once the cursor has left this panel's bounds.
					if (!contains(e.getPoint()))
					{
						hovered = false;
						repaint();
					}
				}
			});
		}
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
			g2.setColor(hovered ? borderHover : border);
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
