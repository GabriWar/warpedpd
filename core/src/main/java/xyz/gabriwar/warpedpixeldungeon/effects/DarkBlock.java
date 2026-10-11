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

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

//a creature darkened (doom, corruption, a challenge's spectator): its sprite dimmed, breathing
//between LOW and HIGH brightness at HZ, and tinted TINT of its dark (CharSprite.darkTint: doom's
//wine, corruption's violet, else a shadow's) - multiplied in, so its own shading stays. Not while a
//blow's flash or the ice holds its colour. The wisps rising off it are its state's look (DARKENED)
public class DarkBlock extends Gizmo{

	/** The brightness it breathes between, how fast, and how much of its dark tints it. */
	public static final float LOW = 0.40f, HIGH = 0.55f, HZ = 0.5f, TINT = 0.4f;

	private CharSprite target;
	private float time;

	public DarkBlock( CharSprite target ) {
		super();

		this.target = target;
	}

	/** Its brightness `t` s in. */
	public static float brightness( float t ){
		return (LOW + HIGH) / 2f - (HIGH - LOW) / 2f * (float)Math.cos( 2 * Math.PI * HZ * t );
	}

	@Override
	public void update() {
		super.update();

		time += Game.elapsed;
		if (!target.tintFree( this )) return;
		float b = brightness( time );
		int c = target.darkTint();
		target.rm = b * (1f - TINT + TINT * ((c >> 16) & 0xFF) / 255f);
		target.gm = b * (1f - TINT + TINT * ((c >> 8) & 0xFF) / 255f);
		target.bm = b * (1f - TINT + TINT * (c & 0xFF) / 255f);
		target.ra = target.ga = target.ba = 0;

	}

	public void lighten() {

		target.resetColor();
		killAndErase();

	}

	public static DarkBlock darken( CharSprite sprite ) {

		DarkBlock darkBlock = new DarkBlock( sprite );
		if (sprite.parent != null)
			sprite.parent.add( darkBlock );

		return darkBlock;
	}

}
