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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * A phone that played its own game and then joins a co-op one: the host's full state takes
 * Dungeon over, and nothing of the game played here before may stay in the turn loop's actors.
 * Left in, they were acted against the host's level whenever a loop ran again under it (the
 * connection dropping turns the game back into a local one, and GameScene starts a loop): that
 * hero's cell on the 176x176 surface read off the end of a town's map in TileTemperature
 * (index 16642 of 2072, index 13637 of 1755).
 */
public class FullStateTakeoverTest {

	private Level savedLevel;
	private Hero savedHero;
	private int savedDepth, savedBranch, savedGold;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedDepth = Dungeon.depth;
		savedBranch = Dungeon.branch;
		savedGold = Dungeon.gold;
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.depth = savedDepth;
		Dungeon.branch = savedBranch;
		Dungeon.gold = savedGold;
	}

	//the host's floor as a full state carries it: a plain room of w x h, and the host's hero in it
	private static JSONObject fullState( int depth, int w, int h, int heroPos ) throws Exception {
		JSONArray map = new JSONArray();
		for (int i = 0; i < w * h; i++){
			int x = i % w, y = i / w;
			map.put( x == 0 || y == 0 || x == w - 1 || y == h - 1 ? Terrain.WALL : Terrain.EMPTY );
		}
		JSONObject hero = new JSONObject()
				.put( "id", 1 )
				.put( "pos", heroPos )
				.put( "cls", HeroClass.CLERIC.ordinal() )
				.put( "hp", 18 )
				.put( "ht", 25 );
		return new JSONObject()
				.put( "depth", depth )
				.put( "branch", 0 )
				.put( "w", w )
				.put( "h", h )
				.put( "map", map )
				.put( "hero", hero );
	}

	@Test
	public void theGamePlayedHereBeforeLeavesNoActorUnderTheHostsLevel() throws Exception {
		//the game this phone played before joining: its hero and a rat out on the surface
		Hero mine = new Hero();
		mine.heroClass = HeroClass.DUELIST;
		mine.pos = 16642;
		Dungeon.hero = mine;
		Actor.add( mine );
		Rat rat = new Rat();
		rat.pos = 17375;
		Actor.add( rat );

		//the host's town comes in over the network: 56 x 37, 2072 cells
		new SpectatorReceiver().onMessage( new Protocol.Message( Protocol.FULL_STATE,
				fullState( 2, 56, 37, 1583 ) ) );

		//the full state was taken in...
		assertEquals( 2072, Dungeon.level.length() );
		assertEquals( 1583, Dungeon.hero.pos );
		assertNotSame( mine, Dungeon.hero );
		//...and no turn loop can act anything of the old game against it
		assertTrue( "the old game's actors: " + Actor.all(), Actor.all().isEmpty() );
		assertTrue( Actor.chars().isEmpty() );
	}
}
