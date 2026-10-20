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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Vanish;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkillDecoy;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DecoySprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MirrorSprite;
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

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Fear, shadow, the dead and the rogue's and huntress's tricks (docs/fx/skills.md, dread-shadow):
 *
 *  - every changed proc and cast, played for real headless (DreadShadowProcs), draws the game's
 *    Random as often as master (72b40617b) did, with the same next value after it;
 *  - every file touched calls the game's Random, and the pictures that draw it (a burst, a pour, a
 *    start, a bolt, a lost icon), as often as it did at master;
 *  - Shadow Link's tether (a LinkFX bind since the second pass) bites on the frame the ethereal
 *    chain it replaces bit (30, 60, 144 fps) and keeps to LinkFX's caps; the replaced bolts land on
 *    the frames of the MagicMissiles they replace (same points);
 *  - the shade decoy keeps its colour through a flash, pours its ink from an FxEmitter that draws
 *    nothing of the game's Random, stops it when it dies and lets it go (Fx.dispose) with the sprite;
 *  - DreadFX draws nothing of the game's Random, keeps to the kit's caps and leaves nothing behind;
 *    nothing touches the GPU while being made;
 *  - none of the package's skills borrows a look of the game's any more (the alarm's brackets, a
 *    curse's or a grim trap's shadow, a wand's bolt, the ethereal chain, a healing beam, a thrown
 *    item, gas, snow or flame on the hero, the legacy fan, a curse's or a gas's sound).
 */
public class SfxDreadShadowTest {

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
		//a fresh scene's counts (SkillsFx.sceneCreated resets them in the game)
		LinkFX.reset();
		RuneFX.reset();
	}

	@After
	public void tearDown() throws Exception {
		t.down();
	}

	// ------------------------------------------------------------------ the game's Random

	/** Master's (72b40617b) draws and next value (x 1e6) of each proc, measured by DreadShadowProcs
	 *  in a checkout of it (DREAD_BASELINE=1). */
	static final Object[][] BASELINE = {
			{ "demoralize", 18, 668809 }, { "lone-wolf", 7, 423783 }, { "dread-howl", 164, 546308 },
			{ "panic-harvest", 46, 863540 }, { "hex", 14, 517673 }, { "silent-death", 39, 358342 },
			{ "vanish", 27, 717650 }, { "dark-pact", 63, 519850 }, { "ambush", 26, 531400 },
			{ "shadow-link", 18, 668809 }, { "soul-drain", 23, 146320 }, { "master-thief", 18, 668809 },
			{ "necrotoxin", 136, 960404 }, { "venom", 47, 107420 }, { "aimed-shot", 47, 107420 },
			{ "double-shot", 49, 330386 }, { "bombvoyage", 26, 531400 }, { "stealth", 33, 938352 },
			{ "ninja-bomb", 398, 2363 }, { "blackout", 207, 928086 }, { "auras", 84, 458985 } };

	@Test
	public void everyProcDrawsTheGamesRandomAsMasterDid() throws Exception {
		t.down();
		LinkedHashMap<String, long[]> now = DreadShadowProcs.measure();
		t.up();
		assertEquals( BASELINE.length, now.size() );
		for (Object[] row : BASELINE){
			long[] n = now.get( (String) row[0] );
			assertNotNull( (String) row[0], n );
			assertEquals( row[0] + ": draws", (long) (Integer) row[1], n[0] );
			assertEquals( row[0] + ": the next value", (long) (Integer) row[2], n[1] );
		}
	}

	@Test
	public void vanishShowsTheGamesLostIconWhereTheMarksBurstWas(){
		//the vanish proc above is only a proof if the hunters do lose the trail
		Vanish v = DreadShadowProcs.skill( new Vanish() );
		t.hero.MP = t.hero.MT = 200;
		Rat r = t.rat( t.at( 5, 4 ) );
		r.state = r.HUNTING;
		r.aggro( t.hero );
		v.execute( t.hero, Skill.AC_CAST );
		assertTrue( "lost the trail", r.state == r.WANDERING );
		assertEquals( "its lost icon", 1, countScene() );
	}

	//the lost icons in the bare scene's emoicons group
	private static int countScene(){
		try {
			java.lang.reflect.Field s = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.class.getDeclaredField( "scene" );
			s.setAccessible( true );
			java.lang.reflect.Field e = xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.class.getDeclaredField( "emoicons" );
			e.setAccessible( true );
			return count( (Group) e.get( s.get( null ) ), xyz.gabriwar.warpedpixeldungeon.effects.EmoIcon.Lost.class );
		} catch (ReflectiveOperationException ex){
			throw new AssertionError( ex );
		}
	}

	/** Each touched file: its calls of the game's Random, and the pictures that draw it (a burst, a
	 *  pour, a start, a splash, a lightning, a bolt, a lost icon), at master (72b40617b). */
	static final Object[][] CENSUS = {
			{ "actors/hero/skills/Demoralize.java", 0, 2 },
			{ "actors/hero/skills/LoneWolf.java", 0, 3 },
			{ "actors/hero/skills/DreadHowl.java", 0, 5 },
			{ "actors/hero/skills/PanicHarvest.java", 0, 6 },
			//one flight, written as the two ways MagicMissile.boltFromChar took (2 at master)
			{ "actors/hero/skills/Hex.java", 0, 3 },
			{ "actors/hero/skills/SilentDeath.java", 0, 3 },
			//the mark's burst: the lost icon's draw and two more, as many as the burst made (0, 2 at master)
			{ "actors/hero/skills/Vanish.java", 2, 2 },
			{ "actors/hero/skills/Stealth.java", 1, 2 },
			{ "actors/hero/skills/DarkPact.java", 0, 4 },
			{ "actors/hero/skills/Ambush.java", 0, 2 },
			{ "actors/hero/skills/DoubleStab.java", 0, 4 },
			{ "actors/hero/skills/SoulDrain.java", 0, 3 },
			{ "actors/hero/skills/SoulDetonation.java", 0, 2 },
			{ "actors/hero/skills/SummonSkeleton.java", 1, 2 },
			{ "actors/hero/skills/Blackout.java", 0, 3 },
			{ "actors/hero/skills/PhantomStrike.java", 0, 4 },
			{ "actors/hero/skills/NinjaBomb.java", 0, 3 },
			{ "actors/hero/skills/MasterThief.java", 0, 3 },
			{ "actors/hero/skills/Necrotoxin.java", 1, 3 },
			{ "actors/hero/skills/Venom.java", 1, 2 },
			{ "actors/hero/skills/AimedShot.java", 0, 5 },
			{ "actors/hero/skills/DoubleShot.java", 1, 4 },
			{ "actors/hero/skills/Bombvoyage.java", 1, 4 },
			{ "actors/mobs/SkillDecoy.java", 0, 0 },
			{ "effects/skillfx/RogueHuntressAuras.java", 0, 0 },
			//new: never the game's Random (the decoy's pour is an FxEmitter's)
			{ "effects/skillfx/DreadFX.java", 0, 0 },
			{ "sprites/DecoySprite.java", 0, 1 } };

	static final Pattern RANDOM = Pattern.compile( "(?<![A-Za-z0-9_])Random\\." ),
			DRAWS = Pattern.compile( "\\.burst\\(|(?<!FxTimeline)\\.start\\(|\\.pour\\(|Splash\\.at\\(|new Lightning\\("
					+ "|MagicMissile\\.boltFromChar\\(|\\.reset\\(\\s*(xyz\\.gabriwar\\.warpedpixeldungeon\\.effects\\.)?MagicMissile\\."
					+ "|SkillBolt\\.fly\\(|\\.showLost\\(" );

	@Test
	public void everyTouchedFileCallsTheGamesRandomAsOftenAsItDid() throws Exception {
		for (Object[] row : CENSUS){
			String src = source( (String) row[0] );
			assertEquals( row[0] + ": the game's Random", (int) (Integer) row[1], matches( RANDOM, src ) );
			assertEquals( row[0] + ": its pictures that draw it", (int) (Integer) row[2], matches( DRAWS, src ) );
		}
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
	public void theTetherBitesOnTheChainsFrame(){
		Group mobs = t.rig.stage;
		PointF[][] ways = { { new PointF( 40, 60 ), new PointF( 120, 30 ) }, { new PointF( 40, 60 ), new PointF( 56, 60 ) },
				{ new PointF( 30, 30 ), new PointF( 150, 110 ) } };
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			for (PointF[] w : ways){
				int chain = arrival( cb -> mobs.add( new Chains( new PointF( w[0] ), new PointF( w[1] ), Effects.Type.ETHEREAL_CHAIN, cb::run ) ), 600 );
				final int[] bites = { 0 };
				int tether = arrival( cb -> DreadFX.tether( mobs, new PointF( w[0] ), new PointF( w[1] ), () -> { bites[0]++; cb.run(); } ), 600 );
				assertTrue( chain > 0 );
				assertEquals( "dt " + dt, chain, tether );
				t.rig.frames( 120, true );
				assertEquals( "it bites once", 1, bites[0] );
			}
		}
		//and lets go of itself, its knot and its glow
		assertEquals( 0, count( mobs, Gizmo.class, "Bind" ) );
		assertEquals( 0, LinkFX.live() );
	}

	@Test
	public void theTetherKeepsToTheLinksCaps(){
		//the stage's longest bond (about 144 px): every count a bind has at its most, frame by frame
		Group mobs = t.rig.stage;
		Group lights = t.rig.layers[xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.Layer.LIGHTS.ordinal()];
		t.rig.frames( 60, true );
		int particles0 = FxBudget.live( FxBudget.PARTICLES ), lights0 = FxLight.live();
		PointF a = new PointF( 30, 30 ), b = new PointF( 150, 110 );
		float len = (float) Math.hypot( b.x - a.x, b.y - a.y );
		DreadFX.tether( mobs, a, b, null );
		//its motes (MOTES_MAX a side at most), its thread, the motes flicked at its ends, the bite's
		//star and its two racers, the ink peeling off both bodies (one a side every PEEL_EVERY s,
		//each living 0.4 s), and DreadFX's stream out of both (PULL_MOST a side)
		int peel = 2 * (int) Math.ceil( 0.4f / LinkFX.PEEL_EVERY );
		int most = 2 * LinkFX.MOTES_MAX + LinkFX.THREAD_MAX + 2 * LinkFX.END_MOTES + 3 + peel + 2 * DreadFX.PULL_MOST;
		assertEquals( LinkFX.MOTES_MAX, LinkFX.motesASide( len ) );
		assertEquals( LinkFX.THREAD_MAX, LinkFX.threadBits( len ) );
		int peak = 0, light = 0, knots = 0;
		for (int f = 0; f < 90; f++){
			t.rig.frames( 1, true );
			peak = Math.max( peak, FxBudget.live( FxBudget.PARTICLES ) - particles0 );
			light = Math.max( light, FxLight.live() - lights0 );
			knots = Math.max( knots, count( lights, FxPic.class ) );
			assertTrue( LinkFX.live() <= LinkFX.CAP );
		}
		assertTrue( "particles " + peak + " > " + most, peak <= most );
		assertTrue( "a bind's motes fed its knot", peak >= 2 * LinkFX.MOTES_MIN );
		assertTrue( "lights " + light, light <= 1 );
		assertTrue( "knot quads " + knots, knots <= 1 );
		assertEquals( 0, LinkFX.live() );
	}

	@Test
	public void theBoltsLandOnTheMissilesFrames(){
		Group mobs = t.rig.stage;
		Rat r = t.rat( t.at( 8, 6 ) );
		CharSprite hs = Dungeon.hero.sprite;
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			//Hex: MagicMissile.boltFromChar to a cell a character stands on
			int old = arrival( cb -> MagicMissile.boltFromChar( mobs, MagicMissile.SHADOW, hs, r.pos, cb::run ), 600 );
			int now = arrival( cb -> SkillBolt.fly( mobs, hs.center(), r.sprite.destinationCenter(), SkillBolt.HEX, cb::run ), 600 );
			assertEquals( "hex, dt " + dt, old, now );
			//Panic Harvest: from the enemy's middle home to the rogue's
			old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class )).reset( MagicMissile.SHADOW, r.sprite.center(), hs.center(), cb::run ), 600 );
			now = arrival( cb -> SkillBolt.fly( mobs, r.sprite.center(), hs.center(), SkillBolt.MANA, cb::run ), 600 );
			assertEquals( "panic harvest, dt " + dt, old, now );
			//Soul Drain: cell to cell
			old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class )).reset( MagicMissile.SHADOW, t.at( 2, 2 ), r.pos, cb::run ), 600 );
			now = arrival( cb -> SkillBolt.fly( mobs, t.at( 2, 2 ), r.pos, SkillBolt.SOUL, cb::run ), 600 );
			assertEquals( "soul drain, dt " + dt, old, now );
			t.rig.frames( 30, true );
		}
	}

	// ------------------------------------------------------------------ the shade decoy

	@Test
	public void theDecoyIsAShadeAndLetsItsInkGo(){
		DecoySprite d = new DecoySprite();
		t.rig.stage.add( d );
		d.place( t.at( 4, 3 ) );
		d.visible = true;
		assertShade( d );
		//its ink: an FxEmitter pouring, never the game's Random
		assertEquals( 0, EnchantFxTest.draws( () -> t.rig.frames( 120, true ) ) );
		FxEmitter ink = d.ink();
		assertNotNull( ink );
		assertTrue( ink.on );
		assertTrue( ink.parent != null );
		//a blow's flash gives its colour back
		d.flash();
		t.rig.frames( 30, true );
		assertShade( d );
		//dying: the ink stops; killed: let go of
		d.die();
		t.rig.frames( 2, true );
		assertFalse( ink.on );
		d.killAndErase();
		t.rig.frames( 2, true );
		assertNull( d.ink() );
		assertFalse( ink.exists );
		assertNull( ink.parent );
	}

	private static void assertShade( CharSprite d ){
		int c = DecoySprite.COLOR;
		assertEquals( ((c >> 16) & 0xFF) / 255f, d.rm, 0.001f );
		assertEquals( ((c >> 8) & 0xFF) / 255f, d.gm, 0.001f );
		assertEquals( (c & 0xFF) / 255f, d.bm, 0.001f );
		assertEquals( DecoySprite.ALPHA, d.alpha(), 0.001f );
	}

	@Test
	public void theDecoyWearsTheShadeAMirrorImageNever(){
		assertTrue( MirrorSprite.class.isAssignableFrom( DecoySprite.class ) );
		assertEquals( DecoySprite.class, new SkillDecoy().spriteClass );
	}

	// ------------------------------------------------------------------ DreadFX

	//every picture of DreadFX once, round a rat
	private void makeEverything( CharSprite a ){
		CharSprite hs = Dungeon.hero.sprite;
		DreadFX.nerve( a );
		DreadFX.madness( a );
		DreadFX.pool( 60, 70, 0.5f, 0.3f, 0.4f );
		DreadFX.sink( a );
		DreadFX.vanish( hs );
		DreadFX.tether( t.rig.stage, hs.center(), a.destinationCenter(), null );
		DreadFX.pact( hs );
		DreadFX.knit( a );
		DreadFX.stake( a );
		DreadFX.star( 70, 60, Element.GOLD.light );
		DreadFX.pin( a, 40 );
		DreadFX.coat( hs, Element.POISON.light );
	}

	@Test
	public void dreadFxDrawsNothingOfTheGamesRandom(){
		CharSprite a = t.rat( t.at( 5, 4 ) ).sprite;
		assertEquals( 0, EnchantFxTest.draws( () -> {
			makeEverything( a );
			t.rig.frames( 90, true );
		} ) );
	}

	@Test
	public void dreadFxKeepsToTheKitsCapsAndLeavesNothing(){
		CharSprite a = t.rat( t.at( 5, 4 ) ).sprite;
		for (int i = 0; i < 12; i++){
			makeEverything( a );
			t.rig.frames( 1, true );
			assertTrue( "afterimages " + Afterimage.live(), Afterimage.live() <= FxBudget.afterimageCap() );
			assertTrue( "flares " + PixelFlare.live(), PixelFlare.live() <= PixelFlare.CAP );
			assertTrue( "lances " + LanceFX.live(), LanceFX.live() <= LanceFX.CAP );
			assertTrue( "particles " + FxBudget.live( FxBudget.PARTICLES ), FxBudget.live( FxBudget.PARTICLES ) <= FxBudget.hard() );
			//a howl maddening a crowd: at most RUNES_CAP pairs of runes circle at once
			int runes = 0;
			for (Group g : t.rig.layers) runes += countClass( g, DreadFX.class.getName() + "$Runes" );
			assertTrue( "runes " + runes, runes <= DreadFX.RUNES_CAP );
		}
		//past the cap the oldest decals fade out over 0.3 s: within the cap once they have
		t.rig.frames( 24, true );
		assertTrue( "decals " + FxDecal.live(), FxDecal.live() <= FxBudget.decalCap() );
		t.rig.frames( 180, true );
		//the runes, the pins, the tethers and the lances are gone with their lives
		for (String c : new String[]{ "Runes", "Pin", "Bind" }){
			int n = count( t.rig.stage, Gizmo.class, c );
			for (Group g : t.rig.layers) n += count( g, Gizmo.class, c );
			assertEquals( c, 0, n );
		}
		assertEquals( 0, LanceFX.live() );
		assertEquals( 0, LinkFX.live() );
		assertEquals( 0, RuneFX.live() );
	}

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite a = t.rat( t.at( 5, 4 ) ).sprite;
		GL20 saved = Gdx.gl;
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, args) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
		try {
			makeEverything( a );
			DecoySprite d = new DecoySprite();
			t.rig.stage.add( d );
			d.place( t.at( 4, 3 ) );
			t.rig.frames( 40, true );
		} finally {
			Gdx.gl = Gdx.gl20 = saved;
		}
	}

	@Test
	public void theFlinchesAndTheStakeAreNeverWashedWhite(){
		//a double image in near white over a body reads as the body hit: the flinch ramps' second
		//stop (their brightest double image, at 0.7) stays under 0xC0 in red and green
		for (int[] r : new int[][]{ DreadFX.WOLF_FLINCH, DreadFX.DREAD_FLINCH }){
			int c = r[1];
			assertTrue( ((c >> 16) & 0xFF) < 0xC0 && ((c >> 8) & 0xFF) < 0xC0 );
		}
		//Ambush's stake is driven down: its head (and its glow) violet, never a white beam coming down
		int head = DreadFX.STAKE_RAMP[1];
		assertTrue( ((head >> 8) & 0xFF) < 0xC0 );
	}

	// ------------------------------------------------------------------ nothing borrowed

	/** What a skill must not draw any more: the game's looks for things that are not there. */
	static final String[] BANNED = { "Speck.SCREAM", "ShadowParticle", "MagicMissile", "Beam.", "Chains", "ETHEREAL_CHAIN",
			"new Flare(", "effects.Flare(", "Speck.QUESTION", "Speck.TOXIC", "Speck.BONE", "Speck.ROCK", "Speck.STAR",
			"SnowParticle", "FlameParticle", "PoisonParticle.MISSILE", "new Gold()", "new Bomb()", "Sounds.CURSED",
			"Sounds.GAS", "Sounds.BURNING", "Sounds.HOWL" };

	@Test
	public void thePackagesSkillsNoLongerBorrowTheGamesLooks() throws Exception {
		for (Object[] row : CENSUS){
			String path = (String) row[0];
			if (!path.contains( "/skills/" ) && !path.contains( "RogueHuntressAuras" )) continue;
			String src = code( source( path ) );
			for (String b : BANNED) assertFalse( path + " uses " + b, src.contains( b ) );
		}
		//smoke where the skill makes no smoke: the real SmokeScreen's (Ninja Bomb) and a pinched fuse
		//(Bombvoyage's lowering) keep theirs
		for (String f : new String[]{ "SilentDeath", "Stealth", "DarkPact", "Blackout" }){
			assertFalse( f, source( "actors/hero/skills/" + f + ".java" ).contains( "Speck.SMOKE" ) );
		}
		//coins only where gold really is picked up (the orbit loading), never on the struck
		assertEquals( 2, matches( Pattern.compile( "Speck\\.COIN" ), source( "actors/hero/skills/MasterThief.java" ) ) );
		//the decoy is no Mirror Image
		assertFalse( source( "actors/mobs/SkillDecoy.java" ).contains( "MirrorSprite" ) );
	}

	// ------------------------------------------------------------------ helpers

	private static int count( Group g, Class<?> c ){
		return count( g, c, null );
	}

	//alive members of exactly this class (by its binary name) in g and its groups
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

	//alive members of class c (and, if named, of a class of that simple name) in g and its groups
	private static int count( Group g, Class<?> c, String simpleName ){
		if (g == null) return 0;
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (c.isInstance( m ) && (simpleName == null || m.getClass().getSimpleName().equals( simpleName ))) n++;
			if (m instanceof Group) n += count( (Group) m, c, simpleName );
		}
		return n;
	}

	//the source without its comments (what it says it no longer draws is no drawing)
	private static String code( String src ){
		return src.replaceAll( "(?s)/\\*.*?\\*/", "" ).replaceAll( "//[^\n]*", "" );
	}

	private static int matches( Pattern p, String s ){
		Matcher m = p.matcher( s );
		int n = 0;
		while (m.find()) n++;
		return n;
	}

	static String source( String path ) throws Exception {
		File f = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		if (!f.exists()) f = new File( "core/src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		assertTrue( "no source " + f.getAbsolutePath(), f.exists() );
		return new String( Files.readAllBytes( f.toPath() ), "UTF-8" );
	}
}
