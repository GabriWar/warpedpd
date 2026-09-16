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

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The aurora: curtains of light hung at fixed places over the level, each a
 * pixel painting of rays, green along the body and rose at the top rim, cycled
 * through a ring of frames and cross-faded from one to the next so they fold and
 * flow without a jump. They drift a little, breathe in brightness, and show only
 * where the hero can see. Drawn additively.
 */
public class AuroraOverlay extends Group {

	private static final int NUM_CURTAINS = 9;
	private static final int VARIANTS = 3;
	private static final int FRAMES = 12;
	private static final int CW = 96, CH = 48;
	private static final float FRAME_RATE = 4f;

	private static final float FADE_IN_TIME  = 4f;
	private static final float FADE_OUT_TIME = 3f;

	private final SmartTexture[] sheets = new SmartTexture[VARIANTS];
	private final AuroraCurtain[] curtains = new AuroraCurtain[NUM_CURTAINS];
	private float masterAlpha = 0f;
	private float globalTime  = 0f;
	private boolean active    = false;
	private boolean fadingIn  = false;
	private boolean fadingOut = false;

	public AuroraOverlay() {
		super();
		for (int v = 0; v < VARIANTS; v++) sheets[v] = sheet(v);
		for (int i = 0; i < NUM_CURTAINS; i++) {
			curtains[i] = new AuroraCurtain(i, sheets[i % VARIANTS]);
			add(curtains[i]);
			add(curtains[i].next);
		}
		visible = false;
	}

	/** one curtain's frames, side by side, painted once and kept */
	private static SmartTexture sheet(int variant) {
		String key = "aurora-curtain-" + variant;
		boolean fresh = !TextureCache.contains(key);
		SmartTexture t = TextureCache.create(key, CW * FRAMES, CH);
		if (fresh) {
			Pixmap pm = t.bitmap;
			pm.setBlending(Pixmap.Blending.None);
			pm.setColor(0);
			pm.fill();
			long seed = 0xA0A0L + variant * 977L;
			for (int f = 0; f < FRAMES; f++) {
				WeatherSprites.paintCurtain(pm, f * CW, CW, CH, seed, f * (float)(Math.PI * 2 / FRAMES));
			}
			t.bitmap(pm);
			t.filter(Texture.NEAREST, Texture.NEAREST);
		}
		return t;
	}

	public void show() {
		if (active && !fadingOut) return;
		active = true; fadingIn = true; fadingOut = false; visible = true;
	}

	public void hide() {
		if (!active) return;
		fadingOut = true; fadingIn = false;
	}

	public boolean isActive() { return active; }

	@Override
	public void update() {
		super.update();
		float dt = Game.elapsed;

		if (fadingIn) {
			masterAlpha = Math.min(1f, masterAlpha + dt / FADE_IN_TIME);
			if (masterAlpha >= 1f) fadingIn = false;
		} else if (fadingOut) {
			masterAlpha = Math.max(0f, masterAlpha - dt / FADE_OUT_TIME);
			if (masterAlpha <= 0f) { fadingOut = false; active = false; visible = false; }
		}

		if (!active || Dungeon.level == null) return;

		globalTime += dt;

		float levelW = Dungeon.level.width()  * DungeonTilemap.SIZE;
		float levelH = Dungeon.level.height() * DungeonTilemap.SIZE;

		for (AuroraCurtain curtain : curtains) {
			curtain.updateCurtain(levelW, levelH, masterAlpha, globalTime);
		}
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	// =====================================================================

	private static class AuroraCurtain extends Image {

		// Fixed level-space position fractions
		private final float xFrac;
		private final float yFrac;
		private final float widthFrac;

		private final float waveFreq1, waveFreq2;
		private final float waveAmp1, waveAmp2;
		private final float shimmerFreq;
		private final float driftFreq;
		private final float phaseOffset;
		private final float baseAlpha;
		private int shown = -1, shownNext = -1;
		//the frame after this one, faded in as this one fades out
		final Image next;

		AuroraCurtain(int seed, SmartTexture sheet) {
			super();
			texture(sheet);
			next = new Image(sheet);

			// Distribute curtains evenly across the full level in both axes
			xFrac       = seed / (float) NUM_CURTAINS;
			yFrac       = 0.05f + (seed % 3) * 0.20f;   // 0.05, 0.25, 0.45 cycling
			widthFrac   = 0.08f + (seed % 3) * 0.04f;   // 0.08-0.16 of level width

			waveFreq1 = 0.2f  + seed * 0.07f;
			waveFreq2 = 0.35f + seed * 0.05f;
			waveAmp1  = 4f    + seed * 1.5f;
			waveAmp2  = 2f    + seed * 1.0f;
			shimmerFreq = 0.5f + seed * 0.15f;
			driftFreq   = 0.08f + seed * 0.03f;
			phaseOffset = seed * 1.7f;

			baseAlpha = 0.55f + (seed % 3 == 0 ? 0.15f : 0f);
			show(0);
		}

		private void show(int f) {
			if (f != shown) {
				shown = f;
				frame(f * CW, 0, CW, CH);
			}
			int n = (f + 1) % FRAMES;
			if (n != shownNext) {
				shownNext = n;
				next.frame(n * CW, 0, CW, CH);
			}
		}

		void updateCurtain(float levelW, float levelH, float masterAlpha, float time) {
			float t = time + phaseOffset;

			//the frames cycle, and each curtain is at its own point in the cycle; the
			//blend between a frame and the next moves with it, so nothing snaps
			float fpos = t * FRAME_RATE;
			int f = ((int)fpos) % FRAMES;
			float blend = fpos - (float)Math.floor(fpos);
			show(f);

			//blown up by whole pixels only, so the rays stay rays
			int k = Math.max(1, Math.round(levelW * widthFrac / CW));
			scale.set(k);
			next.scale.set(k);
			float bandW = CW * k, bandH = CH * k;

			// Gentle drift in level space
			float driftX = (float) (Math.sin(t * driftFreq) * levelW * 0.04f
					+ Math.sin(t * driftFreq * 1.6f + 1f) * levelW * 0.02f);
			float waveY = (float) (Math.sin(t * waveFreq1) * waveAmp1
					+ Math.sin(t * waveFreq2 + 0.7f) * waveAmp2);

			float rawX = levelW * xFrac - bandW * 0.5f + driftX;
			float rawY = levelH * yFrac + waveY;
			x = (float)Math.floor(Math.max(0, Math.min(levelW - bandW, rawX)));
			y = (float)Math.floor(Math.max(0, Math.min(levelH - bandH, rawY)));

			// Shimmer
			float s1 = (float) Math.sin(t * shimmerFreq);
			float s2 = (float) Math.sin(t * shimmerFreq * 2.3f + 1.5f);
			float s3 = (float) Math.sin(t * shimmerFreq * 0.7f + 3f);
			float shimmer = (s1 * 0.5f + s2 * 0.3f + s3 * 0.2f + 1f) * 0.5f;

			float bright = baseAlpha * (0.4f + shimmer * 0.6f) * masterAlpha;

			// FOV check: only show if hero can see this curtain's centre tile
			if (Dungeon.level != null && Dungeon.level.heroFOV != null) {
				int col = (int) ((x + bandW * 0.5f) / DungeonTilemap.SIZE);
				int row = (int) ((y + bandH * 0.5f) / DungeonTilemap.SIZE);
				int w = Dungeon.level.width();
				col = Math.max(0, Math.min(w - 1, col));
				row = Math.max(0, Math.min(Dungeon.level.height() - 1, row));
				int cell = col + row * w;
				if (cell < 0 || cell >= Dungeon.level.heroFOV.length
						|| !Dungeon.level.heroFOV[cell]) {
					bright = 0;
				}
			}
			//additive, so the two halves add up to one curtain
			am = bright * (1f - blend);
			next.am = bright * blend;
			next.x = x;
			next.y = y;
			next.visible = visible;
		}
	}
}
