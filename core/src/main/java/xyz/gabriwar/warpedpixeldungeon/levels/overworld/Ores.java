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
import xyz.gabriwar.warpedpixeldungeon.items.ore.Amethyst;
import xyz.gabriwar.warpedpixeldungeon.items.ore.CopperOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Deepsilver;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Diamond;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Emerald;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Garnet;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Gem;
import xyz.gabriwar.warpedpixeldungeon.items.ore.GoldOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Sapphire;
import xyz.gabriwar.warpedpixeldungeon.items.ore.SilverOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Skyiron;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;

/**
 * The ore in the rock of the slices above and below the surface. Nothing of it is stored: a
 * vein is a pure function of (seed, altitude, world cell), so every window, every reload, the
 * preparation workers and every network mirror agree on where it runs without a byte saved.
 *
 * Shape: the slice is cut into 8x8 blocks, and a block holds at most one vein. A vein starts
 * on a rock FACE - natural rock with open ground south of it, the stone a player sees - and
 * runs three to eight cells along the wall, keeping to its faces where it can and running on
 * into the rock behind where it cannot. So a vein reads as a seam along the wall, and the
 * pick that follows it finds it goes on.
 *
 * Seen and mined: the dress draws a vein only on a WALL whose face shows (showsFace - the
 * same rule DungeonTileSheet.getRaisedWallTile draws a face by), but the pick gets the ore out
 * of any vein cell it breaks, so breaking one face shows the cell behind it, which glints in
 * turn if the vein goes on. A mined vein is an ordinary edit of the window (the cell becomes
 * floor like any mined wall, OverworldLevel.captureDiffs), so it never comes back.
 *
 * A window works its veins out once, on the thread that generates it (WindowGenerator.generate
 * -> Window.veins); the dress, the pick and the redraw after an edit only read that map.
 *
 * Gems: the crystal seams of the caves (MINE_CRYSTAL) are gem pockets - a crystal mined
 * itself may hold one (gemAt); the crystals a blow shatters beside it lose theirs.
 *
 * Only natural rock holds ore (naturalRock): built walls never do, whatever stands there.
 */
public final class Ores {

	private Ores(){}

	/*
	 * Exchange rates (Kind and GemKind below), in turns, the one clock fighting and mining share.
	 * A masterwork core is the progression unit, and fighting pays it slowly: a boss drops one or
	 * two (Quality.onKill), any other kill one time in 1400 - a few tenths of a core in a thousand
	 * turns of fighting, most of it the bosses'. A core sells to a shop for 1000 and costs at
	 * least 2000 at the black market (BlackMarket: 2000 * step).
	 *
	 * Mining, measured (OresTest.minerRate: a miner on real windows walking to the nearest glint,
	 * a turn for each cell walked and each cell broken, 4 hunger a cell): about 0.15 vein cells a
	 * turn - in a thousand turns 150 at -5 (29 copper, 83 iron, 39 silver: 3.3 batches) and 184 at
	 * -12 (112 silver, 61 gold, 36 deepsilver: 10 batches). Smelted as fast as it is dug that
	 * would be ten to thirty times what fighting pays, so a hero gets ONE crucible a day, at the
	 * troll's or any deep forge (Blacksmith2.pourCrucible, the day kept on the hero; a day is 2500
	 * turns): at most 0.4 cores a thousand turns from the forges, about what a fighter earns (at
	 * -5 six hundred turns of digging fill it). What depth
	 * and height buy is the rest of the ore, sold: about 1.7 gold a turn at -5, 8 at -12 (beside
	 * an overworld gold pile of 1000-2000, Gold.random at depths 97-112), with the extreme pair's
	 * filings paid on top of their core.
	 *
	 * The ways to spend it never loop: a whole batch sold to a shop always earns less than a
	 * core's 1000 (copper 500, iron 500, silver 600, gold 700); the extreme pair sells for
	 * exactly 1000 a batch, and the troll's 150 gold for their filings tips it to him. 10
	 * deepsilver make 1 core + 150 at the troll, 1.43 cores at the deep forge (unit
	 * cave-sites: a batch of 7, no filings), 1500 gold at the miners' camp (1.5x) and 1000 at
	 * a shop - so the forges are the progression road, the camp the gold road, and the gold
	 * never buys back (at 2000 a core) more cores than smelting would have made. Star-metal
	 * stays the better find: two fragments make a core.
	 *
	 * Gems are never smelted: shops pay their price, alchemists their energy. A crystal costs
	 * a turn and 1 hunger, a vein 4, and a gem pays no more gold per hunger than ore does: the
	 * gem chance (gemAt, 6% at -1 to 17% at -12) times the mean stone's price, measured against
	 * the glinting faces of real windows (OresTest.oreAndGemEconomy), gives 1.5 / 2.7 / 6.6
	 * gold a hunger at -3 / -6 / -12, the ore 1.8 / 4.6 / 12.0.
	 */

	/** The metals, in rising worth. NEVER reorder: the ordinal picks the dress tile
	 *  (OverworldDress.ORE_FACE + ORE_VERSIONS * ordinal) and decides which vein shows where two cross. */
	public enum Kind {
		//          item               price batch bonus energy colour    sound                      pitch stars
		COPPER(     CopperOre.class,      5,  100,    0,    0, 0xE07630, Assets.Sounds.ROCKS_LIGHT, 1.2f, 0 ),
		IRON(       IronOre.class,       10,   50,    0,    0, 0xD2B4A0, Assets.Sounds.HIT_CRUSH,   0.8f, 0 ),
		SILVER(     SilverOre.class,     20,   30,    0,    0, 0xD0D6E4, Assets.Sounds.GOLD,        1.3f, 2 ),
		GOLD(       GoldOre.class,       35,   20,    0,    0, 0xF2C028, Assets.Sounds.GOLD,        1.0f, 3 ),
		DEEPSILVER( Deepsilver.class,   100,   10,  150,    5, 0x46CEC4, Assets.Sounds.CHARMS,      0.8f, 4 ),
		SKYIRON(    Skyiron.class,      100,   10,  150,    5, 0x96BEF2, Assets.Sounds.SHATTER,     1.4f, 4 );

		public final Class<? extends Ore> item;
		//gold a lump fetches at a shop; lumps to a core at the troll's forge (one crucible a day);
		//gold he pays per core for the filings; alchemical energy a lump gives
		public final int price, batch, bonus, energy;
		//the splash and the sound of the strike, and how many stars fly off it
		public final int colour, stars;
		public final String sound;
		public final float pitch;

		Kind( Class<? extends Ore> item, int price, int batch, int bonus, int energy, int colour,
				String sound, float pitch, int stars ){
			this.item = item;
			this.price = price;
			this.batch = batch;
			this.bonus = bonus;
			this.energy = energy;
			this.colour = colour;
			this.sound = sound;
			this.pitch = pitch;
			this.stars = stars;
		}

		/** the lumps to a core at the deep forge by the burning rift (CaveSites, DwarvenForge): the
		 *  rift's heat takes the extremes in smaller crucibles, and pays nothing for their filings */
		public int forgeBatch(){ return this == DEEPSILVER || this == SKYIRON ? 7 : batch; }
	}

	/** The gems of the crystal seams, by depth. NEVER reorder (the GEMS columns). */
	public enum GemKind {
		//         item             price energy colour    pitch
		AMETHYST(  Amethyst.class,    15,   1,  0x9650C8, 1.1f ),
		GARNET(    Garnet.class,      20,   2,  0xC42838, 1.0f ),
		EMERALD(   Emerald.class,     35,   3,  0x30B860, 1.2f ),
		SAPPHIRE(  Sapphire.class,    35,   3,  0x3060E6, 1.2f ),
		DIAMOND(   Diamond.class,     80,   5,  0xE0F0FA, 1.5f );

		public final Class<? extends Gem> item;
		public final int price, energy, colour;
		public final float pitch;

		GemKind( Class<? extends Gem> item, int price, int energy, int colour, float pitch ){
			this.item = item;
			this.price = price;
			this.energy = energy;
			this.colour = colour;
			this.pitch = pitch;
		}
	}

	//the share (in %) of each metal among the veins of a slice, by |altitude|: COPPER, IRON,
	//SILVER, GOLD and the extreme one (DEEPSILVER in the caves, SKYIRON on the peaks). copper
	//is the shallow caves' and the foothills' metal, gold and the extremes the far slices'
	private static final int[][] CAVE_TIERS = {
			{   0,  0,  0,  0,  0 },
			{ 100,  0,  0,  0,  0 },   //-1
			{  75, 25,  0,  0,  0 },
			{  55, 45,  0,  0,  0 },
			{  35, 50, 15,  0,  0 },
			{  15, 55, 30,  0,  0 },   //-5
			{   0, 50, 40, 10,  0 },
			{   0, 40, 45, 15,  0 },
			{   0, 30, 50, 20,  0 },
			{   0, 20, 52, 23,  5 },
			{   0, 10, 55, 25, 10 },   //-10
			{   0,  0, 58, 27, 15 },
			{   0,  0, 52, 26, 22 },   //-12
	};
	//on the peaks the row is the band of the ROCK the vein starts in - how high that rock
	//reaches - not the slice it is seen from: the summit rock of a band-9 peak holds skyiron
	//wherever its face shows (slices +5..+8 under it)
	private static final int[][] PEAK_TIERS = {
			{   0,  0,  0,  0,  0 },
			{ 100,  0,  0,  0,  0 },   //band 1
			{  70, 30,  0,  0,  0 },
			{  45, 55,  0,  0,  0 },
			{  20, 60, 20,  0,  0 },
			{   0, 60, 40,  0,  0 },   //band 5
			{   0, 45, 50,  5,  0 },
			{   0, 30, 55, 15,  0 },
			{   0, 15, 55, 22,  8 },
			{   0,  0, 60, 25, 15 },
			{   0,  0, 52, 26, 22 },   //band 10
	};
	//the gem mix of a crystal by depth band ((d-1)/3 for d = -altitude): AMETHYST, GARNET,
	//EMERALD, SAPPHIRE, DIAMOND - diamonds only below -3, commonest at the bottom
	private static final int[][] GEMS = {
			{ 60, 30,  8,  2,  0 },
			{ 35, 30, 18, 14,  3 },
			{ 20, 25, 24, 22,  9 },
			{ 10, 20, 25, 28, 17 },
	};

	static final int BLOCK = 8;
	private static final long VEIN_SALT = 0x0E5E1EL, SHAPE_SALT = 0x5E1FL, YIELD_SALT = 0x91E1DL,
			FACE_SALT = 0xFACE5L, GEM_SALT = 0x6E3L, GEM_KIND_SALT = 0x6E3C1L;

	/**
	 * The rock ore may lie in: the caves' rock where the cave field is closed, the peaks' rock of
	 * the bands above the slice (but not the tunnels through it); the surface holds none. Built
	 * walls are never natural rock (tomb rings stand on cave floor, huts and towers on a peak's
	 * own ground), so masonry never holds ore.
	 */
	public static boolean naturalRock( long seed, int altitude, int wx, int wy ){
		if (altitude < 0) return !WorldModel.caveOpen( seed, wx, wy, altitude );
		if (altitude > 0) return WorldLayers.band( WorldModel.elevation( seed, wx, wy ) ) > altitude
				&& !WorldModel.tunnelAt( seed, wx, wy, altitude );
		return false;
	}

	/** Per mille: the chance a block of the slice with a rock face in it holds a vein. None on
	 *  the surface, and none on the top slice: nothing rises above +10, so it has no rock. */
	public static int density( int altitude ){
		if (altitude == 0 || !WorldLayers.exists( altitude ) || altitude >= WorldLayers.MAX_ABOVE) return 0;
		return 600 + 20 * (Math.abs( altitude ) - 1);
	}

	/** The share (in %) of this metal among the veins of the caves at this depth, or of the
	 *  peaks' rock of this band (altitude > 0 is the band); 0 on the surface and off the stack. */
	public static int weight( int altitude, Kind k ){
		int[] row = row( altitude );
		if (row == null) return 0;
		switch (k){
			case DEEPSILVER: return altitude < 0 ? row[4] : 0;
			case SKYIRON:    return altitude > 0 ? row[4] : 0;
			default:         return row[k.ordinal()];
		}
	}

	private static int[] row( int altitude ){
		if (altitude < 0 && altitude >= -WorldLayers.MAX_BELOW) return CAVE_TIERS[-altitude];
		if (altitude > 0 && altitude <= WorldLayers.MAX_ABOVE) return PEAK_TIERS[altitude];
		return null;
	}

	// ------------------------------------------------------------ the veins

	/**
	 * Natural rock, asked once per cell: the vein walk tests a cell and its neighbours over and
	 * over, and the peaks' test takes a whole world sample. One memo per query, never shared
	 * between threads; a cell outside its rect is simply asked straight.
	 *
	 * Inside a generated window the generator has already answered: its row pass records
	 * naturalRock for every cell (Window.rock) before anything is built, flooded or cut into the
	 * terrain. Never the terrain itself - a site's masonry is WALL on natural floor, its carved
	 * floor open in natural rock, and the veins must not move with either.
	 * OresTest.rockBitsAreTheWorldsRock holds the two to the same answer.
	 */
	static final class Rock {
		final long seed;
		final int altitude, x0, y0, w, h;
		final byte[] known;   //0 unasked, 1 rock, 2 open
		//a generated window's natural-rock bits and origin, or null
		boolean[] bits;
		int px, py;

		Rock( long seed, int altitude, int x0, int y0, int w, int h ){
			this.seed = seed;
			this.altitude = altitude;
			this.x0 = x0;
			this.y0 = y0;
			this.w = w;
			this.h = h;
			known = new byte[w * h];
		}

		Rock over( boolean[] bits, int ox, int oy ){
			this.bits = bits;
			px = ox;
			py = oy;
			return this;
		}

		boolean at( int wx, int wy ){
			int x = wx - x0, y = wy - y0;
			if (x < 0 || y < 0 || x >= w || y >= h) return ask( wx, wy );
			int i = x + y * w;
			if (known[i] == 0) known[i] = (byte)(ask( wx, wy ) ? 1 : 2);
			return known[i] == 1;
		}

		private boolean ask( int wx, int wy ){
			if (bits != null){
				int lx = wx - px, ly = wy - py;
				if (lx >= 0 && ly >= 0 && lx < WindowGenerator.WIDTH && ly < WindowGenerator.HEIGHT){
					return bits[lx + ly * WindowGenerator.WIDTH];
				}
			}
			return naturalRock( seed, altitude, wx, wy );
		}

		//rock with open ground south of it: the stone a player sees
		boolean face( int wx, int wy ){
			return at( wx, wy ) && !at( wx, wy + 1 );
		}
	}

	//the memo for the veins of blocks bx0..bx1 x by0..by1: every cell they can reach, plus the
	//row under them their faces look at
	private static Rock rockFor( long seed, int altitude, int bx0, int by0, int bx1, int by1 ){
		int x0 = (bx0 - 1) * BLOCK, y0 = (by0 - 1) * BLOCK;
		return new Rock( seed, altitude, x0, y0, (bx1 - bx0 + 3) * BLOCK, (by1 - by0 + 3) * BLOCK + 1 );
	}

	/**
	 * The vein of block (bx, by), if it has one: its length (0 = none), its cells in xs/ys (8
	 * each) and its metal in kind[0].
	 *
	 * It starts on the first face down one of two columns of the block (a block of solid rock or
	 * open floor has no face and no vein), then walks 3..8 cells east or west: onto the next
	 * face along (level, up or down a step), else up into the rock behind the face, else it ends.
	 * A walk cut short under three cells makes no vein. Every step moves at most one cell on each
	 * axis and the start lies inside the block, so the whole vein stays within blocks bx-1..bx+1,
	 * by-1..by+1 - which is why oreAt looks no further.
	 */
	static int vein( long seed, int altitude, int bx, int by, int[] xs, int[] ys, Kind[] kind, Rock rock ){
		int d = density( altitude );
		if (d == 0) return 0;
		if (Math.floorMod( WorldModel.linkHash( seed ^ VEIN_SALT, bx, by, altitude ), 1000 ) >= d) return 0;
		long s = WorldModel.linkHash( seed ^ SHAPE_SALT, bx, by, altitude );
		int x = 0, y = 0;
		boolean found = false;
		for (int t = 0; t < 2 && !found; t++){
			int cx = bx * BLOCK + (int)((s >>> (3 * t)) & 7);
			int first = (int)((s >>> (6 + 3 * t)) & 7);
			for (int i = 0; i < BLOCK; i++){
				int cy = by * BLOCK + (first + i) % BLOCK;
				if (rock.face( cx, cy )){
					x = cx;
					y = cy;
					found = true;
					break;
				}
			}
		}
		if (!found) return 0;

		int[] row;
		if (altitude < 0){
			row = row( altitude );
		} else {
			int band = WorldLayers.band( WorldModel.elevation( seed, x, y ) );
			row = PEAK_TIERS[Math.max( altitude + 1, Math.min( WorldLayers.MAX_ABOVE, band ) )];
		}
		int roll = (int)((s >>> 16) % 100);
		Kind k = null;
		for (int i = 0; i < row.length; i++){
			roll -= row[i];
			if (roll < 0){
				k = i < 4 ? Kind.values()[i] : altitude < 0 ? Kind.DEEPSILVER : Kind.SKYIRON;
				break;
			}
		}
		if (k == null) return 0;
		kind[0] = k;

		int len = 3 + (int)((s >>> 24) % 6);
		int dx = ((s >>> 30) & 1) == 0 ? 1 : -1;
		xs[0] = x;
		ys[0] = y;
		int n = 1;
		while (n < len){
			int nx = x + dx, ny;
			if (rock.face( nx, y ))             ny = y;
			else if (rock.face( nx, y - 1 ))    ny = y - 1;
			else if (rock.face( nx, y + 1 ))    ny = y + 1;
			else if (rock.at( nx, y - 1 ))      ny = y - 1;
			else if (rock.at( x, y - 1 )){      nx = x; ny = y - 1; }
			else break;
			x = nx;
			y = ny;
			xs[n] = x;
			ys[n] = y;
			n++;
		}
		//a stub of one or two cells is no seam: none at all
		return n < 3 ? 0 : n;
	}

	/** The ore in the rock at this world cell of the slice, or null. The rarer metal wins where two veins cross. */
	public static Kind oreAt( long seed, int altitude, int wx, int wy ){
		if (density( altitude ) == 0) return null;
		int bx = Math.floorDiv( wx, BLOCK ), by = Math.floorDiv( wy, BLOCK );
		Rock rock = rockFor( seed, altitude, bx - 1, by - 1, bx + 1, by + 1 );
		int[] xs = new int[BLOCK], ys = new int[BLOCK];
		Kind[] kind = new Kind[1];
		Kind best = null;
		for (int j = by - 1; j <= by + 1; j++){
			for (int i = bx - 1; i <= bx + 1; i++){
				int n = vein( seed, altitude, i, j, xs, ys, kind, rock );
				for (int c = 0; c < n; c++){
					if (xs[c] == wx && ys[c] == wy && (best == null || kind[0].ordinal() > best.ordinal())){
						best = kind[0];
					}
				}
			}
		}
		return best;
	}

	/** Every vein cell of a w x h rect at (ox, oy) at once, into out (ordinal + 1, 0 for none): the
	 *  same answer as oreAt cell by cell, block by block. rock, when given, is the natural-rock bits
	 *  of the generated window at (ox, oy) (Window.rock; the rect then is that window: see Rock). */
	public static void veins( long seed, int altitude, int ox, int oy, int w, int h, boolean[] rock, byte[] out ){
		if (density( altitude ) == 0) return;
		int bx0 = Math.floorDiv( ox, BLOCK ) - 1, bx1 = Math.floorDiv( ox + w - 1, BLOCK ) + 1;
		int by0 = Math.floorDiv( oy, BLOCK ) - 1, by1 = Math.floorDiv( oy + h - 1, BLOCK ) + 1;
		Rock memo = rockFor( seed, altitude, bx0, by0, bx1, by1 ).over( rock, ox, oy );
		int[] xs = new int[BLOCK], ys = new int[BLOCK];
		Kind[] kind = new Kind[1];
		for (int by = by0; by <= by1; by++){
			for (int bx = bx0; bx <= bx1; bx++){
				int n = vein( seed, altitude, bx, by, xs, ys, kind, memo );
				for (int c = 0; c < n; c++){
					int x = xs[c] - ox, y = ys[c] - oy;
					if (x < 0 || y < 0 || x >= w || y >= h) continue;
					int i = x + y * w;
					out[i] = (byte) Math.max( out[i], kind[0].ordinal() + 1 );
				}
			}
		}
	}

	/** The lumps one strike prises out of a vein cell: one, or two now and then where the rock
	 *  is rich - from -6 down (3% there, 20% at -12) and in the rock of band 6 and up on the peaks. */
	public static int yield( long seed, int altitude, int wx, int wy ){
		int a = Math.abs( altitude );
		if (altitude > 0) a = Math.max( altitude + 1, WorldLayers.band( WorldModel.elevation( seed, wx, wy ) ) );
		int extra = Math.max( 0, Math.min( 20, 3 * (a - 5) ) );
		return Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, YIELD_SALT ^ altitude ), 100 ) < extra ? 2 : 1;
	}

	// ------------------------------------------------------------ the gems

	/** The gem a crystal of a cave seam holds, or null: 6% of crystals at -1 to 17% at -12, the
	 *  finer stones deeper down. None above the caves. */
	public static GemKind gemAt( long seed, int altitude, int wx, int wy ){
		if (altitude >= 0 || !WorldLayers.exists( altitude )) return null;
		int d = -altitude;
		if (Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, GEM_SALT ^ (altitude * 0x9E37L) ), 1000 ) >= 60 + 10 * (d - 1)){
			return null;
		}
		int[] mix = GEMS[(d - 1) / 3];
		int roll = (int)Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, GEM_KIND_SALT ^ altitude ), 100 );
		for (int i = 0; i < mix.length; i++){
			roll -= mix[i];
			if (roll < 0) return GemKind.values()[i];
		}
		return GemKind.values()[mix.length - 1];
	}

	// ------------------------------------------------------- the places' share

	/** The metal most of a slice's veins hold (caves; a peak's band): what an old mine's cache
	 *  holds (CaveSites). The shallower metal wins a tie. */
	public static Kind commonKind( int altitude ){
		Kind best = Kind.COPPER;
		for (Kind k : Kind.values()){
			if (weight( altitude, k ) > weight( altitude, best )) best = k;
		}
		return best;
	}

	/** The rarest metal a slice's veins hold at all: what a sealed tomb was buried with (CaveSites). */
	public static Kind richestKind( int altitude ){
		Kind best = Kind.COPPER;
		for (Kind k : Kind.values()){
			if (weight( altitude, k ) > 0) best = k;
		}
		return best;
	}

	/** A gem of this cave slice's mix (the one a mined crystal's gem is drawn from, gemAt) for a
	 *  roll in 0..99: what a crystal cavern's cache holds (CaveSites). */
	public static GemKind rollGem( int altitude, int roll100 ){
		int d = Math.max( 1, Math.min( WorldLayers.MAX_BELOW, -altitude ) );
		int[] mix = GEMS[(d - 1) / 3];
		int roll = Math.floorMod( roll100, 100 );
		for (int i = 0; i < mix.length; i++){
			roll -= mix[i];
			if (roll < 0) return GemKind.values()[i];
		}
		return GemKind.values()[mix.length - 1];
	}

	/** n lumps of a metal, one stack. */
	public static Ore lumps( Kind k, int n ){
		Ore ore = com.watabou.utils.Reflection.newInstance( k.item );
		if (ore != null) ore.quantity( Math.max( 1, n ) );
		return ore;
	}

	/**
	 * The miners' word on the rock (CaveSites rumours): the nearest vein of the rarest metal any
	 * vein within `radius` of (wx, wy) holds, as {x, y, Kind ordinal} of its first cell, or null when
	 * no vein is near. Block by block, the way veins() finds them; pure, so what a miner says is
	 * always there.
	 */
	public static int[] notableVein( long seed, int altitude, int wx, int wy, int radius ){
		if (density( altitude ) == 0) return null;
		int bx0 = Math.floorDiv( wx - radius, BLOCK ), bx1 = Math.floorDiv( wx + radius, BLOCK );
		int by0 = Math.floorDiv( wy - radius, BLOCK ), by1 = Math.floorDiv( wy + radius, BLOCK );
		Rock rock = rockFor( seed, altitude, bx0, by0, bx1, by1 );
		int[] xs = new int[BLOCK], ys = new int[BLOCK];
		Kind[] kind = new Kind[1];
		int[] best = null;
		long bestD = Long.MAX_VALUE;
		for (int by = by0; by <= by1; by++){
			for (int bx = bx0; bx <= bx1; bx++){
				if (vein( seed, altitude, bx, by, xs, ys, kind, rock ) == 0) continue;
				long dx = xs[0] - wx, dy = ys[0] - wy;
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) > radius) continue;
				long d = dx * dx + dy * dy;
				int k = kind[0].ordinal();
				if (best == null || k > best[2] || (k == best[2] && d < bestD)){
					best = new int[]{ xs[0], ys[0], k };
					bestD = d;
				}
			}
		}
		return best;
	}

	// ------------------------------------------------------------ the art

	/** Does this window cell show a rock face - a WALL over a cell that does not stitch to it? The
	 *  rule DungeonTileSheet.getRaisedWallTile draws a RAISED_WALL face by. */
	public static boolean showsFace( int[] map, int cell ){
		int w = WindowGenerator.WIDTH;
		return cell >= 0 && cell + w < map.length && map[cell] == Terrain.WALL
				&& !DungeonTileSheet.wallStitcheable( map[cell + w] );
	}

	/** The dress tile of a metal's glint, in one of its versions. */
	public static int faceTile( Kind k, int version ){
		return OverworldDress.ORE_FACE + OverworldDress.ORE_VERSIONS * k.ordinal() + version;
	}

	/** The metal a dress tile shows, or null when it is no glint. */
	public static Kind faceKind( int id ){
		if (id < OverworldDress.ORE_FACE || id >= OverworldDress.ORE_FACE + OverworldDress.ORE_VERSIONS * Kind.values().length){
			return null;
		}
		return Kind.values()[(id - OverworldDress.ORE_FACE) / OverworldDress.ORE_VERSIONS];
	}

	private static int version( long seed, int wx, int wy ){
		return (int)Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, FACE_SALT ), OverworldDress.ORE_VERSIONS );
	}

	/** The glint one window cell wears, or -1: a vein (the window's vein map, Window.veins - null
	 *  on the surface) under a face that shows, with no canopy (a tree crown, a rock's peak)
	 *  hanging over it. canopy may be null. */
	public static int faceTileAt( long seed, int ox, int oy, int[] map, byte[] vein, int[] canopy, int cell ){
		int w = WindowGenerator.WIDTH;
		//the window's solid ring is never dressed (dressVeins)
		if (cell % w == 0 || cell % w == w - 1 || cell < w || cell >= map.length - w) return -1;
		if (vein == null || vein[cell] == 0 || !showsFace( map, cell )) return -1;
		if (canopy != null && cell < canopy.length && canopy[cell] != -1) return -1;
		return faceTile( Kind.values()[vein[cell] - 1], version( seed, ox + cell % w, oy + cell / w ) );
	}

	/**
	 * The dressing's last pass on a slice: every vein cell (the window's vein map, Window.veins -
	 * null on the surface) that is a WALL with its face showing, and nothing else drawn on it or
	 * hanging over it, wears its metal's glint. The map is the live one, so a vein mined in an
	 * earlier visit is floor by now and shows nothing. It only reads the vein map: cheap enough
	 * for the render thread, where a network mirror dresses its windows.
	 */
	public static void dressVeins( long seed, int ox, int oy, int[] map, byte[] vein, int[] ground, int[] canopy ){
		if (vein == null) return;
		final int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		for (int y = 1; y < h - 1; y++){
			for (int x = 1; x < w - 1; x++){
				int c = x + y * w;
				if (vein[c] == 0 || ground[c] != -1 || canopy[c] != -1 || !showsFace( map, c )) continue;
				ground[c] = faceTile( Kind.values()[vein[c] - 1], version( seed, ox + x, oy + y ) );
			}
		}
	}
}
