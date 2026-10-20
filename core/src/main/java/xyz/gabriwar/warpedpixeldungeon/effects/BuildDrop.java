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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

/**
 * A piece just put up settling into place (homestead/Homestead.build): its picture (BuildPiece.preview,
 * standing where the build bar's ghost shows it, BuildGhost) DROP px over its spot falls onto it,
 * easing out, over FALL s; lands with a squash of a pixel or two (its foot kept, a pixel wider each
 * side) for SQUASH s; and is gone at TIME s, fading over its last FADE s, so what is left is the
 * piece's own tile, roof or sprite - drawn from the first frame, the game's at once, the picture
 * only falling onto it. Whole pixels throughout. Nothing comes down for a channel: it is dug.
 * Among the effects, under the night's tint as the piece is; it rides the slide of the overworld's
 * window. Render thread (play posts itself there); visual only.
 */
public class BuildDrop extends Image {

	/** How far over its spot it starts, px; its fall, its squash and its whole life, s; its fade at
	 *  the end, s; its squash at its deepest, px. */
	public static final float DROP = 8f, FALL = 0.12f, SQUASH = 0.04f, TIME = 0.18f, FADE = 0.02f;
	public static final int DEEPEST = 2;

	private static final int SIZE = DungeonTilemap.SIZE;
	//how far a station's sprite stands up out of its cell (CharSprite.perspectiveRaise), as BuildGhost
	private static final float RAISE = 6f;

	//its spot (its top-left at rest), where it was put last frame, and its age
	private float restX, restY, placedX, placedY, t;

	private BuildDrop( Image picture ){
		super( picture );
		//its own scale (a copy shares its picture's), squashed about its foot
		scale = new PointF( 1, 1 );
		origin.set( width / 2f, height );
	}

	/** A piece put up on a cell of a level `width` cells wide: its picture drops into place. Any
	 *  thread; nothing off the game's scene. */
	public static void play( final BuildPiece piece, final int cell, final int width ){
		if (piece.kind == BuildPiece.Kind.TERRAIN && (Terrain.flags[piece.terrain] & Terrain.LIQUID) != 0) return;
		Fx.post( () -> {
			if (!(Game.scene() instanceof GameScene)) return;
			BuildDrop d = new BuildDrop( piece.preview() );
			float x = (cell % width) * SIZE, y = (cell / width) * SIZE;
			if (piece.kind == BuildPiece.Kind.ACTOR){
				//a station stands on its cell as its sprite does: bottom-aligned, a little raised
				d.restX = x + (SIZE - d.width) / 2f;
				d.restY = y + SIZE - d.height - RAISE;
			} else {
				d.restX = x;
				d.restY = y;
			}
			d.place();
			GameScene.effect( d );
		} );
	}

	/** How far over its spot it is `t` s in, whole px: DROP to 0 over FALL, easing out. */
	static int lift( float t ){
		return Math.round( DROP * (1f - FxCurves.easeOut( t / FALL )) );
	}

	/** How many px it is squashed `t` s in: 0 until it lands, up to DEEPEST and back over SQUASH. */
	static int squash( float t ){
		if (t <= FALL || t >= FALL + SQUASH) return 0;
		return Math.round( DEEPEST * FxCurves.tri( (t - FALL) / SQUASH ) );
	}

	/** Its alpha `t` s in: whole, fading out over the last FADE s of its TIME. */
	static float fade( float t ){
		return Math.max( 0f, Math.min( 1f, (TIME - t) / FADE ) );
	}

	@Override
	public void update(){
		super.update();
		//slid with its layer (the overworld's window moved): its spot moves with it
		restX += x - placedX;
		restY += y - placedY;
		t += Game.elapsed;
		if (t >= TIME){
			Fx.dispose( this );
			return;
		}
		place();
	}

	//where it is and how it is squashed now: on whole pixels, a squash of k px shorter and 2 px wider
	private void place(){
		int k = squash( t );
		scale.set( k > 0 ? (width + 2) / width : 1f, (height - k) / height );
		x = placedX = restX;
		y = placedY = restY - lift( t );
		alpha( fade( t ) );
	}
}
