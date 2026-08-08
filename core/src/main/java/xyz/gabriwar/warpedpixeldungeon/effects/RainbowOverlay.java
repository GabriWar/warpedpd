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
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * Rainbow arc overlay. A single smooth ROYGBIV gradient texture is rendered
 * across parabolic arc segments, producing a natural-looking rainbow with
 * seamless color blending. A faint secondary (double) rainbow with inverted
 * colors appears above. FOV-masked per segment.
 */
public class RainbowOverlay extends Group {

	private static final Object CACHE_KEY     = RainbowOverlay.class;
	private static final Object CACHE_KEY_INV = "RainbowOverlay_inv";
	private static final int TEX_HEIGHT = 128;

	// ROYGBIV color stops for smooth gradient
	private static final int[] GRADIENT_COLORS = {
			0xFF3333, // red
			0xFF7733, // red-orange
			0xFF9933, // orange
			0xFFCC33, // yellow-orange
			0xFFEE33, // yellow
			0xAAEE33, // yellow-green
			0x33DD55, // green
			0x33CCAA, // green-cyan
			0x3399FF, // blue
			0x4455FF, // blue-indigo
			0x6633FF, // indigo
			0x9933DD, // violet
	};

	private static final int SEGMENTS = 16;

	// Arc geometry as fraction of viewport
	private static final float ARC_SPAN   = 0.90f;
	private static final float ARC_TOP_Y  = 0.02f;
	private static final float ARC_DEPTH  = 0.22f;
	private static final float ARC_THICK  = 0.18f; // how tall the rainbow band is

	// Secondary arc: wider, higher, thinner, dimmer
	private static final float SEC_SPAN   = 1.0f;
	private static final float SEC_TOP_Y  = -0.06f;
	private static final float SEC_DEPTH  = 0.30f;
	private static final float SEC_THICK  = 0.12f;

	private static final float FADE_IN_TIME  = 3f;
	private static final float FADE_OUT_TIME = 4f;

	private ArcSegment[] primarySegs;
	private ArcSegment[] secondarySegs;
	private float masterAlpha = 0f;
	private float globalTime  = 0f;
	private boolean active    = false;
	private boolean fadingIn  = false;
	private boolean fadingOut = false;

	public RainbowOverlay() {
		super();
		createTextures();

		primarySegs = new ArcSegment[SEGMENTS];
		for (int i = 0; i < SEGMENTS; i++) {
			primarySegs[i] = new ArcSegment(CACHE_KEY);
			add(primarySegs[i]);
		}

		secondarySegs = new ArcSegment[SEGMENTS];
		for (int i = 0; i < SEGMENTS; i++) {
			secondarySegs[i] = new ArcSegment(CACHE_KEY_INV);
			add(secondarySegs[i]);
		}

		visible = false;
	}

	private void createTextures() {
		// Primary: smooth ROYGBIV gradient top-to-bottom with transparent edges
		if (!TextureCache.contains(CACHE_KEY)) {
			Pixmap px = TextureCache.create(CACHE_KEY, 1, TEX_HEIGHT).bitmap;
			px.setColor(0x00000000);
			px.fill();
			paintGradient(px, false);
		}

		// Secondary (inverted): VIBGYOR (reversed order)
		if (!TextureCache.contains(CACHE_KEY_INV)) {
			Pixmap px = TextureCache.create(CACHE_KEY_INV, 1, TEX_HEIGHT).bitmap;
			px.setColor(0x00000000);
			px.fill();
			paintGradient(px, true);
		}
	}

	/** Paint a smooth ROYGBIV gradient into a 1-pixel-wide pixmap. */
	private void paintGradient(Pixmap px, boolean inverted) {
		int h = px.getHeight();
		// Margins: top/bottom 15% are transparent fade
		float marginFrac = 0.15f;
		int marginPx = (int) (h * marginFrac);
		int gradStart = marginPx;
		int gradEnd = h - marginPx;
		int gradLen = gradEnd - gradStart;

		for (int y = 0; y < h; y++) {
			// Transparent margin envelope
			float envelope;
			if (y < marginPx) {
				envelope = y / (float) marginPx;
				envelope = envelope * envelope; // smooth fade-in
			} else if (y >= h - marginPx) {
				envelope = (h - 1 - y) / (float) marginPx;
				envelope = envelope * envelope; // smooth fade-out
			} else {
				envelope = 1f;
			}

			// Color from gradient stops
			int r, g, b;
			if (y < gradStart || y >= gradEnd) {
				// In margin — use nearest edge color
				int idx = y < gradStart ? 0 : GRADIENT_COLORS.length - 1;
				if (inverted) idx = GRADIENT_COLORS.length - 1 - idx;
				int c = GRADIENT_COLORS[idx];
				r = (c >> 16) & 0xFF;
				g = (c >> 8) & 0xFF;
				b = c & 0xFF;
			} else {
				// Interpolate between gradient stops
				float t = (y - gradStart) / (float) (gradLen - 1);
				if (inverted) t = 1f - t;
				float stopF = t * (GRADIENT_COLORS.length - 1);
				int stop0 = Math.min((int) stopF, GRADIENT_COLORS.length - 2);
				int stop1 = stop0 + 1;
				float frac = stopF - stop0;

				// Smooth interpolation (smoothstep)
				frac = frac * frac * (3f - 2f * frac);

				int c0 = GRADIENT_COLORS[stop0];
				int c1 = GRADIENT_COLORS[stop1];
				r = (int) (((c0 >> 16) & 0xFF) * (1f - frac) + ((c1 >> 16) & 0xFF) * frac);
				g = (int) (((c0 >> 8) & 0xFF) * (1f - frac) + ((c1 >> 8) & 0xFF) * frac);
				b = (int) ((c0 & 0xFF) * (1f - frac) + (c1 & 0xFF) * frac);
			}

			int alpha = (int) (envelope * 255);
			// RGBA8888
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

		Camera cam = Camera.main;
		if (cam == null) return;

		globalTime += dt;

		float levelW = Dungeon.level.width()  * DungeonTilemap.SIZE;
		float levelH = Dungeon.level.height() * DungeonTilemap.SIZE;

		float shimmer = ((float) Math.sin(globalTime * 0.35f) + 1f) * 0.5f;
		float alpha = masterAlpha * (0.65f + shimmer * 0.35f);

		layoutArc(primarySegs, cam, levelW, levelH,
				ARC_SPAN, ARC_TOP_Y, ARC_DEPTH, ARC_THICK, alpha * 0.30f);

		layoutArc(secondarySegs, cam, levelW, levelH,
				SEC_SPAN, SEC_TOP_Y, SEC_DEPTH, SEC_THICK, alpha * 0.10f);
	}

	private void layoutArc(ArcSegment[] segs, Camera cam,
	                        float levelW, float levelH,
	                        float span, float topY, float depth, float thick,
	                        float baseAlpha) {
		float viewW = cam.width;
		float viewH = cam.height;
		int lvlW = Dungeon.level.width();
		int lvlH = Dungeon.level.height();

		float segW = viewW * span / SEGMENTS;
		float bandH = viewH * thick;

		for (int seg = 0; seg < SEGMENTS; seg++) {
			ArcSegment s = segs[seg];

			float segMid = (seg + 0.5f) / SEGMENTS;

			// Parabolic arc relative to viewport
			float rawX = cam.scroll.x + viewW * (0.5f - span * 0.5f + span * segMid);
			float parab = 2f * segMid - 1f;
			float rawY = cam.scroll.y + viewH * topY + viewH * depth * parab * parab;

			// Clamp to level
			s.x = Math.max(0, Math.min(levelW - segW, rawX - segW * 0.5f));
			s.y = Math.max(0, Math.min(levelH - bandH, rawY));
			s.scale.set(segW, bandH / TEX_HEIGHT);

			// Edge falloff — ends of the arc fade out
			float edgeFade = 1f - Math.abs(2f * segMid - 1f);
			edgeFade = (float) Math.sqrt(edgeFade); // softer than quadratic

			// Per-segment shimmer
			float segShimmer = ((float) Math.sin(
					globalTime * 0.5f + seg * 0.4f) + 1f) * 0.5f;

			s.am = baseAlpha * edgeFade * (0.75f + segShimmer * 0.25f);

			// FOV check
			if (Dungeon.level.heroFOV != null) {
				int col = (int) ((s.x + segW * 0.5f) / DungeonTilemap.SIZE);
				int row = (int) ((s.y + bandH * 0.5f) / DungeonTilemap.SIZE);
				col = Math.max(0, Math.min(lvlW - 1, col));
				row = Math.max(0, Math.min(lvlH - 1, row));
				int cell = col + row * lvlW;
				if (cell < 0 || cell >= Dungeon.level.heroFOV.length
						|| !Dungeon.level.heroFOV[cell]) {
					s.am = 0;
				}
			}
		}
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	private static class ArcSegment extends Image {
		ArcSegment(Object textureKey) {
			super();
			texture(textureKey);
		}
	}
}
