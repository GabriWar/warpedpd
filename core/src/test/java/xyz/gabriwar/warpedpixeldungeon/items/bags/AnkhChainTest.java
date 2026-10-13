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

package xyz.gabriwar.warpedpixeldungeon.items.bags;

import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.items.AncientCoin;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfDead;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfTranscendence;
import xyz.gabriwar.warpedpixeldungeon.items.Bone;
import xyz.gabriwar.warpedpixeldungeon.items.ConchShell;
import xyz.gabriwar.warpedpixeldungeon.items.ReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.LloydsBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.quest.EscapeCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.CavesKey;
import xyz.gabriwar.warpedpixeldungeon.items.CityKey;
import xyz.gabriwar.warpedpixeldungeon.items.HallsKey;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.PrisonKey;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnkhChainTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void holdsEveryKindOfKey(){
		AnkhChain chain = new AnkhChain();
		for (Item key : new Item[]{ new SewersKey(), new PrisonKey(), new CavesKey(), new CityKey(),
				new HallsKey(), new TenguKey(), new IronKey(), new SkeletonKey() }){
			assertTrue( key.getClass().getSimpleName(), chain.canHold( key ) );
		}
		for (Item portal : new Item[]{ new BookOfDead(), new BookOfLife(), new BookOfTranscendence(),
				new AncientCoin(), new Bone(), new ConchShell(), new SanChikarah(), new ReturnBeacon(),
				new LloydsBeacon(), new EscapeCrystal() }){
			assertTrue( portal.getClass().getSimpleName(), chain.canHold( portal ) );
		}
		assertTrue( chain.canHold( new Ankh() ) );
		assertFalse( chain.canHold( new Food() ) );
	}

	@Test
	public void mrDestructoGoesInTheScrollHolderAndChestsInTheSatchel(){
		ScrollHolder holder = new ScrollHolder();
		for (Item destructo : new Item[]{ new xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo(),
				new xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo(),
				new xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo2(),
				new xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo2() }){
			assertTrue( destructo.getClass().getSimpleName(), holder.canHold( destructo ) );
		}
		assertTrue( new BuildersSatchel().canHold( new xyz.gabriwar.warpedpixeldungeon.items.PortableChest() ) );
		assertFalse( new BuildersSatchel().canHold( new Food() ) );
	}
}
