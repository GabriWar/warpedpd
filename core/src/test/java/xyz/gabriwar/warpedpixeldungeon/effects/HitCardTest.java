/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Piece;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;

/**
 * The hit card (HitCard), headless on EnchantFxTest's stage (no fonts: its words are left out,
 * everything else runs): rapid blows of one attacker merge within MERGE s and not after, another
 * attacker gets a card of its own and pushes the older one up; xN counts; four chips, or three and
 * "+N"; the chips keep the order they came in; a buff the body shows or a piece stands for leaves
 * no chip; the blow's reason survives a proc's number coming first; the X takes the left slot; the
 * hop and the X's stamp come at most three times a second; a merge flashes the number white; the
 * fade starts by 2 s; at most twelve cards fade in at once; a card follows its sprite; a frame of a
 * dozen cards allocates nothing; nothing of the game's Random is drawn; a recycled card's pictures
 * exist (it draws).
 */
public class HitCardTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		HitCard.reset();
	}

	@After
	public void tearDown(){
		HitCard.reset();
		rig.down();
	}

	private static HitCard card( CharSprite s, int attacker, int number, boolean crit, int... chips ){
		int[] tints = new int[chips.length];
		float[] lands = new float[chips.length];
		return HitCard.show( s, attacker, number, CharSprite.NEGATIVE, FloatingText.PHYS_DMG, crit, 0f, chips, tints, lands, chips.length );
	}

	//seconds of frames at a 30th of a second
	private void wait( float seconds ){
		rig.frames( Math.round( seconds * 30f ), true );
	}

	@Test
	public void rapidBlowsOfOneAttackerMergeWithinTheWindowAndNotAfter(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 5, false );
		assertNotNull( "a card on the scene's statuses", c );
		wait( 0.3f );
		assertSame( "merged within " + HitCard.MERGE + " s", c, card( s, 1, 7, false ) );
		assertEquals( 12, c.number() );
		assertEquals( 2, c.count() );
		wait( 0.5f );
		HitCard later = card( s, 1, 3, false );
		assertNotSame( "a new card past the window", c, later );
		assertEquals( 3, later.number() );
		assertEquals( 1, later.count() );
	}

	@Test
	public void anotherAttackerGetsItsOwnCardAndPushesTheOlderUp(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard mine = card( s, 1, 5, false );
		wait( 0.1f );
		float top = mine.cardTop();
		HitCard theirs = card( s, 2, 4, false );
		assertNotSame( mine, theirs );
		assertEquals( 5, mine.number() );
		assertEquals( 4, theirs.number() );
		assertTrue( "pushed up", mine.cardTop() < top - 2f );
		assertTrue( "its fade starts soon", mine.fadeAt() <= mine.age() + HitCard.PUSHED_FADE + 1e-4f );
	}

	@Test
	public void aPushedCardClearsATallerNewOneUntilItIsGone(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard[] cards = new HitCard[3];
		//a number-only card, then another attacker's with chips (taller), then a third's taller again
		cards[0] = card( s, 1, 5, false );
		wait( 0.1f );
		cards[1] = card( s, 2, 4, false, FloatingText.F_EMBER );
		assertTrue( "the new one is taller", cards[1].cardTop() - cards[1].baseY() < cards[0].cardTop() - cards[0].baseY() );
		wait( 0.1f );
		cards[2] = card( s, 3, 3, false, FloatingText.F_EMBER, -1 );
		for (int f = 0; f < 60; f++){
			for (int i = 0; i + 1 < cards.length; i++){
				HitCard old = cards[i], young = cards[i + 1];
				if (!old.alive || !young.alive) continue;
				assertTrue( "frame " + f + ": card " + i + " clears the one after it (" + old.baseY() + " vs " + young.cardTop() + ")",
						old.baseY() <= young.cardTop() - 2f + 0.5f );
			}
			rig.frames( 1, true );
		}
	}

	@Test
	public void xNCountsTheMergedBlows(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 2, false );
		for (int i = 0; i < 4; i++){
			wait( 0.1f );
			card( s, 1, 2, false );
		}
		assertEquals( 5, c.count() );
		assertEquals( 10, c.number() );
	}

	@Test
	public void fourChipsOrThreeAndPlusN(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard four = card( s, 1, 2, false, FloatingText.F_EMBER, FloatingText.HEAVE, FloatingText.MOON, -(BuffIndicator.VULNERABLE + 1) );
		assertEquals( 4, four.chipCount() );
		assertEquals( 4, four.shownChips() );
		CharSprite t = rig.rat( at( 7, 6 ) );
		HitCard six = card( t, 1, 2, false, FloatingText.F_EMBER, FloatingText.HEAVE, FloatingText.MOON, FloatingText.STORM,
				FloatingText.S_BEAST, -(BuffIndicator.VULNERABLE + 1) );
		assertEquals( 6, six.chipCount() );
		assertEquals( "three and +3 in the fourth slot", 3, six.shownChips() );
	}

	@Test
	public void chipsKeepTheOrderTheyCameInAndNoneTwice(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 2, false, FloatingText.F_EMBER, FloatingText.HEAVE );
		wait( 0.1f );
		card( s, 1, 2, false, FloatingText.HEAVE, FloatingText.MOON );
		assertEquals( 3, c.chipCount() );
		assertEquals( FloatingText.F_EMBER, c.chip( 0 ) );
		assertEquals( FloatingText.HEAVE, c.chip( 1 ) );
		assertEquals( FloatingText.MOON, c.chip( 2 ) );
	}

	@Test
	public void theXTakesTheLeftSlotForGood(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard plain = card( s, 1, 2, false );
		assertEquals( FloatingText.PHYS_DMG, plain.leftIcon() );
		wait( 0.1f );
		card( s, 1, 2, true );
		assertEquals( FloatingText.CRIT, plain.leftIcon() );
		wait( 0.1f );
		card( s, 1, 2, false );
		assertEquals( "a later plain blow keeps the X", FloatingText.CRIT, plain.leftIcon() );
	}

	@Test
	public void theHopAndTheStampComeAtMostThreeTimesASecond(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		Game.elapsed = 1 / 60f;
		HitCard c = card( s, 1, 2, true );
		int stamps = 0, hops = 0;
		boolean wasStamping = false, wasHopping = false;
		for (int f = 0; f < 60; f++){
			//a crit every 0.05 s: twenty a second
			if (f % 3 == 0) card( s, 1, 2, true );
			rig.frames( 1, true );
			boolean st = c.stamping(), hop = c.hop() < 0f;
			if (st && !wasStamping) stamps++;
			if (hop && !wasHopping) hops++;
			wasStamping = st;
			wasHopping = hop;
		}
		assertTrue( "stamps " + stamps, stamps >= 2 && stamps <= 4 );
		assertTrue( "hops " + hops, hops >= 2 && hops <= 4 );
		assertTrue( "the card lived through it", c.alive );
	}

	@Test
	public void theFadeStartsByTwoSecondsWhateverMerges(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 2, false );
		for (int i = 0; i < 12; i++){
			wait( 0.3f );
			HitCard m = card( s, 1, 2, false );
			if (m != c) break;
		}
		assertTrue( "its fade at " + c.fadeAt(), c.fadeAt() <= HitCard.LATEST + 1e-4f );
		assertTrue( "gone by then", !c.alive || c.age() <= HitCard.LATEST + HitCard.FADE + 0.05f );
		wait( 0.5f );
		assertTrue( "gone", !c.alive || c.target() != s );
	}

	@Test
	public void atMostTwelveCardsLiveUnfaded(){
		List<HitCard> cards = new ArrayList<>();
		for (int i = 0; i < 14; i++){
			cards.add( card( rig.rat( at( 1 + i, 12 ) ), 1, 3, false ) );
			wait( 0.05f );
		}
		int unfaded = 0;
		for (HitCard c : cards) if (c.alive && c.age() < c.fadeAt()) unfaded++;
		assertTrue( "unfaded " + unfaded, unfaded <= HitCard.MOST );
		wait( 0.2f );
		int alive = 0;
		for (HitCard c : cards) if (c.alive) alive++;
		assertTrue( "the oldest went at once: " + alive, alive <= HitCard.MOST );
	}

	@Test
	public void aCardFollowsItsSprite(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 2, false );
		wait( 0.1f );
		s.x += 16;
		wait( 0.3f );
		assertEquals( s.x + s.width() / 2f, c.left(), 0.5f );
	}

	@Test
	public void aBodyStateIsAChipAndAFoldingPieceLeavesNone(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Blow.begin( a.ch, d.ch );
		Blow.offer( new Piece( -1, Blow.MINOR, Blow.TARGET, d.ch ){
			{ folds = new Class<?>[]{ Cripple.class }; }
			@Override public void lead( boolean mayShake ){}
			@Override public void accent( float x, float y ){}
		} );
		assertTrue( "the body burns: taken as its icon", Blow.announce( d.ch, new Burning() ) );
		assertTrue( Blow.announce( d.ch, new Cripple() ) );
		assertTrue( Blow.announce( d.ch, new Vulnerable() ) );
		Blow.claim( d.ch, "4", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		HitCard c = EnchantState.get( d ).card;
		assertNotNull( c );
		assertEquals( "the burning's and the vulnerable chips, not the folded cripple's", 2, c.chipCount() );
		assertEquals( -(BuffIndicator.FIRE + 1), c.chip( 0 ) );
		assertEquals( -(BuffIndicator.VULNERABLE + 1), c.chip( 1 ) );
	}

	@Test
	public void theBlowsReasonSurvivesAProcsNumberComingFirst(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Blow.begin( a.ch, d.ch );
		Blow.reason( a.ch, d.ch, FloatingText.HIT_SUPR );
		assertTrue( Blow.claim( d.ch, "3", CharSprite.NEGATIVE, FloatingText.MAGIC_DMG ) );
		assertTrue( Blow.claim( d.ch, "7", CharSprite.NEGATIVE, FloatingText.PHYS_DMG ) );
		Blow.end();
		HitCard c = EnchantState.get( d ).card;
		assertEquals( 10, c.number() );
		assertEquals( FloatingText.HIT_SUPR, c.leftIcon() );
	}

	@Test
	public void aFrameOfADozenCardsAllocatesNothing(){
		//short frames: the cards live through the three hundred of them
		Game.elapsed = 1 / 600f;
		for (int i = 0; i < 12; i++){
			card( rig.rat( at( 1 + i, 3 ) ), 1, 3 + i, i % 2 == 0, FloatingText.F_EMBER, FloatingText.HEAVE,
					-(BuffIndicator.VULNERABLE + 1) );
		}
		final Group statuses = GameScene.layer( GameScene.Layer.STATUSES );
		for (int f = 0; f < 30; f++) statuses.update();
		long bytes = allocated( () -> {
			for (int f = 0; f < 300; f++) statuses.update();
		} );
		assertTrue( "three hundred frames of twelve cards made " + bytes + " bytes", bytes < 4 * 1024 );
		int cards = 0;
		for (Gizmo g : statuses.membersView()) if (g instanceof HitCard && g.alive) cards++;
		assertEquals( "they lived through it", 12, cards );
	}

	@Test
	public void cardsDrawNothingOfTheGamesRandom(){
		final CharSprite s = rig.rat( at( 6, 6 ) ), t = rig.rat( at( 7, 6 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			card( s, 1, 5, true, FloatingText.F_EMBER );
			HitCard.small( t, 3, CharSprite.NEGATIVE, -1, 0 );
			HitCard.gain( t, FloatingText.HEALING, 4, CharSprite.POSITIVE );
			wait( 2.5f );
		} ) );
	}

	@Test
	public void aMergeFlashesTheNumber(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard c = card( s, 1, 5, false );
		HitCard lone = card( rig.rat( at( 8, 6 ) ), 1, 4, false );
		assertFalse( "a fresh card does not flash", c.flashing() );
		wait( 0.1f );
		assertSame( c, card( s, 1, 2, false ) );
		assertTrue( "the merged hit flashes the number", c.flashing() );
		assertFalse( "a plain merge does not stamp the X", c.stamping() );
		assertEquals( FloatingText.PHYS_DMG, c.leftIcon() );
		rig.frames( HitCard.STAMP_FRAMES, true );
		assertFalse( "the flash is over after " + HitCard.STAMP_FRAMES + " frames", c.flashing() );
		for (int f = 0; f < 30; f++){
			assertFalse( "frame " + f + ": a card nothing merged into never flashes", lone.flashing() );
			rig.frames( 1, true );
		}
	}

	@Test
	public void aRecycledCardsPicturesExist(){
		CharSprite s = rig.rat( at( 6, 6 ) );
		HitCard first = card( s, 1, 5, false, FloatingText.F_EMBER );
		assertTrue( first.leftExists() && first.numberExists() && first.chipExists( 0 ) );
		wait( 1.5f );
		assertFalse( "gone after 1.5 s", first.alive );
		HitCard second = card( rig.rat( at( 8, 6 ) ), 1, 7, false, FloatingText.F_EMBER );
		assertSame( "the dead card recycled", first, second );
		//Group.kill killed its pictures with it; Group.draw skips a picture that does not exist
		assertTrue( "its left icon exists", second.leftExists() );
		assertTrue( "its number exists", second.numberExists() );
		assertTrue( "its chip exists", second.chipExists( 0 ) );
	}

	@Test
	public void twentyBlowsAllDraw(){
		CharSprite a = rig.rat( at( 6, 6 ) ), b = rig.rat( at( 8, 6 ) );
		for (int i = 0; i < 20; i++){
			HitCard c = card( i % 2 == 0 ? a : b, 1 + (i % 3), 3 + i, false, FloatingText.F_EMBER );
			assertNotNull( c );
			assertTrue( "blow " + i + ": its pictures exist", c.leftExists() && c.numberExists() && c.chipExists( 0 ) );
			wait( 0.6f );
		}
	}

	static long allocated( Runnable r ){
		com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long id = Thread.currentThread().getId();
		long before = mx.getThreadAllocatedBytes( id );
		r.run();
		return mx.getThreadAllocatedBytes( id ) - before;
	}
}
