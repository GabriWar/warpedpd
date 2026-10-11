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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Group;

/**
 * What a beam (effects/Beam) has round its core: a halo band along it shimmering (+-6% at 20 Hz),
 * a glow at its source, a glow at its end and a pool under that end, and what its kind adds - the
 * death ray's ash and its scorched, flaking end, the light ray's warm motes, the sun ray's dust
 * and the pools it lays on the floor, the health ray's bead, its pulses and the motes its
 * receiver is given, the supernova's shock rings at both ends.
 *
 * Built on the beam's first frame (the render thread; a beam is often made on the actor thread)
 * and let go of with it. At most FULL beams at once get all of it; later ones, and every lite()
 * beam (a sentry's, a ward's, an eye's), only their core and halo.
 */
public final class RayFx {

	public static final int DEATH = 0, LIGHT = 1, SUN = 2, HEALTH = 3, SUPERNOVA = 4;

	/** Full beams at once. */
	public static final int FULL = 3;
	/** The halo's alpha (full, lite) and its shimmer; the edge jitter's period in frames. */
	public static final float HALO = 0.3f, HALO_LITE = 0.2f, SHIMMER = 0.06f, SHIMMER_HZ = 20f;
	public static final int JITTER_FRAMES = 2;
	/** Bits along a beam: one per this many px, at most this many. */
	public static final int ASH_EVERY = 12, ASH_MOST = 16, MOTE_EVERY = 16, MOTE_MOST = 12;
	/** The health ray's bead (its flight, s), its pulses (px/s, how many). */
	public static final float BEAD_FOR = 0.12f, PULSE_SPEED = 80f;
	public static final int PULSES = 4;
	/** The dying halo after the core is gone, s. */
	public static final float AFTERGLOW = 0.2f;

	private static int fullLive;
	//the scene the count is of (its light layer): a beam left on the last scene when the level
	//changed is never let go of, so a new scene starts the count again
	private static Group countedIn;
	private Group counted;

	private int kind;
	private boolean full, built;
	private float sx, sy, ex, ey, len, dx, dy, nx, ny;
	private int color;
	private float duration;
	private ArcImage halo, bead;
	private final ArcImage[] pulses = new ArcImage[PULSES];
	private FxLight src, end, endPool;
	private final FxLight[] floor = new FxLight[3];
	private int frames, jitter;
	private float bitIn, bitEvery;
	private int bitsLeft;
	private boolean landed;

	/** Full beams alive now. */
	public static int fullLive(){
		return countedIn == GameScene.layer( GameScene.Layer.LIGHTS ) ? fullLive : 0;
	}

	/** The colour a kind's halo and light take unless its beam was tinted. */
	public static int colorOf( int kind ){
		switch (kind){
			case DEATH:     return 0xC060FF;
			case LIGHT:     return 0xFFF6CC;
			case SUN:       return 0xFFF0A0;
			case HEALTH:    return Element.LIFE.light;
			case SUPERNOVA: return 0x5CDAFF;
			default:        return 0xFFFFFF;
		}
	}

	/**
	 * Builds it round a beam from (sx, sy) to (ex, ey) of a kind lasting `duration` s, in `color`;
	 * lite: core and halo only. Render thread.
	 */
	public void build( int kind, float sx, float sy, float ex, float ey, float duration, int color, boolean lite ){
		this.kind = kind;
		this.sx = sx;
		this.sy = sy;
		this.ex = ex;
		this.ey = ey;
		this.duration = duration;
		this.color = color;
		float vx = ex - sx, vy = ey - sy;
		len = (float)Math.sqrt( vx * vx + vy * vy );
		dx = len > 0 ? vx / len : 1;
		dy = len > 0 ? vy / len : 0;
		nx = -dy;
		ny = dx;
		built = true;
		frames = 0;
		landed = false;
		if (!Arc.onScene()) return;

		Group scene = GameScene.layer( GameScene.Layer.LIGHTS );
		if (scene != countedIn){
			countedIn = scene;
			fullLive = 0;
		}
		full = !lite && fullLive < FULL;
		if (full){
			fullLive++;
			counted = scene;
		}

		halo = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
		halo.show( FxFrames.Light.BAND_21 ).rgb( color );
		halo.am = 0;
		halo.line( sx, sy, ex, ey );
		if (!full) return;

		float hold = duration * 0.5f, release = duration * 0.5f + AFTERGLOW;
		float at = kind == HEALTH ? BEAD_FOR : 0f;
		src = Arc.glow( sx, sy, 11, color, 0.5f );
		if (src != null) src.life( 0.02f, hold, release );
		end = Arc.glow( ex, ey, 15, color, kind == LIGHT ? 0.55f : 0.45f );
		if (end != null) end.life( at + 0.02f, hold - at, release );
		endPool = Arc.pool( ex, ey + Arc.GROUND, 23, color, 0.3f );
		if (endPool != null) endPool.life( at + 0.02f, hold - at, release );

		switch (kind){
			case DEATH:
				bitsLeft = Math.min( ASH_MOST, Math.round( len / ASH_EVERY ) );
				land();
				break;
			case LIGHT:
				bitsLeft = Math.min( MOTE_MOST, Math.round( len / MOTE_EVERY ) );
				land();
				break;
			case SUN:
				bitsLeft = Math.min( MOTE_MOST, Math.round( len / MOTE_EVERY ) );
				for (int i = 0; i < floor.length; i++){
					float t = (i + 1) / (float)(floor.length + 1);
					floor[i] = Arc.pool( sx + (ex - sx) * t, sy + (ey - sy) * t + Arc.GROUND, 23, color, 0.2f );
					if (floor[i] != null) floor[i].life( 0.05f, hold, release );
				}
				land();
				break;
			case HEALTH:
				bead = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
				bead.show( FxFrames.Light.GLOW_7 ).rgb( 0xFFFFFF );
				bead.centre( sx, sy );
				for (int i = 0; i < PULSES; i++){
					pulses[i] = ArcImage.get( GameScene.Layer.LIGHTS, true, true );
					pulses[i].show( FxFrames.Light.PULSE_DOT ).rgb( Element.LIFE.ramp.stop( 2 ) );
					pulses[i].visible = false;
				}
				break;
			case SUPERNOVA:
				Arc.groundRing( sx, sy + Arc.GROUND, 13, 41, 0.4f, color, 0.6f, true );
				Arc.groundRing( ex, ey + Arc.GROUND, 13, 41, 0.4f, color, 0.6f, true );
				land();
				break;
		}
		//its bits spread over the first 60% of its life
		bitEvery = bitsLeft > 0 ? duration * 0.6f / bitsLeft : 1f;
		bitIn = 0;
	}

	public boolean built(){
		return built;
	}

	/**
	 * A frame: `age` s since it began, `core` its core's alpha now (0 once the core is gone; the
	 * halo then dies over AFTERGLOW s, `after` 0..1 through it). Returns the share of the beam's
	 * length its core should reach (the health ray's grows behind its bead).
	 */
	public float update( float age, float dt, float core, float after ){
		if (!built) return 1f;
		frames++;
		float reach = 1f;
		if (halo != null){
			float shimmer = 1f + SHIMMER * (float)Math.sin( 2 * Math.PI * SHIMMER_HZ * age );
			float a = (full ? HALO : HALO_LITE) * shimmer * (core > 0 ? Math.max( 0.35f, core ) : 0.35f * (1f - after));
			if (kind == DEATH && frames % JITTER_FRAMES == 0) jitter = FxRandom.IntRange( -1, 1 );
			if (kind == HEALTH && full) reach = Math.min( 1f, age / BEAD_FOR );
			halo.line( sx + nx * jitter, sy + ny * jitter, sx + (ex - sx) * reach + nx * jitter, sy + (ey - sy) * reach + ny * jitter );
			halo.am = a;
			halo.visible = a > 0.004f;
		}
		if (!full) return reach;

		if (kind == HEALTH) health( age, core );

		if (bitsLeft > 0 && (bitIn -= dt) <= 0){
			bitIn += bitEvery;
			bitsLeft--;
			bit();
		}
		return reach;
	}

	//the health ray: the bead's flight, then pulses flowing to the receiver, and its motes
	private void health( float age, float core ){
		if (bead != null){
			float t = age / BEAD_FOR;
			if (t >= 1f){
				bead.kill();
				bead = null;
				land();
			} else {
				bead.centre( sx + (ex - sx) * t, sy + (ey - sy) * t );
			}
		}
		float spacing = len / PULSES;
		float angle = (float)Math.toDegrees( Math.atan2( dy, dx ) );
		for (int i = 0; i < PULSES; i++){
			ArcImage p = pulses[i];
			if (p == null) continue;
			if (age < BEAD_FOR || core <= 0){
				p.visible = false;
				continue;
			}
			float d = ((age - BEAD_FOR) * PULSE_SPEED + i * spacing) % Math.max( 1f, len );
			p.origin.set( p.width / 2f, p.height / 2f );
			p.x = sx + dx * d - p.width / 2f;
			p.y = sy + dy * d - p.height / 2f;
			p.angle = angle;
			p.am = 0.8f * core;
			p.visible = true;
		}
	}

	//one bit along it: ash, a warm mote, a mote of dust
	private void bit(){
		float t = FxRandom.Float( 0.05f, 0.95f ), side = FxRandom.Float( -2f, 2f );
		float x = sx + (ex - sx) * t + nx * side, y = sy + (ey - sy) * t + ny * side;
		FxParticle p;
		switch (kind){
			case DEATH:
				if ((p = Arc.matter( x, y, FxBudget.P2 )) != null){
					p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( FxRandom.chance( 0.5f ) ? 0x3A3040 : 0x22182A )
							.vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 4f, 8f ) ).drag( 1f ).life( FxRandom.Float( 0.8f, 1.2f ) )
							.alpha( 0.85f, 0.05f, 0.4f );
				}
				break;
			case LIGHT:
				if ((p = Arc.light( x, y, FxBudget.P2 )) != null){
					float s = FxRandom.sign() * FxRandom.Float( 4f, 8f );
					p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( 0xFFE9A8 )
							.vel( nx * s, ny * s - 2f ).drag( 2f ).life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0.05f, 0.3f ).snap();
				}
				break;
			case SUN:
				if ((p = Arc.light( x, y, FxBudget.P3 )) != null){
					p.look( FxFrames.MOTE_1 ).paint( 0xF0E0A0 ).vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( 2f, 5f ) )
							.life( FxRandom.Float( 0.8f, 1.2f ) ).alpha( 0.8f, 0.2f, 0.4f ).snap();
				}
				break;
		}
	}

	//what the beam does where it ends, once
	private void land(){
		if (landed) return;
		landed = true;
		float gy = ey + Arc.GROUND;
		boolean water = WaterFX.onWater( ex, gy );
		switch (kind){
			case DEATH: {
				for (int i = 0; i < 8; i++){
					FxParticle p = Arc.matter( ex + FxRandom.Float( -3, 3 ), ey + FxRandom.Float( -3, 3 ), FxBudget.P2 );
					if (p == null) break;
					p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.PEBBLE_2 ).paint( 0x22182A )
							.polar( FxRandom.Float( (float)-Math.PI, 0 ), FxRandom.Float( 15f, 35f ) ).gravity( 60f ).drag( 1.5f )
							.ground( gy + FxRandom.Float( -2, 2 ), water ? FxParticle.RING : FxParticle.DIE, 0 ).strength( 0.1f )
							.life( FxRandom.Float( 0.6f, 1f ) ).alpha( 1f, 0, 0.3f );
				}
				FxEmitter e = GameScene.lightEmitter();
				if (e != null){
					e.pos( ex, ey );
					e.burst( PurpleParticle.BURST, 4 );
				}
				if (!water) FxDecal.at( ex, gy, FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, 0.6f, 0.2f, 0.3f, FxRandom.chance( 0.5f ) );
				else WaterFX.ring( ex, gy, WaterFX.M, color, 0.6f );
				break;
			}
			case LIGHT:
			case SUN: {
				//a bloom at both ends
				FxLight b = Arc.glow( ex, ey, 23, color, 0.45f );
				if (b != null) b.life( 0.02f, 0.05f, 0.35f );
				b = Arc.glow( sx, sy, 15, color, 0.4f );
				if (b != null) b.life( 0.02f, 0.05f, 0.3f );
				if (water) WaterFX.ring( ex, gy, WaterFX.S, color, 0.6f );
				break;
			}
			case HEALTH: {
				for (int i = 0; i < 3; i++){
					FxParticle p = Arc.light( ex + FxRandom.Float( -5, 5 ), ey + FxRandom.Float( -3, 3 ), FxBudget.P2 );
					if (p == null) break;
					p.look( FxFrames.PLUS_3 ).paint( Element.LIFE.ramp.stop( 2 ) ).vel( 0, -FxRandom.Float( 10f, 16f ) ).drag( 1f )
							.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0.05f, 0.3f ).snap();
				}
				break;
			}
			case SUPERNOVA:
				if (water) WaterFX.ring( ex, gy, WaterFX.L, color, 0.6f );
				break;
		}
	}

	/** Lets go of everything it built (the beam is done). */
	public void dispose(){
		if (halo != null) halo.kill();
		if (bead != null) bead.kill();
		for (int i = 0; i < PULSES; i++){
			if (pulses[i] != null) pulses[i].kill();
			pulses[i] = null;
		}
		halo = bead = null;
		if (src != null) src.putOut( 0.1f );
		if (end != null) end.putOut( 0.1f );
		if (endPool != null) endPool.putOut( 0.1f );
		for (int i = 0; i < floor.length; i++){
			if (floor[i] != null) floor[i].putOut( 0.1f );
			floor[i] = null;
		}
		src = end = endPool = null;
		if (full && counted == countedIn) fullLive = Math.max( 0, fullLive - 1 );
		full = false;
		counted = null;
	}

	/** Test only: no full beams counted. */
	static void resetCount(){
		fullLive = 0;
	}
}
