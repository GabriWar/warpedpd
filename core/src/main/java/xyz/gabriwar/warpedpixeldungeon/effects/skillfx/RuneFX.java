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
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The runic language (docs/fx/skills.md, runes): the carved RUNE_3 glyphs the owner loves on Spirit
 * Volley and on an item's type being changed (Enchanting's turning ring), as one kit piece. A rune
 * alone is a dot; runes read in numbers (4 to 8), in a pattern and as they change glyph (every
 * GLYPH_EVERY s while they hold), so every verb draws a pattern of them:
 *
 * - ring: Enchanting's ring, turning (turn rad/s, negative the other way, from a phase), the far
 *   half dimmer (BACK), fading in over RING_IN and out over RING_OUT; then spin, tighten, release
 *   (thrown out along their angles) or lift (rising off their places);
 * - lock: WardFx's ward, the runes snapping on one by one `gap` s apart (two frames at full, in the
 *   ramp's flash, then LOCK_ALPHA), still, each held `hold` s then fading over LOCK_FADE; on a whole
 *   ring or an arc (a struck side);
 * - shut: WardFx's finisher, the ring closing on its point in `seconds`, then a one-frame STAR4_5
 *   (P0) where it shuts;
 * - scatter: a ward breaking, runes thrown apart `dist` px (0.6 to 1 of it) with a drag, fading;
 * - rise: WardFx's finale, a ring of runes rising off the feet;
 * - stamp: one rune still (a RUNE_3 at x2, or a RUNE_5 at x1), fading its last STAMP_FADE s;
 * - steady (chained): the runes keep the glyph they were born with;
 * - shed: one rune rising off a stream (a particle, P3).
 *
 * Where runes go and where they never go: the arcane, spells, enchants, wards and binds; never fire,
 * frost, storm, nature, martial blows, shadow ink, heals or mana gains; never held or stamped over a
 * head (SoulMark's MARKED look), never on the floor like a lock tile, never rising off the hands like
 * a scroll read. In a skill runes circle, lock in, snap shut, stamp, scatter, rise as a ring, or
 * stream off a bolt.
 *
 * Caps: CAP Runes live at once (counted; past it a call shows nothing, its chained calls do
 * nothing), RUNES runes each; sheds through FxBudget (P3). The pictures are FxPics of the sheet,
 * added in the light layer; no allocation per frame; a Visual target is read every frame (its feet
 * for ring and rise, its middle for the rest). Every verb is any-thread (the gizmo is added on the
 * render thread), draws only FxRandom, and lets itself go with Fx.dispose when done. No method here
 * is called burst, start, pour or rain (the skills' draw-site censuses count those words).
 */
public final class RuneFX {

	private RuneFX(){}

	/** Runes gizmos live at once, and runes a gizmo. */
	public static final int CAP = 8, RUNES = 8;
	/** A held rune changes glyph this often, s. */
	public static final float GLYPH_EVERY = 0.25f;

	/** ring: its runes, its ellipse (px), its turn (rad/s), the far half's alpha, its fades (s). */
	public static final int RING_N = 6;
	public static final float RING_RX = 5f, RING_RY = 2f, RING_TURN = 2.4f, BACK = 0.5f, RING_IN = 0.15f, RING_OUT = 0.25f;
	/** release's drag (a share of its speed a second). */
	public static final float RELEASE_DRAG = 3f;
	/** lock: its gap (s), its ellipse (px), its hold and fade (s), the frames it snaps in at full,
	 *  its alpha held. */
	public static final float LOCK_GAP = 0.08f, LOCK_RX = 8f, LOCK_RY = 4f, LOCK_HOLD = 0.3f, LOCK_FADE = 0.3f, LOCK_ALPHA = 0.9f;
	public static final int SNAP_FRAMES = 2;
	/** shut: its ellipse (px) and its time (s); the star where it shuts lives this long. */
	public static final float SHUT_RX = 9f, SHUT_RY = 4.5f, SHUT_FOR = 0.2f, STAR_LIFE = 0.04f;
	/** scatter: how far (px), how long (s), its drag. */
	public static final float SCATTER_DIST = 10f, SCATTER_LIFE = 0.35f, SCATTER_DRAG = 3f;
	/** A scatter's height over its width: the plain one's, a ward's ring breaking (a lock's 2:1). */
	public static final float SQUASH = 0.6f, WARD_SQUASH = 0.5f;
	/** rise: its ellipse (px), its speed (px/s), its life (s). */
	public static final float RISE_RX = 9f, RISE_RY = 3.5f, RISE_SPEED = 18f, RISE_LIFE = 0.6f;
	/** stamp: its life and its fade (s). */
	public static final float STAMP_LIFE = 0.35f, STAMP_FADE = 0.2f;
	/** shed: its sway of speed (px/s), its rise (px/s), its life (s). */
	public static final float SHED_SWAY = 3f, SHED_RISE = 8f, SHED_LIFE = 0.5f;

	static final int RING = 0, LOCK = 1, SHUT = 2, SCATTER = 3, RISE = 4, STAMP = 5;
	static final int MIDDLE = 0, FEET = 1;

	private static int live;

	/** Runes gizmos alive now. */
	public static synchronized int live(){
		return live;
	}

	/** A new scene: none of the last one's counted. */
	public static synchronized void reset(){
		live = 0;
	}

	private static synchronized boolean enter(){
		if (live >= CAP) return false;
		live++;
		return true;
	}

	private static synchronized void leave(){
		live = Math.max( 0, live - 1 );
	}

	// ------------------------------------------------------------------ ring

	/** Enchanting's ring at (x, y): n runes on an rx x ry ellipse turning `turn` rad/s (negative the
	 *  other way), for `life` s. */
	public static Runes ring( float x, float y, int n, Ramp ramp, float rx, float ry, float turn, float life ){
		return ring( x, y, n, ramp, rx, ry, turn, life, FxRandom.angle() );
	}

	/** The same from a phase (radians: 0 right, PI/2 the near side). */
	public static Runes ring( float x, float y, int n, Ramp ramp, float rx, float ry, float turn, float life, float phase ){
		Runes r = make( RING, n, ramp, rx, ry, life );
		r.at( x, y );
		r.turn = turn;
		r.phase = phase;
		return r.add();
	}

	/** The ring round a visual's feet, following it. */
	public static Runes ring( Visual target, int n, Ramp ramp, float rx, float ry, float turn, float life ){
		return ring( target, n, ramp, rx, ry, turn, life, FxRandom.angle() );
	}

	public static Runes ring( Visual target, int n, Ramp ramp, float rx, float ry, float turn, float life, float phase ){
		Runes r = make( RING, n, ramp, rx, ry, life );
		r.follow( target, FEET );
		r.turn = turn;
		r.phase = phase;
		return r.add();
	}

	/** The ring round a visual's feet with Enchanting's numbers (RING_N runes, RING_RX x RING_RY,
	 *  RING_TURN). */
	public static Runes ring( Visual target, Ramp ramp, float life ){
		return ring( target, RING_N, ramp, RING_RX, RING_RY, RING_TURN, life );
	}

	// ------------------------------------------------------------------ lock

	/** WardFx's ward at (x, y): n runes snapping onto a whole rx x ry ring `gap` s apart, each held
	 *  `hold` s, then fading over LOCK_FADE. */
	public static Runes lock( float x, float y, int n, Ramp ramp, float rx, float ry, float gap, float hold ){
		Runes r = make( LOCK, n, ramp, rx, ry, (Math.max( 1, n ) - 1) * gap + hold + LOCK_FADE );
		r.at( x, y );
		r.lockRing( gap, hold, FxRandom.angle(), (float) (2 * Math.PI) );
		return r.add();
	}

	/** The same on an arc only: centred on `angle` (radians, 0 right, PI/2 the near side), `spread`
	 *  wide (a struck side). */
	public static Runes lock( float x, float y, int n, Ramp ramp, float rx, float ry, float gap, float hold, float angle, float spread ){
		Runes r = make( LOCK, n, ramp, rx, ry, (Math.max( 1, n ) - 1) * gap + hold + LOCK_FADE );
		r.at( x, y );
		r.lockRing( gap, hold, angle, spread );
		return r.add();
	}

	/** The ward round a visual's middle, following it. */
	public static Runes lock( Visual target, int n, Ramp ramp, float rx, float ry, float gap, float hold ){
		Runes r = make( LOCK, n, ramp, rx, ry, (Math.max( 1, n ) - 1) * gap + hold + LOCK_FADE );
		r.follow( target, MIDDLE );
		r.lockRing( gap, hold, FxRandom.angle(), (float) (2 * Math.PI) );
		return r.add();
	}

	public static Runes lock( Visual target, int n, Ramp ramp, float rx, float ry, float gap, float hold, float angle, float spread ){
		Runes r = make( LOCK, n, ramp, rx, ry, (Math.max( 1, n ) - 1) * gap + hold + LOCK_FADE );
		r.follow( target, MIDDLE );
		r.lockRing( gap, hold, angle, spread );
		return r.add();
	}

	// ------------------------------------------------------------------ shut, scatter, rise, stamp

	/** WardFx's finisher at (x, y): n runes on an rx x ry ring closing on the point in `seconds`, a
	 *  one-frame STAR4_5 (P0) where it shuts. */
	public static Runes shut( float x, float y, int n, Ramp ramp, float rx, float ry, float seconds ){
		Runes r = make( SHUT, n, ramp, rx, ry, seconds );
		r.at( x, y );
		r.turn = RING_TURN;
		r.phase = FxRandom.angle();
		return r.add();
	}

	public static Runes shut( Visual target, int n, Ramp ramp, float rx, float ry, float seconds ){
		Runes r = make( SHUT, n, ramp, rx, ry, seconds );
		r.follow( target, MIDDLE );
		r.turn = RING_TURN;
		r.phase = FxRandom.angle();
		return r.add();
	}

	/** A ward breaking at (x, y): n runes thrown apart, each 0.6 to 1 of `dist` px, with a drag,
	 *  over `life` s. */
	public static Runes scatter( float x, float y, int n, Ramp ramp, float dist, float life ){
		Runes r = make( SCATTER, n, ramp, dist, dist, life );
		r.at( x, y );
		r.scatterOut();
		return r.add();
	}

	public static Runes scatter( Visual target, int n, Ramp ramp, float dist, float life ){
		Runes r = make( SCATTER, n, ramp, dist, dist, life );
		r.follow( target, MIDDLE );
		r.scatterOut();
		return r.add();
	}

	/** A ward's ring breaking round a visual's middle: its n runes start on a `from` x from / 2 px
	 *  ring (outside the body, where a ward's lock was seen) and are thrown out on that ellipse's
	 *  shape to 0.6 to 1 of the way on to `dist` px. */
	public static Runes scatter( Visual target, int n, Ramp ramp, float from, float dist, float life ){
		Runes r = make( SCATTER, n, ramp, dist, dist, life );
		r.follow( target, MIDDLE );
		r.squash = WARD_SQUASH;
		r.scatterOut( from );
		return r.add();
	}

	/** WardFx's finale: n runes on an rx x ry ring about (x, feetY) rising `speed` px/s for `life` s. */
	public static Runes rise( float x, float feetY, int n, Ramp ramp, float rx, float ry, float speed, float life ){
		Runes r = make( RISE, n, ramp, rx, ry, life );
		r.at( x, feetY );
		r.speed = speed;
		r.phase = FxRandom.angle();
		return r.add();
	}

	/** The same off a visual's feet (where it stood when it began: a rising ring is let go of). */
	public static Runes rise( Visual target, int n, Ramp ramp, float rx, float ry, float speed, float life ){
		Runes r = make( RISE, n, ramp, rx, ry, life );
		r.follow( target, FEET );
		r.speed = speed;
		r.phase = FxRandom.angle();
		return r.add();
	}

	/** One rune stamped at (x, y): `glyph` a RUNE_3 frame (drawn at x2) or a RUNE_5 (at x1), lit
	 *  `color`, still for `life` s, fading its last STAMP_FADE. */
	public static Runes stamp( float x, float y, int[] glyph, int color, float life ){
		Runes r = make( STAMP, 1, null, 0, 0, life );
		r.at( x, y );
		r.glyph = glyph;
		r.color = color;
		return r.add();
	}

	public static Runes stamp( Visual target, int[] glyph, int color, float life ){
		Runes r = make( STAMP, 1, null, 0, 0, life );
		r.follow( target, MIDDLE );
		r.glyph = glyph;
		r.color = color;
		return r.add();
	}

	/** One rune rising off a stream at (x, y) (vel +-SHED_SWAY, -SHED_RISE; SHED_LIFE s; its glyph
	 *  changing every GLYPH_EVERY), lit the ramp's body, snapped. P3. Any thread. */
	public static void shed( final float x, final float y, final Ramp ramp ){
		Fx.post( () -> {
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.RUNE_3, 1f / GLYPH_EVERY ).paint( SkillPalette.mid( ramp ) )
					.vel( FxRandom.Float( -SHED_SWAY, SHED_SWAY ), -SHED_RISE ).life( SHED_LIFE ).alpha( 0.85f, 0, 0.2f ).snap();
		} );
	}

	private static Runes make( int kind, int n, Ramp ramp, float rx, float ry, float life ){
		return new Runes( kind, Math.max( 1, Math.min( RUNES, n ) ), ramp, rx, ry, Math.max( 0.05f, life ), enter() );
	}

	// ------------------------------------------------------------------ the gizmo

	/**
	 * A pattern of runes (one verb's). Chained calls (spin, tighten, release, lift) take effect from
	 * the moment they are made (posted to the render thread). Let go of by itself when done; a holder
	 * may let it go early with Fx.dispose.
	 */
	public static final class Runes extends Group {

		final int kind, n;
		final Ramp ramp;
		final boolean shown;
		private boolean counted;
		private final FxPic[] pics = new FxPic[RUNES];
		private final int[] glyphs = new int[RUNES], frames = new int[RUNES];
		//each rune's angle, when it appears, how far it goes (scatter); where it was let go of
		private final float[] ang = new float[RUNES], born = new float[RUNES], far = new float[RUNES];
		private final float[] fx = new float[RUNES], fy = new float[RUNES];
		private float rx, ry, life, age;
		//where a scattered rune starts out from the middle, px, and its way's height over its width
		private float near = 3f, squash = SQUASH;
		float turn, phase, speed;
		int[] glyph;
		int color;
		private float hold;
		//where it is: a point, or a visual's feet or middle
		private float cx, cy;
		private Visual target;
		private int anchor;
		//its turning so far, its spin, its tightening
		private float rot, spinFrom, spinTo, spinAt = -1, spinFor;
		private float scale = 1f, tightFrom = 1f, tightTo = 1f, tightAt = -1, tightFor;
		//let go of: thrown out (release) or lifted (lift), from when, for how long, how fast
		private int freed = -1;
		private float freedAt, freedFor, freedSpeed;
		private boolean added, done, starred;
		//its runes keep the glyph they were born with (steady)
		private volatile boolean still;
		private float lastX0 = Float.NaN, lastY0;

		Runes( int kind, int n, Ramp ramp, float rx, float ry, float life, boolean shown ){
			this.kind = kind;
			this.n = n;
			this.ramp = ramp;
			this.rx = rx;
			this.ry = ry;
			this.life = life;
			this.shown = shown;
			counted = shown;
			for (int i = 0; i < n; i++) glyphs[i] = FxRandom.Int( FxFrames.RUNE_3.length );
		}

		void at( float x, float y ){
			cx = x;
			cy = y;
		}

		void follow( Visual v, int anchor ){
			target = v;
			this.anchor = anchor;
		}

		void lockRing( float gap, float hold, float angle, float spread ){
			this.hold = hold;
			boolean whole = spread >= 2 * Math.PI - 0.01f;
			for (int i = 0; i < n; i++){
				born[i] = gap * i;
				ang[i] = whole ? angle + (float) (2 * Math.PI * i / n)
						: (n == 1 ? angle : angle - spread / 2f + spread * i / (n - 1));
			}
		}

		void scatterOut(){
			float a0 = FxRandom.angle();
			for (int i = 0; i < n; i++){
				ang[i] = a0 + (float) (2 * Math.PI * i / n) + FxRandom.Float( -0.3f, 0.3f );
				far[i] = rx * FxRandom.Float( 0.6f, 1f );
			}
		}

		void scatterOut( float from ){
			near = from;
			float a0 = FxRandom.angle();
			for (int i = 0; i < n; i++){
				ang[i] = a0 + (float) (2 * Math.PI * i / n) + FxRandom.Float( -0.3f, 0.3f );
				far[i] = from + (rx - from) * FxRandom.Float( 0.6f, 1f );
			}
		}

		//on the render thread: into the light layer (a shown one), counted till it goes
		Runes add(){
			if (!shown) return this;
			Fx.post( () -> {
				if (GameScene.layer( GameScene.Layer.LIGHTS ) == null || done || target != null && target.parent == null){
					done = true;
					let();
					return;
				}
				//where it begins, read here (a rising ring keeps to it)
				if (target != null) where();
				GameScene.light( this );
				added = true;
			} );
			return this;
		}

		/** Its runes keep the glyph each was born with: no change of glyph while they hold (a pattern
		 *  that must never flicker: Sanctuary's). Any thread. */
		public Runes steady(){
			still = true;
			return this;
		}

		/** Spins up (or down) from its turn now to `toTurn` rad/s over `over` s. */
		public Runes spin( final float toTurn, final float over ){
			if (shown) Fx.post( () -> {
				spinFrom = turnNow();
				spinTo = toTurn;
				spinAt = age;
				spinFor = Math.max( 0.01f, over );
			} );
			return this;
		}

		/** Its ellipse shrinks (or grows) to `to` of its size over `over` s. */
		public Runes tighten( final float to, final float over ){
			if (shown) Fx.post( () -> {
				tightFrom = scale;
				tightTo = to;
				tightAt = age;
				tightFor = Math.max( 0.01f, over );
			} );
			return this;
		}

		/** Its runes thrown out along their angles at `speed` px/s (a drag of RELEASE_DRAG), fading
		 *  over `life` s; then it is gone. */
		public Runes release( final float speed, final float life ){
			if (shown) Fx.post( () -> free( 0, speed, life ) );
			return this;
		}

		/** Its runes leaving their places rising at `vy` px/s (negative up), fading over `life` s;
		 *  then it is gone (a rune still to appear never does). */
		public Runes lift( final float vy, final float life ){
			if (shown) Fx.post( () -> free( 1, vy, life ) );
			return this;
		}

		private void free( int how, float speed, float life ){
			if (done || freed >= 0) return;
			//each rune let go of where it is now
			centre();
			for (int i = 0; i < n; i++){
				fx[i] = runeX( i );
				fy[i] = runeY( i );
			}
			freed = how;
			freedAt = age;
			freedFor = Math.max( 0.05f, life );
			freedSpeed = speed;
		}

		//its turn now, spin taken in
		private float turnNow(){
			if (spinAt < 0) return turn;
			float k = Math.min( 1f, (age - spinAt) / spinFor );
			return spinFrom + (spinTo - spinFrom) * FxCurves.smooth( k );
		}

		private void where(){
			Visual v = target;
			if (v instanceof CharSprite){
				CharSprite s = (CharSprite) v;
				cx = SkillVfx.midX( s );
				cy = anchor == FEET ? s.y + s.height() - 2 : SkillVfx.midY( s );
			} else {
				cx = v.x + v.width() / 2f;
				cy = anchor == FEET ? v.y + v.height() - 2 : v.y + v.height() / 2f;
			}
		}

		//its middle now: the target's, or its point with any slide of the window taken in
		private void centre(){
			if (target != null && target.exists && kind != RISE){
				where();
				return;
			}
			FxPic p0 = pics[0];
			if (p0 != null && !Float.isNaN( lastX0 ) && (p0.x != lastX0 || p0.y != lastY0)){
				cx += p0.x - lastX0;
				cy += p0.y - lastY0;
				for (int i = 0; i < n; i++){
					fx[i] += p0.x - lastX0;
					fy[i] += p0.y - lastY0;
				}
			}
		}

		private float runeAngle( int i ){
			return kind == LOCK ? ang[i] : phase + (float) (2 * Math.PI * i / n) + rot;
		}

		private float runeX( int i ){
			if (kind == SCATTER) return cx + (float) Math.cos( ang[i] ) * travel( i );
			if (kind == STAMP) return cx;
			return cx + (float) Math.cos( runeAngle( i ) ) * rx * scale * closing();
		}

		private float runeY( int i ){
			if (kind == SCATTER) return cy + (float) Math.sin( ang[i] ) * travel( i ) * squash;
			if (kind == STAMP) return cy;
			float y = cy + (float) Math.sin( runeAngle( i ) ) * ry * scale * closing();
			return kind == RISE ? y - speed * age - 2 : y;
		}

		//a shut ring's share of its size
		private float closing(){
			return kind == SHUT ? 1f - FxCurves.easeIn( Math.min( 1f, age / life ) ) : 1f;
		}

		//a scattered rune's way out so far: its whole way by its end, slowing (drag)
		private float travel( int i ){
			float k = (float) ((1 - Math.exp( -SCATTER_DRAG * age )) / (1 - Math.exp( -SCATTER_DRAG * life )));
			return near + (far[i] - near) * Math.min( 1f, k );
		}

		@Override
		public void update(){
			super.update();
			if (done) return;
			float dt = Game.elapsed;
			age += dt;
			if (pics[0] == null) build();
			centre();
			rot += turnNow() * dt;
			if (tightAt >= 0) scale = tightFrom + (tightTo - tightFrom) * FxCurves.smooth( Math.min( 1f, (age - tightAt) / tightFor ) );

			float end = freed >= 0 ? freedAt + freedFor : life;
			if (age >= end){
				if (kind == SHUT && !starred) star();
				finish();
				return;
			}
			int glyphStep = still ? 0 : (int) (age / GLYPH_EVERY);
			for (int i = 0; i < n; i++) pose( i, glyphStep );
			FxPic p0 = pics[0];
			lastX0 = p0.x;
			lastY0 = p0.y;
		}

		private void build(){
			for (int i = 0; i < n; i++){
				FxPic p = new FxPic( SkillVfx.SHEET );
				p.additive = true;
				p.visible = false;
				pics[i] = p;
				add( p );
			}
		}

		private void pose( int i, int glyphStep ){
			FxPic p = pics[i];
			float x, y, a;
			int c;
			if (freed >= 0){
				float t = age - freedAt, k = t / freedFor;
				if (freed == 0){
					float d = (float) (freedSpeed * (1 - Math.exp( -RELEASE_DRAG * t )) / RELEASE_DRAG);
					float an = runeAngleAtFree( i );
					x = fx[i] + (float) Math.cos( an ) * d;
					y = fy[i] + (float) Math.sin( an ) * d * 0.6f;
				} else {
					x = fx[i];
					y = fy[i] + freedSpeed * t;
				}
				a = (1f - k) * baseAlpha( i, freedAt );
				c = tone( 1f );
				if (kind == LOCK && freedAt < born[i]) a = 0;
			} else {
				x = runeX( i );
				y = runeY( i );
				a = baseAlpha( i, age );
				c = baseColor( i );
			}
			if (a <= 0.004f){
				p.visible = false;
				return;
			}
			int[] f = kind == STAMP ? glyph : FxFrames.RUNE_3[(glyphs[i] + glyphStep) % FxFrames.RUNE_3.length];
			p.show( f );
			p.lit( c );
			p.am = Math.min( 1f, a );
			int sc = kind == STAMP && f[2] <= 3 ? 2 : 1;
			p.scale.set( sc );
			p.x = Math.round( x - f[2] * sc / 2f );
			p.y = Math.round( y - f[3] * sc / 2f );
			p.visible = true;
		}

		//a rune's angle when it was let go of (a ring's turned with it; a lock's, a rise's their own)
		private float runeAngleAtFree( int i ){
			if (kind == SCATTER) return ang[i];
			return runeAngle( i );
		}

		//its alpha by its kind at `t` s
		private float baseAlpha( int i, float t ){
			switch (kind){
				case RING: {
					float env = Math.min( 1f, t / RING_IN ) * Math.min( 1f, Math.max( 0f, (life - t) / RING_OUT ) );
					return env * (Math.sin( runeAngle( i ) ) < 0 ? BACK : 1f);
				}
				case LOCK: {
					if (t < born[i]) return 0;
					frames[i]++;
					if (frames[i] <= SNAP_FRAMES) return 1f;
					float left = born[i] + hold + LOCK_FADE - t;
					return LOCK_ALPHA * Math.min( 1f, Math.max( 0f, left / LOCK_FADE ) );
				}
				case SHUT:
					return 1f;
				case SCATTER:
					return t < life * 0.4f ? 1f : Math.max( 0f, (life - t) / (life * 0.6f) );
				case RISE: {
					float env = Math.min( 1f, t / 0.05f ) * Math.min( 1f, Math.max( 0f, (life - t) / 0.3f ) );
					return 0.95f * env * (Math.sin( runeAngle( i ) ) < 0 ? BACK : 1f);
				}
				default:
					return Math.min( 1f, Math.max( 0f, (life - t) / STAMP_FADE ) );
			}
		}

		//its colour: the flash as a rune snaps in or a ring shuts, its body while it holds, its deep
		//on a ring's far side
		private int baseColor( int i ){
			switch (kind){
				case RING: case RISE:
					return Math.sin( runeAngle( i ) ) < 0 ? tone( 1f ) : tone( 0.5f );
				case LOCK:
					return frames[i] <= SNAP_FRAMES ? tone( 0f ) : tone( 0.5f );
				case SHUT:
					return tone( 0.5f * (1f - Math.min( 1f, age / life )) );
				case SCATTER:
					return frames[i]++ < SNAP_FRAMES ? tone( 0f ) : tone( 0.5f + 0.5f * Math.min( 1f, age / life ) );
				default:
					return color;
			}
		}

		//a colour along its ramp: 0 its flash, 0.5 its body, 1 its deep
		private int tone( float p ){
			if (ramp == null) return color;
			return ramp.at( p );
		}

		//where a shut ring closes: one frame of a star (P0)
		private void star(){
			starred = true;
			FxParticle p = SkillVfx.glow( cx, cy, FxBudget.P0 );
			if (p != null) p.look( FxFrames.STAR4_5 ).paint( tone( 0f ) ).life( STAR_LIFE ).alpha( 1f, 0, 0.01f ).snap();
		}

		private void finish(){
			done = true;
			Fx.dispose( this );
			let();
		}

		private synchronized void let(){
			if (counted){
				counted = false;
				leave();
			}
		}

		@Override
		public void kill(){
			super.kill();
			done = true;
			let();
		}

		/** Whether it shows (false past CAP). */
		public boolean shown(){
			return shown;
		}
	}
}
