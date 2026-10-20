/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFxTest;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The pictures that took the place of floating words: the text icon cells of a miss's verb,
 * immunity, a resisted effect, rage, a cursed fuse and a free reload are painted (the four cells
 * kept for the next enchantment family are not), and the five skill marks that had no buff icon
 * have one at both sizes.
 */
public class WordIconsTest {

	@BeforeClass
	public static void boot(){
		EnchantFxTest.boot();
	}

	private static BufferedImage sheet( String asset ) throws IOException {
		File f = new File( "src/main/assets/" + asset );
		assertTrue( asset + " is not in the assets", f.exists() );
		return ImageIO.read( f );
	}

	//opaque pixels in a cell of a sheet of w x h cells, `cols` a row
	private static int opaque( BufferedImage im, int cell, int w, int h, int cols ){
		int x0 = (cell % cols) * w, y0 = (cell / cols) * h, n = 0;
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((im.getRGB( x0 + x, y0 + y ) >>> 24) != 0) n++;
		return n;
	}

	@Test
	public void theNewCellsArePainted() throws IOException {
		BufferedImage im = sheet( "effects/text_icons.png" );
		int cols = im.getWidth() / FloatingText.ICON_WIDTH;
		int[] cells = { FloatingText.MISS_DODGE, FloatingText.MISS_BLOCK, FloatingText.GUARDED, FloatingText.IMMUNE,
				FloatingText.RESISTED, FloatingText.RAGE, FloatingText.FUSE, FloatingText.RELOAD };
		for (int c : cells){
			assertTrue( "cell " + c + " is painted", opaque( im, c, FloatingText.ICON_WIDTH, FloatingText.ICON_HEIGHT, cols ) > 6 );
		}
		for (int c : new int[]{ 106, 107, 124, 125 }){
			assertEquals( "cell " + c + " stays free for the next family", 0,
					opaque( im, c, FloatingText.ICON_WIDTH, FloatingText.ICON_HEIGHT, cols ) );
		}
	}

	@Test
	public void theSkillMarksHaveBuffIconsAtBothSizes() throws IOException {
		BufferedImage small = sheet( "interfaces/buffs.png" ), large = sheet( "interfaces/large_buffs.png" );
		int[] icons = { BuffIndicator.GAP, BuffIndicator.FORM_READ, BuffIndicator.PICKPOCKETED, BuffIndicator.DEMORALIZED,
				BuffIndicator.ACCURACY_MARK };
		for (int i : icons){
			assertTrue( "small " + i, opaque( small, i, BuffIndicator.SIZE_SMALL, BuffIndicator.SIZE_SMALL, small.getWidth() / BuffIndicator.SIZE_SMALL ) > 10 );
			assertTrue( "large " + i, opaque( large, i, BuffIndicator.SIZE_LARGE, BuffIndicator.SIZE_LARGE, large.getWidth() / BuffIndicator.SIZE_LARGE ) > 30 );
		}
		assertEquals( "NONE stays empty", 0, opaque( small, BuffIndicator.NONE, BuffIndicator.SIZE_SMALL, BuffIndicator.SIZE_SMALL,
				small.getWidth() / BuffIndicator.SIZE_SMALL ) );
	}
}
