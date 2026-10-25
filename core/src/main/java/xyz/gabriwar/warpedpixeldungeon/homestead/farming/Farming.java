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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore;
import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarScythe;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 * The fields of the world's slices: sowing a seed on farmland, growing it through the seasons and
 * the hero's absences, harvesting it with the sickle, and losing it to the frost, to pests or to
 * the ground being taken from under it. The crop's record (Crop, kept by the slice's FarmSlice) is
 * the crop; the FarmCrop on its cell shows it and is touched in its stead.
 *
 * Growth: a crop grows a turn of its cycle for every turn of the clock at its season's pace
 * (FarmRules.seasonRate: spring 1.25, summer 1, autumn 0.75, winter nothing), at summer's all year
 * under glass (a glass roof over its cell, or a room labelled a greenhouse). Water beside it
 * shortens its cycles and its odds of withering (FarmRules.water). Its first cycle ripens it; it
 * waits for the sickle then, and each cycle after drops a seed beside it or withers it, until it
 * has dropped its last.
 *
 * Frost: a crop not under glass dies when its cell's temperature falls below FarmRules.FROST_C
 * while the hero is there, and of any winter day that went by while he was not
 * (FarmRules.winterBetween) - unless it stands in a room (Rooms) or on a slice with no sky over
 * it (the caves), whose air the winter does not reach in his absence.
 */
public final class Farming {

	private Farming(){}

	/** What came of tending a crop. */
	public enum Outcome { GROWING, WITHERED, SPENT, FROSTED, EATEN, UPROOTED,
		//a flood over its bank, a wildfire beside it (levels/overworld: FloodEvent, WildfireEvent)
		DROWNED, SCORCHED }

	/** This slice's fields, null anywhere else. */
	public static FarmSlice fields( Level level ){
		HomesteadStore store = Homestead.store( level );
		return store == null ? null : store.section( FarmSlice.class );
	}

	/** The crop growing on a window cell of a slice, null for none. */
	public static Crop cropAt( Level level, int cell ){
		FarmSlice f = fields( level );
		if (f == null || cell < 0 || cell >= level.length()) return null;
		OverworldLevel ow = (OverworldLevel) level;
		return f.at( ow.worldX + cell % ow.width(), ow.worldY + cell / ow.width() );
	}

	/** The FarmCrop standing on a cell, null for none. */
	public static FarmCrop mobAt( Level level, int cell ){
		for (Mob m : level.mobs){
			if (m.pos == cell && m instanceof FarmCrop) return (FarmCrop) m;
		}
		return null;
	}

	// ----------------------------------------------------------------- sowing

	/**
	 * Level.plant on a world slice: a seed sown (or thrown) on farmland becomes a crop there. The
	 * hero sowing the plot he stands on (or a seed landing on a crop) sows the free farmland beside
	 * it; with none free the seed is handed back. False, having done nothing, off the slices' farmland (the seed is
	 * planted the usual way) and under the no-herbalism challenge.
	 */
	public static boolean sow( Level level, Plant.Seed seed, int pos ){
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror() || fields( level ) == null
				|| pos < 0 || pos >= level.length() || !BuiltTerrain.farmland( level.map[pos] )
				|| Dungeon.isChallenged( Challenges.NO_HERBALISM )) return false;
		OverworldLevel ow = (OverworldLevel) level;
		int at = -1;
		Char here = Actor.findChar( pos );
		if (here == null && cropAt( ow, pos ) == null){
			at = pos;
		} else {
			//the hero on his own plot (the Plant verb sows where he stands), or a seed thrown onto a
			//crop: the free farmland beside it
			for (int d : PathFinder.NEIGHBOURS8){
				int c = pos + d;
				if (c >= 0 && c < ow.length() && BuiltTerrain.farmland( ow.map[c] )
						&& Actor.findChar( c ) == null && cropAt( ow, c ) == null){
					at = c;
					break;
				}
			}
		}
		if (at == -1){
			GLog.w( Messages.get( FarmCrop.class, "no_room" ) );
			Heap h = ow.drop( Reflection.newInstance( seed.getClass() ), pos );
			if (h.sprite != null) h.sprite.drop();
			return true;
		}
		plant( ow, at, seed.getClass() );
		if (live( ow )){
			if (ow.heroFOV[at]){
				SpatialSound.play( Assets.Sounds.PLANT, at );
				CellEmitter.get( at ).burst( LeafParticle.GENERAL, 8 );
			}
			//the guide's page on the fields
			xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.reveal( xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph.TAG_FARM );
		}
		return true;
	}

	/** Puts a crop of a seed in the ground on a cell of a slice, no questions asked (sow checks;
	 *  a debug scene or a test plants directly): its record and its FarmCrop. */
	public static FarmCrop plant( OverworldLevel ow, int cell, Class<? extends Plant.Seed> seed ){
		Crop c = new Crop( seed, FarmCrop.clock(), WorldClock.day() );
		c.wx = ow.worldX + cell % ow.width();
		c.wy = ow.worldY + cell / ow.width();
		fields( ow ).put( c );
		water( ow, cell );
		return show( ow, cell, c );
	}

	//the FarmCrop that shows a crop on its cell
	static FarmCrop show( OverworldLevel ow, int cell, Crop c ){
		FarmCrop m = new FarmCrop();
		m.crop = c;
		m.pos = cell;
		if (live( ow )) GameScene.add( m );
		else ow.mobs.add( m );
		return m;
	}

	/** Does this FarmCrop stand for the crop recorded on its cell (the same sowing)? It takes the
	 *  record over when it carried a copy (a save's). False when no crop of its own is there: it
	 *  is a stray (its crop is gone, or it was set down off its own cell) and leaves. */
	public static boolean owns( OverworldLevel ow, FarmCrop m ){
		Crop rec = cropAt( ow, m.pos );
		if (rec == null || !same( rec, m.crop )) return false;
		m.crop = rec;
		return true;
	}

	/** Are two records of the same sowing (the same seed, sown at the same moment)? */
	public static boolean same( Crop a, Crop b ){
		return a == b || (a != null && b != null && a.seed == b.seed && a.planted == b.planted);
	}

	// ----------------------------------------------------------------- growing

	/** The water a crop on this cell has (FarmRules.water). */
	public static int waterAt( Level level, int cell ){
		return FarmRules.water( level.map, level.width(), cell );
	}

	/** Is this cell under glass: a glass roof over it, or in a room labelled a greenhouse
	 *  (FarmRooms, or any part's). Summer's pace all year there, and no frost. */
	public static boolean greenhouse( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return false;
		if (Homestead.roofAt( level, cell ) == RoofTiles.GLASS) return true;
		Rooms.Room room = Rooms.at( level, cell );
		if (room == null) return false;
		OverworldLevel ow = (OverworldLevel) level;
		synchronized (glassRooms){
			HomesteadStore store = ow.homestead();
			if (store != glassStore || store.version() != glassVersion){
				glassRooms.clear();
				glassStore = store;
				glassVersion = store.version();
			}
			Boolean yes = glassRooms.get( room );
			if (yes == null){
				yes = RoomTraits.is( ow, room, FarmRooms.GREENHOUSE );
				glassRooms.put( room, yes );
			}
			return yes;
		}
	}

	//the rooms' greenhouse answers, by room, as of the slice's store version they were worked out
	//at (a roof or a wall changing changes it): every crop asks each turn, and a room's traits look
	//over every cell of it. A room flooded again is a new Room, asked afresh
	private static final IdentityHashMap<Rooms.Room, Boolean> glassRooms = new IdentityHashMap<>();
	private static HomesteadStore glassStore;
	private static int glassVersion;

	/** The pace a crop on this cell grows at today (growth a turn): the season's, or summer's
	 *  under glass; off the slices always 1. */
	public static float rateAt( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return 1f;
		return FarmRules.seasonRate( GameCalendar.season(), greenhouse( level, cell ) );
	}

	/**
	 * Brings a crop up to date with the clock: the frost first (a crop in the open, see the class),
	 * then the growth since it was last tended, then every cycle that growth completed - the first
	 * ripens it, each after that withers it (its die against FarmRules.witherChance) or drops a seed
	 * beside it, until the last seed spends it. Off the world's slices there are no seasons and no
	 * frost. `live`: the hero is here and the crop is acting (the cell's temperature is asked);
	 * the settling of an absence (FarmSettler) tends it with `live` false. Changes only the record:
	 * the caller takes a crop that did not come out GROWING away (dies).
	 */
	public static Outcome tend( Level level, int cell, Crop c, boolean live ){
		Class<? extends Plant> plant = c.plant();
		if (plant == null) return Outcome.UPROOTED;
		float now = FarmCrop.clock();
		int today = WorldClock.day();
		float dt = now - c.clock;
		if (dt <= 0f){
			c.clock = now;
			return Outcome.GROWING;
		}
		boolean world = level instanceof OverworldLevel;
		boolean away = dt > FarmRules.AWAY_TURNS;
		boolean glass = world && greenhouse( level, cell );
		if (world && !glass){
			//away: the winter's days kill it in the open air only
			boolean sheltered = !((OverworldLevel) level).openSky() || Rooms.at( level, cell ) != null;
			if (away && !sheltered && FarmRules.winterBetween( c.day, today )) return Outcome.FROSTED;
			if (!away && live && level == Dungeon.level && TileTemperature.tileTemp( cell ) < FarmRules.FROST_C){
				return Outcome.FROSTED;
			}
		}
		c.growth += world ? FarmRules.growth( dt, c.day, today, away, glass ) * silt( level, cell ) : dt;
		c.clock = now;
		c.day = Math.max( c.day, today );

		int water = waterAt( level, cell );
		long seed = world ? ((OverworldLevel) level).worldSeed() : Dungeon.seed;
		long key = world ? OverworldLevel.worldKey( c.wx, c.wy ) : cell;
		while (true){
			int len = FarmRules.cycleTurns( plant, water );
			if (c.growth < len) break;
			c.growth -= len;
			c.fed = 0;
			c.cycles++;
			//the first cycle ripens it: it waits for the sickle
			if (c.cycles == 1) continue;
			if (FarmRules.roll( seed, key, c.planted, c.cycles ) < FarmRules.witherChance( plant, water )){
				return Outcome.WITHERED;
			}
			dropSeed( level, cell, c );
			c.seeds++;
			if (c.seeds >= FarmRules.maxSeeds( plant )) return Outcome.SPENT;
		}
		return Outcome.GROWING;
	}

	//the silt a flood left on the crop's bank: it grows faster on it for a while (FloodEvent)
	private static float silt( Level level, int cell ){
		return level instanceof OverworldLevel
				? xyz.gabriwar.warpedpixeldungeon.levels.overworld.FloodEvent.siltFactor( (OverworldLevel) level, cell, WorldClock.turn() )
				: 1f;
	}

	/** Does an untended crop wait for its home to be settled (FarmSettler), rather than catch up
	 *  by itself: left alone (FarmRules.AWAY_TURNS) in the fields of a home the hero has not been
	 *  back to yet. The settling tends it, and tells him what came of it. */
	public static boolean waiting( Level level, Crop c ){
		return level instanceof OverworldLevel && FarmCrop.clock() - c.clock > FarmRules.AWAY_TURNS
				&& FarmSettler.pending( (OverworldLevel) level, c.wx, c.wy );
	}

	//a ripe crop's seed of its own, on a free cell beside it (or its own)
	private static void dropSeed( Level level, int cell, Crop c ){
		Item s = Reflection.newInstance( c.seed );
		if (s == null) return;
		ArrayList<Integer> spots = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int m = cell + n;
			if (m >= 0 && m < level.length() && level.passable[m] && Actor.findChar( m ) == null && level.heaps.get( m ) == null){
				spots.add( m );
			}
		}
		//the first free one round it from the north, clockwise: the crop's own dice are spent on
		//its withering, and the settling of an absence must come out the same every time
		int spot = spots.isEmpty() ? cell : spots.get( 0 );
		Heap h = level.drop( s, spot );
		if (h.sprite != null) h.sprite.drop();
		if (live( level ) && level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( ShaftParticle.FACTORY, 4 );
			GLog.p( Messages.get( FarmCrop.class, "yield", name( c ) ) );
		}
	}

	/** Plant food on a crop: FarmRules.FOOD_SHARE of the growth left in its cycle. False when
	 *  it would not help (ripe, or under a turn left). */
	public static boolean feed( Level level, int cell, Crop c ){
		Class<? extends Plant> plant = c.plant();
		if (plant == null || c.ripe()) return false;
		float add = (FarmRules.cycleTurns( plant, waterAt( level, cell ) ) - c.growth) * FarmRules.FOOD_SHARE;
		if (add < 1f) return false;
		c.growth += add;
		c.fed += add;
		if (live( level ) && level.heroFOV[cell]) CellEmitter.get( cell ).burst( LeafParticle.GENERAL, 5 );
		return true;
	}

	/** The name of what a crop grows (its plant's), for the messages. */
	public static String name( Crop c ){
		Class<? extends Plant> plant = c.plant();
		Plant p = plant == null ? null : Reflection.newInstance( plant );
		return p != null ? p.name() : Messages.get( FarmCrop.class, "name_generic" );
	}

	// ----------------------------------------------------------------- the end of a crop

	/**
	 * A crop comes out of the ground: its record goes, and its FarmCrop with it - withered, spent,
	 * frozen, eaten or uprooted, as the outcome says, the hero told when he sees it (`tell`).
	 */
	public static void dies( Level level, int cell, Crop c, Outcome how, boolean tell ){
		FarmSlice f = fields( level );
		if (f != null) f.remove( c.wx, c.wy );
		FarmCrop m = mobAt( level, cell );
		if (m != null && same( m.crop, c )){
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
		if (!tell || !live( level ) || !level.heroFOV[cell]) return;
		CellEmitter.get( cell ).burst( LeafParticle.GENERAL, 6 );
		String key;
		switch (how){
			case SPENT:    key = "spent"; break;
			case FROSTED:  key = "frosted"; break;
			case UPROOTED: key = "uprooted"; break;
			case DROWNED:  key = "drowned"; break;
			case SCORCHED: key = "scorched"; break;
			case EATEN:    return;   //Pests tells it, naming the beast
			default:       key = "wither"; break;
		}
		GLog.w( Messages.get( FarmCrop.class, key, name( c ) ) );
	}

	/** The sickle on a ripe crop: what it gives (FarmRules.harvest) in a heap on its cell, and the
	 *  crop is cut. A turn's work. */
	public static void harvest( Hero hero, FarmCrop m ){
		Crop c = m.crop;
		Level level = Dungeon.level;
		int cell = m.pos;
		for (Item i : FarmRules.harvest( c.plant(), c.seed )){
			//a Reaper's scythe may cut a second seed loose; food crops give no seed to double
			if (i instanceof Plant.Seed && Quality.secondSeed( hero, cell )) i.quantity( i.quantity() * 2 );
			Heap h = level.drop( i, cell );
			if (h.sprite != null) h.sprite.drop();
		}
		GLog.p( Messages.get( FarmCrop.class, FarmRules.food( c.plant() ) ? "harvest_food" : "harvest", name( c ) ) );
		if (live( level )){
			CellEmitter.get( cell ).burst( LeafParticle.GENERAL, 8 );
			SpatialSound.play( Assets.Sounds.REAP, cell );
		}
		FarmSlice f = fields( level );
		if (f != null) f.remove( c.wx, c.wy );
		m.destroy();
		if (m.sprite != null) m.sprite.killAndErase();
		hero.spendAndNext( 1f );
	}

	/** Does the hero carry a sickle or a war scythe to cut a ripe crop (or tall grass) with. */
	public static boolean hasSickle( Hero hero ){
		return hero.belongings.getItem( Sickle.class ) != null || hero.belongings.getItem( WarScythe.class ) != null;
	}

	// ----------------------------------------------------------------- watering

	/**
	 * The farmland within FarmRules.WELL_REACH of a cell shows whether it is watered: FARMLAND_WET
	 * where it has water (FarmRules.water), FARMLAND where it has none. Run when a piece goes up or
	 * comes down there (FarmSlice.cellChanged) and on sowing; the crops ask the water itself, so
	 * the look never decides anything. Every cell changed is redrawn in one go (one look round,
	 * Homestead.refreshArea), however many of them changed.
	 */
	public static void water( Level level, int cell ){
		int w = level.width(), x0 = cell % w, y0 = cell / w, r = FarmRules.WELL_REACH;
		//the rectangle the changed cells span
		int xa = w, xb = -1, ya = 0, yb = 0;
		for (int y = Math.max( 0, y0 - r ); y <= Math.min( level.height() - 1, y0 + r ); y++){
			for (int x = Math.max( 0, x0 - r ); x <= Math.min( w - 1, x0 + r ); x++){
				int c = x + y * w;
				if (!BuiltTerrain.farmland( level.map[c] )) continue;
				int want = waterAt( level, c ) > 0 ? Terrain.FARMLAND_WET : Terrain.FARMLAND;
				if (level.map[c] == want) continue;
				Level.set( c, want, level );
				if (xb == -1) ya = y;
				yb = y;
				xa = Math.min( xa, x );
				xb = Math.max( xb, x );
			}
		}
		if (xb != -1) Homestead.refreshArea( level, xa + ya * w, xb + yb * w );
	}

	/** Is a crop's cell shut in, out of the pests' reach (Enclosure, gates as they stand)? */
	public static boolean fenced( Level level, int cell ){
		return Enclosure.closed( level, cell, false );
	}

	//is this level the one on screen
	static boolean live( Level level ){
		return level == Dungeon.level && Game.scene() instanceof GameScene;
	}
}
