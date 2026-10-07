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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

/**
 * Someone on the road between two places (a Traveller, the town watch's RoadPatrol): walks
 * a route RoadTraffic lays out and keeps to its clock - wherever the schedule says he should
 * have got to by now, his mark, he makes his way there along the road, a leg at a time (a
 * path worked out once a leg, some twelve cells on). Fallen far behind where nobody can see,
 * he simply picks up where his walk has got to. Never stored in the parked store
 * (OverworldLevel.park): the next look at the road puts whoever should be out there back on
 * it. Nobody can hurt him.
 */
public abstract class RoadWalker extends NPC {

	//cells behind his mark before one nobody sees is simply set down on it
	private static final int CATCH_UP = 12;
	//how far along the route each leg of the walk reaches past his mark
	private static final int LEG = 12;
	//turns a walker whose walk is over may stay in sight before going in anyway
	private static final int LINGER = 60;
	//how long a walker with no way along the road goes across country
	private static final int OFF_ROAD_TURNS = 20;
	//turns before a walker whose way could not be found looks again
	private static final int RETRY = 5;

	//the walk: world-cell polyline x0,y0,x1,y1,... (RoadTraffic)
	public float[] route = new float[0];
	//the traffic day (RoadTraffic.day) the walk belongs to
	public int day = -1;

	//none of this is bundled: worked out again after a load
	private boolean offRoad = false;
	private int offRoadLeft = 0, overdue = 0, retryIn = 0, blocked = 0, avoidCell = -1;
	//the cell this leg makes for (and which way along the route it was aimed, in which window),
	//and the path there (and the window it was found in)
	private int goal = -1, goalDir = 0, goalVersion = -1;
	private PathFinder.Path way;
	private int wayGoal = -1, wayVersion = -1;

	//the road's mask for one search, shared by every walker (actor thread only)
	private static boolean[] scratch;

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	protected final boolean act() {
		if (sprite == null || !(Dungeon.level instanceof OverworldLevel)){
			spend( TICK );
			return true;
		}
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		//a walk an older build cannot read, or one knocked down into a slice below: he is gone
		if (route.length < 4 || ow.altitude() != 0){
			spend( TICK );
			vanish();
			return true;
		}
		long t = LagMonitor.begin();
		if (ow.heroFOV[pos]) Bestiary.setSeen( getClass() );
		if (offRoadLeft > 0 && --offRoadLeft == 0) offRoad = false;
		boolean done = walk( ow, RoadTraffic.now() );
		LagMonitor.end( "OW walkers", t );
		return done;
	}

	/** The walker's turn on the surface: spends time (or returns false with an attack pending). */
	protected abstract boolean walk( OverworldLevel ow, long now );

	/** How far along the route the point nearest where he stands is. */
	protected float here( OverworldLevel ow ){
		return RoadTraffic.project( route, ow.worldX() + pos % ow.width(), ow.worldY() + pos / ow.width() );
	}

	/** Keeps to the schedule: makes for his mark, the point s cells along the route, heading `dir`
	 *  along it (1 on, -1 back, 0 standing at the end) at `pace` cells a turn. Spends. */
	protected boolean follow( OverworldLevel ow, float s, float pace, int dir ){
		float mine = here( ow );
		boolean seen = NetManager.anyHeroSees( pos );
		//one a hero is watching never has his mark run more than a catch-up ahead of him: the
		//traffic clock moves on the host's turns, which a hasted hero takes twice as fast
		if (seen && dir > 0) s = Math.min( s, mine + CATCH_UP - 1 );
		if (seen && dir < 0) s = Math.max( s, mine - (CATCH_UP - 1) );
		float[] p = RoadTraffic.point( route, s );
		//his walk has gone on past the window's edge: out of anyone's sight, he is gone with it
		if (!ow.inWindow( p[0], p[1] ) && !seen){
			spend( TICK );
			vanish();
			return true;
		}
		int mark = ow.trafficCell( p[0], p[1] );
		if (mark == -1){
			spend( TICK );   //nowhere to stand there just now: he waits
			return true;
		}
		float len = RoadTraffic.length( route );
		float lead = dir < 0 ? s - mine : mine - s;
		//ahead of the clock: he lets it catch up. standing at the end of the walk or the beat, on
		//his mark: he stays there
		boolean end = dir == 0 || (dir > 0 && s >= len - 0.5f) || (dir < 0 && s <= 0.5f);
		if (lead >= 0.5f || (end && ow.distance( pos, mark ) <= 1)){
			spend( TICK );
			return true;
		}
		float aim = dir > 0 ? Math.min( len, s + LEG ) : dir < 0 ? Math.max( 0f, s - LEG ) : s;
		return toward( ow, mark, aim, -lead, pace, dir );
	}

	/** The walk is over: in at the door s cells along the route (reached heading `dir`) - at
	 *  once where nobody sees him, else through it (and, kept out too long, gone all the same).
	 *  Spends. */
	protected boolean goIn( OverworldLevel ow, float s, int dir ){
		float[] p = RoadTraffic.point( route, s );
		int door = ow.inWindow( p[0], p[1] ) ? ow.localCell( Math.round( p[0] ), Math.round( p[1] ) ) : -1;
		if (door != -1 && ow.map[door] != Terrain.DOOR && ow.map[door] != Terrain.OPEN_DOOR) door = -1;
		if (door == -1 || pos == door || !NetManager.anyHeroSees( pos ) || ++overdue > LINGER){
			spend( TICK );
			vanish();
			return true;
		}
		if (ow.adjacent( pos, door )){
			//through the doorway (it opens to him), and in next turn
			if (Actor.findChar( door ) != null){
				spend( TICK );
				return true;
			}
			int old = pos;
			move( door );
			spend( TICK );
			return moveSprite( old, pos );
		}
		return toward( ow, door, s, Math.abs( s - here( ow ) ), 1f, dir );
	}

	//on toward his mark: set straight down on it when he has fallen far behind and nobody sees
	//either end, else a step along this leg's path (the leg reaches aimS cells along the route).
	//`behind` is how far behind his mark he is: a step at his own pace, quicker behind, at a
	//run once far behind
	private boolean toward( OverworldLevel ow, int mark, float aimS, float behind, float pace, int dir ){
		if (ow.distance( pos, mark ) >= CATCH_UP && !NetManager.anyHeroSees( pos ) && !NetManager.anyHeroSees( mark )
				&& Actor.findChar( mark ) == null){
			move( mark, false );
			sprite.place( mark );
			way = null;
			goal = -1;
			spend( TICK );
			return true;
		}
		if (goal == -1 || goalVersion != ow.windowVersion() || goalDir != dir || ow.distance( pos, goal ) <= 2){
			float[] g = RoadTraffic.point( route, aimS );
			goal = clear( ow, ow.trafficCell( g[0], g[1] ) );
			goalDir = dir;
			goalVersion = ow.windowVersion();
		}
		if (goal == -1 || goal == pos){
			spend( TICK );
			return true;
		}
		if (way == null || wayGoal != goal || wayVersion != ow.windowVersion()){
			if (retryIn > 0){
				retryIn--;
				spend( TICK );
				return true;
			}
			way = path( ow, goal );
			if (way == null && !offRoad){
				//no way along the road from here (it is blocked, or broken by rock or ice): across
				//country for a while
				offRoad = true;
				offRoadLeft = OFF_ROAD_TURNS;
				way = path( ow, goal );
			}
			wayGoal = goal;
			wayVersion = ow.windowVersion();
			avoidCell = -1;
			if (way == null){
				retryIn = RETRY;
				goal = -1;
				spend( TICK );
				return true;
			}
		}
		int next = way.getFirst();
		if (!ow.adjacent( pos, next )){
			way = null;
			spend( TICK );
			return true;
		}
		if (Actor.findChar( next ) != null || !ow.passable[next] || ow.avoid[next]){
			//somebody in the way: a little patience, then a way round them
			if (++blocked >= 3){
				blocked = 0;
				way = null;
				avoidCell = next;
				if (next == goal) goal = -1;
			}
			spend( TICK );
			return true;
		}
		blocked = 0;
		way.removeFirst();
		if (way.isEmpty()) way = null;
		int old = pos;
		move( next );
		float rate = behind >= 5 ? 1f : behind >= 1 ? pace * 1.25f : pace;
		spend( 1f / Math.min( 1f, rate ) );
		return moveSprite( old, pos );
	}

	//the cell, or a free road cell beside it when someone else stands there (a leg is never
	//aimed at another's feet)
	private int clear( OverworldLevel ow, int cell ){
		if (cell == -1) return -1;
		Char o = Actor.findChar( cell );
		if (o == null || o == this) return cell;
		boolean[] road = ow.roadMask();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c >= 0 && c < ow.length() && road[c] && ow.passable[c] && !ow.avoid[c] && Actor.findChar( c ) == null) return c;
		}
		return cell;
	}

	//a way to the cell over the road (or, across country, over the ground the settlers walk:
	//no wading, no trampling), round the cell given up on
	private PathFinder.Path path( OverworldLevel ow, int to ){
		boolean[] mask;
		if (offRoad){
			mask = VillageRoutine.walkMask( ow );
		} else {
			boolean[] road = ow.roadMask();
			if (scratch == null || scratch.length != road.length) scratch = new boolean[road.length];
			for (int i = 0; i < road.length; i++) scratch[i] = road[i] && ow.passable[i] && !ow.avoid[i];
			mask = scratch;
		}
		boolean held = avoidCell >= 0 && avoidCell < mask.length && mask[avoidCell];
		if (held) mask[avoidCell] = false;
		PathFinder.Path p = PathFinder.find( pos, to, mask );
		if (held) mask[avoidCell] = true;
		return p;
	}

	//in through the door and gone: the door shuts behind him (one left open would be kept as a
	//player's edit), and the road's next walker is somebody else
	protected void vanish(){
		if (Dungeon.level.map[pos] == Terrain.OPEN_DOOR) Door.leave( pos );
		OverworldLevel.vanish( Dungeon.level, this );
	}

	private static final String ROUTE = "route";
	private static final String DAY   = "day";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ROUTE, route );
		bundle.put( DAY, day );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		route = bundle.contains( ROUTE ) ? bundle.getFloatArray( ROUTE ) : new float[0];
		day = bundle.contains( DAY ) ? bundle.getInt( DAY ) : -1;
	}
}
