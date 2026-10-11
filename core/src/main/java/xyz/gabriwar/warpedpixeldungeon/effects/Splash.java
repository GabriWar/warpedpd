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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Spray;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//a splash of something struck or spilled: blood, a potion, a crystal's shards, bone dust. Each call
//is its own (effects/fx/combat/Spray: its direction, its cone, what it leaves), made on the render
//thread from the effects' own random numbers; the call itself draws the game's Random exactly once,
//as its emitter's start always did, on whichever thread called it
public class Splash {

	public static void at( int cell, final int color, int n ) {
		at( DungeonTilemap.tileCenterToWorld( cell ), color, n );
	}

	public static void at( PointF p, final int color, int n ) {
		spray( p, null, -3.1415926f / 2, 3.1415926f, color, n, 0 );
	}

	public static void at( PointF p, final float dir, final float cone, final int color, int n ) {
		spray( p, null, dir, cone, color, n, 0 );
	}

	public static void around(Visual v, final int color, int n ) {
		spray( null, v, -3.1415926f / 2, 3.1415926f, color, n, 0 );
	}

	public static void at( PointF p, final float dir, final float cone, final int color, int n, float interval ) {
		spray( p, null, dir, cone, color, n, interval );
	}

	private static void spray( PointF p, Visual around, float dir, float cone, int color, int n, float interval ){
		if (n <= 0 || GameScene.layer( GameScene.Layer.EFFECTS ) == null) {
			return;
		}
		//the one draw of the game's Random a splash has always made (its emitter's start)
		Random.Float();

		Spray spray = new Spray( color, dir, cone );
		FxEmitter emitter = spray.emitter();
		if (emitter == null) return;
		if (around != null) {
			emitter.pos( around );
		} else {
			emitter.pos( p );
		}
		n = Math.min( n, Spray.MOST );
		if (interval > 0) {
			emitter.start( spray, interval, n );
		} else {
			emitter.burst( spray, n );
		}
	}
}
