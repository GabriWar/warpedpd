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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
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
	 * A blind cave fish gliding just under a pool's surface, pale against the dark water: it
	 * turns back from the bank, rings the water now and then, and sinks out of sight.
	 */
	public static class Swimmer extends CritterSprite {

		private static final int MAX_TILT = 35;
		private float heading, pace, span, checkIn, ringIn;

		public Swimmer( float ax, float ay, float heading ){
			super( ax, ay );
			this.heading = heading;
			origin.set( 8, 10 );
			pace = rf( 9f, 13f );
			span = rf( 3.5f, 5.5f );
			ringIn = rf( 1.2f, 2f );
			shown = 0f;
			keepLevel();
			pose( ROW_FISH, 4 );
			ring();
		}

		//within MAX_TILT degrees of the level: a fish seen from the side
		private void keepLevel(){
			float deg = (float)Math.toDegrees( Math.atan2( Math.sin( heading ), Math.cos( heading ) ) );
			boolean left = Math.abs( deg ) > 90f;
			float off = left ? (deg > 0 ? 180f - deg : -180f - deg) : deg;
			off = Math.max( -MAX_TILT, Math.min( MAX_TILT, off ) );
			heading = (float)Math.toRadians( left ? 180f - off : off );
			face( left );
		}

		private void ring(){
			int w = Dungeon.level.width();
			int cell = (int)(gx() / DungeonTilemap.SIZE) + (int)(gy() / DungeonTilemap.SIZE) * w;
			if (cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.water[cell]) GameScene.ripple( cell );
		}

		@Override
		protected void act( float dt ){
			//back from the bank: the water ten pixels ahead
			if ((checkIn -= dt) <= 0f){
				checkIn = 0.3f;
				float px = gx() + (float)Math.cos( heading ) * 10f, py = gy() + (float)Math.sin( heading ) * 10f;
				int w = Dungeon.level.width();
				int cell = (int)(px / DungeonTilemap.SIZE) + (int)(py / DungeonTilemap.SIZE) * w;
				if (px < 0 || py < 0 || cell >= Dungeon.level.length()
						|| (Terrain.flags[Dungeon.level.map[cell]] & Terrain.LIQUID) == 0){
					heading += rf( 1.6f, 2.6f );
					keepLevel();
				}
			}
			ox += (float)Math.cos( heading ) * pace * dt;
			oy += (float)Math.sin( heading ) * pace * dt;
			float tilt = (float)Math.sin( heading ) * 25f;
			angle = flipHorizontal ? -tilt : tilt;
			pose( ROW_FISH, 4 + (((int)(life * 6f)) & 1) );
			shown = 0.5f * Math.min( Math.min( 1f, life / 0.6f ), Math.max( 0f, (span - life) / 0.8f ) );
			if ((ringIn -= dt) <= 0f){
				ringIn = rf( 1.2f, 2f );
				ring();
			}
			if (life >= span) vanish();
		}
	}
}
