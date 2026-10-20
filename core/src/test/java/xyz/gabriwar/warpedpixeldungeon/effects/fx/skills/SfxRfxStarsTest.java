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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Accuracy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.DragonKick;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HeadShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LastStand;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The rfx-stars package (owner ask 4, "remove the stars that drop and go offscreen", with 6): every
 * Speck.STAR burst in the skills and the three duelist weapons swapped for a picture of its meaning
 * that stays where it was made (a real daze's amber twinkle, steel sparks, a steel glint, the hero's
 * sparkle), Accuracy's and Second Wind's colours by meaning, and Blade Bind's two-ended steel pull.
 *
 * - The game's Random: every touched moment played for real (RfxStarsProcs) draws as f0783eacd did,
 *   from a seed through two seconds of frames, to the next value; every touched file keeps its draw
 *   sites and every burst its quantity, in order; each moment really bursts its new look.
 * - No star speck left in actors/hero/skills/** or the three weapons (comments excluded); no alarm
 *   brackets (Speck.SCREAM) on the Trumpet's held foes (a daze's twinkle) or the Wardrum's terrified
 *   ones (fear's shudder, War Cry's TREMBLE); the Trumpet's blessing in holy gold (HOLY_MOTE).
 * - DAZE twinkles in place: one particle an emit, over the head, the stun's amber, within 5 px of
 *   where it was born over 1.2 s at 60 fps, never falling (no gravity: it only slows).
 * - Last Stand only shudders a boss it cannot hold (TREMBLE) and twinkles on a held rat (DAZE).
 * - Blade Bind's bite comes on the chain's frame at 30, 60 and 144 fps, and its link's knot snaps on
 *   that same frame; the link pulls steel off both ends.
 * - Piercing Focus puts no reticle ring on an enemy; colours by meaning (steel, never peach or sage).
 * - A loud martial moment (Dragon Kick's wall, Pirouette's landing on 4, Head Shot's ring-out on 3,
 *   Last Stand on 3, Blade Bind) on a phone's numbers stays under SOFT and the kit's caps.
 * - Nothing touches the GPU while any of it is made.
 */
public class SfxRfxStarsTest {

	private static final String BASE = "src/main/java/xyz/gabriwar/warpedpixeldungeon/";

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	// ------------------------------------------------------------------ the game's Random

	/** f0783eacd's draws and next value (x 1e6) of each touched moment, measured by RfxStarsProcs in
	 *  a checkout of it (RFX_STARS_BASELINE=1). Lunge, Pirouette, Bandit's snatch and Fight Gloves'
	 *  jabs need a whole scene (the fog, the attack indicator, a pickpocket's loot) and are held by the
	 *  draw-site census below instead. */
	static final Object[][] BASELINE = {
			{ "aplomb", 18, 660644 }, { "dragon-kick", 81, 507654 }, { "sidestep", 14, 2120 },
			{ "sixth-sense", 22, 277476 }, { "piercing-focus", 31, 62458 }, { "head-shot", 34, 967549 },
			{ "iron-stance", 22, 277476 }, { "last-stand", 55, 82607 }, { "scorpion", 38, 474635 },
			{ "aggression", 9, 556843 }, { "mastery", 29, 280844 }, { "hunting", 10, 578213 },
			{ "parry", 20, 207127 }, { "riposte", 25, 183856 }, { "counter-time", 12, 19111 },
			{ "blade-mastery", 21, 838969 }, { "precision", 7, 406656 }, { "awareness", 9, 556843 },
			{ "rampage", 11, 603927 }, { "accuracy", 0, 477554 }, { "second-wind", 11, 603927 },
			{ "blade-bind", 41, 796327 }, { "trumpet", 27, 883566 }, { "wardrum", 52, 329130 } };

	@Test
	public void everyTouchedMomentDrawsTheGamesRandomAsF0783Did() throws Exception {
		LinkedHashMap<String, long[]> now = RfxStarsProcs.measure();
		assertEquals( BASELINE.length, now.size() );
		for (Object[] row : BASELINE){
			long[] n = now.get( (String) row[0] );
			assertNotNull( (String) row[0], n );
			assertEquals( row[0] + ": draws", (long) (Integer) row[1], n[0] );
			assertEquals( row[0] + ": the next value", (long) (Integer) row[2], n[1] );
		}
	}

	@Test
	public void everyTouchedMomentShowsItsNewLookAndNoStar() throws Exception {
		Object[][] want = {
				{ "aplomb", SkillBursts.DAZE }, { "dragon-kick", SkillBursts.DAZE }, { "sidestep", SkillBursts.DAZE },
				{ "sixth-sense", SkillBursts.DAZE }, { "piercing-focus", SkillBursts.DAZE, SkillBursts.STEEL_GLINT, SkillBursts.FLOURISH },
				{ "head-shot", SkillBursts.METAL_SPARK, SkillBursts.DAZE }, { "iron-stance", SkillBursts.DAZE },
				{ "last-stand", SkillBursts.DAZE, SkillBursts.TREMBLE }, { "scorpion", SkillBursts.DAZE },
				{ "aggression", SkillBursts.METAL_SPARK }, { "mastery", SkillBursts.METAL_SPARK }, { "hunting", SkillBursts.METAL_SPARK },
				{ "parry", SkillBursts.STEEL_GLINT }, { "riposte", SkillBursts.STEEL_GLINT }, { "counter-time", SkillBursts.STEEL_GLINT },
				{ "blade-mastery", SkillBursts.STEEL_GLINT }, { "precision", SkillBursts.STEEL_GLINT }, { "awareness", SkillBursts.FLOURISH },
				{ "rampage", SkillBursts.FLOURISH }, { "second-wind", SkillBursts.BREATH }, { "blade-bind", SkillBursts.METAL_SPARK },
				{ "trumpet", SkillBursts.DAZE, SkillBursts.HOLY_MOTE }, { "wardrum", SkillBursts.TREMBLE, SkillBursts.FLOURISH } };
		LinkedHashMap<String, Set<Emitter.Factory>> seen = RfxStarsProcs.bursts();
		Emitter.Factory star = Speck.factory( Speck.STAR ), electric = xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY,
				alarm = Speck.factory( Speck.SCREAM );
		for (Object[] row : want){
			Set<Emitter.Factory> s = seen.get( (String) row[0] );
			assertNotNull( (String) row[0], s );
			for (int i = 1; i < row.length; i++) assertTrue( row[0] + " shows " + name( row[i] ), s.contains( row[i] ) );
		}
		for (Map.Entry<String, Set<Emitter.Factory>> e : seen.entrySet()){
			assertFalse( e.getKey() + " throws a falling star", e.getValue().contains( star ) );
			assertFalse( e.getKey() + " throws the shock's sparks", e.getValue().contains( electric ) );
			assertFalse( e.getKey() + " flashes the alarm's red brackets", e.getValue().contains( alarm ) );
		}
		//the fanfare blesses: holy gold (its flare's), never the hero's class sparkle
		assertFalse( "a blessing in the class's colour", seen.get( "trumpet" ).contains( SkillBursts.FLOURISH ) );
	}

	private static String name( Object f ){
		for (Object[] r : SkillBursts.ALL) if (r[1] == f) return (String) r[0];
		return String.valueOf( f );
	}

	/** f0783eacd's draw sites (bursts, starts, delayed starts, pours, lightnings, arcs, bolts, rains,
	 *  Random calls, splashes) and every burst's, start's and pour's arguments after the factory, in
	 *  order, of every touched file (rfx/stars/census.py read them at f0783eacd). */
	static final Object[][] CENSUS = {
			{ "actors/hero/skills/Aplomb.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:4 burst:4" },
			{ "actors/hero/skills/DragonKick.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 3, 0 }, "burst:3 burst:4 burst:5 burst:3" },
			{ "actors/hero/skills/Lunge.java", new int[]{ 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:4 burst:4 start:SkillSequence.CROSSCUT|level|from|Math.max(1,damage/3)|1|lane.subList(0,Math.min(lane.size(),Dungeon.level.distance(from,land)+1))" },
			{ "actors/hero/skills/Sidestep.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:2 burst:4" },
			{ "actors/hero/skills/SixthSense.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:6 burst:4" },
			{ "actors/hero/skills/PiercingFocus.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:2 burst:3 burst:3" },
			{ "actors/hero/skills/Pirouette.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:10 burst:6 burst:4 burst:3" },
			{ "actors/hero/skills/HeadShot.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:6 burst:3" },
			{ "actors/hero/skills/IronStance.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:3 burst:6 burst:4" },
			{ "actors/hero/skills/LastStand.java", new int[]{ 2, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:5 start:0.05f|6 burst:3" },
			{ "actors/hero/skills/Scorpion.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:4 burst:5" },
			{ "actors/hero/skills/Aggression.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:4" },
			{ "actors/hero/skills/Mastery.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:3 burst:4" },
			{ "actors/hero/skills/Hunting.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:4 burst:8" },
			{ "actors/hero/skills/ParryStance.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:6" },
			{ "actors/hero/skills/RiposteStance.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:3 burst:4" },
			{ "actors/hero/skills/CounterTime.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:4" },
			{ "actors/hero/skills/BladeMastery.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:3 burst:3" },
			{ "actors/hero/skills/Precision.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:3" },
			{ "actors/hero/skills/Awareness.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:4" },
			{ "actors/hero/skills/Rampage.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:2 burst:5" },
			{ "actors/hero/skills/Bandit.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 2, 0 }, "burst:5 burst:4 burst:4" },
			{ "actors/hero/skills/Accuracy.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "" },
			{ "actors/hero/skills/SecondWind.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:5" },
			{ "actors/hero/skills/BladeBind.java", new int[]{ 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "burst:5 burst:6 burst:8 burst:4" },
			{ "items/weapon/melee/Trumpet.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 2, 0 }, "burst:3 burst:3 burst:6" },
			{ "items/weapon/melee/Wardrum.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 2, 0 }, "burst:3 burst:2 burst:2" },
			{ "items/weapon/melee/FightGloves.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 }, "burst:2" } };

	static final String[] SITES = { "\\.burst\\s*\\(", "(?<!FxTimeline)\\.start\\s*\\(", "\\.startDelayed\\s*\\(", "\\.pour\\s*\\(",
			"new\\s+Lightning\\s*\\(", "new\\s+Lightning\\.Arc\\s*\\(",
			"(MagicMissile\\.boltFromChar|\\(\\s*\\(\\s*MagicMissile\\s*\\)[^;]*?\\.reset\\s*\\(|SkillBolt\\.fly\\s*\\()",
			"SkillFX\\.rain\\s*\\(",
			"(?<![A-Za-z])Random\\.(Int|Float|NormalIntRange|NormalFloat|IntRange|Long|chances|element|shuffle|oneOf|chance)\\b",
			"Splash\\.at\\s*\\(" };

	@Test
	public void everyDrawSiteAndQuantityIsWhereF0783HadIt() throws Exception {
		for (Object[] row : CENSUS){
			String src = code( BASE + row[0] );
			int[] want = (int[]) row[1];
			for (int k = 0; k < SITES.length; k++){
				Matcher m = Pattern.compile( SITES[k], Pattern.DOTALL ).matcher( src );
				int n = 0;
				while (m.find()) n++;
				assertEquals( row[0] + " " + SITES[k], want[k], n );
			}
			assertEquals( row[0] + ": the bursts' quantities", row[2], quantities( src ) );
		}
	}

	//every .burst/.start/.pour call's arguments after the first (the factory), spaces out, in order
	static String quantities( String src ){
		StringBuilder out = new StringBuilder();
		Matcher m = Pattern.compile( "\\.(burst|start|pour)\\s*\\(" ).matcher( src );
		while (m.find()){
			int i = m.end(), depth = 1;
			List<String> args = new ArrayList<>();
			StringBuilder cur = new StringBuilder();
			while (depth > 0){
				char c = src.charAt( i++ );
				if (c == '(' || c == '[') depth++;
				else if (c == ')' || c == ']') depth--;
				if (depth == 1 && c == ','){
					args.add( cur.toString() );
					cur.setLength( 0 );
					continue;
				}
				if (depth > 0) cur.append( c );
			}
			args.add( cur.toString() );
			if (out.length() > 0) out.append( ' ' );
			out.append( m.group( 1 ) ).append( ':' );
			for (int a = 1; a < args.size(); a++){
				if (a > 1) out.append( '|' );
				out.append( args.get( a ).replaceAll( "\\s+", "" ) );
			}
		}
		return out.toString();
	}

	// ------------------------------------------------------------------ no star left

	@Test
	public void noStarSpeckIsLeftInTheSkillsOrTheThreeWeapons() throws Exception {
		List<Path> files = new ArrayList<>();
		try (Stream<Path> s = Files.walk( new File( BASE + "actors/hero/skills" ).toPath() )){
			s.filter( p -> p.toString().endsWith( ".java" ) ).forEach( files::add );
		}
		for (String w : new String[]{ "Trumpet", "Wardrum", "FightGloves" }) files.add( new File( BASE + "items/weapon/melee/" + w + ".java" ).toPath() );
		assertTrue( files.size() > 100 );
		for (Path p : files){
			String src = code( p.toString() );
			assertFalse( p + " bursts the falling star", Pattern.compile( "Speck\\s*\\.\\s*STAR\\b" ).matcher( src ).find() );
		}
		//the instruments' held and terrified foes: the stun's twinkle and fear's shudder, never the alarm trap's brackets
		for (String w : new String[]{ "Trumpet", "Wardrum" }){
			assertFalse( w + " flashes the alarm", Pattern.compile( "Speck\\s*\\.\\s*SCREAM\\b" ).matcher( code( BASE + "items/weapon/melee/" + w + ".java" ) ).find() );
		}
		//the honest choices: a boss Last Stand cannot hold only shudders; Lunge twinkles only when dazed;
		//Piercing Focus pins a glint of steel, never the shock's sparks, and never a reticle ring
		assertTrue( code( BASE + "actors/hero/skills/LastStand.java" ).contains( "burst( boss ? SkillBursts.TREMBLE : SkillBursts.DAZE, 3 )" ) );
		assertTrue( code( BASE + "actors/hero/skills/Lunge.java" ).contains( "enemy.buff(Daze.class)!=null?SkillBursts.DAZE:SkillBursts.METAL_SPARK,4" ) );
		String focus = code( BASE + "actors/hero/skills/PiercingFocus.java" );
		assertTrue( focus.contains( "burst( SkillBursts.STEEL_GLINT, 3 )" ) );
		assertFalse( focus.contains( "SparkParticle" ) );
		assertFalse( "a reticle on an enemy", focus.contains( "ClosingRing" ) );
	}

	// ------------------------------------------------------------------ the daze, in place

	@Test
	public void aDazeTwinklesInPlaceInTheStunsAmberAndNeverFalls() throws Exception {
		//the game's own stun colour (Looks.Stunned.COLOR, package-private there)
		Field stun = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Looks$Stunned" ).getDeclaredField( "COLOR" );
		stun.setAccessible( true );
		assertEquals( "the game's stun colour", stun.getInt( null ), SkillPalette.DAZE );
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		try {
			Game.elapsed = 1 / 60f;
			Rat r = t.rat( t.at( 5, 4 ) );
			CharSprite s = r.sprite;
			s.emitter().burst( SkillBursts.DAZE, 4 );
			//a cell's emitter (Scorpion's): no sprite to circle, over the point
			CellEmitter.center( t.at( 7, 2 ) ).burst( SkillBursts.DAZE, 4 );
			IdentityHashMap<FxParticle, float[]> born = new IdentityHashMap<>();
			for (int f = 0; f < 72; f++){
				t.rig.frames( 1, true );
				for (FxParticle p : particles( t.rig.layers, t.rig.stage )){
					float[] b = born.get( p );
					float v = (float) Math.hypot( p.vx, p.vy );
					if (b == null){
						born.put( p, new float[]{ p.px, p.py, v } );
						assertEquals( "the stun's amber", SkillPalette.DAZE, p.colorNow() );
						assertTrue( "a light", p.isLight() );
						continue;
					}
					assertTrue( "within 5 px of its birth: " + Math.hypot( p.px - b[0], p.py - b[1] ),
							Math.hypot( p.px - b[0], p.py - b[1] ) <= 5f );
					assertTrue( "never falling (it only slows)", v <= b[2] + 1e-3f );
					b[2] = v;
				}
			}
			//one particle an emit
			assertEquals( 8, born.size() );
			//the sprite's four over its head on the MARK ellipse, the cell's over the point
			float mid = SkillVfx.midX( s ), top = s.y - 2, cx = SkillVfx.cx( t.at( 7, 2 ) ), cy = SkillVfx.cy( t.at( 7, 2 ) );
			int over = 0, atCell = 0;
			for (float[] b : born.values()){
				if (Math.abs( b[0] - mid ) <= SkillBursts.DAZE_RX + 1 && Math.abs( b[1] - top ) <= SkillBursts.DAZE_RY + 1) over++;
				else if (Math.abs( b[0] - cx ) <= 1 && Math.abs( b[1] - (cy - SkillBursts.DAZE_UP) ) <= 1) atCell++;
			}
			assertEquals( 4, over );
			assertEquals( 4, atCell );
			//all gone after their life
			assertEquals( 0, particles( t.rig.layers, t.rig.stage ).size() );
		} finally {
			t.down();
		}
	}

	@Test
	public void lastStandOnlyShakesABossItCannotHold() throws Exception {
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		try {
			t.hero.HP = t.hero.HT = 100;
			LastStand l = DreadShadowProcs.skill( new LastStand() );
			Rat rat = t.rat( t.at( 4, 4 ) );
			Rat boss = t.rat( t.at( 2, 4 ) );
			properties( boss ).add( Char.Property.BOSS );
			l.incomingDamageReduction( 90, rat );
			Map<Visual, Emitter.Factory> by = burstsBy( t );
			assertTrue( by.get( rat.sprite ) == SkillBursts.DAZE );
			assertTrue( by.get( boss.sprite ) == SkillBursts.TREMBLE );
			assertTrue( "held", rat.paralysed > 0 || rat.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class ) != null );
			assertTrue( "a boss is never held", boss.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class ) == null );
		} finally {
			t.down();
		}
	}

	//each sprite's emitter's factory (a sprite's emitters follow it)
	private static Map<Visual, Emitter.Factory> burstsBy( DreadShadowProcs t ) throws Exception {
		Field target = Emitter.class.getDeclaredField( "target" ), factory = Emitter.class.getDeclaredField( "factory" );
		target.setAccessible( true );
		factory.setAccessible( true );
		Map<Visual, Emitter.Factory> out = new IdentityHashMap<>();
		List<Group> groups = new ArrayList<>( Arrays.asList( t.rig.layers ) );
		groups.add( t.rig.stage );
		while (!groups.isEmpty()){
			Group g = groups.remove( groups.size() - 1 );
			for (Gizmo m : g.membersView()){
				if (m instanceof Emitter && target.get( m ) != null) out.put( (Visual) target.get( m ), (Emitter.Factory) factory.get( m ) );
				if (m instanceof Group) groups.add( (Group) m );
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ Blade Bind

	@Test
	public void bladeBindsKnotSnapsOnTheBitesFrameWhichIsTheChainsFrame() throws Exception {
		Method bind = BladeBind.class.getDeclaredMethod( "bind", Hero.class, int.class );
		bind.setAccessible( true );
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			try {
				//a new scene's count (SkillsFx.sceneCreated resets it in the game)
				LinkFX.reset();
				Game.elapsed = dt;
				t.hero.MP = t.hero.MT = 200;
				BladeBind b = DreadShadowProcs.skill( new BladeBind() );
				Rat r = t.rat( t.at( 4, 4 ) );
				PointF a = t.hero.sprite.center(), c = r.sprite.center();
				PointF from = new PointF( a.x + 4, a.y + 2 ), to = new PointF( c.x - 3, c.y + 3 );
				//the ethereal chain the bind once threw, on the same two points, for its frame
				final int[] frame = { 0 }, chain = { -1 };
				Group ref = new Group();
				ref.add( new Chains( new PointF( from.x, from.y ), new PointF( to.x, to.y ), Effects.Type.ETHEREAL_CHAIN,
						() -> chain[0] = frame[0] ) );
				bind.invoke( b, t.hero, r.pos );
				assertEquals( 1, count( t.rig.stage, BladeBind.Bite.class ) );
				int bite = -1, knot = -1, fromHer = 0, fromIt = 0;
				float sag = SkillMarkFX.sag( (float) Math.hypot( to.x - from.x, to.y - from.y ) );
				float kx = (from.x + to.x) / 2f, ky = (from.y + to.y) / 2f + sag;
				for (frame[0] = 1; frame[0] <= 400 && (bite < 0 || knot < 0 || chain[0] < 0); frame[0]++){
					t.rig.frames( 1, true );
					ref.update();
					if (bite < 0 && count( t.rig.stage, BladeBind.Bite.class ) == 0) bite = frame[0];
					for (FxParticle p : particles( t.rig.layers, t.rig.stage )){
						if (knot < 0 && sameFrame( p, FxFrames.IMPACT_STAR[0] )) knot = frame[0];
						//a cool steel mote flying at the knot: off her blade (west of it) or off the bound (east)
						int col = p.colorNow();
						boolean cool = (col & 0xFF) > (col >> 16 & 0xFF) + 0x10;
						float dx = kx - p.px, dy = ky - p.py, d = (float) Math.hypot( dx, dy ), v = (float) Math.hypot( p.vx, p.vy );
						if (bite < 0 && cool && d > 1f && v > 1f && (dx * p.vx + dy * p.vy) / (d * v) > 0.97f){
							if (dx > 0) fromHer++;
							else fromIt++;
						}
					}
				}
				assertTrue( "at " + dt, chain[0] > 0 );
				assertEquals( "the bite on the chain's frame at " + dt, chain[0], bite );
				assertEquals( "the knot snaps on the bite's frame at " + dt, bite, knot );
				assertTrue( "steel pulled off her blade at " + dt, fromHer > 0 );
				assertTrue( "steel pulled off the bound one at " + dt, fromIt > 0 );
				assertTrue( LinkFX.live() <= LinkFX.CAP );
				t.rig.frames( 120, true );
				assertEquals( "the link let go", 0, LinkFX.live() );
			} finally {
				t.down();
			}
		}
		//the link takes the Bite's own two points and bites nothing itself
		String src = code( BASE + "actors/hero/skills/BladeBind.java" );
		assertTrue( src.contains( "new Bite( biteFrom, biteTo," ) );
		assertTrue( src.contains( "LinkFX.bind( hero.sprite.parent, biteFrom, biteTo, LinkFX.STEEL, null )" ) );
		assertTrue( src.indexOf( "new Bite( biteFrom" ) < src.indexOf( "LinkFX.bind(" ) );
	}

	// ------------------------------------------------------------------ colours by meaning

	@Test
	public void theColoursSayWhatHappens() throws Exception {
		//Accuracy's bullseye in the arrowhead's steel (Precision's), never a fire's peach
		assertEquals( 0xE6EDF5, Accuracy.BULLSEYE );
		String acc = code( BASE + "actors/hero/skills/Accuracy.java" );
		assertTrue( acc.contains( "PixelFlare.show( SkillVfx.midX( enemy.sprite ), enemy.sprite.y + 3, BULLSEYE, PixelFlare.S, 0.5f )" ) );
		assertFalse( acc.contains( "DF9979" ) );
		//Second Wind gives back weapon charges, not health: steel, never the mend's sage or green
		String wind = code( BASE + "actors/hero/skills/SecondWind.java" );
		assertEquals( 0xB9DEEE, SkillPalette.STEEL_LIGHT );
		assertTrue( wind.contains( "SkillPalette.STEEL_LIGHT, PixelFlare.M, 0.6f )" ) );
		assertTrue( wind.contains( "PulseRingFX.around( hero.sprite, SkillPalette.STEEL_LIGHT, 16, 0.4f )" ) );
		assertTrue( wind.contains( "burst( SkillBursts.BREATH, 5 )" ) );
		assertFalse( wind.contains( "B3D699" ) );
		assertFalse( wind.contains( "HEAL" ) );
		assertFalse( wind.contains( "Speck.HEALING" ) );
	}

	@Test
	public void piercingFocusPutsNoReticleOnAnEnemy() throws Exception {
		Runnable[] run = new Runnable[1];
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		try {
			run[0] = RfxStarsProcs.PROCS.get( "piercing-focus" ).stage( t );
			run[0].run();
			for (int f = 0; f < 30; f++){
				t.rig.frames( 1, true );
				int rings = 0;
				for (Group g : t.rig.layers) rings += count( g, ClosingRing.class );
				rings += count( t.rig.stage, ClosingRing.class );
				assertEquals( "a reticle ring", 0, rings );
			}
		} finally {
			t.down();
		}
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void aLoudMartialMomentStaysUnderTheBudget() throws Exception {
		//a phone's numbers (SOFT 360, lights 24), not the desktop's half again
		Field scale = FxBudget.class.getDeclaredField( "scale" );
		scale.setAccessible( true );
		float desktop = scale.getFloat( null );
		scale.setFloat( null, 1f );
		DreadShadowProcs t = new DreadShadowProcs();
		t.up();
		try {
			LinkFX.reset();
			Game.elapsed = 1 / 60f;
			Hero hero = t.hero;
			hero.HP = hero.HT = 100;
			hero.MP = hero.MT = 200;
			//Dragon Kick into a wall, two dazed beside where it lands
			Rat victim = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 3 ) );
			t.rat( t.at( 5, 5 ) );
			int wall = t.at( 6, 4 );
			Dungeon.level.solid[wall] = true;
			Dungeon.level.passable[wall] = false;
			//Head Shot's ring-out on three
			Rat struck = t.rat( t.at( 9, 2 ) );
			t.rat( t.at( 8, 2 ) );
			t.rat( t.at( 10, 2 ) );
			t.rat( t.at( 9, 1 ) );
			//Last Stand on three (one of them bound by the blades)
			t.rat( t.at( 2, 3 ) );
			t.rat( t.at( 2, 5 ) );
			Rat bound = t.rat( t.at( 3, 3 ) );
			//Pirouette's landing on four (its leap wants a whole scene's fog: its pictures restaged)
			Rat[] left = { t.rat( t.at( 8, 6 ) ), t.rat( t.at( 9, 6 ) ), t.rat( t.at( 10, 6 ) ), t.rat( t.at( 9, 7 ) ) };

			Method kick = DragonKick.class.getDeclaredMethod( "kick", Hero.class, Char.class );
			kick.setAccessible( true );
			kick.invoke( DreadShadowProcs.skill( new DragonKick() ), hero, victim );
			Method ring = HeadShot.class.getDeclaredMethod( "ringOut", Char.class );
			ring.setAccessible( true );
			struck.sprite.emitter().burst( SkillBursts.METAL_SPARK, 6 );
			ring.invoke( null, struck );
			DreadShadowProcs.skill( new LastStand() ).incomingDamageReduction( 90, bound );
			Method bind = BladeBind.class.getDeclaredMethod( "bind", Hero.class, int.class );
			bind.setAccessible( true );
			bind.invoke( DreadShadowProcs.skill( new BladeBind() ), hero, bound.pos );
			//Pirouette.spin's pictures, as it lands by the four
			CellEmitter.bottom( hero.pos ).burst( Speck.factory( Speck.DUST ), 10 );
			new Flare( 5, 18 ).color( 0xCCE0FF, true ).show( hero.sprite, 0.5f ).angularSpeed = 240;
			ArcSpinFX.around( hero.sprite, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
			ArcSpinFX.around( hero.sprite, 0xFFFFFF, 7, 0.35f, 180, 1080, 0.5f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			PulseRingFX.around( hero.sprite, 0xCCE0FF, 12, 0.4f );
			for (Rat m : left) m.sprite.emitter().burst( SkillBursts.DAZE, 3 );

			int most = 0, lights = 0, links = 0, flares = 0;
			for (int f = 0; f < 120; f++){
				t.rig.frames( 1, true );
				most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
				lights = Math.max( lights, FxLight.live() );
				links = Math.max( links, LinkFX.live() );
				flares = Math.max( flares, PixelFlare.live() );
			}
			assertTrue( "particles " + most, most <= FxBudget.soft() );
			assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
			assertTrue( "links " + links, links >= 1 && links <= LinkFX.CAP );
			assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
			System.out.println( "rfx-stars loud moment: peak particles " + most + " (SOFT " + FxBudget.soft() + "), lights " + lights
					+ ", links " + links + ", flares " + flares );
			t.rig.frames( 600, true );
			assertEquals( "nothing left", 0, FxBudget.live( FxBudget.PARTICLES ) );
			assertEquals( 0, LinkFX.live() );
		} finally {
			t.down();
			scale.setFloat( null, desktop );
		}
	}

	// ------------------------------------------------------------------ the GPU

	@Test
	public void nothingTouchesTheGpuWhileMade() throws Exception {
		for (Map.Entry<String, RfxStarsProcs.Proc> e : RfxStarsProcs.PROCS.entrySet()){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			GL20 gl = Gdx.gl, gl20 = Gdx.gl20;
			try {
				Runnable run = e.getValue().stage( t );
				failOnGl( e.getKey() );
				run.run();
				t.rig.frames( 40, true );
			} finally {
				Gdx.gl = gl;
				Gdx.gl20 = gl20;
				t.down();
			}
		}
	}

	private static void failOnGl( String what ){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( what + ": GL call while building: " + m.getName() ); } );
	}

	// ------------------------------------------------------------------ helpers

	//a source file without its comments
	static String code( String path ) throws Exception {
		String s = new String( Files.readAllBytes( new File( path ).toPath() ), "UTF-8" );
		s = s.replaceAll( "(?s)/\\*.*?\\*/", "" );
		return s.replaceAll( "//[^\\n]*", "" );
	}

	//every live, shown kit particle on the layers and the stage
	private static List<FxParticle> particles( Group[] layers, Group stage ){
		List<FxParticle> out = new ArrayList<>();
		for (Group g : layers) particles( g, out );
		particles( stage, out );
		return out;
	}

	private static void particles( Group g, List<FxParticle> out ){
		for (Gizmo m : g.membersView()){
			if (m == null || !m.exists || !m.alive) continue;
			if (m instanceof FxParticle){
				if (m.visible) out.add( (FxParticle) m );
			} else if (m instanceof Group){
				particles( (Group) m, out );
			}
		}
	}

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	private static boolean sameFrame( Image i, int[] f ){
		if (i.texture == null || !TextureCache.contains( Assets.Effects.FX_SHEET ) || i.texture != TextureCache.get( Assets.Effects.FX_SHEET )) return false;
		RectF r = i.frame();
		return Math.round( r.left * i.texture.width ) == f[0] && Math.round( r.top * i.texture.height ) == f[1]
				&& Math.round( (r.right - r.left) * i.texture.width ) == f[2] && Math.round( (r.bottom - r.top) * i.texture.height ) == f[3];
	}

	@SuppressWarnings( "unchecked" )
	private static java.util.HashSet<Char.Property> properties( Char c ) throws ReflectiveOperationException {
		Field f = Char.class.getDeclaredField( "properties" );
		f.setAccessible( true );
		return (java.util.HashSet<Char.Property>) f.get( c );
	}
}
