package com.catastrophic.events.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.catastrophic.events.alerts.OneTimeRewardListener.Reward;
import org.junit.Test;

public class OneTimeRewardListenerTest
{
	@Test
	public void matchesCollectionLogEntries()
	{
		assertEquals(Reward.FIRE_CAPE, OneTimeRewardListener.match("New item added to your collection log: Fire cape"));
		assertEquals(Reward.INFERNAL_CAPE, OneTimeRewardListener.match("New item added to your collection log: Infernal cape"));
		assertEquals(Reward.DIZANAS_QUIVER, OneTimeRewardListener.match("New item added to your collection log: Dizana's quiver"));
		assertEquals(Reward.DIZANAS_QUIVER,
			OneTimeRewardListener.match("New item added to your collection log: Dizana's quiver (uncharged)"));
	}

	@Test
	public void ignoresCompletionTextThatIsNotACollectionLogEntry()
	{
		assertNull(OneTimeRewardListener.match("Congratulations, you've completed the TzHaar Fight Cave!"));
		assertNull(OneTimeRewardListener.match("Congratulations, you've completed the Inferno!"));
		assertNull(OneTimeRewardListener.match("You have been awarded Dizana's quiver for completing the Colosseum."));
		assertNull(OneTimeRewardListener.match("Fire cape"));
	}

	@Test
	public void ignoresOtherCollectionLogEntries()
	{
		assertNull(OneTimeRewardListener.match("New item added to your collection log: Tzrek-jad"));
		assertNull(OneTimeRewardListener.match("New item added to your collection log: Infernal cape (broken)"));
		assertNull(OneTimeRewardListener.match("New item added to your collection log: Fire cape (l)"));
		assertNull(OneTimeRewardListener.match("New item added to your collection log: Dizana's max cape"));
	}

	@Test
	public void ignoresEmptyInput()
	{
		assertNull(OneTimeRewardListener.match(null));
		assertNull(OneTimeRewardListener.match(""));
	}
}
