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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AvatarOfLightHalo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChargedShotDraw;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DoubleStab;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillFlight;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The links package of the second pass (rfx plan.md 5.3: Shadow Link and the other links, drains,
 * rays, lasers and light trails):
 *
 *  - the game's Random: each touched moment (RfxLinksProcs) draws it as often as f0783eacd did, with
 *    the same next value; every touched file keeps f0783's census of Random calls and of the pictures
 *    that draw it; the new pictures draw none;
 *  - frames: Shadow Link bites on the ethereal chain's frame, a spirit arrow and a holy blade land
 *    on the frame their flight always did (30, 60, 144 fps; the orb's impact is SkillsFxTest's);
 *  - the bond: drawn out of BOTH ends, never paler than the shadow's A070E0 but the bite's one-frame
 *    star; the held pair let go of with their marks; a lone link's ring, never a target;
 *  - meaning: the mends show the heal's crosses only when HP rose; the palette's colours by meaning;
 *  - caps in a loud moment on a phone's numbers, nothing left behind, no GPU while made, and none of
 *    the game's looks borrowed (the rope, the target, a chain, a beam, a missile, a star, a puddle).
 */
public class SfxRfxLinksTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private DreadShadowProcs t;

	@Before
	public void setUp() throws Exception {
		t = new DreadShadowProcs();
		t.up();
		Game.elapsed = 1 / 60f;
		LinkFX.reset();
		RuneFX.reset();
		SkillRay.reset();
		SkillBolt.reset();
	}

	@After
	public void tearDown() throws Exception {
		t.down();
	}

	private Group layer( GameScene.Layer l ){
		return t.rig.layers[l.ordinal()];
	}

	// ------------------------------------------------------------------ the game's Random

	/** f0783eacd's draws and next value (x 1e6), measured by RfxLinksProcs in a detached checkout of
	 *  it (RFX_LINKS_BASELINE=1). */
	static final Object[][] BASELINE = {
			{ "charged-shot", 58, 670722 }, { "charged-draw", 14, 2120 }, { "avatar-halo", 18, 660644 },
			{ "transcendence", 20, 207127 }, { "true-edge", 30, 967827 }, { "cinder-launch", 0, 477554 },
			{ "shards-heal", 0, 477554 }, { "linked-cut", 0, 477554 }, { "soul-field", 0, 477554 } };

	@Test
	public void everyMomentDrawsTheGamesRandomAsF0783Did() throws Exception {
		t.down();
		LinkedHashMap<String, long[]> now = RfxLinksProcs.measure();
		t.up();
		assertEquals( BASELINE.length, now.size() );
		for (Object[] row : BASELINE){
			long[] n = now.get( (String) row[0] );
			assertNotNull( (String) row[0], n );
			assertEquals( row[0] + ": draws", (long) (Integer) row[1], n[0] );
			assertEquals( row[0] + ": the next value", (long) (Integer) row[2], n[1] );
		}
	}

	/** Every file the package touched: its Random calls and the pictures that draw it, at f0783eacd. */
	static final Object[][] CENSUS = {
			{ "actors/hero/skills/DoubleStab.java", 0, 4 }, { "effects/skillfx/DreadFX.java", 0, 0 },
			{ "effects/SkillMarkFX.java", 0, 0 }, { "actors/hero/skills/SoulDrain.java", 0, 3 },
			{ "actors/hero/skills/PanicHarvest.java", 0, 6 }, { "actors/hero/skills/SoulDetonation.java", 0, 2 },
			{ "actors/hero/skills/DarkPact.java", 0, 4 }, { "actors/hero/skills/Venom.java", 1, 2 },
			{ "actors/hero/skills/ChargedShot.java", 0, 1 }, { "actors/buffs/ChargedShotDraw.java", 0, 1 },
			{ "effects/skillfx/SkillFlight.java", 0, 0 }, { "actors/buffs/AvatarOfLightHalo.java", 0, 0 },
			{ "actors/hero/skills/Transcendence.java", 2, 4 }, { "actors/hero/skills/HolyCharge.java", 0, 4 },
			{ "effects/skillfx/StreakFX.java", 0, 0 }, { "actors/hero/skills/TrueEdge.java", 2, 0 },
			{ "effects/ElementalOrbitFX.java", 0, 0 }, { "actors/hero/skills/SkillInteractions.java", 0, 0 } };

	@Test
	public void everyTouchedFileKeepsItsDrawSites() throws Exception {
		for (Object[] row : CENSUS){
			String src = SfxDreadShadowTest.source( (String) row[0] );
			assertEquals( row[0] + ": the game's Random", (int) (Integer) row[1], matches( SfxDreadShadowTest.RANDOM, src ) );
			assertEquals( row[0] + ": its pictures that draw it", (int) (Integer) row[2], matches( SfxDreadShadowTest.DRAWS, src ) );
		}
	}

	//three rats for makeEverything (made before any count: a mob's making draws the game's Random)
	private Rat[] rats(){
		Rat ally = t.rat( t.at( 3, 3 ) );
		ally.alignment = Char.Alignment.ALLY;
		ally.HP = 10;
		return new Rat[]{ t.rat( t.at( 5, 3 ) ), t.rat( t.at( 9, 6 ) ), ally };
	}

	//every new picture of the package once
	private void makeEverything( Rat[] r ){
		CharSprite hs = Dungeon.hero.sprite;
		Rat a = r[0], b = r[1];
		DreadFX.tether( t.rig.stage, a.sprite.center(), b.sprite.destinationCenter(), null );
		DoubleStab.link( a, b, 3 );
		DreadFX.pact( hs );
		DreadFX.coat( hs, SkillPalette.POISON.stop( 0 ) );
		DreadFX.madness( b.sprite );
		ChargedShot.release( hs, t.at( 12, 4 ) );
		ChargedShotDraw.AimLine.show( Arrays.asList( t.at( 4, 4 ), t.at( 5, 4 ), t.at( 6, 4 ) ) );
		SkillFlight.fly( t.rig.stage, 50, 70, 150, 70, SkillFlight.SPIRIT_ARROW, 240, null );
		SkillFlight.fly( t.rig.stage, 50, 90, 150, 50, SkillFlight.HOLY_BLADE, 240, null );
		StreakFX.dash( Dungeon.hero.pos, t.at( 9, 4 ), 0xF1D99C, 0.45f, 0.3f, true, StreakFX.MOTES );
		StreakFX.show( Dungeon.hero.pos, t.at( 9, 5 ), 0xB9DEEE, 0.45f, 0.25f, StreakFX.SPARKS );
		SkillRay.show( hs, a.sprite, SkillRay.HOLY, AvatarOfLightHalo.SEAR_RAY );
		SkillBolt.fly( t.rig.stage, b.pos, Dungeon.hero.pos, SkillBolt.SOUL, null ).peel( b.sprite, 6 );
		ElementalOrbitFX.launch( Dungeon.hero, a.pos, 0, 2, () -> {} );
		ElementalOrbitFX.launch( Dungeon.hero, b.pos, 1, 2, () -> {} );
		SkillInteractions.Mark m = SkillInteractions.mark( b, SkillInteractions.Mark.SHARDS, 2, 5 );
		m.power = 6;
		m.act();
	}

	@Test
	public void theNewPicturesDrawNothingOfTheGamesRandomButTheBoltsPour(){
		//the one SkillBolt.fly pours once (MagicMissile's draw): everything else draws none
		Rat[] r = rats();
		assertEquals( 1, EnchantFxTest.draws( () -> {
			makeEverything( r );
			t.rig.frames( 120, true );
		} ) );
	}

	// ------------------------------------------------------------------ the frames the game waits on

	//the frame (1 = the first update) a callback comes on, at most `max`
	private int arrival( java.util.function.Consumer<Runnable> start, int max ){
		final int[] at = { -1 }, frame = { 0 };
		start.accept( () -> { if (at[0] < 0) at[0] = frame[0]; } );
		for (frame[0] = 1; frame[0] <= max && at[0] < 0; frame[0]++) t.rig.frames( 1, true );
		return at[0];
	}

	@Test
	public void theCallbacksKeepTheirFrames(){
		Group stage = t.rig.stage;
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			//Shadow Link: the chain's frame
			PointF a = new PointF( 40, 60 ), b = new PointF( 130, 20 );
			int chain = arrival( cb -> stage.add( new Chains( new PointF( a ), new PointF( b ), Effects.Type.ETHEREAL_CHAIN, cb::run ) ), 600 );
			assertEquals( "shadow link, dt " + dt, chain, arrival( cb -> DreadFX.tether( stage, a, b, cb::run ), 600 ) );
			//a spirit arrow and a holy blade land when a flight with nothing drawn would
			int plain = arrival( cb -> SkillFlight.fly( stage, 40, 60, 170, 90, SkillFlight.SHOCK, 240, cb::run ), 600 );
			assertEquals( "spirit arrow, dt " + dt, plain, arrival( cb -> SkillFlight.fly( stage, 40, 60, 170, 90, SkillFlight.SPIRIT_ARROW, 240, cb::run ), 600 ) );
			assertEquals( "holy blade, dt " + dt, plain, arrival( cb -> SkillFlight.fly( stage, 40, 60, 170, 90, SkillFlight.HOLY_BLADE, 240, cb::run ), 600 ) );
			//(a Cinder Trail orb's impact frame: SkillsFxTest holds it against the old code)
			t.rig.frames( 60, true );
		}
	}

	// ------------------------------------------------------------------ the bond

	@Test
	public void shadowLinkPullsFromBothEndsAndNeverTurnsWhite() throws Exception {
		Group lights = layer( GameScene.Layer.LIGHTS ), effects = layer( GameScene.Layer.EFFECTS );
		PointF a = new PointF( 30, 40 ), b = new PointF( 150, 100 );
		float kx = (a.x + b.x) / 2f, ky = (a.y + b.y) / 2f + SkillMarkFX.sag( (float) Math.hypot( b.x - a.x, b.y - a.y ) );
		final boolean[] bitten = { false };
		DreadFX.tether( t.rig.stage, a, b, () -> bitten[0] = true );
		Field shown = FxParticle.class.getDeclaredField( "shown" );
		shown.setAccessible( true );
		int fromA = 0, fromB = 0, pale = 0;
		for (int f = 0; f < 90; f++){
			t.rig.frames( 1, true );
			for (Group g : new Group[]{ lights, effects }){
				for (FxParticle p : particles( g )){
					float px = p.px, py = p.py;
					boolean toKnot = p.vx * (kx - px) + p.vy * (ky - py) > 0;
					if (!bitten[0] && toKnot && Math.hypot( px - a.x, py - a.y ) < 8) fromA++;
					if (!bitten[0] && toKnot && Math.hypot( px - b.x, py - b.y ) < 8) fromB++;
					if (!p.isLight() || isStar( (int[]) shown.get( p ) )) continue;
					int c = p.colorNow();
					//never paler than the shadow's light A070E0 (channel by channel)
					if (((c >> 16) & 0xFF) > 0xA0 || ((c >> 8) & 0xFF) > 0x70 || (c & 0xFF) > 0xE0) pale++;
				}
			}
		}
		assertTrue( bitten[0] );
		assertTrue( "pulled out of the first: " + fromA, fromA >= 6 );
		assertTrue( "pulled out of the second: " + fromB, fromB >= 6 );
		assertEquals( "light paler than A070E0", 0, pale );
		assertEquals( 0, LinkFX.live() );
	}

	/** The two currents and the kit's motes are additive light: two of them on one pixel add up past
	 *  the shadow's light to white (the preview showed 3x3 white squares mid-bond where the stream
	 *  rode the bind's own chords). Painted as the light layer adds them, no pixel of the bond's motes
	 *  may hold more green than one mote at its brightest and a half while it gathers. */
	@Test
	public void shadowLinksCurrentsNeverStackIntoWhite() throws Exception {
		Group lights = layer( GameScene.Layer.LIGHTS );
		Field shown = FxParticle.class.getDeclaredField( "shown" );
		shown.setAccessible( true );
		java.util.List<int[]> motes = Arrays.asList( FxFrames.MOTE_1, FxFrames.MOTE_2, FxFrames.SQ_3 );
		for (PointF[] ends : new PointF[][]{ { new PointF( 30, 40 ), new PointF( 150, 100 ) },
				{ new PointF( 60, 30 ), new PointF( 80, 130 ) }, { new PointF( 40, 60 ), new PointF( 72, 60 ) } }){
			final boolean[] bitten = { false };
			DreadFX.tether( t.rig.stage, ends[0], ends[1], () -> bitten[0] = true );
			int stacked = 0, frames = 0;
			for (int f = 0; f < 60 && !bitten[0]; f++){
				t.rig.frames( 1, true );
				//the bite's one frame (its star, the motes racing home) is a flash, as LinkFX draws it
				if (bitten[0]) break;
				frames++;
				java.util.HashMap<Long, Float> green = new java.util.HashMap<>();
				for (FxParticle p : particles( lights )){
					int[] fr = (int[]) shown.get( p );
					if (!p.isLight() || !motes.contains( fr )) continue;
					float g = ((p.colorNow() >> 8) & 0xFF) * p.am;
					int x0 = Math.round( p.px - fr[2] / 2f ), y0 = Math.round( p.py - fr[3] / 2f );
					for (int x = x0; x < x0 + fr[2]; x++) for (int y = y0; y < y0 + fr[3]; y++)
						green.merge( ((long) x << 32) ^ (y & 0xFFFFFFFFL), g, Float::sum );
				}
				for (float g : green.values()) if (g > 0x70 * 1.5f) stacked++;
			}
			assertTrue( frames > 10 );
			assertEquals( "pixels stacked past the shadow's light, from " + ends[0].x + "," + ends[0].y + " to " + ends[1].x + "," + ends[1].y, 0, stacked );
			t.rig.frames( 40, true );
		}
	}

	/** Past LinkFX.CAP a bind draws only its thread at the bite: Shadow Link's own stream keeps to
	 *  that cap too (nothing gathers where no knot will be), and the bite keeps its frame. */
	@Test
	public void pastTheBondsCapNoStreamGathers(){
		Group lights = layer( GameScene.Layer.LIGHTS ), effects = layer( GameScene.Layer.EFFECTS );
		//the cap filled by bonds far out of sight (counted, drawing nothing)
		for (int i = 0; i < LinkFX.CAP; i++) LinkFX.bind( t.rig.stage, new PointF( -900, -900 ), new PointF( -700, -900 ), LinkFX.SHADOW, null );
		assertEquals( LinkFX.CAP, LinkFX.live() );
		final boolean[] bitten = { false };
		DreadFX.tether( t.rig.stage, new PointF( 30, 40 ), new PointF( 150, 100 ), () -> bitten[0] = true );
		int drawn = 0;
		for (int f = 0; f < 90 && !bitten[0]; f++){
			t.rig.frames( 1, true );
			if (!bitten[0]) drawn += particles( lights ).size() + particles( effects ).size();
		}
		assertTrue( bitten[0] );
		assertEquals( "particles gathering past the cap", 0, drawn );
	}

	private static boolean isStar( int[] f ){
		for (int[] s : FxFrames.IMPACT_STAR) if (s == f) return true;
		return false;
	}

	@Test
	public void theHeldPairGoesWithItsMarksAndALoneLinkIsARingNeverATarget(){
		Group lights = layer( GameScene.Layer.LIGHTS ), floor = layer( GameScene.Layer.FLOOR );
		Rat a = t.rat( t.at( 5, 3 ) ), b = t.rat( t.at( 8, 5 ) );
		DoubleStab.link( a, b, 3 );
		t.rig.frames( 10, true );
		assertEquals( "one bond for the pair", 1, countClass( lights, LinkFX.class.getName() + "$Held" ) );
		assertEquals( 1, LinkFX.live() );
		//a few seconds: its motes fed from both bodies, its glint at the middle, nothing piling up
		t.rig.frames( 240, true );
		assertEquals( 1, countClass( lights, LinkFX.class.getName() + "$Held" ) );
		DoubleStab.clearLinks();
		t.rig.frames( 5, true );
		assertEquals( "let go of with its marks", 0, countClass( lights, LinkFX.class.getName() + "$Held" ) );
		assertEquals( 0, LinkFX.live() );
		//the focus: one enemy linked to itself shows a ring of shadow at its feet, no bond
		Rat c = t.rat( t.at( 6, 6 ) );
		DoubleStab.link( c, c, 3 );
		t.rig.frames( 10, true );
		assertEquals( 0, LinkFX.live() );
		int rings = 0;
		for (Gizmo g : floor.membersView()){
			if (g instanceof FxPic && g.visible && Math.round( ((FxPic) g).width ) == FxFrames.RING_13[2]) rings++;
		}
		assertEquals( "the focus's ring", 1, rings );
		DoubleStab.clearLinks();
		t.rig.frames( 5, true );
		for (Gizmo g : floor.membersView()) assertFalse( "its ring let go of", g instanceof FxPic && g.exists && g.visible
				&& Math.round( ((FxPic) g).width ) == FxFrames.RING_13[2] );
	}

	// ------------------------------------------------------------------ meaning

	@Test
	public void theMendsShowTheHealOnlyWhenHealthRose(){
		Rat foe = t.rat( t.at( 4, 4 ) );
		Rat ally = t.rat( t.at( 3, 3 ) );
		ally.alignment = Char.Alignment.ALLY;
		//Avatar of Light at +3, its ally hurt: the heal's red crosses
		ally.HP = 10;
		AvatarOfLightHalo h = Buff.affect( Dungeon.hero, AvatarOfLightHalo.class );
		h.set( 3, 5 );
		h.act();
		assertTrue( "crosses on a mend", crossesWithin( 20 ) > 0 );
		//whole: no crosses
		t.rig.frames( 120, true );
		ally.HP = ally.HT;
		h.act();
		assertEquals( "no crosses with no health gained", 0, crossesWithin( 20 ) );
		//a shards mark mending an ally beside it
		t.rig.frames( 120, true );
		Rat near = t.rat( t.at( 5, 4 ) );
		near.alignment = Char.Alignment.ALLY;
		near.HP = 10;
		SkillInteractions.Mark m = SkillInteractions.mark( foe, SkillInteractions.Mark.SHARDS, 2, 5 );
		m.power = 6;
		m.act();
		assertTrue( "crosses on the shards' mend", crossesWithin( 20 ) > 0 );
	}

	//the heal's crosses (PLUS_5 of the kit, lit the heal's red) alive at most over n frames
	private int crossesWithin( int n ){
		int most = 0;
		try {
			Field shown = FxParticle.class.getDeclaredField( "shown" );
			shown.setAccessible( true );
			for (int f = 0; f < n; f++){
				t.rig.frames( 1, true );
				int k = 0;
				for (FxParticle p : particles( layer( GameScene.Layer.LIGHTS ) )){
					if (shown.get( p ) == FxFrames.PLUS_5 && ((p.colorNow() >> 16) & 0xFF) > 0xC0) k++;
				}
				most = Math.max( most, k );
			}
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
		return most;
	}

	@Test
	public void theColoursSayWhatHappens(){
		//the huntress's leaf green, never the heal's mint or the old cream
		assertEquals( SkillPalette.NATURE_LIGHT, ChargedShot.LIGHT );
		assertEquals( SkillPalette.NATURE_LIGHT, ChargedShotDraw.AimLine.COLOR );
		assertEquals( SkillPalette.NATURE_LIGHT, SkillFlight.SPIRIT );
		//madness and a cut crimson, never pink; the mage's soul its lavender-grey
		assertEquals( SkillPalette.BLOOD.stop( 1 ), DreadFX.MADNESS );
		assertEquals( SkillPalette.BLOOD.stop( 1 ), SkillMarkFX.CUT_COLOR );
		assertEquals( SkillPalette.SOUL_LIGHT, DreadFX.SOUL[2] );
		assertEquals( SkillPalette.SHADOW.stop( 1 ), SkillMarkFX.FOCUS_RING );
		for (int c : new int[]{ DreadFX.MADNESS, SkillMarkFX.CUT_COLOR }){
			int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
			assertTrue( "crimson: red over twice its blue", r > 2 * b && r > 2 * g );
		}
		//no light ramp of the package starts at white
		assertTrue( DreadFX.SOUL[0] != 0xFFFFFF );
	}

	@Test
	public void darkPactsSealIsWrittenInRunesAtTheirBrightest(){
		//a rune alone is a dot (RuneFX): the seal is an arc of runes a side, whole when they meet
		DreadFX.pact( Dungeon.hero.sprite );
		int meet = Math.round( DreadFX.PACT_MEET * 60 );
		t.rig.frames( meet, true );
		int shown = 0;
		float brightest = 0;
		for (FxPic p : runePics( layer( GameScene.Layer.LIGHTS ) )){
			shown++;
			brightest = Math.max( brightest, p.am );
		}
		assertTrue( "runes at the meet " + shown, shown >= 6 );
		assertTrue( "the seal fading as it is sealed " + brightest, brightest >= 0.9f );
	}

	//the visible rune pictures of every RuneFX ring in a group
	private static java.util.ArrayList<FxPic> runePics( Group g ){
		java.util.ArrayList<FxPic> out = new java.util.ArrayList<>();
		if (g == null) return out;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof RuneFX.Runes){
				for (Gizmo p : ((Group) m).membersView()) if (p instanceof FxPic && p.exists && p.visible) out.add( (FxPic) p );
			} else if (m instanceof Group) out.addAll( runePics( (Group) m ) );
		}
		return out;
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void aLoudMomentStaysUnderEveryCap() throws Exception {
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		try {
			CharSprite hs = Dungeon.hero.sprite;
			Rat[] r = new Rat[8];
			for (int i = 0; i < r.length; i++) r[i] = t.rat( t.at( 5 + (i % 4) * 2, 2 + (i / 4) * 5 ) );
			//two binds, two held pairs, three rays, two souls flowing home, two peeled bolts
			DreadFX.tether( t.rig.stage, r[0].sprite.center(), r[3].sprite.center(), null );
			DreadFX.tether( t.rig.stage, r[4].sprite.center(), r[7].sprite.center(), null );
			DoubleStab.link( r[0], r[1], 3 );
			SkillInteractions.mark( r[2], SkillInteractions.Mark.CUT, 3, 8 ).other = r[3].id();
			SkillInteractions.mark( r[3], SkillInteractions.Mark.CUT, 3, 8 ).other = r[2].id();
			for (int i = 0; i < 3; i++) SkillRay.show( hs, r[4 + i].sprite, SkillRay.HOLY, 0.4f );
			SkillBolt.fly( t.rig.stage, r[5].pos, Dungeon.hero.pos, SkillBolt.SOUL, null ).peel( r[5].sprite, 6 );
			SkillBolt.fly( t.rig.stage, r[6].sprite.center(), hs.center(), SkillBolt.MANA, null ).peel( r[6].sprite, 4 );
			SkillBolt.mote( r[1].sprite, hs, SkillBolt.SOUL, null ).peel( r[1].sprite, 6 );
			SkillBolt.mote( r[2].sprite, hs, SkillBolt.SOUL, null ).peel( r[2].sprite, 6 );
			int most = 0, lights = 0;
			for (int f = 0; f < 120; f++){
				t.rig.frames( 1, true );
				most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
				lights = Math.max( lights, FxLight.live() );
				assertTrue( LinkFX.live() <= LinkFX.CAP );
				assertTrue( SkillRay.full() <= SkillRay.FULL );
				assertTrue( RuneFX.live() <= RuneFX.CAP );
				assertTrue( PixelFlare.live() <= PixelFlare.CAP );
			}
			//(about 256 particles and 18 lights at their most)
			assertTrue( "particles " + most, most <= FxBudget.soft() );
			assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
			//let go of: the marks, then everything runs out
			DoubleStab.clearLinks();
			for (Rat x : r) for (SkillInteractions.Mark m : x.buffs( SkillInteractions.Mark.class )) m.detach();
			t.rig.frames( 600, true );
			assertEquals( 0, LinkFX.live() );
			assertEquals( 0, SkillRay.full() );
			assertEquals( 0, RuneFX.live() );
			assertEquals( 0, FxBudget.live( FxBudget.PARTICLES ) );
		} finally {
			scale.setFloat( null, desktop );
		}
	}

	@Test
	public void theStrokesExtrasKeepTheirCapsAndOldStrokesLookAsTheyDid(){
		t.rig.frames( 60, true );
		int p0 = FxBudget.live( FxBudget.PARTICLES );
		StreakFX.show( Dungeon.hero.pos, t.at( 14, 4 ), 0xB9DEEE, 0.45f, 0.25f );
		int plain = peak( p0, 30 );
		assertTrue( "a plain stroke: its dust and its tip " + plain, plain <= StreakFX.DUST_MAX + 1 );
		t.rig.frames( 60, true );
		p0 = FxBudget.live( FxBudget.PARTICLES );
		StreakFX.show( Dungeon.hero.pos, t.at( 14, 4 ), 0xF1D99C, 0.45f, 0.25f, StreakFX.MOTES | StreakFX.SPARKS );
		int both = peak( p0, 40 );
		assertTrue( "extras " + both, both > plain && both <= StreakFX.DUST_MAX + 1 + StreakFX.MOTES_MOST + StreakFX.SPARKS_MOST );
		//a light's core in flight: at most CORE_MOST, and its landing glint
		t.rig.frames( 60, true );
		p0 = FxBudget.live( FxBudget.PARTICLES );
		SkillFlight.fly( t.rig.stage, 30, 70, 230, 70, SkillFlight.SPIRIT_ARROW, 240, null );
		int core = peak( p0, 90 );
		assertTrue( "core " + core, core > 3 && core <= SkillFlight.CORE_MOST + 1 );		//a volley: only CORE_RICH of its lights at once carry a core (never a rain of streaks)
		t.rig.frames( 60, true );
		p0 = FxBudget.live( FxBudget.PARTICLES );
		for (int i = 0; i < 12; i++) SkillFlight.fly( t.rig.stage, 20 + 12 * i, 0, 20 + 12 * i, 150, SkillFlight.SPIRIT_ARROW, 400, null );
		int most = 0, volley = 0;
		for (int f = 0; f < 90; f++){
			t.rig.frames( 1, true );
			most = Math.max( most, SkillFlight.cored() );
			volley = Math.max( volley, FxBudget.live( FxBudget.PARTICLES ) - p0 );
		}
		assertEquals( SkillFlight.CORE_RICH, most );
		assertTrue( "volley " + volley, volley <= SkillFlight.CORE_RICH * (SkillFlight.CORE_MOST + 1) );
		assertEquals( 0, SkillFlight.cored() );
	}

	private int peak( int base, int frames ){
		int most = 0;
		for (int f = 0; f < frames; f++){
			t.rig.frames( 1, true );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) - base );
		}
		return most;
	}

	@Test
	public void venomDripsAndIsGoneAtTheFeet(){
		CharSprite hs = Dungeon.hero.sprite;
		float feet = hs.y + hs.height() - 2;
		DreadFX.coat( hs, SkillPalette.POISON.stop( 0 ) );
		int drops = 0;
		for (int f = 0; f < 60; f++){
			t.rig.frames( 1, true );
			for (FxParticle p : particles( layer( GameScene.Layer.EFFECTS ) )){
				drops++;
				assertTrue( "under the feet: " + p.y, p.y <= feet + 1 );
			}
		}
		assertTrue( drops > 0 );
	}

	// ------------------------------------------------------------------ the GPU, borrowed looks

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		Rat[] r = rats();
		GL20 saved = Gdx.gl;
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, args) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
		try {
			makeEverything( r );
			t.rig.frames( 40, true );
		} finally {
			Gdx.gl = Gdx.gl20 = saved;
		}
	}

	@Test
	public void noneOfTheGamesLooksIsBorrowed() throws Exception {
		String[] files = { "actors/hero/skills/DoubleStab.java", "actors/hero/skills/SoulDrain.java", "actors/hero/skills/PanicHarvest.java",
				"actors/hero/skills/SoulDetonation.java", "actors/hero/skills/DarkPact.java", "actors/hero/skills/Venom.java",
				"actors/hero/skills/ChargedShot.java", "actors/hero/skills/Transcendence.java", "actors/hero/skills/HolyCharge.java",
				"actors/hero/skills/TrueEdge.java", "actors/hero/skills/SkillInteractions.java", "actors/buffs/AvatarOfLightHalo.java",
				"actors/buffs/ChargedShotDraw.java", "effects/skillfx/DreadFX.java", "effects/skillfx/SkillFlight.java",
				"effects/skillfx/StreakFX.java", "effects/ElementalOrbitFX.java" };
		for (String f : files){
			String code = code( SfxDreadShadowTest.source( f ) );
			for (String b : new String[]{ "Beam.", "MagicMissile", "new Chains", "ETHEREAL_CHAIN", "Speck.STAR", "new Flare(", "Icons.TARGET" }){
				assertFalse( f + " uses " + b, code.contains( b ) );
			}
		}
		//Shadow Link's cut: no grappling rope, no target, no pink; the CHAIN mark keeps Bear Trap's rope
		String mark = code( SfxDreadShadowTest.source( "effects/SkillMarkFX.java" ) );
		for (String b : new String[]{ "Icons.", "0xD66AAB", "0xD94969", "0xFFB0D0" }) assertFalse( "SkillMarkFX uses " + b, mark.contains( b ) );
		assertEquals( 1, matches( Pattern.compile( "Effects\\.Type\\.ROPE" ), mark ) );
		assertTrue( mark.contains( "chain=kind==SkillInteractions.Mark.CHAIN;" ) );
		//Venom: drops, never a puddle on the floor (a toxic hazard)
		String dread = code( SfxDreadShadowTest.source( "effects/skillfx/DreadFX.java" ) );
		String coat = dread.substring( dread.indexOf( "public static void coat" ) );
		assertFalse( coat.contains( "SPLAT" ) || coat.contains( "FxDecal" ) || coat.contains( "pool(" ) );
		assertFalse( code( SfxDreadShadowTest.source( "actors/hero/skills/Venom.java" ) ).contains( "SPLAT" ) );
		//Soul Detonation heals no one: no heal's picture on it
		String det = code( SfxDreadShadowTest.source( "actors/hero/skills/SoulDetonation.java" ) );
		assertFalse( det.contains( "SOUL_HEAL" ) || det.contains( "crosses" ) || det.contains( "HEALING" ) );
		//the madness runes and the terror star start crimson, never the old pink
		assertFalse( dread.contains( "0xF08A9E" ) || dread.contains( "0xCC3355" ) );
	}

	// ------------------------------------------------------------------ helpers

	private static java.util.ArrayList<FxParticle> particles( Group g ){
		java.util.ArrayList<FxParticle> out = new java.util.ArrayList<>();
		collect( g, out );
		return out;
	}

	private static void collect( Group g, java.util.ArrayList<FxParticle> out ){
		if (g == null) return;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive || !m.visible) continue;
			if (m instanceof FxParticle) out.add( (FxParticle) m );
			if (m instanceof Group) collect( (Group) m, out );
		}
	}

	private static int countClass( Group g, String name ){
		if (g == null) return 0;
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m.getClass().getName().equals( name )) n++;
			if (m instanceof Group) n += countClass( (Group) m, name );
		}
		return n;
	}

	private static String code( String src ){
		return src.replaceAll( "(?s)/\\*.*?\\*/", "" ).replaceAll( "//[^\n]*", "" );
	}

	private static int matches( Pattern p, String s ){
		Matcher m = p.matcher( s );
		int n = 0;
		while (m.find()) n++;
		return n;
	}
}
