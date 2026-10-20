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

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * A skill's ray (docs/fx/skills.md, SkillRay): the weight of the game's Beam and RayFx in a skill's
 * own colours, with particles riding it, so a ray is a dense, concentrated thing and never a faint
 * stroke.
 *
 * - show: a core of the light sheet's bands (CORE_BAND_1, 3, 5) along its way, striking in over
 *   STRIKE s a step at a time (1, 3, 5 px), held to HOLD of its life, then stepping 5, 3, 1 to its
 *   end, lit the look's core at CORE_ALPHA; a BAND_7 halo of the look's halo at HALO_ALPHA
 *   shimmering +-HALO_SHIMMER at HALO_HZ; a stream of motes born at the source every STREAM_EVERY s
 *   riding to the end at STREAM_SPEED px/s (P2 for its first STREAM_P2, P3 after; at most
 *   STREAM_MAX a ray); a mote per SHED_PX px of it peeling sideways as it strikes (P3, at most
 *   SHED_MAX); its ends crisp: a one-frame IMPACT_STAR at the source, a one-frame IMPACT_STAR, four
 *   motes flicked 4 to 8 px, a GLOW_11 (END_GLOW) and a POOL_23 (END_POOL) at the end: two lights a
 *   ray, no flare. HEAL is a mend flowing, never a hard core: a GLOW_7 bead racing to the end in
 *   BEAD_FOR s, a CORE_BAND_1 band of the heal's green behind it, four PULSE_DOTs flowing at
 *   DOT_SPEED px/s, green motes streaming; at the bead's arrival the heal's red crosses and a small
 *   green ring at the feet. The first FULL rays at once get all of it; later ones their core and
 *   halo only (no lights, no motes).
 * - stream: the mote stream alone (every STREAM_ALONE_EVERY s at STREAM_ALONE_SPEED px/s, at most
 *   STREAM_ALONE_MAX alive), following two visuals or between two points: what rides a restored
 *   Beam.LightRay without touching it.
 *
 * Looks: HOLY (core FFF6CC, halo F2C050, motes FFF4D0 to F1D99C), HEAL (the flow). (NATURE and STEEL
 * went with round 2: Charged Shot's release is its thin stroke again, StreakFX.)
 * Any thread (made on the render thread, its points read there), FxRandom only (a Beam drew none of
 * the game's Random), no allocation per frame, let go of with Fx.dispose when done.
 */
public final class SkillRay {

	private SkillRay(){}

	public static final int HOLY = 0, HEAL = 1;
	public static final int LOOKS = 2;

	/** Rays at once in full. */
	public static final int FULL = 3;
	/** The core: its strike (s), the share of its life it holds whole, its alpha. */
	public static final float STRIKE = 0.04f, HOLD = 0.6f, CORE_ALPHA = 0.9f;
	/** The halo: its alpha and its shimmer (+-, Hz). */
	public static final float HALO_ALPHA = 0.35f, HALO_SHIMMER = 0.06f, HALO_HZ = 20f;
	/** The stream: a mote every STREAM_EVERY s at STREAM_SPEED px/s living at most STREAM_LIFE s;
	 *  the first STREAM_P2 P2, the rest P3; at most STREAM_MAX alive a ray. */
	public static final float STREAM_EVERY = 0.015f, STREAM_SPEED = 200f, STREAM_LIFE = 0.4f;
	public static final int STREAM_P2 = 12, STREAM_MAX = 30;
	/** The sheds as it strikes: one a SHED_PX px, at most SHED_MAX. */
	public static final int SHED_PX = 10, SHED_MAX = 8;
	/** Its end's lights. */
	public static final float END_GLOW = 0.5f, END_POOL = 0.3f, STAR_LIFE = 0.04f;
	public static final int END_MOTES = 4;
	//a speed that carries a mote d px against a drag of 4 a second over 0.25 s: d x END_REACH px/s
	static final float END_REACH = (float) (4 / (1 - Math.exp( -4 * 0.25 )));
	/** The stream alone: every this often (s), this fast (px/s), at most this many alive. */
	public static final float STREAM_ALONE_EVERY = 0.03f, STREAM_ALONE_SPEED = 160f;
	public static final int STREAM_ALONE_MAX = 16;
	/** HEAL: the bead's run (s), the band's alpha, the dots' speed (px/s), the motes' (every s, px/s). */
	public static final float BEAD_FOR = 0.12f, BAND_ALPHA = 0.5f, DOT_SPEED = 80f, HEAL_EVERY = 0.02f, HEAL_SPEED = 120f;
	public static final int DOTS = 4;

	//by look: the core's colour, the halo's, the stream's motes
	static final int[] CORE = { SkillPalette.HOLY_RAY, SkillPalette.HEAL_LIGHT };
	static final int[] HALO = { SkillPalette.HOLY_STAR, SkillPalette.HEAL.stop( 0 ) };
	/** The streams' ramps by look (a pale flash for their first quarter, then their body). */
	public static final Ramp STREAM_HOLY = flashed( 0xFFF4D0, 0xF1D99C ), STREAM_HEAL = SkillPalette.HEAL;
	static final Ramp[] STREAM = { STREAM_HOLY, STREAM_HEAL };
	static final int[][] SHRINK = { FxFrames.MOTE_2, FxFrames.MOTE_1 };

	/** Two colours, the first only for the first quarter of a life (the palest stop is a flash). */
	public static Ramp flashed( int pale, int body ){
		return new Ramp( pale, body ).keyed( 0f, 0.25f );
	}

	private static int full;

	/** Rays in full now. */
	public static synchronized int full(){
		return full;
	}

	/** A new scene: none of the last one's counted. */
	public static synchronized void reset(){
		full = 0;
	}

	private static synchronized boolean enterFull(){
		if (full >= FULL) return false;
		full++;
		return true;
	}

	private static synchronized void leaveFull(){
		full = Math.max( 0, full - 1 );
	}

	// ------------------------------------------------------------------ show

	/** A ray from one point to another in `look` for `life` s, in the light layer. Any thread. */
	public static Gizmo show( PointF from, PointF to, int look, float life ){
		final Ray r = new Ray( from.x, from.y, to.x, to.y, Math.max( 0, Math.min( LOOKS - 1, look ) ), Math.max( 0.1f, life ) );
		Fx.post( () -> r.go( null, null ) );
		return r;
	}

	/** A ray between two visuals' middles as they are when it strikes. Any thread. */
	public static Gizmo show( Visual from, Visual to, int look, float life ){
		final Ray r = new Ray( 0, 0, 0, 0, Math.max( 0, Math.min( LOOKS - 1, look ) ), Math.max( 0.1f, life ) );
		Fx.post( () -> r.go( from, to ) );
		return r;
	}

	static final class Ray extends Group {
		private float sx, sy, ex, ey, len, angle;
		private final int look;
		private final float life;
		private boolean full, done, healed;
		private FxPic core, halo, bead;
		private final FxPic[] dots = new FxPic[DOTS];
		private float age, streamIn;
		private int streamed;
		private final float[] until = new float[STREAM_MAX];
		private int alive;
		private FxLight glow, pool;
		private int glowSetups, poolSetups;

		Ray( float sx, float sy, float ex, float ey, int look, float life ){
			this.sx = sx;
			this.sy = sy;
			this.ex = ex;
			this.ey = ey;
			this.look = look;
			this.life = life;
		}

		//on the render thread: its points read, its pictures made, its strike
		void go( Visual from, Visual to ){
			if (from != null && to != null){
				if (from.parent == null || to.parent == null) return;
				sx = midX( from );
				sy = midY( from );
				ex = midX( to );
				ey = midY( to );
			}
			Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
			if (lights == null || !(Fx.seen( sx, sy ) || Fx.seen( ex, ey ))) return;
			len = (float) Math.hypot( ex - sx, ey - sy );
			angle = (float) Math.toDegrees( Math.atan2( ey - sy, ex - sx ) );
			full = enterFull();
			halo = band( FxFrames.Light.BAND_7, HALO[look] );
			core = band( FxFrames.Light.CORE_BAND_1, CORE[look] );
			if (look == HEAL){
				bead = new FxPic( SkillVfx.LIGHT_SHEET );
				bead.additive = true;
				bead.show( FxFrames.Light.GLOW_7 ).lit( SkillPalette.HEAL.stop( 0 ) );
				add( bead );
				for (int i = 0; i < DOTS && full; i++){
					FxPic d = new FxPic( SkillVfx.LIGHT_SHEET );
					d.additive = true;
					d.show( FxFrames.Light.PULSE_DOT ).lit( SkillPalette.HEAL.stop( 0 ) );
					d.origin.set( 2.5f, 1.5f );
					d.angle = angle;
					d.visible = false;
					dots[i] = d;
					add( d );
				}
			}
			lights.add( this );
			if (full && look != HEAL) strike();
			pose();
		}

		private FxPic band( int[] frame, int color ){
			FxPic b = new FxPic( SkillVfx.LIGHT_SHEET );
			b.additive = true;
			b.show( frame ).lit( color );
			b.origin.set( 0, frame[3] / 2f );
			b.angle = angle;
			b.scale.set( len / frame[2], 1 );
			b.x = sx;
			b.y = sy - frame[3] / 2f;
			add( b );
			return b;
		}

		//its strike: crisp ends (a frame of a star at each), the end's lights, the sheds
		private void strike(){
			star( sx, sy );
			star( ex, ey );
			Ramp m = STREAM[look];
			for (int i = 0; i < END_MOTES; i++){
				FxParticle p = SkillVfx.glow( ex, ey, FxBudget.P2 );
				if (p != null) p.look( FxFrames.MOTE_2 ).ramp( m, true ).polar( FxRandom.angle(), FxRandom.Float( 4f, 8f ) * END_REACH )
						.drag( 4f ).life( 0.25f ).alpha( 1f, 0, 0.12f ).snap();
			}
			if (FxLight.room( 2, 0 )){
				glow = FxLight.glow( ex, ey, 11, HALO[look], END_GLOW ).life( 0, life * HOLD, life * (1 - HOLD) );
				pool = FxLight.pool( ex, ey + 6, 23, HALO[look], END_POOL ).life( 0, life * HOLD, life * (1 - HOLD) );
				glowSetups = glow.setups();
				poolSetups = pool.setups();
			}
			int sheds = Math.min( SHED_MAX, (int) (len / SHED_PX) );
			float nx = -(ey - sy) / Math.max( 1f, len ), ny = (ex - sx) / Math.max( 1f, len );
			for (int i = 0; i < sheds; i++){
				float u = (i + 0.5f) / sheds;
				FxParticle p = SkillVfx.glow( sx + (ex - sx) * u, sy + (ey - sy) * u, FxBudget.P3 );
				if (p == null) continue;
				float s = FxRandom.sign() * FxRandom.Float( 4f, 8f );
				p.look( FxFrames.MOTE_1 ).ramp( m, true ).vel( nx * s, ny * s ).life( 0.4f ).alpha( 0.9f, 0, 0.2f ).snap();
			}
		}

		private void star( float x, float y ){
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P0 );
			if (p != null) p.look( FxFrames.IMPACT_STAR[0] ).paint( CORE[look] ).life( STAR_LIFE ).alpha( 1f, 0, 0.01f ).snap();
		}

		@Override
		public void update(){
			super.update();
			if (done) return;
			float dt = Game.elapsed;
			age += dt;
			if (age >= life){
				finish();
				return;
			}
			pose();
			if (!full) return;
			if (look == HEAL){
				heal( dt );
				return;
			}
			//the stream, while the core holds
			for (int i = 0; i < alive; ){
				if (until[i] <= age) until[i] = until[--alive];
				else i++;
			}
			if (age < life * HOLD && (streamIn -= dt) <= 0){
				streamIn += STREAM_EVERY;
				if (alive < STREAM_MAX){
					float l = Math.min( len / STREAM_SPEED, STREAM_LIFE );
					FxParticle p = SkillVfx.glow( sx, sy, streamed < STREAM_P2 ? FxBudget.P2 : FxBudget.P3 );
					if (p != null){
						streamed++;
						until[alive++] = age + l;
						float k = STREAM_SPEED / Math.max( 1f, len );
						p.look( SHRINK, 0 ).ramp( STREAM[look], true ).vel( (ex - sx) * k, (ey - sy) * k ).life( l ).alpha( 0.95f, 0, 0.05f );
					}
				}
			}
		}

		//the core stepping 1, 3, 5 px in, held, 5, 3, 1 out; the halo shimmering
		private void pose(){
			if (core == null) return;
			if (look == HEAL){
				float k = Math.min( 1f, age / BEAD_FOR );
				core.scale.set( len * k / FxFrames.Light.CORE_BAND_1[2], 1 );
				core.am = BAND_ALPHA * fade();
				halo.scale.set( len * k / FxFrames.Light.BAND_7[2], 1 );
				halo.am = HALO_ALPHA * 0.6f * fade();
				if (bead != null){
					bead.visible = k < 1f;
					bead.place( sx + (ex - sx) * k, sy + (ey - sy) * k );
				}
				return;
			}
			float p = age / life;
			int[] f;
			if (age < STRIKE * 0.5f) f = FxFrames.Light.CORE_BAND_1;
			else if (age < STRIKE) f = FxFrames.Light.CORE_BAND_3;
			else if (p < HOLD) f = FxFrames.Light.CORE_BAND_5;
			else {
				float q = (p - HOLD) / (1 - HOLD);
				f = q < 0.34f ? FxFrames.Light.CORE_BAND_5 : (q < 0.67f ? FxFrames.Light.CORE_BAND_3 : FxFrames.Light.CORE_BAND_1);
			}
			core.show( f );
			core.origin.set( 0, f[3] / 2f );
			core.x = sx;
			core.y = sy - f[3] / 2f;
			core.scale.set( len / f[2], 1 );
			core.am = CORE_ALPHA * fade();
			halo.am = HALO_ALPHA * (1f + HALO_SHIMMER * (float) Math.sin( 2 * Math.PI * HALO_HZ * age )) * fade();
		}

		//whole while it holds, then down to nothing
		private float fade(){
			float p = age / life;
			return p < HOLD ? 1f : Math.max( 0f, 1f - (p - HOLD) / (1 - HOLD) );
		}

		//the heal's flow: dots running down it, green motes streaming, and the crosses where the bead lands
		private void heal( float dt ){
			for (int i = 0; i < DOTS; i++){
				FxPic d = dots[i];
				if (d == null) continue;
				float u = ((age * DOT_SPEED / Math.max( 1f, len )) + i / (float) DOTS) % 1f;
				boolean on = age > BEAD_FOR && u * len <= len * Math.min( 1f, age / BEAD_FOR );
				d.visible = on;
				if (!on) continue;
				d.x = sx + (ex - sx) * u - 2.5f;
				d.y = sy + (ey - sy) * u - 1.5f;
				d.am = 0.8f * fade() * Math.min( 1f, Math.min( u, 1f - u ) * 6f );
			}
			for (int i = 0; i < alive; ){
				if (until[i] <= age) until[i] = until[--alive];
				else i++;
			}
			if (age < life * HOLD && (streamIn -= dt) <= 0){
				streamIn += HEAL_EVERY;
				if (alive < STREAM_MAX){
					float l = Math.min( len / HEAL_SPEED, STREAM_LIFE );
					FxParticle p = SkillVfx.glow( sx + FxRandom.Float( -1f, 1f ), sy + FxRandom.Float( -1f, 1f ),
							streamed < STREAM_P2 ? FxBudget.P2 : FxBudget.P3 );
					if (p != null){
						streamed++;
						until[alive++] = age + l;
						float k = HEAL_SPEED / Math.max( 1f, len );
						p.look( SHRINK, 0 ).ramp( STREAM_HEAL, true ).vel( (ex - sx) * k, (ey - sy) * k ).life( l ).alpha( 0.9f, 0, 0.05f );
					}
				}
			}
			if (!healed && age >= BEAD_FOR){
				healed = true;
				MoteFX.crosses( ex, ey, 2 );
				Group floor = GameScene.layer( GameScene.Layer.FLOOR );
				if (floor != null) FxRing.ground( floor, ex, ey + 6, 9, 13, 0.35f, SkillPalette.HEAL_LIGHT, 0.5f, true );
			}
		}

		private void finish(){
			done = true;
			if (full){
				full = false;
				leaveFull();
			}
			if (glow != null && glow.setups() == glowSetups) glow.putOut( 0.05f );
			if (pool != null && pool.setups() == poolSetups) pool.putOut( 0.05f );
			glow = pool = null;
			Fx.dispose( this );
		}

		@Override
		public void kill(){
			super.kill();
			done = true;
			if (full){
				full = false;
				leaveFull();
			}
		}
	}

	// ------------------------------------------------------------------ stream

	/** The mote stream alone from one point to another, for `seconds`. Any thread. */
	public static Gizmo stream( PointF from, PointF to, Ramp ramp, float seconds ){
		final Stream s = new Stream( null, null, from.x, from.y, to.x, to.y, ramp, seconds );
		Fx.post( s::go );
		return s;
	}

	/** The same between two visuals' middles, following both. Any thread. */
	public static Gizmo stream( Visual from, Visual to, Ramp ramp, float seconds ){
		final Stream s = new Stream( from, to, 0, 0, 0, 0, ramp, seconds );
		Fx.post( s::go );
		return s;
	}

	static final class Stream extends Gizmo {
		private final Visual from, to;
		private float sx, sy, ex, ey;
		private final Ramp ramp;
		private final float seconds;
		private float age, nextIn;
		private final float[] until = new float[STREAM_ALONE_MAX];
		private int alive;

		Stream( Visual from, Visual to, float sx, float sy, float ex, float ey, Ramp ramp, float seconds ){
			this.from = from;
			this.to = to;
			this.sx = sx;
			this.sy = sy;
			this.ex = ex;
			this.ey = ey;
			this.ramp = ramp;
			this.seconds = seconds;
		}

		void go(){
			Group lights = GameScene.layer( GameScene.Layer.LIGHTS );
			if (lights == null) return;
			lights.add( this );
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			age += dt;
			if (age >= seconds){
				killAndErase();
				return;
			}
			if (from != null && to != null){
				if (from.parent == null || to.parent == null){
					killAndErase();
					return;
				}
				sx = midX( from );
				sy = midY( from );
				ex = midX( to );
				ey = midY( to );
			}
			for (int i = 0; i < alive; ){
				if (until[i] <= age) until[i] = until[--alive];
				else i++;
			}
			if ((nextIn -= dt) > 0) return;
			nextIn += STREAM_ALONE_EVERY;
			if (alive >= STREAM_ALONE_MAX || !(Fx.seen( sx, sy ) || Fx.seen( ex, ey ))) return;
			float len = (float) Math.hypot( ex - sx, ey - sy );
			if (len < 1f) return;
			float l = len / STREAM_ALONE_SPEED;
			FxParticle p = SkillVfx.glow( sx, sy, FxBudget.P2 );
			if (p == null) return;
			until[alive++] = age + l;
			float k = STREAM_ALONE_SPEED / len;
			p.look( SHRINK, 0 ).ramp( ramp, true ).vel( (ex - sx) * k, (ey - sy) * k ).life( l ).alpha( 0.95f, 0.02f, 0.05f );
		}
	}

	//a visual's middle, without allocating
	static float midX( Visual v ){
		return v instanceof CharSprite ? SkillVfx.midX( (CharSprite) v ) : v.x + v.width() / 2f;
	}

	static float midY( Visual v ){
		return v instanceof CharSprite ? SkillVfx.midY( (CharSprite) v ) : v.y + v.height() / 2f;
	}
}
