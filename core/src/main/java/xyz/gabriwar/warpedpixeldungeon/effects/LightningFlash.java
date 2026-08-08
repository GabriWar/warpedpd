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

import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;

/**
 * Brief full-screen white flash that simulates lightning during storms.
 * Self-destructs after the flash fades. Call {@link #flash()} to trigger.
 * <p>
 * Flash profile: instant white spike → rapid exponential decay → done.
 * Optionally shakes the camera for impact.
 */
public class LightningFlash extends ColorBlock {

	private static final float DURATION = 0.35f;

	private float left;

	public LightningFlash() {
		super(1, 1, 0xFFFFFFFF);
		left = DURATION;
	}

	/** Trigger a lightning flash covering the entire camera viewport. */
	public static LightningFlash flash() {
		LightningFlash f = new LightningFlash();
		Camera cam = Camera.main;
		if (cam != null) {
			f.size(cam.width, cam.height);
			f.x = cam.scroll.x;
			f.y = cam.scroll.y;
			cam.shake(2, 0.15f);
		}
		return f;
	}

	@Override
	public void update() {
		super.update();
		left -= Game.elapsed;
		if (left <= 0) {
			killAndErase();
			return;
		}

		// Quick exponential decay: bright spike → fast fade
		float p = left / DURATION; // 1 → 0
		float intensity = p * p; // quadratic falloff
		am = intensity * 0.6f; // peak 60% opacity so it doesn't fully white-out
	}
}
