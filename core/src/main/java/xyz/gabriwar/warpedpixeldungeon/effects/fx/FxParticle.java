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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.RectF;

/**
 * The kit's particle: a painted frame of the sprite sheet (FxFrames), never a scaled square. Set
 * up in one fluent run after reset - its look (a flipbook, or frames stepped through over its
 * life), its colour (a ramp it flows along as light or steps along as matter), its life and
 * alpha, its motion (velocity, gravity, drag, sway, turbulence, the wind's share) and what it does
 * when it reaches the ground - and nothing allocates once it runs.
 *
 *     ((FxParticle) emitter.recycle( FxParticle.class )).reset( x, y )
 *         .look( FxFrames.TONGUE_M, 12 ).ramp( fire, true ).life( 0.58f )
 *         .vel( vx, vy ).gravity( -28 ).sway( 6, 7 ).priority( FxBudget.P1 );
 *
 * The texture is fetched through the texture cache (no GPU work: safe on any thread); it counts
 * itself in FxBudget while it lives; slower than 12 px/s (or snap()) it is drawn on whole pixels;
 * with a source it shows only where the hero sees that point (hidden is visible = false, so a
 * hidden one costs no draw).
 */
public class FxParticle extends PixelParticle {

	/** What it does as it reaches its ground. RING: a ring on water (WaterFX.drop), else its dry
	 *  landing (landDry); SPLAT: a splat decal; WET: a wet mark. */
	public static final int DIE = 0, BOUNCE = 1, SETTLE = 2, RING = 3, SPLAT = 4, WET = 5;

	/** What becomes of a particle when it lands or its life runs out: a shared, stateless one
	 *  (an ember popping into sparks, a spark's last twinkle). It may spawn others into the
	 *  particle's own emitter. */
	public interface Fate {
		void end( FxParticle p, boolean landed );
	}

	//the frames: a flipbook at fps, or stepped through over its life (fps 0); up to two later
	//sets taking over at shares of its life (a flame's tongue shrinking)
	protected int[][] frames;
	protected int[] single;
	protected float fps;
	protected int start;
	private int[][] then1, then2;
	private float at1 = 2, at2 = 2;
	private int[] slowFrame;
	private float slowBelow;
	private int[] shown;

	//its colour
	protected Ramp ramp;
	protected boolean light, smooth;
	protected int tint;

	//its life, its alpha
	protected float life, age;
	protected float peak, fadeIn, fadeOut;

	/** Its true position (its frame's middle) and velocity, px and px/s. */
	public float px, py, vx, vy;
	protected float gravity, drag, swayAmp, swayRate, swayPhase, turbAmp, turbRate, turbPhase, windShare;

	//its ground
	protected float groundY;
	protected int landing, dryLanding;
	protected float bounce, strength;
	protected int bounces;
	//the colour its marks take (a splat's, a wet mark's, a ring's): its own unless set
	protected int markColor;

	protected boolean orient, snap;
	protected boolean sourced;
	protected float srcX, srcY;
	protected int priority;
	protected Fate fate;
	//a quarter turn every this many seconds (debris, shards), 0 for none
	protected float spinEvery, spinLeft;

	private final RectF uv = new RectF();
	private boolean counted;
	//where its frame was last put: a move from outside since (a window's slide) moves it all
	private float placedX, placedY;

	public FxParticle(){
		super();
		texture( Assets.Effects.FX_SHEET );
	}

	/** Back to life at (x, y), every setting at its default: one frame of a mote, white, a
	 *  second's life, no motion, P2. */
	public FxParticle reset( float x, float y ){
		frames = null;
		single = null;
		fps = 0;
		start = 0;
		then1 = then2 = null;
		at1 = at2 = 2;
		slowFrame = null;
		shown = null;
		ramp = null;
		light = smooth = false;
		tint = 0xFFFFFF;
		life = 1;
		age = 0;
		peak = 1;
		fadeIn = 0;
		fadeOut = 0.25f;
		px = x;
		py = y;
		vx = vy = 0;
		gravity = drag = swayAmp = swayRate = swayPhase = turbAmp = turbRate = turbPhase = windShare = 0;
		groundY = Float.NaN;
		landing = DIE;
		dryLanding = DIE;
		bounce = 0;
		strength = 0.2f;
		bounces = 0;
		markColor = -1;
		orient = snap = false;
		sourced = false;
		priority = FxBudget.P2;
		fate = null;
		spinEvery = spinLeft = 0;
		angle = 0;
		speed.set( 0 );
		acc.set( 0 );
		scale.set( 1 );
		resetColor();
		visible = true;
		revive();
		look( FxFrames.MOTE_1 );
		place();
		return this;
	}

	@Override
	public void revive(){
		super.revive();
		if (!counted){
			counted = true;
			FxBudget.add( FxBudget.PARTICLES, 1 );
		}
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			FxBudget.add( FxBudget.PARTICLES, -1 );
		}
	}

	// ------------------------------------------------------------------ setup

	/** Frames as a flipbook at fps from a random frame, or (fps 0) stepped through over its life. */
	public FxParticle look( int[][] frames, float fps ){
		this.frames = frames;
		single = null;
		this.fps = fps;
		start = fps > 0 ? FxRandom.Int( frames.length ) : 0;
		show( frames[start] );
		return this;
	}

	/** One frame. */
	public FxParticle look( int[] frame ){
		frames = null;
		single = frame;
		fps = 0;
		show( frame );
		return this;
	}

	/** Frames taking over at a share of its life (a tongue shrinking from large to medium). */
	public FxParticle then( float at, int[][] frames ){
		if (then1 == null){
			then1 = frames;
			at1 = at;
		} else {
			then2 = frames;
			at2 = at;
		}
		return this;
	}

	/** This frame instead while it is slower than `below` px/s (a spark become a dot). */
	public FxParticle slow( int[] frame, float below ){
		slowFrame = frame;
		slowBelow = below;
		return this;
	}

	/** Its colour along a ramp: as light it flows (at), as matter it steps (step). */
	public FxParticle ramp( Ramp r, boolean light ){
		ramp = r;
		this.light = light;
		apply( r.at( 0 ) );
		return this;
	}

	/** One colour (0xRRGGBB): a light frame shows it, a matter frame is lit in it. */
	public FxParticle paint( int color ){
		ramp = null;
		tint = color & 0xFFFFFF;
		apply( tint );
		return this;
	}

	/** Whether it is light (its alpha fades smoothly) or matter (it fades in painted steps). */
	public FxParticle light( boolean light ){
		this.light = light;
		return this;
	}

	/** Matter fading smoothly all the same: a big puff, a wisp (small matter fades in steps). */
	public FxParticle smooth(){
		smooth = true;
		return this;
	}

	public FxParticle life( float seconds ){
		life = Math.max( 0.01f, seconds );
		age = 0;
		return this;
	}

	/** Its alpha: rising to `peak` over `in` s, falling over its last `out` s. */
	public FxParticle alpha( float peak, float in, float out ){
		this.peak = peak;
		fadeIn = in;
		fadeOut = out;
		return this;
	}

	public FxParticle at( float x, float y ){
		px = x;
		py = y;
		return this;
	}

	public FxParticle vel( float vx, float vy ){
		this.vx = vx;
		this.vy = vy;
		return this;
	}

	/** Off at `angle` (radians, 0 east, positive down the screen) at `speed` px/s. */
	public FxParticle polar( float angle, float speed ){
		vx = (float)Math.cos( angle ) * speed;
		vy = (float)Math.sin( angle ) * speed;
		return this;
	}

	/** px/s^2, positive down: drops 240, debris 160-240, sparks 120; negative rises (embers). */
	public FxParticle gravity( float g ){
		gravity = g;
		return this;
	}

	/** The share of its speed it loses a second. */
	public FxParticle drag( float perSecond ){
		drag = perSecond;
		return this;
	}

	/** A sideways swing of `amp` px/s at `rate` rad/s, from a random phase. */
	public FxParticle sway( float amp, float rate ){
		swayAmp = amp;
		swayRate = rate;
		swayPhase = FxRandom.angle();
		return this;
	}

	/** A wobble of `amp` px at `rate` rad/s, by its height too (dx = A sin(wt + 0.3 y)). */
	public FxParticle turbulence( float amp, float rate ){
		turbAmp = amp;
		turbRate = rate;
		turbPhase = FxRandom.angle();
		return this;
	}

	/** Its share of the wind under an open sky (smoke 0.35, flames 0.15, embers 0.4, leaves 0.6). */
	public FxParticle wind( float share ){
		windShare = share;
		return this;
	}

	/** Its ground at y: what it does on reaching it (DIE, BOUNCE, SETTLE, RING, SPLAT, WET) and
	 *  how much of its fall a bounce keeps. */
	public FxParticle ground( float y, int landing, float bounce ){
		groundY = y;
		this.landing = landing;
		this.bounce = bounce;
		return this;
	}

	/** What a RING landing does off the water. */
	public FxParticle landDry( int landing ){
		dryLanding = landing;
		return this;
	}

	/** How hard it lands on water (WaterFX.drop: under 0.34 a small ring, under 0.67 a middling). */
	public FxParticle strength( float s ){
		strength = s;
		return this;
	}

	/** The colour its splat or wet mark (or ring) takes, if not its own. */
	public FxParticle mark( int color ){
		markColor = color;
		return this;
	}

	/** Turned to face its way (a line frame: a spark, a drop). */
	public FxParticle orient(){
		orient = true;
		return this;
	}

	/** Drawn on whole pixels whatever its speed. */
	public FxParticle snap(){
		snap = true;
		return this;
	}

	/** Shown only where the hero sees this point. */
	public FxParticle source( float x, float y ){
		sourced = true;
		srcX = x;
		srcY = y;
		return this;
	}

	public FxParticle priority( int p ){
		priority = p;
		return this;
	}

	public FxParticle fate( Fate f ){
		fate = f;
		return this;
	}

	/** A quarter turn every `seconds` (tumbling debris). */
	public FxParticle spin( float seconds ){
		spinEvery = spinLeft = seconds;
		return this;
	}

	// ------------------------------------------------------------------ reading it

	public int priority(){
		return priority;
	}

	/** How far through its life, 0..1. */
	public float progress(){
		return Math.min( 1f, age / life );
	}

	/** Its colour now, 0xRRGGBB. */
	public int colorNow(){
		if (ramp == null) return tint;
		float p = progress();
		return light ? ramp.at( p ) : ramp.step( p );
	}

	public boolean isLight(){
		return light;
	}

	// ------------------------------------------------------------------ the frame

	@Override
	public void update(){
		//slid with its layer (a rebase): its true position, its ground and its source go too
		float sx = x - placedX, sy = y - placedY;
		if (sx != 0 || sy != 0){
			px += sx;
			py += sy;
			groundY += sy;
			srcX += sx;
			srcY += sy;
		}
		float dt = Game.elapsed;
		age += dt;
		if (age >= life){
			end( false );
			return;
		}

		if (drag > 0){
			float k = Math.max( 0f, 1f - drag * dt );
			vx *= k;
			vy *= k;
		}
		vy += gravity * dt;
		float sway = swayAmp != 0 ? swayAmp * (float)Math.sin( swayPhase + age * swayRate ) : 0f;
		float wind = windShare != 0 ? windShare * Fx.windX() : 0f;
		px += (vx + sway + wind) * dt;
		py += vy * dt;

		if (spinEvery > 0 && (spinLeft -= dt) <= 0){
			spinLeft += spinEvery;
			angle = (angle + 90) % 360;
		}

		if (!Float.isNaN( groundY ) && py >= groundY && vy >= 0){
			py = groundY;
			if (land()) return;
		}

		float p = age / life;
		int[][] set = frames;
		if (then2 != null && p >= at2) set = then2;
		else if (then1 != null && p >= at1) set = then1;
		int[] f;
		float speed2 = vx * vx + vy * vy;
		if (slowFrame != null && speed2 < slowBelow * slowBelow){
			f = slowFrame;
		} else if (set == null){
			f = single;
		} else if (fps > 0){
			f = set[(start + (int)(age * fps)) % set.length];
		} else {
			f = set[Math.min( set.length - 1, (int)(p * set.length) )];
		}
		if (f != shown) show( f );

		if (ramp != null) apply( light ? ramp.at( p ) : ramp.step( p ) );

		float a = peak;
		if (fadeIn > 0 && age < fadeIn) a *= age / fadeIn;
		float left = life - age;
		if (fadeOut > 0 && left < fadeOut){
			float k = left / fadeOut;
			//light fades smoothly; matter in painted steps, 1, 0.6, 0.3, gone
			a *= light || smooth ? k : (k > 0.75f ? 1f : k > 0.5f ? 0.6f : k > 0.25f ? 0.3f : 0f);
		}
		am = a;

		if (orient && speed2 > 0.01f){
			angle = (float)Math.toDegrees( Math.atan2( vy, vx ) ) - 90f;
		}

		place();
		visible = a > 0.004f && (!sourced || Fx.seen( srcX, srcY ));
	}

	//its frame's top-left from its middle, on whole pixels when slow (or snapped)
	private void place(){
		float dx = px + (turbAmp != 0 ? turbAmp * (float)Math.sin( turbPhase + age * turbRate + 0.3f * py ) : 0f);
		float x0 = dx - origin.x, y0 = py - origin.y;
		if (snap || vx * vx + vy * vy < 144f){
			x0 = Math.round( x0 );
			y0 = Math.round( y0 );
		}
		x = placedX = x0;
		y = placedY = y0;
	}

	//a frame of the sheet, without allocating: its uv set in place, turned about its middle
	private void show( int[] f ){
		shown = f;
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		origin.set( f[2] / 2f, f[3] / 2f );
	}

	//lit by c: a white light frame shows c, a grey matter frame keeps its shading in it
	private void apply( int c ){
		hardlight( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
	}

	//on its ground: true if that is the end of it
	private boolean land(){
		if (fate != null) fate.end( this, true );
		if (!alive) return true;
		int mode = landing;
		if (mode == RING){
			if (WaterFX.onWater( px, py )){
				WaterFX.drop( px, py, strength, markColor );
				kill();
				return true;
			}
			mode = dryLanding;
		}
		switch (mode){
			case BOUNCE:
				if (bounces++ == 0 && bounce > 0){
					vy = -vy * bounce;
					vx *= 0.7f;
					return false;
				}
				//its second touch: it lies there
				vx = vy = 0;
				gravity = 0;
				groundY = Float.NaN;
				return false;
			case SETTLE:
				vx = vy = 0;
				gravity = swayAmp = 0;
				groundY = Float.NaN;
				//what it has left, half a second at most, to fade where it lies
				age = Math.max( age, life - Math.max( fadeOut, 0.5f ) );
				return false;
			case SPLAT:
				FxDecal.splat( px, py, markColor != -1 ? markColor : colorNow() );
				kill();
				return true;
			case WET:
				FxDecal.wet( px, py );
				kill();
				return true;
			default:
				kill();
				return true;
		}
	}

	private void end( boolean landed ){
		if (fate != null) fate.end( this, landed );
		kill();
	}
}
