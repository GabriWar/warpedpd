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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Visual;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The skills' crisp motes (docs/fx/skills.md, MoteFX): the small pixel pictures a meaning is told
 * in, made straight into the kit's holders (light added in the light layer, matter among the
 * effects), so a skill file adds them without a literal burst and without drawing the game's Random.
 *
 * - crosses: the heal's red cross (Speck.HEALING's, the kit's PLUS_5 lit FF2020 warming to FFE8A0
 *   over its last 30%) rising off a body, for a real HP gain with no Speck.HEALING burst of its own.
 * - greens: the heal's green light, motes rising beside the crosses.
 * - gather: motes drawn in from an ellipse round a point (MagicParticle.ATTRACTING's motion), deep
 *   to pale, growing as they arrive: mana gathering, a charge.
 * - peel: motes leaving a body toward a point.
 * - flick: the one crisp thrower (sparks, shards, drops): n bits of a frame thrown at vMin..vMax with
 *   a drag and a gravity, each dying where it reaches its floor line (never off the stage).
 * - glint: one snapped light quad, still or running (a blade's edge catching the light).
 *
 * Caps a call: crosses CROSSES 6, greens GREENS 8, gather GATHER 12, peel PEEL 8, flick FLICK 12;
 * priorities crosses and glints P1, the rest P2 unless said. Every call is any-thread (posted to the
 * render thread, a sprite's place read there) and draws only FxRandom.
 */
public final class MoteFX {

	private MoteFX(){}

	/** The most a call makes. */
	public static final int CROSSES = 6, GREENS = 8, GATHER = 12, PEEL = 8, FLICK = 12;
	/** A cross: its rise (px/s), drag, sway (px either way, Hz), life (s) and where it warms. */
	public static final float CROSS_RISE = 20f, CROSS_DRAG = 1.2f, CROSS_SWAY = 1.5f, CROSS_SWAY_HZ = 1.2f,
			CROSS_LIFE = 0.9f, CROSS_WARM = 0.7f;
	/** A green: its rise (px/s), sway (px either way), life (s). */
	public static final float GREEN_RISE_MIN = 9f, GREEN_RISE_MAX = 14f, GREEN_SWAY = 2f, GREEN_LIFE = 0.7f;

	//the cross's colour: the heal's red, warming over its last 30%
	static final Ramp CROSS = new Ramp( SkillPalette.HEAL_CROSS, SkillPalette.HEAL_CROSS, SkillPalette.HEAL_CROSS_WARM )
			.keyed( 0f, CROSS_WARM, 1f );
	//a gather grows as it arrives
	static final int[][] GROW = { FxFrames.MOTE_1, FxFrames.MOTE_2, FxFrames.SQ_3 };
	static final int[][] GROW_2 = { FxFrames.MOTE_1, FxFrames.MOTE_2 };

	// ------------------------------------------------------------------ the heal's crosses and greens

	/** n (at most CROSSES) red crosses rising from about (x, y), spread +-4 px. Any thread. */
	public static void crosses( final float x, final float y, final int n ){
		Fx.post( () -> crossesNow( x, y, n ) );
	}

	/** The same off a body's middle. */
	public static void crosses( final CharSprite s, final int n ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			crossesNow( SkillVfx.midX( s ), SkillVfx.midY( s ), n );
		} );
	}

	private static void crossesNow( float x, float y, int n ){
		n = Math.min( CROSSES, n );
		float rate = (float) (2 * Math.PI * CROSS_SWAY_HZ);
		for (int i = 0; i < n; i++){
			float px = x + FxRandom.Float( -4f, 4f ), py = y + FxRandom.Float( -4f, 4f );
			FxParticle p = SkillVfx.glow( px, py, FxBudget.P1 );
			if (p == null) continue;
			p.look( FxFrames.PLUS_5 ).ramp( CROSS, true ).vel( 0, -CROSS_RISE ).drag( CROSS_DRAG )
					.sway( CROSS_SWAY * rate, rate ).life( CROSS_LIFE * FxRandom.Float( 0.9f, 1f ) )
					.alpha( 1f, 0.05f, 0.45f ).snap().source( px, py );
		}
	}

	/** n (at most GREENS) motes of the heal's green rising from about (x, y). Any thread. */
	public static void greens( final float x, final float y, final int n ){
		Fx.post( () -> greensNow( x, y, n ) );
	}

	public static void greens( final CharSprite s, final int n ){
		if (s == null) return;
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			greensNow( SkillVfx.midX( s ), SkillVfx.midY( s ), n );
		} );
	}

	private static void greensNow( float x, float y, int n ){
		n = Math.min( GREENS, n );
		for (int i = 0; i < n; i++){
			float px = x + FxRandom.Float( -5f, 5f ), py = y + FxRandom.Float( -3f, 4f );
			FxParticle p = SkillVfx.glow( px, py, FxBudget.P2 );
			if (p == null) continue;
			float rate = FxRandom.Float( 4f, 6f );
			p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.MOTE_1 ).ramp( SkillPalette.HEAL, true )
					.vel( 0, -FxRandom.Float( GREEN_RISE_MIN, GREEN_RISE_MAX ) ).sway( GREEN_SWAY * rate, rate )
					.life( GREEN_LIFE * FxRandom.Float( 0.85f, 1f ) ).alpha( 0.95f, 0.06f, 0.3f ).source( px, py );
		}
	}

	// ------------------------------------------------------------------ drawn in, peeled off

	/** n (at most GATHER) motes born on an ellipse r px round (x, y) (its height x0.6) flying in over
	 *  `seconds`, growing MOTE_1, MOTE_2, SQ_3 and running the ramp deep to pale, fading in over
	 *  their first 60%. P2. Any thread. */
	public static void gather( final float x, final float y, final int n, final Ramp ramp, final float r, final float seconds ){
		final Ramp in = reversed( ramp );
		Fx.post( () -> gatherNow( x, y, n, in, r, seconds ) );
	}

	public static void gather( final CharSprite s, final int n, final Ramp ramp, final float r, final float seconds ){
		if (s == null) return;
		final Ramp in = reversed( ramp );
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			gatherNow( SkillVfx.midX( s ), SkillVfx.midY( s ), n, in, r, seconds );
		} );
	}

	private static void gatherNow( float x, float y, int n, Ramp in, float r, float seconds ){
		n = Math.min( GATHER, n );
		float t = Math.max( 0.05f, seconds );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			float a = a0 + (float) (2 * Math.PI * i / Math.max( 1, n )) + FxRandom.Float( -0.25f, 0.25f );
			float d = r * FxRandom.Float( 0.8f, 1.1f );
			float mx = x + (float) Math.cos( a ) * d, my = y + (float) Math.sin( a ) * d * 0.6f;
			FxParticle p = SkillVfx.glow( mx, my, FxBudget.P2 );
			if (p == null) continue;
			float life = t * FxRandom.Float( 0.85f, 1f );
			p.look( GROW, 0 ).ramp( in, true ).vel( (x - mx) / life, (y - my) / life ).life( life )
					.alpha( 1f, life * 0.6f, 0.03f ).source( x, y );
		}
	}

	/** n (at most PEEL) motes leaving a body (born on its middle half) for (tx, ty), each arriving
	 *  within `seconds` (0.8 to 1 of it), MOTE_1 to MOTE_2, deep to pale. P2. Any thread. */
	public static void peel( final CharSprite from, final float tx, final float ty, final int n, final Ramp ramp, final float seconds ){
		if (from == null) return;
		final Ramp out = reversed( ramp );
		Fx.post( () -> {
			if (from.parent == null || !from.visible) return;
			int k = Math.min( PEEL, n );
			float w = from.width() * 0.25f, h = from.height() * 0.25f;
			float t = Math.max( 0.05f, seconds );
			for (int i = 0; i < k; i++){
				float mx = SkillVfx.midX( from ) + FxRandom.Float( -w, w ), my = SkillVfx.midY( from ) + FxRandom.Float( -h, h );
				FxParticle p = SkillVfx.glow( mx, my, FxBudget.P2 );
				if (p == null) continue;
				float life = t * FxRandom.Float( 0.8f, 1f );
				p.look( GROW_2, 0 ).ramp( out, true ).vel( (tx - mx) / life, (ty - my) / life ).life( life )
						.alpha( 1f, 0.04f, 0.04f );
			}
		} );
	}

	// ------------------------------------------------------------------ thrown

	/** n (at most FLICK) bits of `frame` (light, added; drops, pebbles and ink are matter) thrown all
	 *  round from (x, y) at vMin..vMax px/s, slowed by `drag` (a share of their speed a second),
	 *  pulled by `gravity` (px/s^2, positive down), living `life` s (0.8 to 1 of it), running the
	 *  ramp; each dies where it reaches y + floorDy (never falls off the stage). Any thread. */
	public static void flick( float x, float y, int n, int[] frame, Ramp ramp, float vMin, float vMax, float drag,
	                          float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, new int[][]{ frame }, ramp, 0, (float) -Math.PI / 2, (float) (2 * Math.PI), vMin, vMax,
				drag, gravity, life, floorDy, priority );
	}

	/** The same in one colour. */
	public static void flick( float x, float y, int n, int[] frame, int color, float vMin, float vMax, float drag,
	                          float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, new int[][]{ frame }, null, color, (float) -Math.PI / 2, (float) (2 * Math.PI), vMin, vMax,
				drag, gravity, life, floorDy, priority );
	}

	/** The same thrown about `angle` (radians, 0 east, positive down the screen), `spread` wide. */
	public static void flick( float x, float y, int n, int[] frame, Ramp ramp, float angle, float spread, float vMin, float vMax,
	                          float drag, float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, new int[][]{ frame }, ramp, 0, angle, spread, vMin, vMax, drag, gravity, life, floorDy, priority );
	}

	public static void flick( float x, float y, int n, int[] frame, int color, float angle, float spread, float vMin, float vMax,
	                          float drag, float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, new int[][]{ frame }, null, color, angle, spread, vMin, vMax, drag, gravity, life, floorDy, priority );
	}

	/** Any of several frames (a shard's two), each bit one of them. */
	public static void flick( float x, float y, int n, int[][] frames, Ramp ramp, float vMin, float vMax, float drag,
	                          float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, frames, ramp, 0, (float) -Math.PI / 2, (float) (2 * Math.PI), vMin, vMax, drag, gravity, life,
				floorDy, priority );
	}

	public static void flick( float x, float y, int n, int[][] frames, int color, float vMin, float vMax, float drag,
	                          float gravity, float life, float floorDy, int priority ){
		bits( x, y, n, frames, null, color, (float) -Math.PI / 2, (float) (2 * Math.PI), vMin, vMax, drag, gravity, life,
				floorDy, priority );
	}

	private static void bits( final float x, final float y, final int n, final int[][] frames, final Ramp ramp, final int color,
	                          final float angle, final float spread, final float vMin, final float vMax,
	                          final float drag, final float gravity, final float life, final float floorDy, final int priority ){
		Fx.post( () -> {
			int k = Math.min( FLICK, n );
			float floor = y + floorDy;
			for (int i = 0; i < k; i++){
				int[] f = frames[FxRandom.Int( frames.length )];
				boolean matter = matter( f );
				FxParticle p = matter ? SkillVfx.matter( x, y, priority ) : SkillVfx.glow( x, y, priority );
				if (p == null) continue;
				float a = spread >= 2 * Math.PI - 0.01f
						? (float) (angle + 2 * Math.PI * (i + FxRandom.Float( 0.8f )) / Math.max( 1, k ))
						: angle + FxRandom.Float( -spread / 2f, spread / 2f );
				p.look( f );
				if (ramp != null) p.ramp( ramp, !matter );
				else p.paint( color );
				if (f[2] == 1 && f[3] > 1) p.orient();
				float l = life * FxRandom.Float( 0.8f, 1f );
				p.polar( a, FxRandom.Float( vMin, vMax ) ).drag( drag ).gravity( gravity ).life( l )
						.alpha( 1f, 0, l * 0.4f ).ground( floor, FxParticle.DIE, 0 ).priority( priority );
				if (f[2] > 1 || f[3] == 1) p.snap();
			}
		} );
	}

	//the frames of matter (drawn plainly among the effects); the rest are light
	static boolean matter( int[] f ){
		return f == FxFrames.DROP_2 || f == FxFrames.DROP_3 || f == FxFrames.PLINK || f == FxFrames.PEBBLE_2
				|| f == FxFrames.PEBBLE_3 || f == FxFrames.PEBBLE_3V || f == FxFrames.INK_CORE || f == FxFrames.SEED
				|| f == FxFrames.FEATHER || f == FxFrames.BUBBLE_2 || f == FxFrames.BUBBLE_3 || f == FxFrames.PETAL_B
				|| f == FxFrames.BLADE_3 || f == FxFrames.BLADE_4 || f == FxFrames.BLADE_5;
	}

	// ------------------------------------------------------------------ a glint

	/** One still light quad of `frame` at (x, y), snapped, fading over its last 40%. P1. Any thread. */
	public static void glint( float x, float y, int[] frame, int color, float life ){
		glint( x, y, frame, color, life, 0, 0 );
	}

	/** The same running at (vx, vy) px/s (along a blade). */
	public static void glint( final float x, final float y, final int[] frame, final int color, final float life,
	                          final float vx, final float vy ){
		Fx.post( () -> {
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P1 );
			if (p != null) p.look( frame ).paint( color ).vel( vx, vy ).life( life ).alpha( 1f, 0, life * 0.4f ).snap();
		} );
	}

	/** A glint on a visual's middle (followed no further: it is a moment). */
	public static void glint( final Visual v, final int[] frame, final int color, final float life ){
		if (v == null) return;
		Fx.post( () -> {
			if (v.parent == null) return;
			FxParticle p = SkillVfx.glow( v.x + v.width() / 2f, v.y + v.height() / 2f, FxBudget.P1 );
			if (p != null) p.look( frame ).paint( color ).life( life ).alpha( 1f, 0, life * 0.4f ).snap();
		} );
	}

	// ------------------------------------------------------------------ helpers

	/** A ramp's stops the other way round: deep first (things drawn in brighten as they arrive). */
	public static Ramp reversed( Ramp r ){
		int[] s = new int[r.size()];
		for (int i = 0; i < s.length; i++) s[i] = r.stop( s.length - 1 - i );
		return new Ramp( s );
	}
}
