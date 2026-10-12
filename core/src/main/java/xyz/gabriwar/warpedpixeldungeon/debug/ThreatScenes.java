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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.AwaySettlers;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.HomeThreats;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.Notice;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatSettler;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHomeLedger;
import com.watabou.noosa.Game;

/**
 * The threats' debug scenes (the Scenes menu's "Homestead" group), each a home laid round the hero
 * through the Homestead API, its "hearthstone" a home of HomesteadWorld on a cell (the stone
 * itself is the travel part's: none stands there):
 *
 * homestead-wolf-siege: a winter night in a forest (or the snow), a log house with a barred door
 * inside a fence ring with a gate, the hero inside, and the night's siege set going: the pack
 * howls at the edge, circles four to eight cells off, bites at the fences and the gate and never
 * at the walls or the door; step out of the ring and they hunt you. They leave at dawn.
 *
 * homestead-raid: a night near the town: a stone house with a barred door and the richer chest,
 * a plank hut with a plain door and a poorer one, a fire in each (their light counts), the notice
 * above the raid line, and a raid set going: the band makes for the stone house's chest, breaks
 * in by the weakest way, grabs and runs; cut them down to get it back.
 *
 * homestead-away-week: a plank hut with a chest of gold and a fence, the home last visited seven
 * days ago: the absence is settled at once (a raid empties the chest, never more than one) and the
 * ledger opens with it.
 *
 * homestead-defences: a night near the town, a watchtower, an alarm bell, a row of spike traps and
 * two rats past the traps coming for the hero: climb the tower, rest to hear the bell ring as
 * they come onto the home's ground, and watch the spikes bite them and not you.
 *
 * homestead-bell-night: dusk near the town, a plank house with a wooden bed and a rich chest, an
 * alarm bell outside its door, and a home whose coming night brings a raid: sleep in the bed until
 * dawn and the bell wakes you in the dark as the bandits come (AlarmBell.sleepFor).
 */
final class ThreatScenes {

	private ThreatScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new WolfSiegeScene(),
			new RaidScene(),
			new AwayWeekScene(),
			new DefencesScene(),
			new BellNightScene()
	};

	// ----------------------------------------------------------------- helpers

	//the surface, south of the town's streets: true when the hero is there, else he is sent and
	//the scene runs again on arrival
	private static boolean onSurface( DebugScenes.Scene scene, boolean[] travelling ){
		if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0){
			travelling[0] = false;
			return true;
		}
		if (travelling[0]){
			travelling[0] = false;
			GLog.w( "Scene: could not reach the surface" );
			return false;
		}
		travelling[0] = true;
		DebugScenes.travelThen( scene, 0, WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) + 40 );
		return false;
	}

	//the slice the hero stands on, the host's own: null (and said why) when it is not one
	private static OverworldLevel slice(){
		if (!(Dungeon.level instanceof OverworldLevel)){
			GLog.w( "Scene: not on a world slice" );
			return null;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (ow.isMirror()){
			GLog.w( "Scene: only the host can build" );
			return null;
		}
		return ow;
	}

	/** The top-left window cell of the nearest open stretch w x h (and a cell round it) that keeps
	 *  two cells from the hero, -1 within 40 cells of none. */
	static int findGround( OverworldLevel ow, int heroPos, int w, int h ){
		int width = ow.width(), hx = heroPos % width, hy = heroPos / width;
		for (int r = 3; r <= 40; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (fits( ow, hx + dx, hy + dy, w, h, hx, hy )) return hx + dx + (hy + dy) * width;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int ox, int oy, int w, int h, int hx, int hy ){
		int width = ow.width();
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 2 || y < 2 || x >= width - 2 || y >= ow.height() - 2) return false;
				if (Math.abs( x - hx ) <= 1 && Math.abs( y - hy ) <= 1) return false;
				int c = x + y * width;
				int t = ow.map[c];
				if (!ow.passable[c] || ow.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
						|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || Terrain.homestead( t )
						|| ow.protectedForBuilding( c ) || Actor.findChar( c ) != null || ow.heaps.get( c ) != null){
					return false;
				}
			}
		}
		return true;
	}

	private static void build( OverworldLevel ow, int cell, String id, Hero hero ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), hero );
	}

	//a ring of `wall` w x h at o, `door` in the middle of its south side and `window` in the
	//middle of its north side, `floor` inside, `roof` over all of it: the door's cell
	private static int house( OverworldLevel ow, int o, int w, int h, String wall, String door, String window,
							 String floor, String roof, Hero hero ){
		int width = ow.width(), d = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * width;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge) build( ow, c, floor, hero );
				else if (x == w / 2 && y == h - 1){
					build( ow, c, door, hero );
					d = c;
				} else if (x == w / 2 && y == 0 && window != null) build( ow, c, window, hero );
				else build( ow, c, wall, hero );
			}
		}
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * width, roof, hero );
		}
		return d;
	}

	//a home of HomesteadWorld on a window cell, visited now
	private static HomesteadWorld.Home home( OverworldLevel ow, int cell, String name ){
		int w = ow.width();
		HomesteadWorld.Home h = new HomesteadWorld.Home( WorldLayers.depthOf( ow.altitude() ),
				ow.worldX + cell % w, ow.worldY + cell / w, name, WorldClock.day() );
		h.visited();
		HomesteadWorld.addHome( h );
		return h;
	}

	//a chest on a cell with these things in it
	private static StorageChest chest( OverworldLevel ow, int cell, Item... things ){
		StorageChest c = new StorageChest();
		c.pos = cell;
		for (Item i : things) if (i != null) i.collect( c.storage );
		GameScene.add( c );
		return c;
	}

	//a camp fire's coals burning on a cell, for most of a night (a coal bed's 100 + the turns left,
	//CoalBed's own encoding: a camp fire burns 400)
	private static void fire( int cell ){
		GameScene.add( Blob.seed( cell, 100 + 400, CampFire.class ) );
	}

	//the home's notice worked out now, as the slice's next turn would
	private static String notice( OverworldLevel ow, HomesteadWorld.Home h ){
		Notice.Score s = Notice.refresh( ow, h );
		return s.total() + (s.aboveLine() ? " (above" : " (not above") + " the raid line of " + HomeThreats.RAID_LINE + ")";
	}

	// ----------------------------------------------------------------- the scenes

	private static final class WolfSiegeScene implements DebugScenes.Scene {
		//set while the hero is on his way to the woods
		boolean travelling;

		@Override public String id(){ return "homestead-wolf-siege"; }
		@Override public String title(){ return "Homestead: a wolf siege on a fenced log house, a winter night"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				arrive( hero );
				return;
			}
			if (!DebugScenes.walkClock( GameCalendar.Season.WINTER, DayNightCycle.Phase.NIGHT, 0.1f )){
				GLog.w( "Scene: a real-clock run keeps its own season and hour" );
			}
			final long seed = OverworldLevel.worldSeedOf( Dungeon.seed );
			final float shift = WorldModel.calendarShift();
			DebugScenes.search( "a forest (or the snow) near the origin",
					() -> DebugScenes.biomeGround( seed, shift, WorldModel.Biome.FOREST, WorldModel.Biome.SNOWFIELD, WorldModel.Biome.TUNDRA ),
					spot -> {
						if (spot == null){
							GLog.w( "Scene: no forest, snowfield or tundra near the origin in this world" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, spot[0], spot[1] );
					} );
		}

		private void arrive( Hero hero ){
			OverworldLevel ow = slice();
			if (ow == null) return;
			//the house 7x5 inside a fence ring three cells out: 15 x 13 in all
			int ring = findGround( ow, hero.pos, 15, 13 );
			if (ring == -1){
				GLog.w( "Scene: no open ground near the hero for the lodge; walk somewhere clearer" );
				return;
			}
			int w = ow.width();
			for (int y = 0; y < 13; y++){
				for (int x = 0; x < 15; x++){
					if (x != 0 && y != 0 && x != 14 && y != 12) continue;
					build( ow, ring + x + y * w, x == 7 && y == 12 ? "gate" : "fence", hero );
				}
			}
			int o = ring + 4 + 4 * w;
			house( ow, o, 7, 5, "wall_log", "door_barred", "window_log", "floor_plank", "roof_thatch", hero );
			int inside = o + 3 + 2 * w;
			HomesteadWorld.Home h = home( ow, inside, "Forest lodge" );
			ScrollOfTeleportation.appear( hero, o + 2 + 2 * w );
			Dungeon.observe();
			ThreatWorld.Event e = ThreatSlice.force( ow, h, ThreatWorld.Event.SIEGE );
			GLog.i( "Scene: a log house with a barred door in a ring of fences, a winter night; notice " + notice( ow, h ) + "." );
			GLog.i( "Scene: " + (e == null ? "no siege could start" : e.came + " wolves came")
					+ ". They circle 4 to 8 cells off, howl, and bite the fences and the gate, never the walls or the door."
					+ " Step outside the ring and they hunt you; they leave at dawn." );
		}
	}

	private static final class RaidScene implements DebugScenes.Scene {
		final boolean[] travelling = { false };

		@Override public String id(){ return "homestead-raid"; }
		@Override public String title(){ return "Homestead: bandits raid a rich home at night"; }

		@Override public void apply( Hero hero ){
			if (!onSurface( this, travelling )) return;
			OverworldLevel ow = slice();
			if (ow == null) return;
			DebugScenes.walkClock( null, DayNightCycle.Phase.NIGHT, 0.1f );
			//a stone house 7x5, two cells of ground, a plank hut 5x4
			int o = findGround( ow, hero.pos, 14, 9 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for the houses; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			house( ow, o, 7, 5, "wall_stone", "door_barred", "window_stone", "floor_stone", "roof_slate", hero );
			int hut = o + 9;
			house( ow, hut, 5, 4, "wall_plank", "door", null, "floor_plank", "roof_plank", hero );
			chest( ow, o + 3 + 2 * w, new Gold( 3500 ), Generator.random( Generator.Category.WAND ),
					Generator.random( Generator.Category.RING ), Generator.random( Generator.Category.ARMOR ) );
			chest( ow, hut + 2 + w, new Gold( 300 ), Generator.random( Generator.Category.POTION ) );
			fire( o + 1 + w );
			fire( hut + 1 + 2 * w );
			HomesteadWorld.Home h = home( ow, o + 8 + 2 * w, "Stone farm" );
			ScrollOfTeleportation.appear( hero, o + 8 + 7 * w );
			Dungeon.observe();
			String n = notice( ow, h );
			ThreatWorld.Event e = ThreatSlice.force( ow, h, ThreatWorld.Event.RAID );
			GLog.i( "Scene: a stone house (barred door, the rich chest) and a plank hut (plain door, a poorer chest), a fire in each; notice " + n + "." );
			GLog.i( "Scene: " + (e == null ? "no raid could start" : e.came + " bandits came")
					+ ". They go for the richest chest and break in by the weakest way; each grabs up to 4 things and runs. Kill them to get it back." );
		}
	}

	private static final class AwayWeekScene implements DebugScenes.Scene {
		final boolean[] travelling = { false };

		@Override public String id(){ return "homestead-away-week"; }
		@Override public String title(){ return "Homestead: come home after a week away"; }

		@Override public void apply( Hero hero ){
			if (!onSurface( this, travelling )) return;
			OverworldLevel ow = slice();
			if (ow == null) return;
			int o = findGround( ow, hero.pos, 9, 7 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for the hut; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			int hut = o + 2 + 2 * w;
			house( ow, hut, 5, 4, "wall_plank", "door", null, "floor_plank", "roof_plank", hero );
			for (int x = 0; x < 9; x++) build( ow, o + x, x == 4 ? "gate" : "fence", hero );
			chest( ow, hut + 2 + w, new Gold( 4000 ), Generator.random( Generator.Category.RING ) );
			ScrollOfTeleportation.appear( hero, o + 4 + 6 * w );
			Dungeon.observe();

			//a hearthstone whose seven nights away bring a raid (each is pure: one of the cells
			//round the hut will)
			int now = WorldClock.turn();
			HomesteadWorld.Home h = null;
			for (int dy = 0; dy <= 6 && h == null; dy++){
				for (int dx = 0; dx <= 8 && h == null; dx++){
					HomesteadWorld.Home probe = new HomesteadWorld.Home( WorldLayers.depthOf( ow.altitude() ),
							ow.worldX + (o + dx) % w, ow.worldY + o / w + dy, "Riverside hut", WorldClock.day() - 7 );
					probe.lastVisitTurn = now - 7 * DayNightCycle.FULL_CYCLE;
					if (raidsWithin( ow, probe )) h = probe;
				}
			}
			if (h == null){
				GLog.w( "Scene: no raid falls on these seven nights anywhere round the hut; settling them anyway" );
				h = new HomesteadWorld.Home( WorldLayers.depthOf( ow.altitude() ), ow.worldX + (o + 4) % w,
						ow.worldY + o / w + 3, "Riverside hut", WorldClock.day() - 7 );
				h.lastVisitTurn = now - 7 * DayNightCycle.FULL_CYCLE;
			}
			HomesteadWorld.addHome( h );
			GLog.i( "Scene: a plank hut with a chest of gold behind a fence, last visited seven days ago; notice "
					+ Notice.of( ow, h, true ).total() + ". Coming back settles the week:" );
			//the hero is at the home: his coming back is settled now, as the next turn would
			AwaySettlers.visit( ow );
			final HomesteadWorld.Home home = h;
			Game.runOnRenderThread( () -> GameScene.show( new WndHomeLedger( home ) ) );
		}

		//does one of the nights of this absence bring a raid, at this notice?
		private static boolean raidsWithin( OverworldLevel ow, HomesteadWorld.Home h ){
			int notice = Notice.of( ow, h, true ).total();
			for (int night = ThreatSettler.firstNight( h.lastVisitTurn ); ThreatSettler.past( night ); night++){
				if (HomeThreats.raidNight( ow.worldSeed, h, night, notice )) return true;
			}
			return false;
		}
	}

	private static final class DefencesScene implements DebugScenes.Scene {
		final boolean[] travelling = { false };

		@Override public String id(){ return "homestead-defences"; }
		@Override public String title(){ return "Homestead: a watchtower, an alarm bell and spike traps"; }

		@Override public void apply( Hero hero ){
			if (!onSurface( this, travelling )) return;
			OverworldLevel ow = slice();
			if (ow == null) return;
			//by night: the rats unseen until they are near, and the tower's sight worth more
			DebugScenes.walkClock( null, DayNightCycle.Phase.NIGHT, 0.1f );
			//the tower, the bell and the hero's spot, a row of traps four cells south, the rats
			//coming from past the home's ground: 7 wide, 22 deep
			int o = findGround( ow, hero.pos, 7, 22 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for the defences; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			build( ow, o + 2 + w, "watchtower", hero );
			build( ow, o + 4 + w, "alarm_bell", hero );
			for (int x = 0; x < 7; x++) build( ow, o + x + 5 * w, ThreatSlice.SPIKE_PIECE, hero );
			HomesteadWorld.Home h = home( ow, o + 3, "Watch post" );
			ScrollOfTeleportation.appear( hero, o + 3 + w );
			Dungeon.observe();
			for (int k = 0; k < 2; k++){
				Mob rat = DebugScenes.spawn( Rat.class, o + 2 + 2 * k + 20 * w );
				if (rat == null) continue;
				rat.state = rat.HUNTING;
				rat.beckon( hero.pos );
			}
			GLog.i( "Scene: a watchtower (tap it to climb: you see 4 farther and spot wolves and bandits from 30 cells),"
					+ " an alarm bell and a row of spike traps; notice " + notice( ow, h ) + "." );
			GLog.i( "Scene: two rats are coming from past the home's ground. Rest (the search button, held) and the bell"
					+ " rings and wakes you as they come onto it; the spikes bite them, never you." );
		}
	}

	private static final class BellNightScene implements DebugScenes.Scene {
		final boolean[] travelling = { false };

		@Override public String id(){ return "homestead-bell-night"; }
		@Override public String title(){ return "Homestead: sleep through a raid night with an alarm bell"; }

		@Override public void apply( Hero hero ){
			if (!onSurface( this, travelling )) return;
			OverworldLevel ow = slice();
			if (ow == null) return;
			if (!DebugScenes.walkClock( null, DayNightCycle.Phase.DUSK, 0.5f )){
				GLog.w( "Scene: a real-clock run keeps its own hour" );
				return;
			}
			//a plank house 7x5, its door south, the bell two cells east of the door
			int o = findGround( ow, hero.pos, 10, 7 );
			if (o == -1){
				GLog.w( "Scene: no open ground near the hero for the house; walk somewhere flatter" );
				return;
			}
			int w = ow.width();
			int door = house( ow, o, 7, 5, "wall_plank", "door", "window_plank", "floor_plank", "roof_plank", hero );
			int bed = o + 1 + w;
			build( ow, bed, "bed_wood", hero );
			chest( ow, o + 5 + w, new Gold( 4000 ), Generator.random( Generator.Category.RING ) );
			build( ow, door + 2, "alarm_bell", hero );

			//a home round the house whose night tonight (the one a sleep from dusk runs into) brings
			//a raid at its notice: each cell rolls its own nights, purely
			int now = WorldClock.turn();
			int tonight = ThreatSettler.firstNight( now );
			HomesteadWorld.Home h = null;
			for (int dy = 0; dy <= 6 && h == null; dy++){
				for (int dx = 0; dx <= 9 && h == null; dx++){
					HomesteadWorld.Home probe = new HomesteadWorld.Home( WorldLayers.depthOf( ow.altitude() ),
							ow.worldX + (o + dx) % w, ow.worldY + o / w + dy, "Plank hall", WorldClock.day() );
					probe.visited();
					int notice = Notice.of( ow, probe, true ).total();
					if (HomeThreats.raidNight( ow.worldSeed, probe, tonight, notice )) h = probe;
				}
			}
			if (h == null){
				GLog.w( "Scene: no cell round the house brings a raid tonight in this world; try again tomorrow's dusk" );
				return;
			}
			HomesteadWorld.addHome( h );
			ScrollOfTeleportation.appear( hero, bed + 1 );
			Dungeon.observe();
			GLog.i( "Scene: a plank house with a wooden bed and a rich chest, an alarm bell by the door, at dusk; notice "
					+ notice( ow, h ) + ". Tonight bandits come." );
			GLog.i( "Scene: tap the bed and sleep until dawn: the bell rings and wakes you in the dark as they come."
					+ " Without the bell (demolish it first) you sleep through and wake to what they took." );
		}
	}
}
