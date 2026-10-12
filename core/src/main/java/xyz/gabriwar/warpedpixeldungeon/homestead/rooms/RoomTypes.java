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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WellFed;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * What a room IS, from what stands in it (homestead/Rooms finds the room; RoomKinds names it in a
 * look at it, through RoomTraits). The types this part tells (OWN):
 *   - BEDROOM: a bed. Sleep there is safe while the room is sealed (HomeBed, Bedroll.safe).
 *   - KITCHEN: a hearth or a cooking pot. A meal that leaves the hero well fed (Food.grantsWellFed)
 *     keeps him so KITCHEN_WELL_FED times as long when eaten there, or cooked at a pot in one
 *     (kitchenMeal, cookedAt).
 *   - STOREHOUSE: two chests or more. Each chest in it holds STOREHOUSE_SLOTS more (chestBonus:
 *     StorageChest asks it).
 *   - SHRINE: a statue or an altar. Waking from a night's sleep in one blesses the hero
 *     (ShrineBlessing).
 * And the two the other parts tell by their own room traits (RoomTraits.PROVIDERS), which has()
 * passes on to them: WORKSHOP (stations.StationRooms: a workbench and a second station, where the
 * stations work faster) and GREENHOUSE (farming.FarmRooms: glass over the room, where crops grow at
 * summer's pace).
 * A room may be several at once; the first in OWN's order names it, before the other parts' traits.
 *
 * The other parts' fixtures are told apart without their help (fixtureOf): a RoomFixture says what
 * it is; otherwise a bed is any Bedroll, a chest any StorageChest, and a stove is known by its
 * build piece's id or its class's simple name ("hearth", "cooking_pot").
 *
 * Nothing is kept: every answer is read from the room as it stands, on the host or a co-op mirror.
 */
public final class RoomTypes {

	private RoomTypes(){}

	public static final String BEDROOM = "bedroom", KITCHEN = "kitchen", WORKSHOP = "workshop",
			STOREHOUSE = "storehouse", GREENHOUSE = "greenhouse", SHRINE = "shrine";

	/** The types this part tells, in the order they name a room. WORKSHOP and GREENHOUSE are the
	 *  labels of the stations' and the farming's own traits (has asks RoomTraits for them). */
	public static final String[] OWN = { BEDROOM, KITCHEN, STOREHOUSE, SHRINE };

	/** How much longer a kitchen meal keeps the hero well fed. */
	public static final float KITCHEN_WELL_FED = 1.5f;
	/** The slots each chest in a storehouse holds beyond its own. */
	public static final int STOREHOUSE_SLOTS = 10;
	/** The chests that make a storehouse. */
	public static final int STOREHOUSE_CHESTS = 2;

	//stoves told by their build piece's id or their class's simple name, lower case
	private static final HashMap<String, RoomFixture.Kind> NAMED = new HashMap<>();
	static {
		NAMED.put( "hearth", RoomFixture.Kind.HEARTH );
		NAMED.put( "cooking_pot", RoomFixture.Kind.COOKING_POT );
		NAMED.put( "cookingpot", RoomFixture.Kind.COOKING_POT );
	}

	/** What a mob is to the room it stands in, or null for nothing of a room's. */
	public static RoomFixture.Kind fixtureOf( Level level, Mob m ){
		if (m instanceof RoomFixture) return ((RoomFixture) m).roomFixture();
		if (m instanceof Bedroll) return RoomFixture.Kind.BED;
		if (m instanceof StorageChest) return RoomFixture.Kind.CHEST;
		BuildPiece p = Homestead.pieceAt( level, m.pos );
		RoomFixture.Kind named = p != null ? NAMED.get( p.id ) : null;
		if (named == null) named = NAMED.get( m.getClass().getSimpleName().toLowerCase( Locale.ENGLISH ) );
		return named;
	}

	/** What one room holds, counted in one pass over its mobs and cells. */
	static final class Contents {
		int beds, hearths, pots, chests, altars, statues, decor;

		static Contents of( OverworldLevel level, Rooms.Room room ){
			Contents c = new Contents();
			for (Mob m : level.mobs.toArray( new Mob[0] )){
				if (!room.contains( m.pos )) continue;
				RoomFixture.Kind k = fixtureOf( level, m );
				if (k == null) continue;
				switch (k){
					case BED:         c.beds++; break;
					case HEARTH:      c.hearths++; break;
					case COOKING_POT: c.pots++; break;
					case CHEST:       c.chests++; break;
					case ALTAR:       c.altars++; break;
					case DECOR:       c.decor++; break;
				}
			}
			for (int cell : room.cells){
				int t = level.map[cell];
				if (t == Terrain.STATUE || t == Terrain.STATUE_SP){
					c.statues++;
					c.decor++;
				} else if (t == Terrain.WOOL_RUG || t == Terrain.BOOKSHELF || t == Terrain.EMPTY_BOOKSHELF){
					c.decor++;
				}
			}
			return c;
		}

		boolean is( String type ){
			switch (type){
				case BEDROOM:    return beds > 0;
				case KITCHEN:    return hearths + pots > 0;
				case STOREHOUSE: return chests >= STOREHOUSE_CHESTS;
				case SHRINE:     return statues + altars > 0;
				default:         return false;
			}
		}
	}

	// ----------------------------------------------------------------- asking

	/** The types of OWN a room is, in that order (empty for a plain room). */
	public static List<String> of( OverworldLevel level, Rooms.Room room ){
		return of( Contents.of( level, room ) );
	}

	static List<String> of( Contents c ){
		ArrayList<String> out = new ArrayList<>();
		for (String type : OWN) if (c.is( type )) out.add( type );
		return Collections.unmodifiableList( out );
	}

	/** Is the room of this type: one of OWN, or another part's trait (WORKSHOP, GREENHOUSE: asked of
	 *  RoomTraits, so never from inside a RoomTraits provider)? */
	public static boolean has( OverworldLevel level, Rooms.Room room, String type ){
		for (String own : OWN) if (own.equals( type )) return Contents.of( level, room ).is( type );
		return RoomTraits.is( level, room, type );
	}

	/** Is the cell in a room of this type? False off the world's slices and outside rooms. */
	public static boolean at( Level level, int cell, String type ){
		Rooms.Room room = Rooms.at( level, cell );
		return room != null && has( (OverworldLevel) level, room, type );
	}

	/** The extra slots a chest standing on this cell holds: STOREHOUSE_SLOTS in a storehouse. */
	public static int chestBonus( Level level, int cell ){
		return at( level, cell, STOREHOUSE ) ? STOREHOUSE_SLOTS : 0;
	}

	/** A cooking pot on `cell` has just made this food: a meal that leaves the eater well fed
	 *  (Food.grantsWellFed), cooked in a kitchen, remembers it. The stations' pot calls it on what
	 *  it makes. */
	public static void cookedAt( Food food, Level level, int cell ){
		if (food.grantsWellFed() && at( level, cell, KITCHEN )) food.kitchenMade = true;
	}

	/**
	 * Food.meal, after the food's satisfy (`fedBefore`: the turns of WellFed the hero had before it,
	 * -1 for none): a meal that makes him well fed (Food.grantsWellFed: it sets his WellFed afresh)
	 * keeps him so KITCHEN_WELL_FED times as long when he ate it in a kitchen or it was cooked in
	 * one - and never leaves him less well fed than he was before it.
	 */
	public static void kitchenMeal( Hero hero, Food food, int fedBefore ){
		if (!food.grantsWellFed()) return;
		WellFed fed = hero.buff( WellFed.class );
		if (fed == null) return;
		if (!food.kitchenMade && !at( Dungeon.level, hero.pos, KITCHEN )) return;
		int fresh = fed.turnsLeft();
		int want = Math.max( fedBefore, Math.round( fresh * KITCHEN_WELL_FED ) );
		if (want > fresh) fed.extend( want - fresh );
	}
}
