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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.FishBrain;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterAmbience;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterGeom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterLive;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

/**
 * The small life of the world's slices (levels/overworld/CaveLife, PeakLife): bats hanging in
 * the caves and fluttering off, a blind fish gliding under a cave pool, an eagle circling over
 * the peaks, a marmot by its boulder. Pictures only, like the surface's critters
 * (CritterSprite): no Actor, nothing saved, nothing to fight - and drawn smaller and paler
 * than the creatures that are (the red-eyed Bat, the eyrie's Eagle), so nobody mistakes one
 * for the other. effects/layer_critters.png, tools/layer_life_gen.py.
 */
public final class SliceCritterSprite {

	private SliceCritterSprite(){}

	static final TextureFilm FILM = new TextureFilm( 128, 96, 16, 16 );
	static final int ROW_BAT = 0, ROW_EAGLE = 1, ROW_MARMOT = 2, ROW_FX = 3;

	/**
	 * A cave bat: hangs from the rock face in a colony, shifts its wings now and then (more
	 * often when the hero is near), and when something comes close drops off the rock and
	 * flutters away on an erratic course, gone into the dark in a second or two. A lone one
	 * may flit across a chamber.
	 */
	public static class Bat extends CritterSprite {

		private static final int HANG = 0, TWITCH = 1, DROP = 2;
		private static final int[] FLAP = { 3, 4, 5, 4 };
		private static final int HANGING = 0, DROPPING = 1, FLYING = 2;

		//its sight is the floor cell under the face it hangs on: this far below its claws
		private final float seenDY;
		private int state = HANGING;
		private float t, idle, twitch, heading, speed, vmax, jitterIn, span, baseZ;
		private boolean wary, flitting;

		public Bat( float ax, float ay, float seenDY ){
			super( Assets.Effects.LAYER_CRITTERS, FILM, ax, ay );
			this.seenDY = seenDY;
			origin.set( 8, 1 );
			pose( ROW_BAT, HANG );
			idle = rf( 2f, 6f );
		}

		/** A bat flitting through a chamber on its own: out of the dark and back into it. */
		public static Bat flitter( float ax, float ay, float z, float heading ){
			Bat b = new Bat( ax, ay, 0f );
			b.state = FLYING;
			b.flitting = true;
			b.heading = heading;
			b.vmax = b.speed = rf( 40f, 55f );
			b.z = b.baseZ = z;
			b.span = rf( 2.5f, 4f );
			b.jitterIn = rf( 0.12f, 0.25f );
			b.shown = 0f;
			return b;
		}

		/** Made out where it was hanging all along, over 0.8s. */
		public void appear(){
			shown = 0f;
		}

		/** Still on the rock: something coming near can startle it. */
		public boolean hanging(){
			return state == HANGING;
		}

		public void wary( boolean on ){
			wary = on;
		}

		/** Lets go of the rock after `delay` and flutters off along `heading`. */
		public void drop( float heading, float delay ){
			if (state != HANGING) return;
			//not yet made out: it simply is not there
			if (shown <= 0f){
				vanish();
				return;
			}
			state = DROPPING;
			this.heading = heading;
			t = -delay;
		}

		@Override
		protected float seenX(){
			return state == FLYING ? super.seenX() : ax;
		}

		@Override
		protected float seenY(){
			return state == FLYING ? super.seenY() : ay + seenDY;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			if (state == HANGING){
				shown = Math.min( 1f, shown + dt / 0.8f );
				if (twitch > 0f){
					twitch -= dt;
					pose( ROW_BAT, TWITCH );
				} else {
					pose( ROW_BAT, HANG );
					if ((idle -= dt) <= 0f){
						twitch = 0.25f;
						idle = wary ? rf( 0.6f, 1.4f ) : rf( 2f, 6f );
					}
				}
			} else if (state == DROPPING){
				if (t < 0f){
					pose( ROW_BAT, wary ? TWITCH : HANG );
					return;
				}
				float p = Math.min( 1f, t / 0.15f );
				pose( ROW_BAT, DROP );
				oy = 5f * p;
				if (p >= 1f){
					state = FLYING;
					t = 0f;
					speed = 55f;
					vmax = rf( 70f, 85f );
					jitterIn = rf( 0.12f, 0.25f );
				}
			} else {
				fly( dt );
			}
		}

		private void fly( float dt ){
			speed = Math.min( vmax, speed + 120f * dt );
			//the bat's way of flying: a new heading every fraction of a second
			if ((jitterIn -= dt) <= 0f){
				jitterIn = rf( 0.12f, 0.25f );
				heading += rf( -0.9f, 0.9f );
			}
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			pose( ROW_BAT, FLAP[(int)(t * 14f) & 3] );
			if (flitting){
				z = baseZ + (float)Math.sin( t * 5f ) * 1.5f;
				shown = Math.min( Math.min( 1f, t / 0.3f ), Math.max( 0f, (span - t) / 0.5f ) );
				if (t >= span) vanish();
			} else {
				z += 4f * dt;
				shown = t < 1.2f ? 1f : Math.max( 0f, 1f - (t - 1.2f) / 0.7f );
				if (t >= 1.9f) vanish();
			}
		}
	}

	/**
	 * An eagle high over the peaks: it circles slowly over the hero's part of the mountain, its
	 * shadow passing over the ground below, beats its wings now and then, and in the end
	 * glides off or folds and stoops out of sight. In the sky, never anything to fight (the
	 * eyries' Eagle is).
	 */
	public static class Eagle extends CritterSprite {

		private static final int SOAR = 0, BANK = 1, FLAP_UP = 2, FLAP_DOWN = 3, STOOP = 4;
		private static final int CIRCLING = 0, GLIDING = 1, STOOPING = 2;

		/** Its shadow on the ground: added to the effects group under the rest by PeakLife. */
		public final Image shadow;
		private final float radius, omega, height, span;
		private int state = CIRCLING;
		private float theta, heading, flapIn, flapT = -1f, t, speed;

		public Eagle( float ax, float ay ){
			super( Assets.Effects.LAYER_CRITTERS, FILM, ax, ay );
			origin.set( 8, 8 );
			pose( ROW_EAGLE, BANK );
			radius = rf( 44f, 64f );
			omega = (RNG.nextBoolean() ? 1f : -1f) * rf( 0.32f, 0.45f );
			theta = rf( 0f, 6.2832f );
			height = z = rf( 76f, 92f );
			span = rf( 25f, 45f );
			flapIn = rf( 2f, 6f );
			shown = 0f;
			shadow = new Image( Assets.Effects.LAYER_CRITTERS );
			shadow.frame( FILM.get( ROW_EAGLE * 8 + SOAR ) );
			shadow.origin.set( 8, 8 );
			shadow.hardlight( 0x000000 );
			shadow.alpha( 0 );
			circle( 0f );
		}

		/** The circle's centre follows the hero, slowly: it keeps over his part of the mountain. */
		public void drift( float hx, float hy, float dt ){
			if (state != CIRCLING) return;
			float k = Math.min( 1f, dt * 0.06f );
			ax += (hx - ax) * k;
			ay += (hy - ay) * k;
			if (Math.max( Math.abs( hx - ax ), Math.abs( hy - ay ) ) > 240f) depart();
		}

		/** Away for good: a long glide off along its circle, or folded wings and a stoop out of sight. */
		public void depart(){
			if (state != CIRCLING) return;
			t = 0f;
			if (RNG.nextFloat() < 0.6f){
				state = GLIDING;
				speed = 40f;
				pose( ROW_EAGLE, SOAR );
			} else {
				state = STOOPING;
				pose( ROW_EAGLE, STOOP );
			}
		}

		@Override
		protected boolean hidesOutOfSight(){
			return false;
		}

		@Override
		public void vanish(){
			shadow.killAndErase();
			shadow.destroy();
			super.vanish();
		}

		private void circle( float dt ){
			theta += omega * dt;
			ox = radius * (float)Math.cos( theta );
			oy = radius * (float)Math.sin( theta ) * 0.8f;
			//the tangent of its circle, the way it flies round
			heading = (float)Math.atan2( Math.cos( theta ) * 0.8f * Math.signum( omega ), -Math.sin( theta ) * Math.signum( omega ) );
		}

		@Override
		protected void act( float dt ){
			t += dt;
			if (state == CIRCLING){
				circle( dt );
				shown = Math.min( 1f, shown + dt / 2f );
				if (flapT >= 0f){
					flapT += dt;
					//three beats at five a second
					pose( ROW_EAGLE, ((int)(flapT * 10f) & 1) == 0 ? FLAP_UP : FLAP_DOWN );
					if (flapT >= 0.6f){
						flapT = -1f;
						flapIn = rf( 6f, 12f );
					}
				} else {
					pose( ROW_EAGLE, BANK );
					if ((flapIn -= dt) <= 0f) flapT = 0f;
				}
				//the bank frame is a left turn (its left wing the inner one, foreshortened): a
				//clockwise circle on screen is a right turn, mirrored
				face( omega > 0 );
				if (life >= span) depart();
			} else if (state == GLIDING){
				speed = Math.min( 70f, speed + 30f * dt );
				ox += (float)Math.cos( heading ) * speed * dt;
				oy += (float)Math.sin( heading ) * speed * dt;
				shown = Math.max( 0f, shown - dt / 3f );
				if (shown <= 0f){
					vanish();
					return;
				}
			} else {
				ox += (float)Math.cos( heading ) * 90f * dt;
				oy += (float)Math.sin( heading ) * 90f * dt;
				z = Math.max( 0f, height * (1f - t / 1.4f) );
				if (t > 1.0f) shown = Math.max( 0f, 1f - (t - 1.0f) / 0.4f );
				if (t >= 1.4f){
					vanish();
					return;
				}
			}
			angle = (float)Math.toDegrees( heading ) + 90f;
			//the shadow: straight below, faint, a little stronger as the bird comes down
			shadow.frame( frame );
			shadow.flipHorizontal = flipHorizontal;
			shadow.angle = angle;
			shadow.x = gx() - 8;
			shadow.y = gy() - 8;
			shadow.alpha( shown * (z < 20f ? 0.35f : 0.18f) );
			shadow.visible = WeatherSprites.visible( gx(), gy() );
		}
	}

	/**
	 * A marmot by its boulder on the alpine meadow: it pops up beside the rock, sits up and
	 * looks about or forages, whistles when the hero comes near (a little call drawn by its
	 * head), and when he comes nearer it dives into its burrow under the rock.
	 */
	public static class Marmot extends CritterSprite {

		private static final int PEEK = 0, STAND = 1, WHISTLE = 2, FORAGE = 3, DIVE = 4;
		private static final int PEEKING = 0, UP = 1, HOPPING = 2, DIVING = 3;

		//the unit step to its boulder, where the burrow is
		private final int bx, by;
		private int state = PEEKING;
		private float t, idle, whistleT = -1f, rewhistle;
		private boolean foraging, alert;
		private Image mark;

		public Marmot( float ax, float ay, int bx, int by ){
			super( Assets.Effects.LAYER_CRITTERS, FILM, ax, ay );
			this.bx = bx;
			this.by = by;
			origin.set( 8, 14 );
			pose( ROW_MARMOT, PEEK );
			face( bx > 0 );
			idle = rf( 1.5f, 3f );
		}

		/** Pops its head up out of the burrow, then sits up. */
		public void peek(){
			state = PEEKING;
			t = 0f;
			shown = 0f;
		}

		/** Above ground: something coming near can still send it down. */
		public boolean up(){
			return state == PEEKING || state == UP;
		}

		/** Sits up and whistles, facing (px), while the hero is near. */
		public void alert( boolean on, float px ){
			if (on && !alert) rewhistle = 0f;
			alert = on;
			if (on) face( px < gx() );
		}

		/** Down into the burrow by its boulder: a hop toward the rock and gone. */
		public void dive(){
			if (!up()) return;
			state = HOPPING;
			t = 0f;
			whistleT = -1f;
			//head first into the burrow under its rock
			if (bx != 0) face( bx < 0 );
			if (mark != null) mark.visible = false;
		}

		@Override
		public void vanish(){
			if (mark != null){
				mark.killAndErase();
				mark.destroy();
			}
			super.vanish();
		}

		@Override
		protected void act( float dt ){
			t += dt;
			switch (state){
				case PEEKING:
					pose( ROW_MARMOT, PEEK );
					shown = Math.min( 1f, t / 0.6f );
					if (t >= 0.6f){
						state = UP;
						shown = 1f;
					}
					break;
				case UP:
					up( dt );
					break;
				case HOPPING:
					float p = Math.min( 1f, t / 0.2f );
					ox = bx * 6f * p;
					oy = by * 6f * p;
					z = (float)Math.sin( p * Math.PI ) * 3f;
					pose( ROW_MARMOT, STAND );
					if (p >= 1f){
						state = DIVING;
						t = 0f;
						z = 0f;
					}
					break;
				default:
					float q = Math.min( 1f, t / 0.25f );
					pose( ROW_MARMOT, DIVE );
					oy = by * 6f + 4f * q;
					shown = 1f - q;
					if (q >= 1f) vanish();
			}
		}

		private void up( float dt ){
			if (alert){
				foraging = false;
				if ((rewhistle -= dt) <= 0f){
					rewhistle = 2.5f;
					whistleT = 0f;
				}
			}
			if (whistleT >= 0f){
				whistleT += dt;
				pose( ROW_MARMOT, WHISTLE );
				showMark();
				if (whistleT >= 0.6f){
					whistleT = -1f;
					mark.visible = false;
				}
				return;
			}
			if (alert){
				pose( ROW_MARMOT, STAND );
				return;
			}
			pose( ROW_MARMOT, foraging ? FORAGE : STAND );
			if ((idle -= dt) <= 0f){
				foraging = !foraging;
				idle = foraging ? rf( 2f, 4f ) : rf( 1.5f, 3f );
			}
		}

		//the call, by its head: two arcs, then three
		private void showMark(){
			if (mark == null){
				mark = new Image( Assets.Effects.LAYER_CRITTERS );
				GameScene.effect( mark );
				if (mark.parent != null) mark.parent.bringToFront( mark );
			}
			mark.frame( FILM.get( ROW_FX * 8 + (((int)(whistleT / 0.15f) & 1) == 0 ? 6 : 7) ) );
			mark.flipHorizontal = flipHorizontal;
			mark.x = flipHorizontal ? gx() - 17f : gx() + 1f;
			mark.y = gy() - 20f;
			mark.alpha( shown );
			mark.visible = WeatherSprites.visible( gx(), gy() - 1f );
		}
	}

	/**
	 * A fish gliding just under a pool's surface - the caves' blind fish, the dungeon's - seen
	 * through its surface, under its sheens (DungeonLife and CaveLife put it in the scene's
	 * subsurface layer): on dark water (the caves', the city's) a pale ghost in the water's glint at
	 * 0.45, 0.4 at night; on lighter water a shadow of its deep (Element.Liquid.deep) at 0.55, 0.45
	 * at night (FishBrain.shade). Level or tilted 20 degrees, never between. It turns back from the bank (it looks 8 px ahead
	 * ten times a second), rings the water small at its own point now and then (once in 4 s in a
	 * light rain, rising to it), and sinks out of sight.
	 *
	 * It lives as fish do (effects/fx/water/FishBrain): startled - by a stir of the water within
	 * its reach, the hero within 32 px, anyone else within 24 - it turns away and bursts off at
	 * 50-60 px/s, its tail beating at 14 a second, slowing as exp(-2.5 t), a swirl of a ring where
	 * it turned; then dives out of sight over 0.6 s (one time in two) or glides to its pace again.
	 * Food on the water within 40 px draws it at half again its pace; at the food, three times in
	 * ten it kisses the surface (a small ring, a bubble rising and popping), at most once in 3 s.
	 * Its water frozen under it, it is gone in 0.4 s. Its rings stir nothing (WaterFX.quietRing).
	 */
	public static class Swimmer extends CritterSprite {

		private static final int SWIM = 0, FLEE = 1, CURIOUS = 2, DIVE = 3;
		static final float ALPHA = 0.55f, ALPHA_NIGHT = 0.45f, PALE = 0.45f, PALE_NIGHT = 0.4f, DIVE_TIME = 0.6f, KISS_GAP = 3f, KISS_SHARE = 0.3f;
		static final float RISE_EVERY = 4f, CURIOUS_PACE = 1.5f, TAIL_FPS = 6f, BURST_TAIL_FPS = 14f;

		private float heading, pace, span, lookIn, ringIn, riseIn, speed, burstT, kissedAt = -KISS_GAP, diveT;
		private float foodX, foodY, v0;
		private int state = SWIM;
		private boolean glides;
		private final boolean pale;

		public Swimmer( float ax, float ay, float heading ){
			super( ax, ay );
			this.heading = heading;
			origin.set( 8, 10 );
			pace = speed = rf( 9f, 13f );
			span = rf( 3.5f, 5.5f );
			ringIn = rf( 1.2f, 2f );
			riseIn = rf( 0f, RISE_EVERY );
			shown = 0f;
			//the liquid as shown (the gallery's carousel's, else the level's)
			Element.Liquid l = WaterLive.liquid();
			pale = FishBrain.darkWater( l );
			hardlight( FishBrain.shade( l ) );
			level();
			pose( ROW_FISH, 4 );
			ring( WaterFX.S );
		}

		/** Whether something may yet startle it. */
		public boolean calm(){
			return state == SWIM || state == CURIOUS;
		}

		/** Something at (sx, sy) startled it: away it bursts. */
		public void startle( float sx, float sy ){
			if (state == FLEE || state == DIVE || leaving()) return;
			//not yet made out: it was never there
			if (shown <= 0f){
				vanish();
				return;
			}
			state = FLEE;
			heading = FishBrain.away( gx(), gy(), sx, sy, rf( 0f, 6.2832f ) ) + rf( -FishBrain.AWAY_JITTER, FishBrain.AWAY_JITTER );
			level();
			v0 = speed = rf( FishBrain.BURST_MIN, FishBrain.BURST_MAX );
			burstT = 0f;
			glides = RNG.nextBoolean();
			//the swirl where it turned
			ring( WaterFX.S );
		}

		/** Food at (fx, fy): it swims to look. */
		public void curious( float fx, float fy ){
			if (state != SWIM) return;
			state = CURIOUS;
			foodX = fx;
			foodY = fy;
			heading = (float)Math.atan2( fy - gy(), fx - gx() );
			level();
		}

		//on whole steps, a fish seen from the side: level or tilted 20 degrees, facing its way
		private void level(){
			face( Math.cos( heading ) < 0 );
			angle = FishBrain.tilt( heading );
		}

		//a small ring at its own point, stirring nothing
		private void ring( int size ){
			if (!visible && life > 0f) return;
			Element.Liquid l = WaterFX.liquid();
			WaterFX.quietRing( gx(), gy() - 1f, size, l.foam, WaterFX.alphaOf( size ) * 0.8f, FxBudget.P3 );
		}

		@Override
		protected void act( float dt ){
			//its water froze (or drained) under it
			if (!FishBrain.wet( gx(), gy() - 1f )){
				leave( 0.4f );
				return;
			}
			hear();
			//back from the bank: the water 8 px ahead, ten times a second
			if ((lookIn -= dt) <= 0f){
				lookIn = FishBrain.LOOK_EVERY;
				float px = gx() + (float)Math.cos( heading ) * FishBrain.AHEAD, py = gy() - 1f + (float)Math.sin( heading ) * FishBrain.AHEAD;
				if (!FishBrain.wet( px, py )){
					heading += (float)Math.PI + rf( -0.6f, 0.6f );
					if (state == CURIOUS) state = SWIM;
					level();
				}
			}
			float tailFps = TAIL_FPS;
			switch (state){
				case FLEE:
					burstT += dt;
					speed = FishBrain.burst( v0, burstT, pace );
					tailFps = BURST_TAIL_FPS;
					if (speed <= pace * 1.2f){
						if (glides){
							state = SWIM;
						} else {
							state = DIVE;
							diveT = 0f;
						}
					}
					break;
				case CURIOUS:
					speed = pace * CURIOUS_PACE;
					if (FishBrain.dist( gx(), gy(), foodX, foodY ) <= FishBrain.ARRIVED){
						state = SWIM;
						speed = pace;
						if (life - kissedAt >= KISS_GAP && RNG.nextFloat() < KISS_SHARE){
							kissedAt = life;
							ring( WaterFX.S );
							WaterAmbience.bubble( gx(), gy() - 2f, WaterFX.liquid().foam );
						}
					}
					break;
				case DIVE:
					diveT += dt;
					speed = pace;
					break;
				default:
					speed = pace;
			}
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			pose( ROW_FISH, 4 + (((int)(life * tailFps)) & 1) );

			boolean night = WaterGeom.night( GameScene.nightTintAlpha() ) > 0.5f;
			float full = pale ? (night ? PALE_NIGHT : PALE) : (night ? ALPHA_NIGHT : ALPHA);
			float fade = Math.min( Math.min( 1f, life / 0.6f ), Math.max( 0f, (span - life) / 0.8f ) );
			if (state == DIVE) fade = Math.min( fade, Math.max( 0f, 1f - diveT / DIVE_TIME ) );
			shown = full * fade;
			if (state == DIVE && diveT >= DIVE_TIME){
				vanish();
				return;
			}

			if ((ringIn -= dt) <= 0f){
				ringIn = rf( 1.2f, 2f );
				ring( WaterFX.S );
			}
			//a light rain: up it comes to the drops now and then
			float rain = WaterAmbience.rain();
			if (rain > 0.05f && rain < WaterGeom.POCKS_FROM && (riseIn -= dt) <= 0f){
				riseIn = RISE_EVERY;
				ring( WaterFX.S );
			}
			if (life >= span) vanish();
		}

		//the water's stirrings this frame: away from a startle, toward food
		private void hear(){
			if (state == FLEE || state == DIVE) return;
			float[] bus = FishBrain.disturbances();
			int n = FishBrain.disturbed();
			float x = gx(), y = gy() - 1f;
			for (int i = 0; i < n; i++){
				float sx = bus[i * 4], sy = bus[i * 4 + 1], r = bus[i * 4 + 2];
				boolean food = bus[i * 4 + 3] > 0f;
				if (FishBrain.startles( x, y, sx, sy, r, food )){
					startle( sx, sy );
					return;
				}
				if (FishBrain.draws( x, y, sx, sy, food )) curious( sx, sy );
			}
		}
	}
}
