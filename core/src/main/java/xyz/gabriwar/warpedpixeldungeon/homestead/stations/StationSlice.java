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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * A slice's register of its stations, by world cell: what kind each is, the heat it gives its room
 * and whether it anchors a home while its fire burns, and until when on the global clock
 * (FarmCrop.clock) that fire burns on what it holds. The heat (StationHeat), the anchors
 * (StationAnchors) and the workshop (StationRooms, HomesteadStation.speed) read it, on the render
 * thread as on the game's, without walking the level's mobs.
 *
 * Each station writes its own entry as it acts (and as its fire is lit or put out, or it is fed),
 * and takes it away when it comes down. The entries are by world cell, so they hold through every
 * move of the window: a rebase or a jump only changes which of them fall inside it. Nothing is
 * forgotten while the window moves (dematerialize), so the wildlife set down as a new window is
 * peopled - before the sections are told the window has arrived - still keeps away from a lit
 * hearth; and a fire's end is read off the clock, so a hearth whose fuel ran out while it lay
 * parked off the window anchors nothing from that moment, whether or not it has acted since. Once
 * the window has arrived (materialize) the entries inside it are made again from the stations
 * standing there, and any whose station is gone are dropped. Nothing is saved: the stations are,
 * with the slice, and the register is made again from them when it is loaded.
 *
 * Lookups by window cell go through an index of the window's cells (made again on the first look
 * after the window moved), so a room's heat costs an array read a floor cell.
 */
public class StationSlice extends StoreSection {

	private static final class At {
		final Class<? extends HomesteadStation> kind;
		//what it gives while its fire burns, and the clock it burns until
		final float heat;
		final boolean anchor;
		final float until;

		At( Class<? extends HomesteadStation> kind, float heat, boolean anchor, float until ){
			this.kind = kind;
			this.heat = heat;
			this.anchor = anchor;
			this.until = until;
		}

		boolean giving(){
			return FarmCrop.clock() < until;
		}
	}

	//the register's entries for one window's cells (null where none), as of its origin
	private static final class Index {
		final int ox, oy, length;
		final At[] cells;

		Index( int ox, int oy, int length ){
			this.ox = ox;
			this.oy = oy;
			this.length = length;
			this.cells = new At[length];
		}

		boolean of( OverworldLevel level ){
			return ox == level.worldX && oy == level.worldY && length == level.length();
		}
	}

	private final HashMap<Long, At> at = new HashMap<>();
	private volatile Index index;
	//bumped each time a station is set down in the register or taken out of it (not as a fire is
	//lit or goes out): what the workshops are made of
	private volatile int changes;

	/** A slice's register, null on any other level. */
	public static StationSlice of( Level level ){
		HomesteadStore store = Homestead.store( level );
		return store == null ? null : store.section( StationSlice.class );
	}

	private static long key( OverworldLevel level, int cell ){
		int w = level.width();
		return OverworldLevel.worldKey( level.worldX + cell % w, level.worldY + cell / w );
	}

	private static int cellOf( OverworldLevel level, long key ){
		return level.localCell( (int)(key & 0xFFFFFFFFL), (int)(key >> 32) );
	}

	/** A station's entry, as it is now. */
	void put( OverworldLevel level, HomesteadStation station ){
		int cell = station.pos;
		if (cell < 0 || cell >= level.length()) return;
		//read off the station before the register's lock is taken: the station locks itself
		At a = new At( station.getClass(), station.roomHeat(), station.anchorsHome(), station.givesUntil() );
		synchronized (this){
			At old = at.put( key( level, cell ), a );
			if (old == null || old.kind != a.kind) changes++;
			Index i = index;
			if (i != null && i.of( level )) i.cells[cell] = a;
		}
	}

	/** Whatever stood on a cell is gone. */
	synchronized void remove( OverworldLevel level, int cell ){
		if (cell < 0 || cell >= level.length()) return;
		if (at.remove( key( level, cell ) ) != null) changes++;
		Index i = index;
		if (i != null && i.of( level )) i.cells[cell] = null;
	}

	/** Counts the stations set down in the register and taken out of it: unchanged, the window's
	 *  workshops are as they were (HomesteadStation.speed keeps its answer while it is). */
	public int changes(){
		return changes;
	}

	//the entry on a window cell, null for none
	private At lookup( OverworldLevel level, int cell ){
		if (cell < 0 || cell >= level.length()) return null;
		Index i = index;
		if (i == null || !i.of( level )) i = reindex( level );
		return i.cells[cell];
	}

	//the index of the window the level shows now, made from the entries inside it
	private synchronized Index reindex( OverworldLevel level ){
		Index i = index;
		if (i != null && i.of( level )) return i;
		i = new Index( level.worldX, level.worldY, level.length() );
		for (Map.Entry<Long, At> e : at.entrySet()){
			int c = cellOf( level, e.getKey() );
			if (c != -1) i.cells[c] = e.getValue();
		}
		index = i;
		return i;
	}

	/** The heat the station on a window cell gives its room now, C; 0 for none. */
	public float heatAt( OverworldLevel level, int cell ){
		At a = lookup( level, cell );
		return a == null || a.heat == 0f || !a.giving() ? 0f : a.heat;
	}

	/** The kind of station on a window cell, null for none. */
	public Class<? extends HomesteadStation> kindAt( OverworldLevel level, int cell ){
		At a = lookup( level, cell );
		return a == null ? null : a.kind;
	}

	/** Is the room a workshop: a workbench and another station on its floor (StationRooms)? */
	public synchronized boolean workshop( OverworldLevel level, Rooms.Room room ){
		int stations = 0;
		boolean bench = false;
		for (Map.Entry<Long, At> e : at.entrySet()){
			int c = cellOf( level, e.getKey() );
			if (c == -1 || !room.contains( c )) continue;
			stations++;
			if (Workbench.class.isAssignableFrom( e.getValue().kind )) bench = true;
		}
		return bench && stations >= 2;
	}

	/** Hands over the window cells of the stations that anchor a home now (a lit hearth). */
	public synchronized void anchors( OverworldLevel level, IntConsumer cells ){
		for (Map.Entry<Long, At> e : at.entrySet()){
			At a = e.getValue();
			if (!a.anchor || !a.giving()) continue;
			int c = cellOf( level, e.getKey() );
			if (c != -1) cells.accept( c );
		}
	}

	//the window has arrived: the entries inside it made again from the stations standing there
	//(the ones that came back with it, unparked, wrote nothing while they were away), and the ones
	//whose station is no longer there dropped
	@Override
	public void materialize( OverworldLevel level ){
		HashSet<Long> standing = new HashSet<>();
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof HomesteadStation) || m.pos < 0 || m.pos >= level.length()) continue;
			put( level, (HomesteadStation) m );
			standing.add( key( level, m.pos ) );
		}
		synchronized (this){
			for (Iterator<Map.Entry<Long, At>> it = at.entrySet().iterator(); it.hasNext(); ){
				Map.Entry<Long, At> e = it.next();
				if (cellOf( level, e.getKey() ) != -1 && !standing.contains( e.getKey() )){
					it.remove();
					changes++;
				}
			}
			index = null;
		}
	}

	//nothing of its own to save: it is made again from the stations, which are
	@Override
	public void storeInBundle( Bundle bundle ){}

	@Override
	public void restoreFromBundle( Bundle bundle ){}
}
