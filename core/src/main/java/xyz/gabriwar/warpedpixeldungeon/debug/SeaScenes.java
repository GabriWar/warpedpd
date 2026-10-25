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
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shark;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LighthouseKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VillageFisher;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaSites;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SeaWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.sea.Afloat;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.function.Predicate;

/**
 * The sea's debug scenes (the Scenes menu's "Surface" group; docs/sea.md):
 *
 * overworld-sea-raft: the nearest island with a shipwreck on it, the hero aboard a raft in the water
 * beside it, the wreck a few steps off. Step ashore onto the island, walk into the wreck's hold for its
 * salvage, tap the raft to board it again or haul it out.
 *
 * overworld-sea-storm: the nearest island, the hero aboard a raft out on the deep beside it, a storm
 * forced (heavy rain in a 15 m/s wind) and a shark set down in sight: waves break over the raft and
 * soak him, its hull wears, the warnings come at half and a quarter, and it capsizes into a swim.
 * weather-clear lets the storm go.
 *
 * overworld-sea-lighthouse: the nearest lighthouse at night, the hero on its apron beside its keeper:
 * the lamp's glow and its two beams sweeping round, the keeper's shop (a sea chart, sea lanterns).
 *
 * overworld-sea-treasure: the nearest island with a buried chest still in it, the hero beside the
 * cross in the sand with an adventurer's shovel: dig it up.
 *
 * overworld-sea-fisher: the nearest human village by the sea, the hero beside its fisher, who sells
 * rafts and sailboats.
 */
final class SeaScenes {

	private SeaScenes(){}

	static final DebugScenes.Scene[] SCENES = {
			new RaftByAWreck(),
			new StormAboard(),
			new LighthouseAtNight(),
			new BuriedChest(),
			new Fisher()
	};

	//how far out the searches go, sectors (WorldStructures.SECTOR cells each) round the hero
	static final int RINGS = 24;

	// ------------------------------------------------------------------ helpers

	private static boolean onSurface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0;
	}

	//where the searches start: the hero's world cell on the surface, the world's origin elsewhere
	private static int[] from( Hero hero ){
		if (!onSurface()) return new int[]{ 0, 0 };
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		return new int[]{ ow.worldX + hero.pos % ow.width(), ow.worldY + hero.pos / ow.width() };
	}

	private static long seed(){
		return OverworldLevel.worldSeedOf( Dungeon.seed );
	}

	//a place of the sea's world cell: an island's middle, a wreck's hold, a lighthouse's base
	private static int[] spotOf( SeaSites.Sector s ){
		if (s.island != null) return new int[]{ s.island.cx, s.island.cy };
		if (s.wreck != null) return new int[]{ s.wreck.holdX, s.wreck.holdY };
		return new int[]{ s.light.x, s.light.y };
	}

	/** Of the sea's places within RINGS sectors of (wx, wy), the nearest that suits, ring by ring
	 *  (pure: for the search's thread); null for none. */
	static SeaSites.Sector nearest( long seed, int wx, int wy, Predicate<SeaSites.Sector> suits ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int r = 0; r <= RINGS; r++){
			SeaSites.Sector best = null;
			long bestD = Long.MAX_VALUE;
			for (int sy = sy0 - r; sy <= sy0 + r; sy++){
				for (int sx = sx0 - r; sx <= sx0 + r; sx++){
					if (Math.max( Math.abs( sx - sx0 ), Math.abs( sy - sy0 ) ) != r) continue;
					SeaSites.Sector s = SeaSites.sector( seed, sx, sy );
					if (s.island == null && s.wreck == null && s.light == null) continue;
					if (!suits.test( s )) continue;
					int[] p = spotOf( s );
					long d = (long)(p[0] - wx) * (p[0] - wx) + (long)(p[1] - wy) * (p[1] - wy);
					if (d < bestD){
						bestD = d;
						best = s;
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	//the nearest window cell to the hero (within `reach`) that a test holds for, -1 for none
	private static int nearestCell( Hero hero, int reach, Predicate<Integer> ok ){
		Level l = Dungeon.level;
		int w = l.width(), hx = hero.pos % w, hy = hero.pos / w;
		for (int r = 1; r <= reach; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = hx + dx, y = hy + dy;
					if (x < 2 || y < 2 || x >= w - 2 || y >= l.height() - 2) continue;
					int c = x + y * w;
					if (Actor.findChar( c ) == null && ok.test( c )) return c;
				}
			}
		}
		return -1;
	}

	//open water with land beside it: where a boat lies a step off the shore
	private static boolean byTheShore( Level l, int c ){
		if (!Seafaring.water( l, c )) return false;
		for (int n : PathFinder.NEIGHBOURS8){
			int m = c + n;
			if (l.passable[m] && !Seafaring.water( l, m ) && !l.pit[m]) return true;
		}
		return false;
	}

	/** The hero put aboard a boat of a kind floating on a window cell (full hull); a boat of his
	 *  already afloat is hauled into his pack first. Host only; false when he is not aboard after. */
	static boolean board( Hero hero, Boat.Kind kind, int cell ){
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		Afloat old = Seafaring.boat( hero );
		if (old != null){
			Boat back = old.kind.item();
			back.hull = old.hull;
			old.detach();
			if (!back.collect( hero.belongings.backpack )) Dungeon.level.drop( back, hero.pos );
			GLog.i( "Scene: your boat afloat elsewhere went back into your pack" );
		}
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = kind;
		a.hull = a.max();
		a.told = 0;
		a.wx = ow.worldX + cell % ow.width();
		a.wy = ow.worldY + cell / ow.width();
		a.aboard = false;
		//stepping onto his own moored boat takes him aboard (Seafaring.moved)
		ScrollOfTeleportation.appear( hero, cell );
		Dungeon.observe();
		ow.sea().boatsChanged();
		return Seafaring.aboard( hero );
	}

	//the hero set down on a free cell of open ground beside a cell
	private static boolean besides( Hero hero, int cell ){
		Level l = Dungeon.level;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (l.passable[c] && !l.pit[c] && !Seafaring.water( l, c ) && Actor.findChar( c ) == null){
				ScrollOfTeleportation.appear( hero, c );
				Dungeon.observe();
				return true;
			}
		}
		return false;
	}

	private static boolean mirror(){
		if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).isMirror()){
			GLog.w( "Scene: only the host can set the sea's scenes up" );
			return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ the scenes

	/** Aboard a raft beside an island, its wreck a few steps off. */
	private static final class RaftByAWreck implements DebugScenes.Scene {
		boolean travelling;
		SeaSites.Wreck wreck;

		@Override public String id(){ return "overworld-sea-raft"; }
		@Override public String title(){ return "Surface: aboard a raft off an island, a shipwreck in sight"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSurface() || mirror()){
					GLog.w( "Scene: could not reach the island" );
					return;
				}
				int c = nearestCell( hero, 10, cell -> byTheShore( Dungeon.level, cell ) );
				if (c == -1 || !board( hero, Boat.Kind.RAFT, c )){
					GLog.w( "Scene: no open water beside the island here" );
					return;
				}
				GLog.i( "Scene: aboard a raft off " + SeaSites.islandName( seed(), Math.floorDiv( wreck.holdX, WorldStructures.SECTOR ),
						Math.floorDiv( wreck.holdY, WorldStructures.SECTOR ) ) + ", the wreck of the "
						+ SeaSites.shipName( seed(), Math.floorDiv( wreck.holdX, WorldStructures.SECTOR ), Math.floorDiv( wreck.holdY, WorldStructures.SECTOR ) )
						+ " beside you. Sail along the shore, step onto the island to go ashore (the raft stays moored), walk into the"
						+ " wreck's hold for its salvage, then tap the raft to board it again or haul it out." );
				return;
			}
			int[] at = from( hero );
			final long seed = seed();
			DebugScenes.search( "an island with a shipwreck",
					() -> nearest( seed, at[0], at[1], s -> s.island != null && s.island.wreck != null ),
					s -> {
						if (s == null){
							GLog.w( "Scene: no island with a wreck within " + RINGS + " sectors" );
							return;
						}
						wreck = s.island.wreck;
						int[] m = wreck.mouth()[1];
						travelling = true;
						DebugScenes.travelThen( this, 0, m[0], m[1] );
					} );
		}
	}

	/** Aboard a raft out on the deep in a storm, a shark in sight. */
	private static final class StormAboard implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "overworld-sea-storm"; }
		@Override public String title(){ return "Surface: a storm at sea aboard a raft, a shark in sight"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSurface() || mirror()){
					GLog.w( "Scene: could not reach the island" );
					return;
				}
				Level l = Dungeon.level;
				int c = nearestCell( hero, 12, cell -> l.map[cell] == Terrain.DEEP_WATER && l.distance( hero.pos, cell ) >= 3 );
				if (c == -1 || !board( hero, Boat.Kind.RAFT, c )){
					GLog.w( "Scene: no deep water beside the island here" );
					return;
				}
				new DebugScenes.Sky( PrecipType.RAIN, 0.6f, 15f, 90f, true ).hold();
				int s = nearestCell( hero, 9, cell -> l.map[cell] == Terrain.DEEP_WATER && l.distance( hero.pos, cell ) >= 5
						&& l.heroFOV[cell] );
				if (s != -1) DebugScenes.spawn( Shark.class, s );
				GLog.i( "Scene: a storm held over the sea (weather-clear lets it go), the raft at full hull ("
						+ Boat.Kind.RAFT.hull + "). Waves break over it a turn in four and wear 3 to 6 each: the warnings come at half"
						+ " and a quarter, then it capsizes and you swim (12 turns before the sea tires you)."
						+ (s != -1 ? " A shark is in sight: its bites land on the hull, one in three on you too." : " No deep water in sight for a shark.") );
				return;
			}
			int[] at = from( hero );
			final long seed = seed();
			DebugScenes.search( "an island out at sea",
					() -> nearest( seed, at[0], at[1], s -> s.island != null ),
					s -> {
						if (s == null){
							GLog.w( "Scene: no island within " + RINGS + " sectors" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, s.island.cx, s.island.cy );
					} );
		}
	}

	/** A lighthouse at night, its keeper on the apron. */
	private static final class LighthouseAtNight implements DebugScenes.Scene {
		boolean travelling;
		SeaSites.Light light;

		@Override public String id(){ return "overworld-sea-lighthouse"; }
		@Override public String title(){ return "Surface: a lighthouse at night, its keeper at the door"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSurface()){
					GLog.w( "Scene: could not reach the lighthouse" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				if (!DebugScenes.walkClock( null, DayNightCycle.Phase.NIGHT, 0.3f )){
					GLog.w( "Scene: a real-clock run keeps its own hour - the clock was not moved" );
				}
				LighthouseKeeper keeper = null;
				for (Mob m : ow.mobs){
					if (m instanceof LighthouseKeeper && ((LighthouseKeeper) m).lightKey == light.key()) keeper = (LighthouseKeeper) m;
				}
				int apron = ow.localCell( light.x + 1, light.y + 1 );
				if (apron != -1 && Actor.findChar( apron ) == null && ow.passable[apron]){
					ScrollOfTeleportation.appear( hero, apron );
					Dungeon.observe();
				}
				GLog.i( "Scene: " + SeaWorld.nameOf( seed(), SeaWorld.LIGHTHOUSE, light.x, light.y ) + " at night: the lamp glows and"
						+ " two beams sweep round every " + (int) xyz.gabriwar.warpedpixeldungeon.effects.LighthouseLamp.PERIOD + " s."
						+ (keeper != null ? " Its keeper is beside you: tap him for his shop (open by day)." : " Its keeper is not here"
						+ " (driven off, or he comes once the whole lighthouse is in the window)." ) );
				return;
			}
			int[] at = from( hero );
			final long seed = seed();
			DebugScenes.search( "a lighthouse",
					() -> nearest( seed, at[0], at[1], s -> s.light != null ),
					s -> {
						if (s == null){
							GLog.w( "Scene: no lighthouse within " + RINGS + " sectors" );
							return;
						}
						light = s.light;
						travelling = true;
						DebugScenes.travelThen( this, 0, light.x, light.y + 2 );
					} );
		}
	}

	/** Beside an island's buried chest, a shovel in the pack. */
	private static final class BuriedChest implements DebugScenes.Scene {
		boolean travelling;
		SeaSites.Island island;

		@Override public String id(){ return "overworld-sea-treasure"; }
		@Override public String title(){ return "Surface: a buried chest on an island, a shovel in hand"; }

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSurface() || mirror()){
					GLog.w( "Scene: could not reach the island" );
					return;
				}
				OverworldLevel ow = (OverworldLevel) Dungeon.level;
				int dig = ow.localCell( island.digX, island.digY );
				if (dig == -1 || !ow.sea().treasureAt( dig )){
					GLog.w( "Scene: this island's chest is dug up already" );
					return;
				}
				if (hero.belongings.getItem( Shovel.class ) == null){
					Shovel shovel = new Shovel();
					shovel.identify();
					if (!shovel.collect( hero.belongings.backpack )){
						Heap h = Dungeon.level.drop( shovel, hero.pos );
						if (h.sprite != null) h.sprite.drop();
					}
				}
				besides( hero, dig );
				GLog.i( "Scene: the cross in the sand of " + SeaSites.islandName( seed(), island.sx, island.sy )
						+ " is beside you, an adventurer's shovel in your pack: dig there for the sea chest." );
				return;
			}
			int[] at = from( hero );
			final long seed = seed();
			DebugScenes.search( "an island with a buried chest",
					() -> nearest( seed, at[0], at[1], s -> s.island != null && s.island.digX != Integer.MIN_VALUE ),
					s -> {
						if (s == null){
							GLog.w( "Scene: no island with a buried chest within " + RINGS + " sectors" );
							return;
						}
						island = s.island;
						travelling = true;
						DebugScenes.travelThen( this, 0, island.digX, island.digY );
					} );
		}
	}

	/** Beside the fisher of the nearest human village by the sea. */
	private static final class Fisher implements DebugScenes.Scene {
		boolean travelling;

		@Override public String id(){ return "overworld-sea-fisher"; }
		@Override public String title(){ return "Surface: a fishing village's fisher, who sells boats"; }

		//deep sea within reach of a village's well: its banks may hold the fisher's stall
		private static boolean bySea( long seed, int wx, int wy ){
			WorldModel.Sample scratch = new WorldModel.Sample();
			for (int dy = -30; dy <= 30; dy += 3){
				for (int dx = -30; dx <= 30; dx += 3){
					if (WorldModel.elevationOf( seed, wx + dx, wy + dy, scratch ) < WorldModel.SEA - 0.02f) return true;
				}
			}
			return false;
		}

		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				if (!onSurface()){
					GLog.w( "Scene: could not reach the village" );
					return;
				}
				for (Mob m : Dungeon.level.mobs){
					if (m instanceof VillageFisher){
						besides( hero, m.pos );
						GLog.i( "Scene: the village's fisher is beside you: tap her for a raft or a sailboat (open by day)." );
						return;
					}
				}
				GLog.w( "Scene: no fisher here: none of this village's banks has deep water beside it in the window" );
				return;
			}
			int[] at = from( hero );
			final long seed = seed();
			DebugScenes.search( "a human village by the sea",
					() -> {
						for (int[] v : WorldStructures.settlementsNear( seed, at[0], at[1], WorldStructures.Faction.HUMAN, 1, 8 )){
							if (bySea( seed, v[2], v[3] )) return v;
						}
						return null;
					},
					v -> {
						if (v == null){
							GLog.w( "Scene: no human village by the sea within 8 sectors" );
							return;
						}
						travelling = true;
						DebugScenes.travelThen( this, 0, v[2], v[3] );
					} );
		}
	}
}
