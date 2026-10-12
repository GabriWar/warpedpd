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

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.GameMath;

/**
 * What the build bar draws over the map (homestead/building/BuildPlacer), over the fog: a faint green
 * on every cell within reach where the chosen piece would go up now, and a ghost of the piece on
 * each cell it is aimed at (the real tile, roof or station frame at {@link #GHOST_ALPHA}) in a
 * frame, green where it would go up, amber where only the materials are short, red where it
 * can't. Taking down and mending frame the cell with their own icon over it.
 *
 * Laid again whenever what it shows changes (a tap, the pointer onto another cell, the hero's
 * turn), never every frame; the frames breathe gently while they stand. The tint is kept apart
 * and only laid again when the cells it covers change (not when the pointer moves).
 */
public class BuildGhost extends Group {

	public static final int VALID = 0x55EE55;
	public static final int SHORT = 0xFFAA33;
	public static final int INVALID = 0xEE4444;
	public static final int LOOK = 0xEEEEEE;

	public static final float GHOST_ALPHA = 0.6f;
	private static final float TINT_ALPHA = 0.16f;
	private static final float FRAME_ALPHA = 0.9f;

	private static final int SIZE = DungeonTilemap.SIZE;
	//how far a station's sprite stands up out of its cell (CharSprite.perspectiveRaise)
	private static final float RAISE = 6f;

	//the tint under everything, the ghosts and icons, and the frames over them, which breathe
	private final Frames tints = new Frames(), ghosts = new Frames(), frames = new Frames();
	private float time;

	public BuildGhost(){
		super();
		add( tints );
		add( ghosts );
		add( frames );
	}

	/** Takes the ghosts, icons and frames off; the tint stays. Nothing once the ghost has been
	 *  destroyed with its scene. */
	public void reset(){
		if (members == null) return;
		ghosts.destroyAll();
		frames.destroyAll();
	}

	/** A faint green over each of these cells (the cells within reach that take the piece), in
	 *  place of the cells tinted before. */
	public void tint( int[] cells, int width ){
		if (members == null) return;
		tints.destroyAll();
		for (int c : cells){
			ColorBlock b = new ColorBlock( SIZE, SIZE, 0xFF000000 | VALID );
			b.x = (c % width) * SIZE;
			b.y = (c / width) * SIZE;
			b.alpha( TINT_ALPHA );
			tints.add( b );
		}
	}

	/** The piece's ghost on a cell, framed in a colour. */
	public void piece( int cell, int width, BuildPiece piece, int color ){
		Image img = piece.preview();
		float x = (cell % width) * SIZE, y = (cell / width) * SIZE;
		if (piece.kind == BuildPiece.Kind.ACTOR){
			//a station stands on its cell as its sprite does: bottom-aligned, a little raised
			img.x = x + (SIZE - img.width()) / 2f;
			img.y = y + SIZE - img.height() - RAISE;
		} else {
			img.x = x;
			img.y = y;
		}
		img.alpha( GHOST_ALPHA );
		ghosts.add( img );
		frame( cell, width, color );
	}

	/** A frame round a cell, an icon in its middle (taking down, mending). */
	public void icon( int cell, int width, BuildIcons icon, int color ){
		frame( cell, width, color );
		Image img = icon.get();
		img.x = (cell % width) * SIZE + (SIZE - img.width()) / 2f;
		img.y = (cell / width) * SIZE + (SIZE - img.height()) / 2f;
		img.alpha( GHOST_ALPHA + 0.2f );
		ghosts.add( img );
	}

	/** One frame round the whole rectangle with corners a and b (a shape too big to build,
	 *  shown by its bounds alone). */
	public void box( int a, int b, int width, int color ){
		float x0 = Math.min( a % width, b % width ) * SIZE, y0 = Math.min( a / width, b / width ) * SIZE;
		float x1 = (Math.max( a % width, b % width ) + 1) * SIZE, y1 = (Math.max( a / width, b / width ) + 1) * SIZE;
		int c = 0xFF000000 | color;
		edge( x0, y0, x1 - x0, 1, c );
		edge( x0, y1 - 1, x1 - x0, 1, c );
		edge( x0, y0 + 1, 1, y1 - y0 - 2, c );
		edge( x1 - 1, y0 + 1, 1, y1 - y0 - 2, c );
	}

	/** A frame round a cell. */
	public void frame( int cell, int width, int color ){
		float x = (cell % width) * SIZE, y = (cell / width) * SIZE;
		int c = 0xFF000000 | color;
		edge( x, y, SIZE, 1, c );
		edge( x, y + SIZE - 1, SIZE, 1, c );
		edge( x, y + 1, 1, SIZE - 2, c );
		edge( x + SIZE - 1, y + 1, 1, SIZE - 2, c );
	}

	private void edge( float x, float y, float w, float h, int color ){
		ColorBlock b = new ColorBlock( w, h, color );
		b.x = x;
		b.y = y;
		b.alpha( FRAME_ALPHA );
		frames.add( b );
	}

	@Override
	public void update(){
		super.update();
		//a slow breath, so the frames read as live over a still map
		time += Game.elapsed;
		float a = FRAME_ALPHA - 0.25f * (0.5f + 0.5f * (float) Math.sin( time * 4f ));
		frames.alphaAll( GameMath.gate( 0f, a, 1f ) );
	}

	//a layer of the ghost: the frames' edges breathe together
	private static final class Frames extends Group {

		void alphaAll( float a ){
			for (int i = 0; i < members.size(); i++){
				Gizmo g = members.get( i );
				if (g instanceof ColorBlock) ((ColorBlock) g).alpha( a );
			}
		}

		void destroyAll(){
			for (Gizmo g : members) if (g != null) g.destroy();
			clear();
		}
	}
}
