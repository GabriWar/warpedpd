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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * The ward over the Pedigree Hall's three plinths. It marks the cells they stand on and
 * watches what lies on them: the moment one of the three is gone - lifted, snatched,
 * pulled away by any means at all - the other two break apart where they lie, and the
 * ward with them. One lineage, one choice.
 */
public class PedigreeWard extends Blob {

	{
		//after the hero and the mobs: a piece lifted this turn is missed this turn
		actPriority = MOB_PRIO - 1;
	}

	@Override
	protected void evolve(){
		int marked = 0, filled = 0;
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
				if (cur[cell] > 0){
					marked++;
					Heap heap = Dungeon.level.heaps.get( cell );
					if (heap != null && !heap.isEmpty()) filled++;
				}
			}
		}
		if (marked > 0 && filled < marked) shatter();
	}

	private void shatter(){
		boolean seen = false;
		for (int cell = 0; cell < off.length; cell++){
			if (off[cell] <= 0) continue;
			Heap heap = Dungeon.level.heaps.get( cell );
			if (heap != null){
				heap.destroy();
				if (Dungeon.level.heroFOV[cell]){
					seen = true;
					CellEmitter.center( cell ).burst( Speck.factory( Speck.STAR ), 8 );
					CellEmitter.get( cell ).burst( Speck.factory( Speck.DUST ), 4 );
				}
			}
			off[cell] = 0;
		}
		volume = 0;
		if (seen){
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
			GLog.w( Messages.get( this, "shattered" ) );
		}
	}
}
