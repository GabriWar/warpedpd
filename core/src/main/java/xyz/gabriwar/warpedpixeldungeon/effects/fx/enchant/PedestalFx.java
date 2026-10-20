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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * The enchanting pedestal's own pictures (EnchantingScene and its window of three weaves): the
 * kit's layers belong to the game's scene, so this is a group of the scene (or the window) with
 * its own pooled FxEmitters for bursts and a small UI mote (Mote: one frame or a run of the kit's
 * sheet, added as light) that the game's FxBudget never counts. FxRandom only, render thread, no
 * allocation in a frame's update. At most CAP motes live at once across every pedestal group.
 *
 *   burst(x, y, look, n, speed)     n motes of the look's frame thrown out (a gem igniting, a gem taken)
 *   motif(look, x, y, owner)        one mote of the look's motif (a gem's live magic), tagged with its owner
 *   comet(from, to, look, landed)   the look's lead frame flying a curve in COMET_FOR s, shedding SHED motes,
 *                                   STREAM motes streaming behind it; `landed` as it lands
 *   glint(x, y, color)              a PLUS_3 for two frames (a new pip)
 *   leak(x, y, color, dx, dy)       a mote leaking out of the ring and falling, greying (an unweave)
 *   echoRing(x, y, scale)           the vestige's sign: two thin rings 0.06 s apart in the echo's grey
 *   motion(family, t, i, out)       how the ring's motes move for a family (pure: no random of either kind)
 */
public class PedestalFx extends Group {

	/** The most motes alive at once, every pedestal group together. */
	public static final int CAP = 40;
	/** A gem's ignition burst, a taken gem's; the comet's flight, its shed motes and its stream. */
	public static final int IGNITE = 6, TAKEN = 8, SHED = 5, STREAM = 12;
	public static final float COMET_FOR = 0.4f, STREAM_GAP = 0.018f;
	/** A pip's glint (two frames), an unweave's leak, the echo's rings. */
	public static final float GLINT_FOR = 2f / 60f, LEAK_FOR = 0.5f, ECHO_GAP = 0.06f, ECHO_FOR = 0.35f;
	/** The grey a leak turns, and the echo's. */
	public static final int ASH = 0x707070;

	//motes alive across every group (render thread)
	private static int live;

	/** Motes alive now, every pedestal group together. */
	public static int live(){
		return live;
	}

	//a few timers (a comet's landing), fixed: a full list runs the new one at once
	private final Runnable[] timers = new Runnable[8];
	private final float[] timerLeft = new float[8];

	@Override
	public void update(){
		super.update();
		float dt = Game.elapsed;
		for (int i = 0; i < timers.length; i++){
			if (timers[i] == null) continue;
			if ((timerLeft[i] -= dt) > 0) continue;
			Runnable r = timers[i];
			timers[i] = null;
			r.run();
		}
	}

	/** Runs `r` `seconds` from now with the group (never, if the group goes first). */
	public void after( float seconds, Runnable r ){
		for (int i = 0; i < timers.length; i++){
			if (timers[i] != null) continue;
			timers[i] = r;
			timerLeft[i] = seconds;
			return;
		}
		r.run();
	}

	// ------------------------------------------------------------------ the looks' pieces

	//single frames as runs, made once
	static final int[][] EMBER = { FxFrames.EMBER_2 }, MOTE_1 = { FxFrames.MOTE_1 }, MOTE_2 = { FxFrames.MOTE_2 },
			PLUS_3 = { FxFrames.PLUS_3 }, SPARK_3 = { FxFrames.SPARK_3 }, BLADE_3 = { FxFrames.BLADE_3 },
			STREAK = { FxFrames.STREAK_TAPER };
	private static final int[][][] LEADS = new int[EnchantLooks.COUNT][][];

	//a look's own frame (its gained rune, or a mote for a ghost's), as a run
	static int[][] lead( EnchantLook l ){
		if (l.code < 0 || l.code >= LEADS.length) return MOTE_2;
		if (LEADS[l.code] == null) LEADS[l.code] = l.rune != null ? new int[][]{ l.rune } : MOTE_2;
		return LEADS[l.code];
	}

	//a look's bright colour, and the deep one it fades toward
	static int bright( EnchantLook l ){
		return l.curse ? Element.VOID.ramp.stop( 1 ) : l.light;
	}

	static int deep( EnchantLook l ){
		Ramp r = l.curse ? Element.VOID.ramp : l.el.ramp;
		return r.stop( Math.min( r.size() - 1, 3 ) );
	}

	/** One mote: frames stepped over its life, from (x, y), colour c0 to c1, up to `peak`. Null at
	 *  the cap. */
	public Mote mote( int[][] frames, float x, float y, int c0, int c1, float peak, float life ){
		return mote( this, frames, x, y, c0, c1, peak, life );
	}

	static Mote mote( Group in, int[][] frames, float x, float y, int c0, int c1, float peak, float life ){
		if (live >= CAP || frames == null || frames.length == 0) return null;
		Mote m = (Mote) in.recycle( Mote.class );
		if (m == null) return null;
		return m.setup( frames, x, y, c0, c1, peak, life );
	}

	/** n motes of the look's frame thrown out from (x, y) at about `speed` px/s (a pooled emitter,
	 *  next frame). */
	public void burst( float x, float y, EnchantLook l, int n, float speed ){
		if (l == null || n <= 0) return;
		FxEmitter e = (FxEmitter) recycle( FxEmitter.class );
		if (e == null) return;
		e.revive();
		e.pos( x, y );
		e.burst( new Burst( l, speed ), n );
	}

	//a burst's motes: a ring of the look's frame outward, slowing, bright to deep
	private static final class Burst extends Emitter.Factory {
		final EnchantLook l;
		final float speed;
		final float a0 = FxRandom.angle();

		Burst( EnchantLook l, float speed ){
			this.l = l;
			this.speed = speed;
		}

		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			Mote m = mote( emitter, lead( l ), x, y, bright( l ), deep( l ), 1f, FxRandom.Float( 0.4f, 0.6f ) );
			if (m == null) return;
			float a = a0 + (float)(2 * Math.PI * index / 8) + FxRandom.Float( -0.25f, 0.25f );
			float v = speed * FxRandom.Float( 0.7f, 1.2f );
			m.vel( (float)Math.cos( a ) * v, (float)Math.sin( a ) * v ).drag( 3f );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	}

	/** One mote of the look's motif at (x, y) (a gem's magic, shown as a window shows it). */
	public Mote motif( EnchantLook l, float x, float y, Object owner ){
		if (l == null) return null;
		int c0 = bright( l ), c1 = deep( l );
		Mote m;
		switch (l.motif){
			case RISE:
				m = mote( l.fam == Family.EMBER ? EMBER : MOTE_1, x, y, c0, c1, 0.9f, 0.9f );
				if (m != null) m.vel( FxRandom.Float( -1f, 1f ), -FxRandom.Float( 6f, 9f ) );
				break;
			case FALL:
				m = mote( l.fam == Family.RIME ? FxFrames.SNOWFLAKE_5 : PLUS_3, x, y, c0, c1, 0.8f, 1.1f );
				if (m != null) m.vel( FxRandom.Float( -1.5f, 1.5f ), FxRandom.Float( 3f, 5f ) );
				break;
			case DRIP:
				m = mote( MotifFx.DRIP_RUN, x, y + 2f, c0, c1, 1f, 1f );
				if (m != null) m.accel( 0, 25f );
				break;
			case SIP:
				m = mote( MotifFx.SIP_RUN, x, y + 4f, c0, c1, 1f, 1f );
				if (m != null) m.vel( 0, -8f );
				break;
			case BUBBLE:
				m = mote( MotifFx.BUBBLE_RUN, x, y, c0, c1, 1f, 0.8f );
				if (m != null) m.vel( 0, -4f );
				break;
			case SEEP:
				m = mote( FxFrames.INK_WISP, x, y, l.rim, c1, 0.8f, 1.4f );
				if (m != null) m.vel( FxRandom.Float( -1f, 1f ), -3f );
				break;
			case CRACKLE:
				m = mote( SPARK_3, x + FxRandom.Float( -3f, 3f ), y, l.el.glint, c0, 1f, 0.08f );
				if (m != null) m.angle = FxRandom.Float( 360f );
				break;
			case ORBIT:
				m = mote( lead( l ), x - 5f, y, c0, c1, 0.9f, 0.7f );
				if (m != null) m.vel( 14f, -2f ).drag( 1.5f );
				break;
			case GLINT:
				m = mote( MotifFx.GLINT_RUN, x - 2f, y + 2f, 0xFFFFFF, c0, 1f, 0.3f );
				if (m != null) m.vel( 10f, -10f );
				break;
			case SPROUT:
				m = mote( BLADE_3, x, y + 3f, c0, c1, 1f, 1.2f );
				if (m != null) m.vel( 0, -6f ).accel( 0, 8f );
				break;
			case THROB:
				m = mote( MotifFx.THROB_RUN, x, y + 3f, c0, c1, 0.7f, 0.4f );
				break;
			case GHOST:
				m = mote( STREAK, x - 3f, y, l.rim, EnchantLook.GHOST, 0.3f, 0.6f );
				if (m != null) m.vel( 5f, 0 );
				break;
			case CYCLE:
			default: {
				int c = Profile.cycleColor( l.cycle, Game.timeTotal, 1f );
				m = mote( MOTE_2, x, y, c, c, 0.9f, 0.8f );
				if (m != null) m.vel( 0, -5f );
				break;
			}
		}
		if (m != null) m.owner = owner;
		return m;
	}

	/**
	 * A comet of the look from (x0, y0) to (x1, y1): its lead frame flies a curve (lifted above
	 * the straight way) in COMET_FOR s, speeding up; SHED motes left hanging along the way; STREAM
	 * motes streaming down the same curve behind it, each a little later and fainter. `landed` runs
	 * as it lands.
	 */
	public void comet( float x0, float y0, float x1, float y1, EnchantLook l, Runnable landed ){
		if (l == null) return;
		float dx = x1 - x0, dy = y1 - y0, len = (float)Math.sqrt( dx * dx + dy * dy );
		//the lift: off the middle, a third of the way's length, to the side above it
		float nx = len > 0 ? -dy / len : 0, ny = len > 0 ? dx / len : -1;
		if (ny > 0){
			nx = -nx;
			ny = -ny;
		}
		float qx = (x0 + x1) / 2f + nx * len * 0.33f, qy = (y0 + y1) / 2f + ny * len * 0.33f;
		int c0 = bright( l ), c1 = deep( l );
		Mote head = mote( lead( l ), x0, y0, 0xFFFFFF, c0, 1f, COMET_FOR );
		if (head != null){
			head.along( x0, y0, qx, qy, x1, y1, COMET_FOR, 0f );
			head.scale.set( 2f );
		}
		for (int i = 0; i < STREAM; i++){
			float lag = STREAM_GAP * (i + 1);
			Mote m = mote( MOTE_2, x0, y0, c0, c1, 0.9f * (1f - i / (float)(STREAM + 2)), COMET_FOR + lag );
			if (m != null) m.along( x0, y0, qx, qy, x1, y1, COMET_FOR, lag );
		}
		for (int i = 0; i < SHED; i++){
			float t = (i + 1) / (float)(SHED + 1);
			float u = ease( t );
			float sx = bez( x0, qx, x1, u ), sy = bez( y0, qy, y1, u );
			Mote m = mote( lead( l ), sx, sy, c0, c1, 0.8f, 0.35f );
			if (m != null){
				m.delay = t * COMET_FOR;
				m.vel( FxRandom.Float( -4f, 4f ), FxRandom.Float( 2f, 6f ) ).drag( 2f );
			}
		}
		if (landed != null) after( COMET_FOR, landed );
	}

	static float ease( float t ){
		return t * t * (1.6f - 0.6f * t);
	}

	static float bez( float a, float q, float b, float t ){
		float u = 1f - t;
		return u * u * a + 2 * u * t * q + t * t * b;
	}

	/** A PLUS_3 glint for two frames (a new pip). */
	public void glint( float x, float y, int color ){
		mote( PLUS_3, x, y, 0xFFFFFF, color, 1f, GLINT_FOR );
	}

	/** A mote leaking out of the ring at (x, y) toward (dx, dy) and falling, its colour greying
	 *  over LEAK_FOR s (an unweave). */
	public void leak( float x, float y, int color, float dx, float dy ){
		Mote m = mote( MOTE_2, x, y, color, ASH, 1f, LEAK_FOR * FxRandom.Float( 0.85f, 1.15f ) );
		if (m != null) m.vel( dx * FxRandom.Float( 6f, 12f ), dy * FxRandom.Float( 6f, 12f ) ).accel( 0, 40f );
	}

	/** The vestige's sign: two thin rings opening 0.06 s apart, in the echo's grey. */
	public void echoRing( float x, float y, float scale ){
		for (int i = 0; i < 2; i++){
			Mote m = mote( GainFx.ECHO_RING, x, y, EnchantLook.GHOST, EnchantLook.GHOST, GainFx.ECHO_ALPHA, ECHO_FOR );
			if (m == null) return;
			m.scale.set( scale );
			m.delay = ECHO_GAP * i;
		}
	}

	// ------------------------------------------------------------------ the ring's motion

	/**
	 * How the pedestal ring's mote i moves for a family at t s: out[0] and out[1] a shift (px),
	 * out[2] how far along its radius (px, out positive), out[3] its alpha's share. Embers rise and
	 * flicker, frost drifts down, static jumps a pixel now and then, venom and blood drip, growth
	 * sways, dread and drains pull in, force pulses out, radiance rises slowly, fortune twinkles,
	 * mirages slide, hearts float; wards hold still. A pure function: no random of either kind.
	 */
	public static void motion( Family f, float t, int i, float[] out ){
		float k = i * 0.618f;
		out[0] = out[1] = out[2] = 0f;
		out[3] = 1f;
		if (f == null) return;
		switch (f){
			case EMBER: {
				float u = frac( t * 0.9f + k );
				out[1] = -3f * u;
				out[3] = (1f - 0.5f * u) * (0.9f + 0.1f * (float)Math.sin( 13f * t + i ));
				break;
			}
			case RIME: {
				float u = frac( t * 0.35f + k );
				out[1] = 2.5f * u;
				out[3] = 0.85f + 0.15f * (float)Math.sin( 3f * t + i );
				break;
			}
			case STORM: {
				int w = (int)Math.floor( t / 0.45f + k );
				if (Profile.hash( w, i ) < 0.25f){
					out[0] = Profile.hash( w + 31, i ) < 0.5f ? -1f : 1f;
					out[1] = Profile.hash( w + 77, i ) < 0.5f ? -1f : 1f;
					out[3] = 1.2f;
				}
				break;
			}
			case VENOM: case CUT: {
				float u = frac( t * (f == Family.CUT ? 0.7f : 0.5f) + k );
				out[1] = 3f * u * u;
				out[3] = 1f - 0.6f * u;
				break;
			}
			case VERDANT:
				out[0] = 1.2f * (float)Math.sin( 1.8f * t + k * 6f );
				out[1] = -0.6f * Math.abs( (float)Math.sin( 0.9f * t + k * 6f ) );
				break;
			case DREAD:
				out[2] = -1.5f * (0.5f + 0.5f * (float)Math.sin( 1.2f * t + k * 6f ));
				out[3] = 0.8f;
				break;
			case REAPER: {
				float u = frac( t * 0.3f + k );
				out[1] = 2f * u;
				out[3] = 1f - 0.5f * u;
				break;
			}
			case DRAIN:
				out[2] = -2f * Profile.HEARTBEAT.value( t, 0f ) / Profile.LUB;
				break;
			case FORCE:
				out[2] = 2f * Profile.pulse( t, 0.45f ) / 0.6f;
				break;
			case RADIANCE: {
				float u = frac( t * 0.4f + k );
				out[1] = -2f * u;
				out[3] = 0.9f + 0.1f * (float)Math.sin( 2 * Math.PI * 3f * t + i );
				break;
			}
			case FORTUNE:
				out[3] = 0.6f + 0.4f * (float)Math.sin( 2 * Math.PI * 2.2f * t + k * 6f );
				break;
			case MIRAGE:
				out[0] = 1.5f * (float)Math.sin( 0.8f * t + k * 6f );
				out[3] = 0.75f;
				break;
			case HEART: {
				float u = frac( t * 0.5f + k );
				out[1] = -2.5f * u;
				out[3] = 1f - 0.4f * u;
				break;
			}
			case WARD:
			default:
				break;
		}
	}

	private static float frac( float x ){
		return x - (float)Math.floor( x );
	}

	// ------------------------------------------------------------------ a mote

	/**
	 * A mote of the pedestal: a frame (or a run of frames stepped over its life) of the kit's
	 * sheet, added as light; its middle moving at a speed with a pull and a drag, or flying a curve;
	 * its colour going from one to another over its life; up to its alpha, gone by the end. Shown
	 * only after its delay. Pooled in its group; counted toward CAP while it lives.
	 */
	public static class Mote extends Image {

		private int[][] frames;
		private int shown = -1;
		private boolean counted;
		private float age, life, peak, vx, vy, ax, ay, drag, mx, my;
		float delay;
		private int c0, c1;
		//along a curve: its points, its flight, how late it starts
		private boolean curve;
		private float x0, y0, qx, qy, x1, y1, travel, lag;
		/** Whose it is (a card's gem keeps its own). */
		public Object owner;
		private final RectF uv = new RectF();

		public Mote(){
			super( Assets.Effects.FX_SHEET );
		}

		Mote setup( int[][] frames, float x, float y, int c0, int c1, float peak, float life ){
			this.frames = frames;
			shown = -1;
			mx = x;
			my = y;
			this.c0 = c0 & 0xFFFFFF;
			this.c1 = c1 & 0xFFFFFF;
			this.peak = peak;
			this.life = Math.max( 0.01f, life );
			age = delay = 0f;
			vx = vy = ax = ay = drag = 0f;
			curve = false;
			owner = null;
			angle = 0;
			scale.set( 1 );
			resetColor();
			paint( this.c0 );
			show( 0 );
			am = peak;
			visible = true;
			revive();
			if (!counted){
				counted = true;
				live++;
			}
			return this;
		}

		public Mote vel( float vx, float vy ){
			this.vx = vx;
			this.vy = vy;
			return this;
		}

		public Mote accel( float ax, float ay ){
			this.ax = ax;
			this.ay = ay;
			return this;
		}

		public Mote drag( float perSecond ){
			drag = perSecond;
			return this;
		}

		/** Flying the curve a-q-b over `travel` s, starting `lag` s late (waiting at a till then). */
		public Mote along( float ax, float ay, float qx, float qy, float bx, float by, float travel, float lag ){
			curve = true;
			x0 = ax;
			y0 = ay;
			this.qx = qx;
			this.qy = qy;
			x1 = bx;
			y1 = by;
			this.travel = Math.max( 0.01f, travel );
			this.lag = lag;
			return this;
		}

		private void paint( int c ){
			hardlight( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
		}

		private void show( int i ){
			if (i == shown) return;
			shown = i;
			int[] f = frames[i];
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
			origin.set( f[2] / 2f, f[3] / 2f );
			place();
		}

		private void place(){
			x = Math.round( mx - origin.x );
			y = Math.round( my - origin.y );
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			if (delay > 0){
				delay -= dt;
				visible = false;
				return;
			}
			age += dt;
			if (age >= life){
				kill();
				return;
			}
			if (curve){
				float t = Math.max( 0f, Math.min( 1f, (age - lag) / travel ) );
				float u = ease( t );
				mx = bez( x0, qx, x1, u );
				my = bez( y0, qy, y1, u );
			} else {
				vx += ax * dt;
				vy += ay * dt;
				if (drag > 0){
					float k = Math.max( 0f, 1f - drag * dt );
					vx *= k;
					vy *= k;
				}
				mx += vx * dt;
				my += vy * dt;
			}
			float p = age / life;
			show( Math.min( frames.length - 1, (int)(p * frames.length) ) );
			place();
			paint( c0 == c1 ? c0 : Ramp.lerp( c0, c1, p ) );
			float a = p < 0.6f ? 1f : 1f - (p - 0.6f) / 0.4f;
			am = peak * a;
			visible = am > 0.004f;
		}

		@Override
		public void draw(){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}

		@Override
		public void kill(){
			super.kill();
			uncount();
		}

		@Override
		public void destroy(){
			uncount();
			super.destroy();
		}

		private void uncount(){
			if (counted){
				counted = false;
				live--;
			}
		}
	}
}
