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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Foliage;
import xyz.gabriwar.warpedpixeldungeon.items.EasterEgg;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.SteelHoneypot;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import com.watabou.utils.Random;

import java.util.Calendar;

public class GardenRoom extends SpecialRoom {

	public void paint( Level level ) {
		
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.HIGH_GRASS );
		Painter.fill( level, this, 2, Terrain.GRASS );
		
		entrance().set( Door.Type.LOCKED );
		level.addItemToSpawn( new IronKey( Dungeon.depth ) );

		int bushes = Random.Int(3);
		if (bushes == 0) {
			level.plant(new Sungrass.Seed(), plantPos( level ));
		} else if (bushes == 1) {
			level.plant(new BlandfruitBush.Seed(), plantPos( level ));
		} else if (Random.Int(5) == 0) {
			level.plant(new Sungrass.Seed(), plantPos( level ));
			level.plant(new BlandfruitBush.Seed(), plantPos( level ));
		}
		
		// 1-in-100 chance to contain a steel honeypot (spawns a bee when shattered)
		if (Random.Int(100) == 0) {
			level.drop(new SteelHoneypot(), level.pointToCell(random()));
		}

		//Sprouted's easter egg: around Easter an egg is hidden in the garden. This is
		//the item's only source, so the in-season rate is generous enough to actually
		//be found (Sprouted's 1% made it a lottery).
		int month = Calendar.getInstance().get(Calendar.MONTH);
		if ((month == Calendar.APRIL || month == Calendar.MAY) && Random.Int(4) == 0) {
			level.drop(new EasterEgg(), level.pointToCell(random()));
		}

		//living-world twist: the garden follows the season and the dawn. Spring's
		//growth sprouts an extra seed; a rare bloom opens only at dawn.
		if (GameCalendar.season() == GameCalendar.Season.SPRING) {
			level.drop(Generator.random(Generator.Category.SEED), level.pointToCell(random()));
		}
		if (DayNightCycle.phase() == DayNightCycle.Phase.DAWN) {
			level.drop(Generator.random(Generator.Category.SEED), level.pointToCell(random()));
		}

		Foliage light = (Foliage)level.blobs.get( Foliage.class );
		if (light == null) {
			light = new Foliage();
		}
		for (int i=top + 1; i < bottom; i++) {
			for (int j=left + 1; j < right; j++) {
				light.seed( level, j + level.width() * i, 1 );
			}
		}
		level.blobs.put( Foliage.class, light );
	}
	
	private int plantPos( Level level ){
		int pos;
		do{
			pos = level.pointToCell(random());
		} while (level.plants.get(pos) != null);
		return pos;
	}
}
