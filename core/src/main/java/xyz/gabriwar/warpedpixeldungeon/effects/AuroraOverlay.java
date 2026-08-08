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
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * Aurora borealis overlay. Curtains are placed at fixed level-space positions
 * distributed across the entire dungeon (like snow/rain particles), not
 * following the camera. Each curtain uses a smooth procedural gradient texture
 * and is FOV-masked — only shown when the hero can see its tile.
 */
public class AuroraOverlay extends Group {

	private static final int TEX_HEIGHT = 128;
	private static final int NUM_CURTAINS = 9;

	// Multiple gradient textures with different color offsets for variety
	private static final Object[] CACHE_KEYS = new Object[NUM_CURTAINS];
	static {
		for (int i = 0; i < NUM_CURTAINS; i++)
			CACHE_KEYS[i] = "AuroraOverlay_" + i;
	}

	// Aurora palette — smooth gradient through these
	private static final int[] PALETTE = {
			0x115533, // dark green
			0x22FF88, // bright green
			0x33FFCC, // teal
			0x44CCFF, // cyan
			0x5588FF, // sky blue
			0x6644FF, // indigo
			0x8844FF, // purple
			0xAA44DD, // violet
			0x6644FF, // indigo (wrap back)
			0x44CCFF, // cyan
			0x22FF88, // green
			0x115533, // dark green
	};

	private static final float FADE_IN_TIME  = 4f;
	private static final float FADE_OUT_TIME = 3f;

	private AuroraCurtain[] curtains;
	private float masterAlpha = 0f;
	private float globalTime  = 0f;
	private boolean active    = false;
	private boolean fadingIn  = false;
	private boolean fadingOut = false;

	public AuroraOverlay() {
		super();
		curtains = new AuroraCurtain[NUM_CURTAINS];
		for (int i = 0; i < NUM_CURTAINS; i++) {
			createTexture(i);
			curtains[i] = new AuroraCurtain(i);
			add(curtains[i]);
		}
		visible = false;
	}

	private void createTexture(int index) {
		Object key = CACHE_KEYS[index];
		if (TextureCache.contains(key)) return;

		Pixmap px = TextureCache.create(key, 1, TEX_HEIGHT).bitmap;
		px.setColor(0x00000000);
		px.fill();

		// Each curtain has a different color offset into the palette
		int colorOffset = index * 2;

		// Top 10%: sharp transparent fade-in
		// 10-30%: bright aurora core
		// 30-100%: long trailing fade (curtain wisps)
		int h = px.getHeight();
		for (int y = 0; y < h; y++) {
			float t = y / (float) (h - 1);

			// Envelope: asymmetric — sharp top, long trailing bottom
			float envelope;
			if (t < 0.08f) {
				envelope = t / 0.08f;
				envelope = envelope * envelope;
			} else if (t < 0.30f) {
				envelope = 1f;
			} else {
				float fade = (t - 0.30f) / 0.70f;
				envelope = (1f - fade);
				envelope = envelope * envelope * envelope; // cubic fade
			}
			envelope = Math.max(0f, Math.min(1f, envelope));

			// Color: interpolate through palette based on vertical position
			float colorT = t * 0.8f;
			float stopF = colorT * (PALETTE.length - 1);
			int stop0 = Math.min((int) stopF, PALETTE.length - 2);
			int stop1 = stop0 + 1;
			float frac = stopF - stop0;
			frac = frac * frac * (3f - 2f * frac); // smoothstep

			// Apply color offset for variety between curtains
			int idx0 = (stop0 + colorOffset) % PALETTE.length;
			int idx1 = (stop1 + colorOffset) % PALETTE.length;
			int c0 = PALETTE[idx0];
			int c1 = PALETTE[idx1];

			int r = (int) (((c0 >> 16) & 0xFF) * (1f - frac) + ((c1 >> 16) & 0xFF) * frac);
			int g = (int) (((c0 >> 8) & 0xFF) * (1f - frac) + ((c1 >> 8) & 0xFF) * frac);
			int b = (int) ((c0 & 0xFF) * (1f - frac) + (c1 & 0xFF) * frac);

			int alpha = (int) (envelope * 255);
			px.drawPixel(0, y, (r << 24) | (g << 16) | (b << 8) | alpha);
		}
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

		private final int index;

		// Fixed level-space position fractions
		private final float xFrac;
		private final float yFrac;
		private final float widthFrac;
		private final float heightFrac;

		private final float waveFreq1, waveFreq2;
		private final float waveAmp1, waveAmp2;
		private final float shimmerFreq;
		private final float driftFreq;
		private final float phaseOffset;
		private final float baseAlpha;

		AuroraCurtain(int seed) {
			super();
			texture(CACHE_KEYS[seed]);
			this.index = seed;

			// Distribute curtains evenly across the full level in both axes
			xFrac       = seed / (float) NUM_CURTAINS;
			yFrac       = 0.05f + (seed % 3) * 0.20f;   // 0.05, 0.25, 0.45 cycling
			widthFrac   = 0.08f + (seed % 3) * 0.04f;   // 0.08–0.16 of level width
			heightFrac  = 0.30f + (seed % 3) * 0.10f;   // 0.30–0.50 of level height

			waveFreq1 = 0.2f  + seed * 0.07f;
			waveFreq2 = 0.35f + seed * 0.05f;
			waveAmp1  = 4f    + seed * 1.5f;
			waveAmp2  = 2f    + seed * 1.0f;
			shimmerFreq = 0.5f + seed * 0.15f;
			driftFreq   = 0.08f + seed * 0.03f;
			phaseOffset = seed * 1.7f;

			baseAlpha = 0.25f + (seed % 3 == 0 ? 0.10f : 0f);
		}

		void updateCurtain(float levelW, float levelH,
		                   float masterAlpha, float time) {
			float t = time + phaseOffset;

			// Level-space size
			float bandW = levelW * widthFrac;
			float bandH = levelH * heightFrac;
			scale.set(bandW, bandH / TEX_HEIGHT);

			// Gentle drift in level space
			float driftX = (float) (Math.sin(t * driftFreq) * levelW * 0.04f
					+ Math.sin(t * driftFreq * 1.6f + 1f) * levelW * 0.02f);
			float waveY = (float) (Math.sin(t * waveFreq1) * waveAmp1
					+ Math.sin(t * waveFreq2 + 0.7f) * waveAmp2);

			float rawX = levelW * xFrac - bandW * 0.5f + driftX;
			float rawY = levelH * yFrac + waveY;
			x = Math.max(0, Math.min(levelW - bandW, rawX));
			y = Math.max(0, Math.min(levelH - bandH, rawY));

			// Shimmer
			float s1 = (float) Math.sin(t * shimmerFreq);
			float s2 = (float) Math.sin(t * shimmerFreq * 2.3f + 1.5f);
			float s3 = (float) Math.sin(t * shimmerFreq * 0.7f + 3f);
			float shimmer = (s1 * 0.5f + s2 * 0.3f + s3 * 0.2f + 1f) * 0.5f;

			am = baseAlpha * (0.3f + shimmer * 0.7f) * masterAlpha;

			// FOV check — only show if hero can see this curtain's center tile
			if (Dungeon.level != null && Dungeon.level.heroFOV != null) {
				int col = (int) ((x + bandW * 0.5f) / DungeonTilemap.SIZE);
				int row = (int) ((y + bandH * 0.5f) / DungeonTilemap.SIZE);
				int w = Dungeon.level.width();
				col = Math.max(0, Math.min(w - 1, col));
				row = Math.max(0, Math.min(Dungeon.level.height() - 1, row));
				int cell = col + row * w;
				if (cell < 0 || cell >= Dungeon.level.heroFOV.length
						|| !Dungeon.level.heroFOV[cell]) {
					am = 0;
				}
			}
		}
	}
}
