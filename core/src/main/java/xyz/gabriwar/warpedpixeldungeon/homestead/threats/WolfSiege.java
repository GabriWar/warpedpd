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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * A wolf siege on a home (HomeThreats.siegeNight): a pack of PACK_MIN..PACK_MAX wolves of the
 * home's country - the forest's brown, the snow's grey - comes in at the edge of the home's ground
 * howling, and hunts the hero as wolves do wherever it can reach him. When it cannot - he is shut
 * in behind walls, barred doors, palisades, fences or gates - the pack CIRCLES him, each wolf
 * keeping RING_MIN..RING_MAX cells off and going round, howling now and then, and TESTS the fences
 * and gates it passes: one turn in TEST_ODDS a wolf beside one bites at it, a test of a quarter of
 * its bite (TEST_SHARE). It never touches a wall, a window, a door or a palisade. At dawn (or with
 * its home gone) the pack slinks off out of sight.
 */
public final class WolfSiege {

	private WolfSiege(){}

	/** The ring the pack keeps round a hero it cannot reach, in cells. */
	public static final int RING_MIN = 4, RING_MAX = 8;
	/** A turn in this many a circling wolf beside a fence or a gate bites at it... */
	public static final int TEST_ODDS = 4;
	/** ...with its damage roll over this: a test, not a kill. */
	public static final int TEST_SHARE = 4;
	/** A turn in this many a circling wolf howls. */
	public static final int HOWL_ODDS = 60;
	//a wolf making off: out of everyone's sight, or this long about it, and it is gone
	static final int LEAVE_TURNS = 40, UNSEEN = 21;

	/** A siege wolf's part in its siege, kept with the wolf. */
	public static final class Pack {
		/** Its home's key (HomeThreats.key) and its siege's id (ThreatWorld.Event.id). */
		public String home;
		public long event;
		/** Which way round it circles: 1 or -1. */
		int dir = 1;
		boolean leaving;
		//turns spent making off (not saved: a reload gives it the full while again)
		int fled;

		private static final String HOME = "siege_home", EVENT = "siege_event", DIR = "siege_dir", LEAVING = "siege_leaving";

		public void store( Bundle bundle ){
			bundle.put( HOME, home == null ? "" : home );
			bundle.put( EVENT, event );
			bundle.put( DIR, dir );
			bundle.put( LEAVING, leaving );
		}

		public void restore( Bundle bundle ){
			home = bundle.getString( HOME );
			event = bundle.getLong( EVENT );
			dir = bundle.contains( DIR ) ? bundle.getInt( DIR ) : 1;
			leaving = bundle.getBoolean( LEAVING );
		}
	}

	/** The siege begins: its pack set down at the edge of the home's ground, howling. The wolves put down. */
	public static int start( OverworldLevel level, HomesteadWorld.Home home, ThreatWorld.Event e, int size, boolean grey ){
		int hearth = HomeThreats.hearth( level, home );
		if (hearth == -1) return 0;
		//the pack comes in together, from one side
		float bearing = Random.Float();
		int placed = 0, first = -1;
		for (int i = 0; i < size; i++){
			int c = HomeThreats.edgeCell( level, hearth, bearing + i * 0.04f );
			if (c == -1) continue;
			Mob wolf = grey ? new SiegeGrayWolf() : new SiegeBrownWolf();
			Pack p = ((SiegeWolf) wolf).pack();
			p.home = HomeThreats.key( home );
			p.event = e.id;
			p.dir = i % 2 == 0 ? 1 : -1;
			HomeThreats.add( level, wolf, c );
			if (Dungeon.hero != null) wolf.aggro( Dungeon.hero );
			placed++;
			if (first == -1) first = c;
		}
		e.came += placed;
		if (placed > 0){
			int w = level.width();
			String from = HomeThreats.bearing( level.worldX + first % w - home.wx, level.worldY + first / w - home.wy );
			GLog.w( Messages.get( WolfSiege.class, "came", home.title(), from ) );
			if (HomeThreats.live( level )) SpatialSound.play( Assets.Sounds.HOWL, first );
		}
		return placed;
	}

	/** Is a siege wolf's night over: dawn come, its home or its siege gone? */
	static boolean over( Pack p ){
		DayNightCycle.Phase phase = DayNightCycle.phase();
		if (phase == DayNightCycle.Phase.DAWN || phase == DayNightCycle.Phase.DAY) return true;
		HomesteadWorld.Home home = HomeThreats.home( p.home );
		ThreatWorld world = ThreatWorld.get();
		return home == null || world == null || world.event( p.home, p.event ) == null;
	}

	/** A siege wolf's turn. */
	static boolean act( Mob self, SiegeWolf w ){
		Pack p = w.pack();
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || self.alignment != Char.Alignment.ENEMY) return w.wolfAct();
		if (!p.leaving && over( p )) p.leaving = true;
		if (p.leaving) return leave( self, w, (OverworldLevel) level );
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || self.paralysed > 0) return w.wolfAct();
		w.wolfLook();
		Ground ground = Ground.of( level, hero.pos );
		//a hero in the open is hunted as any wolf hunts
		if (ground.reaches( self.pos )){
			self.aggro( hero );
			return w.wolfAct();
		}
		//one of his allies at its throat is fought, as any wolf fights
		for (int n : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar( self.pos + n );
			if (c != null && c.alignment == Char.Alignment.ALLY) return w.wolfAct();
		}

		if (HomeThreats.live( (OverworldLevel) level ) && Random.Int( HOWL_ODDS ) == 0){
			SpatialSound.play( Assets.Sounds.HOWL, self.pos, 0.8f, Random.Float( 0.9f, 1.1f ) );
		}
		//a fence or a gate beside it tried now and then
		int fence = fenceBeside( level, self.pos );
		if (fence != -1 && Random.Int( TEST_ODDS ) == 0){
			return w.wolfBite( Breaker.Order.strike( fence, Math.max( 1, self.damageRoll() / TEST_SHARE ) ) );
		}
		int d = level.distance( self.pos, hero.pos );
		if (d < RING_MIN && w.wolfAway( hero.pos )) return true;
		int step = d > RING_MAX ? ground.towardRing( level, self.pos ) : roundRing( level, self.pos, hero.pos, p.dir );
		if (step != -1 && Actor.findChar( step ) == null) return w.wolfStep( step );
		//blocked going round: the other way next time
		p.dir = -p.dir;
		return w.wolfWait();
	}

	/** A fence or a gate beside a cell, shut, that a siege wolf may bite at; -1 for none. */
	static int fenceBeside( Level level, int cell ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (Breaker.breakable( level, c, Breaker.Rule.FENCES )) return c;
		}
		return -1;
	}

	//a step round the ring, the circling way: the neighbour within the ring that goes furthest round
	//(and keeps nearest its middle); -1 for none going round at all
	static int roundRing( Level level, int from, int hero, int dir ){
		int w = level.width();
		int hx = hero % w, hy = hero / w;
		double a0 = Math.atan2( from / w - hy, from % w - hx );
		int best = -1;
		double bestScore = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (c < 0 || c >= level.length() || !level.passable[c] || level.pit[c] || Actor.findChar( c ) != null) continue;
			int d = level.distance( c, hero );
			if (d < RING_MIN || d > RING_MAX) continue;
			double da = Math.atan2( c / w - hy, c % w - hx ) - a0;
			while (da > Math.PI) da -= 2 * Math.PI;
			while (da < -Math.PI) da += 2 * Math.PI;
			double score = da * dir - 0.02 * Math.abs( d - (RING_MIN + RING_MAX) / 2 );
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	/**
	 * The ground round the hero as the whole pack reads it, worked out once a turn for all of its
	 * wolves (a search over the whole window each would be dear): the cells from which he can be
	 * walked to (a wolf on one hunts him as any wolf does), and how many steps every cell is from
	 * the nearest cell of the ring RING_MIN..RING_MAX round him that can be stood on. Kept until the
	 * turn, the hero's cell, the level or what is built on it changes.
	 */
	static final class Ground {
		//the last one worked out (its level held weakly: a slice left behind is let go)
		private static Ground last;

		final WeakReference<Level> level;
		final int hero, turn, built;
		final int[] toHero, toRing;

		private Ground( Level level, int hero, int turn, int built ){
			this.level = new WeakReference<>( level );
			this.hero = hero;
			this.turn = turn;
			this.built = built;
			PathFinder.buildDistanceMap( hero, level.passable );
			toHero = PathFinder.distance.clone();
			toRing = ringDistances( level, hero );
		}

		static Ground of( Level level, int hero ){
			int turn = (int) Actor.now();
			int built = level instanceof OverworldLevel ? ((OverworldLevel) level).homestead().version() : 0;
			Ground g = last;
			if (g == null || g.level.get() != level || g.hero != hero || g.turn != turn || g.built != built){
				g = last = new Ground( level, hero, turn, built );
			}
			return g;
		}

		/** Can the hero be walked to from this cell? */
		boolean reaches( int cell ){
			return toHero[cell] != Integer.MAX_VALUE;
		}

		/** A step from a cell toward the nearest cell of the ring it can reach, onto a free cell
		 *  beside it; -1 for none. */
		int towardRing( Level level, int from ){
			int best = -1, bestD = toRing[from];
			for (int n : PathFinder.NEIGHBOURS8){
				int c = from + n;
				if (c < 0 || c >= level.length() || Actor.findChar( c ) != null) continue;
				if (toRing[c] < bestD){
					bestD = toRing[c];
					best = c;
				}
			}
			return best;
		}

		//steps to the nearest ring cell that can be stood on, over open ground (no pits), from
		//every cell: a search from all of the ring's cells at once
		private static int[] ringDistances( Level level, int hero ){
			int len = level.length(), w = level.width(), hx = hero % w, hy = hero / w;
			int[] dist = new int[len];
			Arrays.fill( dist, Integer.MAX_VALUE );
			int[] queue = new int[len];
			int head = 0, tail = 0;
			for (int y = Math.max( 1, hy - RING_MAX ); y <= Math.min( level.height() - 2, hy + RING_MAX ); y++){
				for (int x = Math.max( 1, hx - RING_MAX ); x <= Math.min( w - 2, hx + RING_MAX ); x++){
					int c = x + y * w;
					int d = level.distance( c, hero );
					if (d < RING_MIN || d > RING_MAX || !level.passable[c] || level.pit[c]) continue;
					dist[c] = 0;
					queue[tail++] = c;
				}
			}
			while (head < tail){
				int c = queue[head++];
				for (int n : PathFinder.NEIGHBOURS8){
					int m = c + n;
					if (m < 0 || m >= len || dist[m] != Integer.MAX_VALUE || !level.passable[m] || level.pit[m]) continue;
					//no wrapping round the window's sides
					if (Math.abs( m % w - c % w ) > 1) continue;
					dist[m] = dist[c] + 1;
					queue[tail++] = m;
				}
			}
			return dist;
		}
	}

	//dawn: away from the heroes, and gone the moment none of them sees it (or once it has taken
	//long enough about it)
	private static boolean leave( Mob self, SiegeWolf w, OverworldLevel level ){
		Pack p = w.pack();
		if (self.paralysed > 0) return w.wolfWait();
		if (++p.fled > LEAVE_TURNS
				|| (OverworldLevel.heroDistance( level, self.pos ) > UNSEEN && !NetManager.anyHeroSees( self.pos ))){
			OverworldLevel.vanish( level, self );
			return true;
		}
		Hero hero = Dungeon.hero;
		if (hero != null && hero.isAlive() && w.wolfAway( hero.pos )) return true;
		return w.wolfWait();
	}

	/** A siege wolf fell: its siege's tally. */
	static void died( SiegeWolf w ){
		ThreatWorld world = ThreatWorld.get();
		if (world != null) world.slain( w.pack().home, w.pack().event );
	}
}
