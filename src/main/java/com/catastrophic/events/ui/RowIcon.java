package com.catastrophic.events.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.Icon;

/** Small hand-drawn glyphs for info rows (clock, world, people) - not font glyphs, so coverage is never a gamble. */
class RowIcon implements Icon
{
	enum Type
	{
		CLOCK, WORLD, PEOPLE
	}

	private final Type type;
	private final int size;

	RowIcon(Type type, int size)
	{
		this.type = type;
		this.size = size;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(CatastrophicTheme.TEXT_DIM);
		g2.setStroke(new java.awt.BasicStroke(1.3f));

		switch (type)
		{
			case CLOCK:
				paintClock(g2, x, y);
				break;
			case WORLD:
				paintWorld(g2, x, y);
				break;
			case PEOPLE:
				paintPeople(g2, x, y);
				break;
		}

		g2.dispose();
	}

	private void paintClock(Graphics2D g2, int x, int y)
	{
		float inset = size * 0.1f;
		g2.draw(new Ellipse2D.Float(x + inset, y + inset, size - 2 * inset, size - 2 * inset));
		float cx = x + size / 2f;
		float cy = y + size / 2f;
		g2.draw(new Line2D.Float(cx, cy, cx, y + size * 0.28f));
		g2.draw(new Line2D.Float(cx, cy, x + size * 0.66f, cy));
	}

	private void paintWorld(Graphics2D g2, int x, int y)
	{
		float inset = size * 0.1f;
		g2.draw(new Ellipse2D.Float(x + inset, y + inset, size - 2 * inset, size - 2 * inset));
		g2.draw(new Ellipse2D.Float(x + size * 0.32f, y + inset, size * 0.36f, size - 2 * inset));
		g2.draw(new Line2D.Float(x + inset, y + size / 2f, x + size - inset, y + size / 2f));
	}

	private void paintPeople(Graphics2D g2, int x, int y)
	{
		float headSize = size * 0.34f;
		g2.draw(new Ellipse2D.Float(x + size * 0.08f, y + size * 0.12f, headSize, headSize));
		g2.draw(new Ellipse2D.Float(x + size * 0.42f, y + size * 0.3f, headSize, headSize));
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
