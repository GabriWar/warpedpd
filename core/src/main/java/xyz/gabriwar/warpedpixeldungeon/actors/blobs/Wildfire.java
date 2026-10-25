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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WildfireEvent;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.Arrays;

/**
 * A wildfire's flames on the surface's window (levels/overworld/WildfireEvent): a fire's look and
 * its harm - whoever stands in them burns, what lies there burns, a plant withers, the air round
 * them warms - but none of its spreading, which is the wildfire's own reckoning (WildfireEvent.
 * Blaze): every turn the cells alight are worked out again from it, so the flames follow the fire
 * wherever the window lies and never run past it. Water on a burning cell (PotionOfWater) or a
 * frost over it puts it out there and keeps the fire from it and its four sides (WildfireEvent.
 * doused). A co-op guest's mirror shows the flames its host ships, as it does any blob's.
 */
public class Wildfire extends Fire {

	@Override
	protected void evolve(){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror()){
			//nothing to work out here: what is shown stays as it was shipped
			System.arraycopy( cur, 0, off, 0, cur.length );
			for (int v : cur) volume += v;
			return;
		}
		OverworldLevel ow = (OverworldLevel) level;
		int[] alight = WildfireEvent.burning( ow, ow.eventTurn() );
		Arrays.fill( off, 0 );
		area.setEmpty();
		Freezing freeze = (Freezing) level.blobs.get( Freezing.class );
		int w = level.width();
		for (int k = 0; k + 1 < alight.length; k += 2){
			int c = alight[k];
			//a frost over the flames puts them out, as water does
			if (freeze != null && freeze.volume > 0 && freeze.cur[c] > 0){
				WildfireEvent.doused( level, c );
				continue;
			}
			off[c] = alight[k + 1];
			volume += off[c];
			area.union( c % w, c / w );
			TileTemperature.applyFireHeat( c );
			burn( c );
		}
	}

	@Override
	public void clear( int cell ){
		super.clear( cell );
		WildfireEvent.doused( Dungeon.level, cell );
	}

	@Override
	public String tileDesc(){
		return Messages.get( this, "desc" );
	}
}
