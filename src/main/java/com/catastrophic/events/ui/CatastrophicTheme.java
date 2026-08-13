package com.catastrophic.events.ui;

import java.awt.Color;
import java.awt.Font;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Antique-gold-on-black accents layered over RuneLite's usual dark sidebar palette,
 * approximating the provided mockup within what a plain Swing panel can paint itself.
 */
final class CatastrophicTheme
{
	static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	static final Color CARD_BACKGROUND = new Color(0x1c, 0x18, 0x12);
	static final Color CARD_BORDER = new Color(0x5c, 0x49, 0x22);
	static final Color GOLD = new Color(0xd9, 0xb2, 0x4c);
	static final Color GOLD_DIM = new Color(0x9a, 0x7f, 0x45);
	static final Color TEXT = new Color(0xe8, 0xe0, 0xd0);
	static final Color TEXT_DIM = new Color(0x9a, 0x93, 0x84);
	static final Color TEXT_DISABLED = new Color(0x63, 0x60, 0x58);
	static final Color GREEN = new Color(0x5f, 0xb8, 0x62);
	static final Color GREEN_BADGE_BG = new Color(0x1f, 0x3a, 0x1f);
	static final Color DISCORD_BLURPLE = new Color(0x58, 0x65, 0xf2);

	/** 4px base spacing unit - every gap/margin in the panel is one of these, not a one-off number. */
	static final int SPACE_XS = 4;
	static final int SPACE_SM = 8;
	static final int SPACE_MD = 12;
	static final int SPACE_LG = 16;

	static Font titleFont()
	{
		return FontManager.getRunescapeBoldFont().deriveFont(17f);
	}

	static Font heroCountdownFont()
	{
		return FontManager.getRunescapeBoldFont().deriveFont(20f);
	}

	static Font boldFont()
	{
		return FontManager.getRunescapeBoldFont();
	}

	static Font smallFont()
	{
		return FontManager.getRunescapeSmallFont();
	}

	private CatastrophicTheme()
	{
	}
}
