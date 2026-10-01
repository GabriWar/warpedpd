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
}
