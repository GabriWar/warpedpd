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
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bananaspider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownBat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ColdSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSpinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Minotaur;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.WildCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Life on the surface: which beasts roam which biome, at which hour, in
 * which weather. A static table the overworld's populate pass rolls against,
 * plus the hour-change cull (the night's wolves do not pile up into the day)
 * and the night's fireflies - and on every slice above and below it: the
 * mountains' and the caves' bands, each with its own beasts, its own strength
 * and the ground they keep to (docs/world-layers.md, "Who lives where").
 */
public class OverworldFauna {

	/** The ground a row's beasts keep to. DRY: open dry floor (every surface row). POOL: the wadeable
	 *  shelf of a deep pool (WATER beside DEEP_WATER: the deep water itself is solid). CRYSTAL: floor
	 *  beside a crystal seam. FUNGUS: a mushroom patch within two cells. HEAT: hot ground (HOT_GROUND, as
	 *  the world laid it) within five. CLIFF: a drop (CHASM) within two. FROZEN: frozen ground (the
	 *  window's snow line). All but POOL are dry floor too. */
	public enum Habitat {
		DRY, POOL, CRYSTAL, FUNGUS, HEAT, CLIFF, FROZEN;
		public final int bit = 1 << ordinal();
	}

	//hot ground for the fire's creatures: embers, which the caves only hold where the world laid them
	//(the cave sites' rift); read off the pristine map, so a fire the hero lit draws none
	private static final int[] HOT_GROUND = { Terrain.EMBERS };
	private static final int FUNGUS_REACH = 2, HEAT_REACH = 5, CLIFF_REACH = 2;

	/** One row of the table: a beast, how often, when, in what numbers, and on what ground. */
	private static class Entry {
		final Class<? extends Mob> cls;
		final int weight;
		final EnumSet<Phase> phases;
		final int packMin, packMax;
		final Habitat habitat;

		Entry( Class<? extends Mob> cls, int weight, EnumSet<Phase> phases, int packMin, int packMax ){
			this( cls, weight, phases, packMin, packMax, Habitat.DRY );
		}

		Entry( Class<? extends Mob> cls, int weight, EnumSet<Phase> phases, int packMin, int packMax, Habitat habitat ){
			this.cls = cls;
			this.weight = weight;
			this.phases = phases;
			this.packMin = packMin;
			this.packMax = packMax;
			this.habitat = habitat;
		}

		boolean fits( Phase p, int hab ){
			return phases.contains( p ) && (hab & habitat.bit) != 0;
		}
	}

	/** A band of slices of the world and its table: the slices lo..hi, the weight of nothing and its
	 *  rows. Its depth-scaled beasts are tuned to the slice's WorldLayers.statDepth (Mob.setStatDepth). */
	private static class Band {
		final int lo, hi, empty;
		final Entry[] rows;

		Band( int lo, int hi, int empty, Entry... rows ){
			this.lo = lo;
			this.hi = hi;
			this.empty = empty;
			this.rows = rows;
		}
	}

	private static final EnumSet<Phase> DAYTIME  = EnumSet.of( Phase.DAWN, Phase.DAY );
	private static final EnumSet<Phase> DARK     = EnumSet.of( Phase.DUSK, Phase.NIGHT );
	private static final EnumSet<Phase> NIGHT    = EnumSet.of( Phase.NIGHT );
	private static final EnumSet<Phase> ANY      = EnumSet.allOf( Phase.class );

	private static final EnumMap<WorldModel.Biome, Entry[]> TABLE = new EnumMap<>( WorldModel.Biome.class );
	//weight of "nothing" per biome, rolled against the rows: the mountains
	//are mostly empty, a meadow is not
	private static final EnumMap<WorldModel.Biome, Integer> EMPTY = new EnumMap<>( WorldModel.Biome.class );

	//derived from the table: a beast that only ever appears after dark is
	//nocturnal, one that only ever appears by day is diurnal. the cull uses them
	private static final HashSet<Class<? extends Mob>> NOCTURNAL = new HashSet<>();
	private static final HashSet<Class<? extends Mob>> DIURNAL   = new HashSet<>();

	static {
		Entry[] frozen = {
				new Entry( GrayWolf.class,  3, DARK,    2, 3 ),
				new Entry( Yeti.class,      1, NIGHT,   1, 1 ),
				new Entry( AlbinoRat.class, 3, ANY,     1, 1 ),
		};
		TABLE.put( WorldModel.Biome.SNOWFIELD, frozen );
		TABLE.put( WorldModel.Biome.TUNDRA,    frozen );
		EMPTY.put( WorldModel.Biome.SNOWFIELD, 4 );
		EMPTY.put( WorldModel.Biome.TUNDRA,    3 );

		TABLE.put( WorldModel.Biome.FOREST, new Entry[]{
				new Entry( BrownWolf.class, 3, NIGHT,   2, 3 ),
				new Entry( Bunny.class,     4, DAYTIME, 1, 1 ),
				new Entry( Bat.class,       2, DARK,    1, 1 ),
				new Entry( Deer.class,      2, DAYTIME, 1, 2 ),
		} );
		EMPTY.put( WorldModel.Biome.FOREST, 2 );

		Entry[] grass = {
				new Entry( Bunny.class,     5, DAYTIME, 1, 1 ),
				new Entry( AlbinoRat.class, 3, NIGHT,   1, 1 ),
		};
		//the meadow's deer graze the open grass by day; the plains keep to rabbits and rats
		TABLE.put( WorldModel.Biome.MEADOW, new Entry[]{ grass[0], grass[1], new Entry( Deer.class, 2, DAYTIME, 1, 2 ) } );
		TABLE.put( WorldModel.Biome.PLAINS, grass );
		EMPTY.put( WorldModel.Biome.MEADOW, 1 );
		EMPTY.put( WorldModel.Biome.PLAINS, 2 );

		TABLE.put( WorldModel.Biome.DESERT, new Entry[]{
				new Entry( Scorpio.class,   2, DAYTIME, 1, 1 ),
				new Entry( Snake.class,     3, DARK,    1, 1 ),
		} );
		EMPTY.put( WorldModel.Biome.DESERT, 4 );

		TABLE.put( WorldModel.Biome.SWAMP, new Entry[]{
				new Entry( Snake.class,        3, ANY, 1, 1 ),
				new Entry( Slime.class,        2, ANY, 1, 1 ),
				new Entry( Bananaspider.class, 2, ANY, 1, 1 ),
		} );
		EMPTY.put( WorldModel.Biome.SWAMP, 2 );

		TABLE.put( WorldModel.Biome.BEACH, new Entry[]{
				new Entry( WildCrab.class,  4, DAYTIME, 1, 1 ),
				new Entry( Crab.class,      2, DAYTIME, 1, 1 ),
		} );
		EMPTY.put( WorldModel.Biome.BEACH, 2 );

		TABLE.put( WorldModel.Biome.FOOTHILLS, new Entry[]{
				new Entry( Bat.class,       3, DARK,    1, 1 ),
				new Entry( GrayWolf.class,  2, NIGHT,   2, 2 ),
		} );
		EMPTY.put( WorldModel.Biome.FOOTHILLS, 4 );

		TABLE.put( WorldModel.Biome.MOUNTAIN, new Entry[]{
				new Entry( Yeti.class,      1, ANY,     1, 1 ),
		} );
		EMPTY.put( WorldModel.Biome.MOUNTAIN, 9 );
	}

	//where the deep caves begin, for the ambience's cave life (unit ambience); the fauna's own tables
	//go by the bands below
	public static final int DEEP_CAVES = -5;

	/*
	 * The slices' bands (docs/world-layers.md, "Who lives where"). Weights are relative within a band;
	 * a row rolls only on ground of its habitat, so the seam-, pool-, grove-, fire-, cliff- and
	 * ice-dwellers come out where that ground is and nowhere else. The cave rows keep every hour (no
	 * sky); the mountains' keep the surface's hours and the hour cull. Each band has its own strength:
	 * the depth-scaled beasts (bats, rats, piranhas, shamans, elementals, wraiths) draw theirs from the
	 * band's depth (WorldLayers.statDepth), not from the slices' own depth numbers (87..112), and the rest have fixed
	 * stats - so the deeper and the higher, the harder.
	 *
	 * Left out on purpose: the ghoul (its revival keeps a raw cell index a rebase never moves), the
	 * DM-200 (its gas reads the slice's raw depth), lichen and beetles (plant allies), larvae
	 * (Yog's), the gnoll geomancer and sapper (the mine quest's), the oni (its health reads the raw depth: 1300 at -11),
	 * the ice guardian (the frozen core's), the phantom piranha (the cave key's), the armoured brute
	 * (armour every kill) and the fungal sentry (a quest miniboss). Bats stay in the caves: with no
	 * coat for the cold they would freeze on a mountain night.
	 */
	private static final Band[] BANDS = {
			//the shallow caves
			new Band( -3, -1, 3,
					new Entry( Bat.class,            3, ANY, 1, 2 ),
					new Entry( BrownBat.class,       2, ANY, 2, 3 ),
					new Entry( AlbinoRat.class,      3, ANY, 1, 2 ),
					new Entry( Slime.class,          2, ANY, 1, 1 ),
					new Entry( Snake.class,          2, ANY, 1, 1 ),
					new Entry( Swarm.class,          1, ANY, 1, 1 ),
					new Entry( Spinner.class,        1, ANY, 1, 1 ) ),
			//the middle caves: gnolls and their shamans, the old dead, the wisps of the crystal
			//seams and the pale fish of the deep pools
			new Band( -6, -4, 3,
					new Entry( Bat.class,            3, ANY, 1, 3 ),
					new Entry( Brute.class,          3, ANY, 1, 2 ),
					new Entry( Shaman.RedShaman.class,    1, ANY, 1, 1 ),
					new Entry( Shaman.BlueShaman.class,   1, ANY, 1, 1 ),
					new Entry( Shaman.PurpleShaman.class, 1, ANY, 1, 1 ),
					new Entry( Spinner.class,        2, ANY, 1, 1 ),
					new Entry( ClayGolem.class,      2, ANY, 1, 1 ),
					new Entry( FossilSkeleton.class, 2, ANY, 1, 2 ),
					new Entry( CrystalWisp.class,    6, ANY, 1, 2, Habitat.CRYSTAL ),
					new Entry( AlbinoPiranha.class,  2, ANY, 1, 1, Habitat.POOL ) ),
			//the deep caves: golems, and the spinners of the mushroom groves
			new Band( -9, -7, 3,
					new Entry( Golem.class,          3, ANY, 1, 1 ),
					new Entry( Brute.class,          2, ANY, 2, 2 ),
					new Entry( Spinner.class,        2, ANY, 1, 2 ),
					new Entry( FossilSkeleton.class, 2, ANY, 2, 3 ),
					new Entry( Bat.class,            2, ANY, 2, 3 ),
					new Entry( FungalSpinner.class,  6, ANY, 1, 2, Habitat.FUNGUS ),
					new Entry( CrystalWisp.class,    4, ANY, 1, 2, Habitat.CRYSTAL ),
					new Entry( AlbinoPiranha.class,  2, ANY, 1, 1, Habitat.POOL ) ),
			//the abyss: golems and the restless dead, now and then a minotaur or a grey oni, and the
			//fire's own creatures where the rock runs hot
			new Band( -12, -10, 4,
					new Entry( Golem.class,         16, ANY, 1, 2 ),
					new Entry( Wraith.class,         8, ANY, 1, 2 ),
					new Entry( Minotaur.class,       1, ANY, 1, 1 ),
					new Entry( GreyOni.class,        1, ANY, 1, 1 ),
					new Entry( Elemental.FireElemental.class, 24, ANY, 1, 1, Habitat.HEAT ),
					new Entry( LostSoul.class,      20, ANY, 1, 2, Habitat.HEAT ),
					new Entry( CrystalWisp.class,   16, ANY, 2, 2, Habitat.CRYSTAL ),
					new Entry( AlbinoPiranha.class,  3, ANY, 1, 1, Habitat.POOL ) ),
			//the high pastures: the wolves' nights, the hares' days, and the goats on the cliffs' lips
			new Band( 1, 3, 3,
					new Entry( GrayWolf.class,       3, DARK,    2, 3 ),
					new Entry( Bunny.class,          3, DAYTIME, 1, 1 ),
					new Entry( Goat.class,           5, DAYTIME, 1, 3, Habitat.CLIFF ) ),
			//the snowfields: yetis, and the icy things where the snow lies
			new Band( 4, 7, 4,
					new Entry( GrayWolf.class,       3, DARK,    2, 4 ),
					new Entry( Yeti.class,           2, ANY,     1, 1 ),
					new Entry( ColdSpirit.class,     2, ANY,     1, 1, Habitat.FROZEN ),
					new Entry( KoboldIcemancer.class, 2, ANY,    1, 2, Habitat.FROZEN ),
					new Entry( Goat.class,           2, DAYTIME, 1, 2, Habitat.CLIFF ) ),
			//the peaks: yetis, in pairs by night, ice demons, and the cold's own spirits
			new Band( 8, 10, 3,
					new Entry( Yeti.class,           3, ANY,     1, 2 ),
					new Entry( Yeti.class,           2, NIGHT,   2, 2 ),
					new Entry( IceDemon.class,       2, ANY,     1, 1, Habitat.FROZEN ),
					new Entry( Elemental.FrostElemental.class, 1, ANY, 1, 1, Habitat.FROZEN ),
					new Entry( ColdSpirit.class,     2, ANY,     2, 4, Habitat.FROZEN ) ),
	};

	//the ground each beast of the slices keeps to (one kind of ground per beast across the bands)
	private static final HashMap<Class<? extends Mob>, Habitat> HOME = new HashMap<>();

	static {
		//the hours a beast keeps: from the surface and the slices under the sky, never the caves'
		//(a cave's bats are out at any hour, which would make the surface's bats keep any hour too)
		HashMap<Class<? extends Mob>, EnumSet<Phase>> hours = new HashMap<>();
		ArrayList<Entry[]> sky = new ArrayList<>( TABLE.values() );
		for (Band b : BANDS) if (b.lo > 0) sky.add( b.rows );
		for (Entry[] rows : sky){
			for (Entry e : rows){
				EnumSet<Phase> seen = hours.get( e.cls );
				if (seen == null) hours.put( e.cls, EnumSet.copyOf( e.phases ) );
				else seen.addAll( e.phases );
			}
		}
		for (HashMap.Entry<Class<? extends Mob>, EnumSet<Phase>> h : hours.entrySet()){
			if (DARK.containsAll( h.getValue() ))    NOCTURNAL.add( h.getKey() );
			if (DAYTIME.containsAll( h.getValue() )) DIURNAL.add( h.getKey() );
		}

		//every beast of the slices keeps to one kind of ground wherever it lives
		for (Band b : BANDS){
			for (Entry e : b.rows) HOME.put( e.cls, e.habitat );
		}
	}

	//the band a slice belongs to, or null for the surface (and anything off the stack)
	private static Band band( int altitude ){
		for (Band b : BANDS) if (altitude >= b.lo && altitude <= b.hi) return b;
		return null;
	}

	/** The depth a slice's fauna is tuned to (Mob.setStatDepth), -1 for the surface, which is not tuned. */
	public static int statDepth( int altitude ){
		return band( altitude ) == null ? -1 : WorldLayers.statDepth( altitude );
	}

	/** The kinds of a slice's band in row order, each once (scenes, tests); empty for the surface. */
	public static ArrayList<Class<? extends Mob>> kindsAt( int altitude ){
		ArrayList<Class<? extends Mob>> out = new ArrayList<>();
		Band b = band( altitude );
		if (b == null) return out;
		for (Entry e : b.rows) if (!out.contains( e.cls )) out.add( e.cls );
		return out;
	}

	/** The ground a beast of the slices keeps to: DRY for anything the bands do not know. A kind the
	 *  bands do not list keeps to its parent's ground (a shrine's piranha is still a fish). */
	public static Habitat home( Class<? extends Mob> cls ){
		for (Class<?> c = cls; c != null && Mob.class.isAssignableFrom( c ); c = c.getSuperclass()){
			Habitat h = HOME.get( c );
			if (h != null) return h;
		}
		return Habitat.DRY;
	}

	/** Does the table know this beast at all? Counts toward the fauna cap. */
	public static boolean isFauna( Mob m ){
		for (Entry[] rows : TABLE.values()){
			for (Entry e : rows){
				if (e.cls.isInstance( m )) return true;
			}
		}
		for (Band b : BANDS){
			for (Entry e : b.rows){
				if (e.cls.isInstance( m )) return true;
			}
		}
		return false;
	}

	/**
	 * The kinds of ground a window cell is (Habitat bits), 0 where nothing of the table may stand:
	 * rock, and water other than a deep pool's shelf. The surface's dry land is DRY alone. Pure, so
	 * the tests can ask it of a hand-made map; `world` is what the world laid there (the pristine
	 * map), the hot ground's only source, or null.
	 */
	static int habitat( int[] map, int[] world, int w, int h, int cell, boolean frozen, int altitude ){
		int t = map[cell];
		if ((Terrain.flags[t] & Terrain.PASSABLE) == 0) return 0;
		int x = cell % w, y = cell / w;
		if ((Terrain.flags[t] & Terrain.LIQUID) != 0){
			if (altitude == 0 || t != Terrain.WATER) return 0;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx >= 0 && ny >= 0 && nx < w && ny < h && map[nx + ny * w] == Terrain.DEEP_WATER) return Habitat.POOL.bit;
				}
			}
			return 0;
		}
		int m = Habitat.DRY.bit;
		if (altitude == 0) return m;
		for (int dy = -HEAT_REACH; dy <= HEAT_REACH; dy++){
			for (int dx = -HEAT_REACH; dx <= HEAT_REACH; dx++){
				int nx = x + dx, ny = y + dy;
				if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
				int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
				int n = map[nx + ny * w];
				if (d == 1 && n == Terrain.MINE_CRYSTAL) m |= Habitat.CRYSTAL.bit;
				if (d <= FUNGUS_REACH && n == Terrain.MUSHROOM_PATCH) m |= Habitat.FUNGUS.bit;
				if (d <= CLIFF_REACH && n == Terrain.CHASM) m |= Habitat.CLIFF.bit;
				if (world != null){
					int g = world[nx + ny * w];
					for (int hot : HOT_GROUND) if (g == hot) m |= Habitat.HEAT.bit;
				}
			}
		}
		if (frozen) m |= Habitat.FROZEN.bit;
		return m;
	}

	/** ...of a cell of a slice's window. */
	public static int habitat( OverworldLevel l, int cell ){
		return habitat( l.map, l.pristine(), l.width(), l.height(), cell, l.frozenAt( cell ), l.altitude() );
	}

	/** Is this cell ground the beast keeps to? A pack member is only set down where its kind lives. */
	public static boolean livesAt( OverworldLevel l, Mob m, int cell ){
		return (habitat( l, cell ) & home( m.getClass() ).bit) != 0;
	}

	/** Can it stand on this cell at all? A fish only in the water: a parked beast coming back into the
	 *  window takes a cell near its own, which for a fish may be dry land. */
	public static boolean standsAt( OverworldLevel l, Mob m, int cell ){
		return home( m.getClass() ) != Habitat.POOL || l.water[cell];
	}

	/**
	 * Inside a settlement's berth (its radius plus three): no wildlife in the
	 * streets. A metropolis can reach half a sector past its own, so the
	 * neighbouring sectors are asked too.
	 */
	public static boolean nearSettlement( long seed, int wx, int wy ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR );
		int sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int sy = sy0-1; sy <= sy0+1; sy++){
			for (int sx = sx0-1; sx <= sx0+1; sx++){
				if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int berth = WorldStructures.settlementLayout( seed, sx, sy )[0] + 3;
				if (Math.abs( WorldStructures.siteX( seed, sx, sy ) - wx ) <= berth
						&& Math.abs( WorldStructures.siteY( seed, sx, sy ) - wy ) <= berth){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * How much the weather lets out: nothing in a storm or a blizzard, half
	 * of it in the rain, everything otherwise.
	 */
	public static float weatherFactor(){
		if (ClimateManager.isStorming()) return 0f;
		if (ClimateManager.localPrecipType() == PrecipType.BLIZZARD && ClimateManager.localPrecipRate() > 0f) return 0f;
		//snow driven hard enough is a blizzard whatever the front calls itself
		if (ClimateManager.isSnowing() && ClimateManager.localWindSpeed() > 12f) return 0f;
		if (ClimateManager.isRaining()) return 0.5f;
		return 1f;
	}

	/**
	 * Rolls the table for one candidate cell: the beasts to put down there
	 * (a pack, or one), or an empty list when the hour, the weather or the
	 * empty-weight says nothing stirs.
	 */
	public static ArrayList<Mob> roll( WorldModel.Biome biome, Phase phase ){
		return roll( 0, biome, phase, Habitat.DRY.bit );
	}

	/** ...on a slice of the world, for a cell of this ground (habitat bits): the mountains and the
	 *  caves keep their own bands, and a row rolls only on its own ground. */
	public static ArrayList<Mob> roll( int altitude, WorldModel.Biome biome, Phase phase, int habitat ){
		ArrayList<Mob> out = new ArrayList<>();
		Entry[] rows;
		int empty;
		Band b = band( altitude );
		if (b != null){
			rows = b.rows;
			empty = b.empty;
		} else {
			rows = TABLE.get( biome );
			if (rows == null) return out;
			empty = EMPTY.get( biome );
		}
		//the weather stays out of the caves
		float weather = altitude < 0 ? 1f : weatherFactor();
		if (weather <= 0f || Random.Float() >= weather) return out;
		int total = empty;
		for (Entry e : rows) if (e.fits( phase, habitat )) total += e.weight;
		int pick = Random.Int( total );
		for (Entry e : rows){
			if (!e.fits( phase, habitat )) continue;
			pick -= e.weight;
			if (pick < 0){
				int n = Random.IntRange( e.packMin, e.packMax );
				for (int i = 0; i < n; i++){
					Mob m = Reflection.newInstance( e.cls );
					prepare( m, altitude );
					out.add( m );
				}
				return out;
			}
		}
		return out;
	}

	/** What a beast needs beyond being made: the swamp's spiders their size; on a slice, the
	 *  depth-scaled ones their band's strength (Mob.setStatDepth), the restless dead their wandering,
	 *  and the high pastures' hares their winter coat. */
	public static void prepare( Mob m, int altitude ){
		if (m instanceof Bananaspider) ((Bananaspider) m).spawn( 3 );
		if (band( altitude ) == null) return;
		m.setStatDepth( WorldLayers.statDepth( altitude ) );
		if (m instanceof Wraith) m.state = m.WANDERING;
		if (m instanceof Bunny && altitude > 0) ((Bunny) m).setAlpine();
	}

	/** A wolf pack: the howl is theirs. */
	public static boolean isPack( ArrayList<Mob> spawned ){
		return spawned.size() >= 2
				&& (spawned.get( 0 ) instanceof GrayWolf || spawned.get( 0 ) instanceof BrownWolf);
	}

	//the night the pack last howled: cycle index, so it is once a night
	private static long howledNight = Long.MIN_VALUE;

	/** A night pack spawned close enough to matter: one warning per night. */
	public static void howl(){
		long night = Math.floorDiv( Dungeon.cycleTurn, DayNightCycle.FULL_CYCLE );
		if (night == howledNight) return;
		howledNight = night;
		GLog.w( Messages.get( OverworldFauna.class, "howl" ) );
	}

	/**
	 * The hour-change cull: by day (dawn included) nocturnal beasts that are
	 * out of sight and more than 20 cells from the hero are gone; after dusk
	 * the same goes for the day's. Runs on the live level only.
	 */
	public static void cull( OverworldLevel level ){
		if (Dungeon.level != level || Dungeon.hero == null || level.heroFOV == null || !level.openSky()) return;
		HashSet<Class<? extends Mob>> gone = DARK.contains( DayNightCycle.phase() ) ? DIURNAL : NOCTURNAL;
		int w = level.width();
		int hx = Dungeon.hero.pos % w, hy = Dungeon.hero.pos / w;
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!gone.contains( m.getClass() )) continue;
			//a world event's beasts are the event's to end (HuntEvent): never culled mid-hunt
			if (m.buff( HuntPack.class ) != null) continue;
			if (m.pos < 0 || m.pos >= level.heroFOV.length || level.heroFOV[m.pos]) continue;
			if (Math.abs( m.pos % w - hx ) <= 20 && Math.abs( m.pos / w - hy ) <= 20) continue;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
	}

	/**
	 * Fireflies at night: a few drifting lights around the hero each step,
	 * none in the rain. Two or three over the swamp any night; over a summer
	 * meadow or wood one now and then. Cheap: at most three particles a step.
	 * Render thread: the critters' Field stirs them, on every machine about
	 * its own hero, with its own dice (never the dungeon's seeded ones).
	 */
	public static void fireflies( OverworldLevel level, int heroPos, java.util.Random rng ){
		if (!DayNightCycle.isNight() || ClimateManager.isRaining() || ClimateManager.isStorming()) return;
		scatterFireflies( level, heroPos, fireflyCount( level.biomeAtCell( heroPos ), GameCalendar.season(), rng ), rng );
	}

	/** How many lights a step stirs: two or three over the swamp, none or one over a summer meadow or wood. */
	static int fireflyCount( WorldModel.Biome biome, GameCalendar.Season season, java.util.Random rng ){
		if (biome == WorldModel.Biome.SWAMP) return 2 + rng.nextInt( 2 );
		if (season == GameCalendar.Season.SUMMER
				&& (biome == WorldModel.Biome.MEADOW || biome == WorldModel.Biome.FOREST)) return rng.nextInt( 2 );
		return 0;
	}

	/** n fireflies on open ground or water within six cells of the hero (the critter scenes light a swarm). Render thread. */
	public static void scatterFireflies( OverworldLevel level, int heroPos, int n, java.util.Random rng ){
		int w = level.width(), h = level.height();
		for (int i = 0; i < n; i++){
			int x = heroPos % w + rng.nextInt( 13 ) - 6;
			int y = heroPos / w + rng.nextInt( 13 ) - 6;
			if (x <= 0 || y <= 0 || x >= w-1 || y >= h-1) continue;
			int cell = x + y * w;
			if (!level.passable[cell] && !level.water[cell]) continue;
			//this machine's own lights: CellEmitter.get would tag the emitter, and a host would
			//ship the burst to clients that light their own (a recycled emitter keeps its tag)
			Emitter e = GameScene.emitter();
			e.netCell = -1;
			PointF p = DungeonTilemap.tileToWorld( cell );
			e.pos( p.x, p.y, DungeonTilemap.SIZE, DungeonTilemap.SIZE );
			e.burst( FireflyParticle.FACTORY, 1 );
		}
	}
}
