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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aegis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArcaneEcho;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.BladeBind;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Blink;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Condemn;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CreepingDread;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FinishingBlow;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.IronStance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.KneeShot;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LastRites;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.LastStand;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MeteorCall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Shieldbearer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpellBlade;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Transcendence;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Venom;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WeaponBond;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The runes package (rfx plan 5.2: the runic language on the arcane, mana, ward and bind skills;
 * Spirit Volley's ward bolt back): every touched file keeps f0783eacd's draw sites of the game's
 * Random, kind by kind; the touched procs and casts played for real (RfxRunesProcs) draw it as
 * f0783eacd did, the same count and the same next value; none of the new pictures draws it; the
 * volley's bolts land on the ward missile's frame at 30, 60 and 144 fps with its one draw; a busy
 * arcane moment keeps every cap; Mana Shield leaves nothing behind; nothing touches the GPU while it is
 * made; no rune is held or stamped over a head, and every rune is in its meaning's colours; the casts
 * take their schools and colours by meaning; Spirit Armor's full ring and its spent mote are runes;
 * and no skill here borrows a scroll's runes, a star speck or a new reticle ring (source scans).
 */
public class SfxRfxRunesTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group mobs;
	private Hero hero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		stubGl();
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		resetKit();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 11 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		mobs = new Group();
		Dungeon.level = room();
		hero = new Hero();
		hero.HP = hero.HT = 50;
		Dungeon.hero = hero;
		link( plain(), hero, at( 3, 4 ) );
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		resetKit();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	private static void resetKit(){
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		PixelFlare.reset();
		LanceFX.reset();
		SkillSpectacleFX.reset();
		ShoutFX.reset();
		SkillFieldFX.clearCap();
		WaterFX.sceneCreated();
		RuneFX.reset();
		LinkFX.reset();
		SkillRay.reset();
		SkillBolt.reset();
	}

	private static Level room(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static CharSprite plain(){
		return new CharSprite(){
			{
				texture( xyz.gabriwar.warpedpixeldungeon.Assets.Effects.FX_SHEET );
				frame( 0, 0, 12, 15 );
			}
		};
	}

	//a sprite standing on its cell as the game places it
	private CharSprite link( CharSprite s, Char ch, int cell ){
		if (ch instanceof Mob) ((Mob) ch).state = ((Mob) ch).PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		mobs.add( s );
		s.place( cell );
		s.visible = true;
		return s;
	}

	private CharSprite rat( int cell ){
		Rat r = new Rat();
		r.HP = r.HT = 50;
		return link( new RatSprite(), r, cell );
	}

	private static void stubGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			String n = m.getName();
			if ((n.equals( "glGetShaderiv" ) || n.equals( "glGetProgramiv" )) && a[2] instanceof IntBuffer){
				((IntBuffer) a[2]).put( 0, 1 );
				return null;
			}
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			if (r == String.class) return "";
			return null;
		} );
	}

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			mobs.update();
			for (Group g : layers) g.update();
		}
	}

	//the frame (1 = the first update) a run's callback comes on, at most `max`
	private int arrival( java.util.function.Consumer<Runnable> start, int max ){
		final int[] at = { -1 }, f = { 0 };
		start.accept( () -> { if (at[0] < 0) at[0] = f[0]; } );
		for (f[0] = 1; f[0] <= max && at[0] < 0; f[0]++) frames( 1 );
		return at[0];
	}

	private Group lights(){
		return layers[GameScene.Layer.LIGHTS.ordinal()];
	}

	// ------------------------------------------------------------------ the game's Random

	/** Every touched file's draw sites of the game's Random, kind by kind, as f0783eacd has them
	 *  (bursts, starts, delayed starts, pours, lightnings and arcs, bolts, rains, Random calls,
	 *  splashes): the runes are kit calls, never a burst on a plain emitter. */
	@Test
	public void everyDrawSiteIsWhereF0783HadIt() throws Exception {
		String base = "src/main/java/xyz/gabriwar/warpedpixeldungeon/";
		Object[][] census = {
				{ "actors/buffs/SpiritArmorMotes.java", new int[]{ 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 } },
				{ "effects/SpiritArmorFX.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/SpiritArmor.java", new int[]{ 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "effects/SkillCastFX.java", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/ManaShield.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/buffs/ManaShieldWard.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/SpellBlade.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/Wizard.java", new int[]{ 2, 0, 0, 0, 0, 0, 1, 0, 1, 0 } },
				{ "actors/hero/skills/ArcaneEcho.java", new int[]{ 2, 0, 0, 0, 2, 0, 0, 0, 2, 0 } },
				{ "actors/hero/skills/Sorcerer.java", new int[]{ 3, 0, 0, 0, 1, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/Hex.java", new int[]{ 1, 0, 0, 0, 0, 0, 2, 0, 0, 0 } },
				{ "actors/hero/skills/Condemn.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 1, 0 } },
				{ "actors/hero/skills/WeaponBond.java", new int[]{ 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/Sanctuary.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
				{ "actors/hero/skills/Aegis.java", new int[]{ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } } };
		String[] sites = { "\\.burst\\s*\\(", "(?<!FxTimeline)\\.start\\s*\\(", "\\.startDelayed\\s*\\(", "\\.pour\\s*\\(",
				"new\\s+Lightning\\s*\\(", "new\\s+Lightning\\.Arc\\s*\\(",
				"(MagicMissile\\.boltFromChar|\\(\\s*\\(\\s*MagicMissile\\s*\\)[^;]*?\\.reset\\s*\\(|SkillBolt\\.fly\\s*\\()",
				"SkillFX\\.rain\\s*\\(",
				"(?<![A-Za-z])Random\\.(Int|Float|NormalIntRange|NormalFloat|IntRange|Long|chances|element|shuffle|oneOf|chance)\\b",
				"Splash\\.at\\s*\\(" };
		for (Object[] row : census){
			String src = source( base + row[0] );
			int[] want = (int[]) row[1];
			for (int k = 0; k < sites.length; k++){
				Matcher m = Pattern.compile( sites[k], Pattern.DOTALL ).matcher( src );
				int n = 0;
				while (m.find()) n++;
				assertEquals( row[0] + " " + sites[k], want[k], n );
			}
		}
	}

	/** RfxRunesProcs measured in a checkout of f0783eacd (RFX_RUNES_BASELINE=1): { name, draws, next * 1e6 }. */
	static final Object[][] F0783 = {
			{ "spirit-volley", 183, 517080 },
			{ "mana-shield", 51, 688976 },
			{ "spell-blade", 17, 722490 },
			{ "spell-blade-overflow", 19, 628235 },
			{ "arcane-echo", 84, 184849 },
			{ "sorcerer", 90, 402372 },
			{ "hex", 14, 2120 },
			{ "condemn", 13, 714072 },
			{ "weapon-bond", 9, 556843 },
			{ "sanctuary", 21, 838969 },
			{ "aegis", 13, 714072 } };

	@Test
	public void theProcsDrawTheGamesRandomAsF0783Did() throws Exception {
		LinkedHashMap<String, long[]> now = RfxRunesProcs.measure();
		assertEquals( F0783.length, now.size() );
		for (Object[] row : F0783){
			long[] n = now.get( (String) row[0] );
			assertNotNull( (String) row[0], n );
			assertEquals( row[0] + " draws", ((Integer) row[1]).longValue(), n[0] );
			assertEquals( row[0] + " next", ((Integer) row[2]).longValue(), n[1] );
		}
	}

	@Test
	public void theNewPicturesDrawNone(){
		CharSprite hs = hero.sprite, a = rat( at( 4, 4 ) ), b = rat( at( 6, 4 ) );
		assertEquals( 0, EnchantFxTest.draws( () -> {
			ManaShield.raised( hs );
			ManaShieldWard.absorbed( hs, a.ch.pos );
			ManaShieldWard.broke( hs );
			SpellBlade.inscribed( hs, a );
			ArcaneEcho.echo( a.ch.pos, hero.pos );
			RuneFX.Runes held = Sorcerer.charging( b );
			Hex.spoken( b );
			Condemn.branded( a );
			WeaponBond.bonded( hs );
			Sanctuary.raised( hs );
			Aegis.warded( hs, a.ch.pos );
			SpiritArmorFX.spend( hero, a.ch );
			frames( 30 );
			Sorcerer.discharged( held, b.ch.pos );
			Sorcerer.discharged( null, b.ch.pos );
			frames( 90 );
		} ) );
		//a cast of every school the package touched
		assertEquals( 0, EnchantFxTest.draws( () -> {
			for (Skill k : schools()){
				SkillCastFX.play( k, hero );
				frames( 40 );
			}
		} ) );
	}

	//one skill of each cast the package changed, at their first level
	private static Skill[] schools(){
		Skill[] k = { new SpiritArmor(), new ManaShield(), new Shieldbearer(), new IronStance(), new BladeBind(),
				new Transcendence(), new MeteorCall(), new Venom(), new Sanctuary() };
		for (Skill s : k) s.level = 1;
		return k;
	}

	// ------------------------------------------------------------------ the volley's frame

	@Test
	public void theVolleysBoltsLandOnTheWardMissilesFrame(){
		for (float dt : new float[]{ 1 / 30f, 1 / 60f, 1 / 144f }){
			Game.elapsed = dt;
			int old = arrival( cb -> ((MagicMissile) mobs.recycle( MagicMissile.class )).reset( MagicMissile.WARD,
					new PointF( 50, 70 ), new PointF( 150, 40 ), cb::run ), 400 );
			int now = arrival( cb -> SkillBolt.fly( mobs, new PointF( 50, 70 ), new PointF( 150, 40 ), SkillBolt.RUNE, cb::run ), 400 );
			assertTrue( old > 0 );
			assertEquals( "dt " + dt, old, now );
			frames( 60 );
		}
		Game.elapsed = 1 / 60f;
		//its one draw, as the missile's
		assertEquals( 1, EnchantFxTest.draws( () -> SkillBolt.fly( mobs, new PointF( 50, 70 ), new PointF( 150, 40 ), SkillBolt.RUNE, null ) ) );
	}

	// ------------------------------------------------------------------ caps, and nothing left behind

	@Test
	public void aBusyArcaneMomentKeepsEveryCap(){
		CharSprite[] r = new CharSprite[6];
		for (int i = 0; i < r.length; i++) r[i] = rat( at( 6 + i % 3, 2 + i / 3 * 4 ) );
		//rank 3's volley (four bolts), an echo with its chain, Mana Shield breaking and reforming,
		//Spell Blade, an overload winding up, a hex, a brand, the mana ward cast, an arcane cast
		for (int i = 0; i < 4; i++){
			SkillBolt.fly( mobs, hero.sprite.center(), r[i].center(), SkillBolt.RUNE, null );
			SpiritArmorFX.spend( hero, r[i].ch );
		}
		ArcaneEcho.echo( r[0].ch.pos, hero.pos );
		ArcaneEcho.echo( r[1].ch.pos, r[0].ch.pos );
		ManaShieldWard.broke( hero.sprite );
		ManaShield.raised( hero.sprite );
		SpellBlade.inscribed( hero.sprite, r[2] );
		RuneFX.Runes held = Sorcerer.charging( r[3] );
		Hex.spoken( r[4] );
		Condemn.branded( r[5] );
		ManaShield m = new ManaShield();
		m.level = 1;
		SkillCastFX.play( m, hero );
		int most = 0, runes = 0, lights = 0, flares = 0, rings = 0;
		for (int f = 0; f < 90; f++){
			if (f == 30) Sorcerer.discharged( held, r[3].ch.pos );
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
			runes = Math.max( runes, RuneFX.live() );
			lights = Math.max( lights, FxLight.live() );
			flares = Math.max( flares, PixelFlare.live() );
			rings = Math.max( rings, FxRing.live() );
		}
		assertTrue( "particles " + most, most <= FxBudget.soft() );
		assertTrue( "runes " + runes, runes > 0 && runes <= RuneFX.CAP );
		assertTrue( "lights " + lights, lights <= FxBudget.lightCap() );
		assertTrue( "flares " + flares, flares <= PixelFlare.CAP );
		assertTrue( "rings " + rings, rings <= FxBudget.ringCap() );
		//and every rune pattern has let itself go
		frames( 120 );
		assertEquals( 0, RuneFX.live() );
		assertEquals( 0, count( lights(), RuneFX.Runes.class ) );
	}

	@Test
	public void manaShieldLeavesNothingBehind(){
		CharSprite a = rat( at( 4, 4 ) );
		int lights = FxLight.live();
		ManaShield.raised( hero.sprite );
		frames( 10 );
		ManaShieldWard.absorbed( hero.sprite, a.ch.pos );
		frames( 10 );
		ManaShieldWard.broke( hero.sprite );
		frames( 5 );
		assertTrue( RuneFX.live() > 0 );
		frames( 120 );
		assertEquals( 0, RuneFX.live() );
		assertEquals( 0, count( lights(), RuneFX.Runes.class ) );
		assertEquals( 0, FxBudget.live( FxBudget.PARTICLES ) );
		assertEquals( 0, PixelFlare.live() );
		assertEquals( lights, FxLight.live() );
	}

	/** The ward breaking into the weapon: its runes are thrown apart from outside the body (from the
	 *  raised ward's RX x RY ring), so the ward's ring is seen breaking, and its flare is a small
	 *  one (never a white blob over the hero for a third of a second). */
	@Test
	public void aBrokenWardsRunesAreThrownFromOutsideTheBodyUnderASmallFlare(){
		CharSprite hs = hero.sprite;
		ManaShieldWard.broke( hs );
		int seen = 0, widest = 0;
		float far = 0;
		for (int f = 0; f < 40; f++){
			frames( 1 );
			float mx = SkillVfx.midX( hs ), my = SkillVfx.midY( hs );
			ArrayList<Shown> out = new ArrayList<>();
			runes( lights(), out );
			for (Shown s : out){
				seen++;
				float ex = (s.x - mx) / ManaShield.RX, ey = (s.y - my) / ManaShield.RY;
				assertTrue( "a broken rune inside the body (" + (s.x - mx) + ", " + (s.y - my) + ")", ex * ex + ey * ey >= 0.75f );
				far = Math.max( far, Math.abs( s.x - mx ) );
			}
			for (Gizmo g : lights().membersView()){
				if (g instanceof PixelFlare && g.exists && g.visible) widest = Math.max( widest, Math.round( ((PixelFlare) g).width() ) );
			}
		}
		assertTrue( seen > 0 );
		assertTrue( "thrown out " + far, far >= 12f );
		assertTrue( "a small flare: " + widest, widest > 0 && widest <= 15 );
		assertEquals( 8f, ManaShieldWard.SCATTER_FROM, 0f );
		assertEquals( 15f, ManaShieldWard.SCATTER_DIST, 0f );
		assertEquals( 0.45f, ManaShieldWard.SCATTER_LIFE, 0f );
		frames( 60 );
		assertEquals( 0, RuneFX.live() );
	}

	/** Dark Pact's seal reads as runes: a ring of three blood runes and one of three mana runes
	 *  round his feet, turning the two ways round on a PACT_RX x PACT_RY ellipse wider than
	 *  the body; two Runes gizmos, six runes. */
	@Test
	public void darkPactsSealIsTwoTrianglesOfRunesRoundTheFeet(){
		CharSprite hs = hero.sprite;
		DreadFX.pact( hs );
		int most = 0, gizmos = 0;
		boolean blood = false, mana = false;
		float far = 0;
		for (int f = 0; f < 30; f++){
			frames( 1 );
			gizmos = Math.max( gizmos, RuneFX.live() );
			ArrayList<Shown> out = new ArrayList<>();
			runes( lights(), out );
			most = Math.max( most, out.size() );
			for (Shown s : out){
				blood |= on( SkillPalette.RUNE_BLOOD, s.color );
				mana |= on( SkillPalette.RUNE_MANA, s.color );
				far = Math.max( far, Math.abs( s.x - SkillVfx.midX( hs ) ) );
			}
		}
		assertEquals( "two Runes", 2, gizmos );
		assertEquals( "six runes", 6, most );
		assertTrue( blood && mana );
		assertTrue( "outside the body: " + far, far >= 8f );
		assertEquals( 9f, DreadFX.PACT_RX, 0f );
		assertEquals( 3.5f, DreadFX.PACT_RY, 0f );
		frames( 60 );
		assertEquals( 0, RuneFX.live() );
	}

	/** A mana ward's runes lock round the feet where he stands: when he walks off, during the cast
	 *  or after it while they still hold, they fade where they are within WARD_LEAVE s, never left
	 *  lying on the floor (the Elemental Lock's tiles). Standing still, they hold their course. */
	@Test
	public void theManaWardsRunesFadeWhenHeWalksOff(){
		for (int at : new int[]{ 12, 36 }){
			ManaShield m = new ManaShield();
			m.level = 1;
			SkillCastFX.play( m, hero );
			frames( at );
			ArrayList<Shown> out = new ArrayList<>();
			runes( lights(), out );
			assertFalse( "held at frame " + at, out.isEmpty() );
			hero.sprite.x += 16;
			frames( Math.round( SkillCastFX.WARD_LEAVE * 60 ) + 3 );
			out.clear();
			runes( lights(), out );
			assertTrue( "left on the floor at frame " + at, out.isEmpty() );
			hero.sprite.x -= 16;
			frames( 90 );
			assertEquals( 0, RuneFX.live() );
		}
		//standing still: still there at 0.75 s, past the cast, and gone with their course
		ManaShield m = new ManaShield();
		m.level = 1;
		SkillCastFX.play( m, hero );
		frames( 45 );
		ArrayList<Shown> out = new ArrayList<>();
		runes( lights(), out );
		assertFalse( out.isEmpty() );
		frames( 60 );
		assertEquals( 0, RuneFX.live() );
	}

	/** An overloaded enemy dying before it discharges (below +3, where nothing bursts): the ring
	 *  winding up round it fades where it is within Sorcerer.FIZZLE s, never spinning on over a
	 *  body for a discharge that never comes. */
	@Test
	public void anOverloadDyingWithItsBodyLetsItsRingFade() throws Exception {
		Sorcerer sorcerer = new Sorcerer();
		hero.heroClass = HeroClass.MAGE;
		xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills cs =
				new xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills( HeroClass.MAGE );
		Field all = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills.class.getDeclaredField( "allSkills" );
		all.setAccessible( true );
		sorcerer.level = 2;
		((List<Skill>) all.get( cs )).add( 0, sorcerer );
		hero.heroSkills = cs;
		CharSprite a = rat( at( 6, 4 ) );
		xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile wand = new xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile();
		for (int i = 0; i < 80 && a.ch.buff( Sorcerer.Overload.class ) == null; i++) Sorcerer.onZap( wand, a.ch, a.ch.pos );
		assertNotNull( a.ch.buff( Sorcerer.Overload.class ) );
		frames( 15 );
		ArrayList<Shown> out = new ArrayList<>();
		runes( lights(), out );
		assertFalse( "the ring winding up", out.isEmpty() );
		sorcerer.onEnemyDeath( (Mob) a.ch, null );
		frames( Math.round( Sorcerer.FIZZLE * 60 ) + 3 );
		out.clear();
		runes( lights(), out );
		assertTrue( "the ring of a body gone", out.isEmpty() );
		assertEquals( 0, RuneFX.live() );
	}

	/** The +3 ward breaks, then reforms: its runes lock in again only once the broken ward's runes
	 *  have scattered and gone, never the two rings at once. */
	@Test
	public void theReformLocksInAfterTheBreakHasScattered(){
		ManaShieldWard.broke( hero.sprite );
		ManaShieldWard.reformed( hero.sprite );
		int most = 0;
		for (int f = 0; f < Math.round( ManaShieldWard.SCATTER_LIFE * 60 ) - 2; f++){
			frames( 1 );
			most = Math.max( most, RuneFX.live() );
		}
		assertEquals( "the break's scatter alone", 1, most );
		frames( 6 );
		assertEquals( 1, RuneFX.live() );
		ArrayList<Shown> out = new ArrayList<>();
		runes( lights(), out );
		assertFalse( "the reform's lock", out.isEmpty() );
		for (Shown sh : out) assertTrue( on( SkillPalette.RUNE_MANA, sh.color ) );
		frames( 90 );
		assertEquals( 0, RuneFX.live() );
	}

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite hs = hero.sprite, a = rat( at( 4, 4 ) );
		failOnGl();
		ManaShield.raised( hs );
		ManaShieldWard.absorbed( hs, a.ch.pos );
		ManaShieldWard.broke( hs );
		SpellBlade.inscribed( hs, a );
		ArcaneEcho.echo( a.ch.pos, hero.pos );
		Sorcerer.discharged( Sorcerer.charging( a ), a.ch.pos );
		Hex.spoken( a );
		Condemn.branded( a );
		WeaponBond.bonded( hs );
		Sanctuary.raised( hs );
		Aegis.warded( hs, a.ch.pos );
		SpiritArmorFX.spend( hero, a.ch );
		SkillBolt.fly( mobs, hs, a, SkillBolt.RUNE, null );
		for (Skill k : schools()) SkillCastFX.play( k, hero );
		frames( 40 );
	}

	// ------------------------------------------------------------------ where runes go, in what colours

	//a rune shown in the light layer: its middle and its colour
	private static final class Shown {
		final float x, y;
		final int color;

		Shown( float x, float y, int color ){
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	private static boolean rune( FxPic p ){
		float tw = p.texture.width, th = p.texture.height;
		int fx = Math.round( p.frame().left * tw ), fy = Math.round( p.frame().top * th );
		for (int[] f : FxFrames.RUNE_3) if (f[0] == fx && f[1] == fy) return true;
		for (int[] f : FxFrames.RUNE_5) if (f[0] == fx && f[1] == fy) return true;
		return false;
	}

	private static void runes( Group g, List<Shown> out ){
		for (Gizmo m : g.membersView()){
			if (m instanceof Group) runes( (Group) m, out );
			else if (m instanceof FxPic && !(m instanceof FxParticle) && m.visible && rune( (FxPic) m )){
				FxPic p = (FxPic) m;
				int c = (Math.round( p.rm * 255 ) << 16) | (Math.round( p.gm * 255 ) << 8) | Math.round( p.bm * 255 );
				out.add( new Shown( p.x + p.width() / 2f, p.y + p.height() / 2f, c ) );
			}
		}
	}

	//whether a colour lies on a ramp (lerped) within a step of rounding
	private static boolean on( Ramp r, int c ){
		for (int i = 0; i <= 200; i++){
			int k = r.at( i / 200f );
			if (Math.abs( ((k >> 16) & 0xFF) - ((c >> 16) & 0xFF) ) <= 3 && Math.abs( ((k >> 8) & 0xFF) - ((c >> 8) & 0xFF) ) <= 3
					&& Math.abs( (k & 0xFF) - (c & 0xFF) ) <= 3) return true;
		}
		return false;
	}

	//plays a picture and checks every rune it shows over `frames` frames: on its ramp (or one of the
	//extra colours), never above the top of the body it is about; returns how many it showed
	private int check( String what, Runnable play, CharSprite body, Ramp ramp, int... extra ){
		play.run();
		int seen = 0;
		for (int f = 0; f < 50; f++){
			frames( 1 );
			ArrayList<Shown> out = new ArrayList<>();
			runes( lights(), out );
			for (Shown s : out){
				seen++;
				assertTrue( what + ": a rune over the head (" + s.y + " above " + body.y + ")", s.y >= body.y );
				boolean ok = on( ramp, s.color );
				for (int c : extra) ok |= c == s.color;
				assertTrue( what + ": " + Integer.toHexString( s.color ), ok );
			}
		}
		frames( 60 );
		return seen;
	}

	@Test
	public void runesKeepToTheBodyAndToTheirMeaningsColours(){
		CharSprite hs = hero.sprite, a = rat( at( 4, 4 ) ), n = rat( at( 3, 3 ) );
		assertTrue( check( "mana shield", () -> ManaShield.raised( hs ), hs, SkillPalette.RUNE_MANA ) > 0 );
		//a blow from straight above: the arc on top of the body, never over the head
		assertTrue( check( "mana shield absorb", () -> ManaShieldWard.absorbed( hs, n.ch.pos ), hs, SkillPalette.RUNE_MANA ) > 0 );
		assertTrue( check( "mana shield break", () -> ManaShieldWard.broke( hs ), hs, SkillPalette.RUNE_MANA ) > 0 );
		assertTrue( check( "spell blade", () -> SpellBlade.inscribed( hs, a ), a, SkillPalette.RUNE_MANA, SkillPalette.MANA_LIGHT ) > 0 );
		assertTrue( check( "arcane echo", () -> ArcaneEcho.echo( a.ch.pos, hero.pos ), a, SkillPalette.RUNE_ARCANE ) > 0 );
		final RuneFX.Runes[] held = new RuneFX.Runes[1];
		assertTrue( check( "sorcerer", () -> held[0] = Sorcerer.charging( a ), a, SkillPalette.RUNE_ARCANE ) > 0 );
		//the ring held a while (charging's own check ran past it: the long wait's fresh ring)
		assertTrue( check( "sorcerer discharge", () -> Sorcerer.discharged( held[0], a.ch.pos ), a, SkillPalette.RUNE_ARCANE ) > 0 );
		assertTrue( check( "sorcerer held", () -> {
			held[0] = Sorcerer.charging( a );
			frames( 20 );
			Sorcerer.discharged( held[0], a.ch.pos );
		}, a, SkillPalette.RUNE_ARCANE ) > 0 );
		assertTrue( check( "hex", () -> Hex.spoken( a ), a, SkillPalette.RUNE_VOID ) > 0 );
		assertTrue( check( "condemn", () -> Condemn.branded( a ), a, SkillPalette.RUNE_HOLY ) > 0 );
		assertTrue( check( "weapon bond", () -> WeaponBond.bonded( hs ), hs, SkillPalette.RUNE_STEEL ) > 0 );
		assertTrue( check( "aegis", () -> Aegis.warded( hs, n.ch.pos ), hs, SkillPalette.RUNE_HOLY ) > 0 );
		ManaShield m = new ManaShield();
		m.level = 1;
		assertTrue( check( "mana ward cast", () -> SkillCastFX.play( m, hero ), hs, SkillPalette.RUNE_MANA ) > 0 );
		//Sanctuary's ring rises off the feet (a ring let go of, never held): its colours only
		ArrayList<Shown> out = new ArrayList<>();
		Sanctuary.raised( hs );
		for (int f = 0; f < 40; f++){
			frames( 1 );
			runes( lights(), out );
		}
		assertFalse( out.isEmpty() );
		for (Shown sh : out) assertTrue( on( SkillPalette.RUNE_HOLY, sh.color ) );
		//Condemn's seal is stamped on the body's middle, not over it
		Condemn.branded( a );
		frames( Math.round( (Condemn.SEAL_FOR + 0.1f) * 60 ) );
		out.clear();
		runes( lights(), out );
		assertEquals( 1, out.size() );
		assertEquals( SkillVfx.midY( a ), out.get( 0 ).y, 1.5f );
		assertEquals( SkillVfx.midX( a ), out.get( 0 ).x, 1.5f );
		assertEquals( SkillPalette.mid( SkillPalette.RUNE_HOLY ), out.get( 0 ).color );
	}

	// ------------------------------------------------------------------ casts by meaning

	@Test
	public void castsTakeTheirSchoolsAndColoursByMeaning(){
		Object[][] want = {
				{ new SpiritArmor(), School.ARCANE }, { new ManaShield(), School.ARCANE },
				{ new Shieldbearer(), School.MARTIAL }, { new IronStance(), School.MARTIAL },
				{ new BladeBind(), School.STEEL }, { new Transcendence(), School.HOLY },
				{ new MeteorCall(), School.FIRE }, { new Sanctuary(), School.HOLY },
				//Blink is a chain of the storm's lightning: its cast crackles the storm's static, never
				//the rogue's ink of fear and stealth its silhouette shares with Blackout and Vanish
				{ new Blink(), School.STORM } };
		for (Object[] w : want) assertSame( w[0].getClass().getSimpleName(), w[1], SkillCastFX.school( (Skill) w[0] ) );
		//the silhouettes stay the same pictures
		assertEquals( 17, SkillCastFX.sprite( new ManaShield() ) );
		assertEquals( 13, SkillCastFX.sprite( new MeteorCall() ) );
		assertEquals( 19, SkillCastFX.sprite( new Blink() ) );
		//a mana ward glows in mana blue; the rest in their school's colour
		assertTrue( SkillCastFX.manaWard( new SpiritArmor() ) && SkillCastFX.manaWard( new ManaShield() ) );
		assertFalse( SkillCastFX.manaWard( new Sanctuary() ) );
		assertEquals( 0x66CCFF, SkillCastFX.color( new ManaShield(), hero ) );
		assertEquals( 0xD9AE75, SkillCastFX.color( new IronStance(), hero ) );
		assertEquals( 0xB9DEEE, SkillCastFX.color( new BladeBind(), hero ) );
		assertEquals( 0xF1D99C, SkillCastFX.color( new Transcendence(), hero ) );
		assertEquals( 0xFF8418, SkillCastFX.color( new MeteorCall(), hero ) );
		assertEquals( 0x7FA6FF, SkillCastFX.color( new Blink(), hero ) );
		//the silhouettes' colours by meaning: arcane blue-violet, fire orange, fury red, nature leaf
		//green, frost pale cyan, a storm white-blue (never the rogue's lilac, peach or a cream sage)
		assertEquals( 0x9070F0, SkillCastFX.color( 13 ) );
		assertEquals( 0xFF8418, SkillCastFX.color( 20 ) );
		assertEquals( SkillPalette.FURY_LIGHT, SkillCastFX.color( 21 ) );
		assertEquals( 0xA8D880, SkillCastFX.color( 4 ) );
		assertEquals( 0xA4E9FF, SkillCastFX.color( 12 ) );
		assertEquals( 0x7FA6FF, SkillCastFX.color( 14 ) );
		assertEquals( 0xB89CE4, SkillCastFX.color( 15 ) );
		//a mana ward's runes plus its orbit within its quads
		assertTrue( FxOrbit.MOST + SkillCastFX.RUNES <= SkillCastFX.QUADS );
	}

	@Test
	public void martialAndSteelCastsShowNoRunes(){
		for (Skill k : new Skill[]{ new IronStance(), new BladeBind(), new Shieldbearer() }){
			k.level = 1;
			SkillCastFX.play( k, hero );
			for (int f = 0; f < 30; f++){
				frames( 1 );
				ArrayList<Shown> out = new ArrayList<>();
				runes( lights(), out );
				assertTrue( k.getClass().getSimpleName(), out.isEmpty() );
			}
			frames( 30 );
		}
	}

	/** The skills sprite() does not name (its default silhouette: the rogue's, the huntress's, the
	 *  warrior's and the cleric's procs, Venom to Last Rites) are not the arcane's: their casts show
	 *  no runes and glow in their caster's class colour, never the arcane's blue-violet. */
	@Test
	public void aCastWithNoLookOfItsOwnShowsNoRunesInItsCastersColour(){
		Object[][] want = {
				{ new Venom(), HeroClass.HUNTRESS, School.NATURE.color },
				{ new KneeShot(), HeroClass.HUNTRESS, School.NATURE.color },
				{ new CreepingDread(), HeroClass.ROGUE, School.SHADOW.color },
				{ new LastStand(), HeroClass.WARRIOR, School.MARTIAL.color },
				{ new FinishingBlow(), HeroClass.DUELIST, School.STEEL.color },
				{ new LastRites(), HeroClass.CLERIC, School.HOLY.color } };
		for (Object[] w : want){
			Skill k = (Skill) w[0];
			String name = k.getClass().getSimpleName();
			assertFalse( name, SkillCastFX.ownLook( k ) );
			hero.heroClass = (HeroClass) w[1];
			assertEquals( name, (int) w[2], SkillCastFX.color( k, hero ) );
			k.level = 1;
			SkillCastFX.play( k, hero );
			for (int f = 0; f < 40; f++){
				frames( 1 );
				ArrayList<Shown> out = new ArrayList<>();
				runes( lights(), out );
				assertTrue( name, out.isEmpty() );
			}
			frames( 30 );
		}
		//the named ones keep their own looks
		for (Skill k : new Skill[]{ new MeteorCall(), new Transcendence(), new ManaShield(), new IronStance(), new Hex() })
			assertTrue( k.getClass().getSimpleName(), SkillCastFX.ownLook( k ) );
	}

	// ------------------------------------------------------------------ Spirit Armor's ring

	@Test
	public void spiritArmorsFullRingAndItsSpentMoteAreRunes() throws Exception {
		CharSprite a = rat( at( 6, 4 ) );
		SpiritArmorMotes motes = new SpiritArmorMotes();
		motes.motes = 2;
		SpiritArmorFX ring = new SpiritArmorFX( hero, motes );
		mobs.add( ring );
		frames( 3 );
		FxOrbit.Ring orbit = FxOrbit.get( hero.sprite, FxOrbit.Slot.SPIRIT_ARMOR );
		assertNotNull( orbit );
		//rank 1 holds 2: full, its motes runes changing glyph four times a second
		assertTrue( motes.full() );
		java.util.HashSet<Integer> glyphs = new java.util.HashSet<>();
		for (int f = 0; f < 70; f++){
			frames( 1 );
			int[] shown = orbitFrame( orbit );
			int k = -1;
			for (int i = 0; i < FxFrames.RUNE_3.length; i++) if (FxFrames.RUNE_3[i][0] == shown[0] && FxFrames.RUNE_3[i][1] == shown[1]) k = i;
			assertTrue( "a full ring shows runes", k >= 0 );
			glyphs.add( k );
		}
		assertEquals( 4, glyphs.size() );
		//not full: dots
		motes.motes = 1;
		Field rank = SpiritArmorMotes.class.getDeclaredField( "rank" );
		rank.setAccessible( true );
		rank.setInt( motes, 3 );
		frames( 3 );
		int[] shown = orbitFrame( orbit );
		assertEquals( FxFrames.MOTE_2[0], shown[0] );
		assertEquals( FxFrames.MOTE_2[1], shown[1] );
		Fx.dispose( ring );
		//the spent mote leaves as a rune
		SpiritArmorFX.spend( hero, a.ch );
		Field frames = FxParticle.class.getDeclaredField( "frames" );
		frames.setAccessible( true );
		boolean found = false;
		for (FxParticle p : particles( lights(), new ArrayList<>() )) found |= frames.get( p ) == FxFrames.RUNE_3;
		assertTrue( found );
	}

	private static int[] orbitFrame( FxOrbit.Ring ring ) throws Exception {
		Field image = FxOrbit.Ring.class.getDeclaredField( "image" );
		image.setAccessible( true );
		com.watabou.noosa.Image im = (com.watabou.noosa.Image) image.get( ring );
		float tw = im.texture.width, th = im.texture.height;
		return new int[]{ Math.round( im.frame().left * tw ), Math.round( im.frame().top * th ) };
	}

	private static List<FxParticle> particles( Group g, List<FxParticle> out ){
		for (Gizmo m : g.membersView()){
			if (m instanceof FxParticle && m.alive) out.add( (FxParticle) m );
			else if (m instanceof Group) particles( (Group) m, out );
		}
		return out;
	}

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	// ------------------------------------------------------------------ nothing borrowed

	@Test
	public void noSkillHereBorrowsAScrollAStarOrAReticle() throws Exception {
		String base = "src/main/java/xyz/gabriwar/warpedpixeldungeon/";
		String[] files = { "actors/buffs/SpiritArmorMotes.java", "effects/SpiritArmorFX.java", "actors/hero/skills/SpiritArmor.java",
				"effects/SkillCastFX.java", "actors/hero/skills/ManaShield.java", "actors/buffs/ManaShieldWard.java",
				"actors/hero/skills/SpellBlade.java", "actors/hero/skills/Wizard.java", "actors/hero/skills/ArcaneEcho.java",
				"actors/hero/skills/Sorcerer.java", "actors/hero/skills/Hex.java", "actors/hero/skills/Condemn.java",
				"actors/hero/skills/WeaponBond.java", "actors/hero/skills/Sanctuary.java", "actors/hero/skills/Aegis.java" };
		for (String f : files){
			String src = source( base + f );
			//a scroll being read, the level-up's (or a stun's) stars
			for (String b : new String[]{ "Speck.SCROLL", "ItemFx", "Speck.STAR" })
				assertFalse( f + " uses " + b, src.contains( b ) );
		}
		//a full ward ring shows runes, never the heal's plus
		assertFalse( source( base + "effects/SpiritArmorFX.java" ).contains( "PLUS_3" ) );
		//no reticle on the condemned
		assertFalse( source( base + "actors/hero/skills/Condemn.java" ).contains( "ClosingRing" ) );
		//the volley throws the ward's runes
		assertTrue( source( base + "actors/buffs/SpiritArmorMotes.java" ).contains( "SkillBolt.RUNE" ) );
		//every rune of the package is said in its meaning's ramp (SkillPalette's runes, never a literal)
		for (String f : new String[]{ "actors/hero/skills/ManaShield.java", "actors/buffs/ManaShieldWard.java",
				"actors/hero/skills/SpellBlade.java", "actors/hero/skills/ArcaneEcho.java", "actors/hero/skills/Sorcerer.java",
				"actors/hero/skills/Hex.java", "actors/hero/skills/Condemn.java", "actors/hero/skills/WeaponBond.java",
				"actors/hero/skills/Sanctuary.java", "actors/hero/skills/Aegis.java", "effects/SkillCastFX.java" }){
			Matcher m = Pattern.compile( "RuneFX\\.(ring|lock|shut|scatter|rise)\\s*\\((.*?)\\);", Pattern.DOTALL ).matcher( source( base + f ) );
			int n = 0;
			while (m.find()){
				n++;
				assertTrue( f + ": " + m.group(), m.group( 2 ).contains( "SkillPalette.RUNE_" ) );
			}
			assertTrue( f, n > 0 );
		}
	}

	//a source file without its comments
	private static String source( String path ) throws Exception {
		String s = new String( Files.readAllBytes( new File( path ).toPath() ), "UTF-8" );
		s = s.replaceAll( "(?s)/\\*.*?\\*/", "" );
		return s.replaceAll( "//[^\\n]*", "" );
	}
}
