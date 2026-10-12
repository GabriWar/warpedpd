package xyz.gabriwar.warpedpixeldungeon.tiles;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;

/** Builds frozen falls from the actual shoreline art, without guessed bank widths. */
final class IceFallTexture {

	static final int FIRST_TILE = 320;

	static String forLevel() {
		String source = Dungeon.level.tilesTex();
		String key = IceFallTexture.class.getName() + ":" + source;
		if (!TextureCache.contains(key)) {
			Pixmap pixels = build(TextureCache.get(source).bitmap,
					TextureCache.get(Assets.Environment.ICE_FRINGE).bitmap);
			SmartTexture texture = TextureCache.create(key, pixels.getWidth(), pixels.getHeight());
			texture.bitmap.setBlending(Pixmap.Blending.None);
			texture.bitmap.drawPixmap(pixels, 0, 0);
			texture.filter(Texture.NEAREST, Texture.NEAREST);
			pixels.dispose();
		}
		return key;
	}

	static Pixmap build(Pixmap terrain, Pixmap fringe) {
		Pixmap result = new Pixmap(256, 384, Pixmap.Format.RGBA8888);
		result.setBlending(Pixmap.Blending.None);
		result.drawPixmap(fringe, 0, 0);
		matchIce(result, terrain);
		for (int mask = 0; mask < 16; mask++) {
			int pool = DungeonTileSheet.FROZEN_WATER + mask;
			for (int sides = 0; sides < 4; sides++) {
				int tile = FIRST_TILE + 4 * mask + sides;
				for (int x = 0; x < 16; x++) {
					//Continue every column, including the shoreline's own shadow pixels.
					int lip = terrain.getPixel(pool % 16 * 16 + x, pool / 16 * 16 + 15);
					for (int y = 0; y < 16; y++) {
						float depth = y / 15f;
						float light = (1 - depth) * (1 - depth);
						int r = Math.round((lip >>> 24) * light);
						int g = Math.round((lip >>> 16 & 255) * light);
						int b = Math.round((lip >>> 8 & 255) * light);
						int color = r << 24 | g << 16 | b << 8 | 255;
						//The lip stays continuous. Below it, ice breaks into a running neighbour.
						int edge = Math.min((sides & 1) != 0 ? x : 16,
								(sides & 2) != 0 ? 15 - x : 16);
						if (y > 1 && edge < 5 && ((x * 13 + y * 7) % 17) < (5 - edge) * depth)
							color = 0;
						result.drawPixel(tile % 16 * 16 + x, tile / 16 * 16 + y, color);
					}
				}
			}
		}
		return result;
	}

	/**
	 * The fringe art is drawn once, in steel blue, but each tileset frosts its own water: the
	 * sewers' ice is mint, the caves' teal. Recolour every coloured fringe pixel with the colour
	 * this tileset's own ice (its stitched pool row) has at the same lightness rank: the fringe's
	 * darkest tones take the ice's darkest, its lightest the ice's lightest. The fray then uses
	 * exactly the ice's own palette, so it reads as the same ice breaking up, with no paler or
	 * bluer seam where the pool ends. A tileset with no ice row keeps the art as drawn.
	 */
	static void matchIce(Pixmap atlas, Pixmap terrain) {
		int[] ice = palette(terrain, DungeonTileSheet.FROZEN_WATER / 16 * 16, 16, true);
		if (ice.length == 0) return;
		int rows = FIRST_TILE / 16 * 16;
		int[] fringe = palette(atlas, 0, rows, false);
		if (fringe.length == 0) return;
		float[] hsl = new float[3];
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < atlas.getWidth(); x++) {
				int c = atlas.getPixel(x, y);
				if ((c & 255) == 0) continue;
				toHsl(c, hsl);
				if (hsl[1] < 0.04f) continue;
				//this tone's middle rank among the fringe's coloured pixels, as a share
				float l = hsl[2];
				int lo = 0, hi = fringe.length;
				while (lo < hi) { int m = (lo + hi) >>> 1; if (lightness(fringe[m]) < l) lo = m + 1; else hi = m; }
				int first = lo;
				hi = fringe.length;
				while (lo < hi) { int m = (lo + hi) >>> 1; if (lightness(fringe[m]) <= l) lo = m + 1; else hi = m; }
				float share = fringe.length == 1 ? 0.5f : ((first + lo - 1) / 2f) / (fringe.length - 1);
				int pick = ice[Math.round(share * (ice.length - 1))];
				atlas.drawPixel(x, y, (pick & 0xFFFFFF00) | (c & 255));
			}
		}
	}

	//every coloured (and, if litOnly, lit) opaque pixel of a band of rows, sorted darkest first
	static int[] palette(Pixmap pixels, int top, int rows, boolean litOnly) {
		java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
		float[] hsl = new float[3];
		for (int y = top; y < Math.min(top + rows, pixels.getHeight()); y++) {
			for (int x = 0; x < pixels.getWidth(); x++) {
				int c = pixels.getPixel(x, y);
				if ((c & 255) < 200) continue;
				toHsl(c, hsl);
				if (hsl[1] < 0.04f || (litOnly && hsl[2] < 0.3f)) continue;
				out.add(c);
			}
		}
		out.sort((a, b) -> Float.compare(lightness(a), lightness(b)));
		int[] arr = new int[out.size()];
		for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
		return arr;
	}

	private static float lightness(int rgba) {
		float r = (rgba >>> 24) / 255f, g = (rgba >>> 16 & 255) / 255f, b = (rgba >>> 8 & 255) / 255f;
		return (Math.max(r, Math.max(g, b)) + Math.min(r, Math.min(g, b))) / 2f;
	}

	//mean hue (as a vector, so it does not wrap), saturation and lightness of the coloured,
	//lit pixels of a band of rows; null when the band has none
	static float[] averageHsl(Pixmap pixels, int top, int rows, boolean litOnly) {
		float hx = 0, hy = 0, sat = 0;
		int n = 0;
		float[] hsl = new float[3];
		for (int y = top; y < Math.min(top + rows, pixels.getHeight()); y++) {
			for (int x = 0; x < pixels.getWidth(); x++) {
				int c = pixels.getPixel(x, y);
				if ((c & 255) < 200) continue;
				toHsl(c, hsl);
				if (hsl[1] < 0.04f || (litOnly && hsl[2] < 0.3f)) continue;
				hx += Math.cos(hsl[0] * 2 * Math.PI);
				hy += Math.sin(hsl[0] * 2 * Math.PI);
				sat += hsl[1];
				n++;
			}
		}
		if (n == 0) return null;
		float h = (float) (Math.atan2(hy, hx) / (2 * Math.PI));
		return new float[]{ h < 0 ? h + 1 : h, sat / n };
	}

	private static void toHsl(int rgba, float[] out) {
		float r = (rgba >>> 24) / 255f, g = (rgba >>> 16 & 255) / 255f, b = (rgba >>> 8 & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float l = (max + min) / 2f, h = 0, s = 0;
		if (max != min) {
			float d = max - min;
			s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
			if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
			else if (max == g) h = (b - r) / d + 2;
			else h = (r - g) / d + 4;
			h /= 6f;
		}
		out[0] = h; out[1] = s; out[2] = l;
	}
}
