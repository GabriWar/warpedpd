package xyz.gabriwar.warpedpixeldungeon.tiles;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class IceFallTextureTest {
	@Test
	public void fallsContinueEveryShorelinePixelAndFadeToBlack() {
		GdxNativesLoader.load();
		File assets = new File("src/main/assets/environment");
		Pixmap fringe = new Pixmap(new FileHandle(new File(assets, "ice_fringe.png")));
		//Lava tilesheets have no frozen-water art.
		String[] regions = {"sewers", "prison", "caves", "frozen",
				"spidernest", "temple", "overworld"};
		Pixmap preview = new Pixmap(16 * 4, 32 * regions.length, Pixmap.Format.RGBA8888);
		preview.setBlending(Pixmap.Blending.None);
		try {
			for (int region = 0; region < regions.length; region++) {
				Pixmap terrain = new Pixmap(new FileHandle(new File(assets, "tiles_" + regions[region] + ".png")));
				Pixmap atlas = IceFallTexture.build(terrain, fringe);
				try {
					for (int mask = 0; mask < 16; mask++) {
						int pool = DungeonTileSheet.FROZEN_WATER + mask;
						for (int sides = 0; sides < 4; sides++) {
							int fall = IceFallTexture.FIRST_TILE + 4 * mask + sides;
							for (int x = 0; x < 16; x++) {
								assertEquals(regions[region] + " mask " + mask + " x " + x,
										terrain.getPixel(pool % 16 * 16 + x, pool / 16 * 16 + 15) | 255,
										atlas.getPixel(fall % 16 * 16 + x, fall / 16 * 16));
								if (sides == 0) assertEquals(255,
										atlas.getPixel(fall % 16 * 16 + x, fall / 16 * 16 + 15));
							}
						}
					}
					//A contact sheet drawn from the same atlas used by the game.
					int[] masks = {8, 0, 2, 10};
					for (int column = 0; column < masks.length; column++) {
						int pool = DungeonTileSheet.FROZEN_WATER + masks[column];
						int fall = IceFallTexture.FIRST_TILE + 4 * masks[column];
						preview.drawPixmap(terrain, column * 16, region * 32,
								pool % 16 * 16, pool / 16 * 16, 16, 16);
						preview.drawPixmap(atlas, column * 16, region * 32 + 16,
								fall % 16 * 16, fall / 16 * 16, 16, 16);
					}
				} finally {
					atlas.dispose();
					terrain.dispose();
				}
			}
			Pixmap enlarged = new Pixmap(preview.getWidth() * 4, preview.getHeight() * 4, Pixmap.Format.RGBA8888);
			try {
				enlarged.setFilter(Pixmap.Filter.NearestNeighbour);
				enlarged.drawPixmap(preview, 0, 0, preview.getWidth(), preview.getHeight(),
						0, 0, enlarged.getWidth(), enlarged.getHeight());
				PixmapIO.writePNG(new FileHandle("build/reports/icefall-preview.png"), enlarged);
			} finally {
				enlarged.dispose();
			}
		} finally {
			preview.dispose();
			fringe.dispose();
		}
	}
}
