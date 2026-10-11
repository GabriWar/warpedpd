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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * The shadows of fish under open water (docs/fx/water-life.md, "Surface shadows"): on the
 * overworld's rivers, lakes and sea under the open sky, and on the shore's water, up to three dark
 * silhouettes (the critters' fish, in the liquid's deep, alpha 0.35-0.5) glide under the surface
 * at 6-9 px/s, turning back where the water stops being open (five of eight neighbours liquid).
 * Only with twelve water cells or more in view, one at most at night. Any stir of the water within
 * 48 px - and the hero within 32, anyone 24 - scatters one as the fish under the dungeon's water
 * flee (FishBrain), and it is gone; another comes 4 to 8 s later somewhere else in view, within
 * eight cells of the hero. One out of sight for 2 s (scrolled off, out of his view) or more than
 * ten cells from him slips away, so the three never linger behind him.
 *
 * One a scene, in the scene's layer under the water's surface (and its sheens), put there by the
 * water's life (WaterLifeFx.sceneCreated); it slides with the overworld's window as that layer does.
 */
public class FishShadows extends Group {

	private static final float SIZE = DungeonTilemap.SIZE;

	/** At most this many by day, this many at night; only with this much water in view. */
	public static final int MOST = 3, MOST_NIGHT = 1, WATER_NEEDED = 12;
	/** A stir this near scatters one, px; the next comes this long after, s. */
	public static final float SCATTER_PX = 48f, BACK_MIN = 4f, BACK_MAX = 8f;
	/** One comes within this many cells of the hero; one farther than FAR_CELLS, or out of sight
	 *  this long, s, slips away. */
	public static final int NEAR_CELLS = 8, FAR_CELLS = 10;
	public static final float UNSEEN_FOR = 2f;
	/** Their pace, px/s, and their alpha. */
	static final float PACE_MIN = 6f, PACE_MAX = 9f, ALPHA_MIN = 0.35f, ALPHA_MAX = 0.5f;

	private float lookIn, nextIn = FxRandom.Float( 1f, 3f );
	private boolean here;

	/** Whether shadows swim on this level: the overworld under an open sky, the shore. */
	static boolean waters( Level level ){
		if (level == null) return false;
		if (level.ambience() == Place.SHORE) return true;
		return level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel && Fx.openSky();
	}

	/** How many swim now (tests). */
	public int swimming(){
		int n = 0;
		for (int i = 0; i < length; i++){
			Gizmo g = members.get( i );
			if (g instanceof Shadow && g.alive) n++;
		}
		return n;
	}

	@Override
	public void update(){
		super.update();
		Level level = Dungeon.level;
		if (level == null || level.fogHeld() || Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		if ((lookIn -= dt) <= 0f){
			lookIn = 1f;
			here = waters( level );
		}
		if (!here) return;
		int most = WaterGeom.night( GameScene.nightTintAlpha() ) > 0.5f ? MOST_NIGHT : MOST;
		if (WaterLive.waterInView() < WATER_NEEDED) most = 0;
		int n = swimming();
		//past what the hour allows: the extra ones slip away
		for (int i = 0; i < length && n > most; i++){
			Gizmo g = members.get( i );
			if (g instanceof Shadow && g.alive && !((Shadow) g).leaving){
				((Shadow) g).slipAway();
				n--;
			}
		}
		if (n >= most || (nextIn -= dt) > 0f) return;
		nextIn = FxRandom.Float( BACK_MIN, BACK_MAX );
		int cell = openCellInView( level );
		if (cell < 0) return;
		Shadow s = (Shadow) recycle( Shadow.class );
		s.reset( (cell % level.width() + 0.5f) * SIZE, (cell / level.width() + 0.5f) * SIZE );
	}

	/** A shadow just went (scattered or slipped away): the next comes 4 to 8 s later. */
	void gone(){
		nextIn = Math.max( nextIn, FxRandom.Float( BACK_MIN, BACK_MAX ) );
	}

	//a cell of open water in view that the hero sees, within NEAR_CELLS of him, a few tries at
	//random, or -1
	private static int openCellInView( Level level ){
		Camera c = Camera.main;
		int w = level.width(), h = level.height();
		int x0 = 1, y0 = 1, x1 = w - 2, y1 = h - 2;
		int hero = heroCell( level );
		if (hero >= 0){
			x0 = Math.max( x0, hero % w - NEAR_CELLS );
			y0 = Math.max( y0, hero / w - NEAR_CELLS );
			x1 = Math.min( x1, hero % w + NEAR_CELLS );
			y1 = Math.min( y1, hero / w + NEAR_CELLS );
		}
		if (c != null){
			x0 = Math.max( x0, (int)(c.scroll.x / SIZE) );
			y0 = Math.max( y0, (int)(c.scroll.y / SIZE) );
			x1 = Math.min( x1, (int)((c.scroll.x + c.width) / SIZE) );
			y1 = Math.min( y1, (int)((c.scroll.y + c.height) / SIZE) );
		}
		if (x1 < x0 || y1 < y0) return -1;
		for (int t = 0; t < 24; t++){
			int cell = FxRandom.IntRange( x0, x1 ) + FxRandom.IntRange( y0, y1 ) * w;
			if (level.heroFOV != null && level.heroFOV[cell] && FishBrain.openWater( level, cell )) return cell;
		}
		return -1;
	}

	//the hero's cell on this level, or -1
	private static int heroCell( Level level ){
		int p = Dungeon.hero != null ? Dungeon.hero.pos : -1;
		return p >= 0 && p < level.length() ? p : -1;
	}

	/** Whether a world point lies more than FAR_CELLS from the hero (cells, Chebyshev). */
	static boolean farFromHero( Level level, float x, float y ){
		int hero = heroCell( level );
		int cell = Fx.cellAt( x, y );
		if (hero < 0 || cell < 0) return false;
		int w = level.width();
		return Math.max( Math.abs( cell % w - hero % w ), Math.abs( cell / w - hero / w ) ) > FAR_CELLS;
	}

	/** One shadow of a fish. */
	public static class Shadow extends Image {

		private static final RectF[] FRAMES = { frame( 0 ), frame( 1 ) };

		private static RectF frame( int col ){
			//the critters' sheet: 128 px square, the fish on row 6, 16 px to a frame
			return new RectF( col * 16 / 128f, 6 * 16 / 128f, (col + 1) * 16 / 128f, 7 * 16 / 128f );
		}

		float px, py;
		private float heading, pace, speed, v0, burstT, life, lookIn, peak, out = -1f, turnIn, unseen;
		private boolean fleeing, leaving;
		private float placedX = Float.NaN, placedY = Float.NaN;
		private int shownFrame = -1;

		public Shadow(){
			super( Assets.Effects.CRITTERS );
			frame( FRAMES[0] );
			origin.set( 8, 10 );
		}

		void reset( float x, float y ){
			px = x;
			py = y;
			heading = FxRandom.angle();
			pace = speed = FxRandom.Float( PACE_MIN, PACE_MAX );
			peak = FxRandom.Float( ALPHA_MIN, ALPHA_MAX );
			life = 0f;
			unseen = 0f;
			out = -1f;
			fleeing = leaving = false;
			lookIn = 0f;
			turnIn = FxRandom.Float( 1f, 3f );
			placedX = placedY = Float.NaN;
			shownFrame = -1;
			resetColor();
			hardlight( FishBrain.shade( WaterLive.liquid() ) );
			am = 0f;
			visible = false;
			revive();
			place();
		}

		/** Off it goes, fading, away from (sx, sy). */
		void scatter( float sx, float sy ){
			if (fleeing) return;
			fleeing = true;
			heading = FishBrain.away( px, py, sx, sy, FxRandom.angle() ) + FxRandom.Float( -FishBrain.AWAY_JITTER, FishBrain.AWAY_JITTER );
			v0 = speed = FxRandom.Float( FishBrain.BURST_MIN, FishBrain.BURST_MAX );
			burstT = 0f;
			slipAway( 0.6f );
		}

		/** It sinks out of sight, unhurried. */
		void slipAway(){
			slipAway( 1f );
		}

		private void slipAway( float seconds ){
			leaving = true;
			if (out < 0f || seconds < out) out = seconds;
		}

		@Override
		public void update(){
			super.update();
			//slid with its layer (the window's rebase): its point goes too
			if (!Float.isNaN( placedX ) && (x != placedX || y != placedY)){
				px += x - placedX;
				py += y - placedY;
			}
			if (Emitter.freezeEmitters){
				place();
				return;
			}
			float dt = Game.elapsed;
			life += dt;
			if (!fleeing) hear();
			if (fleeing){
				burstT += dt;
				speed = FishBrain.burst( v0, burstT, pace );
			} else if ((turnIn -= dt) <= 0f){
				//it wanders: a gentle change of way now and then
				turnIn = FxRandom.Float( 1f, 3f );
				heading += FxRandom.Float( -0.7f, 0.7f );
			}
			//back from where the water stops being open, ten times a second
			if ((lookIn -= dt) <= 0f){
				lookIn = FishBrain.LOOK_EVERY;
				Level level = Dungeon.level;
				float ax = px + (float)Math.cos( heading ) * 10f, ay = py + (float)Math.sin( heading ) * 10f;
				int ahead = Fx.cellAt( ax, ay );
				if (level == null || ahead < 0 || !FishBrain.openWater( level, ahead )){
					heading += (float)Math.PI + FxRandom.Float( -0.6f, 0.6f );
				}
			}
			px += (float)Math.cos( heading ) * speed * dt;
			py += (float)Math.sin( heading ) * speed * dt;

			float a = peak * Math.min( 1f, life / 1f );
			if (out >= 0f){
				out -= dt;
				a *= Math.max( 0f, out / 0.6f );
				if (out <= 0f){
					gone();
					return;
				}
			}
			am = a;
			int f = ((int)(life * (fleeing ? 14f : 3f))) & 1;
			if (f != shownFrame){
				shownFrame = f;
				frame( FRAMES[f] );
			}
			boolean left = Math.cos( heading ) < 0;
			if (flipHorizontal != left){
				flipHorizontal = left;
				updateFrame();
			}
			angle = FishBrain.tilt( heading );
			place();
			Level level = Dungeon.level;
			int cell = Fx.cellAt( px, py );
			boolean inSight = cell >= 0 && level != null && Fx.onScreen( px, py, 0f ) && Fx.seen( px, py );
			visible = a > 0.004f && inSight && level.water[cell];
			//scrolled off, out of the hero's view or left far behind him: it does not linger
			if (!leaving){
				unseen = inSight ? 0f : unseen + dt;
				if (unseen >= UNSEEN_FOR || (level != null && farFromHero( level, px, py ))) slipAway();
			}
		}

		private void gone(){
			kill();
			if (parent instanceof FishShadows) ((FishShadows) parent).gone();
		}

		//what stirs the water within 48 px, or comes too near
		private void hear(){
			float[] bus = FishBrain.disturbances();
			int n = FishBrain.disturbed();
			for (int i = 0; i < n; i++){
				if (FishBrain.dist( px, py, bus[i * 4], bus[i * 4 + 1] ) <= SCATTER_PX){
					scatter( bus[i * 4], bus[i * 4 + 1] );
					return;
				}
			}
			CharSprite hero = Dungeon.hero != null ? Dungeon.hero.sprite : null;
			if (hero != null && FishBrain.tooNear( px, py, hero.x + hero.width() / 2f, hero.y + hero.height(), true )){
				scatter( hero.x + hero.width() / 2f, hero.y + hero.height() );
			}
		}

		//on whole pixels: slow
		private void place(){
			x = placedX = Math.round( px - origin.x );
			y = placedY = Math.round( py - origin.y );
		}
	}
}
