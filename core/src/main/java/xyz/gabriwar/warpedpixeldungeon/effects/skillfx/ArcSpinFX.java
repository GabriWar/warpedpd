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

/**
 * A blade arc: a slice of light that sweeps around a point or a sprite, then fades. One arc is a
 * slash, two counter-spinning arcs are a flurry, a slow wide one is a pirouette's ribbon.
 */
public class ArcSpinFX extends Group {

	private final CircleArc arc;
	private final Visual follow;
	private final PointF at;
	private final float spin, duration;
	private float left;

	private ArcSpinFX( Visual follow, PointF at, int color, float radius, float sweep, float startAngle, float spin, float duration ){
		this.follow = follow;
		this.at = at;
		this.spin = spin;
		this.duration = this.left = duration;
		arc = new CircleArc( 14, radius ).color( color, true );
		arc.setSweep( sweep );
		arc.angle = startAngle;
		add( arc );
		place();
	}

	private void place(){
		PointF p = follow != null
				? (follow instanceof CharSprite ? ((CharSprite) follow).destinationCenter() : follow.center())
				: at;
		arc.point( p );
	}

	/** an arc that rides along with a sprite */
	public static ArcSpinFX around( Visual sprite, int color, float radius, float sweep, float startAngle, float spin, float duration ){
		if (sprite == null || sprite.parent == null) return null;
		ArcSpinFX fx = new ArcSpinFX( sprite, null, color, radius, sweep, startAngle, spin, duration );
		sprite.parent.add( fx );
		return fx;
	}

	/** an arc over a cell, on the hero's layer */
	public static ArcSpinFX at( int cell, int color, float radius, float sweep, float startAngle, float spin, float duration ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return null;
		ArcSpinFX fx = new ArcSpinFX( null, DungeonTilemap.raisedTileCenterToWorld( cell ), color, radius, sweep, startAngle, spin, duration );
		Dungeon.hero.sprite.parent.add( fx );
		return fx;
	}

	/** a quick slash: a quarter arc snapping 200 degrees around a sprite in a fifth of a second */
	public static void slash( Visual sprite, int color, boolean clockwise ){
		around( sprite, color, 11, 0.22f, clockwise ? -60 : 240, clockwise ? 900 : -900, 0.22f );
	}

	@Override
	public void update(){
		super.update();
		if (follow != null && !follow.exists){ killAndErase(); return; }
		left -= Game.elapsed;
		if (left <= 0){ killAndErase(); return; }
		place();
		arc.angle += spin * Game.elapsed;
		float p = left / duration;
		arc.alpha( p < 0.5f ? p * 2 * 0.85f : 0.85f );
		if (follow != null) visible = follow.visible;
	}
}
