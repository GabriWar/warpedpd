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
 * morning; it grows from one foot to the other as it comes (3 s) and fades out
 * over a few seconds. It hangs far off: it moves with the view at 0.3 of its pace
 * (the parallax of a thing on the horizon). A glint runs along its bands, a slice
 * every half second. Drawn additively, in slices, each easing out of sight and back
 * (0.4 s) where the hero cannot see.
 */
public class RainbowOverlay extends Group {

	private static final int SLICES = 12;
	private static final float FADE_IN_TIME  = 3f;
	private static final float FADE_OUT_TIME = 4f;
	/** A slice going out of sight or coming back eases over this long, s. */
	public static final float SEEN_EASE = 0.4f;
	/** The glint: a slice every GLINT_STEP s, a pause of GLINT_REST slices between runs, its
	 *  share of brightness. */
	public static final float GLINT_STEP = 0.5f, GLINT_SHARE = 0.35f;
	public static final int GLINT_REST = 6;
	/** How far the bow keeps up with the view: it falls back by this share of every move. */
	public static final float PARALLAX = 0.3f;

	private SmartTexture tex;
	private int texW = 0, texH = 0, side = 0;
	private final Image[] slices = new Image[SLICES];
	private final float[] seen = new float[SLICES];
	//how far it has grown from its first foot (0..1), and where it hung when the view was there
	private float reveal = 1f;
	private float anchorX, anchorY;
	private boolean anchored;

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
		if (!active) {
			reveal = 0f;
			anchored = false;
		}
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
			reveal = Math.max(reveal, masterAlpha);
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

		//the bow hangs far off in the view (falling back by PARALLAX of each move it makes),
		//but never past the edge of the world
		if (!anchored) {
			anchored = true;
			anchorX = cam.scroll.x;
			anchorY = cam.scroll.y;
		}
		float x0 = cam.scroll.x - PARALLAX * (cam.scroll.x - anchorX);
		float y0 = cam.scroll.y - PARALLAX * (cam.scroll.y - anchorY);
		x0 = Math.max(0, Math.min(levelW - texW, x0));
		y0 = Math.max(0, Math.min(levelH - texH, y0));
		x0 = (float)Math.floor(x0);
		y0 = (float)Math.floor(y0);
		//the glint's slice now (past the last, resting)
		float glint = (globalTime / GLINT_STEP) % (SLICES + GLINT_REST);

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
			//the glint running along the bands
			float g = Math.max(0f, 1f - Math.abs(i - glint));
			//translucent: the bands are painted strong so the colours read, and let through here
			s.am = alpha * (0.8f + segShimmer * 0.2f + GLINT_SHARE * g) * 0.6f;

			//growing from its first foot (opposite its lean) to the other as it comes
			int order = side > 0 ? i : SLICES - 1 - i;
			float grown = Math.max(0f, Math.min(1f, (reveal * (SLICES + 2) - order) / 3f));
			s.am *= grown;

			//hidden where the hero cannot see the ground under it, easing out and back
			boolean inSight = true;
			if (Dungeon.level.heroFOV != null) {
				int col = (int) ((s.x + sliceW * 0.5f) / DungeonTilemap.SIZE);
				int row = (int) ((y0 + texH * 0.6f) / DungeonTilemap.SIZE);
				col = Math.max(0, Math.min(lvlW - 1, col));
				row = Math.max(0, Math.min(lvlH - 1, row));
				int cell = col + row * lvlW;
				inSight = cell >= 0 && cell < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[cell];
			}
			seen[i] = inSight ? Math.min(1f, seen[i] + dt / SEEN_EASE) : Math.max(0f, seen[i] - dt / SEEN_EASE);
			s.am *= seen[i];
			s.visible = s.am > 0.004f;
		}
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}
