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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The rogue's Stealth double rises on Hero.spotEnemies: when the hero spots a new enemy, never
 *  because an enemy noticed him, and not again while the same enemy stays in view. */
public class EnemySpottedTest {

	@BeforeClass public static void loadAssets(){
		xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest.titleScreen();
	}

	private Hero previousHero;
	private Level previousLevel;
	private Hero hero;
	private Level floor;
	private final List<Mob> spotted = new ArrayList<>();

	@Before public void setUp(){
		previousHero = Dungeon.hero;
		previousLevel = Dungeon.level;
		floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( 9, 9 );
		floor.mobs = new java.util.HashSet<>();
		Dungeon.level = floor;

		hero = new Hero();
		Dungeon.hero = hero;
		hero.heroClass = HeroClass.ROGUE;
		//records what the skill tree is told, in place of Stealth's double (which needs a scene to draw in)
		hero.heroSkills = new CurrentSkills( HeroClass.ROGUE ){
			@Override public void onEnemySpotted( Mob mob ){ spotted.add( mob ); }
		};
		hero.pos = 4 * 9 + 4;
		hero.fieldOfView = new boolean[ floor.length() ];
	}

	@After public void tearDown(){
		Dungeon.hero = previousHero;
		Dungeon.level = previousLevel;
		if (previousLevel != null) PathFinder.setMapSize( previousLevel.width(), previousLevel.height() );
	}

	private Rat rat( int pos ){
		Rat rat = new Rat();
		rat.pos = pos;
		floor.mobs.add( rat );
		return rat;
	}

	private void see( int... cells ){
		Arrays.fill( hero.fieldOfView, false );
		for (int c : cells) hero.fieldOfView[c] = true;
	}

	@Test public void nothingNewInSightSpotsNothing(){
		rat( 1 * 9 + 1 );
		see( hero.pos );
		hero.spotEnemies();
		hero.spotEnemies();
		assertTrue( spotted.isEmpty() );
	}

	@Test public void anEnemyComingIntoSightIsSpottedOnceWhileItStays(){
		Rat rat = rat( 2 * 9 + 2 );
		see( hero.pos );
		hero.spotEnemies();
		assertTrue( spotted.isEmpty() );

		see( hero.pos, rat.pos );
		hero.spotEnemies();
		assertEquals( Arrays.asList( (Mob) rat ), spotted );

		//it walks around in view for a while: no new sighting
		for (int step = 0; step < 5; step++){
			rat.pos = 2 * 9 + 2 + step;
			see( hero.pos, rat.pos );
			hero.spotEnemies();
		}
		assertEquals( 1, spotted.size() );

		//a second enemy joins it: only the newcomer is spotted
		Rat other = rat( 6 * 9 + 6 );
		see( hero.pos, rat.pos, other.pos );
		hero.spotEnemies();
		assertEquals( Arrays.asList( (Mob) rat, other ), spotted );
	}

	//the old trigger: an enemy out of the hero's sight hunting him spots nothing
	@Test public void anEnemyThatNoticesTheHeroUnseenIsNotSpotted(){
		Rat rat = rat( 1 * 9 + 7 );
		rat.state = rat.HUNTING;
		rat.aggro( hero );
		see( hero.pos );
		hero.spotEnemies();
		assertTrue( spotted.isEmpty() );
	}

	@Test public void mindVisionAlliesAndInvisibleEnemiesAreNotSpotted(){
		Rat sensed = rat( 1 * 9 + 1 );
		Rat ally = rat( 1 * 9 + 2 );
		ally.alignment = Char.Alignment.ALLY;
		Rat hidden = rat( 1 * 9 + 3 );
		hidden.invisible = 1;
		see( hero.pos, sensed.pos, ally.pos, hidden.pos );
		hero.mindVisionEnemies.add( sensed );
		hero.spotEnemies();
		assertTrue( spotted.isEmpty() );

		//the one sensed by mind vision walks into plain sight: now it is spotted
		hero.mindVisionEnemies.clear();
		hero.spotEnemies();
		assertEquals( Arrays.asList( (Mob) sensed ), spotted );
	}

	@Test public void onlyTheLocalHeroTellsItsSkills(){
		Hero other = hero;
		Dungeon.hero = new Hero();
		Rat rat = rat( 2 * 9 + 2 );
		see( other.pos, rat.pos );
		other.spotEnemies();
		assertTrue( spotted.isEmpty() );
	}
}
