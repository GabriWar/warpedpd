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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;

/**
 * A ring of light closing in on a character's feet (a warning, a sense): the kit's painted 2:1
 * rings stepped from a wide one down to a narrow one as it eases in, brightening as it closes and
 * gone the moment it arrives. (The kit's FxRing only spreads; this is its reverse.) On the floor,
 * following the sprite. One quad.
 */
public class ClosingRing extends FxPic {

	private final CharSprite sprite;
	private final int from, to;
	private final float life;
	private float age;
	private int shown = -1;

	private ClosingRing( CharSprite sprite, int fromW, int toW, float life, int color ){
		super( SkillVfx.SHEET );
		this.sprite = sprite;
		from = FxFrames.ringIndex( Math.max( fromW, toW ) );
		to = FxFrames.ringIndex( Math.min( fromW, toW ) );
		this.life = Math.max( 0.05f, life );
		additive = true;
		lit( color );
		visible = false;
	}

	/** The ring frame (an index of FxFrames.RINGS) at a share p of its life. */
	public static int frameAt( int from, int to, float p ){
		int steps = from - to;
		return from - Math.min( steps, (int) (FxCurves.easeIn( Math.max( 0f, Math.min( 1f, p ) ) ) * (steps + 1)) );
	}

	/** A ring closing from fromW px wide to toW about the sprite's feet over `life` s. Any thread. */
	public static void on( final CharSprite s, final int fromW, final int toW, final float life, final int color ){
		if (s == null) return;
		final ClosingRing r = new ClosingRing( s, fromW, toW, life, color );
		Fx.post( () -> {
			if (s.parent == null || GameScene.layer( GameScene.Layer.FLOOR ) == null) return;
			GameScene.layer( GameScene.Layer.FLOOR ).add( r );
		} );
	}

	@Override
	public void update(){
		super.update();
		age += Game.elapsed;
		if (age >= life || !sprite.exists){
			Fx.dispose( this );
			return;
		}
		float p = age / life;
		int f = frameAt( from, to, p );
		if (f != shown){
			shown = f;
			show( FxFrames.RINGS[f] );
		}
		place( SkillVfx.midX( sprite ), sprite.y + sprite.height() - 3 );
		am = 0.35f + 0.55f * p;
		visible = sprite.visible;
	}
}
