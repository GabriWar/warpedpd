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
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A character's afterimage: its frame as it is now, left behind as light in a colour, fading over
 * its life (a dash, a blink, a strike from the sky). A copy of the frame (never Image.copy, which
 * would share the sprite's scale), in the light layer, added. FxBudget.afterimageCap() at once:
 * past it the oldest goes.
 */
public class Afterimage extends Image {

	private static final Afterimage[] live = new Afterimage[24];
	private static int count;
	private static long serials;

	private float peak, life, age;
	private long serial;
	private final RectF uv = new RectF();

	public Afterimage(){
		super( Assets.Effects.FX_SHEET );
	}

	/** A new scene: none of the last one's. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = 0;
	}

	public static synchronized int live(){
		return count;
	}

	/** The sprite's frame now, (dx, dy) off where it stands, in `color` at `alpha`, fading over
	 *  `life` s. Null off the game's scene or for a sprite with nothing on show. */
	public static Afterimage of( CharSprite s, int color, float alpha, float life, float dx, float dy ){
		if (s == null || s.texture == null) return null;
		RectF f = s.frame();
		if (f == null) return null;
		Afterimage a = GameScene.recycle( GameScene.Layer.LIGHTS, Afterimage.class );
		if (a == null) return null;
		a.setup( s, f, color, alpha, life, dx, dy );
		enter( a );
		return a;
	}

	private void setup( CharSprite s, RectF f, int color, float alpha, float life, float dx, float dy ){
		texture = s.texture;
		uv.set( f.left, f.top, f.right, f.bottom );
		flipHorizontal = s.flipHorizontal;
		flipVertical = s.flipVertical;
		frame( uv );
		x = s.x + dx;
		y = s.y + dy;
		scale.set( s.scale.x, s.scale.y );
		origin.set( s.origin.x, s.origin.y );
		angle = s.angle;
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		peak = alpha;
		am = alpha;
		this.life = Math.max( 0.01f, life );
		age = 0;
		visible = true;
		revive();
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (age >= life){
			kill();
			return;
		}
		am = peak * (1f - age / life);
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	private static synchronized void enter( Afterimage a ){
		a.serial = ++serials;
		for (int i = 0; i < count; i++) if (live[i] == a) return;
		if (count >= FxBudget.afterimageCap() || count >= live.length){
			Afterimage oldest = null;
			for (int i = 0; i < count; i++) if (oldest == null || live[i].serial < oldest.serial) oldest = live[i];
			if (oldest != null) oldest.kill();
		}
		if (count < live.length) live[count++] = a;
	}

	private static synchronized void leave( Afterimage a ){
		for (int i = 0; i < count; i++){
			if (live[i] == a){
				live[i] = live[--count];
				live[count] = null;
				return;
			}
		}
	}
}
