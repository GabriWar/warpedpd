/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

public class Pushing extends Actor {

	private CharSprite sprite;
	private int from;
	private int to;

	private Effect effect;
	private Char ch;
	//a creature thrown from where it stands (a knockback, a shove, a pull), not one put down beside
	//its maker (a split, a summon: already on its cell): only a throw can slam into a wall
	private final boolean thrown;

	private Callback callback;

	{
		actPriority = VFX_PRIO+10;
	}

	public Pushing( Char ch, int from, int to ) {
		this.ch = ch;
		sprite = ch.sprite;
		this.from = from;
		this.to = to;
		this.callback = null;
		thrown = from != to && ch.pos == from;

		if (ch == Dungeon.hero){
			Camera.main.panFollow(ch.sprite, 20f);
		}
	}

	public Pushing( Char ch, int from, int to, Callback callback ) {
		this(ch, from, to);
		this.callback = callback;
	}

	@Override
	protected boolean act() {
		Actor.remove( Pushing.this );

		if (sprite != null && sprite.parent != null) {
			if (Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[to]){
				sprite.visible = true;
			}
			if (effect == null) {
				new Effect();
			}
		} else {
			return true;
		}

		//so that all pushing effects at the same time go simultaneously
		for ( Actor actor : Actor.all() ){
			if (actor instanceof Pushing && actor.cooldown() == 0)
				return true;
		}
		return false;

	}

	public static boolean pushingExistsForChar(Char ch) {
		for (Actor a : all()){
			if (a instanceof Pushing && ((Pushing)a).ch == ch){
				return true;
			}
		}
		return false;
	}

	/** Whether a push from one cell to the next one along was stopped by a solid cell beyond it
	 *  (as the render side sees the level). */
	public static boolean stopped( Level level, int from, int to ){
		if (level == null || from == to) return false;
		int w = level.width();
		int dx = Integer.signum( to % w - from % w ), dy = Integer.signum( to / w - from / w );
		int x = to % w + dx, y = to / w + dy;
		if (x < 0 || y < 0 || x >= w || y >= level.height()) return true;
		return level.solid[x + y * w];
	}

	//the push as it is seen: dust kicked up where it starts, two ghosts of the creature trailing
	//it, dust where it lands; thrown and stopped by a wall, pebbles off the wall, the creature
	//squashed a pixel into it and the camera jolted. Its time (DELAY) and its callback's frame are
	//the game's and never change
	public class Effect extends Visual {

		private static final float DELAY = 0.15f;
		/** The ghosts: when each is left behind, its alpha, how far it trails, how long it lasts. */
		static final float GHOST_1 = 0.05f, GHOST_2 = 0.1f, GHOST_ALPHA_1 = 0.3f, GHOST_ALPHA_2 = 0.15f,
				GHOST_LAG_1 = 2f, GHOST_LAG_2 = 4f, GHOST_FOR = 0.15f, SQUASH_FOR = 0.06f;
		static final int KICKED = 3, LANDED = 2, PEBBLES = 4;

		private PointF end;

		private float delay;
		private float ux, uy;
		private int ghosts;
		private boolean started;

		public Effect() {
			super( 0, 0, 0, 0 );

			point( sprite.worldToCamera( from ) );
			end = sprite.worldToCamera( to );

			speed.set( 2 * (end.x - x) / DELAY, 2 * (end.y - y) / DELAY );
			acc.set( -speed.x / DELAY, -speed.y / DELAY );

			float len = (float)Math.sqrt( (end.x - x) * (end.x - x) + (end.y - y) * (end.y - y) );
			ux = len > 0 ? (end.x - x) / len : 0;
			uy = len > 0 ? (end.y - y) / len : 0;

			delay = 0;

			if (sprite.parent != null)
				sprite.parent.add( this );
		}

		@Override
		public void update() {
			super.update();

			if (!started){
				started = true;
				dust( from, KICKED );
			}

			if ((delay += Game.elapsed) < DELAY) {

				sprite.x = x;
				sprite.y = y;

				if (ghosts == 0 && delay >= GHOST_1){
					ghosts++;
					ghost( GHOST_ALPHA_1, GHOST_LAG_1 );
				} else if (ghosts == 1 && delay >= GHOST_2){
					ghosts++;
					ghost( GHOST_ALPHA_2, GHOST_LAG_2 );
				}

			} else {

				sprite.point(end);

				killAndErase();
				Actor.remove(Pushing.this);
				if (callback != null) callback.call();
				GameScene.sortMobSprites();

				landed();

				next();
			}
		}

		//a ghost of the creature as it is now, trailing it `lag` px
		private void ghost( float alpha, float lag ){
			if (sprite.visible) Afterimage.of( sprite, 0xFFFFFF, alpha, GHOST_FOR, -ux * lag, -uy * lag );
		}

		//dust kicked up at a cell's feet
		private void dust( int cell, int n ){
			FxEmitter air = Fx.air();
			if (air == null || !sprite.visible || Dungeon.level == null) return;
			int w = Dungeon.level.width();
			float cx = (cell % w + 0.5f) * 16, fy = (cell / w + 1) * 16 - 3;
			for (int i = 0; i < n; i++){
				if (!FxBudget.keep( FxBudget.P2, cx, fy )) continue;
				((FxParticle) air.recycle( FxParticle.class )).reset( cx + FxRandom.Float( -4f, 4f ), fy )
						.look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] ).ramp( Element.EARTH.smoke, false ).smooth()
						.life( FxRandom.Float( 0.4f, 0.6f ) ).alpha( 0.3f, 0, 0.3f )
						.vel( -ux * FxRandom.Float( 4f, 10f ), -FxRandom.Float( 2f, 5f ) ).drag( 2f ).priority( FxBudget.P2 );
			}
		}

		//where it lands: dust, or, thrown and stopped by a wall, pebbles off it and a squash into it
		private void landed(){
			if (!sprite.visible) return;
			dust( to, LANDED );
			if (!thrown || !stopped( Dungeon.level, from, to )) return;
			float wx = end.x + sprite.width() / 2f + ux * 7f, wy = end.y + sprite.height() / 2f + uy * 5f;
			Emitter grit = GameScene.fxEmitter();
			if (grit != null){
				grit.pos( wx, wy );
				grit.burst( FxFactories.debris( Element.EARTH.ember.stop( 1 ), Element.EARTH.ember.stop( 3 ) ), PEBBLES );
			}
			sprite.nudge( Math.round( ux ), Math.round( uy ), SQUASH_FOR );
			Fx.shake( 1f, 0.1f );
		}
	}

}
