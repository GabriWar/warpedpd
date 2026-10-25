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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalChampion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalCook;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FestivalDecor;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A human village's festival (WorldEvents.Type.FESTIVAL), once a season: a day and a night, from
 * the morning of its day (OPEN, the markets' morning on either clock) to the next morning.
 *
 * WHEN. Every human village with folk in it (a settlement can roll no lived-in house) holds one
 * festival a season, on a day of the season hashed from the world seed, the sector and the
 * season's first day (EventSeasons): EDGE days or more from either end, so it never runs into
 * the next season. Its name is the season's: the spring festival, midsummer, the harvest
 * festival, midwinter. A hero hears of one within HEAR (3) sectors, with the way to go, and finds
 * it pinned on the world map until it ends. FestivalEventTest measures how often.
 *
 * THE DAY. A maypole crowned with flowers stands on the well, and bunting and paper lanterns run
 * from it to the roofs of the nearest houses (effects/FestivalDecor), lit from dusk to dawn. Two
 * cooks (FestivalCook) set up stalls on the rings STALL_RING_MIN to STALL_RING_MAX of the well,
 * out past the dance, and sell their meals at half the shelf price; the village's champion
 * (FestivalChampion) takes on all comers in a friendly bout - the festival's contest - and the
 * first who beats him takes its prize, a good weapon, armour, wand or ring (RaidEvent.prize), once
 * a festival. The folk put their work by for the well (VillageRoutine.festivalSlot), and from
 * dusk into the night they dance round it in two rings, the inner one way and the outer the
 * other, while the band plays (music: a phrase now and then within MUSIC_REACH of the well,
 * livelier from dusk). The weather still sends everyone in, the band too.
 *
 * The stalls go up and the champion comes out once the well lies MARGIN cells inside the window;
 * the champion is the festival's to put back (he is never parked, leaving), the cooks are struck
 * with their stalls when it ends, as a market's traders are (OverworldLevel.expireEvents), and a
 * cook chased off takes the whole festival with him (OverworldLevel.merchantFled). Nothing of it
 * is saved but the cooks, the champion and the event's settled ids (the festival, its prize).
 */
public final class FestivalEvent {

	private FestivalEvent(){}

	/** The turn of its day a festival opens, and of the next day it ends: the morning on either clock. */
	public static final int OPEN = WorldEvents.MARKET_OPEN;
	//days kept clear of either end of its season
	static final int EDGE = 2;
	//sectors: a festival this near a hero of the party is heard of (a market's reach)
	static final int HEAR = 3;
	//cells: the stalls go up and the champion comes out only with the well this far inside the window
	static final int MARGIN = 14;
	//the rings of the well the food stalls stand on (out past the dance, VillageRoutine.DANCE_OUTER),
	//and the champion's
	static final int STALL_RING_MIN = 7, STALL_RING_MAX = 11;
	static final int CHAMPION_RING_MIN = 6, CHAMPION_RING_MAX = 9;
	//the bunting runs to the roofs of this many of the nearest houses, within this many cells of the well
	static final int ROPES = 4, ROPE_REACH = 12;
	//cells: within this of the well the band is heard
	static final int MUSIC_REACH = 22;

	private static final long SALT_DAY = 0xFE57DA7EL, SALT_PRIZE = 0xF1E5A7EL, SALT_FORCED = 0xF0BCE5L,
			SALT_PITCH = 0x5E7A11L;

	// ------------------------------------------------------------ the schedule (pure)

	/** Does the settlement of sector (sx, sy) keep festivals: a human village with folk in it. */
	static boolean keepsFestivals( long seed, int sx, int sy ){
		return WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.VILLAGE
				&& WorldStructures.faction( seed, sx, sy ) == WorldStructures.Faction.HUMAN
				&& WorldStructures.populatedHouses( (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 ) > 0;
	}

	/** The day of a season the village of sector (sx, sy) opens its festival on: EDGE days or more
	 *  from the season's start, and its next morning EDGE days or more from its end. */
	static int festivalDay( long seed, int sx, int sy, EventSeasons.Span s ){
		int room = Math.max( 1, s.days - 2 * EDGE - 1 );
		long h = WorldEvents.mix( seed ^ SALT_DAY, WorldStructures.sectorOf( sx, sy ), s.first );
		return s.first + EDGE + (int) Math.floorMod( h >>> 8, (long) room );
	}

	/** The festival the village of sector (sx, sy) opens on a day, or null: it opens on one day a
	 *  season. The cheap test first, so the village is looked at once a season. */
	public static WorldEvents.Event festivalEvent( long seed, int sx, int sy, int day ){
		if (festivalDay( seed, sx, sy, EventSeasons.of( day ) ) != day || !keepsFestivals( seed, sx, sy )) return null;
		return new WorldEvents.Event( WorldEvents.Type.FESTIVAL, sx, sy, day, day * WorldEvents.FC + OPEN,
				(day + 1) * WorldEvents.FC + OPEN, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				WorldEvents.idOf( WorldEvents.Type.FESTIVAL, sx, sy, day ) );
	}

	/** A festival forced on the village of sector (sx, sy) from a turn until the morning after its
	 *  day (debug scenes): an id of its own. */
	public static WorldEvents.Event forcedFestival( long seed, int sx, int sy, int turn ){
		int d = WorldEvents.day( turn );
		return new WorldEvents.Event( WorldEvents.Type.FESTIVAL, sx, sy, d, turn, (d + 1) * WorldEvents.FC + OPEN,
				WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				WorldEvents.mix( WorldEvents.idOf( WorldEvents.Type.FESTIVAL, sx, sy, d ) ^ SALT_FORCED, turn,
						WorldStructures.sectorOf( sx, sy ) ) );
	}

	/** The festival's name by the season of its day: the spring festival, midsummer... */
	public static String seasonName( WorldEvents.Event e ){
		return Messages.get( FestivalEvent.class, "season_" + EventSeasons.of( e.startDay ).season.name().toLowerCase( Locale.ENGLISH ) );
	}

	/** The id its prize is settled by, once won. */
	static long prizeId( long festival ){
		return WorldEvents.mix( festival ^ SALT_PRIZE, 1, 2 );
	}

	// ------------------------------------------------------------ now, as the folk read it

	//the surface being played, or null
	private static OverworldLevel surface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0
				? (OverworldLevel) Dungeon.level : null;
	}

	/** The festival on now in the village of sector (sx, sy), not settled, or null: worked out from
	 *  the world, the log's forced events and its settled ones, through the log's clock, which is
	 *  only looked at. Any thread. */
	static WorldEvents.Event current( OverworldLevel ow, int sx, int sy ){
		int turn = ow.eventLog().peekClock( WorldClock.turn() );
		int day = WorldEvents.day( turn );
		for (int d = day - 1; d <= day; d++){
			WorldEvents.Event e = festivalEvent( ow.worldSeed(), sx, sy, d );
			if (e != null && e.activeAt( turn ) && !ow.eventResolved( e.id )) return e;
		}
		for (WorldEvents.Event e : ow.eventLog().forced){
			if (e.type == WorldEvents.Type.FESTIVAL && e.sx == sx && e.sy == sy && e.activeAt( turn )
					&& !ow.eventResolved( e.id )) return e;
		}
		return null;
	}

	/** Is a festival on in this settlement (a packed sector) now? Its folk dance and feast (Settler). Any thread. */
	public static boolean on( OverworldLevel ow, long sector ){
		if (ow == null || ow.altitude() != 0 || sector == Long.MIN_VALUE) return false;
		return current( ow, (int)(sector >> 32), (int) sector ) != null;
	}

	/** Is the champion's festival still on? */
	public static boolean stillOn( FestivalChampion c ){
		OverworldLevel ow = surface();
		return ow != null && !ow.eventResolved( c.festival ) && ow.eventLog().peekClock( WorldClock.turn() ) < c.endTurn;
	}

	/** Has someone beaten the champion's festival's champion already? */
	public static boolean prizeTaken( FestivalChampion c ){
		OverworldLevel ow = surface();
		return ow != null && ow.eventResolved( prizeId( c.festival ) );
	}

	// ------------------------------------------------------------ the hero's step

	//a festival whose stalls found no room, by the window it looked in: not looked for again there
	private static final HashMap<Long, Integer> noRoom = new HashMap<>();

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents):
	 *  the festivals on about the window heard of, set up round their wells, their champions sent
	 *  home when they end, and what the host's screen shows and his ears hear of them. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		int turn = level.eventTurn();
		ArrayList<WorldEvents.Event> on = new ArrayList<>();
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type == WorldEvents.Type.FESTIVAL && e.activeAt( turn ) && !level.eventResolved( e.id )) on.add( e );
		}
		sendHome( level, on );
		ArrayList<Hero> party = OverworldLevel.heroesOn( level );
		for (WorldEvents.Event e : on){
			hear( level, party, e );
			setUp( level, e );
		}
		if (level.liveScene()){
			music( level, on, Dungeon.hero.pos );
			showDecor( level, on, turn );
		}
		LagMonitor.end( "OW festival", t0 );
	}

	//is a hero's sector within HEAR of the festival's village?
	private static boolean hears( OverworldLevel level, Hero h, WorldEvents.Event e ){
		if (h == null || h.pos < 0 || h.pos >= level.length()) return false;
		int w = level.width();
		int hsx = Math.floorDiv( level.worldX() + h.pos % w, WorldStructures.SECTOR );
		int hsy = Math.floorDiv( level.worldY() + h.pos / w, WorldStructures.SECTOR );
		return WorldEvents.within( e, hsx, hsy, HEAR );
	}

	//heard of by the party once, each player in his own words, and pinned on the map; a few notes
	//of the band's trumpet carried on the wind the way it lies
	private static void hear( OverworldLevel level, ArrayList<Hero> party, WorldEvents.Event e ){
		if (level.eventAnnounced( e.id )) return;
		boolean heard = false;
		for (Hero h : party) heard |= hears( level, h, e );
		if (!heard) return;
		level.markAnnounced( e );
		final String name = WorldStructures.villageName( level.worldSeed(), e.sx, e.sy ), season = seasonName( e );
		final int near = Math.max( WorldStructures.settlementLayout( level.worldSeed(), e.sx, e.sy )[0] + 3, WorldEvents.STAR_NEAR );
		level.tellParty( ( h, wx, wy ) -> {
			if (!hears( level, h, e )) return null;
			if (Math.max( Math.abs( e.wx - wx ), Math.abs( e.wy - wy ) ) <= near){
				return GLog.HIGHLIGHT + Messages.get( FestivalEvent.class, "heard_here", name, season );
			}
			return GLog.HIGHLIGHT + Messages.get( FestivalEvent.class, "heard", name, season,
					Messages.get( WorldEvents.class, "dir_" + WorldEvents.direction( e.wx - wx, e.wy - wy ) ) );
		} );
		GuideGraph.reveal( GuideGraph.TAG_WORLD_EVENT );
		if (level.liveScene() && hears( level, Dungeon.hero, e )){
			float pan = level.panTowards( e.wx );
			for (int i = 0; i < FANFARE.length; i++){
				SpatialSound.playPanned( Assets.Sounds.TRUMPET_NOTE, 0.2f * i, 0.4f, step( FANFARE[i] ), pan );
			}
		}
	}

	//the stalls round the well and the champion by it, once the well is well inside the window
	private static void setUp( OverworldLevel level, WorldEvents.Event e ){
		int well = level.localCell( e.wx, e.wy );
		if (well == -1) return;
		int w = level.width(), x = well % w, y = well / w;
		if (x < MARGIN || y < MARGIN || x >= w - MARGIN || y >= level.height() - MARGIN) return;
		int[] layout = WorldStructures.settlementLayout( level.worldSeed(), e.sx, e.sy );
		long h = WorldEvents.mix( level.worldSeed() ^ SALT_PITCH, e.id, 0 );
		Integer tried = noRoom.get( e.id );
		if ((tried == null || tried != level.windowVersion()) && !cooksAbout( level, e.id )){
			if (!pitchStalls( level, e, well, layout, h )) noRoom.put( e.id, level.windowVersion() );
		}
		if (!championAbout( level, e.id )) placeChampion( level, e, well, layout, h );
		if (noRoom.size() > 64) noRoom.clear();
	}

	//is a cook of this festival about, in the window or parked?
	private static boolean cooksAbout( OverworldLevel level, long id ){
		for (Mob m : level.mobs){
			if (m instanceof FestivalCook && ((FestivalCook) m).eventId == id) return true;
		}
		for (Mob m : level.parked()){
			if (m instanceof FestivalCook && ((FestivalCook) m).eventId == id) return true;
		}
		return false;
	}

	private static boolean championAbout( OverworldLevel level, long id ){
		for (Mob m : level.mobs){
			if (m instanceof FestivalChampion && ((FestivalChampion) m).festival == id) return true;
		}
		return false;
	}

	//the two cooks' stalls on opposite sides of the well from a hashed bearing, off the roads where
	//there is room (OverworldLevel.stallSpot: open, clear of the houses and of other shelves).
	//false when not one could be pitched
	private static boolean pitchStalls( OverworldLevel level, WorldEvents.Event e, int well, int[] layout, long h ){
		int w = level.width();
		int reach = Math.min( STALL_RING_MAX, layout[0] );
		int bearing = (int) Math.floorMod( h >>> 8, 8L );
		ArrayList<Integer> picked = new ArrayList<>();
		for (int i = 0; i < FestivalCook.STALLS; i++){
			int spot = -1;
			for (int pass = 0; pass < 2 && spot == -1; pass++){
				for (int r = STALL_RING_MIN; r <= reach && spot == -1; r++){
					int perim = 8 * r;
					int aim = (bearing * perim / 8 + i * perim / FestivalCook.STALLS) % perim;
					for (int k = 0; k < perim && spot == -1; k++){
						int idx = Math.floorMod( aim + ((k & 1) == 0 ? k / 2 : -(k + 1) / 2), perim );
						int dx = VillageRoutine.ringX( r, idx ), dy = VillageRoutine.ringY( r, idx );
						int c = well + dx + dy * w;
						if (level.stallSpot( c, dx, dy, layout, picked, pass == 0 )) spot = c;
					}
				}
			}
			if (spot != -1) picked.add( spot );
		}
		boolean show = level.liveScene();
		int opened = 0;
		for (int i = 0; i < picked.size(); i++){
			FestivalCook cook = FestivalCook.of( i, e.id, e.endTurn );
			if (!level.addMob( cook, picked.get( i ) )) continue;
			cook.openShop();
			opened++;
			//the cart rolls up in a cloud of road dust
			if (show) CellEmitter.get( cook.pos ).burst( Speck.factory( Speck.DUST ), 8 );
		}
		return opened > 0;
	}

	//the champion on the rings CHAMPION_RING_MIN to MAX, across the well from the stalls: first on
	//ground no hero sees, so nobody is seen to come out of nothing
	private static void placeChampion( OverworldLevel level, WorldEvents.Event e, int well, int[] layout, long h ){
		int w = level.width();
		int bearing = (int) Math.floorMod( (h >>> 8) + 2, 8L );
		for (int pass = 0; pass < 2; pass++){
			for (int r = CHAMPION_RING_MIN; r <= CHAMPION_RING_MAX; r++){
				int perim = 8 * r, aim = bearing * perim / 8;
				for (int k = 0; k < perim; k++){
					int idx = Math.floorMod( aim + ((k & 1) == 0 ? k / 2 : -(k + 1) / 2), perim );
					int dx = VillageRoutine.ringX( r, idx ), dy = VillageRoutine.ringY( r, idx );
					int c = well + dx + dy * w;
					if (!clearOfHouses( layout, dx, dy ) || !VillageRoutine.restsOn( level, c ) || level.occupied( c )
							|| (pass == 0 && NetManager.anyHeroSees( c ))) continue;
					FestivalChampion champ = FestivalChampion.of( e.id, e.endTurn, WorldStructures.sectorOf( e.sx, e.sy ) );
					if (level.addMob( champ, c )) return;
				}
			}
		}
	}

	//a cell (dx, dy) off the well is outside every house of the layout and a cell clear of it
	private static boolean clearOfHouses( int[] layout, int dx, int dy ){
		for (int i = 1; i + 1 < layout.length; i += 2){
			if (Math.max( Math.abs( dx - layout[i] ), Math.abs( dy - layout[i + 1] ) ) < 4) return false;
		}
		return true;
	}

	//champions whose festival is over (or was chased off) go home, once nobody sees them go
	private static void sendHome( OverworldLevel level, List<WorldEvents.Event> on ){
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof FestivalChampion)) continue;
			FestivalChampion c = (FestivalChampion) m;
			boolean over = true;
			for (WorldEvents.Event e : on) over &= e.id != c.festival;
			if (over && !c.inBout() && !NetManager.anyHeroSees( c.pos )) OverworldLevel.vanish( level, c );
		}
	}

	/** A mob leaving the window (OverworldLevel.park): a festival's champion is its festival's to put
	 *  back, never parked. True when it was his, and he is gone. */
	static boolean leaving( OverworldLevel level, Mob m ){
		if (!(m instanceof FestivalChampion)) return false;
		OverworldLevel.vanish( level, m );
		return true;
	}

	// ------------------------------------------------------------ the bout

	/** The bout begins (FestivalChampion.begin): the ring takes it up. */
	public static void boutBegins( FestivalChampion c, Hero h ){
		OverworldLevel ow = surface();
		if (ow == null || !ow.liveScene()) return;
		SpatialSound.play( Assets.Sounds.CHALLENGE, c.pos, 0.6f );
	}

	/** The champion yields (FestivalChampion.end): the festival's prize is the challenger's, at his
	 *  feet, once a festival. */
	public static void championYields( FestivalChampion c, Hero h ){
		OverworldLevel ow = surface();
		if (ow == null || ow.eventResolved( prizeId( c.festival ) )) return;
		ow.resolveEvent( prizeId( c.festival ), c.endTurn );
		if (c.sprite != null) c.sprite.showStatus( CharSprite.POSITIVE, Messages.get( FestivalEvent.class, "cheer" ) );
		Heap heap = ow.drop( RaidEvent.prize(), RaidEvent.dropCell( ow, h.pos ) );
		if (heap.sprite != null) heap.sprite.drop();
		String name = WorldStructures.villageName( ow.worldSeed(), (int)(c.village >> 32), (int) c.village );
		NetManager.heroLog( h, GLog.POSITIVE + Messages.get( FestivalEvent.class, "prize", name ) );
		if (ow.liveScene()) Sample.INSTANCE.play( Assets.Sounds.GOLD );
	}

	/** The champion won the bout (FestivalChampion.end): the challenger may catch his breath and try again. */
	public static void championWins( FestivalChampion c, Hero h ){
		NetManager.heroLog( h, GLog.WARNING + Messages.get( FestivalEvent.class, "bested" ) );
	}

	// ------------------------------------------------------------ the band

	//the steps of a major pentatonic scale a tune is made of (as audio/Notes keeps them)
	private static final int[] SCALE = { 0, 2, 4, 7, 9, 12 };
	//the fanfare as a festival is heard of, and the tunes: a dance from dusk, an air by day.
	//places on SCALE, a note every TUNE_GAP seconds, the dance's notes taken flute and harp in turn
	private static final int[] FANFARE = { 0, 2, 4 };
	private static final int[][] DANCES = { { 0, 2, 3, 2, 4, 3, 2, 0 }, { 3, 4, 5, 4, 3, 2, 1, 2 } };
	private static final int[] AIR = { 2, 1, 0, 1, 2 };
	private static final float TUNE_GAP = 0.17f;
	//real milliseconds between phrases: never more often, whatever turns go by (a rest)
	private static final long DANCE_EVERY = 4200, AIR_EVERY = 7000;
	private static long lastTune = 0;
	private static int tunes = 0;

	//a place on the scale, as a playback rate
	private static float step( int place ){
		return (float) Math.pow( 2, SCALE[Math.max( 0, Math.min( SCALE.length - 1, place ) )] / 12.0 );
	}

	/** The band, heard within MUSIC_REACH of a festival's well by the hero standing on heroPos: a
	 *  phrase now and then from the well, a dance from dusk into the night and an air by day. The
	 *  host's on his step, a co-op guest's from his mirror (LifeDecorMirror). Fixed tunes: nothing
	 *  random is drawn for them. */
	static void music( OverworldLevel level, List<WorldEvents.Event> events, int heroPos ){
		//the band does not play in the rain: everyone is indoors
		if (ClimateManager.isRaining() || ClimateManager.isStorming()) return;
		long now = System.currentTimeMillis();
		boolean dance = lanternsLit();
		if (now - lastTune < (dance ? DANCE_EVERY : AIR_EVERY)) return;
		int turn = level.eventLog().peekClock( WorldClock.turn() );
		for (WorldEvents.Event e : events){
			if (e.type != WorldEvents.Type.FESTIVAL || !e.activeAt( turn ) || level.eventResolved( e.id )) continue;
			int well = level.localCell( e.wx, e.wy );
			if (well == -1 || heroPos < 0 || level.distance( well, heroPos ) > MUSIC_REACH) continue;
			lastTune = now;
			if (dance){
				int[] tune = DANCES[tunes++ % DANCES.length];
				for (int i = 0; i < tune.length; i++){
					SpatialSound.playDelayed( i % 2 == 0 ? Assets.Sounds.FLUTE_NOTE : Assets.Sounds.HARP_NOTE,
							TUNE_GAP * i, well, 0.5f, step( tune[i] ) );
				}
			} else {
				for (int i = 0; i < AIR.length; i++){
					SpatialSound.playDelayed( Assets.Sounds.FLUTE_NOTE, TUNE_GAP * 1.5f * i, well, 0.4f, step( AIR[i] ) );
				}
			}
			return;
		}
	}

	// ------------------------------------------------------------ what is shown

	/** The lanterns burn from dusk to dawn. */
	static boolean lanternsLit(){
		DayNightCycle.Phase p = DayNightCycle.phase();
		return p == DayNightCycle.Phase.DUSK || p == DayNightCycle.Phase.NIGHT;
	}

	//the roofs the bunting runs to, by festival and world (an event's id is the same in every world):
	//interleaved world x, y of each rope's end (pure)
	private static final ConcurrentHashMap<Long, int[]> ropes = new ConcurrentHashMap<>();

	/** Where a festival's bunting is tied, as interleaved world x, y: on each of the ROPES houses
	 *  nearest the well within ROPE_REACH (Chebyshev to its centre, ties to the lower), the cell of
	 *  its walls nearest the well. A pure function of the world and the festival. */
	static int[] ropeEnds( long seed, WorldEvents.Event e ){
		long key = WorldEvents.mix( e.id, seed, 0x2093 );
		int[] hit = ropes.get( key );
		if (hit != null) return hit;
		int[] layout = WorldStructures.settlementLayout( seed, e.sx, e.sy );
		int houses = (layout.length - 1) / 2;
		boolean[] taken = new boolean[houses];
		int[] out = new int[2 * ROPES];
		int n = 0;
		for (int r = 0; r < ROPES; r++){
			int best = -1, bestD = ROPE_REACH + 1;
			for (int k = 0; k < houses; k++){
				int d = Math.max( Math.abs( layout[1 + 2 * k] ), Math.abs( layout[2 + 2 * k] ) );
				if (!taken[k] && d < bestD){
					bestD = d;
					best = k;
				}
			}
			if (best == -1) break;
			taken[best] = true;
			int hx = layout[1 + 2 * best], hy = layout[2 + 2 * best];
			out[n++] = e.wx + Math.max( hx - 2, Math.min( hx + 2, 0 ) );
			out[n++] = e.wy + Math.max( hy - 2, Math.min( hy + 2, 0 ) );
		}
		hit = java.util.Arrays.copyOf( out, n );
		if (ropes.size() > 64) ropes.clear();
		ropes.put( key, hit );
		return hit;
	}

	/**
	 * What the window shows of its festivals (effects/FestivalDecor), laid out in its scene pixels:
	 * each festival on and not settled with its well in the window, its rope ends that are in it too,
	 * and whether the lanterns are lit. The host's step (showDecor) and a co-op guest's mirror
	 * (LifeDecorMirror) both build theirs here, so every screen shows the same.
	 */
	static ArrayList<FestivalDecor.Fete> fetes( OverworldLevel level, List<WorldEvents.Event> events, int turn ){
		ArrayList<FestivalDecor.Fete> out = new ArrayList<>();
		boolean lit = lanternsLit();
		for (WorldEvents.Event e : events){
			if (e.type != WorldEvents.Type.FESTIVAL || !e.activeAt( turn ) || level.eventResolved( e.id )) continue;
			int well = level.localCell( e.wx, e.wy );
			if (well == -1) continue;
			int[] ends = ropeEnds( level.worldSeed(), e );
			ArrayList<float[]> at = new ArrayList<>();
			ArrayList<Long> keys = new ArrayList<>();
			for (int i = 0; i + 1 < ends.length; i += 2){
				int c = level.localCell( ends[i], ends[i + 1] );
				if (c == -1) continue;
				PointF p = DungeonTilemap.tileToWorld( c );
				at.add( new float[]{ p.x, p.y } );
				keys.add( OverworldLevel.worldKey( ends[i], ends[i + 1] ) );
			}
			long[] k = new long[keys.size()];
			float[] xy = new float[2 * at.size()];
			for (int i = 0; i < k.length; i++){
				k[i] = keys.get( i );
				xy[2 * i] = at.get( i )[0];
				xy[2 * i + 1] = at.get( i )[1];
			}
			PointF w = DungeonTilemap.tileToWorld( well );
			out.add( new FestivalDecor.Fete( e.id, w.x, w.y, k, xy, lit ) );
		}
		return out;
	}

	//the host's: posted to the render thread only when it changed (or the scene did)
	private static final FestivalDecor decor = new FestivalDecor();
	private static int decorScene = 0;
	private static List<FestivalDecor.Fete> shown = new ArrayList<>();

	private static void showDecor( OverworldLevel level, List<WorldEvents.Event> on, int turn ){
		final int sceneId = System.identityHashCode( Game.scene() );
		final ArrayList<FestivalDecor.Fete> fetes = fetes( level, on, turn );
		if (sceneId == decorScene && FestivalDecor.same( fetes, shown )) return;
		decorScene = sceneId;
		shown = fetes;
		Game.runOnRenderThread( () -> decor.show( sceneId, fetes ) );
	}

	// ------------------------------------------------------------ debug

	/** Debug (the festival scenes): the human village nearest a world cell with folk in it, within
	 *  `sectors`, as {sx, sy, cx, cy}; null for none. */
	public static int[] nearestVillage( long seed, int wx, int wy, int sectors ){
		for (int[] v : WorldStructures.settlementsNear( seed, wx, wy, WorldStructures.Faction.HUMAN, 2, sectors )){
			if (keepsFestivals( seed, v[0], v[1] )) return new int[]{ v[0], v[1], v[2], v[3] };
		}
		return null;
	}
}
