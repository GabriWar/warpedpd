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

package xyz.gabriwar.warpedpixeldungeon.sea;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SeaFx;
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Mounts;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.MarchPlanner;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The hero on the water (docs/sea.md). Boats float on the surface alone - no boat goes down into the
 * caves or up onto the peaks - over the shallows and the deep alike, never on ice.
 *
 * - Launching: a boat from the pack, the hero on the shore (or wading) beside open water: it goes onto
 *   the water beside him, the deep first, and he climbs aboard (two turns).
 * - Aboard: he moves over the water only, the boat under him; a step onto land beside it puts him
 *   ashore and the boat stays moored where it floats, kept on him (Afloat) through every save and
 *   journey. A step onto it, or a walk to it from anywhere, puts him aboard again. A tap far inland
 *   sails him to the landing nearest it, and he walks on from there.
 * - Hauling: from beside it, he takes it back into his pack (OverworldLevel's own hero: a co-op guest
 *   sails and moors his, but cannot haul it).
 * - The weather: in the rain or a strong wind the sea is choppy, in a storm it is rough. Waves break
 *   over a boat now and then and drench whoever is aboard; a raft's hull wears in any rough water, a
 *   sailboat's only in a storm, less on the sheltered shelf by a shore. Half its hull gone is warned
 *   of, then three quarters; at nothing it goes down.
 * - Swimming: in deep water with no boat under him, the hero swims (Swimming) until he finds the
 *   shallows or the shore.
 *
 * The rules run on the host for every hero (a co-op guest's hero is the host's actor); the look of a
 * hero aboard reaches guests through his sprite's state, the moored boats through SeaWorld's share.
 */
public final class Seafaring {

	private Seafaring(){}

	/** Turns to launch a boat, and to haul one out. */
	public static final float LAUNCH_TIME = 2f, HAUL_TIME = 2f;
	//a sailboat with the wind behind it (within this many degrees of its course, the wind at this
	//speed at least) runs at twice a walker's pace
	static final float WIND_ARC = 60f, WIND_MIN = 4f;
	public static final float SAIL_BEFORE_WIND = 2f, SWIM_SPEED = 0.5f;

	// ------------------------------------------------------------------ where a boat floats

	/** The slice boats float on, when the level is it: the surface. */
	public static OverworldLevel surface( Level l ){
		return l instanceof OverworldLevel && ((OverworldLevel) l).altitude() == 0 ? (OverworldLevel) l : null;
	}

	/** Open water a boat floats on: the shallows or the deep, never ice. */
	public static boolean water( Level l, int cell ){
		if (cell < 0 || cell >= l.length()) return false;
		int t = l.map[cell];
		return t == Terrain.WATER || t == Terrain.DEEP_WATER;
	}

	/** The map's march aboard a boat (levels/overworld/MarchPlanner): open water only, the land shut. */
	public static final MarchPlanner.Walker SAIL = new MarchPlanner.Walker(){
		@Override
		public float cost( int terrain ){
			return terrain == Terrain.WATER || terrain == Terrain.DEEP_WATER ? 1f : BLOCKED;
		}

		@Override
		public float costOn( Level level, int cell ){
			return water( level, cell ) ? 1f : BLOCKED;
		}

		@Override
		public boolean scatter( int terrain ){
			return false;
		}
	};

	/** A hero's boat, afloat somewhere, or null. */
	public static Afloat boat( Char ch ){
		return ch == null ? null : ch.buff( Afloat.class );
	}

	private static int wxOf( OverworldLevel ow, int cell ){
		return ow.worldX + cell % ow.width();
	}

	private static int wyOf( OverworldLevel ow, int cell ){
		return ow.worldY + cell / ow.width();
	}

	/** Is he sailing his boat: aboard it, on the surface, on its cell. */
	public static boolean aboard( Char ch ){
		Afloat a = boat( ch );
		if (a == null || !a.aboard) return false;
		OverworldLevel ow = surface( Dungeon.level );
		return ow != null && ch.pos >= 0 && ch.pos < ow.length()
				&& wxOf( ow, ch.pos ) == a.wx && wyOf( ow, ch.pos ) == a.wy;
	}

	/** In deep water with no boat under him. */
	public static boolean swimming( Char ch ){
		Level l = Dungeon.level;
		return ch instanceof Hero && l != null && ch.pos >= 0 && ch.pos < l.length()
				&& l.map[ch.pos] == Terrain.DEEP_WATER && !ch.flying && !aboard( ch );
	}

	/** Is he out on the water for good reason - sailing his boat, or already swimming - so that the
	 *  level leaves him there rather than setting him down on open ground (OverworldLevel.reseason).
	 *  The boat's own word that he is aboard counts: in the middle of a step (a reseason can come
	 *  with one) his cell has moved and the boat has not yet. */
	public static boolean atSea( Char ch ){
		Afloat a = boat( ch );
		return (a != null && a.aboard) || (ch.buff( Swimming.class ) != null && swimming( ch ));
	}

	/** The window cell his moored boat floats on, or -1 (aboard it, none afloat, out of the window). */
	public static int boatCell( Char ch ){
		Afloat a = boat( ch );
		OverworldLevel ow = surface( Dungeon.level );
		if (a == null || a.aboard || ow == null) return -1;
		return ow.localCell( a.wx, a.wy );
	}

	//a cell he may step onto from a boat: open ground nobody stands on
	private static boolean landable( Level l, int cell ){
		return cell >= 0 && cell < l.length() && l.passable[cell] && !water( l, cell ) && !l.pit[cell]
				&& Actor.findChar( cell ) == null;
	}

	private static boolean known( Level l, int c ){
		return l.visited[c] || l.mapped[c];
	}

	// ------------------------------------------------------------------ launching and hauling

	/** Why he cannot launch a boat here (what he is told), null when he can. */
	public static String launchRefusal( Hero hero ){
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null) return Messages.get( Seafaring.class, "launch_surface" );
		if (aboard( hero )) return Messages.get( Seafaring.class, "launch_aboard" );
		if (swimming( hero )) return Messages.get( Seafaring.class, "launch_swimming" );
		//his other boat, while he can walk to it: one he cannot (moored off an island he left by
		//other means, out at sea, beyond the window) is left to the sea by the launch
		Afloat other = boat( hero );
		if (other != null && withinReach( hero, ow, other )) return Messages.get( Seafaring.class, "launch_one" );
		if (launchCell( hero ) == -1) return Messages.get( Seafaring.class, "launch_shore" );
		return null;
	}

	/** Can he walk from where he stands to a cell beside his moored boat, over the window's open
	 *  ground and the wadeable shallows? A local search: the shared PathFinder is the actor thread's,
	 *  and a launch is asked for from the pack on the render thread. */
	static boolean withinReach( Hero hero, OverworldLevel ow, Afloat a ){
		int boat = ow.localCell( a.wx, a.wy );
		if (boat == -1 || hero.pos < 0 || hero.pos >= ow.length()) return false;
		boolean[] seen = new boolean[ow.length()];
		int[] queue = new int[ow.length()];
		int head = 0, tail = 0;
		queue[tail++] = hero.pos;
		seen[hero.pos] = true;
		while (head < tail){
			int c = queue[head++];
			if (ow.adjacent( c, boat )) return true;
			for (int n : PathFinder.NEIGHBOURS8){
				int m = c + n;
				if (m < 0 || m >= seen.length || seen[m] || !ow.passable[m] || !ow.insideMap( m )) continue;
				seen[m] = true;
				queue[tail++] = m;
			}
		}
		return false;
	}

	/** The water beside him a boat goes onto: the deep first, then the most open water, nobody on it; -1 for none. */
	public static int launchCell( Hero hero ){
		Level l = Dungeon.level;
		int best = -1, bestScore = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (!water( l, c ) || !l.insideMap( c ) || Actor.findChar( c ) != null) continue;
			int open = 0;
			for (int m : PathFinder.NEIGHBOURS8) if (water( l, c + m )) open++;
			int score = (l.map[c] == Terrain.DEEP_WATER ? 100 : 0) + open;
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	/** The boat goes onto the water beside him (launchCell) and he climbs aboard (Seafaring.moved
	 *  tells it). Host. A boat of his still afloat lies out of his reach (launchRefusal): it is left to
	 *  the sea. A rider gets down first: his horse waits on the shore he leaves (Mounts.stepped). From
	 *  the pack, so the turn is handed on here: no animation of his ends it. */
	public static void launch( Hero hero, Boat item ){
		OverworldLevel ow = surface( Dungeon.level );
		int cell = launchCell( hero );
		if (ow == null || cell == -1) return;
		Afloat other = boat( hero );
		if (other != null){
			other.detach();
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Seafaring.class, "launch_left" ) );
		}
		item.detach( hero.belongings.backpack );
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = item.kind();
		a.hull = Math.max( 1, Math.min( a.max(), item.hull ) );
		a.told = a.hull * 4 <= a.max() ? 2 : a.hull * 2 <= a.max() ? 1 : 0;
		a.wx = wxOf( ow, cell );
		a.wy = wyOf( ow, cell );
		a.aboard = false;
		NetManager.heroLog( hero, Messages.get( Seafaring.class, a.kind == Boat.Kind.RAFT ? "launch_raft" : "launch_sail" ) );
		int from = hero.pos;
		boolean rode = Mounts.rides( hero );
		if (hero.sprite != null) hero.sprite.move( from, cell );
		hero.move( cell );
		if (rode) Mounts.stepped( hero, from, true );
		hero.spendAndNext( LAUNCH_TIME );
		SpatialSound.play( Assets.Sounds.WATER, hero, 1f, 0.8f );
		GuideGraph.reveal( GuideGraph.TAG_SAIL );
		ow.sea().boatsChanged();
	}

	/** May he take his boat back into his pack now: it lies beside him, he stands ashore or wades. */
	public static boolean canHaul( Hero hero ){
		Afloat a = boat( hero );
		OverworldLevel ow = surface( Dungeon.level );
		if (a == null || a.aboard || ow == null || swimming( hero )) return false;
		int c = ow.localCell( a.wx, a.wy );
		return c != -1 && ow.adjacent( hero.pos, c );
	}

	/** He drags his boat out of the water and into his pack (onto the ground when it is full). */
	public static void haul( Hero hero ){
		if (!canHaul( hero )) return;
		Afloat a = boat( hero );
		OverworldLevel ow = surface( Dungeon.level );
		Boat item = a.kind.item();
		item.hull = a.hull;
		int at = ow.localCell( a.wx, a.wy );
		a.detach();
		if (!item.collect( hero.belongings.backpack )){
			Heap h = Dungeon.level.drop( item, hero.pos );
			if (h.sprite != null) h.sprite.drop();
		}
		if (hero.sprite != null) hero.sprite.operate( at );
		hero.spend( HAUL_TIME );
		hero.busy();
		SpatialSound.play( Assets.Sounds.WATER, hero, 0.8f, 1.2f );
		NetManager.heroLog( hero, Messages.get( Seafaring.class, "hauled" ) );
		ow.sea().boatsChanged();
	}

	/**
	 * His own moored boat tapped (Hero.handle): from beside it the host's own hero is asked whether to
	 * climb aboard or haul it out of the water - true, the question is up; from farther off, or a co-op
	 * guest's hero (no window of his reaches him), false: he walks to it and climbs aboard.
	 */
	public static boolean offerBoat( final Hero hero ){
		if (hero != Dungeon.hero || !canHaul( hero )) return false;
		final Afloat a = boat( hero );
		final Boat icon = a.kind.item();
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( new ItemSprite( icon ),
				Messages.titleCase( icon.name() ),
				Messages.get( Seafaring.class, "moored_ask", a.hull, a.max() ),
				Messages.get( Seafaring.class, "opt_board" ),
				Messages.get( Seafaring.class, "opt_haul" ) ){
			@Override
			protected void onSelect( int index ){
				int c = boatCell( hero );
				if (c == -1 || !hero.isAlive() || !hero.ready) return;
				if (index == 0){
					hero.curAction = new HeroAction.Move( c );
					hero.next();
				} else if (index == 1){
					haul( hero );
				}
			}
		} ) );
		return true;
	}

	// ------------------------------------------------------------------ steering (Hero.getCloser)

	/** Does the sea steer his next step toward target: aboard, swimming, or walking to his own boat. */
	public static boolean steers( Hero hero, int target ){
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null || target < 0 || target >= ow.length() || target == hero.pos) return false;
		return aboard( hero ) || swimming( hero ) || target == boatCell( hero );
	}

	/** The cell he steps onto next toward target, -1 for none (Hero.getCloser takes it). */
	public static int nextStep( Hero hero, int target ){
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null) return -1;
		if (aboard( hero )) return sailStep( hero, ow, target );
		if (swimming( hero )) return swimStep( hero, ow, target );
		return walkToBoat( hero, ow, target );
	}

	private static boolean[] vision( Hero hero, Level l ){
		return hero.fieldOfView != null && hero.fieldOfView.length == l.length() ? hero.fieldOfView : l.heroFOV;
	}

	//the water he can sail over: known open water, his own cell
	private static boolean[] seaMap( Hero hero, Level l ){
		boolean[] sea = new boolean[l.length()];
		for (int c = 0; c < sea.length; c++) sea[c] = water( l, c ) && known( l, c ) && l.insideMap( c );
		sea[hero.pos] = true;
		return sea;
	}

	private static int sailStep( Hero hero, OverworldLevel ow, int target ){
		if (water( ow, target )){
			if (ow.adjacent( hero.pos, target )) return Actor.findChar( target ) == null ? target : -1;
			PathFinder.Path p = Dungeon.findPath( hero, target, seaMap( hero, ow ), vision( hero, ow ), true );
			return p == null || p.isEmpty() ? -1 : p.getFirst();
		}
		//a cell ashore: onto it from beside it, else to the best landing for it
		if (ow.adjacent( hero.pos, target )) return landable( ow, target ) ? target : -1;
		boolean[] sea = seaMap( hero, ow );
		int[] landing = landing( hero, ow, target, sea );
		if (landing == null) return -1;
		if (ow.adjacent( hero.pos, landing[0] )) return landing[0];
		if (landing[1] == hero.pos) return -1;
		PathFinder.Path p = Dungeon.findPath( hero, landing[1], sea, vision( hero, ow ), true );
		return p == null || p.isEmpty() ? -1 : p.getFirst();
	}

	/**
	 * Where a hero aboard comes ashore for a target inland: of the open ground beside the water he can
	 * sail to, the nearest the target (then the shortest sail): {landing cell, the water beside it he
	 * steps off from}, or null for none.
	 */
	static int[] landing( Hero hero, Level l, int target, boolean[] sea ){
		PathFinder.buildDistanceMap( hero.pos, sea );
		int w = l.width(), tx = target % w, ty = target / w;
		int best = -1, via = -1;
		long bestScore = Long.MAX_VALUE;
		for (int c = 0; c < l.length(); c++){
			if (!landable( l, c ) || !known( l, c )) continue;
			int near = -1, d = Integer.MAX_VALUE;
			for (int n : PathFinder.NEIGHBOURS8){
				int s = c + n;
				if (s < 0 || s >= l.length() || !sea[s]) continue;
				int ds = PathFinder.distance[s];
				if (ds < d){
					d = ds;
					near = s;
				}
			}
			if (near == -1 || d == Integer.MAX_VALUE) continue;
			long score = (long) Math.max( Math.abs( c % w - tx ), Math.abs( c / w - ty ) ) * 100000L + d;
			if (score < bestScore){
				bestScore = score;
				best = c;
				via = near;
			}
		}
		return best == -1 ? null : new int[]{ best, via };
	}

	private static int swimStep( Hero hero, OverworldLevel ow, int target ){
		boolean[] pass = new boolean[ow.length()];
		for (int c = 0; c < pass.length; c++){
			pass[c] = known( ow, c ) && ow.insideMap( c ) && (water( ow, c ) || (ow.passable[c] && !ow.pit[c]));
		}
		pass[hero.pos] = true;
		if (ow.adjacent( hero.pos, target )) return pass[target] && Actor.findChar( target ) == null ? target : -1;
		PathFinder.Path p = Dungeon.findPath( hero, target, pass, vision( hero, ow ), true );
		return p == null || p.isEmpty() ? -1 : p.getFirst();
	}

	//to his own moored boat: over the ground he knows, the boat's cell his way in
	private static int walkToBoat( Hero hero, OverworldLevel ow, int boat ){
		if (ow.adjacent( hero.pos, boat )) return Actor.findChar( boat ) == null ? boat : -1;
		boolean[] pass = new boolean[ow.length()];
		for (int c = 0; c < pass.length; c++) pass[c] = ow.passable[c] && known( ow, c );
		pass[boat] = true;
		PathFinder.Path p = Dungeon.findPath( hero, boat, pass, vision( hero, ow ), true );
		return p == null || p.isEmpty() ? -1 : p.getFirst();
	}

	// ------------------------------------------------------------------ moving (Hero.move)

	/** The world cell of a window cell of the surface now, {wx, wy}, or null off the surface (Hero.move
	 *  takes his before the step: the step may slide the window under him). */
	public static int[] worldCell( int cell ){
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null || cell < 0 || cell >= ow.length()) return null;
		return new int[]{ wxOf( ow, cell ), wyOf( ow, cell ) };
	}

	/** He stepped from a world cell (worldCell, taken before the step) onto a window cell: the boat
	 *  sails on with him, or stays moored as he steps ashore, or takes him aboard as he steps onto it. */
	public static void moved( Hero hero, int[] from, int to ){
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null || from == null || to < 0 || to >= ow.length()) return;
		Afloat a = boat( hero );
		int fx = from[0], fy = from[1], tx = wxOf( ow, to ), ty = wyOf( ow, to );
		if (fx == tx && fy == ty) return;
		if (a != null && a.aboard && a.wx == fx && a.wy == fy){
			if (water( ow, to )){
				a.wx = tx;
				a.wy = ty;
				a.headX = Integer.signum( tx - fx );
				a.headY = Integer.signum( ty - fy );
			} else {
				a.setAboard( false );
				NetManager.heroLog( hero, Messages.get( Seafaring.class, a.kind == Boat.Kind.RAFT ? "ashore_raft" : "ashore_sail" ) );
				ow.sea().boatsChanged();
			}
		} else if (a != null && !a.aboard && a.wx == tx && a.wy == ty && water( ow, to )){
			a.setAboard( true );
			a.headX = Integer.signum( tx - fx );
			a.headY = Integer.signum( ty - fy );
			NetManager.heroLog( hero, Messages.get( Seafaring.class, "aboard" ) );
			GuideGraph.reveal( GuideGraph.TAG_SAIL );
			ow.sea().boatsChanged();
		}
		startSwimming( hero );
	}

	/** In deep water with nothing under him now: he swims. */
	static void startSwimming( Hero hero ){
		if (swimming( hero ) && hero.buff( Swimming.class ) == null){
			Buff.affect( hero, Swimming.class );
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Seafaring.class, "swimming" ) );
		}
	}

	// ------------------------------------------------------------------ speed (Hero.speed)

	/** His pace on the water: a raft's a walker's, a sailboat's half again (twice before the wind),
	 *  a swimmer's half; 1 ashore. */
	public static float speedFactor( Hero hero ){
		if (Dungeon.level == null) return 1f;
		if (aboard( hero )){
			Afloat a = boat( hero );
			if (a.kind == Boat.Kind.SAILBOAT && beforeTheWind( a )) return SAIL_BEFORE_WIND;
			return a.kind.speed;
		}
		return swimming( hero ) ? SWIM_SPEED : 1f;
	}

	/** Is the wind behind a boat's course (ClimateManager: the way the wind blows, compass degrees)? */
	static boolean beforeTheWind( Afloat a ){
		if (ClimateManager.localWindSpeed() < WIND_MIN || (a.headX == 0 && a.headY == 0)) return false;
		float course = (float) Math.toDegrees( Math.atan2( a.headX, -a.headY ) );
		if (course < 0) course += 360f;
		float diff = Math.abs( course - ClimateManager.surfaceWindDir() );
		if (diff > 180f) diff = 360f - diff;
		return diff <= WIND_ARC;
	}

	// ------------------------------------------------------------------ the boat's turn: the weather

	/** How rough the sea is: 0 calm, 1 choppy (rain, or a wind of ROUGH_WIND), 2 a storm. */
	public static int roughness(){
		if (ClimateManager.isStorming()) return 2;
		return ClimateManager.isRaining() || ClimateManager.localWindSpeed() >= ROUGH_WIND ? 1 : 0;
	}

	static final float ROUGH_WIND = 10f;
	//a wave's odds a turn: on the open deep (the shelf by a shore gets half), choppy for a raft,
	//choppy for a sailboat, in a storm
	static final float WAVE_RAFT = 0.10f, WAVE_SAIL = 0.03f, WAVE_STORM = 0.25f;

	/** Once a turn, his boat's (Afloat.act): a boat he is no longer on is left where it floats, and the
	 *  one he sails meets the weather. */
	static void boatTurn( Hero hero, Afloat a ){
		if (!a.aboard) return;
		OverworldLevel ow = surface( Dungeon.level );
		if (ow == null || ow.isMirror()){
			//off the surface with no step ashore (a scroll, a fall): the boat floats on where it was
			a.setAboard( false );
			return;
		}
		if (!aboard( hero )){
			//moved off it some other way (thrown, carried off, set down elsewhere): it floats on
			a.setAboard( false );
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Seafaring.class, "overboard" ) );
			ow.sea().boatsChanged();
			startSwimming( hero );
			return;
		}
		if (!water( ow, hero.pos )){
			//the water froze under it, or was filled in: it lies beached
			a.setAboard( false );
			NetManager.heroLog( hero, Messages.get( Seafaring.class, "beached" ) );
			ow.sea().boatsChanged();
			return;
		}
		int rough = roughness();
		if (rough == 2 && !a.stormTold){
			a.stormTold = true;
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Seafaring.class, a.kind == Boat.Kind.RAFT ? "storm_raft" : "storm_sail" ) );
		} else if (rough < 2){
			a.stormTold = false;
		}
		if (rough == 0) return;
		float odds = rough == 2 ? WAVE_STORM : a.kind == Boat.Kind.RAFT ? WAVE_RAFT : WAVE_SAIL;
		if (ow.map[hero.pos] != Terrain.DEEP_WATER) odds /= 2f;
		if (Random.Float() >= odds) return;
		//a wave breaks over the boat
		Buff.prolong( hero, Drenched.class, Drenched.DURATION );
		SeaFx.wave( hero );
		int wear = 0;
		if (a.kind == Boat.Kind.RAFT) wear = rough == 2 ? Random.IntRange( 3, 6 ) : Random.IntRange( 1, 2 );
		else if (rough == 2) wear = Random.IntRange( 2, 4 );
		if (wear > 0){
			NetManager.heroLog( hero, Messages.get( Seafaring.class, "wave_hull" ) );
			damage( hero, a, wear );
		} else {
			NetManager.heroLog( hero, Messages.get( Seafaring.class, "wave" ) );
		}
	}

	/** The hull takes a blow (a wave, a shark's teeth): half of it gone is warned of, then three
	 *  quarters, and at nothing the boat goes down. */
	public static void damage( Hero hero, Afloat a, int dmg ){
		if (dmg <= 0) return;
		a.hull -= dmg;
		if (a.hull <= 0){
			capsize( hero, a );
			return;
		}
		if (a.hull * 4 <= a.max() && a.told < 2){
			a.told = 2;
			NetManager.heroLog( hero, GLog.NEGATIVE + Messages.get( Seafaring.class, a.kind == Boat.Kind.RAFT ? "sinking_raft" : "sinking_sail" ) );
		} else if (a.hull * 2 <= a.max() && a.told < 1){
			a.told = 1;
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Seafaring.class, a.kind == Boat.Kind.RAFT ? "leaking_raft" : "leaking_sail" ) );
		}
	}

	/** The boat goes down under him: a plank or two of it floats, and he is in the water. */
	static void capsize( Hero hero, Afloat a ){
		OverworldLevel ow = surface( Dungeon.level );
		boolean wasAboard = a.aboard;
		Boat.Kind kind = a.kind;
		a.detach();
		NetManager.heroLog( hero, GLog.NEGATIVE + Messages.get( Seafaring.class, kind == Boat.Kind.RAFT ? "capsized_raft" : "capsized_sail" ) );
		if (ow == null) return;
		Buff.prolong( hero, Drenched.class, Drenched.DURATION );
		SeaFx.wreckage( hero.pos );
		//the timbers that stay afloat drift to the nearest shallows, a plank or two
		int ashore = -1;
		for (int r = 1; r <= 6 && ashore == -1; r++){
			for (int c : PathFinder.NEIGHBOURS8){
				int at = hero.pos + c * r;
				if (at >= 0 && at < ow.length() && ow.passable[at] && ow.insideMap( at )){
					ashore = at;
					break;
				}
			}
		}
		if (ashore != -1){
			Heap h = ow.drop( new Plank().quantity( Random.IntRange( 1, 2 ) ), ashore );
			if (h.sprite != null) h.sprite.drop();
		}
		if (wasAboard) startSwimming( hero );
		ow.sea().boatsChanged();
	}

	// ------------------------------------------------------------------ the moored boats

	/** Every boat moored on this level, its world cell and kind {wx, wy, kind ordinal}: every hero's of
	 *  the level that is not aboard his (the host's own, and his co-op guests'). The host's own counts
	 *  wherever he is: a surface being restored (a save loaded, a climb back up from the caves) is not
	 *  the game's level yet when its window is adopted. */
	public static ArrayList<int[]> moorings( OverworldLevel ow ){
		ArrayList<int[]> out = new ArrayList<>();
		if (ow.altitude() != 0) return out;
		ArrayList<Hero> heroes = OverworldLevel.heroesOn( ow );
		if (Dungeon.hero != null && !heroes.contains( Dungeon.hero )) heroes.add( Dungeon.hero );
		for (Hero h : heroes){
			Afloat a = boat( h );
			if (a != null && !a.aboard) out.add( new int[]{ a.wx, a.wy, a.kind.ordinal() } );
		}
		return out;
	}
}
