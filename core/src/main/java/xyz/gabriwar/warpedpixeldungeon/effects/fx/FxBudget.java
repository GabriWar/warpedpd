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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.utils.DeviceCompat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * What the effects may draw, so a busy fight on a phone keeps its frame: every Image and particle
 * is its own draw call, so live quads are draw calls, and the kit's are counted here.
 *
 * Each particle has a priority: P0 is what the game reads (a telegraph, a bolt's head, an impact's
 * core, a hit), never thinned; P1 the body of an effect (a flame's tongues, a gas's puffs); P2 its
 * secondary life (embers, smoke, motes, droplets); P3 dressing (shimmer, glints, dust). As the
 * live count passes 80% of SOFT, everything but P0 thins (factor), dressing first; past HARD only
 * P0 is drawn. P2 and P3 also thin with distance from the hero. At most SPAWNS new ones a frame.
 *
 * A governor watches the real frame time: slower than 1/45 s for a second steps the tier down
 * (FULL, REDUCED, MINIMAL), faster than 1/55 s for three seconds steps it back up; a lower tier
 * thins further and takes away lights, decals, sheens, glints and reflections.
 *
 * The counts are a phone's (a mid-range GLES2 phone carries 1-2k small draws at 60 fps next to the
 * scene's own 200 or so); desktop gets half as many again of every one.
 */
public final class FxBudget {

	private FxBudget(){}

	public static final int P0 = 0, P1 = 1, P2 = 2, P3 = 3;
	public static final int FULL = 0, REDUCED = 1, MINIMAL = 2;

	/** What is counted. */
	public static final int PARTICLES = 0, LIGHTS = 1, DECALS = 2, RINGS = 3, DROPLETS = 4, SURFACE = 5;
	static final int KINDS = 6;

	//a phone's numbers (desktop: x1.5)
	public static final int SOFT = 360, HARD = 520, SPAWNS = 48;
	public static final int LIGHTS_CAP = 24, LIGHTS_PERSISTENT = 12, DECALS_CAP = 32, RINGS_CAP = 40,
			DROPLETS_CAP = 24, AFTERIMAGES_CAP = 12;
	static final int LIGHTS_REDUCED = 16, LIGHTS_MINIMAL = 8, DECALS_REDUCED = 20, DECALS_MINIMAL = 12;
	//the governor: an EMA of the real frame time; too slow for DOWN_AFTER s steps down a tier, fast
	//enough for UP_AFTER s steps back up
	static final float EMA = 0.1f, SLOW = 1 / 45f, FAST = 1 / 55f, DOWN_AFTER = 1f, UP_AFTER = 3f;
	//thinning by load: full up to LOAD_FULL of SOFT, down to 0.4 at LOAD_LOW, never under FLOOR
	static final float LOAD_FULL = 0.8f, LOAD_LOW = 1.3f, AT_LOW = 0.4f, FLOOR = 0.25f;
	static final float[] TIER_FACTOR = { 1f, 0.6f, 0.35f };
	//distance from the hero, in cells: full within NEAR, MID_FACTOR within MID, FAR_FACTOR beyond
	static final int NEAR = 4, MID = 7;
	static final float MID_FACTOR = 0.7f, FAR_FACTOR = 0.45f;

	//every count scaled by this: 1.5 on desktop. A test sets 1
	static float scale = DeviceCompat.isDesktop() ? 1.5f : 1f;

	private static final AtomicIntegerArray live = new AtomicIntegerArray( KINDS );
	private static final AtomicInteger spawnsLeft = new AtomicInteger( SPAWNS );
	private static volatile int tier = FULL;
	private static float ema = 1 / 60f, slowFor, fastFor;

	/** The emitters and factories that say how much they matter; BlobEmitter's default is P1. */
	public interface Prioritized {
		int priority();

		/** The share of a blob's cells holding `cur` that emit each tick: thinner where it is thin. */
		default float density( int cur ){
			return blobDensity( cur );
		}
	}

	/** clamp(0.3 + 0.25 log10(cur), 0.3, 1): a blob cell's share of emits by its volume. */
	public static float blobDensity( int cur ){
		if (cur <= 1) return 0.3f;
		return Math.max( 0.3f, Math.min( 1f, 0.3f + 0.25f * (float)Math.log10( cur ) ) );
	}

	// ------------------------------------------------------------------ the frame

	/** Once a frame, from the game's scene, with the real (unscaled) time since the last one. */
	public static void frame( float realDelta ){
		spawnsLeft.set( spawns() );
		if (realDelta <= 0) return;
		//a hitch (a level load, a collection) counts as no worse than a quarter second
		float dt = Math.min( realDelta, 0.25f );
		ema += (dt - ema) * EMA;
		if (ema > SLOW){
			fastFor = 0;
			slowFor += dt;
			if (slowFor >= DOWN_AFTER){
				slowFor = 0;
				if (tier < MINIMAL) tier++;
			}
		} else if (ema < FAST){
			slowFor = 0;
			fastFor += dt;
			if (fastFor >= UP_AFTER){
				fastFor = 0;
				if (tier > FULL) tier--;
			}
		} else {
			slowFor = fastFor = 0;
		}
	}

	/** A new scene: nothing of the kit's is alive. The tier is the device's and stays. */
	public static void reset(){
		for (int i = 0; i < KINDS; i++) live.set( i, 0 );
		spawnsLeft.set( spawns() );
	}

	/** Test only: back to FULL with a fresh frame-time average. */
	static void resetGovernor(){
		tier = FULL;
		ema = 1 / 60f;
		slowFor = fastFor = 0;
	}

	public static int tier(){
		return tier;
	}

	/** The frame-time average the governor goes by, seconds. */
	public static float frameTime(){
		return ema;
	}

	// ------------------------------------------------------------------ thinning

	/** The share of new particles of priority p kept now. */
	public static float factor( int p ){
		if (p <= P0) return 1f;
		int l = live.get( PARTICLES );
		if (l >= hard()) return 0f;
		float load = l / (float)soft();
		float base = load <= LOAD_FULL ? 1f
				: Math.max( FLOOR, 1f - (1f - AT_LOW) * (load - LOAD_FULL) / (LOAD_LOW - LOAD_FULL) );
		float bt = base * TIER_FACTOR[tier];
		switch (p){
			case P1: return Math.max( 0.6f, bt );
			case P2: return bt;
			default: return tier == MINIMAL ? 0f : bt * bt;
		}
	}

	/** Whether to make one new particle of priority p: P0 always; the rest by factor, while the
	 *  frame has spawns left (one is taken). */
	public static boolean keep( int p ){
		if (p <= P0){
			spawnsLeft.decrementAndGet();
			return true;
		}
		float f = factor( p );
		if (f <= 0 || FxRandom.Float() >= f) return false;
		return spawnsLeft.getAndDecrement() > 0;
	}

	/** keep(p), P2 and P3 thinned further by their distance from the hero. */
	public static boolean keep( int p, float x, float y ){
		if (p >= P2 && FxRandom.Float() >= distanceFactor( x, y )) return false;
		return keep( p );
	}

	/** How many of n new particles of priority p to make (rounded at random, at least one of a
	 *  P0 or P1 burst of one or more), taken from the frame's spawns. */
	public static int allow( int n, int p ){
		if (n <= 0) return 0;
		if (p <= P0){
			spawnsLeft.addAndGet( -n );
			return n;
		}
		float f = factor( p );
		int room = hard() - live.get( PARTICLES );
		if (f <= 0 || room <= 0) return 0;
		float want = n * f;
		int k = (int)want;
		if (FxRandom.Float() < want - k) k++;
		int left = Math.max( 0, spawnsLeft.get() );
		//a burst's body keeps one at least, past the frame's spawns too, but never past HARD
		k = p <= P1 ? Math.max( 1, Math.min( k, left ) ) : Math.min( k, left );
		k = Math.min( k, room );
		spawnsLeft.addAndGet( -k );
		return k;
	}

	/** 1 within NEAR cells of the hero, MID_FACTOR within MID, FAR_FACTOR beyond; 1 off a level. */
	public static float distanceFactor( float x, float y ){
		if (Dungeon.level == null || Dungeon.hero == null || Dungeon.hero.pos < 0) return 1f;
		int w = Dungeon.level.width();
		if (w <= 0) return 1f;
		int hx = Dungeon.hero.pos % w, hy = Dungeon.hero.pos / w;
		int cx = (int)Math.floor( x / DungeonTilemap.SIZE ), cy = (int)Math.floor( y / DungeonTilemap.SIZE );
		int d = Math.max( Math.abs( cx - hx ), Math.abs( cy - hy ) );
		return d <= NEAR ? 1f : (d <= MID ? MID_FACTOR : FAR_FACTOR);
	}

	// ------------------------------------------------------------------ counts and caps

	/** How many of a kind are alive. */
	public static int live( int kind ){
		return live.get( kind );
	}

	/** One more (or less) of a kind alive. */
	public static void add( int kind, int delta ){
		live.addAndGet( kind, delta );
	}

	/** The new spawns this frame still has. */
	public static int spawnsLeft(){
		return spawnsLeft.get();
	}

	static int scaled( int n ){
		return Math.round( n * scale );
	}

	public static int soft(){
		return scaled( SOFT );
	}

	public static int hard(){
		return scaled( HARD );
	}

	public static int spawns(){
		return scaled( SPAWNS );
	}

	/** Lights at once, all told, by tier: 24, 16, 8. */
	public static int lightCap(){
		return scaled( tier == FULL ? LIGHTS_CAP : tier == REDUCED ? LIGHTS_REDUCED : LIGHTS_MINIMAL );
	}

	/** Persistent lights at once (fires, torches, auras): half the lights, 12 at most. */
	public static int persistentLightCap(){
		return Math.min( scaled( LIGHTS_PERSISTENT ), lightCap() / 2 );
	}

	/** Decals at once, by tier: 32, 20, 12. */
	public static int decalCap(){
		return scaled( tier == FULL ? DECALS_CAP : tier == REDUCED ? DECALS_REDUCED : DECALS_MINIMAL );
	}

	/** Rings at once, water and ground together. */
	public static int ringCap(){
		return scaled( RINGS_CAP );
	}

	public static int dropletCap(){
		return scaled( DROPLETS_CAP );
	}

	public static int afterimageCap(){
		return scaled( AFTERIMAGES_CAP );
	}

	/** The water's first sheen: off at MINIMAL. */
	public static boolean sheenA(){
		return tier < MINIMAL;
	}

	/** The water's second sheen: off from REDUCED. */
	public static boolean sheenB(){
		return tier == FULL;
	}

	/** The share of glints kept: all, half, none. */
	public static float glints(){
		return tier == FULL ? 1f : tier == REDUCED ? 0.5f : 0f;
	}

	/** Lights' reflections on the water: off at MINIMAL. */
	public static boolean reflections(){
		return tier < MINIMAL;
	}
}
