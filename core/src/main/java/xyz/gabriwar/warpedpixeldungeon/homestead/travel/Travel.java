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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WellFed;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Leashed;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Travel by the stones: from any HOME (its hearthstone) or attuned WAYSTONE to any other, on any
 * slice of the world. It costs HUNGER and nothing else - no time passes on the way, there is no fee
 * and nothing waylays the hero: half a point of hunger for each cell of the straight line between
 * the two stones, and HUNGER_PER_LEVEL more for each step of height between their slices (the
 * mountains' levels up, the caves' down), never more than MAX_HUNGER (a starving stomach's worth).
 * The hunger is paid as eating's is undone: out of a Well Fed hero's meal first (WellFed), the rest
 * onto his Hunger; what goes past starving hurts him as starving does (Hunger.affectHunger), and a
 * trip that would hurt him to death is refused (TOO_HUNGRY).
 *
 * The hero must stand beside the stone, his feet on the ground (not levitating, not rooted), with no
 * enemy in sight; in co-op only the host travels, and the whole party goes with him as with any
 * other party travel (InterlevelScene's RETURN, and NetManager relocating every guest to his landing
 * cell - even on a trip that stays on the same slice). An animal on a rope he holds, beside him,
 * comes too (leashed: on the animals' rope, animals/Leashed). He lands beside the stone he travelled
 * to - on the nearest free cell he could walk to from it when its own side is taken
 * (OverworldLevel.landingCell) - with a flash of its rune (TravelArrival).
 */
public final class Travel {

	private Travel(){}

	/** The most a trip costs in hunger: from fed to starving. */
	public static final int MAX_HUNGER = 450;
	/** The hunger of each step of height between the slices. */
	public static final int HUNGER_PER_LEVEL = 60;
	/** The colour of the stones' rune, and of its flash. */
	public static final int RUNE = 0x6FE8FF;

	/** What a stone travelled from or to is. */
	public enum Kind { HOME, WAYSTONE }

	/** A place to travel from or to: a home's hearthstone, or a waystone, by its world cell. */
	public static final class Place {
		public final Kind kind;
		/** The slice it stands on (WorldLayers.depthOf). */
		public final int depth;
		public final int wx, wy;
		/** A waystone's key (Waystones); 0 for a home. */
		public final long key;
		public final String name;

		public Place( Kind kind, int depth, int wx, int wy, long key, String name ){
			this.kind = kind;
			this.depth = depth;
			this.wx = wx;
			this.wy = wy;
			this.key = key;
			this.name = name;
		}

		public static Place of( HomesteadWorld.Home home ){
			return new Place( Kind.HOME, home.depth, home.wx, home.wy, 0, home.title() );
		}

		public static Place of( Waystone stone ){
			return new Place( Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, stone.wx, stone.wy, stone.key, stone.place );
		}

		public static Place of( TravelWorld.Stone stone ){
			return new Place( Kind.WAYSTONE, WorldLayers.SURFACE_DEPTH, stone.wx, stone.wy, stone.key, stone.name );
		}

		public int altitude(){
			return WorldLayers.altitudeOf( depth );
		}

		/** The same stone? */
		public boolean same( Place o ){
			return o != null && kind == o.kind && depth == o.depth && wx == o.wx && wy == o.wy && key == o.key;
		}
	}

	// ------------------------------------------------------------------ the cost

	/** The straight-line distance between two world cells, in cells (rounded). */
	public static int distance( int ax, int ay, int bx, int by ){
		long dx = ax - bx, dy = ay - by;
		return (int)Math.round( Math.sqrt( dx * dx + dy * dy ) );
	}

	/** The hunger of a trip of `distance` cells between two slices: half a point a cell and
	 *  HUNGER_PER_LEVEL a step of height, MAX_HUNGER at most. */
	public static int hungerCost( int distance, int altitudeFrom, int altitudeTo ){
		return Math.min( MAX_HUNGER, distance / 2 + HUNGER_PER_LEVEL * Math.abs( altitudeFrom - altitudeTo ) );
	}

	public static int distance( Place from, Place to ){
		return distance( from.wx, from.wy, to.wx, to.wy );
	}

	public static int cost( Place from, Place to ){
		return hungerCost( distance( from, to ), from.altitude(), to.altitude() );
	}

	/** How the cost reads: about how many meals (a ration's worth, Hunger.HUNGRY, is a meal), in
	 *  quarters, a quarter at least. A trip costs MAX_HUNGER at most, a meal and a half: six. */
	public static String meals( int hunger ){
		int quarters = Math.max( 1, Math.round( hunger * 4f / Hunger.HUNGRY ) );
		return Messages.get( Travel.class, "meals_" + quarters );
	}

	/** How much of a trip's hunger a Well Fed hero's meal takes (all of it, while it lasts): the rest
	 *  goes onto his Hunger. */
	public static int fromMeal( Hero hero, int cost ){
		WellFed fed = hero.buff( WellFed.class );
		return fed == null ? 0 : Math.max( 0, Math.min( cost, fed.turnsLeft() ) );
	}

	/** What the trip leaves the hero: 0 fed enough, 1 hungry, 2 starving. A Well Fed hero's meal
	 *  takes its share first (fromMeal); only the rest counts. */
	public static int leaves( Hero hero, int cost ){
		int rest = cost - fromMeal( hero, cost );
		if (rest <= 0) return 0;
		Hunger h = hero.buff( Hunger.class );
		int after = (h == null ? 0 : h.hunger()) + rest;
		return after >= Hunger.STARVING ? 2 : after >= Hunger.HUNGRY ? 1 : 0;
	}

	/** The health the trip costs the hero, rounded up: the hunger it puts past starving hurts as
	 *  starving does (Hunger.affectHunger: a thousandth of his health a point, and 1 for starting
	 *  to starve). 0 when it leaves him short of starving. */
	public static int hurt( Hero hero, int cost ){
		int rest = cost - fromMeal( hero, cost );
		Hunger h = hero.buff( Hunger.class );
		if (rest <= 0 || h == null) return 0;
		int now = h.hunger(), excess = now + rest - (int)Hunger.STARVING;
		if (excess < 0) return 0;
		return (int)Math.ceil( excess * hero.HT / 1000f ) + (now < Hunger.STARVING ? 1 : 0);
	}

	/** Would the trip starve the hero to death (hurt: as much as his health or more)? */
	public static boolean deadly( Hero hero, int cost ){
		return hurt( hero, cost ) >= hero.HP;
	}

	// ------------------------------------------------------------------ where to

	/** Everywhere the hero may travel from a stone: every home and every attuned waystone, on any
	 *  slice, but the stone itself; the homes first (oldest first), then the waystones, nearest first. */
	public static List<Place> destinations( Place from ){
		ArrayList<Place> homes = new ArrayList<>(), stones = new ArrayList<>();
		for (HomesteadWorld.Home h : HomesteadWorld.homes()){
			Place p = Place.of( h );
			if (!p.same( from )) homes.add( p );
		}
		TravelWorld tw = TravelWorld.get();
		if (tw != null){
			for (TravelWorld.Stone s : tw.attuned()){
				Place p = Place.of( s );
				if (!p.same( from )) stones.add( p );
			}
		}
		stones.sort( ( a, b ) -> Integer.compare( distance( from, a ), distance( from, b ) ) );
		homes.addAll( stones );
		return homes;
	}

	/** Is the place still there to travel to: its home standing, its waystone attuned? */
	public static boolean stillThere( Place p ){
		if (p.kind == Kind.HOME) return HomesteadWorld.homeAt( p.depth, p.wx, p.wy ) != null;
		TravelWorld tw = TravelWorld.get();
		return tw != null && tw.isAttuned( p.key );
	}

	// ------------------------------------------------------------------ may he go

	/** Why the hero may not travel now; each with its message. */
	public enum Refusal {
		GUEST, NOT_HERE, NOT_BESIDE, LEVITATING, ROOTED, ENEMIES, GONE, TOO_HUNGRY;

		public String message(){
			return Messages.get( Travel.class, "refuse_" + name().toLowerCase( Locale.ENGLISH ) );
		}
	}

	/** Why the hero may not travel from one stone to another now, or null when he may. */
	public static Refusal check( Hero hero, Place from, Place to ){
		Level level = Dungeon.level;
		if (hero != Dungeon.hero || hero.isRemote
				|| (level instanceof OverworldLevel && ((OverworldLevel) level).isMirror())) return Refusal.GUEST;
		if (!(level instanceof OverworldLevel) || Dungeon.depth != from.depth) return Refusal.NOT_HERE;
		Mob stone = stoneOf( level, from );
		if (stone == null) return Refusal.NOT_HERE;
		if (level.distance( hero.pos, stone.pos ) > 1) return Refusal.NOT_BESIDE;
		if (hero.flying || hero.buff( Levitation.class ) != null) return Refusal.LEVITATING;
		if (hero.rooted) return Refusal.ROOTED;
		if (enemyInSight( hero, level )) return Refusal.ENEMIES;
		if (to == null || !stillThere( to ) || to.same( from )) return Refusal.GONE;
		if (deadly( hero, cost( from, to ) )) return Refusal.TOO_HUNGRY;
		return null;
	}

	/** The stone of a place standing on a level, or null. */
	public static Mob stoneOf( Level level, Place p ){
		for (Mob m : level.mobs){
			if (p.kind == Kind.WAYSTONE && m instanceof Waystone && ((Waystone) m).key == p.key) return m;
			if (p.kind == Kind.HOME && m instanceof Hearthstone
					&& ((Hearthstone) m).wx == p.wx && ((Hearthstone) m).wy == p.wy) return m;
		}
		return null;
	}

	private static boolean enemyInSight( Hero hero, Level level ){
		if (hero.fieldOfView == null) return false;
		for (Mob m : level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && m.pos >= 0 && m.pos < hero.fieldOfView.length
					&& hero.fieldOfView[m.pos] && m.invisible <= 0) return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ going

	/**
	 * The hero travels, when he may (else he is told why, and false): the hunger is spent, an
	 * animal on a rope beside him is taken along, the rune flashes as he goes, and he leaves for the
	 * stone's slice, landing beside it. No time passes. In co-op the whole party goes with the host,
	 * on whatever slice the stone stands.
	 */
	public static boolean go( Hero hero, Place from, Place to ){
		Refusal r = check( hero, from, to );
		if (r != null){
			GLog.w( r.message() );
			return false;
		}
		Mob stone = stoneOf( Dungeon.level, from );
		if (stone != null) runeFlash( stone.pos );
		runeFlash( hero.pos );
		Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
		//check refused a trip whose hunger would kill him; should it all the same, the trip ends
		//with him: a dead hero's run is never carried on to a new level
		if (depart( hero, from, to ) < 0) return false;
		//the guests are taken to the host's landing cell when the floor changes (NetManager.
		//onLevelDescent); a trip on the same slice changes no floor, and their cells would point
		//into the window the arrival re-centres: forgetting the floor makes it relocate them too
		if (NetManager.isHost()) NetManager.resetLevelTracking();
		OverworldLevel.travelToAltitude( to.altitude() );
		return true;
	}

	/**
	 * Everything a trip does before the scene changes, which is all of it but the scene: the hunger
	 * spent (out of the meal first: fromMeal), the rope's animals taken off the level into the
	 * hero's TravelArrival, and the arrival set on the stone's world cell (OverworldLevel.arriveAt).
	 * The hero spends no time. Returns the hunger spent, or -1 when it killed him (and nothing else
	 * is done).
	 */
	static int depart( Hero hero, Place from, Place to ){
		int cost = cost( from, to );
		spendHunger( hero, cost );
		if (!hero.isAlive()) return -1;
		TravelArrival arrival = Buff.affect( hero, TravelArrival.class );
		arrival.set( to, takeLeashed( hero, Dungeon.level ) );
		OverworldLevel.arriveAt( to.wx, to.wy );
		GLog.i( Messages.get( Travel.class, "travelled", to.name ) );
		return cost;
	}

	//the trip's hunger: Hunger.affectHunger puts all of a cost on a Well Fed hero's meal and loses
	//what goes past it, so the meal's share goes there and, the meal eaten up, the rest onto Hunger
	private static void spendHunger( Hero hero, int cost ){
		Hunger h = hero.buff( Hunger.class );
		if (h == null) return;
		int meal = fromMeal( hero, cost );
		if (meal > 0) h.affectHunger( -meal );
		int rest = cost - meal;
		if (rest <= 0) return;
		WellFed fed = hero.buff( WellFed.class );
		if (fed != null) fed.detach();
		h.affectHunger( -rest );
	}

	//the animals on a rope the hero holds, beside him, taken off the level (off the scheduler, out
	//of sight) as a parked mob is: the hero's TravelArrival puts them down again beside him
	static ArrayList<Mob> takeLeashed( Hero hero, Level level ){
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : level.mobs.toArray( new Mob[0] )){
			if (!level.adjacent( m.pos, hero.pos ) || !leashed( m, hero )) continue;
			level.mobs.remove( m );
			Actor.remove( m );
			for (Buff b : m.buffs()) Actor.remove( b );
			m.clearTime();
			if (m.sprite != null) m.sprite.killAndErase();
			out.add( m );
		}
		return out;
	}

	/** Is the animal on a rope this one holds (animals/Leashed.holder)? An animal tied to a post,
	 *  only penned, or held by someone else stays where it is. */
	public static boolean leashed( Char animal, Char holder ){
		Leashed rope = animal.buff( Leashed.class );
		return holder != null && rope != null && rope.holder() == holder;
	}

	/** The rune's flash over a cell, where the hero sees it. */
	public static void runeFlash( int cell ){
		Level level = Dungeon.level;
		if (level == null || level.heroFOV == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		PointF p = DungeonTilemap.tileCenterToWorld( cell );
		FxRecipes.bloom( p.x, p.y - 4, RUNE, 31, 0.8f, 0.6f );
	}
}
