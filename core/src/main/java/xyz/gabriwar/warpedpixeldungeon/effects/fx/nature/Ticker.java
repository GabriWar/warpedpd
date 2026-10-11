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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

/**
 * The area's clock in the game's scene: once a frame the growths waiting for their wave and the
 * plants at rest (nothing drawn of its own). Stops with every emitter (freezeEmitters).
 */
public class Ticker extends Gizmo {

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters) return;
		GrowthPulse.tick();
	}
}
