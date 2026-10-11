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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ElementalOrbit;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.FrostNova;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.HealingPrayer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.ElementalOrbitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.MasterThiefCoinsFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillMarkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SpiritArmorFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.WhirlHitFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
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
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The skills area's effects, headless (stand-in layers for the game's scene): the moments the
 * game waits on (a pillar's peak, an elemental orb's impact) come on the same frame as before;
 * nothing is made per spark; the caps hold (spectacles, flares, the fields' particles, every
 * orbit's quads); a timeline's steps scheduled before the overworld window slid are dropped; the
 * shapes' arithmetic (an arc's steps, a pulse's rings, a chain's sag, a closing ring, a pillar's
 * reveal); nothing touches the GPU while made; and none of it draws the game's Random.
 */
public class SkillsFxTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group mobs;
	private Level level;

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
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		PixelFlare.reset();
		SkillFieldFX.clearCap();
		WaterFX.sceneCreated();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 11 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		mobs = new Group();
		level = room( new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		} );
		Dungeon.level = level;
		Hero hero = new Hero();
		hero.pos = at( 4, 4 );
		Dungeon.hero = hero;
		link( plain(), hero, hero.pos );
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		PixelFlare.reset();
		SkillFieldFX.clearCap();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//an open floor with a pool in columns 9-12 of rows 3-8; all of it seen
	private static Level room( Level l ){
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 3; y <= 8; y++) for (int x = 9; x <= 12; x++) l.map[x + y * W] = Terrain.WATER;
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

	//a sprite with a frame of its own and nothing of a mob's (the hero's stand-in)
	private static CharSprite plain(){
		return new CharSprite(){
			{
				texture( xyz.gabriwar.warpedpixeldungeon.Assets.Effects.FX_SHEET );
				frame( 0, 0, 12, 15 );
			}
		};
	}

	//a sprite on the stand-in mobs group, linked by hand to its character
	private CharSprite link( CharSprite s, xyz.gabriwar.warpedpixeldungeon.actors.Char ch, int cell ){
		if (ch instanceof Mob) ((Mob) ch).state = ((Mob) ch).PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		mobs.add( s );
		s.x = (cell % W) * 16 + 2;
		s.y = (cell / W) * 16;
		s.visible = true;
		return s;
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

	private static int count( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.exists && c.isInstance( m )) n++;
			if (m instanceof Group) n += count( (Group) m, c );
		}
		return n;
	}

	private int everywhere( Class<?> c ){
		int n = count( mobs, c );
		for (Group g : layers) n += count( g, c );
		return n;
	}

	// ------------------------------------------------------------------ what the game waits on

	@Test
	public void aPillarPeaksAtTheSameFrameAsBefore(){
		for (float dt : new float[]{ 1 / 60f, 1 / 30f, 1 / 45f, 0.05f }){
			Game.elapsed = dt;
			final int[] peakedAt = { -1 };
			final int[] frame = { 0 };
			PillarRiseFX.show( at( 6, 6 ), 0xFFE080, () -> peakedAt[0] = frame[0] );
			//before: the first update whose accumulated time reaches RISE
			float t = 0;
			int expected = -1;
			for (int i = 1; i < 100 && expected < 0; i++){
				t += dt;
				if (t >= PillarRiseFX.RISE) expected = i;
			}
			for (frame[0] = 1; frame[0] < 100 && peakedAt[0] < 0; frame[0]++) frames( 1 );
			assertEquals( "dt " + dt, expected, peakedAt[0] );
			frames( 120 );
			assertEquals( 0, everywhere( PillarRiseFX.class ) );
		}
	}

	@Test
	public void aPillarUnseenPeaksAtOnce(){
		level.heroFOV[at( 6, 6 )] = false;
		final boolean[] peaked = { false };
		PillarRiseFX.show( at( 6, 6 ), 0xFFE080, () -> peaked[0] = true );
		assertTrue( peaked[0] );
	}

	@Test
	public void anElementalOrbLandsOnTheSameFrameAsBefore(){
		Rat rat = new Rat();
		CharSprite s = link( new RatSprite(), rat, at( 2, 2 ) );
		int target = at( 8, 6 );
		for (float dt : new float[]{ 1 / 60f, 1 / 30f }){
			Game.elapsed = dt;
			//before: from the sprite's middle to the cell's, clamp(d / 160, 0.15, 0.5) s
			float fx = s.x + s.width() / 2f, fy = s.y + s.height() / 2f;
			float tx = (target % W + 0.5f) * 16, ty = (target / W + 0.5f) * 16;
			float duration = Math.max( .15f, Math.min( .5f, (float) Math.hypot( tx - fx, ty - fy ) / 160f ) );
			float elapsed = 0;
			int expected = -1;
			for (int i = 1; i < 200 && expected < 0; i++){
				elapsed += dt;
				if (Math.min( 1, elapsed / duration ) >= 1) expected = i;
			}
			final int[] frame = { 0 };
			final int[] landed = { -1 };
			ElementalOrbitFX.launch( rat, target, 0, 3, () -> landed[0] = frame[0] );
			for (frame[0] = 1; frame[0] < 200 && landed[0] < 0; frame[0]++) frames( 1 );
			assertEquals( "dt " + dt, expected, landed[0] );
		}
	}

	@Test
	public void anOrbWithNoSpriteLandsAtOnce(){
		Rat rat = new Rat();
		final boolean[] landed = { false };
		ElementalOrbitFX.launch( rat, at( 5, 5 ), 1, 0, () -> landed[0] = true );
		assertTrue( landed[0] );
	}

	// ------------------------------------------------------------------ nothing made per spark

	@Test
	public void anOrbitMakesNoPicturePerSpark(){
		Rat rat = new Rat();
		link( new RatSprite(), rat, at( 3, 3 ) );
		ElementalOrbit orbit = new ElementalOrbit();
		Random.pushGenerator( 3 );
		try {
			orbit.set( 3 );
		} finally {
			Random.popGenerator();
		}
		for (int i = 0; i < 30; i++) orbit.warm();
		ElementalOrbitFX fx = new ElementalOrbitFX( rat, orbit );
		ElementalOrbitFX.show( fx );
		int[] sizes = new int[100];
		for (int i = 0; i < 100; i++){
			frames( 1 );
			sizes[i] = count( fx, Image.class );
		}
		//its own pictures: three for each of its five orbs, from the first frame to the last
		for (int n : sizes) assertEquals( 3 * ElementalOrbit.MAX_ORBS, n );
		//and what it sheds comes from the kit's pools: no more than lives at once
		int pooled = count( layers[GameScene.Layer.LIGHTS.ordinal()], Image.class ) + count( layers[GameScene.Layer.EFFECTS.ordinal()], Image.class );
		frames( 100 );
		int later = count( layers[GameScene.Layer.LIGHTS.ordinal()], Image.class ) + count( layers[GameScene.Layer.EFFECTS.ordinal()], Image.class );
		assertTrue( "pooled " + pooled + " later " + later, later <= pooled + 8 );
	}

	@Test
	public void theOrbitDrawsTheOrbsOfOneMoment(){
		Rat rat = new Rat();
		link( new RatSprite(), rat, at( 3, 3 ) );
		ElementalOrbit orbit = new ElementalOrbit();
		Random.pushGenerator( 3 );
		try {
			orbit.set( 3 );
		} finally {
			Random.popGenerator();
		}
		int[][] before = orbit.view();
		assertEquals( ElementalOrbit.MAX_ORBS, before[0].length );
		assertEquals( before[0].length, before[1].length );
		ElementalOrbitFX fx = new ElementalOrbitFX( rat, orbit );
		ElementalOrbitFX.show( fx );
		frames( 2 );
		//two launched between frames: a new pair of arrays, the one already read left whole
		orbit.launch();
		orbit.launch();
		int[][] after = orbit.view();
		assertEquals( ElementalOrbit.MAX_ORBS - 2, after[0].length );
		assertEquals( after[0].length, after[1].length );
		assertEquals( ElementalOrbit.MAX_ORBS, before[0].length );
		frames( 2 );
		int shown = 0;
		for (Gizmo g : fx.membersView()) if (g instanceof Group && ((Group) g).visible) shown++;
		assertEquals( ElementalOrbit.MAX_ORBS - 2, shown );
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void spectaclesKeepToTheirCap(){
		for (int i = 0; i < 200; i++){
			SkillSpectacleFX.fly( SkillSpectacleFX.SHURIKEN, at( 2, 2 ), at( 6, 6 ), 0, 0.4f );
			SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, at( 5, 5 ) );
		}
		assertEquals( SkillSpectacleFX.CAP, SkillSpectacleFX.live() );
		assertEquals( "in the layers", SkillSpectacleFX.CAP, everywhere( SkillSpectacleFX.class ) );
		frames( 90 );
		assertEquals( 0, SkillSpectacleFX.live() );
		//and room again
		SkillSpectacleFX.show( SkillSpectacleFX.JAW, at( 5, 5 ) );
		assertEquals( 1, SkillSpectacleFX.live() );
	}

	@Test
	public void flaresKeepToTheirCap(){
		for (int i = 0; i < 60; i++) SkillInteractions.flare( at( 1 + i % 12, 2 + i / 12 ), 0xFFF1A1 );
		assertEquals( PixelFlare.CAP, PixelFlare.live() );
		assertTrue( FxLight.live() <= FxBudget.lightCap() );
		frames( 30 );
		assertEquals( 0, PixelFlare.live() );
	}

	@Test
	public void fieldsShareTheirParticleCap(){
		for (int i = 0; i < SkillFieldFX.CAP; i++) assertTrue( SkillFieldFX_take( 1f ) );
		assertFalse( SkillFieldFX_take( 1f ) );
		//past every life, room again
		Game.timeTotal += SkillFieldFX.MAX_LIFE + 0.01f;
		assertTrue( SkillFieldFX_take( 1f ) );
	}

	private static boolean SkillFieldFX_take( float life ){
		try {
			java.lang.reflect.Method m = SkillFieldFX.class.getDeclaredMethod( "take", float.class, float.class );
			m.setAccessible( true );
			return (Boolean) m.invoke( null, Game.timeTotal, life );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void manyFieldsNeverPassTheCap(){
		int[] kinds = { SkillField.FRAGMENT, SkillField.ICE, SkillField.SANCTUARY, SkillField.SOUL, SkillField.SMOKE, SkillField.WIND };
		for (int k : kinds){
			int[] cells = new int[12];
			for (int i = 0; i < cells.length; i++) cells[i] = at( 1 + i, 1 + Arrays.binarySearch( kinds, k ) % 6 + (k % 2) * 7 );
			SkillFieldFX.show( new SkillFieldFX( field( k, cells ) ) );
		}
		int most = 0;
		for (int i = 0; i < 240; i++){
			frames( 1 );
			most = Math.max( most, FxBudget.live( FxBudget.PARTICLES ) );
		}
		assertTrue( "most " + most, most <= SkillFieldFX.CAP );
		//the fragment fields' heat: at most POOLS pools, and all persistent ones within the kit's cap
		assertTrue( FxLight.livePersistent() <= FxBudget.persistentLightCap() );
	}

	private static SkillField field( int kind, int[] cells ){
		Bundle b = new Bundle();
		b.put( "kind", kind );
		b.put( "rank", 1 );
		b.put( "remaining", 5 );
		b.put( "age", 0 );
		b.put( "cells", cells );
		b.put( "depth", Dungeon.depth );
		b.put( "branch", Dungeon.branch );
		b.put( "origin", cells[0] );
		SkillField f = new SkillField();
		f.restoreFromBundle( b );
		return f;
	}

	@Test
	public void orbitsKeepToTheKitsQuads(){
		for (int n = 1; n <= SpiritArmorMotes.capacity( 3 ); n++){
			assertTrue( "spirit armour " + n, n * (1 + SpiritArmorFX.trailFor( n )) <= FxOrbit.MOST );
		}
		assertTrue( StanceOrbit.MOTES * (1 + StanceOrbit.TRAIL) <= FxOrbit.MOST );
		assertTrue( MasterThiefCoins.MAX_COINS <= FxOrbit.MOST );
		//a cast: its orbit's quads and its school's particles within its own cap
		int most = 0;
		for (School s : School.values()) most = Math.max( most, SkillCastFX.particles( s ) );
		assertTrue( FxOrbit.MOST + most <= SkillCastFX.QUADS );
	}

	@Test
	public void threeOrbitsAtOnceComeAndGo(){
		Rat rat = new Rat();
		CharSprite s = link( new RatSprite(), rat, at( 3, 3 ) );
		SpiritArmorMotes motes = new SpiritArmorMotes();
		motes.motes = 3;
		MasterThiefCoins coins = new MasterThiefCoins();
		coins.countUp( 4 );
		StanceOrbit stance = new StanceOrbit( rat, 0xFFB060 );
		SpiritArmorFX armour = new SpiritArmorFX( rat, motes );
		MasterThiefCoinsFX purse = new MasterThiefCoinsFX( rat, coins );
		mobs.add( stance );
		mobs.add( armour );
		mobs.add( purse );
		frames( 3 );
		assertEquals( 3, FxOrbit.count( s ) );
		coins.countUp( 1 );
		motes.motes = 1;
		frames( 3 );
		assertEquals( 3, FxOrbit.count( s ) );
		Fx.dispose( armour );
		Fx.dispose( purse );
		frames( 1 );
		assertEquals( 1, FxOrbit.count( s ) );
		//the sprite gone: the last one goes with it
		s.killAndErase();
		frames( 1 );
		assertEquals( 0, FxOrbit.count( s ) );
		assertFalse( stance.exists );
	}

	@Test
	public void stancesHeldTogetherShareTheirOrbit(){
		Rat rat = new Rat();
		CharSprite s = link( new RatSprite(), rat, at( 3, 3 ) );
		StanceOrbit ember = new StanceOrbit( rat, 0xFF5A4A ), aim = new StanceOrbit( rat, 0xFFE070 );
		mobs.add( ember );
		mobs.add( aim );
		frames( 3 );
		FxOrbit.Ring ring = FxOrbit.get( s, FxOrbit.Slot.STANCE );
		assertEquals( 1, FxOrbit.count( s ) );
		//both stances' motes, each pair in its own colour, within the orbit's quads
		assertEquals( 2 * StanceOrbit.MOTES, ringInt( ring, "count" ) );
		assertEquals( 2, ringInt( ring, "tintCount" ) );
		assertTrue( 2 * StanceOrbit.MOTES * (1 + ringInt( ring, "trail" )) <= FxOrbit.MOST );
		//one lowered: the same orbit circles on (no new phase), with the other's two motes
		Fx.dispose( ember );
		frames( 2 );
		assertSame( ring, FxOrbit.get( s, FxOrbit.Slot.STANCE ) );
		assertFalse( ring.isDetached() );
		assertEquals( StanceOrbit.MOTES, ringInt( ring, "count" ) );
		assertEquals( 0, ringInt( ring, "tintCount" ) );
		assertEquals( StanceOrbit.TRAIL, ringInt( ring, "trail" ) );
		//the last one lowered: the orbit goes
		Fx.dispose( aim );
		frames( 1 );
		assertTrue( ring.isDetached() );
		assertEquals( 0, FxOrbit.count( s ) );
	}

	@Test
	public void aKeeperTakenOffTheSceneDropsOutOfItsShare(){
		Rat rat = new Rat();
		CharSprite s = link( new RatSprite(), rat, at( 3, 3 ) );
		StanceOrbit a = new StanceOrbit( rat, 0xFF5A4A ), b = new StanceOrbit( rat, 0xFFE070 );
		mobs.add( a );
		mobs.add( b );
		frames( 2 );
		FxOrbit.Ring ring = FxOrbit.get( s, FxOrbit.Slot.STANCE );
		//off the scene without being let go of: it no longer updates
		mobs.remove( a );
		frames( 3 );
		assertSame( ring, FxOrbit.get( s, FxOrbit.Slot.STANCE ) );
		assertEquals( StanceOrbit.MOTES, ringInt( ring, "count" ) );
		Fx.dispose( b );
		frames( 1 );
		assertTrue( ring.isDetached() );
	}

	@Test
	public void marksOnOneBearerShareTheirOrbit(){
		Rat rat = new Rat();
		CharSprite s = link( new RatSprite(), rat, at( 3, 3 ) );
		SkillInteractions.Mark shards = new SkillInteractions.Mark();
		shards.kind = SkillInteractions.Mark.SHARDS;
		shards.power = 6;
		shards.target = rat;
		SkillInteractions.Mark guard = new SkillInteractions.Mark();
		guard.kind = SkillInteractions.Mark.GUARD;
		guard.power = 6;
		guard.target = rat;
		SkillMarkFX first = new SkillMarkFX( shards ), second = new SkillMarkFX( guard );
		mobs.add( first );
		mobs.add( second );
		frames( 3 );
		FxOrbit.Ring ring = FxOrbit.get( s, FxOrbit.Slot.MARK );
		assertEquals( 2, ringInt( ring, "count" ) );
		assertEquals( 2, ringInt( ring, "tintCount" ) );
		Fx.dispose( first );
		frames( 2 );
		assertSame( ring, FxOrbit.get( s, FxOrbit.Slot.MARK ) );
		assertEquals( 1, ringInt( ring, "count" ) );
		Fx.dispose( second );
		frames( 1 );
		assertTrue( ring.isDetached() );
	}

	private static int ringInt( FxOrbit.Ring ring, String field ){
		try {
			java.lang.reflect.Field f = FxOrbit.Ring.class.getDeclaredField( field );
			f.setAccessible( true );
			return f.getInt( ring );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	// ------------------------------------------------------------------ a slid window

	@Test
	public void staleTimelineStepsAreDroppedAfterTheWindowSlides(){
		OverworldLevel world = (OverworldLevel) room( new OverworldLevel() );
		Dungeon.level = world;
		world.worldX = 100;
		world.worldY = 40;
		final boolean[] ran = { false, false, false };
		FxTimeline.start().at( 0.05f, () -> ran[0] = true ).at( 0.3f, () -> ran[1] = true );
		StaggerFX.after( 0.3f, () -> ran[2] = true );
		frames( 6 );
		assertTrue( ran[0] );
		//the window slides a chunk east: what was scheduled names cells that moved
		world.worldX += 32;
		frames( 40 );
		assertFalse( ran[1] );
		assertFalse( ran[2] );
		assertEquals( 0, count( mobs, FxTimeline.class ) );
		//scheduled after the slide, it runs
		FxTimeline.start().at( 0.1f, () -> ran[1] = true );
		frames( 12 );
		assertTrue( ran[1] );
	}

	@Test
	public void timelinesRunOnALevelThatNeverSlides(){
		final boolean[] ran = { false };
		FxTimeline.start().at( 0.2f, () -> ran[0] = true );
		frames( 15 );
		assertTrue( ran[0] );
	}

	// ------------------------------------------------------------------ shapes

	@Test
	public void anArcStepsADirectionAtItsSpin(){
		assertEquals( 0.05f, ArcSpinFX.stepEvery( 900 ), 1e-6f );
		assertEquals( ArcSpinFX.STEP_MIN, ArcSpinFX.stepEvery( 1300 ), 1e-6f );
		assertEquals( ArcSpinFX.STEP_MAX, ArcSpinFX.stepEvery( 500 ), 1e-6f );
		assertEquals( ArcSpinFX.STEP_MAX, ArcSpinFX.stepEvery( 0 ), 1e-6f );
		assertEquals( 0.05f, ArcSpinFX.stepEvery( -900 ), 1e-6f );
		assertEquals( 5, ArcSpinFX.stepsFor( 0.22f, 0.05f ) );
		assertEquals( 3, ArcSpinFX.stepsFor( 0.12f, 0.04f ) );
		//east first, clockwise, wrapping both ways
		assertEquals( 1, ArcSpinFX.direction( 0, 1, 1 ) );
		assertEquals( 0, ArcSpinFX.direction( 7, 1, 1 ) );
		assertEquals( 7, ArcSpinFX.direction( 0, -1, 1 ) );
		assertEquals( 6, ArcSpinFX.direction( 0, -1, 10 ) );
		//its crescent's edge on the radius, never flung far out
		assertEquals( 10 - 0.255f * 16, ArcSpinFX.offset( 10, 16 ), 1e-4f );
		assertEquals( 0f, ArcSpinFX.offset( 2, 24 ), 0f );
		assertEquals( 8f, ArcSpinFX.offset( 40, 16 ), 0f );
		//about a 12 px sprite: the crescent's body (3 px inside its edge) clears his side
		assertEquals( 10f, ArcSpinFX.aroundRadius( 7, 12 ), 0f );
		assertEquals( 10f, ArcSpinFX.aroundRadius( 9, 12 ), 0f );
		assertEquals( 11f, ArcSpinFX.aroundRadius( 11, 12 ), 0f );
		assertTrue( ArcSpinFX.aroundRadius( 7, 12 ) - ArcSpinFX.BODY > 6f );
		//a white blade's ghosts are steel, a coloured arc's its own colour
		assertEquals( School.STEEL.color, ArcSpinFX.ghostColor( 0xFFFFFF ) );
		assertEquals( 0xCCE0FF, ArcSpinFX.ghostColor( 0xCCE0FF ) );
	}

	@Test
	public void anArcDrawsThreePicturesAndAFewSparks(){
		ArcSpinFX fx = ArcSpinFX.around( Dungeon.hero.sprite, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
		int most = 0;
		for (int i = 0; i < 40; i++){
			frames( 1 );
			if (!fx.exists) break;
			int shown = 0;
			for (Gizmo g : fx.membersView()) if (g instanceof Image && g.exists && ((Image) g).visible) shown++;
			most = Math.max( most, shown );
		}
		assertEquals( 3, most );
		frames( 30 );
		assertFalse( fx.exists );
	}

	@Test
	public void aThrustLeavesNoAfterimagesADashDoes(){
		//a riposte, a counter, an impaling thrust: the hero stays where he stands
		StreakFX.show( Dungeon.hero.pos, at( 8, 4 ), 0xFFFFFF, 0.4f, 0.25f );
		frames( 1 );
		assertEquals( 0, Afterimage.live() );
		//a fleche, a lunge, a sidestep: he crosses it
		StreakFX.dash( Dungeon.hero.pos, at( 8, 4 ), 0xFFFFFF, 0.5f, 0.3f, true );
		frames( 1 );
		assertTrue( Afterimage.live() >= 2 );
	}

	@Test
	public void onlyTheHammerCracksTheGround(){
		SkillSpectacleFX.show( SkillSpectacleFX.SPIRE, at( 5, 5 ) );
		frames( 60 );
		assertEquals( "a spire of light", 0, FxDecal.live() );
		SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, at( 5, 5 ) );
		frames( 60 );
		assertTrue( "a hammer", FxDecal.live() > 0 );
	}

	@Test
	public void aRainStrikesAsWhatFalls(){
		assertEquals( Element.STEEL, SkillFX.rainElement( new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart() ) );
		assertEquals( Element.FIRE, SkillFX.rainElement( new xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb() ) );
		assertEquals( Element.EARTH, SkillFX.rainElement( new xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast() ) );
	}

	@Test
	public void aVolleyStrikesInFullOnlyAFewAFrame(){
		//forty darts on one cell (its Random draws are the game's own, as before)
		Random.pushGenerator( 5 );
		try {
			SkillFX.rain( at( 6, 6 ), new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart(), 40, null );
		} finally {
			Random.popGenerator();
		}
		int last = FxRing.live(), most = 0, total = 0;
		for (int i = 0; i < 90; i++){
			frames( 1 );
			int now = FxRing.live();
			most = Math.max( most, now - last );
			total += Math.max( 0, now - last );
			last = now;
		}
		assertTrue( "rings a frame " + most, most <= SkillFX.RAIN_RICH );
		assertTrue( "struck at all", total > 0 );
	}

	@Test
	public void aLandingOnWaterMakesOneRing(){
		//a splash alone, then the landing: the same rings (the splash's own), none added beside it
		int before = FxRing.live();
		WaterFX.splash( SkillVfx.cx( at( 10, 5 ) ), SkillVfx.cy( at( 10, 5 ) ) + 4, WaterFX.M, Element.NONE );
		int splash = FxRing.live() - before;
		assertTrue( splash > 0 );
		frames( 120 );
		before = FxRing.live();
		SkillFX.landing( at( 10, 5 ) );
		assertEquals( splash, FxRing.live() - before );
	}

	@Test
	public void aPulseSpreadsOnPaintedRings(){
		assertEquals( 41, PulseRingFX.ringWidth( 20 ) );
		assertEquals( 73, PulseRingFX.ringWidth( 40 ) );
		assertEquals( 23, PulseRingFX.ringWidth( 12 ) );
		for (float r = 4; r < 60; r += 1){
			int w = PulseRingFX.ringWidth( r );
			boolean painted = false;
			for (int pw : FxFrames.RING_WIDTHS) painted |= pw == w;
			assertTrue( painted );
			int m = PulseRingFX.moteCount( r );
			assertTrue( m >= PulseRingFX.MOTES_MIN && m <= PulseRingFX.MOTES_MAX );
		}
		//over the pool: a small ring on the water where it passes, at most WATER_RINGS
		int before = FxRing.live();
		PulseRingFX.at( at( 10, 5 ), 0xBBFFCC, 40, 0.6f );
		frames( 50 );
		assertTrue( FxRing.live() >= before );
	}

	@Test
	public void aChainSagsAtItsMiddle(){
		assertEquals( 0f, SkillMarkFX.catenary( 0 ), 0f );
		assertEquals( 0f, SkillMarkFX.catenary( 1 ), 0f );
		assertEquals( 1f, SkillMarkFX.catenary( 0.5f ), 0f );
		assertTrue( SkillMarkFX.catenary( 0.25f ) < 1f );
		assertTrue( SkillMarkFX.sag( 10 ) < SkillMarkFX.sag( 60 ) );
		assertTrue( SkillMarkFX.sag( 1000 ) <= 6f );
	}

	@Test
	public void aClosingRingStepsInward(){
		int from = FxFrames.ringIndex( 31 ), to = FxFrames.ringIndex( 9 );
		assertEquals( from, ClosingRing.frameAt( from, to, 0 ) );
		assertEquals( to, ClosingRing.frameAt( from, to, 1 ) );
		int last = from;
		for (float p = 0; p <= 1; p += 0.02f){
			int f = ClosingRing.frameAt( from, to, p );
			assertTrue( f <= last && f >= to );
			last = f;
		}
	}

	@Test
	public void aPillarIsRevealedFromTheGroundUp(){
		assertEquals( 0f, PillarRiseFX.reveal( 0 ), 0f );
		assertEquals( 1f, PillarRiseFX.reveal( PillarRiseFX.RISE ), 0f );
		assertEquals( 1f, PillarRiseFX.reveal( 1f ), 0f );
		float last = 0;
		for (float t = 0; t < PillarRiseFX.RISE; t += 0.01f){
			float r = PillarRiseFX.reveal( t );
			assertTrue( r >= last );
			last = r;
		}
		//easing out: most of it shows in the first half
		assertTrue( PillarRiseFX.reveal( PillarRiseFX.RISE / 2 ) > 0.6f );
	}

	@Test
	public void aFlareIsBrightestAtItsHeight(){
		assertEquals( 0, PixelFlare.frameAt( 0, 0.2f ) & 2 );
		assertEquals( 2, PixelFlare.frameAt( 0.1f, 0.2f ) & 2 );
		assertEquals( 0, PixelFlare.frameAt( 0.19f, 0.2f ) & 2 );
	}

	@Test
	public void orbHalosGrowAPaintedSizeWithHeat(){
		int last = 0;
		for (int h = 0; h <= 3; h++){
			int s = ElementalOrbitFX.haloSize( h );
			assertTrue( s >= last );
			boolean painted = false;
			for (int g : FxFrames.Light.GLOW_SIZES) painted |= g == s;
			assertTrue( painted );
			last = s;
		}
	}

	// ------------------------------------------------------------------ the GPU, the game's Random

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		Rat rat = new Rat();
		link( new RatSprite(), rat, at( 3, 3 ) );
		failOnGl();
		makeEverything( rat );
		//nor the frames that follow, till it is drawn
		frames( 40 );
	}

	@Test
	public void noneOfItDrawsTheGamesRandom(){
		Rat rat = new Rat();
		link( new RatSprite(), rat, at( 3, 3 ) );
		Random.pushGenerator( 99 );
		float expected;
		try {
			makeEverything( rat );
			frames( 60 );
			expected = Random.Float();
		} finally {
			Random.popGenerator();
		}
		Random.pushGenerator( 99 );
		try {
			assertEquals( Random.Float(), expected, 0f );
		} finally {
			Random.popGenerator();
		}
	}

	//every effect of the area once (none of those whose callers burst the game's own emitters)
	private void makeEverything( Rat rat ){
		CharSprite hs = Dungeon.hero.sprite;
		ArcSpinFX.slash( hs, 0xFFFFFF, true );
		ArcSpinFX.around( hs, 0xCCE0FF, 10, 0.5f, 0, 1080, 0.5f, true );
		ArcSpinFX.at( at( 6, 6 ), 0xFFFFFF, 10, 0.25f, 200, 900, 0.22f );
		PulseRingFX.around( hs, 0xFFD060, 20, 0.6f );
		PulseRingFX.at( at( 10, 5 ), 0xBBFFCC, 36, 0.6f );
		PillarRiseFX.show( at( 6, 6 ), 0xFFE080, null );
		StreakFX.dash( Dungeon.hero.pos, at( 8, 4 ), 0xFFFFFF, 0.5f, 0.3f, true );
		StreakFX.show( Dungeon.hero.pos, at( 6, 4 ), 0xFFFFFF, 0.4f, 0.25f );
		Skill[] casts = { new HealingPrayer(), new FrostNova() };
		for (Skill k : casts){
			k.level = 1;
			SkillCastFX.play( k, Dungeon.hero );
		}
		SkillInteractions.Mark chain = new SkillInteractions.Mark();
		chain.kind = SkillInteractions.Mark.CHAIN;
		chain.cell = at( 1, 1 );
		chain.target = rat;
		mobs.add( new SkillMarkFX( chain ) );
		SkillInteractions.Mark stagger = new SkillInteractions.Mark();
		stagger.kind = SkillInteractions.Mark.STAGGER;
		stagger.target = rat;
		mobs.add( new SkillMarkFX( stagger ) );
		SkillFieldFX.show( new SkillFieldFX( field( SkillField.FRAGMENT, new int[]{ at( 2, 9 ), at( 3, 9 ) } ) ) );
		SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, at( 5, 5 ) );
		SkillSpectacleFX.show( SkillSpectacleFX.JAW, at( 6, 5 ) );
		SkillSpectacleFX.show( SkillSpectacleFX.THORN, at( 7, 5 ) );
		WarriorImpactFX.show( at( 5, 6 ), true );
		WarriorImpactFX.show( at( 10, 6 ) );
		WhirlHitFX.show( at( 6, 7 ) );
		SpiritArmorMotes motes = new SpiritArmorMotes();
		motes.motes = 2;
		mobs.add( new SpiritArmorFX( rat, motes ) );
		SpiritArmorFX.spend( rat, Dungeon.hero );
		MasterThiefCoins coins = new MasterThiefCoins();
		coins.countUp( 3 );
		mobs.add( new MasterThiefCoinsFX( rat, coins ) );
		mobs.add( new StanceOrbit( Dungeon.hero, 0xFFB060 ) );
		SkillFX.landing( at( 7, 7 ) );
		SkillFX.landing( at( 10, 7 ) );
		SkillInteractions.flare( at( 2, 2 ), 0xFFF1A1 );
		ClosingRing.on( hs, 31, 9, 0.45f, 0x88CC66 );
	}
}
