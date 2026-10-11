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

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FlashGate;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;

/**
 * Brief full-screen flash of lightning, tinted COLOR, following the camera: a storm's (flash(),
 * FlashGate's storm gap, at most PEAK_STORM) and a major lightning of the game struck in the
 * hero's sight (game(), FlashGate's game gap, at most PEAK_GAME); a creature's zap never flashes
 * the screen. Self-destructs after the flash fades.
 * <p>
 * Flash profile: instant spike, rapid decay, the return stroke's second pulse, done. It shakes the
 * camera as hard as the screen shake setting lets it, 2 px at most.
 */
public class LightningFlash extends ColorBlock {

	private static final float DURATION = 0.35f;
	/** Its colour, and how bright it peaks: a storm's, and a major lightning of the game's (above
	 *  FlashGate.PEAK_GAME, the cap of the game's other flashes, as the lightning spec asks). */
	public static final int COLOR = 0xD8E4FF;
	public static final float PEAK_STORM = 0.6f, PEAK_GAME = 0.45f;

	private float left;
	private float peak = PEAK_STORM;

	public LightningFlash() {
		super(1, 1, 0xFF000000 | COLOR);
		left = DURATION;
	}

	/** Trigger a lightning flash covering the entire camera viewport. It shows only as FlashGate
	 *  lets a storm's flash show (0.7 s apart, three in ten seconds with the game's own); one it
	 *  refuses runs its course unseen and shakes nothing. */
	public static LightningFlash flash() {
		LightningFlash f = new LightningFlash();
		if (!FlashGate.storm()) {
			f.visible = false;
			return f;
		}
		f.show();
		return f;
	}

	/** A major lightning of the game's flash, as FlashGate lets the game's own flashes go (2 s
	 *  apart, three in ten seconds with the storm's), softer than a storm's; null when refused. */
	public static LightningFlash game() {
		if (!FlashGate.game()) return null;
		LightningFlash f = new LightningFlash();
		f.peak = PEAK_GAME;
		f.show();
		return f;
	}

	//on the camera, and the camera shaken
	private void show() {
		Camera cam = Camera.main;
		if (cam != null) {
			follow(cam);
			//as hard as the screen shake setting lets it (none with it off), never past the 2 px the
			//blizzard's vignette overhangs the screen by (WeatherOverlay)
			int shake = Math.min(2, WPDSettings.screenShake());
			if (shake > 0) cam.shake(shake, 0.15f);
		}
	}

	private void follow(Camera cam) {
		size(cam.width, cam.height);
		x = cam.scroll.x;
		y = cam.scroll.y;
	}

	@Override
	public void update() {
		super.update();
		left -= Game.elapsed;
		if (left <= 0) {
			killAndErase();
			return;
		}
		//it stays on the view as the camera pans
		if (visible && Camera.main != null) follow(Camera.main);

		// Quick exponential decay: bright spike → fast fade, with the second,
		// weaker pulse a real strike has as the return stroke follows the first
		float p = left / DURATION; // 1 → 0
		float intensity = p * p; // quadratic falloff
		if (p > 0.5f && p < 0.68f) intensity += 0.45f * (1f - Math.abs(p - 0.59f) / 0.09f);
		am = Math.min(1f, intensity) * peak; // never a full white-out
	}
}
