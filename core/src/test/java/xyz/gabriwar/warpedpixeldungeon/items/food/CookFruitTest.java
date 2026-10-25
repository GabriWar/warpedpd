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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Blandfruit and a seed in the alchemy pot: a seed that brews no potion (the blandfruit's own) is no
 * recipe, instead of crashing the pot the moment both are in; every potion seed still cooks the fruit.
 */
public class CookFruitTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private static ArrayList<Item> pot( Item... items ){
		return new ArrayList<>( Arrays.asList( items ) );
	}

	@Test
	public void aSeedWithNoPotionIsNoRecipe(){
		Blandfruit.CookFruit cook = new Blandfruit.CookFruit();
		Plant.Seed seed = new BlandfruitBush.Seed();
		assertFalse( cook.testIngredients( pot( new Blandfruit(), seed ) ) );
		assertFalse( cook.testIngredients( pot( seed, new Blandfruit() ) ) );
		assertNull( cook.sampleOutput( pot( seed, new Blandfruit() ) ) );
	}

	@Test
	public void aPotionSeedStillCooksTheFruit(){
		Blandfruit.CookFruit cook = new Blandfruit.CookFruit();
		ArrayList<Item> in = pot( new Sungrass.Seed(), new Blandfruit() );
		assertTrue( cook.testIngredients( in ) );
		Item out = cook.sampleOutput( in );
		assertTrue( out instanceof Blandfruit && ((Blandfruit) out).potionAttrib instanceof PotionOfHealing );
	}

	//every seed the game has, as the pot would meet it with a blandfruit: a recipe it cooks, or none, never a crash
	@Test
	public void noSeedCrashesThePot() throws Exception {
		Path root = Paths.get( "build/classes/java/main" );
		ArrayList<Class<?>> seeds = new ArrayList<>();
		try (Stream<Path> files = Files.walk( root )){
			for (Path f : (Iterable<Path>) files::iterator){
				String n = root.relativize( f ).toString();
				if (!n.endsWith( ".class" )) continue;
				Class<?> c = Class.forName( n.substring( 0, n.length() - 6 ).replace( File.separatorChar, '.' ), false,
						CookFruitTest.class.getClassLoader() );
				if (Plant.Seed.class.isAssignableFrom( c ) && !Modifier.isAbstract( c.getModifiers() )
						&& Modifier.isPublic( c.getModifiers() )) seeds.add( c );
			}
		}
		assertTrue( seeds.size() + " seeds", seeds.size() > 20 );
		Blandfruit.CookFruit cook = new Blandfruit.CookFruit();
		for (Class<?> c : seeds){
			Plant.Seed seed;
			try {
				seed = (Plant.Seed) c.getDeclaredConstructor().newInstance();
			} catch (NoSuchMethodException e){
				continue;
			}
			ArrayList<Item> in = pot( seed, new Blandfruit() );
			if (cook.testIngredients( in )){
				Item out = cook.sampleOutput( in );
				assertTrue( c.getName(), out instanceof Blandfruit && ((Blandfruit) out).potionAttrib != null );
			}
		}
	}
}
