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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

//a banner rendered as vertical slices riding two superposed travelling waves of
//incommensurate wavelengths - organic warping rather than a uniform flag-wave.
//Offsets are quantized to whole pixels, so the wobble stays on the pixel grid.
//Brightness and the glow layer's glisten both derive from the combined wave's
//slope: the surface brightens where it tips toward the light, and the specular
//glint rides the crests as they travel - a reflection obeying the same wave.
public class WobblyBanner extends Group {

	private static final int SLICE = 2;


	public float x, y;
	public float width, height;
	public float am = 1f;

	private final Image[] slices;
	private final float amp, waveLen, speed;
	private final boolean light;

	private float phase, phase2;
	private WobblyBanner driver;

	public WobblyBanner(BannerSprites.Type type, float amp, float waveLen, float speed, boolean light) {
		super();
		this.amp = amp;
		this.waveLen = waveLen;
		this.speed = speed;
		this.light = light;

		int[] r = BannerSprites.rect(type);
		width  = r[2] - r[0];
		height = r[3] - r[1];

		int n = (int) Math.ceil(width / (float) SLICE);
		slices = new Image[n];
		for (int i = 0; i < n; i++) {
			Image s = new Image(Assets.Interfaces.BANNERS);
			int sx0 = r[0] + i * SLICE;
			int sx1 = Math.min(sx0 + SLICE, r[2]);
			s.frame(s.texture.uvRect(sx0, r[1], sx1, r[3]));
			add(s);
			slices[i] = s;
		}
	}

	//the glow layer follows the phase of the banner it sits on
	public void follow(WobblyBanner other) {
		driver = other;
	}

	public float width()  { return width;  }
	public float height() { return height; }

	@Override
	public void update() {
		if (driver != null) {
			phase  = driver.phase;
			phase2 = driver.phase2;
		} else {
			phase  += Game.elapsed * speed;
			phase2 += Game.elapsed * speed * 1.7f;
			if (phase  > 2 * Math.PI) phase  -= 2 * Math.PI;
			if (phase2 > 2 * Math.PI) phase2 -= 2 * Math.PI;
		}

		for (int i = 0; i < slices.length; i++) {
			float sx = i * SLICE;
			float t1 = phase  + 2f * (float) Math.PI * sx / waveLen;
			float t2 = phase2 + 2f * (float) Math.PI * sx / (waveLen * 0.53f);

			slices[i].x = x + sx;
			//whole-pixel offsets: the wobble stays on the pixel grid
			slices[i].y = y + Math.round(amp * ((float) Math.sin(t1) + 0.55f * (float) Math.sin(t2)));

			//the combined wave's slope, normalized to -1..1: where the surface faces the light
			float slope = ((float) Math.cos(t1) + 0.55f * (float) Math.cos(t2)) / 1.55f;

			if (light) {
				//specular: the glint clings to the leading face of each crest and
				//travels with it
				float spec = Math.max(0f, slope);
				slices[i].am = Math.min(1f, am * (0.35f + 1.25f * spec * spec));
			} else {
				slices[i].brightness(1f + 0.10f * slope);
				slices[i].am = am;
			}
		}

		super.update();
	}

	@Override
	public void draw() {
		if (light) {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		} else {
			super.draw();
		}
	}
}
