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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

//a creature frozen in ice: its sprite tinted icy (up to TINT in half a second), unless a blow's flash
//holds its colour; the frost on it and the glint sweeping it are its state's look (FROZEN,
//effects/fx/combat). Melting, it shatters: shards of ice tumbling off, a few drips (Splash, its one
//draw of the game's Random as ever) and a breath of mist
public class IceBlock extends Gizmo {

	/** How icy the sprite turns. */
	public static final float TINT = 0.7f;
	/** Shards of ice as it shatters (at least, at most), drips, puffs of mist. */
	public static final int SHARDS_MIN = 6, SHARDS_MAX = 10, DRIPS = 3, MIST = 2;

	private float phase;

	private CharSprite target;

	public IceBlock( CharSprite target ) {
		super();

		this.target = target;
		phase = 0;
	}

	/** How icy the sprite is `phase` (twice the seconds) in. */
	public static float tint( float phase ){
		return Math.min( 1f, phase ) * TINT;
	}

	@Override
	public void update() {
		super.update();

		phase += Game.elapsed * 2;
		if (target.tintFree( this )) {
			target.tint( 0.83f, 1.17f, 1.33f, tint( phase ) );
		}
	}

	public void melt() {

		target.resetColor();
		killAndErase();

		if (visible) {
			Splash.at( target.center(), 0xFFB2D6FF, DRIPS );
			SpatialSound.play( Assets.Sounds.SHATTER, target.ch );
			final CharSprite s = target;
			Fx.post( () -> shatter( s ) );
		}
	}

	//the ice coming off: shards tumbling from its body, mist breathing off it (render thread)
	private static void shatter( CharSprite s ){
		float cx = s.x + s.width() / 2f, cy = s.y + s.height() / 2f;
		Emitter shards = GameScene.fxEmitter();
		if (shards != null){
			shards.pos( s.x + s.width() * 0.2f, s.y + s.height() * 0.2f, s.width() * 0.6f, s.height() * 0.6f );
			shards.burst( FxFactories.shard( true ), FxRandom.IntRange( SHARDS_MIN, SHARDS_MAX ) );
		}
		//a breath of cold mist: pale curls rising (a puff of the frost's smoke reads as dark smoke on a
		//dark floor)
		FxRecipes.steam( cx, cy, MIST );
	}

	public static IceBlock freeze( CharSprite sprite ) {

		IceBlock iceBlock = new IceBlock( sprite );
		if (sprite.parent != null)
			sprite.parent.add( iceBlock );

		return iceBlock;
	}
}
