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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Explosion;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.LightFrame;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

/**
 * A falling star across the sky (levels/overworld/WorldEvents): a white-hot head - a round glow
 * and a little four-pointed star twinkling between its two painted turns - trailing a core that
 * cools from white through gold to orange (TRAIL_RATE a second by time, never by frame) and
 * embers that drift down as they cool. It lives above the fog and the night's tint, among the
 * effects a window slide moves (an emitter there: the slide moves it and all it holds), and its
 * path moves with it. One that strikes the ground ends in a blast half again as big as a bomb's
 * (effects/fx/fire/Explosion: 30 asked, scaled 1.5), flames, a crater's glow cooling over 2 s, a
 * large scorch, a shudder of the camera (Fx.shake 3) and the crash of rock. Done, it is let go
 * with its pictures' buffers on the render thread.
 */
public class StarStreak extends FxEmitter {

	/** The trail's motes a second, and one ember in EMBER_EVERY of them. */
	public static final float TRAIL_RATE = 60f;
	public static final int EMBER_EVERY = 3;
	/** The trail's colours, as it cools. */
	public static final Ramp TRAIL = new Ramp( 0xFFFFF4, 0xFFE080, 0xFF9030 );
	/** The head's star swaps its turn this often, s. */
	public static final float TWINKLE = 0.06f;
	/** The landing: its blast's scale and its shake. */
	public static final float LAND_SCALE = 1.5f, LAND_SHAKE = 3f, LAND_SHAKE_FOR = 0.4f;

	private float fromX, fromY, toX, toY, duration;
	private boolean impact;
	private float age, trailDue, twinkle;
	private int shed;
	private boolean done, gone;

	private LightFrame glow, star;
	private boolean starA = true;

	/** A star across the sky from `from` to `to` in `duration` seconds, striking the ground at
	 *  `to` when `impact`. Render thread only; nothing without a game scene. */
	public static void fall( PointF from, PointF to, float duration, boolean impact ){
		StarStreak s = GameScene.overFogEmitter( StarStreak.class );
		if (s != null) s.setup( from, to, duration, impact );
	}

	/** Its path set (a star made by fall(), or a test's). */
	public StarStreak setup( PointF from, PointF to, float duration, boolean impact ){
		fromX = from.x;
		fromY = from.y;
		toX = to.x;
		toY = to.y;
		this.duration = Math.max( 0.05f, duration );
		this.impact = impact;
		age = trailDue = twinkle = 0;
		done = gone = false;
		//a window's slide moves an emitter's x and y: its path goes with them
		x = y = 0;
		on = false;
		autoKill = false;
		holdLight();
		return this;
	}

	//where its head is now, slid as its layer was
	private float headX(){
		return fromX + (toX - fromX) * Math.min( 1f, age / duration ) + x;
	}

	private float headY(){
		return fromY + (toY - fromY) * Math.min( 1f, age / duration ) + y;
	}

	@Override
	public void update(){
		super.update();
		if (gone || parent == null || isFrozen()) return;
		if (done){
			//its trail burnt out: let go, its pictures' buffers freed on the render thread
			if (countLiving() == 0){
				gone = true;
				Fx.dispose( this );
			}
			return;
		}
		if (glow == null){
			//its head, made on the render thread at its first frame
			glow = new LightFrame();
			glow.setup( FxFrames.Light.GLOW_15, headX(), headY(), 0xFFFFFF, 0.8f );
			add( glow );
			star = new LightFrame();
			star.setup( FxFrames.Light.STAR4_9_A, headX(), headY(), 0xFFFFF4, 1f );
			add( star );
		}
		float dt = Game.elapsed;
		age += dt;
		float hx = headX(), hy = headY();
		glow.moveTo( hx, hy );
		star.moveTo( hx, hy );
		if ((twinkle -= dt) <= 0){
			twinkle = TWINKLE;
			starA = !starA;
			star.show( starA ? FxFrames.Light.STAR4_9_A : FxFrames.Light.STAR4_9_B );
		}

		//its trail, by time
		trailDue += dt * TRAIL_RATE;
		while (trailDue >= 1f){
			trailDue -= 1f;
			shed( hx, hy );
		}

		if (age >= duration){
			done = true;
			glow.killAndErase();
			glow.destroy();
			star.killAndErase();
			star.destroy();
			if (impact) land( toX + x, toY + y );
		}
	}

	private void shed( float hx, float hy ){
		if (!FxBudget.keep( FxBudget.P1, hx, hy )) return;
		FxParticle p = (FxParticle) recycle( FxParticle.class );
		p.reset( hx + FxRandom.Float( -1f, 1f ), hy + FxRandom.Float( -1f, 1f ) );
		if (shed++ % EMBER_EVERY == 0){
			Flames.ember( p, Element.FIRE.ember ).vel( FxRandom.Float( -10f, 10f ), FxRandom.Float( -6f, 6f ) )
					.gravity( 20f ).life( FxRandom.Float( 0.5f, 0.9f ) ).wind( 0 );
		} else {
			p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_2 : FxFrames.MOTE_1 ).ramp( TRAIL, true )
					.life( FxRandom.Float( 0.3f, 0.45f ) ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P1 );
		}
	}

	//its blast, its flames, the crater's glow, the camera's shudder and the crash, where it struck
	private static void land( float lx, float ly ){
		Emitter blast = GameScene.fxEmitter();
		if (blast != null){
			blast.pos( lx, ly );
			Explosion.hint( lx, ly, LAND_SCALE, LAND_SHAKE, LAND_SHAKE_FOR );
			blast.burst( BlastParticle.FACTORY, Explosion.BOMB_N );
		}
		Emitter fire = GameScene.fxEmitter();
		if (fire != null){
			fire.pos( lx - 8, ly - 6, 16, 12 );
			fire.burst( FlameParticle.FACTORY, 10 );
		}
		//the crater, glowing as it cools
		FxLight.pool( lx, ly + 4, 47, 0xFF7A30, 1f ).life( 0, 0.2f, 1.8f );
		//from where it struck
		int cell = Dungeon.level == null ? -1 : DungeonTilemap.worldToTile( lx, ly, Dungeon.level.width() );
		SpatialSound.play( Assets.Sounds.BLAST, cell, 0.8f );
		SpatialSound.play( Assets.Sounds.ROCKS, cell, 0.7f, 0.8f );
	}
}
