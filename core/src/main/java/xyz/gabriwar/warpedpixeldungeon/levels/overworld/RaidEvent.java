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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Settler;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bandits come down on a human village for a day (WorldEvents.Type.RAID). Where and when is a
 * pure function of the world seed, the sector and the day; what the heroes did about it (the
 * raid beaten off) is the event's settled id in the world's WorldEventLog; everything else is
 * the mobs' own (Raider).
 *
 * HOW RARE. Each human village with a house to its name (a settlement can roll none) rolls
 * once a day: struck one day in ODDS (60), one in ODDS_NEAR_BANDITS (20) with an outlaw camp
 * within BANDIT_REACH (2) sectors. On any day no two
 * raids lie within EXCLUSION (6) sectors of each other - the lower roll keeps its raid - so a
 * hero, who hears of raids within HEAR (3) sectors, never has two to deal with at once. All
 * told a human village is raided about one day in 44 (one in 37 beside the outlaws, one in 72
 * away from them), and a hero hears of a raid about every six or seven days (RaidEventTest
 * measures both on the test seed).
 *
 * THE DAY. The smoke of one burning house, two in a town of nine houses or more, rises over the
 * village (SettlementAmbience draws it as soot over the fog) and a hero within HEAR sectors
 * hears of it once, with the way to go, and finds it pinned on his world map. Its folk keep
 * indoors (Settler) and its vendors will not trade (OverworldShopkeeper.tradeBlock). The band -
 * three raiders, one more for every five houses up to six - comes out of the houses' doors (out
 * of sight where it can) once the well is in the window and the hero is within LIVE_MARGIN (20)
 * cells of the village's edge, or the whole village lies inside the window. They prowl from door
 * to door; in a town with a watch half of them go for the guards at the well, who wear them down
 * but leave the finishing to the hero (Raider.damage).
 *
 * THE END. Every raider dead, or won over, and the village is SAVED: the guard nearest the well
 * (its elder where it keeps no watch) thanks the hero with a purse - 60 coins and 15 more a
 * house - and a good weapon, armour, wand or ring, and its vendors sell to him at FAVOUR_PRICE
 * for the rest of the day and FAVOUR_DAYS more. A raid the day outlasts leaves the village
 * RAIDED: its vendors shut for the RECOVERY_DAYS after, and half its families stay in. Whatever
 * is left of the band makes off with the day (Raider.act). A raid a debug scene forced
 * (WorldEvents.forcedRaid) earns the favour like a natural one, but leaves no mending behind.
 *
 * Every day here is the world's events' own (day(), OverworldLevel.eventDayNow), whose clock
 * never runs back as a real-clock run's wall clock can: the day a band was given is the day the
 * band, the shops and the parked store read.
 */
public final class RaidEvent {

	private RaidEvent(){}

	static final int ODDS = 60, ODDS_NEAR_BANDITS = 20;
	static final int BANDIT_REACH = 2;
	//sectors: no two raids this near on a day; the hero hears of a raid this near his own sector.
	//EXCLUSION is at least twice HEAR, so he never hears of two at once
	static final int EXCLUSION = 6, HEAR = 3;
	//the band comes out when the hero is this many cells past the settlement's edge
	static final int LIVE_MARGIN = 20;
	//a saved village's favour: the rest of the raid's day and three more; a raided one keeps
	//its shops shut for the two days after
	public static final int FAVOUR_DAYS = 3, RECOVERY_DAYS = 2;
	static final float FAVOUR_PRICE = 0.75f;
	//a raid's band, by the size of the village
	static final int MIN_BAND = 3, MAX_BAND = 6;

	private static final long SALT_ROLL = 0x2A1D5EEDL, SALT_BAND = 0x2A1DBA4DL, SALT_FIRE = 0x5A0CEL;

	// ------------------------------------------------------------ the schedule (pure)

	//the day's roll for a village, uniform in [0, 1)
	static double roll( long seed, int sx, int sy, int day ){
		return (WorldEvents.mix( seed ^ SALT_ROLL, WorldStructures.sectorOf( sx, sy ), day ) >>> 11) * 0x1.0p-53;
	}

	//an outlaw camp within BANDIT_REACH sectors: the sites are pure, so the answer is kept per seed
	private static volatile long banditSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, Boolean> banditCache = new ConcurrentHashMap<>();

	static boolean nearBandits( long seed, int sx, int sy ){
		if (seed != banditSeed){
			synchronized (banditCache){
				if (seed != banditSeed){
					banditCache.clear();
					banditSeed = seed;
				}
			}
		}
		long key = WorldStructures.sectorOf( sx, sy );
		Boolean known = banditCache.get( key );
		if (known != null) return known;
		boolean near = false;
		for (int dy = -BANDIT_REACH; dy <= BANDIT_REACH && !near; dy++){
			for (int dx = -BANDIT_REACH; dx <= BANDIT_REACH && !near; dx++){
				if (dx == 0 && dy == 0) continue;
				near = WorldStructures.siteType( seed, sx + dx, sy + dy ) == WorldStructures.Site.VILLAGE
						&& WorldStructures.faction( seed, sx + dx, sy + dy ) == WorldStructures.Faction.BANDIT;
			}
		}
		banditCache.put( key, near );
		return near;
	}

	//passes its own roll: the cheap test first, so the exclusion's scan almost never reaches the
	//sites. a human village with no lived-in house (a city or a metropolis can roll none,
	//WorldStructures.settlementLayout) has nothing to burn, nor doors for a band to come out of
	static boolean candidate( long seed, int sx, int sy, int day ){
		double r = roll( seed, sx, sy, day );
		if (r >= 1.0 / ODDS_NEAR_BANDITS) return false;
		if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE
				|| WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.HUMAN
				|| WorldStructures.populatedHouses( (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 ) == 0) return false;
		return r < 1.0 / ODDS || nearBandits( seed, sx, sy );
	}

	/** Is this sector's village raided on this day? A pure function of (seed, sector, day): its
	 *  own roll passed, and no lower one within EXCLUSION sectors. */
	public static boolean raidedOn( long seed, int sx, int sy, int day ){
		if (!candidate( seed, sx, sy, day )) return false;
		double mine = roll( seed, sx, sy, day );
		long myKey = WorldStructures.sectorOf( sx, sy );
		for (int oy = sy - EXCLUSION; oy <= sy + EXCLUSION; oy++){
			for (int ox = sx - EXCLUSION; ox <= sx + EXCLUSION; ox++){
				if ((ox == sx && oy == sy) || !candidate( seed, ox, oy, day )) continue;
				double theirs = roll( seed, ox, oy, day );
				if (theirs < mine || (theirs == mine && WorldStructures.sectorOf( ox, oy ) < myKey)) return false;
			}
		}
		return true;
	}

	/** The raid on sector (sx, sy)'s village on a day, or null: its whole day, anchored on the
	 *  well (WorldEvents.eventsNear). */
	public static WorldEvents.Event raidEvent( long seed, int sx, int sy, int day ){
		if (!raidedOn( seed, sx, sy, day )) return null;
		return new WorldEvents.Event( WorldEvents.Type.RAID, sx, sy, day, day * WorldEvents.FC, (day + 1) * WorldEvents.FC,
				WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ),
				WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, day ) );
	}

	/** One raid, as the world lays it down. */
	static final class Raid {
		final int sx, sy, day, cx, cy, houses;
		final long sector, id;
		final int[] layout;   //WorldStructures.settlementLayout: never written to

		Raid( long seed, WorldEvents.Event e ){
			sx = e.sx;
			sy = e.sy;
			day = e.startDay;
			cx = e.wx;
			cy = e.wy;
			id = e.id;
			sector = WorldStructures.sectorOf( sx, sy );
			layout = WorldStructures.settlementLayout( seed, sx, sy );
			houses = (layout.length - 1) / 2;
		}

		int berth(){ return layout[0]; }
		//a hamlet's three, one more for every five houses, six at the most
		int raiders(){ return Math.min( MAX_BAND, MIN_BAND + houses / 5 ); }
		String name( long seed ){ return WorldStructures.villageName( seed, sx, sy ); }
		int houseX( int k ){ return cx + layout[1 + 2 * k]; }
		int houseY( int k ){ return cy + layout[2 + 2 * k]; }
		int doorX( int k ){ return cx + WorldStructures.houseDoorDX( layout[1 + 2 * k], layout[2 + 2 * k] ); }
		int doorY( int k ){ return cy + WorldStructures.houseDoorDY( layout[1 + 2 * k], layout[2 + 2 * k] ); }
	}

	// ------------------------------------------------------------ today, as the actors read it

	//the raids on about the window, by settlement sector (takeStock): one object behind a
	//volatile reference, so the shops' windows and the world map may read it from the render
	//thread. it answers only for the level, seed and day it was taken for
	static final class Today {
		final int levelId;
		final long seed;
		final int day;
		final HashMap<Long, WorldEvents.Event> bySector;

		Today( int levelId, long seed, int day, HashMap<Long, WorldEvents.Event> bySector ){
			this.levelId = levelId;
			this.seed = seed;
			this.day = day;
			this.bySector = bySector;
		}
	}
	private static volatile Today today;

	//the surface being played, or null
	private static OverworldLevel surface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0
				? (OverworldLevel) Dungeon.level : null;
	}

	/** The raids' day now: the world's events' (OverworldLevel.eventDayNow), never the wall
	 *  clock, which a real-clock run can turn back - a raider's day (Raider) is always read
	 *  against this one. The wall clock's off the surface. Any thread. */
	public static int day(){
		OverworldLevel ow = surface();
		return ow != null ? ow.eventDayNow() : WorldClock.day();
	}

	//does a snapshot answer for this level, its world and its day?
	private static boolean fresh( Today t, OverworldLevel ow ){
		return ow != null && t != null && t.levelId == System.identityHashCode( ow ) && t.seed == ow.worldSeed
				&& t.day == ow.eventDayNow();
	}

	//today's raid on a settlement, from the snapshot, or null
	private static WorldEvents.Event todays( OverworldLevel ow, long sector ){
		Today t = today;
		return fresh( t, ow ) ? t.bySector.get( sector ) : null;
	}

	/** Takes stock of today's raids about the window that are not beaten off, for the shops, the
	 *  folk, the fires and the band to read: on every step of the host's hero (onHeroStep), and as
	 *  the surface is read back from its save (OverworldLevel.restoreFromBundle) - a level read
	 *  back is a new one, which no snapshot answers for until then. Actor thread, or the level's
	 *  loading while the actors wait. */
	static Today takeStock( OverworldLevel level ){
		int turn = level.eventTurn();
		HashMap<Long, WorldEvents.Event> by = new HashMap<>();
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type == WorldEvents.Type.RAID && e.activeAt( turn ) && !level.eventResolved( e.id )){
				by.put( WorldStructures.sectorOf( e.sx, e.sy ), e );
			}
		}
		Today t = new Today( System.identityHashCode( level ), level.worldSeed, WorldEvents.day( turn ), by );
		today = t;
		return t;
	}

	/** Bandits in this settlement's streets now (a packed sector): a raid on today and not yet
	 *  beaten off. Any thread. */
	public static boolean ongoing( long sector ){
		if (sector == Long.MIN_VALUE) return false;
		OverworldLevel ow = surface();
		WorldEvents.Event e = todays( ow, sector );
		return e != null && !ow.eventResolved( e.id );
	}

	/** The hero beat off a raid on this settlement today or in the FAVOUR_DAYS before. Any thread. */
	public static boolean favoured( long sector ){
		OverworldLevel ow = surface();
		if (ow == null || sector == Long.MIN_VALUE) return false;
		int sx = (int)(sector >> 32), sy = (int) sector, day = ow.eventDayNow();
		for (int d = day - FAVOUR_DAYS; d <= day; d++){
			//only a raid beaten off is ever settled
			if (ow.eventResolved( WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, d ) )) return true;
		}
		return false;
	}

	//which of the RECOVERY_DAYS before a day each settlement was raided on (bit k-1: k days
	//before), pure and worked out once per seed and day; whether those raids were beaten off is
	//read live
	private static final class Memo {
		final long seed;
		final int day;
		final ConcurrentHashMap<Long, Integer> raided = new ConcurrentHashMap<>();
		Memo( long seed, int day ){
			this.seed = seed;
			this.day = day;
		}
	}
	private static volatile Memo memo;

	/** The settlement is mending what a raid the day outlasted broke: one on a natural raid's
	 *  village in the RECOVERY_DAYS before today that nobody beat off. Any thread. */
	public static boolean recovering( long sector ){
		OverworldLevel ow = surface();
		if (ow == null || sector == Long.MIN_VALUE) return false;
		int day = ow.eventDayNow();
		Memo m = memo;
		if (m == null || m.seed != ow.worldSeed || m.day != day){
			m = new Memo( ow.worldSeed, day );
			memo = m;
		}
		int sx = (int)(sector >> 32), sy = (int) sector;
		Integer bits = m.raided.get( sector );
		if (bits == null){
			int b = 0;
			for (int k = 1; k <= RECOVERY_DAYS; k++){
				if (raidedOn( m.seed, sx, sy, day - k )) b |= 1 << (k - 1);
			}
			bits = b;
			m.raided.put( sector, bits );
		}
		for (int k = 1; k <= RECOVERY_DAYS; k++){
			if ((bits & (1 << (k - 1))) != 0
					&& !ow.eventResolved( WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, day - k ) )) return true;
		}
		return false;
	}

	/** What a settlement's vendors charge the hero against the shelf price: a better price from a
	 *  village he saved (OverworldShopkeeper.priceFactor). */
	public static float priceFactor( long sector ){
		return favoured( sector ) ? FAVOUR_PRICE : 1f;
	}

	/** Why a settlement's vendors will not trade now, or null (OverworldShopkeeper.tradeBlock). */
	public static String shutReason( long sector ){
		if (ongoing( sector )) return Messages.get( RaidEvent.class, "shut_raid" );
		if (recovering( sector )) return Messages.get( RaidEvent.class, "shut_recovering" );
		return null;
	}

	/** Does the family of this house keep indoors (Settler): everyone while the bandits are in the
	 *  streets, half the families (every other house) while the village mends after them. */
	public static boolean shelters( long sector, int house ){
		return ongoing( sector ) || (recovering( sector ) && Math.floorMod( house, 2 ) == 0);
	}

	// ------------------------------------------------------------ the hero's step

	//the day and the level whose stale raiders were last swept from the parked store
	private static int forgotDay = Integer.MIN_VALUE, forgotLevel = 0;
	//a raid's withdrawal said once (not bundled: a reload may say it again, harmlessly)
	private static long withdrawnId = Long.MIN_VALUE;

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents):
	 *  today's raids about the window taken stock of, a raid heard of by its smoke, its band let
	 *  loose in the streets, and a band no longer fighting settled as beaten off. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		Hero hero = Dungeon.hero;
		int levelId = System.identityHashCode( level );
		Today t = takeStock( level );
		//yesterday's raiders still sitting in the parked store: forgotten once a day
		if (t.day != forgotDay || levelId != forgotLevel){
			forgotDay = t.day;
			forgotLevel = levelId;
			level.forgetStaleRaiders( t.day );
		}

		int w = level.width();
		int hsx = Math.floorDiv( level.worldX + hero.pos % w, WorldStructures.SECTOR );
		int hsy = Math.floorDiv( level.worldY + hero.pos / w, WorldStructures.SECTOR );
		for (WorldEvents.Event e : t.bySector.values()){
			final Raid raid = new Raid( level.worldSeed, e );
			if (Math.max( Math.abs( e.sx - hsx ), Math.abs( e.sy - hsy ) ) <= HEAR && !level.eventAnnounced( e.id )){
				level.markAnnounced( e );
				announce( level, raid );
			}
			int well = level.localCell( raid.cx, raid.cy );
			if (well == -1 || (OverworldLevel.heroDistance( level, well ) > raid.berth() + LIVE_MARGIN
					&& !wholeInWindow( level, raid ))) continue;
			int[] c = census( level, raid );
			if (c[0] == 0){
				if (band( level, raid ) > 0) letLoose( level, raid, well );
			} else if (c[1] == 0){
				//the last of them won over (charmed for good, corrupted): beaten off all the same
				saved( level, raid );
			}
		}
		LagMonitor.end( "OW raid", t0 );
	}

	//every player hears of it by the smoke, the way to go from where his own hero stands
	private static void announce( OverworldLevel level, Raid raid ){
		final String name = raid.name( level.worldSeed );
		final int near = raid.berth() + LIVE_MARGIN;
		level.tellParty( ( h, wx, wy ) -> {
			if (Math.max( Math.abs( raid.cx - wx ), Math.abs( raid.cy - wy ) ) <= near){
				return GLog.WARNING + Messages.get( RaidEvent.class, "smoke_here", name );
			}
			//past the streets' reach, always further than "here" (WorldEvents.STAR_NEAR)
			return GLog.WARNING + Messages.get( RaidEvent.class, "smoke", name,
					Messages.get( WorldEvents.class, "dir_" + WorldEvents.direction( raid.cx - wx, raid.cy - wy ) ) );
		} );
		if (level.liveScene()) SpatialSound.playPanned( Assets.Sounds.BURNING, 0f, 0.6f, 1f, level.panTowards( raid.cx ) );
	}

	//the band is out: the birds go up off the roofs, and whoever is in the streets hears it
	private static void letLoose( OverworldLevel level, Raid raid, int well ){
		OverworldCritters.noise( well );
		final String name = raid.name( level.worldSeed );
		final int near = raid.berth() + LIVE_MARGIN;
		level.tellParty( ( h, wx, wy ) -> Math.max( Math.abs( raid.cx - wx ), Math.abs( raid.cy - wy ) ) <= near
				? GLog.NEGATIVE + Messages.get( RaidEvent.class, "live", name ) : null );
		if (level.liveScene() && OverworldLevel.heroDistance( level, well ) <= near){
			SpatialSound.play( Assets.Sounds.CHALLENGE, well, 0.8f );
		}
	}

	//the whole village - its fence and a ring past it - lies inside the window
	private static boolean wholeInWindow( OverworldLevel level, Raid raid ){
		int r = raid.berth() + 2;
		return level.localCell( raid.cx - r, raid.cy - r ) != -1 && level.localCell( raid.cx + r, raid.cy + r ) != -1;
	}

	//today's band of a raid, in the window and parked: {all of them, those still fighting}
	static int[] census( OverworldLevel level, Raid raid ){
		int all = 0, hostile = 0;
		for (Mob m : level.mobs){
			if (!ofRaid( m, raid )) continue;
			all++;
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY) hostile++;
		}
		for (Mob m : level.parked()){
			if (!ofRaid( m, raid )) continue;
			all++;
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY) hostile++;
		}
		return new int[]{ all, hostile };
	}

	private static boolean ofRaid( Mob m, Raid raid ){
		return m instanceof Raider && ((Raider) m).sector == raid.sector && ((Raider) m).day == raid.day;
	}

	//the band comes out of the houses' doors, round the village from a hashed one - first the
	//doors no hero can see. in a town with a watch every other one goes for the guards. the
	//number put down
	static int band( OverworldLevel level, Raid raid ){
		long h = WorldEvents.mix( level.worldSeed ^ SALT_BAND, raid.sector, raid.day );
		int n = raid.raiders(), placed = 0, first = (int) Math.floorMod( h, (long) raid.houses );
		//the towns that keep a watch (OverworldLevel.populateSettlement)
		boolean guarded = raid.houses >= 9;
		for (int pass = 0; pass < 2 && placed < n; pass++){
			for (int i = 0; i < raid.houses && placed < n; i++){
				int k = (first + i) % raid.houses;
				int door = level.localCell( raid.doorX( k ), raid.doorY( k ) );
				if (door == -1) continue;
				int at = level.freeSpotWithin( door, 2 );
				if (at == -1 || (pass == 0 && NetManager.anyHeroSees( at ))) continue;
				Raider r = new Raider();
				r.sector = raid.sector;
				r.day = raid.day;
				r.cx = raid.cx;
				r.cy = raid.cy;
				r.role = guarded && placed % 2 == 1 ? Raider.FIGHTER : Raider.LOOTER;
				if (level.addMob( r, at )) placed++;
			}
		}
		return placed;
	}

	/** The raid is beaten off: settled for good (kept the favour's days), the fires go out, and the
	 *  village pays its thanks - the guard nearest the well speaks for it; where it keeps no watch
	 *  its elder does, or any of its folk. The purse and the prize drop at the feet of the hero of
	 *  the party nearest the well, or by the well when none of them is in the streets to take them. */
	static void saved( OverworldLevel level, Raid raid ){
		level.resolveEvent( raid.id, (raid.day + 1 + FAVOUR_DAYS) * WorldEvents.FC );
		long seed = level.worldSeed;
		final String name = raid.name( seed );
		final int reach = raid.berth() + LIVE_MARGIN;
		int well = level.localCell( raid.cx, raid.cy );
		Hero hero = nearestHero( level, raid );

		Mob speaker = null;
		int best = Integer.MAX_VALUE;
		for (Mob m : level.mobs){
			if (!(m instanceof OverworldGuard) || ((OverworldGuard) m).homeSector != raid.sector || well == -1) continue;
			int d = level.distance( m.pos, well );
			if (d < best){
				best = d;
				speaker = m;
			}
		}
		if (speaker == null){
			//an elder before anyone else, then the nearest to the hero
			for (Mob m : level.mobs){
				if (!(m instanceof Settler) || ((Settler) m).settlementKey() != raid.sector) continue;
				int d = level.distance( m.pos, hero.pos )
						+ (((Settler) m).role == VillageRoutine.Role.ELDER.ordinal() ? 0 : level.length());
				if (d < best){
					best = d;
					speaker = m;
				}
			}
		}
		if (speaker != null){
			speaker.yell( Messages.get( RaidEvent.class, "thanks", name ) );
			if (speaker.sprite != null) speaker.sprite.showStatus( CharSprite.POSITIVE, Messages.get( RaidEvent.class, "cheer" ) );
		}

		boolean near = within( level, hero, raid, reach );
		int at = hero.pos;
		if (!near && well != -1){
			int c = level.freeSpotWithin( well, 2 );
			if (c != -1) at = c;
		}
		at = dropCell( level, at );
		//a hamlet's 90 or so, a city's 600, a metropolis's a thousand and more
		int gold = Math.round( (60 + 15 * raid.houses) * Random.Float( 0.85f, 1.15f ) );
		Heap heap = level.drop( new Gold( gold ), at );
		if (heap.sprite != null) heap.sprite.drop();
		heap = level.drop( prize(), at );
		if (heap.sprite != null) heap.sprite.drop();
		//the word reaches every player: those in the streets are rewarded there and then
		level.tellParty( ( h, wx, wy ) -> GLog.POSITIVE + Messages.get( RaidEvent.class,
				Math.max( Math.abs( raid.cx - wx ), Math.abs( raid.cy - wy ) ) <= reach ? "reward" : "reward_well", name ) );
		if (level.liveScene()) Sample.INSTANCE.play( Assets.Sounds.GOLD );
	}

	/** A saved village's prize: a weapon, armour, wand or ring given the shop room's treatment
	 *  (ShopRoom) - no curse on it, none worked into a weapon's edge or an armour's weave, its
	 *  make known. It keeps any upgrade it rolled: it is a gift, not a shelf's stock. */
	static Item prize(){
		Item prize = Generator.randomUsingDefaults( Random.oneOf( Generator.Category.WEAPON, Generator.Category.ARMOR,
				Generator.Category.WAND, Generator.Category.RING ) );
		if (prize instanceof Weapon && ((Weapon) prize).hasCurseEnchant()) ((Weapon) prize).enchant( null );
		if (prize instanceof Armor && ((Armor) prize).hasCurseGlyph()) ((Armor) prize).inscribe( null );
		prize.cursed = false;
		prize.identify( false );
		return prize;
	}

	//the hero of the party nearest a raided village's well (the host's own on a tie or alone)
	private static Hero nearestHero( OverworldLevel level, Raid raid ){
		Hero best = Dungeon.hero;
		int w = level.width(), bestD = Integer.MAX_VALUE;
		if (best != null) bestD = Math.max( Math.abs( level.worldX + best.pos % w - raid.cx ), Math.abs( level.worldY + best.pos / w - raid.cy ) );
		if (NetManager.isHost()){
			for (Hero h : NetManager.getNetHeroes()){
				if (!h.isAlive() || h.atExit || h.pos < 0 || h.pos >= level.length()) continue;
				int d = Math.max( Math.abs( level.worldX + h.pos % w - raid.cx ), Math.abs( level.worldY + h.pos / w - raid.cy ) );
				if (d < bestD){
					bestD = d;
					best = h;
				}
			}
		}
		return best;
	}

	//is a hero within `reach` cells (Chebyshev) of a raided village's well?
	private static boolean within( OverworldLevel level, Hero h, Raid raid, int reach ){
		int w = level.width();
		return Math.max( Math.abs( level.worldX + h.pos % w - raid.cx ), Math.abs( level.worldY + h.pos / w - raid.cy ) ) <= reach;
	}

	// ------------------------------------------------------------ the raiders

	/** A raider's prowl: one of its village's doors in the window, picked at random; the well
	 *  failing that; -1 when neither is (Raider's wandering takes any spot then). */
	public static int prowlTarget( Raider r ){
		OverworldLevel ow = surface();
		if (ow == null || r.sector == Long.MIN_VALUE) return -1;
		int sx = (int)(r.sector >> 32), sy = (int) r.sector;
		if (WorldStructures.siteType( ow.worldSeed, sx, sy ) != WorldStructures.Site.VILLAGE) return -1;
		int[] layout = WorldStructures.settlementLayout( ow.worldSeed, sx, sy );
		int k = Random.Int( (layout.length - 1) / 2 );
		int door = ow.localCell( r.cx + WorldStructures.houseDoorDX( layout[1 + 2 * k], layout[2 + 2 * k] ),
				r.cy + WorldStructures.houseDoorDY( layout[1 + 2 * k], layout[2 + 2 * k] ) );
		return door != -1 ? door : ow.localCell( r.cx, r.cy );
	}

	/** The guard of the raided village a fighter goes for: the nearest it sees within ten cells, or null. */
	public static Char guardFor( Raider r ){
		Level level = Dungeon.level;
		if (level == null || r.fieldOfView == null || r.fieldOfView.length != level.length()) return null;
		Char best = null;
		int bestD = 11;
		for (Mob m : level.mobs){
			if (!(m instanceof OverworldGuard) || ((OverworldGuard) m).homeSector != r.sector || !r.fieldOfView[m.pos]) continue;
			int d = level.distance( r.pos, m.pos );
			if (d < bestD){
				bestD = d;
				best = m;
			}
		}
		return best;
	}

	/** A raider has fallen (Raider.die, after it left the level): the last of today's band still
	 *  fighting gone, the raid is beaten off. */
	public static void raiderDown( Raider r ){
		OverworldLevel ow = surface();
		WorldEvents.Event e = todays( ow, r.sector );
		if (e == null || e.startDay != r.day || ow.eventResolved( e.id )) return;
		Raid raid = new Raid( ow.worldSeed, e );
		if (census( ow, raid )[1] == 0) saved( ow, raid );
	}

	/** A raider makes off with the day (Raider.act): said once a raid, where a hero sees it go. */
	public static void withdrawing( Raider r ){
		long id = WorldEvents.idOf( WorldEvents.Type.RAID, (int)(r.sector >> 32), (int) r.sector, r.day );
		if (id == withdrawnId || !NetManager.anyHeroSees( r.pos )) return;
		withdrawnId = id;
		GLog.w( Messages.get( RaidEvent.class, "withdraw" ) );
	}

	// ------------------------------------------------------------ the fires

	/** The houses burning in today's raids about the window, as world cells {wx, wy} of their
	 *  middles: one, two in a town with a watch, picked from the lived-in houses by the raid's own
	 *  hash. Beaten off, they are out. The raids are the snapshot's where it answers for the
	 *  level; a co-op guest's mirror, which takes no stock, works them out afresh from the world
	 *  and the raids its host settled (OverworldLevel.raidsNow), so his screen shows the same
	 *  fires. Any thread. */
	public static ArrayList<int[]> burningHouses( OverworldLevel ow ){
		ArrayList<int[]> out = new ArrayList<>();
		Today t = today;
		for (WorldEvents.Event e : fresh( t, ow ) ? t.bySector.values() : ow.raidsNow()){
			if (ow.eventResolved( e.id )) continue;
			Raid raid = new Raid( ow.worldSeed, e );
			int pool = WorldStructures.populatedHouses( raid.houses );
			long h = WorldEvents.mix( ow.worldSeed ^ SALT_FIRE, raid.sector, raid.day );
			int h0 = (int) Math.floorMod( h, (long) pool );
			int h1 = (h0 + 1 + (int) Math.floorMod( h >>> 16, (long) Math.max( 1, pool - 1 ) )) % pool;
			int burning = raid.houses >= 9 ? 2 : 1;
			for (int k = 0; k < burning; k++){
				int hi = k == 0 ? h0 : h1;
				if (ow.localCell( raid.houseX( hi ), raid.houseY( hi ) ) != -1) out.add( new int[]{ raid.houseX( hi ), raid.houseY( hi ) } );
			}
		}
		return out;
	}

	// ------------------------------------------------------------ helpers

	/** Where something given or left may drop by a cell: the cell itself when no shop's goods lie
	 *  there, else the nearest open cell within three that holds none - never merged into a stall's
	 *  heap, where it could not be picked up and a keeper's flight would sweep it. */
	public static int dropCell( Level level, int near ){
		if (notForSale( level, near )) return near;
		int w = level.width(), x0 = near % w, y0 = near / w;
		for (int r = 1; r <= 3; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = x0 + dx, y = y0 + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int c = x + y * w;
					if (level.passable[c] && !level.pit[c] && notForSale( level, c )) return c;
				}
			}
		}
		return near;
	}

	private static boolean notForSale( Level level, int cell ){
		Heap h = level.heaps.get( cell );
		return h == null || h.type == Heap.Type.HEAP;
	}

	/** Debug (the raid scene): the world cell berth + 6 out from a village's well toward
	 *  (fx, fy) - inside the band's reach, just past the fence. */
	public static int[] edgeToward( long seed, int sx, int sy, int fx, int fy ){
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		int reach = WorldStructures.settlementLayout( seed, sx, sy )[0] + 6;
		float dx = fx - cx, dy = fy - cy;
		float len = (float) Math.sqrt( dx * dx + dy * dy );
		if (len < 1f){
			dx = 1f;
			dy = 0f;
			len = 1f;
		}
		return new int[]{ cx + Math.round( dx / len * reach ), cy + Math.round( dy / len * reach ) };
	}
}
