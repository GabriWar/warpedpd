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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Lit;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.utils.RectF;

//a creature's own light about it (a crystal wisp's): a painted glow of the light sheet (the glow
//nearest its diameter, 7 to 63 px, doubled past that), never a scaled one, fading in over half a
//second and out over a second when put out. Whatever layer it is put in, it draws as light over the
//night's tint (carried to the light layer, GameScene.carryLight). Its picture is an asset's frame:
//safe to make on any thread
public class TorchHalo extends Image implements Lit {

	private CharSprite target;

	private float phase = 0;
	private float brightness = 0.2f;
	private final RectF uv = new RectF();
	private Gizmo carrier;

	public TorchHalo( CharSprite sprite ) {
		super( Assets.Effects.FX_LIGHT );
		target = sprite;
		hardlight( 0xFFDDCC );
		radius( 20 );
		am = 0;
	}

	/** Its radius, px: the glow nearest twice it, on whole pixels (doubled past 63). */
	public void radius( float value ){
		int d = Math.round( value * 2 + 1 );
		int sc = d > 63 ? 2 : 1;
		int[] f = FxFrames.Light.glow( Math.round( d / (float) sc ) );
		float tw = texture.width, th = texture.height;
		uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
		frame( uv );
		scale.set( sc );
	}

	@Override
	public void alpha( float value ){
		brightness = value;
		super.alpha( value );
	}

	@Override
	public void update() {
		super.update();

		//over the night's tint, wherever it was put
		if ((carrier == null || !carrier.alive) && parent != null){
			carrier = GameScene.carryLight( this );
		}

		if (phase < 0) {
			if ((phase += Game.elapsed) >= 0) {
				killAndErase();
			} else {
				am = -phase * brightness;
			}
		} else if (phase < 1) {
			if ((phase += 2*Game.elapsed) >= 1) {
				phase = 1;
			}
			am = phase * brightness;
		}

		x = Math.round( target.x + target.width() / 2f - width() / 2f );
		y = Math.round( target.y + target.height() / 2f - height() / 2f );
	}

	private boolean carried(){
		return carrier != null && carrier.alive && carrier.parent != null;
	}

	@Override
	public void draw() {
		if (!carried()) drawLit();
	}

	@Override
	public void drawLit() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	@Override
	public boolean isVisible() {
		return visible && exists && (target == null || target.visible || !target.alive);
	}

	public void putOut() {
		phase = -1;
	}
}
