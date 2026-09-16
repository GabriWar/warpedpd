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
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyContext;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * The rainbow after a shower: a bow painted pixel by pixel at the size of the
 * view, seven bands with dithered seams, the brighter sky inside it, faint
 * supernumerary fringes, and the wider, fainter, reversed second bow above. It
 * stands opposite the sun, so it leans east in the afternoon and west in the
 * morning, and it fades in and out over a few seconds. Drawn additively, in
 * slices so each can hide where the hero cannot see.
 */
public class RainbowOverlay extends Group {

	private static final int SLICES = 12;
	private static final float FADE_IN_TIME  = 3f;
	private static final float FADE_OUT_TIME = 4f;

	private SmartTexture tex;
	private int texW = 0, texH = 0, side = 0;
	private final Image[] slices = new Image[SLICES];

	private float masterAlpha = 0f;
	private float globalTime  = 0f;
	private boolean active    = false;
	private boolean fadingIn  = false;
	private boolean fadingOut = false;

	public RainbowOverlay() {
		super();
		for (int i = 0; i < SLICES; i++) {
			slices[i] = new Image();
			add(slices[i]);
		}
		visible = false;
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

	/** the bow is painted for the view's size, and repainted if that changes or the
	 *  sun moves to the other side of the sky */
	private void ensureTexture(Camera cam) {
		int w = Math.max(32, Math.round(cam.width));
		int h = Math.max(24, Math.round(cam.height * 0.5f));
		//opposite the sun: before noon the sun is in the east, so the bow leans west
		int wantSide = SkyContext.dayFraction() < 0.35f ? 1 : -1;
		if (tex != null && w == texW && h == texH && wantSide == side) return;
		texW = w; texH = h; side = wantSide;
		tex = TextureCache.create("rainbow-" + w + "x" + h + "-" + side, w, h);
		Pixmap pm = tex.bitmap;
		pm.setBlending(Pixmap.Blending.None);
		pm.setColor(0);
		pm.fill();
		int radius = Math.round(h * 0.9f);
		int baseY = radius + Math.round(h * 0.12f);
		int cx = w / 2 + side * Math.round(w * 0.10f);
		WeatherSprites.paintBow(pm, w, h, cx, baseY, radius);
		tex.bitmap(pm);
		tex.filter(Texture.NEAREST, Texture.NEAREST);
		int sliceW = w / SLICES;
		for (int i = 0; i < SLICES; i++) {
			slices[i].texture(tex);
			int sw = i == SLICES - 1 ? w - sliceW * i : sliceW;
			slices[i].frame(sliceW * i, 0, sw, h);
		}
	}

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
		ensureTexture(cam);

		globalTime += dt;

		float levelW = Dungeon.level.width()  * DungeonTilemap.SIZE;
		float levelH = Dungeon.level.height() * DungeonTilemap.SIZE;

		//the bow hangs in the view, but never past the edge of the world
		float x0 = Math.max(0, Math.min(levelW - texW, cam.scroll.x));
		float y0 = Math.max(0, Math.min(levelH - texH, cam.scroll.y));
		x0 = (float)Math.floor(x0);
		y0 = (float)Math.floor(y0);

		float shimmer = ((float) Math.sin(globalTime * 0.35f) + 1f) * 0.5f;
		float alpha = masterAlpha * (0.7f + shimmer * 0.3f);

		int lvlW = Dungeon.level.width(), lvlH = Dungeon.level.height();
		//the slices are cut on whole pixels and placed on the same grid, edge to edge
		int sliceW = texW / SLICES;
		for (int i = 0; i < SLICES; i++) {
			Image s = slices[i];
			s.x = x0 + sliceW * i;
			s.y = y0;
			//a slow ripple of brightness along the bow
			float segShimmer = ((float) Math.sin(globalTime * 0.5f + i * 0.5f) + 1f) * 0.5f;
			//translucent: the bands are painted strong so the colours read, and let through here
			s.am = alpha * (0.8f + segShimmer * 0.2f) * 0.6f;

			//hidden where the hero cannot see the ground under it
			if (Dungeon.level.heroFOV != null) {
				int col = (int) ((s.x + sliceW * 0.5f) / DungeonTilemap.SIZE);
				int row = (int) ((y0 + texH * 0.6f) / DungeonTilemap.SIZE);
				col = Math.max(0, Math.min(lvlW - 1, col));
				row = Math.max(0, Math.min(lvlH - 1, row));
				int cell = col + row * lvlW;
				if (cell < 0 || cell >= Dungeon.level.heroFOV.length || !Dungeon.level.heroFOV[cell]) {
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
}
