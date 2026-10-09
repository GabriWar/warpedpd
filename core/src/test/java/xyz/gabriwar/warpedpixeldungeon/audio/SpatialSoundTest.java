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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Spatial sound: off, every effect plays exactly as it always has; on, a sound is panned to the
 * side of the hero it is on and quieter the further off it is, never below the floor, and one
 * at the hero's own cell is untouched. The game's own sounds go through it: a blow is heard
 * where it lands, and on a host a guest's steps from where the guest walks.
 */
public class SpatialSoundTest {

	private static final int W = 19;
	//the listening hero, in the middle of the floor
	private static final int HERE = 9 * W + 9;

	private static final class Played {
		final Object id;
		final float delay, volume, pitch, pan;
		Played( Object id, float delay, float volume, float pitch, float pan ){
			this.id = id; this.delay = delay; this.volume = volume; this.pitch = pitch; this.pan = pan;
		}
	}

	private final ArrayList<Played> played = new ArrayList<>();

	private boolean wasOn, wasDesktop, wasAcoustics;
	private String previousVersion;
	private Level previousLevel;
	private Hero previousHero;
	private Hero hero;

	@BeforeClass
	public static void boot(){
		AllItemsTest.titleScreen();
	}

	@Before
	public void setUp(){
		wasOn = SpatialSound.on;
		wasDesktop = SpatialSound.desktop;
		//spatial sound alone: room acoustics would add a 17 by 17 hall's tails to these
		wasAcoustics = RoomAcoustics.on;
		RoomAcoustics.on = false;
		previousVersion = Game.version;
		Game.version = "test";
		previousLevel = Dungeon.level;
		previousHero = Dungeon.hero;
		SpatialSound.out = ( id, delay, volume, pitch, pan ) -> played.add( new Played( id, delay, volume, pitch, pan ) );

		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( W, W );
		floor.traps = new SparseArray<>();
		floor.plants = new SparseArray<>();
		floor.heaps = new SparseArray<>();
		floor.blobs = new HashMap<>();
		Arrays.fill( floor.solid, true );
		for (int y = 1; y < W - 1; y++) for (int x = 1; x < W - 1; x++) Level.set( y * W + x, Terrain.EMPTY, floor );
		Arrays.fill( floor.heroFOV, true );
		Dungeon.level = floor;

		hero = new Hero();
		hero.pos = HERE;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		SpatialSound.on = wasOn;
		SpatialSound.desktop = wasDesktop;
		RoomAcoustics.on = wasAcoustics;
		SpatialSound.out = SpatialSound.SAMPLE;
		Actor.clear();
		Dungeon.level = previousLevel;
		Dungeon.hero = previousHero;
		Game.version = previousVersion;
		if (previousLevel != null) PathFinder.setMapSize( previousLevel.width(), previousLevel.height() );
	}

	private Played last(){
		assertTrue( "something was played", !played.isEmpty() );
		return played.get( played.size() - 1 );
	}

	private static Char charAt( int cell ){
		Char ch = new Char(){};
		ch.pos = cell;
		return ch;
	}

	@Test
	public void offEveryEffectPlaysAsItAlwaysHas(){
		SpatialSound.on = false;
		assertEquals( 0f, SpatialSound.pan( 7f ), 0f );
		assertEquals( 1f, SpatialSound.level( 7f, 3f ), 0f );

		int far = HERE + 7 + 3 * W;
		SpatialSound.play( Assets.Sounds.HIT, far, 0.8f, 1.2f );
		assertEquals( 0.8f, last().volume, 0f );
		assertEquals( 1.2f, last().pitch, 0f );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 0f, last().delay, 0f );

		SpatialSound.play( Assets.Sounds.HIT, charAt( far ) );
		assertEquals( 1f, last().volume, 0f );
		assertEquals( 0f, last().pan, 0f );

		SpatialSound.playDelayed( Assets.Sounds.ROCKS, 0.25f, far );
		assertEquals( 0.25f, last().delay, 0f );
		assertEquals( 1f, last().volume, 0f );
		assertEquals( 0f, last().pan, 0f );

		//the thunder's own pan, from pan(): centred too
		SpatialSound.playPanned( Assets.Sounds.LIGHTNING, 1f, 0.6f, 0.9f, SpatialSound.pan( -6f ) );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 0.6f, last().volume, 0f );
	}

	@Test
	public void theHerosOwnCellIsUntouched(){
		SpatialSound.on = true;
		SpatialSound.play( Assets.Sounds.STEP, HERE, 0.7f, 1.05f );
		assertEquals( 0.7f, last().volume, 0f );
		assertEquals( 0f, last().pan, 0f );
		SpatialSound.play( Assets.Sounds.STEP, hero );
		assertEquals( 1f, last().volume, 0f );
		assertEquals( 0f, last().pan, 0f );
	}

	@Test
	public void aSoundPansToItsSideAndFallsOffWithDistance(){
		SpatialSound.on = true;

		SpatialSound.play( Assets.Sounds.HIT, HERE + 3, 0.5f );
		assertEquals( 0.6f, last().pan, 1e-6f );
		assertEquals( 0.5f * SpatialSound.level( 3f, 0f ), last().volume, 1e-6f );

		SpatialSound.play( Assets.Sounds.HIT, HERE - 3, 0.5f );
		assertEquals( "the same, to the left", -0.6f, last().pan, 1e-6f );
		assertEquals( 0.5f * SpatialSound.level( 3f, 0f ), last().volume, 1e-6f );

		//straight below: no side, the fall-off of its distance alone
		SpatialSound.play( Assets.Sounds.HIT, HERE + 6 * W );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( SpatialSound.FLOOR + (1f - SpatialSound.FLOOR) / 2f, last().volume, 1e-6f );

		//far to the right and below: as far to the side as it goes
		SpatialSound.playDelayed( Assets.Sounds.BLAST, 0.5f, charAt( HERE + 8 + 4 * W ), 1f, 0.9f );
		assertEquals( SpatialSound.MAX_PAN, last().pan, 1e-6f );
		assertEquals( SpatialSound.level( 8f, 4f ), last().volume, 1e-6f );
		assertEquals( 0.5f, last().delay, 0f );
		assertEquals( 0.9f, last().pitch, 0f );
	}

	@Test
	public void thePanIsBoundedEvenAndGrowsToTheSide(){
		SpatialSound.on = true;
		assertEquals( 0f, SpatialSound.pan( 0f ), 0f );
		assertEquals( 0.2f, SpatialSound.pan( 1f ), 1e-6f );
		assertEquals( SpatialSound.MAX_PAN, SpatialSound.pan( SpatialSound.PAN_CELLS ), 1e-6f );
		assertEquals( SpatialSound.MAX_PAN, SpatialSound.pan( 100f ), 1e-6f );
		float last = -1f;
		for (float dx = -30f; dx <= 30f; dx += 0.5f){
			float p = SpatialSound.pan( dx );
			assertTrue( Math.abs( p ) <= SpatialSound.MAX_PAN );
			assertTrue( "further right, further right", p >= last );
			assertEquals( "the same either side", -p, SpatialSound.pan( -dx ), 1e-6f );
			assertTrue( "to its own side", dx == 0f || Math.signum( p ) == Math.signum( dx ) );
			last = p;
		}
	}

	@Test
	public void distanceTakesNoMoreThanItsShare(){
		SpatialSound.on = true;
		assertEquals( "all of it at the hero", 1f, SpatialSound.level( 0f, 0f ), 0f );
		assertEquals( 0.98f, SpatialSound.level( 1f, 0f ), 0.005f );
		assertEquals( 0.93f, SpatialSound.level( 2f, 0f ), 0.005f );
		assertEquals( 0.785f, SpatialSound.level( 4f, 0f ), 0.005f );
		assertEquals( "half of what it can lose at HALF_CELLS", 0.65f,
				SpatialSound.level( SpatialSound.HALF_CELLS, 0f ), 1e-6f );
		assertEquals( 0.55f, SpatialSound.level( 8f, 0f ), 0.005f );
		assertEquals( 0.44f, SpatialSound.level( 12f, 0f ), 0.005f );
		assertEquals( 0.36f, SpatialSound.level( 20f, 0f ), 0.005f );
		assertEquals( "the same any way round", SpatialSound.level( 3f, 4f ), SpatialSound.level( 0f, -5f ), 1e-6f );
		float last = 1f;
		for (float d = 0f; d <= 200f; d += 0.5f){
			float l = SpatialSound.level( d, 0f );
			assertTrue( "never louder further off: " + d, l <= last );
			assertTrue( "never under the floor: " + d, l >= SpatialSound.FLOOR );
			last = l;
		}
		assertEquals( SpatialSound.FLOOR, SpatialSound.level( 1000f, 1000f ), 0.001f );
	}

	@Test
	public void aSoundWithNowhereToBeIsCentred(){
		SpatialSound.on = true;
		SpatialSound.play( Assets.Sounds.HIT, -1, 0.9f );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 0.9f, last().volume, 0f );
		SpatialSound.play( Assets.Sounds.HIT, W * W + 3 );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 1f, last().volume, 0f );
		SpatialSound.play( Assets.Sounds.HIT, (Char) null );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 1f, last().volume, 0f );

		Dungeon.hero = null;
		SpatialSound.play( Assets.Sounds.HIT, HERE + 4 );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 1f, last().volume, 0f );
		Dungeon.hero = hero;
		Dungeon.level = null;
		SpatialSound.play( Assets.Sounds.HIT, HERE + 4 );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 1f, last().volume, 0f );
	}

	@Test
	public void theLeftAndRightGiveBackTheVolumeAndThePan(){
		//what Sample.play makes of a left and a right volume: the louder of the two, panned by
		//right - left
		for (float v : new float[]{ 0.05f, 0.3f, 1f, 2f }){
			for (float p = -SpatialSound.MAX_PAN; p <= SpatialSound.MAX_PAN + 1e-4f; p += 0.1f){
				float l = SpatialSound.left( v, p ), r = SpatialSound.right( v, p );
				assertEquals( v, Math.max( l, r ), 1e-6f );
				assertEquals( p, r - l, 1e-6f );
			}
			assertEquals( "centred: both are the volume", v, SpatialSound.left( v, 0f ), 0f );
			assertEquals( v, SpatialSound.right( v, 0f ), 0f );
		}
	}

	//libGDX's desktop audio as measured with the OpenAL Soft it ships (1.23.1, stereo out, no
	//HRTF): a mono source at pan p is set p * 90 degrees round from right behind the listener
	//and comes out of the near and the far ear at these levels (0.404 in each, centred)
	private static double desktopEar( float pan, boolean near ){
		double a = Math.abs( pan ) * Math.PI / 2;
		return Math.abs( 0.5 + (near ? 0.5 : -0.5) * Math.sin( a ) - 0.0957 * Math.cos( a ) );
	}

	@Test
	public void onDesktopAPlacedSoundIsNoLouderThanCentredAndNeverInOneEarOnly(){
		SpatialSound.on = true;
		SpatialSound.desktop = true;
		double centre = 2 * desktopEar( 0f, true ) * desktopEar( 0f, true );
		double lastSplit = 0;
		for (float pan = 0.05f; pan <= 1f + 1e-4f; pan += 0.05f){
			float v = SpatialSound.deviceVolume( 1f, pan ), p = SpatialSound.devicePan( pan );
			double near = v * desktopEar( p, true ), far = v * desktopEar( p, false );
			double total = 10 * Math.log10( (near * near + far * far) / centre );
			double split = 20 * Math.log10( far / near );
			assertEquals( "as loud in all as centred, at pan " + pan, 0.0, total, 0.5 );
			assertTrue( "never into one ear only, at pan " + pan + ": " + split, split > -21 );
			assertTrue( "further to the side, further to one side, at pan " + pan, split < lastSplit );
			lastSplit = split;
		}
		//the same either side
		assertEquals( -SpatialSound.devicePan( 0.6f ), SpatialSound.devicePan( -0.6f ), 0f );
		assertEquals( SpatialSound.deviceVolume( 0.7f, 0.6f ), SpatialSound.deviceVolume( 0.7f, -0.6f ), 0f );
		//the widest an effect goes is about as wide as Android makes it (its far side at 1 - pan)
		float p = SpatialSound.devicePan( SpatialSound.MAX_PAN );
		assertEquals( 20 * Math.log10( 1 - SpatialSound.MAX_PAN ),
				20 * Math.log10( desktopEar( p, false ) / desktopEar( p, true ) ), 1.5 );
	}

	@Test
	public void offOrOffDesktopTheDeviceGetsTheSoundAsItIs(){
		//off, the ambience's own pans reach the device as they always have
		SpatialSound.desktop = true;
		SpatialSound.on = false;
		assertEquals( 0.75f, SpatialSound.devicePan( 0.75f ), 0f );
		assertEquals( 0.6f, SpatialSound.deviceVolume( 0.6f, 0.75f ), 0f );
		//Android turns the far side down itself
		SpatialSound.on = true;
		SpatialSound.desktop = false;
		assertEquals( 0.75f, SpatialSound.devicePan( 0.75f ), 0f );
		assertEquals( 0.6f, SpatialSound.deviceVolume( 0.6f, 0.75f ), 0f );
		//and a centred sound is untouched everywhere
		SpatialSound.desktop = true;
		assertEquals( 0f, SpatialSound.devicePan( 0f ), 0f );
		assertEquals( 0.6f, SpatialSound.deviceVolume( 0.6f, 0f ), 0f );
	}

	@Test
	public void anAreaIsHeardFromWhereItIsNearestTheHero(){
		int left = HERE - 1, farRight = HERE + 3 + W;
		assertEquals( left, SpatialSound.nearer( left, farRight ) );
		assertEquals( left, SpatialSound.nearer( farRight, left ) );
		assertEquals( "the hero's own cell, when it is one of them", HERE, SpatialSound.nearer( HERE + 4, HERE ) );
		assertEquals( "none yet: the one there is", farRight, SpatialSound.nearer( -1, farRight ) );
		assertEquals( farRight, SpatialSound.nearer( farRight, -1 ) );
		assertEquals( -1, SpatialSound.nearer( -1, -1 ) );
	}

	@Test
	public void aThrowIsHeardFromTheThrower(){
		SpatialSound.on = true;
		new Item().throwSound( charAt( HERE - 3 ) );
		assertEquals( Assets.Sounds.MISS, last().id );
		assertEquals( -0.6f, last().pan, 1e-6f );
		assertEquals( 0.6f * SpatialSound.level( 3f, 0f ), last().volume, 1e-6f );
		assertEquals( 1.5f, last().pitch, 0f );

		//the hero's own: as it always was
		new Item().throwSound( hero );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 0.6f, last().volume, 0f );
		assertEquals( 1.5f, last().pitch, 0f );
	}

	@Test
	public void aBlowIsHeardWhereItLands(){
		SpatialSound.on = true;
		Char attacker = charAt( HERE + 1 );
		Char defender = charAt( HERE - 4 );
		attacker.hitSound( 1f, defender );
		assertEquals( Assets.Sounds.HIT, last().id );
		assertEquals( SpatialSound.pan( -4f ), last().pan, 1e-6f );
		assertEquals( SpatialSound.level( 4f, 0f ), last().volume, 1e-6f );

		//the hero's own, fists or blade: where it lands, not where the hero stands
		hero.hitSound( 1f, charAt( HERE + 3 ) );
		assertEquals( 0.6f, last().pan, 1e-6f );
		assertEquals( SpatialSound.level( 3f, 0f ), last().volume, 1e-6f );
	}

	@Test
	public void outOfSightASoundDiesAwayToNothingAtTenCells(){
		assertEquals( SpatialSound.UNSEEN_LOUD, SpatialSound.unseen( 0f ), 1e-6f );
		assertEquals( SpatialSound.UNSEEN_LOUD, SpatialSound.unseen( 2f ), 1e-6f );
		float last = SpatialSound.UNSEEN_LOUD;
		for (float d = 2f; d <= 11f; d += 0.25f){
			float u = SpatialSound.unseen( d );
			assertTrue( "never louder further off", u <= last );
			assertTrue( "quieter a little at every step, never at once: " + d, last - u <= 0.08f );
			last = u;
		}
		assertEquals( 0f, SpatialSound.unseen( SpatialSound.UNSEEN_CELLS ), 1e-6f );

		//spatial sound on or off, and no floor: what is not seen is not heard from across the map
		SpatialSound.on = false;
		SpatialSound.playUnseen( Assets.Sounds.HIT, HERE + 4, 1f, 1f );
		assertEquals( SpatialSound.unseen( 4f ), last().volume, 1e-6f );
		assertEquals( "centred while off", 0f, last().pan, 1e-6f );
		SpatialSound.on = true;
		SpatialSound.playUnseen( Assets.Sounds.HIT, HERE + 4, 1f, 1f );
		assertEquals( SpatialSound.unseen( 4f ), last().volume, 1e-6f );
		assertEquals( SpatialSound.pan( 4f ), last().pan, 1e-6f );
		played.clear();
		SpatialSound.playUnseen( Assets.Sounds.HIT, HERE - 8 - 8 * W, 1f, 1f );
		assertTrue( "11 cells off: not heard", played.isEmpty() );
	}

	//a wall down the floor 2 cells right of the hero, a door in it or none
	private void wallAcross( boolean door ){
		for (int y = 1; y < W - 1; y++) Level.set( y * W + 11, Terrain.WALL, Dungeon.level );
		if (door) Level.set( 9 * W + 11, Terrain.DOOR, Dungeon.level );
	}

	@Test
	public void outOfSightThroughTheRockASoundIsQuieter(){
		wallAcross( false );
		SpatialSound.playUnseen( Assets.Sounds.HIT, HERE + 4, 1f, 1f );
		assertEquals( SpatialSound.unseen( 4f ) * 0.6f, last().volume, 1e-6f );
	}

	@Test
	public void outOfSightThroughADoorASoundIsALittleQuieter(){
		wallAcross( true );
		SpatialSound.playUnseen( Assets.Sounds.OPEN, HERE + 4, 1f, 1f );
		assertEquals( SpatialSound.unseen( 4f ) * 0.8f, last().volume, 1e-6f );
	}

	@Test
	public void aFightOutOfSightIsHeardInEarshotAndNoFurther(){
		SpatialSound.on = true;
		com.watabou.utils.Random.pushGenerator( 31 );
		try {
			Char a = charAt( HERE + 3 ), d = charAt( HERE + 4 );
			a.HP = a.HT = d.HP = d.HT = 50;
			Arrays.fill( Dungeon.level.heroFOV, false );
			Dungeon.level.heroFOV[HERE] = true;
			played.clear();
			a.attack( d );
			assertEquals( "one sound for the blow", 1, played.size() );
			Object id = last().id;
			assertTrue( "a blow's: " + id, id == Assets.Sounds.HIT || id == Assets.Sounds.HIT_PARRY || id == Assets.Sounds.MISS );
			assertEquals( "from where it lands, as far off as it is", SpatialSound.unseen( 4f ), last().volume, 1e-6f );
			assertEquals( SpatialSound.pan( 4f ), last().pan, 1e-6f );

			//beyond earshot, as before: nothing
			Char far1 = charAt( HERE - 8 - 8 * W ), far2 = charAt( HERE - 7 - 8 * W );
			far1.HP = far1.HT = far2.HP = far2.HT = 50;
			played.clear();
			far1.attack( far2 );
			assertTrue( played.isEmpty() );
		} finally {
			com.watabou.utils.Random.popGenerator();
		}
	}

	@Test
	public void aGuestsStepsOnTheHostAreHeardFromWhereHeWalks(){
		SpatialSound.on = true;
		Hero guest = new Hero();
		guest.pos = HERE + 3;
		guest.sprite = new CharSprite(){
			@Override public void place( int cell ){}
		};
		guest.move( HERE + 4, true );
		assertEquals( Assets.Sounds.STEP, last().id );
		assertEquals( SpatialSound.pan( 4f ), last().pan, 1e-6f );
		assertEquals( SpatialSound.level( 4f, 0f ), last().volume, 1e-6f );

		//the host's own steps, under him: as they always were
		hero.move( HERE - 1, true );
		assertEquals( Assets.Sounds.STEP, last().id );
		assertEquals( 0f, last().pan, 0f );
		assertEquals( 1f, last().volume, 0f );
	}
}
