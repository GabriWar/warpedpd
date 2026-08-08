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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

/**
 * The wind made visible on the surface: once it blows hard enough to notice
 * (the 3 m/s the cell info calls noticeable) leaves tear off the forest
 * canopies in view and loose snow lifts off the frozen ground, all carried
 * the way the wind goes. ONE emitter stretched over the camera viewport,
 * added by OverworldLevel.addVisuals(): each tick it throws a few darts at
 * the view and only the ones that land on a canopy or on snow become a
 * particle - no per-cell emitters, nothing to rebuild when the window moves.
 */
public class WindDrift extends Emitter {

	private static final float NOTICEABLE_WIND = 3f;

	public WindDrift() {
		super();
		autoKill = false;
		on = false;
		//the base loop needs a factory; emit() below never consults it
		factory = AmbientSnowParticle.FACTORY;
	}

	@Override
	public void update() {
		Camera cam = Camera.main;
		OverworldLevel level = Dungeon.level instanceof OverworldLevel ? (OverworldLevel) Dungeon.level : null;
		float wind = ClimateManager.surfaceWindSpeed();
		if (level == null || cam == null || wind < NOTICEABLE_WIND) {
			on = false;
		} else {
			pos(cam.scroll.x, cam.scroll.y, cam.width, cam.height);
			//a leaf or flake every 0.4s at a noticeable breeze, every 0.05s in a gale
			interval = 1.2f / wind;
			on = true;
		}
		super.update();
	}

	@Override
	protected void emit(int index) {
		OverworldLevel level = (OverworldLevel) Dungeon.level;
		int w = level.width();
		for (int tries = 0; tries < 4; tries++) {
			float px = x + Random.Float(width), py = y + Random.Float(height);
			int cx = (int) (px / DungeonTilemap.SIZE), cy = (int) (py / DungeonTilemap.SIZE);
			if (px < 0 || py < 0 || cx >= w || cy >= level.height()) continue;
			int cell = cx + cy * w;
			if (level.canopyAt(cell)) {
				FallingLeafParticle p = (FallingLeafParticle) recycle(FallingLeafParticle.class);
				int[] colors = FallingLeafParticle.seasonColors(GameCalendar.season());
				p.color(colors[Random.Int(colors.length)]);
				p.resetWind(px, py);
				return;
			}
			if (level.frozenAt(cell) && !level.solid[cell]) {
				((AmbientSnowParticle) recycle(AmbientSnowParticle.class)).resetDrift(px, py);
				return;
			}
		}
	}
}
