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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.audio.WallBreak;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

/**
 * Hunters walled out of what the hero built. Built walls, windows, barred doors, palisades,
 * fences and shut gates stand in every monster's way (a plain door opens to anyone); a monster
 * hunting someone it cannot reach because of them either breaks in or waits outside.
 *
 * The BREAKERS - raiders (village and home raids alike), yetis, brutes, and anything carrying an
 * axe or a hammer - work out the cheapest way in, treating every built piece as a way through
 * that costs a step and a turn for each HP_PER_TURN of its health, and hit the first piece on it
 * with their own damage (Homestead.attackBuilt): the weakest wall on the shortest way in, a door
 * before stone; the way is kept for REPLAN turns while its piece stands and its quarry stays about
 * where it was (the search is the dear part). Everyone else WAITS OUTSIDE: for LURK_TURNS turns it
 * paces about the place it last saw its quarry - within LURK_REACH of it, or a step past the
 * nearest it can get - and goes back to its wandering after that, until it is no longer walled out
 * (FORGET turns without being) or hunts someone else. A siege's wolves keep their own ways
 * (SiegeWolf): they circle, and try only the fences and the gates.
 *
 * Mob.Hunting asks walledOut when it cannot get any closer, and carries the Order out
 * (Mob.obeyBreaker); nothing is searched unless something built stands near the two of them.
 * Host only: everything here changes the world.
 */
public final class Breaker {

	private Breaker(){}

	/** A turn of a breaker's for every this much of a piece's health, in its way's cost. */
	public static final int HP_PER_TURN = 10;
	/** How long a monster walled out waits outside, and how near the spot it last saw its quarry it
	 *  paces (a step past the nearest it can get, when that is further). */
	public static final int LURK_TURNS = 80, LURK_REACH = 3;
	/** A breaker that lost sight of its quarry still breaks for him while he is this near. */
	public static final int SCENT = 10;
	/** How far round the hunter and its quarry the way in is looked for. */
	static final int MARGIN = 10, MAX_SPAN = 72;
	/** A way in (or a waiting monster's verdict that it is walled out) is kept this many turns, while
	 *  its quarry stays within REGOAL of where it was. */
	static final int REPLAN = 10, REGOAL = 2;
	/** A waiting monster that has not been walled out for this many turns stops waiting. */
	static final int FORGET = 10;

	/** What a monster may break: nothing, a siege wolf's fences and gates, or every built piece. */
	public enum Rule { NONE, FENCES, ALL }

	/** What a walled-out monster does this turn (Mob.obeyBreaker). */
	public static final class Order {
		public enum Kind { STRIKE, STEP, WAIT }

		public final Kind kind;
		public final int cell;
		/** A strike's damage; 0 for the monster's own damage roll. */
		public final int damage;

		Order( Kind kind, int cell, int damage ){
			this.kind = kind;
			this.cell = cell;
			this.damage = damage;
		}

		static Order strike( int cell, int damage ){ return new Order( Kind.STRIKE, cell, damage ); }
		static Order step( int cell ){ return new Order( Kind.STEP, cell, 0 ); }
		static Order idle(){ return new Order( Kind.WAIT, -1, 0 ); }
	}

	/** What this monster may break. */
	public static Rule ruleOf( Mob m ){
		if (m instanceof SiegeWolf) return Rule.FENCES;
		if (m instanceof Raider || m instanceof Yeti || m instanceof Brute) return Rule.ALL;
		if (m instanceof Statue && axe( ((Statue) m).weapon() )) return Rule.ALL;
		return Rule.NONE;
	}

	//an axe or a hammer: a tool for doors
	private static boolean axe( Weapon w ){
		return w instanceof Axe || w instanceof BattleAxe || w instanceof Greataxe || w instanceof HandAxe
				|| w instanceof WarHammer || w instanceof Mace;
	}

	/** Is the cell a built piece standing in the way (solid now) that this rule breaks? */
	public static boolean breakable( Level level, int cell, Rule rule ){
		if (rule == Rule.NONE || cell < 0 || cell >= level.length() || level.passable[cell]) return false;
		BuildPiece p = Homestead.pieceAt( level, cell );
		if (p == null || p.kind != BuildPiece.Kind.TERRAIN || !p.destructible()) return false;
		if (rule == Rule.FENCES) return p.terrain == Terrain.FENCE || p.terrain == Terrain.FENCE_GATE;
		return true;
	}

	// ----------------------------------------------------------------- the hunt walled out

	/**
	 * Mob.Hunting, when it cannot get any closer to its target: an order when it is a built piece
	 * that keeps the monster out (a blow, a step toward the piece to break, or a turn waiting
	 * outside), null for the hunt's own way (anything else in the way, no hero near, not a slice).
	 */
	public static Order walledOut( Mob mob, Char enemy, int target, boolean enemyInFOV ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror()
				|| mob.alignment != Char.Alignment.ENEMY || ((OverworldLevel) level).homestead().builtCount() == 0){
			return null;
		}
		int goal = -1;
		if (enemy != null && enemy.isAlive() && (enemyInFOV || level.distance( mob.pos, enemy.pos ) <= SCENT)){
			goal = enemy.pos;
		} else if (target >= 0 && target < level.length() && target != mob.pos){
			goal = target;
		}
		if (goal == -1 || !builtBetween( (OverworldLevel) level, mob.pos, goal )) return null;

		Rule rule = ruleOf( mob );
		if (rule != Rule.NONE){
			Order o = planFor( mob, level, goal, rule );
			if (o != null) return o;
		}
		return lurk( mob, level, goal, target >= 0 && target < level.length() ? target : goal,
				enemy == null ? -1 : enemy.id() );
	}

	/** Does anything built stand in the way (solid) within the reach a way between two window cells
	 *  is looked for in (path's box)? Nothing built there: nothing to be walled out by. */
	static boolean builtBetween( OverworldLevel level, int from, int goal ){
		int w = level.width();
		int x0 = Math.min( from % w, goal % w ) - MARGIN, x1 = Math.max( from % w, goal % w ) + MARGIN;
		int y0 = Math.min( from / w, goal / w ) - MARGIN, y1 = Math.max( from / w, goal / w ) + MARGIN;
		final boolean[] found = { false };
		level.homestead().forEachBuilt( level.worldX + x0, level.worldY + y0, level.worldX + x1 + 1, level.worldY + y1 + 1,
				( x, y, b ) -> {
					if (found[0]) return;
					int c = level.localCell( x, y );
					if (c != -1 && !level.passable[c]) found[0] = true;
				} );
		return found[0];
	}

	/**
	 * A breaker's way in from `from` to `goal`: a blow at the first built piece on the cheapest
	 * way when it stands beside him, else a step toward the open cell before it; null when that
	 * way crosses nothing built (others in the way: no breaking for that) or there is none.
	 */
	public static Order plan( Level level, int from, int goal, Rule rule ){
		int[] way = path( level, from, goal, rule );
		return way == null ? null : follow( level, from, way, goal, rule );
	}

	//a breaker's last way in (not saved: worked out again after a load)
	private static final class Plan {
		final WeakReference<Level> level;
		final int goal;
		final float at;
		final int[] way;

		Plan( Level level, int goal, float at, int[] way ){
			this.level = new WeakReference<>( level );
			this.goal = goal;
			this.at = at;
			this.way = way;
		}
	}

	private static final WeakHashMap<Mob, Plan> plans = new WeakHashMap<>();

	/** plan, for a breaker on its way: its last way in is followed again while it holds - for
	 *  REPLAN turns, its quarry within REGOAL of where it was, the piece it makes for still one it
	 *  breaks - and looked for afresh otherwise. */
	public static Order planFor( Mob mob, Level level, int goal, Rule rule ){
		float now = Actor.now();
		Plan p = plans.get( mob );
		if (p != null && p.level.get() == level && now - p.at < REPLAN && level.distance( p.goal, goal ) <= REGOAL){
			Order o = follow( level, mob.pos, p.way, p.goal, rule );
			if (o != null) return o;
		}
		int[] way = path( level, mob.pos, goal, rule );
		if (way == null){
			plans.remove( mob );
			return null;
		}
		plans.put( mob, new Plan( level, goal, now, way ) );
		return follow( level, mob.pos, way, goal, rule );
	}

	//the order on a way in, from where the breaker stands: a blow at the first built piece still
	//standing on it when beside it, else a step toward the open cell before it; null when it
	//crosses nothing standing, or no longer leads anywhere from here
	private static Order follow( Level level, int from, int[] way, int goal, Rule rule ){
		for (int i = 0; i < way.length; i++){
			int c = way[i];
			if (c == goal || level.passable[c]) continue;
			if (!breakable( level, c, rule )) return null;
			if (level.adjacent( from, c )) return Order.strike( c, 0 );
			return i == 0 ? null : Order.step( way[i - 1] );
		}
		return null;
	}

	/**
	 * The cheapest way from `from` to `goal` (the cells after `from`, ending on goal): open ground a
	 * step a cell, and the built pieces the rule breaks a step and a turn for every HP_PER_TURN of
	 * their health. Looked for within MARGIN round the two (null past MAX_SPAN across, or no way).
	 */
	public static int[] path( Level level, int from, int goal, Rule rule ){
		int w = level.width();
		int fx = from % w, fy = from / w, gx = goal % w, gy = goal / w;
		int x0 = Math.max( 1, Math.min( fx, gx ) - MARGIN ), x1 = Math.min( w - 2, Math.max( fx, gx ) + MARGIN );
		int y0 = Math.max( 1, Math.min( fy, gy ) - MARGIN ), y1 = Math.min( level.height() - 2, Math.max( fy, gy ) + MARGIN );
		if (x1 - x0 > MAX_SPAN || y1 - y0 > MAX_SPAN) return null;
		int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
		int[] dist = new int[bw * bh];
		int[] prev = new int[bw * bh];
		Arrays.fill( dist, Integer.MAX_VALUE );
		PriorityQueue<Long> open = new PriorityQueue<>();
		int start = (fx - x0) + (fy - y0) * bw, end = (gx - x0) + (gy - y0) * bw;
		dist[start] = 0;
		prev[start] = -1;
		open.add( (long) start );
		while (!open.isEmpty()){
			long e = open.poll();
			int at = (int)(e & 0xFFFFFFFFL), cost = (int)(e >>> 32);
			if (cost > dist[at]) continue;
			if (at == end) break;
			int cx = at % bw + x0, cy = at / bw + y0, c = cx + cy * w;
			for (int n : PathFinder.NEIGHBOURS8){
				int m = c + n;
				int mx = m % w, my = m / w;
				if (mx < x0 || mx > x1 || my < y0 || my > y1) continue;
				int step = m == goal ? 1 : stepCost( level, m, rule );
				if (step < 0) continue;
				int j = (mx - x0) + (my - y0) * bw;
				if (cost + step < dist[j]){
					dist[j] = cost + step;
					prev[j] = at;
					open.add( ((long) dist[j] << 32) | j );
				}
			}
		}
		if (dist[end] == Integer.MAX_VALUE) return null;
		ArrayList<Integer> back = new ArrayList<>();
		for (int at = end; at != start; at = prev[at]) back.add( (at % bw + x0) + (at / bw + y0) * w );
		int[] out = new int[back.size()];
		for (int i = 0; i < out.length; i++) out[i] = back.get( back.size() - 1 - i );
		return out;
	}

	//what stepping onto a cell costs a breaker of this rule: a step on open ground, more through a
	//piece it breaks, never through anything else
	private static int stepCost( Level level, int cell, Rule rule ){
		if (level.passable[cell]) return level.pit[cell] ? -1 : 1;
		if (!breakable( level, cell, rule )) return -1;
		return 1 + (Homestead.hpAt( level, cell ) + HP_PER_TURN - 1) / HP_PER_TURN;
	}

	/** Does a way cross anything built standing in the way? */
	static boolean crossesBuilt( Level level, int[] path, int goal ){
		for (int c : path) if (c != goal && !level.passable[c] && Homestead.isBuilt( level, c )) return true;
		return false;
	}

	// ----------------------------------------------------------------- waiting outside

	/**
	 * A walled-out monster's patience: how many more turns it waits outside, for whom (its quarry's
	 * id, -1 for none) and where; when it last made sure something built keeps it out, and how near
	 * the spot it can get. It goes once the monster has not been walled out for FORGET turns (it
	 * got in, or gave up the hunt) or hunts someone else.
	 */
	public static class Lurking extends Buff {

		int left = LURK_TURNS;
		int spot = -1;
		int quarry = -1;
		//turns until the walled-out check is made again, turns since it was last walled out, and
		//how far from the spot it paces (-1 until worked out)
		int recheck, quiet, within = -1;

		@Override
		public boolean act(){
			if (++quiet > FORGET) detach();
			spend( TICK );
			return true;
		}

		private static final String LEFT = "left", SPOT = "spot", QUARRY = "quarry", RECHECK = "recheck", QUIET = "quiet";

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( LEFT, left );
			bundle.put( SPOT, spot );
			bundle.put( QUARRY, quarry );
			bundle.put( RECHECK, recheck );
			bundle.put( QUIET, quiet );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			left = bundle.getInt( LEFT );
			spot = bundle.getInt( SPOT );
			quarry = bundle.contains( QUARRY ) ? bundle.getInt( QUARRY ) : -1;
			recheck = bundle.getInt( RECHECK );
			quiet = bundle.getInt( QUIET );
		}
	}

	//a monster that cannot break what keeps it out paces about the nearest it can get to the spot
	//it last saw its quarry, for as long as its patience lasts - only when it is something built
	//that keeps it out (made sure of every REPLAN turns, or when its quarry moves off)
	private static Order lurk( Mob mob, Level level, int goal, int lastSeen, int quarry ){
		Lurking l = mob.buff( Lurking.class );
		if (l != null && l.quarry != quarry){
			//someone else hunted: its patience anew
			l.detach();
			l = null;
		}
		if (l != null) l.quiet = 0;
		if (l != null && l.left <= 0) return null;
		if (l == null || --l.recheck <= 0 || level.distance( l.spot, lastSeen ) > REGOAL){
			int[] way = path( level, mob.pos, goal, Rule.ALL );
			if (way == null || !crossesBuilt( level, way, goal )) return null;
			if (l == null){
				l = Buff.affect( mob, Lurking.class );
				l.quarry = quarry;
			}
			l.recheck = REPLAN;
			l.within = -1;
		}
		l.left--;
		l.spot = lastSeen;

		//pacing about the spot already: it mostly stays put
		if (l.within >= 0 && level.distance( mob.pos, l.spot ) <= l.within && Random.Int( 3 ) != 0) return Order.idle();

		//the open cells it can reach about the spot: within LURK_REACH of it, or a step past the
		//nearest it can get when that is further
		int w = level.width(), sx = l.spot % w, sy = l.spot / w, r = LURK_REACH + MARGIN;
		PathFinder.buildDistanceMap( mob.pos, level.passable, 2 * (level.distance( mob.pos, l.spot ) + r) );
		ArrayList<Integer> reach = new ArrayList<>();
		int nearest = Integer.MAX_VALUE;
		for (int y = Math.max( 1, sy - r ); y <= Math.min( level.height() - 2, sy + r ); y++){
			for (int x = Math.max( 1, sx - r ); x <= Math.min( w - 2, sx + r ); x++){
				int c = x + y * w;
				if (PathFinder.distance[c] == Integer.MAX_VALUE || level.pit[c]) continue;
				reach.add( c );
				nearest = Math.min( nearest, level.distance( c, l.spot ) );
			}
		}
		int within = l.within = Math.max( LURK_REACH, nearest + 1 );
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c : reach){
			if (level.distance( c, l.spot ) <= within && c != mob.pos && Actor.findChar( c ) == null) spots.add( c );
		}
		return spots.isEmpty() ? Order.idle() : Order.step( Random.element( spots ) );
	}

	// ----------------------------------------------------------------- the blow

	/**
	 * A monster's blow at the built piece on a cell (Homestead.attackBuilt: on the piece, else the
	 * roof over it), with the crack of wood or the chip of stone and a few chips flying where it
	 * shows; a piece that gives way makes its own crash. True when anything was hit.
	 */
	public static boolean strike( Char attacker, int cell, int damage ){
		Level level = Dungeon.level;
		BuildPiece p = Homestead.pieceAt( level, cell );
		boolean wood = p != null && (p.kind == BuildPiece.Kind.TERRAIN ? BuiltTerrain.wooden( p.terrain ) : p.flammable);
		boolean hit = Homestead.attackBuilt( attacker, cell, Math.max( 1, damage ) );
		if (hit && level instanceof OverworldLevel && HomeThreats.live( (OverworldLevel) level )
				&& Homestead.pieceAt( level, cell ) != null){
			if (wood){
				SpatialSound.play( Assets.Sounds.WOOD_BREAK, cell, 0.45f, Random.Float( 1.15f, 1.35f ) );
			} else {
				WallBreak.play( cell, 0.35f, Random.Float( 1.25f, 1.45f ) );
				if (level.heroFOV != null && level.heroFOV[cell]) CellEmitter.get( cell ).burst( Speck.factory( Speck.ROCK ), 2 );
			}
		}
		return hit;
	}
}
