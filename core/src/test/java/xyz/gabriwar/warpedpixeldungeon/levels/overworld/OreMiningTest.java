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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Gem;
import xyz.gabriwar.warpedpixeldungeon.items.ore.GoldOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Mining the world's ore on a real slice window: the pick gets the vein's metal only out of a
 * slice's standing rock and a gem only out of a cave crystal; the lump goes into the pack (or at
 * the miner's feet) without spending a moment of its own, and the log names the first lump and
 * every tenth; a mined vein loses its glint and the rock behind it shows its own; a glinting face
 * is called by its metal.
 */
public class OreMiningTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		//picking up reaches the badges
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		hero = new Hero();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	//a window of the slice at (ox, oy), as a mirror's made the host's own: Dungeon.level
	private static OverworldLevel window( int altitude, int ox, int oy ){
		PathFinder.setMapSize( W, H );
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, 0f, GameCalendar.Season.SUMMER, w.terrain, W, H );
		assertNotNull( ow );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		DungeonTileSheet.setupVariance( ow.length(), 7L );
		return ow;
	}

	private static OverworldDress.Layer layer( OverworldLevel ow, String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return (OverworldDress.Layer) f.get( ow );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Ores.Kind oreAt( OverworldLevel ow, int c ){
		return Ores.oreAt( SEED, ow.altitude(), ow.worldX() + c % W, ow.worldY() + c / W );
	}

	private static ArrayList<String> logged( Runnable run ){
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			run.run();
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	@Test
	public void oreFromAVein(){
		OverworldLevel ow = window( -6, 200, -300 );
		int vein = -1, plain = -1, floor = -1;
		for (int c = W + 1; c < W * (H - 1) && (vein == -1 || plain == -1 || floor == -1); c++){
			if (ow.map[c] == Terrain.WALL){
				if (oreAt( ow, c ) != null){ if (vein == -1) vein = c; }
				else if (plain == -1) plain = c;
			} else if (ow.map[c] == Terrain.EMPTY && floor == -1) floor = c;
		}
		assertTrue( vein != -1 && plain != -1 && floor != -1 );
		Ore ore = ow.oreFrom( vein );
		assertNotNull( ore );
		assertSame( oreAt( ow, vein ).item, ore.getClass() );
		assertEquals( Ores.yield( SEED, -6, ow.worldX() + vein % W, ow.worldY() + vein / W ), ore.quantity() );
		assertNull( ow.oreFrom( plain ) );
		assertNull( ow.oreFrom( floor ) );
		//no crystal, boulder or anything else gives ore: only standing rock
		ow.map[vein] = Terrain.MINE_CRYSTAL;
		assertNull( ow.oreFrom( vein ) );
		ow.map[vein] = Terrain.WALL_DECO;
		assertNull( ow.oreFrom( vein ) );

		//the surface's rock holds none
		OverworldLevel surface = window( 0, 200, -300 );
		for (int c = W + 1; c < W * (H - 1); c++){
			if (surface.map[c] == Terrain.WALL) assertNull( surface.oreFrom( c ) );
		}
	}

	@Test
	public void gemFromACrystal(){
		OverworldLevel ow = window( -9, 200, -300 );
		int gem = -1, bare = -1;
		for (int c = W + 1; c < W * (H - 1) && (gem == -1 || bare == -1); c++){
			if (ow.map[c] != Terrain.MINE_CRYSTAL) continue;
			if (Ores.gemAt( SEED, -9, ow.worldX() + c % W, ow.worldY() + c / W ) != null){ if (gem == -1) gem = c; }
			else if (bare == -1) bare = c;
		}
		assertTrue( gem != -1 && bare != -1 );
		Gem g = ow.gemFrom( gem );
		assertNotNull( g );
		assertSame( Ores.gemAt( SEED, -9, ow.worldX() + gem % W, ow.worldY() + gem / W ).item, g.getClass() );
		assertNull( ow.gemFrom( bare ) );
		//a crystal's gem is the caves': the same cell as plain rock holds none
		ow.map[gem] = Terrain.WALL;
		assertNull( ow.gemFrom( gem ) );
	}

	//into the pack, told the first time and every tenth, never spending a moment; at the feet when full
	@Test
	public void minedOreGoesToThePackAndIsTold(){
		OverworldLevel ow = window( -6, 200, -300 );
		int open = -1;
		for (int c = W + 1; c < W * (H - 1) && open == -1; c++) if (ow.passable[c]) open = c;
		final int floor = open;
		hero.pos = floor;
		float before = hero.cooldown();
		ArrayList<String> first = logged( () -> new IronOre().mined( hero, floor ) );
		assertEquals( 1, first.size() );
		assertEquals( Messages.get( Ore.class, "prised", new IronOre().name() ), first.get( 0 ) );
		for (int i = 2; i <= 9; i++){
			assertTrue( "lump " + i, logged( () -> new IronOre().mined( hero, floor ) ).isEmpty() );
		}
		ArrayList<String> tenth = logged( () -> new IronOre().mined( hero, floor ) );
		assertEquals( 1, tenth.size() );
		assertEquals( Messages.get( Ore.class, "you_now_have", 10, new IronOre().name() ), tenth.get( 0 ) );
		assertEquals( 10, hero.belongings.getItem( IronOre.class ).quantity() );
		assertEquals( "mining's lumps take no time of their own", before, hero.cooldown(), 0f );
		//two at once from a rich vein
		ArrayList<String> two = logged( () -> ((Ore) new GoldOre().quantity( 2 )).mined( hero, floor ) );
		assertEquals( Messages.get( Ore.class, "prised_two", new GoldOre().name() ), two.get( 0 ) );
		assertEquals( 2, hero.belongings.getItem( GoldOre.class ).quantity() );

		//a full pack: the lump of a metal not carried yet lies at the miner's feet
		while (new Pickaxe().collect( hero.belongings.backpack )){
			assertTrue( hero.belongings.backpack.items.size() < 200 );
		}
		Ore lump = new xyz.gabriwar.warpedpixeldungeon.items.ore.Skyiron();
		assertFalse( lump.mined( hero, floor ) );
		Heap heap = ow.heaps.get( floor );
		assertNotNull( heap );
		assertTrue( heap.items.contains( lump ) );
		assertEquals( before, hero.cooldown(), 0f );
	}

	//a mined vein face loses its glint; the vein cell behind it, now a face, shows its own
	@Test
	public void redressFollowsTheEdit(){
		OverworldLevel ow = window( -6, 200, -300 );
		OverworldDress.Layer ground = layer( ow, "dressGround" );
		int face = -1;
		for (int c = 2 * W + 1; c < W * (H - 1) && face == -1; c++){
			if (Ores.faceKind( ground.get( c ) ) != null && ow.map[c - W] == Terrain.WALL && oreAt( ow, c - W ) != null
					&& ground.get( c - W ) == -1) face = c;
		}
		assertTrue( "no vein face with the vein running on behind it", face != -1 );
		Level.set( face, Terrain.EMPTY_DECO, ow );
		ow.redressRock( face );
		assertEquals( -1, ground.get( face ) );
		Ores.Kind behind = Ores.faceKind( ground.get( face - W ) );
		assertEquals( oreAt( ow, face - W ), behind );

		//nothing but a glint is ever taken off: the deep water's shades and the ways keep theirs
		int other = -1;
		for (int c = W + 1; c < W * (H - 1) && other == -1; c++){
			if (ground.get( c ) != -1 && Ores.faceKind( ground.get( c ) ) == null) other = c;
		}
		assertTrue( other != -1 );
		int was = ground.get( other );
		ow.redressRock( other );
		ow.redressRock( other + W );
		assertEquals( was, ground.get( other ) );
	}

	@Test
	public void veinsHaveNames(){
		OverworldLevel ow = window( -6, 200, -300 );
		OverworldDress.Layer ground = layer( ow, "dressGround" );
		int named = 0;
		for (int c = W + 1; c < W * (H - 1); c++){
			Ores.Kind k = Ores.faceKind( ground.get( c ) );
			if (k == null) continue;
			String name = Messages.get( k.item, "name" );
			assertEquals( Messages.capitalize( name ) + " vein", ow.tileNameAt( c ) );
			String desc = ow.tileDescAt( c );
			assertTrue( desc.startsWith( Messages.capitalize( name ) ) );
			assertFalse( desc.contains( Messages.NO_TEXT_FOUND ) );
			named++;
		}
		assertTrue( named > 10 );
		//plain rock is plain rock
		for (int c = W + 1; c < W * (H - 1); c++){
			if (ow.map[c] == Terrain.WALL && ground.get( c ) == -1) assertNull( ow.tileNameAt( c ) );
		}
	}
}
