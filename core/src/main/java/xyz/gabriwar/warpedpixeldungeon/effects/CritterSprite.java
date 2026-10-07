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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * One of the surface's small lives (levels/overworld/OverworldCritters): a bird, a hare,
 * a leaping fish or a butterfly. Only a picture - no Actor, nothing saved, nothing to
 * fight - kept in the scene's effects group, which every sliding-window rebase carries
 * along (GameScene.shiftWorldVisuals).
 *
 * It keeps its place as a ground point (ax, ay) in scene pixels, an offset (ox, oy) from
 * it and a height z, and redraws itself from them every frame. A rebase moves x/y under
 * it; the move is noticed (the sprite is no longer where it drew itself) and carried into
 * the ground point, so nothing jumps. Render thread only, like every Image.
 */
public abstract class CritterSprite extends Image {

	//the sheet, tools/critters_gen.py: 16x16 frames, eight to a row. Sized by hand so
	//the film never has to touch the texture
	private static final TextureFilm FILM = new TextureFilm( 128, 128, 16, 16 );
	protected static final int ROW_CROW = 0, ROW_GULL = 1, ROW_FINCH = 2, ROW_OWL = 3,
			ROW_HARE = 4, ROW_SNOW_HARE = 5, ROW_FISH = 6, ROW_BUTTERFLY = 7;

	//the render thread's own dice: never the dungeon's seeded generators
	protected static final java.util.Random RNG = new java.util.Random();

	protected static float rf( float min, float max ){
		return min + RNG.nextFloat() * (max - min);
	}

	public float ax, ay;
	protected float ox, oy, z;
	protected float life;
	//its own alpha: fading in, climbing out of sight
	protected float shown = 1f;
	private float fade = -1f, fadeTime = 1f;
	private float drawnX = Float.NaN, drawnY = Float.NaN;
	private int posed = -1;
	//the sheet this one is drawn from: the surface's critters, or a slice's (SliceCritterSprite)
	private final TextureFilm film;

	protected CritterSprite( float ax, float ay ){
		this( Assets.Effects.CRITTERS, FILM, ax, ay );
	}

	protected CritterSprite( Object texture, TextureFilm film, float ax, float ay ){
		super( texture );
		this.film = film;
		this.ax = ax;
		this.ay = ay;
	}

	protected void pose( int row, int col ){
		int f = row * 8 + col;
		if (f != posed){
			posed = f;
			frame( film.get( f ) );
		}
	}

	protected boolean posed( int row, int col ){
		return posed == row * 8 + col;
	}

	protected void face( boolean left ){
		if (flipHorizontal != left){
			flipHorizontal = left;
			updateFrame();
		}
	}

	/** Where it is over the ground, in scene pixels. */
	public float gx(){ return ax + ox; }
	public float gy(){ return ay + oy; }

	public boolean leaving(){ return fade >= 0f; }

	/** Fades out over the given seconds, then is gone. */
	public void leave( float seconds ){
		if (fade < 0f){
			fade = fadeTime = seconds;
		}
	}

	/** Gone at once: off the scene, its vertex buffer freed. */
	public void vanish(){
		killAndErase();
		destroy();
	}

	protected abstract void act( float dt );

	//the cell whose sight decides whether it shows: the one under it
	protected float seenX(){ return gx(); }
	protected float seenY(){ return gy() - 1f; }

	//out of the hero's sight it is not drawn (the fog would show it through the dimmed ground)
	protected boolean hidesOutOfSight(){ return true; }

	//a rebase slid this picture with the effects group: carry the slide into the ground point.
	//idempotent - it resyncs the drawn marks - so the Field may call it before this sprite's own update
	public void follow(){
		if (!Float.isNaN( drawnX ) && (x != drawnX || y != drawnY)){
			ax += x - drawnX;
			ay += y - drawnY;
			drawnX = x;
			drawnY = y;
		}
	}

	//the ground point was moved by hand (a network mirror's window re-labelled under it):
	//forget where it last drew itself, so that move is not read as a slide
	public void resync(){
		drawnX = drawnY = Float.NaN;
	}

	@Override
	public void update(){
		follow();
		//time stands still (the hourglass, swiftthistle): so does the critter
		if (!Emitter.freezeEmitters){
			super.update();
			float dt = Game.elapsed;
			life += dt;
			act( dt );
			if (!exists) return;
			if (fade >= 0f){
				fade -= dt;
				if (fade <= 0f){
					vanish();
					return;
				}
			}
		}
		x = ax + ox - origin.x;
		y = ay + oy - origin.y - z;
		alpha( shown * (fade >= 0f ? fade / fadeTime : 1f) );
		visible = !hidesOutOfSight() || WeatherSprites.visible( seenX(), seenY() );
		drawnX = x;
		drawnY = y;
	}

	/**
	 * A crow, a gull, a finch or an owl. It glides in and settles, pecks, hops and turns
	 * about, looks up when the hero comes close, and flies off - flapping up and away,
	 * growing as it climbs toward the eye, fading - when something comes too near.
	 */
	public static class Bird extends CritterSprite {

		public enum Kind { CROW, GULL, FINCH, OWL }

		//poses of a bird row (crow, gull, finch); the owl's row has its own
		private static final int PERCH = 0, PECK = 1, WARY = 2, HOP = 3, GLIDE = 7;
		private static final int[] FLAP = { 4, 5, 6, 5 };
		private static final int OWL_PERCH = 0, OWL_BLINK = 1, OWL_ASIDE = 2, OWL_GLIDE = 6;
		private static final int[] OWL_FLAP = { 3, 4, 5, 4 };

		private static final int LANDING = 0, PERCHED = 1, FLYING = 2;

		public final Kind kind;
		private final int row;
		private final boolean owl;
		//the owl sits in a crown; its sight is the trunk's cell, this far below the perch
		private final float seenDY;
		private int state = PERCHED;
		private float t;
		//landing: from this offset and height onto the ground point, after a delay
		private float delay, dur, fromX, fromY, fromZ;
		//perched
		private float idle, pecks, hopT = -1f, hopFrom, hopTo;
		private boolean wary;
		//flying: a bird scared while still coming down keeps the alpha and the size it had
		//(alphaCap, scale0) and, already in the air, turns away at once (airborne)
		private float heading, speed, vmax, climb, alphaCap = 1f, scale0 = 1f;
		private boolean airborne;

		public Bird( Kind kind, float ax, float ay, float seenDY ){
			super( ax, ay );
			this.kind = kind;
			this.seenDY = seenDY;
			owl = kind == Kind.OWL;
			row = kind == Kind.CROW ? ROW_CROW : kind == Kind.GULL ? ROW_GULL
					: kind == Kind.FINCH ? ROW_FINCH : ROW_OWL;
			origin.set( 8, 14 );
			pose( row, owl ? OWL_PERCH : PERCH );
			idle = rf( 0.4f, 1.5f );
		}

		/** Comes down onto its ground point out of the sky on `heading`'s far side, after `delay`. */
		public void land( float heading, float delay ){
			state = LANDING;
			t = 0f;
			this.delay = delay;
			float dist = owl ? 64f : 56f;
			fromX = -(float)Math.cos( heading ) * dist;
			fromY = -(float)Math.sin( heading ) * dist;
			fromZ = owl ? 44f : 36f;
			dur = owl ? 1.6f : rf( 1.0f, 1.3f );
			face( Math.cos( heading ) < 0 );
			ox = fromX;
			oy = fromY;
			z = fromZ;
			shown = 0f;
		}

		/** Still on (or coming down to) the ground: something coming near can startle it. */
		public boolean grounded(){
			return state != FLYING;
		}

		/** Still gliding in: where it will stand is its ground point, not where it is in the air. */
		public boolean landing(){
			return state == LANDING;
		}

		/** Up and away along `heading` after `delay` (a flock leaves one bird after another). */
		public void takeOff( float heading, float delay ){
			if (state == FLYING) return;
			airborne = state == LANDING;
			//not yet come into view: it simply never arrives
			if (airborne && shown <= 0f){
				vanish();
				return;
			}
			alphaCap = shown;
			scale0 = scale.x;
			state = FLYING;
			t = airborne ? 0f : -delay;
			this.heading = heading;
			speed = owl ? 25f : 40f;
			vmax = owl ? 60f : rf( 85f, 105f );
			climb = owl ? 18f : rf( 26f, 34f );
			hopT = -1f;
			face( Math.cos( heading ) < 0 );
		}

		/** Heads up: the hero is close. No pecking, no hopping. */
		public void wary( boolean on ){
			wary = on;
		}

		//on the ground (or coming down to it) it shows when its own spot is in sight
		@Override
		protected float seenX(){
			return state == FLYING ? super.seenX() : ax;
		}

		@Override
		protected float seenY(){
			return state == FLYING ? super.seenY() : ay + seenDY;
		}

		//in the air it is seen wherever it flies; the fog dims it over what he cannot see
		@Override
		protected boolean hidesOutOfSight(){
			return state != FLYING;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			if (state == LANDING) glideIn();
			else if (state == PERCHED) perched( dt );
			else flying( dt );
		}

		private void flap( float time, float fps ){
			int i = (int)(time * fps) & 3;
			pose( row, owl ? OWL_FLAP[i] : FLAP[i] );
		}

		private void glideIn(){
			if (t < delay){
				shown = 0f;
				return;
			}
			float p = Math.min( 1f, (t - delay) / dur );
			float e = 1f - (1f - p) * (1f - p);
			ox = fromX * (1f - e);
			oy = fromY * (1f - e);
			z = fromZ * (1f - e);
			scale.set( 1f + 0.3f * (1f - e) );
			shown = Math.min( 1f, (t - delay) / 0.3f );
			if (p < 0.7f) flap( t - delay, owl ? 7f : 12f );
			else if (p < 0.92f) pose( row, owl ? OWL_GLIDE : GLIDE );
			else pose( row, owl ? OWL_PERCH : HOP );
			if (p >= 1f){
				state = PERCHED;
				t = 0f;
				ox = oy = z = 0f;
				scale.set( 1f );
				shown = 1f;
				pose( row, owl ? OWL_PERCH : PERCH );
			}
		}

		private void perched( float dt ){
			if (owl){
				owlIdle( dt );
				return;
			}
			//a hop in progress: a little arc a few pixels sideways
			if (hopT >= 0f){
				hopT += dt;
				float p = Math.min( 1f, hopT / 0.22f );
				ox = hopFrom + (hopTo - hopFrom) * p;
				z = (float)Math.sin( p * Math.PI ) * 3f;
				pose( row, HOP );
				if (p >= 1f){
					hopT = -1f;
					z = 0f;
					pose( row, PERCH );
				}
				return;
			}
			//pecking: down and up, 0.15s each
			if (pecks > 0f){
				pecks -= dt;
				pose( row, pecks > 0f && ((int)(pecks / 0.15f) & 1) == 1 ? PECK : PERCH );
				return;
			}
			if (wary){
				pose( row, WARY );
				return;
			}
			if (posed( row, WARY )) pose( row, PERCH );
			if ((idle -= dt) > 0f) return;
			idle = kind == Kind.FINCH ? rf( 0.3f, 1.2f ) : rf( 0.6f, 2.2f );
			float roll = RNG.nextFloat();
			if (roll < 0.45f){
				pecks = 0.3f * (2 + RNG.nextInt( 3 ));
			} else if (roll < 0.70f){
				//four to six pixels, never more than six from where it settled
				boolean left = RNG.nextBoolean();
				float to = ox + (left ? -1f : 1f) * rf( 4f, 6f );
				if (Math.abs( to ) > 6f){
					to = 2f * ox - to;
					left = !left;
				}
				face( left );
				hopFrom = ox;
				hopTo = to;
				hopT = 0f;
			} else if (roll < 0.90f){
				face( !flipHorizontal );
			}
		}

		private void owlIdle( float dt ){
			if (wary){
				pose( row, OWL_ASIDE );
				return;
			}
			if ((idle -= dt) > 0f) return;
			if (!posed( row, OWL_PERCH )){
				pose( row, OWL_PERCH );
				idle = rf( 2f, 5f );
			} else if (RNG.nextFloat() < 0.7f){
				pose( row, OWL_BLINK );
				idle = 0.14f;
			} else {
				pose( row, OWL_ASIDE );
				face( RNG.nextBoolean() );
				idle = rf( 0.8f, 1.6f );
			}
		}

		private void flying( float dt ){
			//the flock's stagger: a beat of alarm first, then the spring off the ground
			if (t < 0f){
				pose( row, owl ? OWL_PERCH : WARY );
				return;
			}
			//(a bird still in the air has no ground to spring from)
			if (!owl && !airborne && t < 0.08f){
				pose( row, HOP );
				return;
			}
			speed = Math.min( vmax, speed + (owl ? 40f : 90f) * dt );
			ox += (float)Math.cos( heading ) * speed * dt;
			oy += (float)Math.sin( heading ) * speed * dt;
			z += climb * dt;
			scale.set( scale0 + Math.min( 0.35f, t * 0.25f ) );
			if (owl){
				if (t > 1f && ((int)(t * 2f) & 1) == 1) pose( row, OWL_GLIDE );
				else flap( t, 7f );
			} else {
				if (t > 1.1f && ((int)(t * 2.5f) & 1) == 1) pose( row, GLIDE );
				else flap( t, t < 0.5f ? 16f : 11f );
			}
			float start = owl ? 1.2f : 1.0f, end = owl ? 2.0f : 1.8f;
			shown = alphaCap * (t < start ? 1f : Math.max( 0f, 1f - (t - start) / (end - start) ));
			if (t >= end) vanish();
		}
	}

	/**
	 * A hare: hops out of cover (or, on open ground, is made out where it was crouched all
	 * along) and sits in the grass, nibbling, ears up when the hero
	 * comes close; when he comes closer it bolts in a zig-zag the Field works out on the
	 * map (OverworldCritters.boltRoute) and is gone into the cover it ran for, or out of sight.
	 */
	public static class Hare extends CritterSprite {

		private static final int SIT = 0, ALERT = 1, NIBBLE = 2, HOP = 6;
		private static final int[] RUN = { 3, 5, 4, 5 };
		private static final int EMERGING = 0, SITTING = 1, BOLTING = 2;

		private final int row;
		private int state = SITTING;
		private float t, idle, nibble;
		private boolean alert;
		private float fromX, fromY;
		//the dash: waypoints as offsets from the ground point, pixels (x0,y0,x1,y1,...)
		private float[] route;
		private int leg;
		private boolean intoCover;
		private float runT, routeLen, ran;

		public Hare( boolean snowy, float ax, float ay ){
			super( ax, ay );
			row = snowy ? ROW_SNOW_HARE : ROW_HARE;
			origin.set( 8, 14 );
			pose( row, SIT );
			face( RNG.nextBoolean() );
			idle = rf( 1.5f, 4f );
		}

		/** Comes out of the cover at this offset onto its ground point, in two hops. */
		public void emerge( float fromX, float fromY ){
			state = EMERGING;
			t = 0f;
			this.fromX = fromX;
			this.fromY = fromY;
			ox = fromX;
			oy = fromY;
			shown = 0f;
			face( fromX > 0f );
		}

		/** With no cover beside it: it was crouched there unseen all along, and is made out where it sits. */
		public void appear(){
			shown = 0f;
		}

		/** Not yet running: something coming near can still startle it. */
		public boolean sitting(){
			return state != BOLTING;
		}

		/** Still hopping out of cover: where it will sit is its ground point. */
		public boolean emerging(){
			return state == EMERGING;
		}

		public void alert( boolean on ){
			alert = on;
		}

		/** Off it goes along `route` (offsets from its ground point), into cover or out of sight. */
		public void bolt( float[] route, boolean intoCover ){
			if (state == BOLTING) return;
			state = BOLTING;
			t = 0f;
			this.route = route;
			this.intoCover = intoCover;
			leg = 0;
			routeLen = 0f;
			float px = ox, py = oy;
			for (int i = 0; i + 1 < route.length; i += 2){
				routeLen += (float)Math.hypot( route[i] - px, route[i+1] - py );
				px = route[i];
				py = route[i+1];
			}
			ran = 0f;
			z = 0f;
			shown = 1f;
		}

		@Override
		protected void act( float dt ){
			t += dt;
			if (state == EMERGING){
				//two hops of 0.3s
				float p = Math.min( 1f, t / 0.6f );
				float hp = (p < 0.5f ? p : p - 0.5f) * 2f;
				ox = fromX * (1f - p);
				oy = fromY * (1f - p);
				z = (float)Math.sin( hp * Math.PI ) * 3f;
				shown = Math.min( 1f, t / 0.25f );
				pose( row, HOP );
				if (p >= 1f){
					state = SITTING;
					ox = oy = z = 0f;
					shown = 1f;
					pose( row, SIT );
				}
				return;
			}
			if (state == SITTING){
				//made out over 0.6s (appear); already whole after an emerge
				shown = Math.min( 1f, shown + dt / 0.6f );
				if (alert){
					nibble = 0f;
					pose( row, ALERT );
				} else if (nibble > 0f){
					nibble -= dt;
					pose( row, NIBBLE );
					if (nibble <= 0f) idle = rf( 1.5f, 4f );
				} else {
					pose( row, SIT );
					if ((idle -= dt) <= 0f) nibble = rf( 0.6f, 1.2f );
				}
				return;
			}
			//bolting: a startled beat, then flat out along the legs
			if (t < 0.12f){
				pose( row, ALERT );
				return;
			}
			float step = 120f * dt;
			while (step > 0f && leg * 2 + 1 < route.length){
				float dx = route[leg*2] - ox, dy = route[leg*2+1] - oy;
				float d = (float)Math.hypot( dx, dy );
				if (d <= step){
					ox = route[leg*2];
					oy = route[leg*2+1];
					step -= d;
					ran += d;
					leg++;
				} else {
					ox += dx / d * step;
					oy += dy / d * step;
					ran += step;
					if (Math.abs( dx ) > 0.5f) face( dx < 0f );
					step = 0f;
				}
			}
			runT += dt;
			pose( row, RUN[(int)(runT * 16f) & 3] );
			z = Math.abs( (float)Math.sin( runT * 4f * Math.PI ) ) * 2f;
			float p = routeLen > 0f ? Math.min( 1f, ran / routeLen ) : 1f;
			if (intoCover) shown = p > 0.85f ? (1f - p) / 0.15f : 1f;
			else shown = p > 0.6f ? (1f - p) / 0.4f : 1f;
			if (leg * 2 + 1 >= route.length) vanish();
		}
	}

	/** A fish leaping out of open water and back in: a ripple and a splash at either end. */
	public static class Fish extends CritterSprite {

		private final int col0;
		private final int dir;
		private final float len, apex, dur;

		public Fish( boolean river, float ax, float ay, int dir ){
			this( river ? 2 : 0, ax, ay, dir );
		}

		/** The same leap, in the pale blind fish of the caves' pools (levels/overworld/CaveLife). */
		public static Fish blind( float ax, float ay, int dir ){
			return new Fish( 4, ax, ay, dir );
		}

		private Fish( int col0, float ax, float ay, int dir ){
			super( ax, ay );
			this.col0 = col0;
			this.dir = dir;
			len = rf( 10f, 18f );
			apex = rf( 9f, 15f );
			dur = rf( 0.55f, 0.75f );
			origin.set( 8, 10 );
			pose( ROW_FISH, col0 );
			face( dir < 0 );
			splash( 3 );
		}

		private void splash( int drops ){
			int w = Dungeon.level.width();
			int cell = (int)(gx() / DungeonTilemap.SIZE) + (int)(gy() / DungeonTilemap.SIZE) * w;
			if (cell >= 0 && cell < Dungeon.level.length()) GameScene.ripple( cell );
			Splash.at( new PointF( gx(), gy() - 2f ), 0xD8EAF4, drops );
		}

		@Override
		protected void act( float dt ){
			float p = Math.min( 1f, life / dur );
			ox = dir * len * p;
			z = 4f * apex * p * (1f - p);
			//nose along the arc: up out of the water, down into it
			float vx = len / dur, vy = -4f * apex * (1f - 2f * p) / dur;
			angle = (float)Math.toDegrees( Math.atan2( vy, vx ) ) * dir;
			pose( ROW_FISH, col0 + (((int)(life * 12f)) & 1) );
			if (p >= 1f){
				splash( 4 );
				vanish();
			}
		}
	}

	/**
	 * A butterfly: flutters about its patch of grass for half a minute, never more than a
	 * few cells from it, the wind carrying the patch along, then fades. Darts off when
	 * the hero brushes past.
	 */
	public static class Butterfly extends CritterSprite {

		//cabbage white, brimstone, orange tip, common blue: the pale art takes the tint
		private static final int[] COLOURS = { 0xF4F4EC, 0xF4DC50, 0xF09838, 0x78A8F8 };
		private static final float[] ODDS = { 0.35f, 0.30f, 0.20f, 0.15f };
		private static final int[] FLUTTER = { 0, 1, 2, 1 };

		private float heading, pace, fps, span, bob, height, startled;

		public Butterfly( float ax, float ay ){
			super( ax, ay );
			origin.set( 7, 8 );
			pose( ROW_BUTTERFLY, 0 );
			float roll = RNG.nextFloat();
			int c = 0;
			while (c < COLOURS.length - 1 && roll >= ODDS[c]){
				roll -= ODDS[c];
				c++;
			}
			hardlight( COLOURS[c] );
			heading = rf( 0f, 6.2832f );
			pace = rf( 10f, 16f );
			fps = rf( 10f, 14f );
			span = rf( 18f, 35f );
			bob = rf( 0f, 6.2832f );
			height = rf( 8f, 14f );
			shown = 0f;
		}

		/** Something brushed past: away from it, twice as fast, for a moment. */
		public void startle( float sx, float sy ){
			if (startled > 0f) return;
			heading = (float)Math.atan2( gy() - sy, gx() - sx ) + rf( -0.4f, 0.4f );
			startled = 1.2f;
		}

		@Override
		protected void act( float dt ){
			//the wind carries the whole patch along, as it does the fireflies
			ax += ClimateManager.localWindSpeed() * 0.25f * dt;
			heading += rf( -1f, 1f ) * 4f * dt;
			//never far from its patch: past 40px it turns for home
			if (startled <= 0f && Math.hypot( ox, oy ) > 40f){
				float home = (float)Math.atan2( -oy, -ox );
				float diff = (float)Math.atan2( Math.sin( home - heading ), Math.cos( home - heading ) );
				heading += diff * Math.min( 1f, 2.5f * dt );
			}
			float v = pace * (startled > 0f ? 2.2f : 1f);
			startled = Math.max( 0f, startled - dt );
			ox += (float)Math.cos( heading ) * v * dt;
			oy += (float)Math.sin( heading ) * v * dt * 0.7f;
			height = Math.max( 5f, Math.min( 16f, height + rf( -6f, 6f ) * dt + (startled > 0f ? 6f * dt : 0f) ) );
			bob += dt * 3f * 6.2832f;
			z = height + (float)Math.sin( bob ) * 2.5f;
			pose( ROW_BUTTERFLY, FLUTTER[(int)(life * fps) & 3] );
			shown = Math.min( 1f, life / 0.6f );
			if (life > span) leave( 1f );
		}
	}
}
