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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowPresenter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.at;
import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest.draws;

/**
 * What waits for its beat on a blow (docs/fx/enchant.md, "Arranged for the hit card"), headless
 * on EnchantFxTest's stage: a drain enchantment's heal shows when the last bead it sends home lands
 * (a lead's, a strong Vampiric's three more, a second's from its own beat, Ares' leech's from every
 * foe it drains, the struck one or not), never before it, with HealFx's whole look; at once (or on a
 * second's beat) when its beat draws no line home; a ward's drain holds nothing; the stun stars of a stunning blow
 * wait for its stun beat (after the crit's X) and drop in, and a stun outside a blow shows at once;
 * none of it draws the game's Random. The owner's call stands (rt2/design/owner-overrides.md): the
 * echo, Shocking and every proc look are left as they were.
 */
public class CalmLooksTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private EnchantFxTest.Rig rig;

	@Before
	public void setUp(){
		rig = new EnchantFxTest.Rig();
		rig.up();
		Game.elapsed = 1 / 100f;
		HitCard.reset();
		HealFx.reset();
		Fx.air();
		Fx.glowAir();
	}

	@After
	public void tearDown(){
		HitCard.reset();
		HealFx.reset();
		rig.down();
	}

	// ------------------------------------------------------------------ drains

	//a blow of a on d healing a by 5: a lead proc of a look (or none), then a Vampiric proc at a tier
	//with its twist, flags as given; the frames until the heal shows (its time from the blow), or -1
	//within `most` s, with the beads that went home (born at a hit point, dying at a's middle)
	private float[] drainBlow( CharSprite a, CharSprite d, EnchantLook lead, int tier, int flags, float most ){
		return drainBlow( a, d, () -> {
			if (lead != null) EnchantFx.proc( lead, a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
			EnchantFx.proc( EnchantLooks.of( Vampiric.class ), a.ch, d.ch, EnchantFx.strengthOf( tier ), 0, flags, ShadowRows.VAMPIRIC );
		}, most );
	}

	//the same with the blow's procs as given
	private float[] drainBlow( CharSprite a, CharSprite d, Runnable procs, float most ){
		Map<Gizmo, float[]> seen = new IdentityHashMap<>();
		IdentityHashMap<Gizmo, Boolean> now = new IdentityHashMap<>();
		float t0 = Game.timeTotal;
		Blow.begin( a.ch, d.ch );
		procs.run();
		assertTrue( Blow.claim( a.ch, "5", CharSprite.POSITIVE, FloatingText.HEALING ) );
		Blow.end();
		float sx = a.x + a.width() / 2f, sy = a.y + a.height() / 2f;
		float healAt = -1f, beadsHome = -1f;
		int beads = 0;
		for (int f = 0; f < Math.round( most / Game.elapsed ); f++){
			rig.frames( 1, true );
			if (healAt < 0 && HealFx.heals() > 0) healAt = Game.timeTotal - t0;
			now.clear();
			rig.alive( now );
			List<float[]> gone = new ArrayList<>();
			for (Gizmo g : now.keySet()){
				if (!(g instanceof FxParticle)) continue;
				FxParticle p = (FxParticle) g;
				float[] r = seen.get( g );
				//a particle the kit reused within the frame (its life begun again) died as it was
				if (r != null && p.progress() < r[4]){
					gone.add( r );
					r = null;
				}
				if (r == null) seen.put( g, new float[]{ p.x, p.y, p.x, p.y, p.progress() } );
				else { r[2] = p.x; r[3] = p.y; r[4] = p.progress(); }
			}
			for (java.util.Iterator<Map.Entry<Gizmo, float[]>> it = seen.entrySet().iterator(); it.hasNext(); ){
				Map.Entry<Gizmo, float[]> e = it.next();
				if (now.containsKey( e.getKey() )) continue;
				gone.add( e.getValue() );
				it.remove();
			}
			for (float[] r : gone){
				//a bead: born well away from the wielder, gone at his middle (an accent's moved with its
				//beat, a few pixels up and aside)
				boolean home = Math.abs( r[2] - sx ) <= 3f && Math.abs( r[3] - sy ) <= 5f;
				boolean far = Math.abs( r[0] - sx ) > 6f;
				if (home && far){
					beads++;
					beadsHome = Math.max( beadsHome, Game.timeTotal - t0 );
				}
			}
		}
		return new float[]{ healAt, beadsHome, beads };
	}

	@Test
	public void aLeadingDrainsHealShowsWhenItsLastBeadLands(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		float[] r = drainBlow( a, d, null, EnchantFx.MID, 0, 1.2f );
		assertTrue( "beads went home: " + r[2], r[2] >= 1 );
		//a lead's beads as its beat drew them (the tier's quads leave the line what the rest did not take)
		assertEquals( "the heal comes as the last bead lands", r[1], r[0], 0.021f );
		assertTrue( "within the plan", r[0] <= DrainFx.landing( DrainFx.BEADS[EnchantFx.MID] ) + 0.011f );
		assertTrue( "the card on the wielder", EnchantState.get( a ).gains[0] != null );
		assertEquals( 5, EnchantState.get( a ).gains[0].number() );
		assertEquals( "the heal's own look, once", 1, HealFx.heals() );
	}

	@Test
	public void aStrongVampiricsHealWaitsForItsThreeBeadsMore(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		float[] r = drainBlow( a, d, null, EnchantFx.STRONG, 0, 1.5f );
		//the three more leave MORE_AT s after the line: they land last
		assertTrue( "the more came: " + r[1], r[1] > DrainFx.MORE_AT + DrainFx.LINE_FOR - 0.011f );
		assertEquals( "the heal comes as the last of them lands", r[1], r[0], 0.021f );
	}

	@Test
	public void aSecondDrainsHealCountsFromItsOwnBeat(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		//a strong second (as the drain-landing-second preview stages it) draws its beads on its
		//accent's beat: the heal waits for the last of them
		float[] r = drainBlow( a, d, EnchantLooks.of( Blazing.class ), EnchantFx.STRONG, 0, 1.6f );
		assertTrue( "beads went home: " + r[2], r[2] > 0 );
		assertEquals( "the heal as its last bead lands", r[1], r[0], 0.021f );
		assertTrue( "after its beat and its line: " + r[0], r[0] > BlowPresenter.BEAT + DrainFx.LINE_FOR - 0.011f );
	}

	@Test
	public void aSecondDrawingNoBeadsHealsInFullOnItsBeat(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		//a mid second's line finds no quads left: nothing carried, so the heal on its beat, in full
		float[] r = drainBlow( a, d, EnchantLooks.of( Blazing.class ), EnchantFx.MID, 0, 1.2f );
		assertEquals( "no bead home", 0f, r[2], 0f );
		assertEquals( "on its beat: " + r[0], BlowPresenter.BEAT, r[0], 0.021f );
	}

	@Test
	public void aresLeechsHealWaitsForTheBeadsOfAFoeOtherThanTheStruckOne(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), other = rig.rat( at( 3, 6 ) );
		//the struck one too low to drain, another foe drained: its line is drawn on its own (no proc
		//leads on the struck one), and the heal still waits for its beads
		float[] r = drainBlow( a, d, () -> ShadowRows.aresLeech( EnchantLooks.of( AresLeech.class ), a.ch,
				Arrays.asList( other.ch ), EnchantFx.strengthOf( EnchantFx.MID ), 0 ), 1.4f );
		assertTrue( "beads went home: " + r[2], r[2] > 0 );
		assertEquals( "the heal as the last bead lands", r[1], r[0], 0.021f );
		assertTrue( "not on the hit frame: " + r[0], r[0] > DrainFx.LINE_FOR - 0.011f );
	}

	@Test
	public void aresLeechsHealWaitsForEveryFoeItDrains(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), other = rig.rat( at( 3, 6 ) );
		//the struck one and another foe drained: the struck one's line leads, the other's is drawn on its own
		float[] r = drainBlow( a, d, () -> ShadowRows.aresLeech( EnchantLooks.of( AresLeech.class ), a.ch,
				Arrays.asList( other.ch, d.ch ), EnchantFx.strengthOf( EnchantFx.MID ), 0 ), 1.4f );
		assertTrue( "beads went home: " + r[2], r[2] > 0 );
		assertEquals( "the heal as the last bead lands", r[1], r[0], 0.021f );
	}

	@Test
	public void aDrainDrawingNoLineHomeHealsAtOnce(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		//a target past the rich ones plays its minimal star: no beads, no wait
		float[] r = drainBlow( a, d, null, EnchantFx.MID, EnchantFx.multi( EnchantFx.RICH_TARGETS ), 0.6f );
		assertTrue( "at once: " + r[0], r[0] >= 0 && r[0] <= 0.021f );
		assertEquals( "no bead home", 0f, r[2], 0f );
	}

	@Test
	public void aWardsDrainAndEveryOtherFamilyHoldNothing(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		Blow.begin( a.ch, d.ch );
		EnchantFx.ward( EnchantLooks.of( Siphoning.class ), d.ch, a.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		EnchantFx.proc( EnchantLooks.of( Blazing.class ), a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		assertTrue( Blow.claim( a.ch, "5", CharSprite.POSITIVE, FloatingText.HEALING ) );
		Blow.end();
		rig.frames( 2, true );
		assertEquals( "the heal at once", 1, HealFx.heals() );
	}

	@Test
	public void aDrainOnABlowDrawsNothingOfTheGamesRandom(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) );
		assertEquals( 0, draws( () -> drainBlow( a, d, EnchantLooks.of( Blazing.class ), EnchantFx.STRONG, 0, 1.2f ) ) );
	}

	// ------------------------------------------------------------------ the stun stars

	//Looks.stunLift (package-private in combat): how far above their orbit the stars are, -1 waiting
	private static float lift( CharSprite s ){
		try {
			Class<?> looks = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Looks" );
			Method m = looks.getDeclaredMethod( "stunLift", CharSprite.class );
			m.setAccessible( true );
			return (float) m.invoke( null, s );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void aStunningBlowsStarsWaitForTheirBeatAndDropIn(){
		CharSprite a = rig.rat( at( 5, 6 ) ), d = rig.rat( at( 6, 6 ) ), other = rig.rat( at( 2, 2 ) );
		//a stun put on once before (the classes loaded), then taken off
		Buff.affect( other.ch, Paralysis.class, 5f );
		Buff.detach( other.ch, Paralysis.class );
		float t0 = Game.timeTotal;
		Blow.begin( a.ch, d.ch );
		//a proc leads, a crit's X takes the first beat, the stun the next
		EnchantFx.proc( EnchantLooks.of( Blazing.class ), a.ch, d.ch, EnchantFx.strengthOf( EnchantFx.MID ), 0, 0 );
		PieceFx.crit( a.ch, d.ch );
		assertEquals( "the stun on a blow draws nothing of the game's Random", 0,
				draws( () -> Buff.affect( d.ch, Paralysis.class, 5f ) ) );
		Blow.claim( d.ch, "6", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		assertEquals( "nor its presenting", 0, draws( Blow::end ) );
		rig.frames( 3, true );
		assertTrue( "the stun is on", d.stunStars() );
		assertEquals( "the stars wait", -1f, lift( d ), 0f );
		float beat = 2 * BlowPresenter.BEAT;
		rig.frames( Math.round( (beat - (Game.timeTotal - t0)) / Game.elapsed ) - 2, true );
		assertEquals( "still waiting before the stun's beat", -1f, lift( d ), 0f );
		rig.frames( 4, true );
		float l = lift( d );
		assertTrue( "dropping in on its beat: " + l, l > 0f && l <= 4f );
		rig.frames( Math.round( 0.1f / Game.elapsed ) + 1, true );
		assertEquals( "in their orbit", 0f, lift( d ), 0f );
		Buff.detach( d.ch, Paralysis.class );
		rig.frames( 30, true );
		assertFalse( d.stunStars() );
	}

	@Test
	public void aStunOutsideABlowShowsAtOnceAndCirclingStarsAreLeftBe(){
		CharSprite d = rig.rat( at( 6, 6 ) ), a = rig.rat( at( 5, 6 ) );
		Buff.affect( d.ch, Paralysis.class, 5f );
		rig.frames( 2, true );
		assertTrue( d.stunStars() );
		assertEquals( "at once", 0f, lift( d ), 0f );
		//a stun again inside a blow while they circle: they stay as they are
		Blow.begin( a.ch, d.ch );
		Buff.affect( d.ch, Paralysis.class, 5f );
		Blow.claim( d.ch, "6", CharSprite.NEGATIVE, FloatingText.PHYS_DMG );
		Blow.end();
		rig.frames( 2, true );
		assertEquals( "left be", 0f, lift( d ), 0f );
	}
}
