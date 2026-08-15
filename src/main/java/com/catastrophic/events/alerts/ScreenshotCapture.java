package com.catastrophic.events.alerts;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageUtil;

/** Captures the current client frame as PNG bytes, for attaching to Discord alerts. */
@Slf4j
public class ScreenshotCapture
{
	private final DrawManager drawManager;

	@Inject
	public ScreenshotCapture(DrawManager drawManager)
	{
		this.drawManager = drawManager;
	}

	public void capture(Consumer<byte[]> onCaptured)
	{
		drawManager.requestNextFrameListener(image ->
		{
			BufferedImage bufferedImage = ImageUtil.bufferedImageFromImage(image);
			try (ByteArrayOutputStream out = new ByteArrayOutputStream())
			{
				ImageIO.write(bufferedImage, "png", out);
				onCaptured.accept(out.toByteArray());
			}
			catch (IOException e)
			{
				log.debug("Failed to encode alert screenshot", e);
			}
		});
	}
}
