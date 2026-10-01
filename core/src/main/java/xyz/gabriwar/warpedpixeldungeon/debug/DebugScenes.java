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
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Debug scenes: a named situation set up around the hero in one tap, for testing
 * something specific without walking the dungeon for it. Applied from the debug
 * menu (Mobs tab, "Scenes") on the current run, or straight from the command line
 * on a fresh run:
 *
 *   ./gradlew :desktop:debug -Pscene=fliers-paralysed
 *
 * which starts a new game, lands on the surface and applies the scene there. Every
 * scene keeps the hero safe (infinite health) and lifts the fog.
 *
 * The rooms Warped adds have scenes of their own, all on levels/WarpedRoomsLevel:
 * warped-rooms, warped-rooms-night (the same floor after dark, so the market is open) and
 * warped-rooms-only (the twelve Warped rooms alone, one of each).
 */
public final class DebugScenes {

	private DebugScenes(){}

	public interface Scene {
		String id();
		String title();
		void apply( Hero hero );
	}

	//all the scenes there are, in menu order
	public static final Scene[] SCENES = {
			new Room(),
			new Fliers( "fliers-paralysed", "All fliers, paralysed", Paralysis.class ),
			new Fliers( "fliers-frozen", "All fliers, frozen", Frost.class ),
			new Fliers( "fliers-free", "All fliers, free", null ),
			new Pit(),
			new Edges(),
			new WarpedRoomsScene( "warped-rooms", "Warped rooms: all, live, with supplies", false, false ),
			new WarpedRoomsScene( "warped-rooms-night", "Warped rooms at nightfall (market open)", true, false ),
			new WarpedRoomsScene( "warped-rooms-only", "Warped rooms only: one of each, no standard rooms", false, true ),
	};

	public static Scene byId( String id ){
		for (Scene s : SCENES) if (s.id().equals( id )) return s;
		return null;
	}

	/** Runs a scene on the live game: safe hero, no fog, then the scene's own setup. */
	public static void run( Scene scene ){
		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null) return;
		hero.migrateDebugGodmode();
		hero.debugInfiniteHealth = true;
		hero.HP = hero.HT;
		Dungeon.debugNoFog = true;
		scene.apply( hero );
		Dungeon.observe();
		GameScene.updateFog();
		GLog.p( "Scene: " + scene.title() );
	}

	// ----------------------------------------------------- command line start

	//the scene the command line asked for: a new run is started for it (TitleScene)
	//and it is applied once the game scene stands (GameScene.create)
	private static Scene pending;
	private static boolean starting;

	public static void request( String id ){
		pending = byId( id );
		if (pending == null) System.out.println( "[scene] unknown scene '" + id + "', known: " + ids() );
	}

	public static String ids(){
		StringBuilder sb = new StringBuilder();
		for (Scene s : SCENES) sb.append( sb.length() == 0 ? "" : ", " ).append( s.id() );
		return sb.toString();
	}

	/** TitleScene: a requested scene starts a fresh run at once instead of showing the menu. */
	public static boolean startRequested(){
		if (pending == null || starting) return false;
		starting = true;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene( InterlevelScene.class );
		return true;
	}

	/** GameScene, once it stands: the requested scene is applied and forgotten. */
	public static void applyRequested(){
		if (pending == null || !starting) return;
		Scene s = pending;
		pending = null;
		run( s );
	}

	// -------------------------------------------------------------- helpers

	//open cells around the hero, nearest first, none adjacent to him (room to breathe)
	static ArrayList<Integer> cellsAround( Hero hero, int count ){
		Level level = Dungeon.level;
		ArrayList<Integer> out = new ArrayList<>();
		int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
		for (int r = 2; r < 12 && out.size() < count; r++){
			for (int dy = -r; dy <= r && out.size() < count; dy++){
				for (int dx = -r; dx <= r && out.size() < count; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = hx + dx, y = hy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int cell = x + y * w;
					if (!level.passable[cell] || level.pit[cell] || Actor.findChar( cell ) != null) continue;
					out.add( cell );
				}
			}
		}
		return out;
	}

	//every kind of monster in the bestiary that flies
	static ArrayList<Class<? extends Mob>> fliers(){
		ArrayList<Class<? extends Mob>> out = new ArrayList<>();
		HashSet<Class<?>> seen = new HashSet<>();
		for (Bestiary b : Bestiary.values()){
			for (Class<?> cls : b.entities()){
				if (!Mob.class.isAssignableFrom( cls ) || !seen.add( cls )) continue;
				Mob sample = (Mob) Reflection.newInstance( cls );
				if (sample != null && sample.flying){
					@SuppressWarnings("unchecked") Class<? extends Mob> mc = (Class<? extends Mob>) cls;
					out.add( mc );
				}
			}
		}
		return out;
	}

	static Mob spawn( Class<? extends Mob> cls, int cell ){
		Mob mob = Reflection.newInstance( cls );
		if (mob == null) return null;
		mob.pos = cell;
		mob.state = mob.WANDERING;
		GameScene.add( mob );
		return mob;
	}

	// --------------------------------------------------------------- scenes

	//a bare walled room around the hero: 15 by 11 of plain floor and nothing else in it.
	//The world's slices are painted over their terrain (town art, dress layers), so from
	//there the hero is first sent to the first sewer floor and the room carved on arrival
	private static final class Room implements Scene {
		static final int HALF_W = 7, HALF_H = 5;
		@Override public String id(){ return "blank-room"; }
		@Override public String title(){ return "Blank room around the hero"; }
		@Override public void apply( Hero hero ){
			ensure( this, hero );
		}

		/** Carves the room for a scene; false when the hero had to be sent to the sewers
		 *  first, in which case the scene is applied again on arrival. */
		static boolean ensure( Scene scene, Hero hero ){
			Level level = Dungeon.level;
			if (level instanceof OverworldLevel){
				pending = scene;
				starting = true;
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = 1;
				InterlevelScene.returnBranch = 0;
				InterlevelScene.returnPos = -1;
				Game.switchScene( InterlevelScene.class );
				return false;
			}
			int w = level.width(), hx = hero.pos % w, hy = hero.pos / w;
			for (int dy = -HALF_H - 1; dy <= HALF_H + 1; dy++){
				for (int dx = -HALF_W - 1; dx <= HALF_W + 1; dx++){
					int x = hx + dx, y = hy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int cell = x + y * w;
					boolean wall = Math.abs( dx ) > HALF_W || Math.abs( dy ) > HALF_H;
					Level.set( cell, wall ? Terrain.WALL : Terrain.EMPTY );
					level.plants.remove( cell );
					level.traps.remove( cell );
					if (level.heaps.get( cell ) != null) level.heaps.get( cell ).destroy();
					if (!wall && level.blobs != null){
						for (xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob b : level.blobs.values()) b.clear( cell );
					}
					for (Mob m : level.mobs.toArray( new Mob[0] )){
						if (m.pos == cell){
							if (m.sprite != null) m.sprite.killAndErase();
							m.destroy();
						}
					}
					level.visited[cell] = true;
				}
			}
			GameScene.updateMap();
			level.cleanWalls();
			return true;
		}
	}
	//every flying monster around the hero in the blank room, held by the given buff for two turns (or free)
	private static final class Fliers implements Scene {
		final String id, title;
		final Class<? extends FlavourBuff> hold;
		Fliers( String id, String title, Class<? extends FlavourBuff> hold ){
			this.id = id; this.title = title; this.hold = hold;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (!Room.ensure( this, hero )) return;
			ArrayList<Class<? extends Mob>> kinds = fliers();
			ArrayList<Integer> cells = cellsAround( hero, kinds.size() );
			for (int i = 0; i < kinds.size() && i < cells.size(); i++){
				Mob mob = spawn( kinds.get( i ), cells.get( i ) );
				if (mob != null && hold != null) Buff.prolong( mob, hold, 2f );
			}
			if (cells.size() < kinds.size()){
				GLog.w( "Scene: only room for " + cells.size() + " of " + kinds.size() + " fliers here" );
			}
		}
	}

	//the nearest place on the surface where rocky foothills run into a snowfield:
	//boulders, standing rocks, the snow line and frozen ponds side by side, for
	//the ground transitions and the rocks' footing
	private static final class Edges implements Scene {
		//set while the hero is on his way there, so the arrival does not send him off again
		boolean travelling;
		@Override public String id(){ return "overworld-edges"; }
		@Override public String title(){ return "Surface: rocks and a snow line"; }
		@Override public void apply( Hero hero ){
			if (travelling){
				travelling = false;
				return;
			}
			int[] spot = xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator.findRockyEdge(
					OverworldLevel.worldSeedOf( Dungeon.seed ) );
			if (spot == null){
				GLog.w( "Scene: no rocky snow edge near the origin in this world" );
				return;
			}
			travelling = true;
			pending = this;
			starting = true;
			OverworldLevel.arriveAt( spot[0], spot[1] );
			OverworldLevel.travelToSurface();
		}
	}

	//the floor of Warped's own rooms (levels/WarpedRoomsLevel): every one of them painted
	//once and left running, the season-reading ones once per season, and a row of supplies
	//by the entrance. Each run of the scene builds the floor afresh, so a room that has been
	//used up is whole again. The night variant also walks the world's clock on to nightfall
	//- moved, not overridden, so it keeps running - because the black market's dealer is
	//only on the floor after dark
	private static final class WarpedRoomsScene implements Scene {
		static final int DEPTH = 86;
		final String id, title;
		final boolean night;
		//the twelve Warped rooms and nothing else: no standard rooms, no season copies
		final boolean warpedOnly;
		//set while the hero is on his way to the floor, so the arrival finishes the scene
		//instead of sending him off again
		boolean travelling;
		WarpedRoomsScene( String id, String title, boolean night, boolean warpedOnly ){
			this.id = id; this.title = title; this.night = night; this.warpedOnly = warpedOnly;
		}
		@Override public String id(){ return id; }
		@Override public String title(){ return title; }
		@Override public void apply( Hero hero ){
			if (!travelling){
				travelling = true;
				pending = this;
				starting = true;
				//read by the floor as it is built, a few moments from now
				xyz.gabriwar.warpedpixeldungeon.levels.WarpedRoomsLevel.warpedOnly = warpedOnly;
				//never the saved floor: a fresh one every time
				Dungeon.generatedLevels.remove( Integer.valueOf( DEPTH ) );
				if (Dungeon.depth == DEPTH && Dungeon.branch == 0){
					InterlevelScene.mode = InterlevelScene.Mode.RESET;
				} else {
					InterlevelScene.mode = InterlevelScene.Mode.RETURN;
					InterlevelScene.returnDepth = DEPTH;
					InterlevelScene.returnBranch = 0;
					InterlevelScene.returnPos = -1;
				}
				Game.switchScene( InterlevelScene.class );
				return;
			}
			travelling = false;

			//enough coin for the market's whole counter, the wheel and a few contracts, and a
			//clean slate with the market: no heat, no ban carried in from an earlier test
			Dungeon.gold = Math.max( Dungeon.gold, 50000 );
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms.settle();
			xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms.banned = false;

			if (night && !Dungeon.isChallenged( xyz.gabriwar.warpedpixeldungeon.Challenges.REAL_CLOCK )){
				xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase p
						= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phase();
				if (p != xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT){
					int turns = xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.turnsUntilPhaseChange();
					for (p = p.next(); p != xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT; p = p.next()){
						turns += xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.phaseDuration( p );
					}
					//a few turns past the boundary, so it is unmistakably night
					turns += 5;
					Dungeon.cycleTurn += turns;
					xyz.gabriwar.warpedpixeldungeon.Statistics.duration += turns;
					xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.onHeroTurn();
				}
			}

			GLog.i( "Warped rooms: the signs name each room; supplies are in the two rows by the entrance." );
			GLog.i( "The floor's climate is a sewer floor's: the debug menu's weather, temperature and hour overrides all reach it." );
			if (night) GLog.i( "Clock moved to nightfall: the black market's dealer sets up on his next turn." );
		}
	}

	//in the blank room, a chasm three rows ahead of the hero with a bat, a wraith and a rat frozen on its lip: for
	//what falls, what shatters and what survives (Chasm.mobFall)
	private static final class Pit implements Scene {
		@Override public String id(){ return "pit-frozen"; }
		@Override public String title(){ return "Pit with frozen monsters on the edge"; }
		@Override public void apply( Hero hero ){
			if (!Room.ensure( this, hero )) return;
			Level level = Dungeon.level;
			int w = level.width();
			int row = hero.pos - 3 * w;
			if (row < w) return;
			for (int dx = -3; dx <= 3; dx++){
				int cell = row + dx;
				if (cell % w <= 0 || cell % w >= w - 1) continue;
				Level.set( cell, Terrain.CHASM );
				GameScene.updateMap( cell );
			}
			Class<?>[] kinds = {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith.class,
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat.class };
			int[] at = { row + w - 2, row + w, row + w + 2 };
			for (int i = 0; i < kinds.length; i++){
				int cell = at[i];
				if (!level.passable[cell] || Actor.findChar( cell ) != null) continue;
				@SuppressWarnings("unchecked") Mob mob = spawn( (Class<? extends Mob>) kinds[i], cell );
				if (mob != null) Buff.prolong( mob, Frost.class, 300f );
			}
			for (int i : PathFinder.NEIGHBOURS9) level.mapped[hero.pos + i] = true;
		}
	}
}
