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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.items.AllItemsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Every spell has an icon of its own, and it is painted. */
public class SpellIconsTest {

	@BeforeClass
	public static void assets(){
		AllItemsTest.titleScreen();
	}

	private static final Class<?>[] SPELLS = {
			Alchemize.class, AquaBlast.class, BeaconOfReturning.class, CrimsonEpithet.class,
			CurseInfusion.class, DoomCall.class, EnchantmentInfusion.class, Evolution.class,
			FeatherFall.class, ForcePush.class, Forcefield.class, HolyBlast.class,
			MagicalInfusion.class, MagicalPorter.class, NaturesLullaby.class, PhaseShift.class,
			PlantSummon.class, ReclaimTrap.class, Recycle.class, SeasonChange.class,
			SpontaneousCombustion.class, SummonElemental.class, TelekineticGrab.class,
			UnstableSpell.class, UpgradeDust.class, WildEnergy.class };

	@Test
	public void noTwoSpellsShareAnIcon() throws Exception {
		BufferedImage sheet = ImageIO.read( new File( "src/main/assets/sprites/items.png" ) );
		HashMap<Integer, String> taken = new HashMap<>();
		for (Class<?> c : SPELLS){
			int image = ((Spell) c.getDeclaredConstructor().newInstance()).image();
			assertNull( c.getSimpleName() + " shares its icon with " + taken.get( image ), taken.get( image ) );
			taken.put( image, c.getSimpleName() );
			assertTrue( c.getSimpleName() + "'s icon is painted", opaque( sheet, image ) > 20 );
		}
	}

	//opaque pixels in an item cell
	private static int opaque( BufferedImage im, int cell ){
		int s = ItemSpriteSheet.SIZE, cols = im.getWidth() / s;
		int x0 = (cell % cols) * s, y0 = (cell / cols) * s, n = 0;
		for (int y = 0; y < s; y++) for (int x = 0; x < s; x++) if ((im.getRGB( x0 + x, y0 + y ) >>> 24) != 0) n++;
		return n;
	}
}
