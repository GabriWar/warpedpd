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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * What a message in a bottle leads to (MessageBottle): a small chest buried out in the wilds of the
 * surface, marked on the world map (BottleMarks) once the note is read, and dug up with a shovel
 * (Gathering.dug hands the spade's work here).
 *
 * Where (place): NEAR to FAR cells from where the note was read, on a bearing and at a distance the
 * world seed and the note's number pick, the first of up to TRIES such spots that is open ground a
 * spade goes into - a meadow's, a plain's or a wood's grass or bare earth, a beach's or a desert's
 * sand - out of the town's land, every settlement, road and place of the world. Judged at the year's
 * mean, so a winter does not move it (frozen ground still takes no spade until the thaw). A pure
 * function: the same note in the same world marks the same spot.
 *
 * The spade finds it on the marked cell or a step off it: gold (scaled as the town scales its prices)
 * and a ring or a wand, sometimes a potion besides, in a chest left on the cell.
 */
public final class BottleCaches {

	private BottleCaches(){}

	/** How far from where the note is read its cache lies, cells. */
	public static final int NEAR = 40, FAR = 120;
	/** Spots tried before there is none in reach (out at sea, deep in a mountain range): the note is
	 *  then kept unread, for somewhere else (MessageBottle). */
	public static final int TRIES = 64;
	//keeps the notes' rolls apart from every other hash of the world
	private static final long SALT = 0xB077_1E5EAL;

	/** The surface's world cell a note's cache is buried at, { wx, wy }, or null for none within reach. */
	public static int[] place( long seed, int fromX, int fromY, int note ){
		WorldModel.Sample s = new WorldModel.Sample();
		for (int i = 0; i < TRIES; i++){
			long h = mix( seed ^ SALT, note * 131L + i );
			double bearing = (h & 0xFFFF) / 65536.0 * Math.PI * 2;
			int dist = NEAR + (int) Math.floorMod( h >>> 16, (long)(FAR - NEAR + 1) );
			int wx = fromX + (int) Math.round( Math.cos( bearing ) * dist );
			int wy = fromY + (int) Math.round( Math.sin( bearing ) * dist );
			if (diggable( seed, wx, wy, s )) return new int[]{ wx, wy };
		}
		return null;
	}

	/** Open ground a spade goes into, out of the town, every settlement, road and place of the world, at the year's mean. */
	static boolean diggable( long seed, int wx, int wy, WorldModel.Sample s ){
		if (WorldModel.townInfluence( seed, wx, wy, WorldModel.TOWN_LAND ) > 0f) return false;
		WorldModel.sample( seed, wx, wy, 0f, s );
		int wild = WorldModel.wildTerrain( seed, wx, wy, s );
		switch (s.biome){
			case MEADOW: case PLAINS: case FOREST:
				if (!Gathering.earth( wild )) return false;
				break;
			case BEACH: case DESERT:
				if (wild != Terrain.EMPTY_SP) return false;
				break;
			default:
				return false;
		}
		return WorldStructures.terrainAt( seed, wx, wy, wild ) == -1 && !OverworldFauna.nearSettlement( seed, wx, wy );
	}

	//a splitmix finaliser
	private static long mix( long seed, long n ){
		long x = seed ^ (n * 0x9E3779B97F4A7C15L);
		x ^= x >>> 31; x *= 0xBF58476D1CE4E5B9L;
		x ^= x >>> 29; x *= 0x94D049BB133111EBL;
		x ^= x >>> 32;
		return x;
	}

	/**
	 * The spade's work done on a cell of the surface (Gathering.dug): a cache marked on it or a step
	 * off it comes up as a chest on the cell, and its mark comes off the map. Host. Whether one did.
	 */
	public static boolean dug( OverworldLevel ow, int cell ){
		if (ow.altitude() != 0 || ow.isMirror()) return false;
		CatchWorld world = CatchWorld.get();
		if (world == null) return false;
		int w = ow.width();
		int[] c = world.cacheNear( ow.worldX() + cell % w, ow.worldY() + cell / w );
		if (c == null || !world.removeCache( c[0], c[1] )) return false;
		int at = RaidEvent.dropCell( ow, cell );
		Heap chest = null;
		for (Item i : treasure()) chest = ow.drop( i, at );
		if (chest != null){
			chest.type = Heap.Type.CHEST;
			if (chest.sprite != null){
				chest.sprite.link();
				chest.sprite.drop();
			}
		}
		GLog.p( Messages.get( BottleCaches.class, "found" ) );
		return true;
	}

	/** A cache's goods: gold, a ring or a wand, and one time in two a potion. */
	static ArrayList<Item> treasure(){
		ArrayList<Item> out = new ArrayList<>();
		out.add( new Gold( TownLedger.scaled( Random.IntRange( 60, 120 ) ) ) );
		Item trinket = Generator.random( Random.Int( 2 ) == 0 ? Generator.Category.RING : Generator.Category.WAND );
		if (trinket != null) out.add( trinket );
		if (Random.Int( 2 ) == 0){
			Item potion = Generator.random( Generator.Category.POTION );
			if (potion != null) out.add( potion );
		}
		return out;
	}
}
