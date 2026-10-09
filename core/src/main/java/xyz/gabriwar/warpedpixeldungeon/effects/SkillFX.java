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

import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The moves the skill tree makes visible. Everything here is cosmetic: the game
 * has already decided what happened, these only show it. Callbacks fire when a
 * streak lands and are meant for a flash or a sound, never for damage, so a
 * multiplayer client and the host never disagree about who was hit.
 */
public final class SkillFX {

	private SkillFX(){}

	private static Group layer(){
		return Dungeon.hero != null && Dungeon.hero.sprite != null ? Dungeon.hero.sprite.parent : null;
	}

	/** a thrown weapon's streak from one cell to another */
	public static void streak( int from, int to, Item look, Callback onArrive ){
		if (look == null || from == to || layer() == null) { if (onArrive != null) onArrive.call(); return; }
		((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, onArrive );
	}

	/** the same, leaving from a sprite (the thrower) rather than a cell */
	public static void streak( Visual from, int to, Item look, Callback onArrive ){
		if (look == null || layer() == null) { if (onArrive != null) onArrive.call(); return; }
		((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, onArrive );
	}

	/** things falling out of the sky onto a cell: count of them, spread over the tile */
	public static void rain( int cell, Item look, int count, Callback onArrive ){
		if (look == null || layer() == null) return;
		for (int i = 0; i < count; i++){
			PointF to = DungeonTilemap.tileCenterToWorld( cell );
			to.offset( Random.Float( -5, 5 ), Random.Float( -5, 5 ) );
			PointF from = new PointF( to.x + Random.Float( -8, 8 ), to.y - DungeonTilemap.SIZE * Random.Float( 5, 7 ) );
			((MissileSprite) layer().recycle( MissileSprite.class )).reset( from, to, look, i == 0 ? onArrive : null );
		}
	}

	/** a column of light down onto a cell, with a flare at its foot */
	public static void pillar( int cell, int color ){
		if (layer() == null) return;
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PointF top = new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 );
		layer().add( new Beam.LightRay( top, foot ) );
		new Flare( 8, 24 ).color( color, true ).show( layer(), foot, 0.8f );
		CellEmitter.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
	}

	/** a heavy landing: dust, a thud and a shake */
	public static void land( int cell ){
		CellEmitter.bottom( cell ).burst( Speck.factory( Speck.DUST ), 8 );
		Camera.main.shake( 2, 0.25f );
		SpatialSound.play( Assets.Sounds.STURDY, cell, 1f, 0.8f );
	}

	/** a flash on whoever was struck, if anyone is there to see it */
	public static void flash( Char ch ){
		if (ch != null && ch.sprite != null && ch.isAlive()) ch.sprite.flash();
	}
}
