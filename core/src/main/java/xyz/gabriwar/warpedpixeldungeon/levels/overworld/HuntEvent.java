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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A wolf pack runs down a deer at nightfall (WorldEvents.Type.HUNT). Every sector out in the
 * wilds has one hunting ground, a fixed spot on open woodland, meadow, plain or tundra away
 * from the town, the settlements and its own site (about a third of the sectors); on any night
 * one ground in ODDS (8) has a hunt. Which night is a pure function of the world seed, the
 * sector and the night (WorldClock.night); the event runs from that night's start to the
 * next's. A hero walking six hundred cells of the wilds a night meets one about every eight
 * nights (HuntEventTest measures it).
 *
 * As night falls the party hears of every hunt of the night within HEAR (1) sector of a hero:
 * a distant howl the way it lies, and a pin on the world map, so a hero can go and find it.
 * It starts when a hero of the party comes within TRIGGER (16) cells of the ground after dusk,
 * one hunt at a time: a deer or two on open ground by the spot and two or three wolves four
 * cells off to one side of them and behind - grey ones in the cold, brown elsewhere - each set
 * down out of every hero's sight where there is any within HIDE (6) cells, so nothing is seen
 * to come out of nothing, and the deer bolts across the hero's view and in toward him, into
 * it. A hero who sees it starting, or is by the ground, hears the howl; one who is not and has
 * not heard of it yet, a distant howl the way it lies (Assets.Sounds.HOWL, loud or faint). It
 * is pinned on the world map until it is over; one heard of that never began is over at dawn.
 *
 * The pack chases its quarry by scent and pays the hero no mind unless he crowds one of them or
 * his side draws blood: then the whole pack turns on him (HuntPack). With no wolf left chasing,
 * the deer bolt for the edge of the world or the trees and are gone. A deer brought down, the
 * pack feeds at the kill for a while (HuntPack.FEED_TURNS) and leaves meat behind (Deer.die).
 * The hunt is over once no beast carries it any more, and it winds down by itself at dawn, when
 * a beast of it leaves the window (OverworldLevel.park) or when every hero of the party is FAR
 * (48) cells from its ground: what a hero can see, or is within UNSEEN (21) cells of, is let go
 * as an ordinary beast (a wolf asleep or put to flight left so), the rest slips away.
 * Nothing of it is saved but the beasts' tags and the event's settled id.
 */
public final class HuntEvent {

	private HuntEvent(){}

	static final int ODDS = 8;
	//sectors: a hunt this near a hero's sector is heard of (and pinned) as night falls
	static final int HEAR = 1;
	//cells: the hunt starts when a hero of the party comes this close to its ground
	static final int TRIGGER = 16;
	//cells: a hunt every hero of the party has walked this far from is over
	static final int FAR = 48;
	//cells: a beast of a hunt that is over slips away only this far from every hero and out of
	//their sight (the surface's sight and a cell: a hero's field of view is a step stale here)
	static final int UNSEEN = 21;
	//a hunting ground keeps this far from the town and from its sector's site
	static final int TOWN_CLEAR = WorldModel.TOWN_IN + 24, SITE_CLEAR = 10;
	//the wolves come in this far to the side of the deer
	static final int FLANK = 4;
	//cells: how far from its mark a beast of the hunt may be set down to be out of every hero's sight
	static final int HIDE = 6;
	static final EnumSet<WorldModel.Biome> GROUNDS = EnumSet.of( WorldModel.Biome.FOREST, WorldModel.Biome.PLAINS,
			WorldModel.Biome.MEADOW, WorldModel.Biome.TUNDRA );

	private static final long SALT_GROUND = 0x6A0DL, SALT_NIGHT = 0x4A7E5L, SALT_PACK = 0x4A7FACL;

	// ------------------------------------------------------------ the schedule (pure)

	private static long groundHash( long seed, int sx, int sy ){
		return WorldEvents.mix( seed ^ SALT_GROUND, WorldStructures.sectorOf( sx, sy ), 0 );
	}

	/** The world x of sector (sx, sy)'s hunting ground: twelve cells or more inside the sector. */
	public static int anchorX( long seed, int sx, int sy ){
		return sx * WorldStructures.SECTOR + 12 + (int) Math.floorMod( groundHash( seed, sx, sy ) >>> 8, (long)(WorldStructures.SECTOR - 24) );
	}

	public static int anchorY( long seed, int sx, int sy ){
		return sy * WorldStructures.SECTOR + 12 + (int) Math.floorMod( groundHash( seed, sx, sy ) >>> 24, (long)(WorldStructures.SECTOR - 24) );
	}

	//which sectors have a hunting ground: pure, so the answer is kept per seed
	private static volatile long groundSeed = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Long, Boolean> groundCache = new ConcurrentHashMap<>();

	/** Does sector (sx, sy) have a hunting ground: its spot on woodland, meadow, plain or tundra
	 *  (at the annual mean, so it never comes and goes with the seasons), clear of the town, of
	 *  every settlement's berth and of the sector's own site. */
	public static boolean ground( long seed, int sx, int sy ){
		if (seed != groundSeed){
			synchronized (groundCache){
				if (seed != groundSeed){
					groundCache.clear();
					groundSeed = seed;
				}
			}
		}
		long key = WorldStructures.sectorOf( sx, sy );
		Boolean known = groundCache.get( key );
		if (known != null) return known;
		int ax = anchorX( seed, sx, sy ), ay = anchorY( seed, sx, sy );
		boolean ok = Math.max( Math.abs( ax ), Math.abs( ay ) ) >= TOWN_CLEAR
				&& GROUNDS.contains( WorldModel.baseBiomeAt( seed, ax, ay ) )
				&& !OverworldFauna.nearSettlement( seed, ax, ay );
		if (ok && WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.NONE){
			ok = Math.max( Math.abs( WorldStructures.siteX( seed, sx, sy ) - ax ),
					Math.abs( WorldStructures.siteY( seed, sx, sy ) - ay ) ) > SITE_CLEAR;
		}
		groundCache.put( key, ok );
		return ok;
	}

	/** Is there a hunt on sector (sx, sy)'s ground on this night (WorldClock.night)? */
	public static boolean scheduled( long seed, int sx, int sy, int night ){
		return Math.floorMod( WorldEvents.mix( seed ^ SALT_NIGHT, WorldStructures.sectorOf( sx, sy ), night ) >>> 8, (long) ODDS ) == 0
				&& ground( seed, sx, sy );
	}

	/** The hunt on sector (sx, sy)'s ground on a night, or null: from that night's start to the
	 *  next's (WorldClock.nightStart), anchored on the ground (WorldEvents.eventsNear). */
	public static WorldEvents.Event huntEvent( long seed, int sx, int sy, int night ){
		if (!scheduled( seed, sx, sy, night )) return null;
		return new WorldEvents.Event( WorldEvents.Type.HUNT, sx, sy, night,
				WorldClock.nightStart( night ), WorldClock.nightStart( night + 1 ),
				anchorX( seed, sx, sy ), anchorY( seed, sx, sy ), WorldEvents.idOf( WorldEvents.Type.HUNT, sx, sy, night ) );
	}

	/** The hours a pack hunts: dusk and the dark. */
	public static boolean night(){
		DayNightCycle.Phase p = DayNightCycle.phase();
		return p == DayNightCycle.Phase.DUSK || p == DayNightCycle.Phase.NIGHT;
	}

	// ------------------------------------------------------------ the hero's step

	//a hunt's escape said once (not bundled: a reload may say it again, harmlessly)
	private static long escapeSaid = Long.MIN_VALUE;

	/** The host hero's step on the surface, or a turn he stood through (OverworldLevel.tickEvents,
	 *  after the raids): the hunts running wound down when their time is up, the ones over
	 *  settled, a new one started on a ground a hero of the party comes near after dusk, and the
	 *  night's others heard of from afar. Actor thread. */
	public static void onHeroStep( OverworldLevel level ){
		long t0 = LagMonitor.begin();
		boolean night = night();
		int nightNow = WorldClock.night();
		int turn = level.eventTurn();
		//the host runs the hunts, by wherever any of the party stands
		ArrayList<Hero> party = OverworldLevel.heroesOn( level );

		//the hunts running in the window, and those to wind down: their night over, settled (a
		//beast of theirs left the window), or their ground far behind every hero
		HashSet<Long> running = new HashSet<>(), ending = new HashSet<>();
		for (Mob m : level.mobs){
			HuntPack p = m.buff( HuntPack.class );
			if (p == null) continue;
			//won over by the hero's side: his now, and out of the hunt
			if (m.alignment == Char.Alignment.ALLY){
				p.detach();
				continue;
			}
			running.add( p.hunt );
			if (!night || p.day != nightNow || level.eventResolved( p.hunt )
					|| distance( level, nearest( level, party, p.ax, p.ay ), p.ax, p.ay ) > FAR) ending.add( p.hunt );
		}
		if (!ending.isEmpty()){
			for (Mob m : level.mobs.toArray( new Mob[0] )){
				HuntPack p = m.buff( HuntPack.class );
				if (p == null || !ending.contains( p.hunt )) continue;
				//what a hero can see, or nearly, is let go as an ordinary beast (the hour's cull takes
				//it in its turn); the rest slips away unseen. never destroy(): a wolf's pays the hero
				//for a kill. only the chase is called off: a wolf asleep (a lullaby's sleep lasts only
				//while its state says so), afraid or at ease is left as it is
				if (NetManager.anyHeroSees( m.pos ) || OverworldLevel.heroDistance( level, m.pos ) <= UNSEEN){
					p.detach();
					if (HuntPack.isWolf( m ) && !p.defending && m.state == m.HUNTING) m.state = m.WANDERING;
				} else {
					OverworldLevel.vanish( level, m );
				}
			}
			running.removeAll( ending );
		}

		//a hunt is over once no beast carries it: the pack fed and gone its way, the deer away,
		//every beast of it killed or let go. one heard of that never began is over with the night
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type != WorldEvents.Type.HUNT || level.eventResolved( e.id )) continue;
			if (level.eventBegun( e.id ) ? !running.contains( e.id ) : !night && level.eventAnnounced( e.id )){
				level.resolveEvent( e.id, e.endTurn );
			}
		}

		//a new hunt: one at a time, after dusk, on a ground a hero has come near
		if (night && running.isEmpty()){
			for (WorldEvents.Event e : level.nearEvents()){
				if (e.type != WorldEvents.Type.HUNT || !e.activeAt( turn )
						|| level.eventResolved( e.id ) || level.eventBegun( e.id )) continue;
				Hero near = nearest( level, party, e.wx, e.wy );
				if (distance( level, near, e.wx, e.wy ) > TRIGGER) continue;
				int at = level.localCell( e.wx, e.wy );
				if (at == -1) continue;
				//no open ground for it here: the night passes quietly
				if (!spawn( level, e, at, near.pos, nightNow )) level.resolveEvent( e.id, e.endTurn );
				break;
			}
		}

		//the night's hunts heard of from afar as it falls: pinned, so a hero can go and find one
		if (night) hearHunts( level, party, turn );
		LagMonitor.end( "OW hunt", t0 );
	}

	//every hunt of the night on a ground within HEAR sectors of a hero of the party, not heard of
	//yet: pinned on the map, and a distant howl to each hero in range the way it lies (close
	//by, just the howl). the pack comes out only once a hero gets within TRIGGER of the ground
	private static void hearHunts( OverworldLevel level, ArrayList<Hero> party, int turn ){
		for (WorldEvents.Event e : level.nearEvents()){
			if (e.type != WorldEvents.Type.HUNT || !e.activeAt( turn )
					|| level.eventResolved( e.id ) || level.eventAnnounced( e.id )) continue;
			boolean heard = false;
			for (Hero h : party) heard |= hears( level, h, e );
			if (!heard) continue;
			level.markAnnounced( e );
			level.tellParty( ( hero, wx, wy ) -> {
				if (!hears( level, hero, e )) return null;
				String dir = WorldEvents.direction( e.wx - wx, e.wy - wy );
				if (dir.equals( "here" )) return GLog.WARNING + Messages.get( HuntEvent.class, "heard_near" );
				return GLog.WARNING + Messages.get( HuntEvent.class, "start_far", Messages.get( WorldEvents.class, "dir_" + dir ) );
			} );
			if (level.liveScene() && hears( level, Dungeon.hero, e )) Sample.INSTANCE.play( Assets.Sounds.HOWL, 0.35f );
		}
	}

	//is a hero's sector within HEAR of the hunt's ground's?
	private static boolean hears( OverworldLevel level, Hero h, WorldEvents.Event e ){
		if (h == null || h.pos < 0 || h.pos >= level.length()) return false;
		int w = level.width();
		int hsx = Math.floorDiv( level.worldX + h.pos % w, WorldStructures.SECTOR );
		int hsy = Math.floorDiv( level.worldY + h.pos / w, WorldStructures.SECTOR );
		return WorldEvents.within( e, hsx, hsy, HEAR );
	}

	//the hero of the party nearest a world cell, or null for none
	private static Hero nearest( OverworldLevel level, ArrayList<Hero> party, int wx, int wy ){
		Hero best = null;
		int bestD = Integer.MAX_VALUE;
		for (Hero h : party){
			int d = distance( level, h, wx, wy );
			if (d < bestD){
				bestD = d;
				best = h;
			}
		}
		return best;
	}

	//cells (Chebyshev) from a hero to a world cell; Integer.MAX_VALUE for no hero
	private static int distance( OverworldLevel level, Hero h, int wx, int wy ){
		if (h == null) return Integer.MAX_VALUE;
		int w = level.width();
		return Math.max( Math.abs( level.worldX + h.pos % w - wx ), Math.abs( level.worldY + h.pos / w - wy ) );
	}

	//the deer on open ground by the hunting ground, the pack off to one side of them and behind,
	//all out of sight where they can be, loosed at them; heard of by every player. heroPos: the
	//hero who came near. false when there was no room for it
	private static boolean spawn( OverworldLevel level, WorldEvents.Event e, int at, int heroPos, int night ){
		int w = level.width();
		int d0 = hiddenGround( level, at, HIDE, 3 );
		if (d0 == -1) return false;
		long h = WorldEvents.mix( level.worldSeed ^ SALT_PACK, e.id, 0 );
		//the pack comes in from the side and from beyond the deer: it bolts away from them, across
		//the hero's view and in toward him - into his sight, from out of it
		float vx = d0 % w - heroPos % w, vy = d0 / w - heroPos / w;
		if (vx == 0 && vy == 0) vx = 1;
		float px = -vy, py = vx;
		if ((h & 1L) != 0){
			px = -px;
			py = -py;
		}
		px += vx;
		py += vy;
		float len = (float) Math.sqrt( px * px + py * py );
		int fx = Math.max( 3, Math.min( w - 4, d0 % w + Math.round( FLANK * px / len ) ) );
		int fy = Math.max( 3, Math.min( level.height() - 4, d0 / w + Math.round( FLANK * py / len ) ) );
		int w0 = hiddenGround( level, fx + fy * w, HIDE, 2 );
		if (w0 == -1) return false;

		WorldModel.Biome b = level.biomeAtCell( d0 );
		//grey wolves in the cold, brown ones elsewhere
		boolean cold = level.frozenAt( d0 ) || b == WorldModel.Biome.TUNDRA || b == WorldModel.Biome.SNOWFIELD
				|| GameCalendar.season() == GameCalendar.Season.WINTER;
		int deer = 1 + (int)((h >>> 40) & 1L), wolves = 2 + (int)((h >>> 41) & 1L);

		ArrayList<Deer> herd = new ArrayList<>();
		for (int i = 0; i < deer; i++){
			int c = i == 0 ? d0 : hiddenGround( level, d0 + PathFinder.NEIGHBOURS8[(i * 3) % 8], 2, 1 );
			if (c == -1) continue;
			Deer d = new Deer();
			d.setDoe( ((h >>> (43 + i)) & 1L) != 0 );
			tag( d, e, night );
			if (level.addMob( d, c )) herd.add( d );
		}
		if (herd.isEmpty()) return false;
		int placed = 0;
		for (int j = 0; j < wolves; j++){
			int c = j == 0 ? w0 : hiddenGround( level, w0 + PathFinder.NEIGHBOURS8[(j * 3 + 1) % 8], 2, 2 );
			if (c == -1) continue;
			Mob wolf = cold ? new GrayWolf() : new BrownWolf();
			tag( wolf, e, night );
			if (!level.addMob( wolf, c )) continue;
			HuntPack.loose( wolf, herd.get( j % herd.size() ) );
			if (wolf.sprite != null) wolf.sprite.showAlert();
			placed++;
		}
		if (placed == 0){
			for (Deer d : herd) OverworldLevel.vanish( level, d );
			return false;
		}

		//heard of from afar already (hearHunts), it is told again only to who sees it start
		final boolean heardOf = level.eventAnnounced( e.id );
		level.markAnnounced( e );
		level.markBegun( e );
		OverworldCritters.noise( d0 );
		final boolean dusk = DayNightCycle.phase() == DayNightCycle.Phase.DUSK;
		final int seenDeer = d0, seenPack = w0;
		level.tellParty( ( hero, wx, wy ) -> {
			String dir = WorldEvents.direction( e.wx - wx, e.wy - wy );
			if (level.heroSees( hero, seenDeer ) || level.heroSees( hero, seenPack ) || dir.equals( "here" )){
				return GLog.WARNING + Messages.get( HuntEvent.class, dusk ? "start_dusk" : "start_night" );
			}
			if (heardOf) return null;
			return GLog.WARNING + Messages.get( HuntEvent.class, "start_far", Messages.get( WorldEvents.class, "dir_" + dir ) );
		} );
		//and the howl itself on the host's own speakers: close by, or carried from afar
		if (level.liveScene()){
			Hero host = Dungeon.hero;
			boolean close = level.heroSees( host, seenDeer ) || level.heroSees( host, seenPack )
					|| distance( level, host, e.wx, e.wy ) <= WorldEvents.STAR_NEAR;
			Sample.INSTANCE.play( Assets.Sounds.HOWL, close ? 0.8f : 0.35f );
		}
		return true;
	}

	//a beast joins a hunt
	private static void tag( Mob m, WorldEvents.Event e, int night ){
		HuntPack p = Buff.affect( m, HuntPack.class );
		p.hunt = e.id;
		p.day = night;
		p.ax = e.wx;
		p.ay = e.wy;
	}

	//open ground for a beast of the hunt: the cell, else the nearest within `radius`, three cells
	//clear of the window's edge, dry, never a pit or harm, nobody on it. -1 for none
	static int openGround( OverworldLevel level, int cell, int radius ){
		return openGround( level, cell, radius, false );
	}

	//the same out of every hero's sight within `hide` cells, so a beast of the hunt is never seen
	//to come out of nothing; failing that (an open plain in the last of the light, the debug
	//scene's clear view) the nearest open ground within `radius`
	static int hiddenGround( OverworldLevel level, int cell, int hide, int radius ){
		int c = openGround( level, cell, hide, true );
		return c != -1 ? c : openGround( level, cell, radius, false );
	}

	//no hero sees the cell, nor any cell next to it: a hero's field of view is a step stale here
	private static boolean unseen( int cell ){
		if (NetManager.anyHeroSees( cell )) return false;
		for (int n : PathFinder.NEIGHBOURS8){
			if (NetManager.anyHeroSees( cell + n )) return false;
		}
		return true;
	}

	private static int openGround( OverworldLevel level, int cell, int radius, boolean hidden ){
		int w = level.width(), x0 = cell % w, y0 = cell / w;
		for (int r = 0; r <= radius; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = x0 + dx, y = y0 + dy;
					if (x < 3 || y < 3 || x > w - 4 || y > level.height() - 4) continue;
					int c = x + y * w;
					if (level.passable[c] && !level.water[c] && !level.pit[c] && !level.avoid[c] && !level.occupied( c )
							&& (!hidden || unseen( c ))) return c;
				}
			}
		}
		return -1;
	}

	// ------------------------------------------------------------ how it ends

	/** Is any wolf of this hunt still after the deer (not turned on the hero) on the level? */
	public static boolean packHunting( long id ){
		if (Dungeon.level == null) return false;
		for (Mob m : Dungeon.level.mobs){
			if (!HuntPack.isWolf( m ) || !m.isAlive()) continue;
			HuntPack p = m.buff( HuntPack.class );
			if (p != null && p.hunt == id && !p.defending) return true;
		}
		return false;
	}

	/** A deer of the hunt finds no wolf on its heels any more (Deer.act): it makes for safety,
	 *  said once a hunt where a hero sees it bolt. */
	public static void packBroken( long id, Deer d ){
		if (id == escapeSaid || !NetManager.anyHeroSees( d.pos )) return;
		escapeSaid = id;
		GLog.p( Messages.get( HuntEvent.class, "escape" ) );
	}

	/** A deer of the hunt is down (Deer.die): the last of them, and every wolf of the pack still
	 *  hunting settles by the kill to feed. */
	public static void deerDown( Deer d, long id, boolean byPack ){
		if (id == Long.MIN_VALUE || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Deer && m != d && m.isAlive()){
				HuntPack p = m.buff( HuntPack.class );
				if (p != null && p.hunt == id) return;
			}
		}
		int wx = HuntPack.worldX( d.pos ), wy = HuntPack.worldY( d.pos );
		boolean fed = false;
		for (Mob m : Dungeon.level.mobs){
			if (!HuntPack.isWolf( m ) || !m.isAlive()) continue;
			HuntPack p = m.buff( HuntPack.class );
			if (p == null || p.hunt != id || p.defending) continue;
			p.feedAt( wx, wy );
			fed = true;
		}
		if (fed && byPack && NetManager.anyHeroSees( d.pos )) GLog.i( Messages.get( HuntEvent.class, "kill" ) );
	}

	/** A beast of a hunt left the window (OverworldLevel.park drops it): the chase is over, and it
	 *  is settled on that level - never run again. */
	public static void lost( OverworldLevel level, HuntPack p ){
		level.resolveEvent( p.hunt, WorldClock.nightStart( p.day + 1 ) );
	}

	/** Debug (the hunt scene): a hunting ground `dist` cells from the hero in the first of the
	 *  eight directions with open ground about it, as a world cell; null when there is none. */
	public static int[] debugGround( OverworldLevel level, int heroPos, int dist ){
		int[] dx = { 1, 1, 0, -1, -1, -1, 0, 1 }, dy = { 0, 1, 1, 1, 0, -1, -1, -1 };
		int w = level.width(), hx = heroPos % w, hy = heroPos / w;
		for (int i = 0; i < 8; i++){
			int x = hx + dx[i] * dist, y = hy + dy[i] * dist;
			if (x < 3 || y < 3 || x > w - 4 || y > level.height() - 4) continue;
			int c = openGround( level, x + y * w, 2 );
			if (c != -1) return new int[]{ level.worldX + c % w, level.worldY + c / w };
		}
		return null;
	}
}
