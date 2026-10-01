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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.CircleArc;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** A soft disc of light that swells out from a point and fades: a prayer's pulse, a purge, a halo's beat. */
public class PulseRingFX extends Group {

	private final CircleArc disc;
	private final Visual follow;
	private final PointF at;
	private final float duration, peak;
	private float left;

	private PulseRingFX( Visual follow, PointF at, int color, float radius, float duration, float peak ){
		this.follow = follow;
		this.at = at;
		this.duration = this.left = duration;
		this.peak = peak;
		disc = new CircleArc( 24, radius ).color( color, true );
		disc.scale.set( 0 );
		add( disc );
		place();
	}

	private void place(){
		disc.point( follow != null
				? (follow instanceof CharSprite ? ((CharSprite) follow).destinationCenter() : follow.center())
				: at );
	}

	public static void around( Visual sprite, int color, float radius, float duration ){
		if (sprite == null || sprite.parent == null) return;
		sprite.parent.addToBack( new PulseRingFX( sprite, null, color, radius, duration, 0.45f ) );
	}

	public static void at( int cell, int color, float radius, float duration ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		Dungeon.hero.sprite.parent.addToBack( new PulseRingFX( null, DungeonTilemap.tileCenterToWorld( cell ), color, radius, duration, 0.45f ) );
	}

	@Override
	public void update(){
		super.update();
		if (follow != null && !follow.exists){ killAndErase(); return; }
		left -= Game.elapsed;
		if (left <= 0){ killAndErase(); return; }
		place();
		float p = 1 - left / duration;
		//eases out: fast at first, then coasting to the rim
		float s = 1 - (1 - p) * (1 - p);
		disc.scale.set( s );
		disc.alpha( peak * (1 - p) );
		if (follow != null) visible = follow.visible;
	}
}
