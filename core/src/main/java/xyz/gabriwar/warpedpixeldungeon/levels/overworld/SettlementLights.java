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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;

import java.util.ArrayList;

/**
 * Where the settlements' fittings stand and when they burn: the windows and chimney of
 * every human house, the fire ring by every gnoll hut, the town's own glass and stacks,
 * and the hours their lamps and fires keep. Pure functions of the world seed, the window
 * and the hour - no GL, no game state - so the window worker dresses the fittings
 * (WindowGenerator.dress) from the very cells SettlementAmbience later lights, and the
 * tests pin both.
 */
public final class SettlementLights {

	private SettlementLights(){}

	// ------------------------------------------------------------ the art

	//measured on the art with PIL (tools/settlement_lights_gen.py checks every pixel):
	//a village house is WorldStructures' 5x5 stone shell - ONE shape, its door facing the
	//well - drawn by tiles_overworld (RAISED_WALL face = pixel rows 4-15 of the front wall,
	//WALL_OVERHANG lip = rows 8-15 of the cell above the back wall). its fittings are dress
	//tiles (tools/dress_sheet.py, row 72):
	//  windows: house offsets (-1,+2) and (+1,+2), a four-paned sash drawn by dress_sheet.py,
	//  style B with its shutters open; glass in four 3x3 panes at x 4-6/8-10, y 5-7/9-11
	//  chimney: town tile 102 on the cell above the back wall, offset (-1,-3) or (+1,-3);
	//  mouth at x 6-9, y 1 (the town's tiles 102/103 alike)
	//  gnoll fire ring: town tile 188 on open ground beside the hut's door, outside (edges layer);
	//  logs burn at x 5-11, y 4-9
	public static final int[][] HOUSE_WINDOWS = { { -1, 2 }, { 1, 2 } };
	public static final int CHIMNEY_DY = -3;
	public static final int[] CHIMNEY_MOUTH = { 6, 1, 4, 1 };   //x, y, w, h within the cell
	public static final int[] HEARTH_MOUTH  = { 6, 5, 4, 3 };
	//light kinds = frame index in effects/settlement_lights.png row 0
	public static final int KIND_WINDOW_A = 0, KIND_WINDOW_B = 1, KIND_TOWN_7 = 2, KIND_TOWN_23 = 3,
			KIND_TOWN_82 = 4, KIND_TOWN_164 = 5, KIND_HEARTH = 6;
	//...and the places of the caves (CaveSites, drawn by tools/cave_sites_gen.py): a lantern's glass
	//(row 0), a campfire's flames, a dwarven furnace's mouth, a giant mushroom's glowing spots and
	//the shards on a crystal cavern's floor (row 2)
	public static final int KIND_LANTERN = 7, KIND_CAMPFIRE = 8, KIND_FURNACE = 9, KIND_SPORES = 10, KIND_SHARDS = 11;
	//...and a violet giant's spots: the teal one's mask (both caps share their spots), its own colour
	public static final int KIND_SPORES_VIOLET = 12;
	public static final int[] VILLAGE_WINDOW_KINDS = { KIND_WINDOW_A, KIND_WINDOW_B };   //by windowStyle
	//the town tile each kind was measured on (the town's kinds; the village windows, A and B, are
	//drawn by dress_sheet.py and only keep the slots of the town tiles they first copied)
	public static final int[] KIND_SOURCE_TILE = { 7, 23, 7, 23, 82, 164, 188 };
	//the town's lit fittings by layout cell (TownRemixedTiles: glass on DECO, stacks on
	//ROOF_DECO): each window with its kind and its building's door (WorldStructures.TOWN_DOORS:
	//the store, the inn, the hall's pair, a house), so one building's windows light together.
	//the chapel's stained glass (264) is no dwelling and stays dark
	public static final int[][] TOWN_WINDOWS = { { 114, KIND_TOWN_164, 146 }, { 275, KIND_TOWN_82, 340 },
			{ 456, KIND_TOWN_7, 490 }, { 457, KIND_TOWN_7, 490 }, { 531, KIND_TOWN_23, 530 } };
	public static final int[] TOWN_CHIMNEYS = { 212, 281, 377, 392, 421, 468, 557 };
	//the inn (door TOWN_DOORS[6]): where the town sleeps (TownCommute), lit all night
	public static final int TOWN_INN_WINDOW = 275, TOWN_INN_CHIMNEY = 212;

	// ---------------------------------------------------------- the hours

	//dusk: each house lights up somewhere in [0.05, 0.6] of the dusk, one after another
	public static final float LIGHT_UP_AT = 0.05f, LIGHT_UP_SPREAD = 0.55f;
	//night: the middle of it is midnight (it runs from dusk to dawn); bed by 0.62 at the latest
	public static final float BEDTIME_AT = 0.5f, BEDTIME_SPREAD = 0.12f;
	//dawn: the night owls put out their lamps, then everyone's breakfast light until 0.5
	public static final float WAKE_SPREAD = 0.12f, BREAKFAST_UNTIL = 0.2f, BREAKFAST_SPREAD = 0.3f;
	//a sleeping house's glow, and the grey dawn's (the day outshines a lamp)
	public static final float EMBER = 0.2f, DAWN_LIGHT = 0.85f;
	//smoke: a hearth burns all day in the cold; a banked fire smokes about half as thick
	public static final float COLD_BELOW_C = 10f, BANKED = 0.45f, PUFF_INTERVAL = 0.3f, RAIN_THINNING = 0.6f;
	//the town's homes empty into the inn over the first stretch of the night
	public static final float TOWN_LEAVE_SPREAD = 0.06f;

	/** How brightly an inhabited house's lamps burn, 0..1: they come on one by one at dusk,
	 *  sink to embers after bedtime but for the night owls', and the grey of dawn brings a
	 *  breakfast light that the day puts out. h is the house's own hash (houseHash01). */
	public static float glowLevel( Phase phase, float p, float h, boolean owl ){
		switch (phase){
			case DUSK:
				return p >= LIGHT_UP_AT + LIGHT_UP_SPREAD * h ? 1f : 0f;
			case NIGHT:
				return owl || p < BEDTIME_AT + BEDTIME_SPREAD * h ? 1f : EMBER;
			case DAWN:
				if (p < WAKE_SPREAD * h) return owl ? 1f : EMBER;
				return p < BREAKFAST_UNTIL + BREAKFAST_SPREAD * h ? DAWN_LIGHT : 0f;
			default:
				return 0f;
		}
	}

	/** The town's: it sleeps at the inn (TownCommute), so its homes go dark early in the
	 *  night while the inn burns till breakfast. */
	public static float townGlowLevel( Phase phase, float p, float h, boolean inn ){
		switch (phase){
			case DUSK:
				return p >= LIGHT_UP_AT + LIGHT_UP_SPREAD * h ? 1f : 0f;
			case NIGHT:
				return inn ? 1f : (p < 0.02f + TOWN_LEAVE_SPREAD * h ? 1f : 0f);
			case DAWN:
				return inn && p < BREAKFAST_UNTIL + BREAKFAST_SPREAD * h ? DAWN_LIGHT : 0f;
			default:
				return 0f;
		}
	}

	/** How much a hearth smokes, 0..1: all day and night in the cold (banked once the house
	 *  is asleep), otherwise for breakfast and supper only. */
	public static float smokeLevel( GameCalendar.Season s, float tempC, Phase phase, float p, float h, boolean owl ){
		boolean cold = s == GameCalendar.Season.WINTER || tempC < COLD_BELOW_C;
		if (cold){
			return phase == Phase.NIGHT && p >= BEDTIME_AT + BEDTIME_SPREAD * h && !owl ? BANKED : 1f;
		}
		switch (phase){
			case DAWN: return p >= WAKE_SPREAD * h ? 1f : 0f;
			case DUSK: return 1f;
			default:   return 0f;
		}
	}

	/** Seconds between puffs for a smoke level, 0 for none: rain thins the column. */
	public static float puffInterval( float level, boolean raining ){
		if (level <= 0f) return 0f;
		return PUFF_INTERVAL / (level * (raining ? RAIN_THINNING : 1f));
	}

	// ------------------------------------------------------------ hashes

	/** A house's own place in the evening, 0..1: who lights up first, who goes to bed last. */
	public static float houseHash01( long seed, int wx, int wy ){
		return (WindowGenerator.dressHash( seed, wx, wy, 0x11647L ) >>> 40) / (float)(1 << 24);
	}

	/** Which of the two window styles a house has: both its windows match. */
	public static int windowStyle( long seed, int wx, int wy ){
		return (int)((WindowGenerator.dressHash( seed, wx, wy, 0x3E1D0L ) >>> 17) & 1);
	}

	/** Which back corner a house's chimney stands over (-1 west, +1 east), and which side
	 *  of a gnoll hut's door its fire ring is laid. */
	public static int chimneySide( long seed, int wx, int wy ){
		return ((WindowGenerator.dressHash( seed, wx, wy, 0xC4111L ) >>> 23) & 1) == 0 ? -1 : 1;
	}

	/** Is this house up late tonight? One in a small village (under six lived-in houses, so
	 *  most still go dark), two anywhere bigger; others each night, the same for everyone.
	 *  `night` is WorldClock.night(). */
	public static boolean nightOwl( long seed, int sx, int sy, int index, int inhabited, long night ){
		long h = WindowGenerator.dressHash( seed, sx, sy, 0x0371E5L ^ (night * 0x9E3779B97F4A7C15L) );
		int first = (int) Math.floorMod( h, (long) inhabited );
		if (index == first) return true;
		if (inhabited < 6) return false;
		return index == (first + 1 + (int) Math.floorMod( h >>> 24, (long)(inhabited - 1) )) % inhabited;
	}

	// ------------------------------------------------------------ houses

	/** One house of a settlement. */
	public static final class House {
		public final int sx, sy;          //the settlement's sector
		public final int index, count;    //place in the layout (centre-out) and the settlement's size
		public final WorldStructures.Faction faction;
		public final int dx, dy;          //offset from the well (layout values)
		public final int wx, wy;          //world cell of the house's centre

		House( int sx, int sy, int index, int count, WorldStructures.Faction faction, int dx, int dy, int wx, int wy ){
			this.sx = sx;
			this.sy = sy;
			this.index = index;
			this.count = count;
			this.faction = faction;
			this.dx = dx;
			this.dy = dy;
			this.wx = wx;
			this.wy = wy;
		}

		/** Somebody lives here: the very rule populateSettlement uses, whether or not the family is loaded. */
		public boolean inhabited(){
			return index < WorldStructures.populatedHouses( count );
		}
	}

	/**
	 * The houses whose shell or chimney row reaches the world rect [x0,x1] x [y0,y1]: every
	 * human and gnoll settlement within a sector of it (nobody lives in a bandit camp's
	 * houses). In order: sector row, sector column, place in the layout.
	 */
	public static ArrayList<House> housesIn( long seed, int x0, int y0, int x1, int y1 ){
		ArrayList<House> out = new ArrayList<>();
		int sx0 = Math.floorDiv( x0, WorldStructures.SECTOR ) - 1, sx1 = Math.floorDiv( x1, WorldStructures.SECTOR ) + 1;
		int sy0 = Math.floorDiv( y0, WorldStructures.SECTOR ) - 1, sy1 = Math.floorDiv( y1, WorldStructures.SECTOR ) + 1;
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				WorldStructures.Faction faction = WorldStructures.faction( seed, sx, sy );
				//nobody lives in a bandit camp's houses
				if (faction == WorldStructures.Faction.BANDIT) continue;
				int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
				int[] layout = WorldStructures.settlementLayout( seed, sx, sy );
				int reach = layout[0] + 3;
				if (cx + reach < x0 || cx - reach > x1 || cy + reach < y0 || cy - reach > y1) continue;
				int n = (layout.length - 1) / 2;
				for (int i = 0; i < n; i++){
					int hdx = layout[1 + 2*i], hdy = layout[2 + 2*i];
					int hx = cx + hdx, hy = cy + hdy;
					//the shell and the chimney row above it
					if (hx + 3 < x0 || hx - 3 > x1 || hy + 3 < y0 || hy - 3 > y1) continue;
					out.add( new House( sx, sy, i, n, faction, hdx, hdy, hx, hy ) );
				}
			}
		}
		return out;
	}

	// ------------------------------------------------- cells in a window
	//each takes the window's terrain (w x h, origin ox, oy) and gives a window cell or -1:
	//the dressing places its fittings by them and SettlementAmbience lights by them, so
	//the two can never disagree

	//a crown or a peak hangs over the cell above an oak or a tall rock: nothing that has
	//to be seen goes under one (WindowGenerator.dress puts them on the canopy)
	private static boolean underCrown( long seed, int[] map, int c, int w, int wx, int wy ){
		int below = map[c + w];
		return below == Terrain.TREE_OAK
				|| (below == Terrain.BOULDER && WindowGenerator.tallRock( seed, 0, wx, wy + 1 ));
	}

	/** The window cell of a house window's glass at world (wx, wy): a wall whose face shows
	 *  (DungeonTerrainTilemap draws a RAISED_WALL face where the cell below does not stitch
	 *  to it) and no tree crown hangs over. */
	public static int glassCell( long seed, int[] map, int w, int h, int ox, int oy, int wx, int wy ){
		int x = wx - ox, y = wy - oy;
		if (x < 1 || x >= w - 1 || y < 1 || y >= h - 2) return -1;
		int c = x + y * w;
		if (map[c] != Terrain.WALL || DungeonTileSheet.wallStitcheable( map[c + w] )) return -1;
		if (underCrown( seed, map, c, w, wx, wy )) return -1;
		return c;
	}

	/** The window cell a human house's chimney stack stands on: above a back corner of the
	 *  shell (the hashed side first), on open ground - never a tree, a boulder, a fence, a
	 *  door's approach or a sign. */
	public static int chimneyCell( long seed, int[] map, int w, int h, int ox, int oy, int hx, int hy ){
		int side = chimneySide( seed, hx, hy );
		for (int k = 0; k < 2; k++, side = -side){
			int x = hx + side - ox, y = hy + CHIMNEY_DY - oy;
			if (x < 1 || x >= w - 1 || y < 1 || y >= h - 2) continue;
			int c = x + y * w;
			if (map[c + w] != Terrain.WALL) continue;
			int t = map[c];
			if ((Terrain.flags[t] & Terrain.SOLID) != 0 || WindowGenerator.blocksSight( t )) continue;
			return c;
		}
		return -1;
	}

	/**
	 * The window cell of a gnoll hut's fire ring: outside, one step from the door toward the
	 * well and one to the side (the hashed side first), else the same a step further out (by
	 * a north door the hut's own wall lays its lip over both near cells), on plain open
	 * ground - earth, grass, a footpath, sand or snow; never a doorway, a wall, a fence,
	 * water, a bridge or a sign, nor under a crown or the lip the walls layer draws over a
	 * cell standing on a wall, a door or a fence. The ring lies on the dressing's edges
	 * layer, over the ground's own overlays (a snow cap, a transition, autumn's leaves stay
	 * under it).
	 *
	 * `pristine` is the window's untouched terrain (WindowGenerator.Window.terrain, the
	 * level's pristine()), never the live map: grass grows tall and is trampled flat, a
	 * fire leaves embers, and the ring must not move for any of it - the dressing and the
	 * lights both read this, so where the ring lies is where it burns.
	 */
	public static int hearthCell( long seed, int[] pristine, int w, int h, int ox, int oy, House house ){
		int ddx = WorldStructures.houseDoorDX( house.dx, house.dy ) - house.dx;
		int ddy = WorldStructures.houseDoorDY( house.dx, house.dy ) - house.dy;
		//one step out of the door: the door minus the inward step (house - door)
		int ux = Integer.signum( ddx ), uy = Integer.signum( ddy );
		int tx = house.wx + ddx + ux, ty = house.wy + ddy + uy;
		//then a step across: the inward step turned a quarter
		int px = -uy, py = -ux;
		int first = chimneySide( seed, house.wx, house.wy );
		for (int out = 0; out < 2; out++){
			for (int k = 0, side = first; k < 2; k++, side = -side){
				int wx = tx + ux * out + px * side, wy = ty + uy * out + py * side;
				int x = wx - ox, y = wy - oy;
				if (x < 1 || x > w - 2 || y < 1 || y > h - 2) continue;
				int c = x + y * w;
				if (!openGround( pristine[c] ) || underLip( pristine[c + w] ) || underCrown( seed, pristine, c, w, wx, wy )) continue;
				return c;
			}
		}
		return -1;
	}

	//the plain ground a fire is laid on
	private static boolean openGround( int t ){
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS || t == Terrain.DIRT_PATH
				|| t == Terrain.SNOW || t == Terrain.EMPTY_SP;
	}

	/** Does the walls layer draw a lip over the lower half of a cell standing on this? A wall's
	 *  overhang, a door's or a fence's (DungeonWallsTilemap): the edges layer is under it. */
	static boolean underLip( int below ){
		return DungeonTileSheet.wallStitcheable( below ) || DungeonTileSheet.doorTile( below ) || below == Terrain.BARRICADE;
	}

	/**
	 * Lays the settlements' fittings into a window's dressing (WindowGenerator.dress, on
	 * the window worker): two windows on the front of every human house and a chimney stack
	 * over its back wall (by the window's map), a fire ring by every gnoll hut's door (by
	 * its untouched terrain, see hearthCell). Every house gets them, lived in or not - a
	 * quiet city house just has cold windows and a cold stack.
	 */
	static void dressFittings( long seed, int ox, int oy, int[] map, int[] pristine, int[] ground, int[] edges, int[] canopy ){
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		for (House house : housesIn( seed, ox, oy, ox + w - 1, oy + h - 1 )){
			if (house.faction == WorldStructures.Faction.HUMAN){
				int style = OverworldDress.VILLAGE_WINDOWS[windowStyle( seed, house.wx, house.wy )];
				for (int[] o : HOUSE_WINDOWS){
					int c = glassCell( seed, map, w, h, ox, oy, house.wx + o[0], house.wy + o[1] );
					if (c != -1) ground[c] = style;
				}
				int c = chimneyCell( seed, map, w, h, ox, oy, house.wx, house.wy );
				if (c != -1) canopy[c] = OverworldDress.VILLAGE_CHIMNEY;
			} else {
				int c = hearthCell( seed, pristine, w, h, ox, oy, house );
				if (c != -1) edges[c] = OverworldDress.GNOLL_HEARTH;
			}
		}
	}

	// ------------------------------------------------------------ debug

	/** Where a debug scene stands the hero to look at a settlement: on its axis south of the
	 *  well, where the house fronts face him, clear of every house (the shell and a cell
	 *  round it - arriving on a wall would carve it open) and off the fence ring. */
	public static int[] standCell( long seed, int sx, int sy ){
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		int[] layout = WorldStructures.settlementLayout( seed, sx, sy );
		int r = layout[0];
		for (int dy = Math.min( r - 2, 10 ); dy <= r + 4; dy++){
			if (dy == r + 2) continue;
			boolean clear = true;
			for (int i = 1; clear && i + 1 < layout.length; i += 2){
				if (Math.abs( layout[i] ) <= 3 && Math.abs( dy - layout[i+1] ) <= 3) clear = false;
			}
			if (clear) return new int[]{ cx, cy + dy };
		}
		return new int[]{ cx, cy + r + 4 };
	}
}
