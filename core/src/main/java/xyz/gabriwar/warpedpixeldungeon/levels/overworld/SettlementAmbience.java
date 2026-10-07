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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.HearthLight;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * The settlements, lived in: smoke from the chimneys of the inhabited houses and the gnolls'
 * fires, and lamplight in the windows from dusk till bedtime (and from the night owls till
 * dawn) - for the houses within RADIUS cells of the hero, the town's own included. Pictures
 * only, nothing saved: SettlementLights decides what burns where and when.
 *
 * Once a hero turn (DayNightCycle.onHeroTurn, the host's, resting or not) the actor thread
 * works out what should be burning around him and, when that changed, posts the list to
 * the render thread, positions taken there and then. The render thread's queue keeps order,
 * so a list posted before a window slide is laid down before the slide moves it, and one
 * posted after uses the new origin: nothing jumps. The smoke and the lights live above the
 * fog (GameScene.overFogEmitter / effectOverFog), which the slide moves with the world.
 *
 * A network client runs no hero turn: its Mirror, among the surface's visuals, works the
 * same rules out on the render thread from its own window, the host's clock and weather.
 *
 * A house set alight in a raid (RaidEvent.burningHouses) smokes black over its roof, seen from
 * anywhere in the window, with flames licking under the smoke where the hero can see them. The
 * raid is the host's to keep; a co-op guest's Mirror works the same fires out from the world
 * and the raids the host settled, so every screen shows them.
 *
 * On the peaks the places of the mountains burn the same way (collectPeaks): the hermit's hut
 * keeps a village house's hours, a waystation's hearth never goes out and its chimney never
 * stops, and the hot springs steam.
 */
public final class SettlementAmbience {

	private SettlementAmbience(){}

	/** A house's lights and smoke are made within this many cells of the hero (Chebyshev; the
	 *  hero never stands within 32 of the window's edge)... */
	public static final int RADIUS = 22;
	//...and kept until they are this far, so a hero pacing at the edge never puts them out
	//and lights them over again
	static final int KEEP = RADIUS + 4;
	//a burning house's smoke is keyed apart from its chimney's, and pours this thick; its
	//flames lick this often
	static final long SOOT_KEY = 0x50075L;
	static final float SOOT_INTERVAL = 0.06f, FLAME_INTERVAL = 0.25f;

	static final class Light {
		final long key;
		final int kind;
		final float x, y, level;
		Light( long key, int kind, float x, float y, float level ){
			this.key = key;
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.level = level;
		}
	}

	static final class Smoke {
		final long key;
		final float x, y, w, h, interval;
		final int gateDX, gateDY;
		final boolean gated;
		//a house on fire: soot, and flames under it
		final boolean soot;
		//a hot spring's: steam
		final boolean steam;
		//a hearth burning under the chimney's smoke, its logs' rect {x, y, w, h} in scene pixels, or null
		final float[] flame;
		Smoke( long key, float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated ){
			this( key, x, y, w, h, interval, gateDX, gateDY, gated, false );
		}
		Smoke( long key, float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated,
				boolean soot ){
			this( key, x, y, w, h, interval, gateDX, gateDY, gated, soot, false, null );
		}
		Smoke( long key, float x, float y, float w, float h, float interval, int gateDX, int gateDY, boolean gated,
				boolean soot, boolean steam, float[] flame ){
			this.key = key;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.interval = interval;
			this.gateDX = gateDX;
			this.gateDY = gateDY;
			this.gated = gated;
			this.soot = soot;
			this.steam = steam;
			this.flame = flame;
		}
	}

	//actor thread only: what was last posted, for which scene (0 for none). a posted list
	//belongs to the render thread from then on and is never touched again
	private static int postedSceneId;
	private static ArrayList<Light> postedLights = new ArrayList<>();
	private static ArrayList<Smoke> postedSmoke = new ArrayList<>();
	private static HashSet<Long> postedKeys = new HashSet<>();

	//render thread only: what burns on screen, for which scene (keyed by the scene's identity,
	//never the scene itself: no dead scene is kept from the collector)
	private static int liveSceneId;
	private static final HashMap<Long, HearthLight> liveLights = new HashMap<>();
	private static final HashMap<Long, ChimneySmokeParticle.Column> liveSmoke = new HashMap<>();
	private static final HashMap<Long, Float> liveInterval = new HashMap<>();
	//the flames under each burning house's smoke, by the smoke's key
	private static final HashMap<Long, Emitter> liveFlames = new HashMap<>();
	//...and what was put out but has not yet faded: wanted back before it is gone, it is lit
	//again rather than doubled by a new one on the same cell
	private static final HashMap<Long, HearthLight> fadingLights = new HashMap<>();
	private static final HashMap<Long, ChimneySmokeParticle.Column> fadingSmoke = new HashMap<>();

	/** Once a turn of the host's hero, on the actor thread (DayNightCycle.onHeroTurn). */
	public static void onHeroTurn(){
		Object scene = Game.scene();
		if (!(Dungeon.level instanceof OverworldLevel) || Dungeon.hero == null || !(scene instanceof GameScene)){
			//off the world: whatever burns goes out with it
			if (postedSceneId != 0){
				postedSceneId = 0;
				postedLights = new ArrayList<>();
				postedSmoke = new ArrayList<>();
				postedKeys = new HashSet<>();
				Game.runOnRenderThread( SettlementAmbience::dropAll );
			}
			return;
		}
		final int sceneId = System.identityHashCode( scene );
		ArrayList<Light> lights = new ArrayList<>();
		ArrayList<Smoke> smoke = new ArrayList<>();
		HashSet<Long> keys = new HashSet<>();
		gather( (OverworldLevel) Dungeon.level, Dungeon.hero.pos, postedKeys, lights, smoke, keys );

		if (sceneId == postedSceneId && sameLights( lights, postedLights ) && sameSmoke( smoke, postedSmoke )) return;
		postedSceneId = sceneId;
		postedLights = lights;
		postedSmoke = smoke;
		postedKeys = keys;
		Game.runOnRenderThread( () -> apply( sceneId, smoke, lights ) );
	}

	//what burns around a hero on cell `pos` of a surface window, at this hour and weather
	private static void gather( OverworldLevel ow, int pos, HashSet<Long> live,
			ArrayList<Light> lights, ArrayList<Smoke> smoke, HashSet<Long> keys ){
		//the slices burn only their own places' lights (the surface's houses are not theirs): under
		//the ground the lanterns, fires and glowing mushrooms of the caves', above it the peaks' huts and fires
		if (ow.altitude() < 0){
			CaveSites.collectLights( ow, pos, live, lights, smoke, keys );
			return;
		}
		int w = ow.width();
		if (ow.altitude() > 0){
			long t = LagMonitor.begin();
			collectPeaks( ow.worldSeed(), ow.mountainSites(), ow.map, w, ow.height(), ow.worldX(), ow.worldY(),
					ow.worldX() + pos % w, ow.worldY() + pos / w, DayNightCycle.phase(), DayNightCycle.phaseProgress(),
					GameCalendar.season(), ClimateManager.localTemp(), ClimateManager.isRaining(), live, lights, smoke, keys );
			LagMonitor.end( "SettlementAmbience.peaks", t );
			return;
		}
		collect( ow.worldSeed(), ow.map, ow.pristine(), w, ow.height(), ow.worldX(), ow.worldY(),
				ow.worldX() + pos % w, ow.worldY() + pos / w,
				DayNightCycle.phase(), DayNightCycle.phaseProgress(), GameCalendar.season(),
				ClimateManager::surfaceTempIn, ClimateManager.isRaining(), WorldClock.night(),
				live, lights, smoke, keys );
		//the houses a raid set alight, anywhere in the window: the smoke rises from the roof and
		//is seen from afar, over the fog. it pours from the back wall's row along the shell's
		//three inner columns (SettlementLights: the 5x5 shell, back wall at middle - 2), never
		//from the middle cell - that is the room, open to view while the hero stands inside,
		//and the soot and flames would bury whoever is in it
		final int S = DungeonTilemap.SIZE;
		for (int[] house : RaidEvent.burningHouses( ow )){
			int c = ow.localCell( house[0] - 1, house[1] - 2 );
			if (c == -1) continue;
			smoke.add( new Smoke( OverworldLevel.worldKey( house[0], house[1] ) ^ SOOT_KEY, (c % w) * S, (c / w) * S - 4,
					3 * S, 8, SOOT_INTERVAL, 0, 0, false, true ) );
		}
	}

	/**
	 * What should burn around a hero standing on world cell (hwx, hwy) of a window (its
	 * live terrain map and its untouched terrain, w x h, origin ox, oy) at this hour and
	 * weather: the window lights and fire glows into `lights`, the smoke columns into
	 * `smoke`, in scene pixels of that window, and every key kept into `keys`. `tempIn`
	 * gives the open air's temperature in a biome (ClimateManager.surfaceTempIn): each
	 * settlement smokes by the air at its own well, never by the hero's. `live` holds the
	 * keys burning now, which stay until KEEP.
	 */
	static void collect( long seed, int[] map, int[] pristine, int w, int h, int ox, int oy, int hwx, int hwy,
			Phase phase, float p, GameCalendar.Season season, ToDoubleFunction<WorldModel.Biome> tempIn,
			boolean raining, int night, HashSet<Long> live, ArrayList<Light> lights, ArrayList<Smoke> smoke, HashSet<Long> keys ){
		final int S = DungeonTilemap.SIZE;
		final int[] cm = SettlementLights.CHIMNEY_MOUTH, hm = SettlementLights.HEARTH_MOUTH;

		//houses come settlement by settlement: each one's air is read once
		long airSector = Long.MIN_VALUE;
		float temp = 0f;
		for (SettlementLights.House house : SettlementLights.housesIn( seed, hwx - KEEP, hwy - KEEP, hwx + KEEP, hwy + KEEP )){
			if (!house.inhabited()) continue;
			long sector = WorldStructures.sectorOf( house.sx, house.sy );
			if (sector != airSector){
				airSector = sector;
				temp = (float) tempIn.applyAsDouble( WorldModel.biomeAt( seed,
						WorldStructures.siteX( seed, house.sx, house.sy ), WorldStructures.siteY( seed, house.sx, house.sy ) ) );
			}
			float hh = SettlementLights.houseHash01( seed, house.wx, house.wy );
			boolean owl = SettlementLights.nightOwl( seed, house.sx, house.sy, house.index,
					WorldStructures.populatedHouses( house.count ), night );
			float glow = SettlementLights.glowLevel( phase, p, hh, owl );
			float iv = SettlementLights.puffInterval( SettlementLights.smokeLevel( season, temp, phase, p, hh, owl ), raining );
			if (glow <= 0 && iv <= 0) continue;
			if (house.faction == WorldStructures.Faction.HUMAN){
				if (glow > 0){
					int kind = SettlementLights.VILLAGE_WINDOW_KINDS[SettlementLights.windowStyle( seed, house.wx, house.wy )];
					for (int[] o : SettlementLights.HOUSE_WINDOWS){
						int c = SettlementLights.glassCell( seed, map, w, h, ox, oy, house.wx + o[0], house.wy + o[1] );
						if (c == -1) continue;
						long key = OverworldLevel.worldKey( ox + c % w, oy + c / w );
						if (wanted( live, key, ox + c % w, oy + c / w, hwx, hwy, keys )){
							lights.add( new Light( key, kind, (c % w) * S, (c / w) * S, glow ) );
						}
					}
				}
				if (iv > 0){
					int c = SettlementLights.chimneyCell( seed, map, w, h, ox, oy, house.wx, house.wy );
					if (c != -1){
						int cwx = ox + c % w, cwy = oy + c / w;
						long key = OverworldLevel.worldKey( cwx, cwy );
						//shown while the house's front is seen: the middle of its front wall, two
						//rows below the house's centre (or while the stack itself is in sight)
						if (wanted( live, key, cwx, cwy, hwx, hwy, keys )){
							smoke.add( new Smoke( key, (c % w) * S + cm[0], (c / w) * S + cm[1], cm[2], cm[3], iv,
									house.wx - cwx, house.wy + 2 - cwy, true ) );
						}
					}
				}
			} else {
				int c = SettlementLights.hearthCell( seed, pristine, w, h, ox, oy, house );
				if (c == -1) continue;
				int cwx = ox + c % w, cwy = oy + c / w;
				long key = OverworldLevel.worldKey( cwx, cwy );
				if (!wanted( live, key, cwx, cwy, hwx, hwy, keys )) continue;
				if (glow > 0) lights.add( new Light( key, SettlementLights.KIND_HEARTH, (c % w) * S, (c / w) * S, glow ) );
				if (iv > 0) smoke.add( new Smoke( key, (c % w) * S + hm[0], (c / w) * S + hm[1], hm[2], hm[3], iv, 0, 0, true ) );
			}
		}

		//the town at the world's origin: always in sight and never fogged, so its smoke is ungated
		int tdx = Math.max( 0, Math.max( WorldStructures.TOWN_X0 - hwx, hwx - (WorldStructures.TOWN_X0 + WorldStructures.TOWN_SIZE - 1) ) );
		int tdy = Math.max( 0, Math.max( WorldStructures.TOWN_Y0 - hwy, hwy - (WorldStructures.TOWN_Y0 + WorldStructures.TOWN_SIZE - 1) ) );
		if (Math.max( tdx, tdy ) <= KEEP){
			for (int[] tw : SettlementLights.TOWN_WINDOWS){
				int wx = WorldStructures.townWorldX( tw[0] ), wy = WorldStructures.townWorldY( tw[0] );
				int x = wx - ox, y = wy - oy;
				if (x < 1 || x > w - 2 || y < 1 || y > h - 2) continue;
				float g = SettlementLights.townGlowLevel( phase, p,
						SettlementLights.houseHash01( seed, WorldStructures.townWorldX( tw[2] ), WorldStructures.townWorldY( tw[2] ) ),
						tw[0] == SettlementLights.TOWN_INN_WINDOW );
				long key = OverworldLevel.worldKey( wx, wy );
				if (g > 0 && wanted( live, key, wx, wy, hwx, hwy, keys )) lights.add( new Light( key, tw[1], x * S, y * S, g ) );
			}
			//the town's air, by its plaza's biome
			float townTemp = (float) tempIn.applyAsDouble( WorldModel.biomeAt( seed,
					WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) ) );
			for (int cell : SettlementLights.TOWN_CHIMNEYS){
				int wx = WorldStructures.townWorldX( cell ), wy = WorldStructures.townWorldY( cell );
				int x = wx - ox, y = wy - oy;
				if (x < 1 || x > w - 2 || y < 1 || y > h - 2) continue;
				//the inn's hearth is kept up all night, like a night owl's
				float iv = SettlementLights.puffInterval( SettlementLights.smokeLevel( season, townTemp, phase, p,
						SettlementLights.houseHash01( seed, wx, wy ), cell == SettlementLights.TOWN_INN_CHIMNEY ), raining );
				long key = OverworldLevel.worldKey( wx, wy );
				if (iv > 0 && wanted( live, key, wx, wy, hwx, hwy, keys )){
					smoke.add( new Smoke( key, x * S + cm[0], y * S + cm[1], cm[2], cm[3], iv, 0, 0, false ) );
				}
			}
		}
	}

	//a hot spring's steam is keyed apart from anything else on its cell
	static final long STEAM_KEY = 0x57EA3L;
	//...and pours this often
	static final float STEAM_INTERVAL = 0.35f;

	/**
	 * What burns round a hero on world cell (hwx, hwy) of a peak's window, from the window's places
	 * (MountainSites, as laid - never resolved here): the hermit's lamps and chimney by a village
	 * house's rules and hours (his cold is the slice's, tempC), each waystation's hearth glowing
	 * but by day and its chimney smoking always - the flames on the logs inside, the smoke out of the
	 * stack above the back wall, never in the room - and the steam over every hot spring, shown
	 * where the pool is seen. Package-private for the tests.
	 */
	static void collectPeaks( long seed, List<MountainSites.Site> sites, int[] map, int w, int h, int ox, int oy,
			int hwx, int hwy, Phase phase, float p, GameCalendar.Season season, float tempC, boolean raining,
			HashSet<Long> live, ArrayList<Light> lights, ArrayList<Smoke> smoke, HashSet<Long> keys ){
		final int S = DungeonTilemap.SIZE;
		final int[] cm = SettlementLights.CHIMNEY_MOUTH, hm = SettlementLights.HEARTH_MOUTH;
		for (MountainSites.Site site : sites){
			switch (site.kind){
				case HERMIT: {
					float hh = SettlementLights.houseHash01( seed, site.wx, site.wy );
					float glow = SettlementLights.glowLevel( phase, p, hh, false );
					float iv = SettlementLights.puffInterval( SettlementLights.smokeLevel( season, tempC, phase, p, hh, false ), raining );
					if (glow > 0){
						int kind = SettlementLights.VILLAGE_WINDOW_KINDS[SettlementLights.windowStyle( seed, site.wx, site.wy )];
						for (int[] o : SettlementLights.HOUSE_WINDOWS){
							int c = SettlementLights.glassCell( seed, map, w, h, ox, oy, site.wx + o[0], site.wy + o[1] );
							if (c == -1) continue;
							long key = OverworldLevel.worldKey( ox + c % w, oy + c / w );
							if (wanted( live, key, ox + c % w, oy + c / w, hwx, hwy, keys )){
								lights.add( new Light( key, kind, (c % w) * S, (c / w) * S, glow ) );
							}
						}
					}
					if (iv > 0){
						int c = SettlementLights.chimneyCell( seed, map, w, h, ox, oy, site.wx, site.wy );
						if (c == -1) break;
						int cwx = ox + c % w, cwy = oy + c / w;
						long key = OverworldLevel.worldKey( cwx, cwy );
						//shown while the hut's front is seen, as a village house's
						if (wanted( live, key, cwx, cwy, hwx, hwy, keys )){
							smoke.add( new Smoke( key, (c % w) * S + cm[0], (c / w) * S + cm[1], cm[2], cm[3], iv,
									site.wx - cwx, site.wy + 2 - cwy, true ) );
						}
					}
					break;
				}
				case PASS: {
					int[] hearth = MountainSites.hearthCell( seed, site );
					int hx = hearth[0] - ox, hy = hearth[1] - oy;
					if (hx < 1 || hy < 1 || hx > w - 2 || hy > h - 2 || map[hx + hy * w] != Terrain.EMBERS) break;
					long hkey = OverworldLevel.worldKey( hearth[0], hearth[1] );
					if (phase != Phase.DAY && wanted( live, hkey, hearth[0], hearth[1], hwx, hwy, keys )){
						lights.add( new Light( hkey, SettlementLights.KIND_HEARTH, hx * S, hy * S, 1f ) );
					}
					int c = MountainSites.chimneyCell( seed, site, map, w, h, ox, oy );
					if (c == -1) break;
					int cwx = ox + c % w, cwy = oy + c / w;
					long key = OverworldLevel.worldKey( cwx, cwy );
					if (wanted( live, key, cwx, cwy, hwx, hwy, keys )){
						smoke.add( new Smoke( key, (c % w) * S + cm[0], (c / w) * S + cm[1], cm[2], cm[3],
								SettlementLights.puffInterval( 1f, raining ), 0, 0, true, false, false,
								new float[]{ hx * S + hm[0], hy * S + hm[1], hm[2], hm[3] } ) );
					}
					break;
				}
				case SPRINGS: {
					int[] pool = MountainSites.poolCells( site );
					int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
					for (int i = 0; i < pool.length; i += 2){
						x0 = Math.min( x0, pool[i] );
						x1 = Math.max( x1, pool[i] );
					}
					long key = site.key ^ STEAM_KEY;
					if (!wanted( live, key, site.wx, site.wy, hwx, hwy, keys )) break;
					//off the water and the ring of warm ground round it, from its middle row: gated on
					//its own cell, so an unexplored spring is not given away by its steam
					smoke.add( new Smoke( key, (x0 - ox) * S, (site.wy - oy) * S + 4, (x1 - x0 + 1) * S, 6,
							STEAM_INTERVAL, 0, 0, true, false, true, null ) );
					break;
				}
				default:
			}
		}
	}

	//within RADIUS of the hero, or within KEEP and already burning; collects the keys kept
	static boolean wanted( HashSet<Long> live, long key, int wx, int wy, int hwx, int hwy, HashSet<Long> keys ){
		int d = Math.max( Math.abs( wx - hwx ), Math.abs( wy - hwy ) );
		if (d <= RADIUS || (d <= KEEP && live.contains( key ))){
			keys.add( key );
			return true;
		}
		return false;
	}

	//the same lights at the same strength (positions aside: a window slide moves what is
	//already on screen, and only what is new needs placing)
	private static boolean sameLights( List<Light> a, List<Light> b ){
		if (a.size() != b.size()) return false;
		for (int i = 0; i < a.size(); i++){
			Light x = a.get( i ), y = b.get( i );
			if (x.key != y.key || x.kind != y.kind || x.level != y.level) return false;
		}
		return true;
	}

	private static boolean sameSmoke( List<Smoke> a, List<Smoke> b ){
		if (a.size() != b.size()) return false;
		for (int i = 0; i < a.size(); i++){
			Smoke x = a.get( i ), y = b.get( i );
			if (x.key != y.key || x.interval != y.interval || x.gated != y.gated || x.soot != y.soot
					|| x.steam != y.steam || (x.flame == null) != (y.flame == null)
					|| x.gateDX != y.gateDX || x.gateDY != y.gateDY) return false;
		}
		return true;
	}

	//render thread: brings what burns on screen in line with a list
	private static void apply( int sceneId, List<Smoke> smoke, List<Light> lights ){
		//a list for a scene that is gone: the next turn posts for the new one
		if (System.identityHashCode( Game.scene() ) != sceneId) return;
		if (liveSceneId != sceneId){
			//the old scene took its visuals with it
			forgetAll();
			liveSceneId = sceneId;
		}
		//what has faded away since is forgotten before anything new is made: a dead column is
		//the scene's to recycle, and must not be lit again under a key it no longer answers to
		for (Iterator<HearthLight> it = fadingLights.values().iterator(); it.hasNext(); ){
			if (it.next().parent == null) it.remove();
		}
		for (Iterator<ChimneySmokeParticle.Column> it = fadingSmoke.values().iterator(); it.hasNext(); ){
			ChimneySmokeParticle.Column e = it.next();
			if (!e.exists || e.parent == null) it.remove();
		}

		HashSet<Long> want = new HashSet<>();
		for (Smoke s : smoke){
			want.add( s.key );
			ChimneySmokeParticle.Column e = liveSmoke.get( s.key );
			if (e == null || e.parent == null){
				e = fadingSmoke.remove( s.key );
				if (e != null){
					//still drifting off: it pours again where it stands
					e.pour( e.puffs(), s.interval );
				} else {
					e = ChimneySmokeParticle.column( s.x, s.y, s.w, s.h, s.interval, s.gateDX, s.gateDY, s.gated, s.soot, s.steam );
					if (e == null) continue;
				}
				liveSmoke.put( s.key, e );
				liveInterval.put( s.key, s.interval );
			} else if (liveInterval.get( s.key ) != s.interval){
				e.pour( e.puffs(), s.interval );
				liveInterval.put( s.key, s.interval );
			}
			//a fire's flames along the roof's back edge, under the fog; a hearth's on its logs
			if (s.soot || s.flame != null){
				Emitter f = liveFlames.get( s.key );
				if (f == null || f.parent == null){
					f = GameScene.emitter();
					if (f == null) continue;
					if (s.soot) f.pos( s.x, s.y + 4, s.w, DungeonTilemap.SIZE );
					else f.pos( s.flame[0], s.flame[1], s.flame[2], s.flame[3] );
					f.pour( FlameParticle.FACTORY, FLAME_INTERVAL );
					liveFlames.put( s.key, f );
				}
			}
		}
		//a fire put out: its flames die down with its smoke, and the emitter is the scene's again
		for (Iterator<Map.Entry<Long, Emitter>> it = liveFlames.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Emitter> e = it.next();
			if (want.contains( e.getKey() )) continue;
			if (e.getValue().parent != null) e.getValue().on = false;
			it.remove();
		}
		for (Iterator<Map.Entry<Long, ChimneySmokeParticle.Column>> it = liveSmoke.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, ChimneySmokeParticle.Column> e = it.next();
			if (want.contains( e.getKey() )) continue;
			//it dies once its last puff has thinned away, and the scene recycles it
			if (e.getValue().parent != null){
				e.getValue().on = false;
				fadingSmoke.put( e.getKey(), e.getValue() );
			}
			liveInterval.remove( e.getKey() );
			it.remove();
		}

		want.clear();
		for (Light l : lights){
			want.add( l.key );
			HearthLight v = liveLights.get( l.key );
			if (v == null || v.parent == null){
				v = fadingLights.remove( l.key );
				if (v == null){
					v = new HearthLight( l.kind, l.x, l.y );
					GameScene.effectOverFog( v.halo );
					GameScene.effectOverFog( v );
				}
				liveLights.put( l.key, v );
			}
			v.target( l.level );
		}
		for (Iterator<Map.Entry<Long, HearthLight>> it = liveLights.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, HearthLight> e = it.next();
			if (want.contains( e.getKey() )) continue;
			//fades, then erases itself and its halo
			if (e.getValue().parent != null){
				e.getValue().putOut();
				fadingLights.put( e.getKey(), e.getValue() );
			}
			it.remove();
		}
	}

	//render thread: moves everything burning or fading by (dx, dy) scene pixels
	private static void slideAll( float dx, float dy ){
		for (HearthLight l : liveLights.values()) l.slide( dx, dy );
		for (HearthLight l : fadingLights.values()) l.slide( dx, dy );
		for (ChimneySmokeParticle.Column e : liveSmoke.values()) e.slide( dx, dy );
		for (ChimneySmokeParticle.Column e : fadingSmoke.values()) e.slide( dx, dy );
		//a fire's flames: a waystation's hearth burns on for good, so they must follow its smoke
		for (Emitter f : liveFlames.values()){
			if (f.parent == null) continue;
			f.pos( f.x + dx, f.y + dy, f.width, f.height );
			for (Gizmo g : f.membersView()){
				if (g instanceof Visual){
					((Visual) g).x += dx;
					((Visual) g).y += dy;
				}
			}
		}
	}

	//render thread: the hero has left the surface
	private static void dropAll(){
		for (HearthLight l : liveLights.values()) takeOff( l );
		for (HearthLight l : fadingLights.values()) takeOff( l );
		for (ChimneySmokeParticle.Column e : liveSmoke.values()){
			if (e.parent != null) e.on = false;
		}
		for (Emitter f : liveFlames.values()){
			if (f.parent != null) f.on = false;
		}
		forgetAll();
		liveSceneId = 0;
	}

	//off the scene at once, its buffers freed - unless it is off already: destroyed with its
	//scene, or gone out by itself (a second destroy would free buffers some other image holds)
	private static void takeOff( HearthLight l ){
		if (l.halo.parent != null){
			l.halo.killAndErase();
			l.halo.destroy();
		}
		if (l.parent != null){
			l.killAndErase();
			l.destroy();
		}
	}

	private static void forgetAll(){
		liveLights.clear();
		liveSmoke.clear();
		liveInterval.clear();
		liveFlames.clear();
		fadingLights.clear();
		fadingSmoke.clear();
	}

	/**
	 * A network client's lights. Its world clock runs no hero turn (the host's actor thread
	 * does), so this, among the surface's visuals (OverworldLevel.addVisuals), works out what
	 * burns by the very same rules - from the mirror's own window, the host's clock and
	 * weather - whenever the turn or the hero's world cell changes, and lays it down at once:
	 * it is on the render thread already. A mirror re-labels its window without sliding the
	 * scene, so what burns is first moved with the ground by hand. On a host, or alone, it
	 * does nothing: onHeroTurn has it.
	 */
	public static final class Mirror extends Gizmo {

		private final OverworldLevel level;
		//the window origin what burns was placed in
		private int visX, visY;
		//when it was last worked out, and what it came to
		private int lastTurn, lastWX, lastWY;
		private ArrayList<Light> lights = new ArrayList<>();
		private ArrayList<Smoke> smoke = new ArrayList<>();
		private HashSet<Long> keys = new HashSet<>();

		public Mirror( OverworldLevel level ){
			this.level = level;
			visX = level.worldX();
			visY = level.worldY();
		}

		@Override
		public void update(){
			if (!NetManager.isNetClient() || Dungeon.level != level || Dungeon.hero == null) return;
			Object scene = Game.scene();
			if (!(scene instanceof GameScene)) return;
			int sceneId = System.identityHashCode( scene );
			int ox = level.worldX(), oy = level.worldY();
			if (ox != visX || oy != visY){
				//what is on screen stands where the old origin put it
				if (liveSceneId == sceneId) slideAll( (visX - ox) * DungeonTilemap.SIZE, (visY - oy) * DungeonTilemap.SIZE );
				visX = ox;
				visY = oy;
			}
			int pos = Dungeon.hero.pos, w = level.width();
			int hwx = ox + pos % w, hwy = oy + pos / w;
			if (liveSceneId == sceneId && Dungeon.cycleTurn == lastTurn && hwx == lastWX && hwy == lastWY) return;
			lastTurn = Dungeon.cycleTurn;
			lastWX = hwx;
			lastWY = hwy;

			long tm = LagMonitor.begin();
			ArrayList<Light> l = new ArrayList<>();
			ArrayList<Smoke> s = new ArrayList<>();
			HashSet<Long> k = new HashSet<>();
			gather( level, pos, keys, l, s, k );
			if (liveSceneId != sceneId || !sameLights( l, lights ) || !sameSmoke( s, smoke )) apply( sceneId, s, l );
			lights = l;
			smoke = s;
			keys = k;
			LagMonitor.end( "SettlementAmbience.Mirror", tm );
		}
	}
}
