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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.LightFrame;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

/**
 * The title's and the welcome's torches: the painted fireball, a warm glow breathing behind it
 * (GLOW_31 at x2 behind the tall one, GLOW_47 behind the short one, at GLOW of its alpha) and
 * embers rising off it, EMBERS a second (EMBERS_MAX at once). Its glow and embers are its own,
 * drawn with it (no game scene here), and fade with it.
 */
public class Fireball extends MovieClip {

	/** The glow's colour and its alpha (of the fireball's own), its embers a second and at most.
	 *  The painted fireballs burn blue, so their glow and embers take the art's own blues, hot
	 *  to cool, not the orange of an ordinary fire. */
	public static final int LIGHT = 0x7FA6F8;
	private static final Ramp EMBER = new Ramp( 0xC0D0F8, 0x6090F8, 0x0030A0 );
	public static final float GLOW = 0.25f, EMBERS = 3f;
	public static final int EMBERS_MAX = 8;

	private static boolean second = false;

	private final boolean tall;
	private LightFrame glow;
	private FxEmitter embers;
	private float emberDue;

	public Fireball() {
		this(second);
		second = !second;
	}

	public Fireball(boolean second) {

		tall = PixelScene.landscape();
		if (tall){
			texture( "effects/fireball-tall.png" );
			TextureFilm frames = new TextureFilm( texture, 61, 61 );
			MovieClip.Animation anim = new MovieClip.Animation( 24, true );
			anim.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 );
			play( anim );
		} else {
			texture( "effects/fireball-short.png" );
			TextureFilm frames = new TextureFilm( texture, 47, 47 );
			MovieClip.Animation anim = new MovieClip.Animation( 24, true );
			anim.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 );
			play( anim );
		}

		//second fireball is flipped and has its animation offset
		if (second){
			flipHorizontal =  true;
			curFrame = 12;
			frame( curAnim.frames[curFrame] );
		}

	}

	@Override
	public void update() {
		super.update();
		if (glow == null){
			//made at its first frame, on the render thread
			glow = new LightFrame();
			glow.setup( tall ? FxFrames.Light.GLOW_31 : FxFrames.Light.GLOW_47, 0, 0, LIGHT, GLOW ).breathe( 0.5f, 0.08f );
			if (tall) glow.scale.set( 2 );
			embers = new FxEmitter();
			embers.holdLight();
			embers.on = false;
			embers.autoKill = false;
		}
		glow.camera = embers.camera = camera();
		float cx = x + width / 2f, cy = y + height * 0.55f;
		glow.peak( GLOW * am );
		glow.moveTo( cx, cy );
		if (tall){
			//its middle, at twice its size
			glow.x = Math.round( cx - glow.width );
			glow.y = Math.round( cy - glow.height );
		}
		glow.update();

		emberDue += Game.elapsed * EMBERS;
		while (emberDue >= 1f){
			emberDue -= 1f;
			if (am > 0.5f && embers.countLiving() < EMBERS_MAX){
				FxParticle p = Flames.make( embers, FxParticle.class, cx + FxRandom.Float( -width / 6f, width / 6f ), y + height * 0.35f );
				//no wind and no ending over a level's water: there is no game here
				Flames.ember( p, EMBER ).wind( 0 ).fate( null );
			}
		}
		embers.update();
	}

	@Override
	public void draw() {
		if (glow != null) glow.draw();
		super.draw();
		if (embers != null) embers.draw();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (glow != null) glow.destroy();
		if (embers != null) embers.destroy();
	}
}
