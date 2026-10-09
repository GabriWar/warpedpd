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
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

/**
 * Motes circling a character, one orbit for every system that circles (a stance's aura, spirit
 * armour, a thief's coins, an elemental orbit, a mark, a skill sequence, feathers, verdant leaves,
 * a cast's spiral): a 2:1 ellipse of the slot's size, its far half drawn behind the sprite and its
 * near half in front (right before and right after the sprite draws: CharSprite.draw, so it sits
 * between the sprites behind and in front of its own), trailing a little behind it as it moves
 * (its offset -velocity x 0.05 s, easing back at 12 a second), all computed from the sprite each
 * frame - so a window's slide carries it too - and nothing allocated once made. An orbit is one
 * picture drawn once for every mote and trail.
 *
 *     FxOrbit.attach( sprite, FxOrbit.Slot.STANCE ).motes( 4, FxFrames.MOTE_2, 0xFFD060, true )
 *         .trail( 1 ).breath( 0.8f, 1f, 0.5f );
 *
 * More than three orbits on one sprite and the outer ones slow to 60% and dim to 70%. At most
 * MOST quads an orbit, trails counted. A sprite's orbits live in its fxSlots[FxModules.KIT] and
 * are let go of when it is killed (FxModules.gone).
 */
public final class FxOrbit {

	private FxOrbit(){}

	/** The quads an orbit may draw, its motes and their trails together. */
	public static final int MOST = 6;
	/** Its lag behind a moving sprite (s of its velocity) and how fast it eases back (/s). */
	public static final float LAG = 0.05f, EASE = 12f;

	/** Where an orbit circles: its ellipse's half-width and half-height, and its height off the
	 *  sprite's middle (negative is up). */
	public enum Slot {
		STANCE( 9f, 3.5f, 0f ),
		SPIRIT_ARMOR( 10f, 5f, -2f ),
		COINS( 12f, 5f, 2f ),
		ELEMENTAL( 13f, 6f, -6f ),
		MARK( 6f, 2.5f, -14f ),
		SEQUENCE( 7f, 3f, -4f ),
		FEATHERS( 8f, 4f, 4f ),
		VERDANT( 11f, 4.5f, -1f ),
		//a cast's spiral, opening from 4 px to 8 and again
		CAST( 4f, 2f, 0f );

		public final float rx, ry, h;

		Slot( float rx, float ry, float h ){
			this.rx = rx;
			this.ry = ry;
			this.h = h;
		}
	}

	//a sprite's orbits, where it was (for its lag) and whether its far halves were just drawn
	static final class Orbits {
		final Ring[] rings = new Ring[Slot.values().length];
		float lastX = Float.NaN, lastY, lagX, lagY;
		boolean placed;
	}

	private static Orbits of( CharSprite s, boolean make ){
		Object o = s.fxSlots[FxModules.KIT];
		if (o == null && make){
			synchronized (s.fxSlots){
				o = s.fxSlots[FxModules.KIT];
				if (o == null) s.fxSlots[FxModules.KIT] = o = new Orbits();
			}
		}
		return (Orbits) o;
	}

	/** The sprite's orbit in this slot, made if it has none yet. Any thread. */
	public static Ring attach( CharSprite s, Slot slot ){
		Orbits o = of( s, true );
		synchronized (o){
			Ring r = o.rings[slot.ordinal()];
			if (r == null || r.detached){
				r = new Ring( s, slot );
				o.rings[slot.ordinal()] = r;
			}
			return r;
		}
	}

	/** The sprite's orbit in this slot, null for none. */
	public static Ring get( CharSprite s, Slot slot ){
		Orbits o = of( s, false );
		if (o == null) return null;
		synchronized (o){
			Ring r = o.rings[slot.ordinal()];
			return r == null || r.detached ? null : r;
		}
	}

	/** How many orbits circle the sprite. */
	public static int count( CharSprite s ){
		Orbits o = of( s, false );
		if (o == null) return 0;
		int n = 0;
		synchronized (o){
			for (Ring r : o.rings) if (r != null && !r.detached && r.count > 0) n++;
		}
		return n;
	}

	/** Every orbit of the sprite let go of: it was killed. Any thread. */
	static void release( CharSprite s ){
		Orbits o = of( s, false );
		if (o == null) return;
		synchronized (o){
			for (Ring r : o.rings) if (r != null) r.detach();
		}
	}

	/** An orbit of motes about a sprite. */
	public static final class Ring {

		final CharSprite sprite;
		final Slot slot;
		//its one picture, drawn once for every mote and trail
		private Image image;
		private final RectF uv = new RectF();
		private int count, trail;
		private boolean light;
		private float lo = 1, hi = 1, hz;
		private float boost, flareFor, flareLeft;
		private int extra;
		private float speed = 2.4f, phase, age;
		volatile boolean detached;
		//its share of speed and brightness, by how many orbits lie inside it
		private float pace = 1, dim = 1;

		Ring( CharSprite sprite, Slot slot ){
			this.sprite = sprite;
			this.slot = slot;
			phase = FxRandom.angle();
		}

		/** n motes of a frame (FxFrames) in a colour, as light (added) or matter. */
		public synchronized Ring motes( int n, int[] frame, int color, boolean light ){
			if (detached) return this;
			this.light = light;
			count = Math.max( 0, n );
			if (image == null) image = new Image( Assets.Effects.FX_SHEET );
			float tw = image.texture.width, th = image.texture.height;
			uv.set( frame[0] / tw, frame[1] / th, (frame[0] + frame[2]) / tw, (frame[1] + frame[3]) / th );
			image.frame( uv );
			image.origin.set( frame[2] / 2f, frame[3] / 2f );
			image.resetColor();
			image.hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
			return this;
		}

		/** Each mote trailed by `steps` fainter copies of it (at 0.5 then 0.25). */
		public synchronized Ring trail( int steps ){
			trail = Math.max( 0, Math.min( 2, steps ) );
			return this;
		}

		/** Its brightness breathing between lo and hi at hz. */
		public synchronized Ring breath( float lo, float hi, float hz ){
			this.lo = lo;
			this.hi = hi;
			this.hz = hz;
			return this;
		}

		/** Its angular speed, rad/s (2.4 unless set). */
		public synchronized Ring speed( float radPerSecond ){
			speed = radPerSecond;
			return this;
		}

		/** Flares up: brighter by `boost` (and a little wider), `extraMotes` more for a while,
		 *  easing back over `seconds`. */
		public synchronized Ring flare( float boost, float seconds, int extraMotes ){
			this.boost = boost;
			flareFor = flareLeft = Math.max( 0.01f, seconds );
			extra = Math.max( 0, extraMotes );
			return this;
		}

		/** Off the sprite: its picture let go of (its buffer freed on the render thread). Any thread. */
		public void detach(){
			detached = true;
			Orbits o = of( sprite, false );
			if (o != null){
				synchronized (o){
					if (o.rings[slot.ordinal()] == this) o.rings[slot.ordinal()] = null;
				}
			}
			final Image gone;
			synchronized (this){
				gone = image;
				image = null;
				count = 0;
			}
			if (gone != null) Fx.post( gone::destroy );
		}

		public boolean isDetached(){
			return detached;
		}

		//the frame's brightness, flare and size, once a frame (before the far half is drawn)
		private float breathNow, flareNow, rx, ry;

		synchronized void step( float dt ){
			age += dt;
			phase += speed * pace * dt;
			breathNow = hz > 0 ? lo + (hi - lo) * (0.5f + 0.5f * (float)Math.sin( 2 * Math.PI * hz * age )) : hi;
			if (flareLeft > 0){
				flareLeft -= dt;
				if (flareLeft <= 0){
					flareLeft = 0;
					extra = 0;
				}
			}
			flareNow = flareFor > 0 ? boost * Math.max( 0f, flareLeft / flareFor ) : 0f;
			if (slot == Slot.CAST){
				float open = (age * 1.5f) % 1f;
				rx = 4f + 4f * open;
				ry = rx / 2f;
			} else {
				rx = slot.rx * (1f + 0.2f * flareNow);
				ry = slot.ry * (1f + 0.2f * flareNow);
			}
		}

		//its motes on the far (behind) or near side about (cx, cy)
		synchronized void draw( float cx, float cy, boolean far ){
			if (image == null) return;
			int shown = Math.min( count + (flareLeft > 0 ? extra : 0), MOST / (1 + trail) );
			if (shown <= 0) return;
			float a = Math.min( 1f, breathNow * (1f + flareNow) ) * dim;
			image.camera = sprite.camera();
			if (light) Blending.setLightMode();
			for (int i = 0; i < shown; i++){
				for (int k = trail; k >= 0; k--){
					float t = phase + (float)(2 * Math.PI * i / shown) - k * 0.35f;
					float s = (float)Math.sin( t );
					if ((s < 0) != far) continue;
					image.x = Math.round( cx + rx * (float)Math.cos( t ) - image.origin.x );
					image.y = Math.round( cy + slot.h + ry * s - image.origin.y );
					image.am = a * (k == 0 ? 1f : k == 1 ? 0.5f : 0.25f);
					image.draw();
				}
			}
			if (light) Blending.setNormalMode();
		}
	}

	//the sprite's middle with its lag, into at[]
	private static void place( CharSprite s, Orbits o, float[] at ){
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		float dt = Game.elapsed;
		if (Float.isNaN( o.lastX ) || dt <= 0){
			o.lastX = cx;
			o.lastY = cy;
		}
		float dx = cx - o.lastX, dy = cy - o.lastY;
		//a jump of more than a cell and a half is a slide or a teleport, not a move
		if (Math.abs( dx ) > 24 || Math.abs( dy ) > 24){
			dx = dy = 0;
		}
		if (dt > 0){
			float tx = -dx / dt * LAG, ty = -dy / dt * LAG;
			float k = Math.min( 1f, EASE * dt );
			o.lagX += (tx - o.lagX) * k;
			o.lagY += (ty - o.lagY) * k;
		}
		o.lastX = cx;
		o.lastY = cy;
		at[0] = cx + o.lagX;
		at[1] = cy + o.lagY;
	}

	private static final float[] AT = new float[2];

	/** Right before the sprite draws: every orbit stepped and its far half drawn. Render thread. */
	static void drawBehind( CharSprite s ){
		Orbits o = of( s, false );
		if (o == null || !s.visible) return;
		synchronized (o){
			o.placed = false;
			int active = 0;
			for (Ring r : o.rings) if (r != null && !r.detached && r.count > 0) active++;
			if (active == 0) return;
			place( s, o, AT );
			for (Ring r : o.rings){
				if (r == null || r.detached) continue;
				//more than three: the outer ones slower and dimmer
				int inside = 0;
				for (Ring q : o.rings) if (q != null && q != r && !q.detached && q.count > 0 && q.slot.rx < r.slot.rx) inside++;
				boolean outer = active > 3 && inside >= 3;
				r.pace = outer ? 0.6f : 1f;
				r.dim = outer ? 0.7f : 1f;
				r.step( Game.elapsed );
				r.draw( AT[0], AT[1], true );
			}
			o.placed = true;
		}
	}

	/** Right after the sprite draws: every orbit's near half, about where its far half was. Render thread. */
	static void drawInFront( CharSprite s ){
		Orbits o = of( s, false );
		if (o == null) return;
		synchronized (o){
			if (!o.placed) return;
			o.placed = false;
			for (Ring r : o.rings){
				if (r != null && !r.detached) r.draw( AT[0], AT[1], false );
			}
		}
	}
}
