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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A thin streak of light drawn from one point to another: it shoots forward over the first third of
 * its life, then thins and fades. A lunge, a fleche, a thrust, a counter-cut.
 */
public class StreakFX extends Image {

	private static final double A = 180 / Math.PI;

	private final float length, thickness, duration;
	private float left;

	private StreakFX( PointF s, PointF e, int color, float thickness, float duration ){
		super( Effects.get( Effects.Type.LIGHT_RAY ) );
		hardlight( color );
		origin.set( 0, height / 2 );
		x = s.x - origin.x;
		y = s.y - origin.y;
		float dx = e.x - s.x, dy = e.y - s.y;
		angle = (float) (Math.atan2( dy, dx ) * A);
		length = (float) Math.sqrt( dx * dx + dy * dy ) / width;
		this.thickness = thickness;
		this.duration = this.left = duration;
		scale.set( 0, thickness );
	}

	/** a streak between two cells on the hero's layer */
	public static void show( int from, int to, int color, float thickness, float duration ){
		show( DungeonTilemap.raisedTileCenterToWorld( from ), DungeonTilemap.raisedTileCenterToWorld( to ), color, thickness, duration );
	}

	public static void show( PointF from, PointF to, int color, float thickness, float duration ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		if (PointF.distance( from, to ) < 1f) return;
		Dungeon.hero.sprite.parent.add( new StreakFX( from, to, color, thickness, duration ) );
	}

	@Override
	public void update(){
		super.update();
		left -= Game.elapsed;
		if (left <= 0){ killAndErase(); return; }
		float p = 1 - left / duration;
		float grow = Math.min( 1, p * 3 );
		scale.set( length * grow, thickness * (p < 0.4f ? 1 : (1 - p) / 0.6f) );
		alpha( p < 0.4f ? 1 : (1 - p) / 0.6f );
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
