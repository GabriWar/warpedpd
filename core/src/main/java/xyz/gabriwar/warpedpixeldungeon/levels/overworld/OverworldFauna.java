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
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bananaspider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.WildCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
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
 * and the swamp's fireflies.
 */
public class OverworldFauna {

	/** One row of the table: a beast, how often, when, and in what numbers. */
	private static class Entry {
		final Class<? extends Mob> cls;
		final int weight;
		final EnumSet<Phase> phases;
		final int packMin, packMax;

		Entry( Class<? extends Mob> cls, int weight, EnumSet<Phase> phases, int packMin, int packMax ){
			this.cls = cls;
			this.weight = weight;
			this.phases = phases;
			this.packMin = packMin;
			this.packMax = packMax;
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
		} );
		EMPTY.put( WorldModel.Biome.FOREST, 2 );

		Entry[] grass = {
				new Entry( Bunny.class,     5, DAYTIME, 1, 1 ),
				new Entry( AlbinoRat.class, 3, NIGHT,   1, 1 ),
		};
		TABLE.put( WorldModel.Biome.MEADOW, grass );
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

		HashMap<Class<? extends Mob>, EnumSet<Phase>> hours = new HashMap<>();
		for (Entry[] rows : TABLE.values()){
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
	}

	/** Does the table know this beast at all? Counts toward the fauna cap. */
	public static boolean isFauna( Mob m ){
		for (Entry[] rows : TABLE.values()){
			for (Entry e : rows){
				if (e.cls.isInstance( m )) return true;
			}
		}
		return false;
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
		ArrayList<Mob> out = new ArrayList<>();
		Entry[] rows = TABLE.get( biome );
		if (rows == null) return out;

		float weather = weatherFactor();
		if (weather <= 0f || Random.Float() >= weather) return out;

		int total = EMPTY.get( biome );
		for (Entry e : rows) if (e.phases.contains( phase )) total += e.weight;
		int pick = Random.Int( total );
		for (Entry e : rows){
			if (!e.phases.contains( phase )) continue;
			pick -= e.weight;
			if (pick < 0){
				int n = Random.IntRange( e.packMin, e.packMax );
				for (int i = 0; i < n; i++){
					Mob m = Reflection.newInstance( e.cls );
					if (m instanceof Bananaspider) ((Bananaspider) m).spawn( 3 );
					out.add( m );
				}
				return out;
			}
		}
		return out;
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
		if (Dungeon.level != level || Dungeon.hero == null || level.heroFOV == null) return;
		HashSet<Class<? extends Mob>> gone = DARK.contains( DayNightCycle.phase() ) ? DIURNAL : NOCTURNAL;
		int w = level.width();
		int hx = Dungeon.hero.pos % w, hy = Dungeon.hero.pos / w;
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!gone.contains( m.getClass() )) continue;
			if (m.pos < 0 || m.pos >= level.heroFOV.length || level.heroFOV[m.pos]) continue;
			if (Math.abs( m.pos % w - hx ) <= 20 && Math.abs( m.pos / w - hy ) <= 20) continue;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
	}

	/**
	 * Fireflies over the swamp at night: a few drifting lights around the
	 * hero each step, none in the rain. Cheap: two or three particles a turn.
	 */
	public static void fireflies( OverworldLevel level, int heroPos ){
		if (!DayNightCycle.isNight()
				|| level.biomeAtCell( heroPos ) != WorldModel.Biome.SWAMP
				|| ClimateManager.isRaining() || ClimateManager.isStorming()) return;
		int w = level.width(), h = level.height();
		int n = Random.IntRange( 2, 3 );
		for (int i = 0; i < n; i++){
			int x = heroPos % w + Random.IntRange( -6, 6 );
			int y = heroPos / w + Random.IntRange( -6, 6 );
			if (x <= 0 || y <= 0 || x >= w-1 || y >= h-1) continue;
			int cell = x + y * w;
			if (!level.passable[cell] && !level.water[cell]) continue;
			CellEmitter.get( cell ).burst( FireflyParticle.FACTORY, 1 );
		}
	}
}
