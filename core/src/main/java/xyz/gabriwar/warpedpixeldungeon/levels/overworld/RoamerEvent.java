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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ElderYeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GiantScorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Roaming;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.RoamerTracks;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.RoamerTrophy;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A great beast with a name of its own, roaming a region for days (WorldEvents.Type.ROAMER): a
 * great wolf on most of the land, an elder yeti in the cold, a giant scorpion in the desert, each
 * four times as hardy as its kin and harder hitting (GreatWolf, ElderYeti, GiantScorpio).
 *
 * WHEN AND WHERE. The world is dealt in regions of REGION x REGION (5x5) sectors and periods of
 * PERIOD (24) days. One period in ODDS (2) a region has its beast, abroad DAYS (8) days from a
 * hashed day of the period - never into the next. Its lair is the first of TRIES hashed spots in
 * the region on open ground one of the kinds keeps to, away from the town and the settlements'
 * streets: its kind is the ground's (Kind.of). Each day after the first it moves on to a new haunt
 * up to STEP cells off, on its kind's ground and inside the region, setting off at LEAVE of the
 * day and there by ARRIVE: where it is at any turn is a pure function of the world and the turn
 * (where), so it roams whether anyone watches or not. Its name is one of its kind's NAMES. A
 * hero meets one every few weeks (RoamerEventTest measures it).
 *
 * HEARING OF IT. The folk of any settlement within RUMOUR (3) sectors of it talk of nothing else:
 * where it was last seen, the way from their village (rumour; Settler.interact), and so does the
 * party once a hero sees the beast or its fresh tracks - the prints it left on open ground over the
 * last TRACKS turns (prints, effects/RoamerTracks), fading as they age. Heard of, it is pinned on the
 * world map on the sector it haunts now (onMap). The villages' notice boards can ask where one is
 * (abroad).
 *
 * ON THE GROUND. Its place within SPAWN cells of a hero in the window brings it out, out of sight
 * where it can be; it keeps to its haunt out of a fight (haunt), hunts a hero it notices like any
 * of its kind, and once far from every hero, or strayed far from its haunt where nobody sees, it is
 * gone until a hero comes near its haunt again - keeping its wounds (the log's), as one that leaves
 * the window does (leaving). Killed, it is settled for good and leaves its trophy (RoamerTrophy), a
 * purse of GOLD coins and a good weapon, armour, wand or ring (slain), and the party hears of it.
 * Its days over, it is gone once nobody sees it.
 */
public final class RoamerEvent {

	private RoamerEvent(){}

	/** Sectors a side of a roaming beast's region. */
	public static final int REGION = 5;
	/** Days to a period; one period in ODDS a region has its beast, abroad DAYS days. */
	public static final int PERIOD = 24, ODDS = 2, DAYS = 8;
	//the share of each day it sets off to its next haunt at, and is there by
	static final float LEAVE = 0.3f, ARRIVE = 0.8f;
	//cells: the furthest a day's walk takes it, and how far into the region its haunts keep
	static final int STEP = 110, INSIDE = 12;
	//hashed spots tried for a lair, and for each day's haunt
	static final int TRIES = 8;
	/** Sectors: the folk of a settlement this near a roaming beast talk of it. */
	public static final int RUMOUR = 3;
	//names to a kind
	static final int NAMES = 6;
	//cells: its place this near a hero brings it out; this much further off it goes; this far
	//from its place, out of sight, it has strayed; and how far from its place it is set down to
	//be out of every hero's sight
	static final int SPAWN = 32, LET_GO = 16, STRAY = 40, HIDE = 6;
	//turns of its trail its tracks show, cells between two prints, and turns between two looks
	//along its way for the next
	static final int TRACKS = WorldEvents.FC / 2, PRINT_GAP = 2, PRINT_BEAT = 8;
	/** The purse it leaves, coins (give or take 15%). */
	public static final int GOLD = 250;
	//a lair keeps this far from the town
	static final int TOWN_CLEAR = WorldModel.TOWN_IN + 24;

	private static final long SALT_ROLL = 0x20A3F011L, SALT_LAIR = 0x1A12L, SALT_HAUNT = 0x4A07EL,
			SALT_NAME = 0x4A3EL, SALT_KIND = 0x1C1DL, SALT_FORCED = 0xF0BCE7L;

	/** The beasts that roam: each with its trophy's picture. */
	public enum Kind {
		WOLF( ItemSpriteSheet.TROPHY_PELT ),
		YETI( ItemSpriteSheet.TROPHY_FANG ),
		SCORPION( ItemSpriteSheet.TROPHY_STINGER );

		public final int trophy;

		Kind( int trophy ){
			this.trophy = trophy;
		}

		/** The kind that makes its lair on this ground, or null: the desert's scorpion, the snow's
		 *  yeti, the tundra's yeti or wolf (by `h`), and the wolf of the woods, meadows, plains and
		 *  hills. Never the water, the swamp, the shore or the peaks. */
		public static Kind of( WorldModel.Biome b, long h ){
			switch (b){
				case DESERT:    return SCORPION;
				case SNOWFIELD: return YETI;
				case TUNDRA:    return (h & 1L) == 0 ? YETI : WOLF;
				case FOREST: case MEADOW: case PLAINS: case FOOTHILLS:
					return WOLF;
				default:
					return null;
			}
		}

		/** Ground this kind roams over. */
		public boolean keepsTo( WorldModel.Biome b ){
			switch (this){
				case SCORPION: return b == WorldModel.Biome.DESERT;
				case YETI:     return b == WorldModel.Biome.SNOWFIELD || b == WorldModel.Biome.TUNDRA
						|| b == WorldModel.Biome.FOOTHILLS;
				default:       return b == WorldModel.Biome.FOREST || b == WorldModel.Biome.MEADOW
						|| b == WorldModel.Biome.PLAINS || b == WorldModel.Biome.FOOTHILLS
						|| b == WorldModel.Biome.TUNDRA || b == WorldModel.Biome.SNOWFIELD;
			}
		}

		/** A new beast of the kind. */
		public Mob make(){
			switch (this){
				case YETI:     return new ElderYeti();
				case SCORPION: return new GiantScorpio();
				default:       return new GreatWolf();
			}
		}

		public String key(){
			return name().toLowerCase( Locale.ENGLISH );
		}
	}

	// ------------------------------------------------------------ the schedule (pure)

	//can a beast make its lair, or a haunt, on this world cell: open ground (at the annual mean)
	//away from the town and every settlement's streets
	private static boolean lairGround( long seed, int x, int y ){
		return Math.max( Math.abs( x ), Math.abs( y ) ) >= TOWN_CLEAR
				&& WorldEvents.openGround( WorldEvents.baseTerrain( seed, x, y ) )
				&& !OverworldFauna.nearSettlement( seed, x, y );
	}

	//the lairs worked out, by region and period: pure, so kept per seed ({} for none)
	private static volatile long lairSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, int[]> lairs = new ConcurrentHashMap<>();

	/** The lair of region (rx, ry)'s beast in a period, world {x, y}, or null: the first of TRIES
	 *  hashed spots INSIDE the region on ground some kind makes its lair on. */
	static int[] lair( long seed, int rx, int ry, int period ){
		if (seed != lairSeed){
			synchronized (lairs){
				if (seed != lairSeed){
					lairs.clear();
					lairSeed = seed;
				}
			}
		}
		long key = WorldEvents.mix( WorldStructures.sectorOf( rx, ry ), period, 0x1A12 );
		int[] known = lairs.get( key );
		if (known != null) return known.length == 0 ? null : known;
		int[] found = null;
		int side = REGION * WorldStructures.SECTOR - 2 * INSIDE;
		for (int k = 0; k < TRIES && found == null; k++){
			long h = WorldEvents.mix( seed ^ SALT_LAIR, WorldStructures.sectorOf( rx, ry ), period * 16L + k );
			int x = rx * REGION * WorldStructures.SECTOR + INSIDE + (int) Math.floorMod( h, (long) side );
			int y = ry * REGION * WorldStructures.SECTOR + INSIDE + (int) Math.floorMod( h >>> 24, (long) side );
			if (Kind.of( WorldModel.baseBiomeAt( seed, x, y ), h >>> 48 ) != null && lairGround( seed, x, y )) found = new int[]{ x, y };
		}
		if (lairs.size() > 4096) lairs.clear();
		lairs.put( key, found == null ? new int[0] : found );
		return found;
	}

	/** The beast roaming region (rx, ry) in a period, or null: one period in ODDS, abroad DAYS days
	 *  from a hashed day of it. Anchored on the region's middle sector; its world cell is its lair. */
	public static WorldEvents.Event roamer( long seed, int rx, int ry, int period ){
		long h = WorldEvents.mix( seed ^ SALT_ROLL, WorldStructures.sectorOf( rx, ry ), period );
		if (Math.floorMod( h, (long) ODDS ) != 0) return null;
		int[] lair = lair( seed, rx, ry, period );
		if (lair == null) return null;
		int start = period * PERIOD + (int) Math.floorMod( h >>> 8, (long)(PERIOD - DAYS + 1) );
		int ax = rx * REGION + REGION / 2, ay = ry * REGION + REGION / 2;
		return new WorldEvents.Event( WorldEvents.Type.ROAMER, ax, ay, start, start * WorldEvents.FC,
				(start + DAYS) * WorldEvents.FC, lair[0], lair[1], WorldEvents.idOf( WorldEvents.Type.ROAMER, ax, ay, start ) );
	}

	/** A beast forced to make its lair on world cell (wx, wy) from a turn for DAYS days (debug
	 *  scenes): its kind the ground's there (a wolf on any other), an id of its own. */
	public static WorldEvents.Event forcedRoamer( int wx, int wy, int turn ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		int d = WorldEvents.day( turn );
		return new WorldEvents.Event( WorldEvents.Type.ROAMER, sx, sy, d, turn, turn + DAYS * WorldEvents.FC, wx, wy,
				WorldEvents.mix( WorldEvents.idOf( WorldEvents.Type.ROAMER, sx, sy, d ) ^ SALT_FORCED, turn, 0 ) );
	}

	/** A roaming beast's kind and its haunts: one a day, the first its lair. */
	public static final class Haunts {
		public final Kind kind;
		public final int[] x, y;
		Haunts( Kind kind, int[] x, int[] y ){
			this.kind = kind;
			this.x = x;
			this.y = y;
		}
	}

	//the haunts worked out, by event and world (an event's id is the same in every world): pure
	private static final ConcurrentHashMap<Long, Haunts> haunts = new ConcurrentHashMap<>();

	/** A beast's kind (its lair's ground's) and its DAYS haunts: each after the first up to STEP
	 *  cells from the day before's, on its kind's ground and INSIDE the REGION round its anchor
	 *  sector; where none of TRIES spots will do, it stays the day where it was. Pure. */
	public static Haunts haunts( long seed, WorldEvents.Event e ){
		long key = WorldEvents.mix( e.id, seed, 0x4A07 );
		Haunts known = haunts.get( key );
		if (known != null) return known;
		Kind kind = Kind.of( WorldModel.baseBiomeAt( seed, e.wx, e.wy ), WorldEvents.mix( e.id ^ SALT_KIND, 0, 0 ) );
		if (kind == null) kind = Kind.WOLF;
		int half = REGION / 2;
		int minX = (e.sx - half) * WorldStructures.SECTOR + INSIDE, maxX = (e.sx + half + 1) * WorldStructures.SECTOR - INSIDE;
		int minY = (e.sy - half) * WorldStructures.SECTOR + INSIDE, maxY = (e.sy + half + 1) * WorldStructures.SECTOR - INSIDE;
		int[] x = new int[DAYS], y = new int[DAYS];
		x[0] = e.wx;
		y[0] = e.wy;
		for (int i = 1; i < DAYS; i++){
			x[i] = x[i - 1];
			y[i] = y[i - 1];
			for (int k = 0; k < TRIES; k++){
				long h = WorldEvents.mix( seed ^ SALT_HAUNT ^ e.id, i, k );
				double a = (h & 0xFFFF) / 65536.0 * 2 * Math.PI;
				int d = STEP / 2 + (int) Math.floorMod( h >>> 16, (long)(STEP / 2 + 1) );
				int cx = Math.max( minX, Math.min( maxX, x[i - 1] + (int) Math.round( Math.cos( a ) * d ) ) );
				int cy = Math.max( minY, Math.min( maxY, y[i - 1] + (int) Math.round( Math.sin( a ) * d ) ) );
				if (kind.keepsTo( WorldModel.baseBiomeAt( seed, cx, cy ) ) && lairGround( seed, cx, cy )){
					x[i] = cx;
					y[i] = cy;
					break;
				}
			}
		}
		Haunts out = new Haunts( kind, x, y );
		if (haunts.size() > 256) haunts.clear();
		haunts.put( key, out );
		return out;
	}

	/** Where the beast is at a turn, world {x, y}: its lair all its first day; on each day after,
	 *  at the day before's haunt until LEAVE, on its way until ARRIVE (easing in and out), then at
	 *  the day's own. Pure. */
	public static int[] where( Haunts h, WorldEvents.Event e, int turn ){
		float days = (turn - e.startTurn) / (float) WorldEvents.FC;
		int i = (int) Math.floor( days );
		if (i < 1) return new int[]{ h.x[0], h.y[0] };
		if (i >= DAYS) return new int[]{ h.x[DAYS - 1], h.y[DAYS - 1] };
		float f = days - i;
		float t = f <= LEAVE ? 0 : f >= ARRIVE ? 1 : (f - LEAVE) / (ARRIVE - LEAVE);
		t = t * t * (3 - 2 * t);
		return new int[]{ Math.round( h.x[i - 1] + (h.x[i] - h.x[i - 1]) * t ), Math.round( h.y[i - 1] + (h.y[i] - h.y[i - 1]) * t ) };
	}

	/** The beast as the world map pins it: in the middle of the sector it haunts now (WorldEvents.onMap). */
	static WorldEvents.Event onMap( long seed, WorldEvents.Event e, int turn ){
		int[] p = where( haunts( seed, e ), e, turn );
		int sx = Math.floorDiv( p[0], WorldStructures.SECTOR ), sy = Math.floorDiv( p[1], WorldStructures.SECTOR );
		return new WorldEvents.Event( e.type, e.sx, e.sy, e.startDay, e.startTurn, e.endTurn,
				sx * WorldStructures.SECTOR + WorldStructures.SECTOR / 2, sy * WorldStructures.SECTOR + WorldStructures.SECTOR / 2, e.id );
	}

	/** Its name among its kind's: fixed by the event. */
	public static int nameOf( long id ){
		return (int) Math.floorMod( WorldEvents.mix( id ^ SALT_NAME, 0, 0 ), (long) NAMES );
	}

	/** A kind's name number `name`: Old Greymane, Hoarfrost... */
	public static String title( Kind kind, int name ){
		return Messages.get( RoamerEvent.class, "name_" + kind.key() + "_" + Math.floorMod( name, NAMES ) );
	}

	/** A roaming beast's own name (its Roaming mark), or `plain` for one without. */
	public static String title( Mob m, String plain ){
		Roaming r = m.buff( Roaming.class );
		return r == null ? plain : title( kindOf( m ), r.name );
	}

	/** The kind a beast is. */
	public static Kind kindOf( Mob m ){
		return m instanceof ElderYeti ? Kind.YETI : m instanceof GiantScorpio ? Kind.SCORPION : Kind.WOLF;
	}

	// ------------------------------------------------------------ now

	//the surface being played, or null
	private static OverworldLevel surface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0
				? (OverworldLevel) Dungeon.level : null;
	}

	//where each beast about the window was as of the host's last step (world {x, y}), for its
	//haunt: actor thread only
	private static final HashMap<Long, int[]> placeNow = new HashMap<>();

	/** A roaming beast's haunt of the hour (GreatWolf, ElderYeti, GiantScorpio.act): the open cell
	 *  nearest where it should be now, or -1 when that is off the window. */
	public static int haunt( Mob m ){
		Roaming r = m.buff( Roaming.class );
		OverworldLevel ow = surface();
		if (r == null || ow == null) return -1;
		int[] p = placeNow.get( r.event );
		if (p == null) return -1;
		int c = ow.localCell( p[0], p[1] );
		if (c == -1) return -1;
		int open = HuntEvent.openGround( ow, c, 4 );
		return open != -1 ? open : -1;
	}

	/** A roaming beast abroad, as the villages' notice boards can ask of it: the event, its kind and
	 *  name, the world cell it is at now, and the turn its days end. */
	public static final class Sighting {
		public final long id;
		public final Kind kind;
		public final String name;
		public final int wx, wy, endTurn;
		Sighting( long id, Kind kind, String name, int wx, int wy, int endTurn ){
			this.id = id;
			this.kind = kind;
			this.name = name;
			this.wx = wx;
			this.wy = wy;
			this.endTurn = endTurn;
		}
	}

	/** The roaming beasts abroad now, not slain, within `sectors` sectors (Chebyshev, by where each
	 *  is now) of world cell (wx, wy), nearest first: pure placement, the log's settled and forced
	 *  events through its clock, which is only looked at. Any thread. */
	public static ArrayList<Sighting> abroad( OverworldLevel ow, int wx, int wy, int sectors ){
		ArrayList<Sighting> out = new ArrayList<>();
		if (ow == null || ow.altitude() != 0) return out;
		int turn = ow.eventLog().peekClock( WorldClock.turn() );
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (WorldEvents.Event e : WorldEvents.eventsNear( ow.worldSeed(), sx, sy, sectors + REGION / 2 + 1,
				WorldEvents.day( turn ), ow.eventLog().forced )){
			if (e.type != WorldEvents.Type.ROAMER || !e.activeAt( turn ) || ow.eventResolved( e.id )) continue;
			Haunts h = haunts( ow.worldSeed(), e );
			int[] p = where( h, e, turn );
			if (Math.max( Math.abs( Math.floorDiv( p[0], WorldStructures.SECTOR ) - sx ),
					Math.abs( Math.floorDiv( p[1], WorldStructures.SECTOR ) - sy ) ) > sectors) continue;
			out.add( new Sighting( e.id, h.kind, title( h.kind, nameOf( e.id ) ), p[0], p[1], e.endTurn ) );
		}
		out.sort( (a, b) -> Long.compare( (long)(a.wx - wx) * (a.wx - wx) + (long)(a.wy - wy) * (a.wy - wy),
				(long)(b.wx - wx) * (b.wx - wx) + (long)(b.wy - wy) * (b.wy - wy) ) );
		return out;
	}

	/** Is this roaming beast (an event id) slain? Any thread. */
	public static boolean slain( OverworldLevel ow, long id ){
		return ow != null && ow.eventResolved( id );
	}

	// ------------------------------------------------------------ the hero's step

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents):
	 *  the beasts abroad about the window brought out near a hero and sent off when far, heard of
	 *  once a hero sees one or its tracks, and the tracks shown. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		int turn = level.eventTurn();
		ArrayList<WorldEvents.Event> on = new ArrayList<>();
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type == WorldEvents.Type.ROAMER && e.activeAt( turn ) && !level.eventResolved( e.id )) on.add( e );
		}
		placeNow.clear();
		sendOff( level, on );
		ArrayList<Hero> party = OverworldLevel.heroesOn( level );
		for (WorldEvents.Event e : on){
			Haunts h = haunts( level.worldSeed(), e );
			int[] p = where( h, e, turn );
			placeNow.put( e.id, p );
			Mob beast = keep( level, e, h, p );
			notice( level, party, e, h, p, beast, turn );
		}
		if (level.liveScene()) showTracks( level, on, turn );
		LagMonitor.end( "OW roamer", t0 );
	}

	//the beast of an event in the window, or null
	private static Mob beastOf( OverworldLevel level, long id ){
		for (Mob m : level.mobs){
			Roaming r = m.buff( Roaming.class );
			if (r != null && r.event == id && m.isAlive()) return m;
		}
		return null;
	}

	//out near a hero, gone when far or strayed where nobody sees: the beast in the window, or null
	private static Mob keep( OverworldLevel level, WorldEvents.Event e, Haunts h, int[] p ){
		Mob beast = beastOf( level, e.id );
		int cell = level.localCell( p[0], p[1] );
		if (beast != null){
			if (!NetManager.anyHeroSees( beast.pos ) && beast.state != beast.HUNTING
					&& (OverworldLevel.heroDistance( level, beast.pos ) > SPAWN + LET_GO
							|| cell == -1 || level.distance( beast.pos, cell ) > STRAY)){
				remember( level, beast );
				OverworldLevel.vanish( level, beast );
				return null;
			}
			return beast;
		}
		if (cell == -1 || OverworldLevel.heroDistance( level, cell ) > SPAWN) return null;
		int w = level.width(), x = cell % w, y = cell / w;
		if (x < 3 || y < 3 || x > w - 4 || y > level.height() - 4) return null;
		int at = HuntEvent.hiddenGround( level, cell, HIDE, 4 );
		if (at == -1) return null;
		Mob m = h.kind.make();
		Roaming r = Buff.affect( m, Roaming.class );
		r.event = e.id;
		r.name = nameOf( e.id );
		r.endTurn = e.endTurn;
		Integer hp = level.eventLog().wounds.get( e.id );
		if (hp != null) m.HP = Math.max( 1, Math.min( m.HT, hp ) );
		return level.addMob( m, at ) ? m : null;
	}

	//a beast leaving keeps its wounds until its days are over
	private static void remember( OverworldLevel level, Mob m ){
		Roaming r = m.buff( Roaming.class );
		if (r == null || m.HP >= m.HT) return;
		level.eventLog().wounds.put( r.event, m.HP );
		level.eventLog().woundEnds.put( r.event, r.endTurn );
	}

	//beasts whose days are over (or settled) go once nobody sees them
	private static void sendOff( OverworldLevel level, List<WorldEvents.Event> on ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			Roaming r = m.buff( Roaming.class );
			if (r == null) continue;
			boolean live = false;
			for (WorldEvents.Event e : on) live |= e.id == r.event;
			if (!live && !NetManager.anyHeroSees( m.pos )) OverworldLevel.vanish( level, m );
		}
	}

	/** A mob leaving the window (OverworldLevel.park): a roaming beast is its event's to put back,
	 *  never parked, and keeps its wounds. True when it was one, and it is gone. */
	static boolean leaving( OverworldLevel level, Mob m ){
		if (m.buff( Roaming.class ) == null) return false;
		remember( level, m );
		OverworldLevel.vanish( level, m );
		return true;
	}

	//heard of once a hero sees the beast, or its fresh tracks: each who saw it is told, every
	//player finds it pinned on the map
	private static void notice( OverworldLevel level, ArrayList<Hero> party, WorldEvents.Event e, Haunts h, int[] p,
			Mob beast, int turn ){
		if (level.eventAnnounced( e.id )) return;
		final String name = title( h.kind, nameOf( e.id ) );
		if (beast != null){
			final int at = beast.pos;
			boolean seen = false;
			for (Hero hero : party) seen |= level.heroSees( hero, at );
			if (seen){
				level.markAnnounced( e );
				level.tellParty( ( hero, wx, wy ) -> level.heroSees( hero, at )
						? GLog.WARNING + Messages.get( RoamerEvent.class, "seen_" + h.kind.key(), name ) : null );
				GuideGraph.reveal( GuideGraph.TAG_WORLD_EVENT );
				if (level.liveScene() && level.heroSees( Dungeon.hero, at )) Sample.INSTANCE.play( Assets.Sounds.ALERT, 0.7f );
				return;
			}
		}
		ArrayList<int[]> trail = trail( level, e, h, turn );
		for (Hero hero : party){
			for (int[] print : trail){
				if (!level.heroSees( hero, print[0] )) continue;
				level.markAnnounced( e );
				level.tellParty( ( o, wx, wy ) -> {
					for (int[] q : trail){
						if (!level.heroSees( o, q[0] )) continue;
						String dir = WorldEvents.direction( p[0] - wx, p[1] - wy );
						return GLog.WARNING + Messages.get( RoamerEvent.class, "tracks_" + h.kind.key(),
								dir.equals( "here" ) ? Messages.get( RoamerEvent.class, "close_by" )
										: Messages.get( RoamerEvent.class, "toward", Messages.get( WorldEvents.class, "dir_" + dir ) ) );
					}
					return null;
				} );
				GuideGraph.reveal( GuideGraph.TAG_WORLD_EVENT );
				return;
			}
		}
	}

	/** A roaming beast is dead (GreatWolf, ElderYeti, GiantScorpio.die): settled for good, and its
	 *  trophy, a purse and a good item where it fell; the party hears of it. */
	public static void slain( Mob m ){
		Roaming r = m.buff( Roaming.class );
		OverworldLevel ow = surface();
		if (r == null || ow == null || ow.eventResolved( r.event )) return;
		ow.resolveEvent( r.event, r.endTurn );
		ow.eventLog().wounds.remove( r.event );
		ow.eventLog().woundEnds.remove( r.event );
		Kind kind = kindOf( m );
		int at = RaidEvent.dropCell( ow, m.pos );
		for (Item it : new Item[]{ RoamerTrophy.of( kind, r.name, r.event ),
				new Gold( Math.round( GOLD * Random.Float( 0.85f, 1.15f ) ) ), RaidEvent.prize() }){
			Heap heap = ow.drop( it, at );
			if (heap.sprite != null) heap.sprite.drop();
		}
		final String name = title( kind, r.name );
		ow.tellParty( ( h, wx, wy ) -> GLog.POSITIVE + Messages.get( RoamerEvent.class, "slain", name ) );
		if (ow.liveScene()) Sample.INSTANCE.play( Assets.Sounds.GOLD );
	}

	// ------------------------------------------------------------ the folk's talk

	//the land a beast is in, as the folk say it
	private static String land( WorldModel.Biome b ){
		switch (b){
			case DESERT:    return "land_desert";
			case SNOWFIELD: case TUNDRA: return "land_snow";
			case FOOTHILLS: return "land_hills";
			case FOREST:    return "land_woods";
			default:        return "land_fields";
		}
	}

	/** What the folk of this settlement (a packed sector) say of a roaming beast within RUMOUR
	 *  sectors of it (Settler.interact), or null: where it was last seen and the way from the
	 *  village. The party has heard of it after that (pinned on the map); once it has, the talk
	 *  comes round one time in three. Actor thread. */
	public static String rumour( OverworldLevel ow, long sector ){
		if (ow == null || ow.altitude() != 0 || sector == Long.MIN_VALUE) return null;
		int sx = (int)(sector >> 32), sy = (int) sector;
		int vx = WorldStructures.siteX( ow.worldSeed(), sx, sy ), vy = WorldStructures.siteY( ow.worldSeed(), sx, sy );
		int turn = ow.eventTurn();
		//every beast that can be within RUMOUR of the village, as abroad asks: one roams up to half
		//its region from the region's middle, further off than the window's own events reach
		for (WorldEvents.Event e : WorldEvents.eventsNear( ow.worldSeed(), sx, sy, RUMOUR + REGION / 2 + 1,
				WorldEvents.day( turn ), ow.eventLog().forced )){
			if (e.type != WorldEvents.Type.ROAMER || !e.activeAt( turn ) || ow.eventResolved( e.id )) continue;
			Haunts h = haunts( ow.worldSeed(), e );
			int[] p = where( h, e, turn );
			if (Math.max( Math.abs( Math.floorDiv( p[0], WorldStructures.SECTOR ) - sx ),
					Math.abs( Math.floorDiv( p[1], WorldStructures.SECTOR ) - sy ) ) > RUMOUR) continue;
			if (ow.eventAnnounced( e.id ) && Random.Int( 3 ) != 0) return null;
			ow.markAnnounced( e );
			GuideGraph.reveal( GuideGraph.TAG_WORLD_EVENT );
			String dir = WorldEvents.direction( p[0] - vx, p[1] - vy );
			String where = dir.equals( "here" ) ? Messages.get( RoamerEvent.class, "close_by" )
					: Messages.get( RoamerEvent.class, "toward", Messages.get( WorldEvents.class, "dir_" + dir ) );
			return Messages.get( RoamerEvent.class, "rumour_" + h.kind.key() + "_" + Random.Int( 2 ),
					title( h.kind, nameOf( e.id ) ), Messages.get( RoamerEvent.class, land( WorldModel.baseBiomeAt( ow.worldSeed(), p[0], p[1] ) ) ),
					where );
		}
		return null;
	}

	// ------------------------------------------------------------ the tracks

	/** The prints a beast left on the window's ground over the last TRACKS turns, oldest first, as
	 *  {window cell, frame (its kind's four ways: east, south, west, north), age in tenths (0
	 *  fresh to 9)}: every PRINT_GAP cells of the way it went, on open dry ground - never water,
	 *  a pit, the tall grass or what nobody can stand on. Each day's walk is followed from that
	 *  day's start, looked at every PRINT_BEAT turns from it, so a print keeps its cell from one
	 *  step to the next: the newest come and the oldest fade, none hops over as the turns go by.
	 *  Pure placement over the window's map. */
	static ArrayList<int[]> trail( OverworldLevel level, WorldEvents.Event e, Haunts h, int turn ){
		ArrayList<int[]> out = new ArrayList<>();
		int from = turn - TRACKS;
		for (int day = Math.max( 0, Math.floorDiv( from - e.startTurn, WorldEvents.FC ) );
				e.startTurn + day * WorldEvents.FC <= turn; day++){
			int dayStart = e.startTurn + day * WorldEvents.FC;
			int lx = Integer.MIN_VALUE, ly = Integer.MIN_VALUE;
			for (int t = dayStart; t < dayStart + WorldEvents.FC && t <= turn; t += PRINT_BEAT){
				int[] p = where( h, e, t );
				if (lx == Integer.MIN_VALUE){
					lx = p[0];
					ly = p[1];
					continue;
				}
				int dx = p[0] - lx, dy = p[1] - ly;
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) < PRINT_GAP) continue;
				lx = p[0];
				ly = p[1];
				//walked longer ago than its tracks last
				if (t < from) continue;
				int cell = level.localCell( p[0], p[1] );
				if (cell == -1 || !level.passable[cell] || level.water[cell] || level.pit[cell]
						|| level.map[cell] == Terrain.HIGH_GRASS || level.map[cell] == Terrain.FURROWED_GRASS) continue;
				int way = Math.abs( dx ) >= Math.abs( dy ) ? (dx > 0 ? 0 : 2) : (dy > 0 ? 1 : 3);
				out.add( new int[]{ cell, h.kind.ordinal() * 4 + way, Math.min( 9, (turn - t) * 10 / (TRACKS + 1) ) } );
			}
		}
		return out;
	}

	/** The window's tracks as effects/RoamerTracks shows them, in its scene pixels: the host's step
	 *  (showTracks) and a co-op guest's mirror (LifeDecorMirror) both build theirs here. */
	static ArrayList<RoamerTracks.Print> prints( OverworldLevel level, List<WorldEvents.Event> events, int turn ){
		ArrayList<RoamerTracks.Print> out = new ArrayList<>();
		int w = level.width();
		for (WorldEvents.Event e : events){
			if (e.type != WorldEvents.Type.ROAMER || !e.activeAt( turn ) || level.eventResolved( e.id )) continue;
			for (int[] p : trail( level, e, haunts( level.worldSeed(), e ), turn )){
				PointF at = DungeonTilemap.tileToWorld( p[0] );
				out.add( new RoamerTracks.Print( OverworldLevel.worldKey( level.worldX() + p[0] % w, level.worldY() + p[0] / w ),
						at.x, at.y, p[1], p[2] ) );
			}
		}
		return out;
	}

	//the host's: posted to the render thread only when it changed (or the scene did)
	private static final RoamerTracks tracks = new RoamerTracks();
	private static int tracksScene = 0;
	private static List<RoamerTracks.Print> shown = new ArrayList<>();

	private static void showTracks( OverworldLevel level, List<WorldEvents.Event> on, int turn ){
		final int sceneId = System.identityHashCode( Game.scene() );
		final ArrayList<RoamerTracks.Print> list = prints( level, on, turn );
		if (sceneId == tracksScene && RoamerTracks.same( list, shown )) return;
		tracksScene = sceneId;
		shown = list;
		Game.runOnRenderThread( () -> tracks.show( sceneId, list ) );
	}

	// ------------------------------------------------------------ debug

	/** Debug (the roamer scenes): open ground for a lair `dist` cells from a world cell, in the
	 *  first of the eight ways round it with any, by the real rules; null when none. */
	public static int[] debugLair( long seed, int wx, int wy, int dist ){
		for (int k = 0; k < 8; k++){
			double a = k * Math.PI / 4;
			for (int d = dist; d <= dist + 12; d += 3){
				int x = wx + (int) Math.round( Math.cos( a ) * d ), y = wy + (int) Math.round( Math.sin( a ) * d );
				if (lairGround( seed, x, y ) && Kind.of( WorldModel.baseBiomeAt( seed, x, y ), 0 ) != null) return new int[]{ x, y };
			}
		}
		return null;
	}
}
