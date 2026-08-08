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

package xyz.gabriwar.warpedpixeldungeon.levels.traps;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class ToxicTrap extends Trap{

	{
		color = GREEN;
		shape = GRILL;
	}

	@Override
	public void activate() {

		GameScene.add( Blob.seed( pos, 300 + 20 * scalingDepth(), ToxicGas.class ) );
		Sample.INSTANCE.play(Assets.Sounds.GAS);

		for( int i : PathFinder.NEIGHBOURS9) {
			if (Actor.findChar(pos+i) instanceof Mob){
				Buff.prolong(Actor.findChar(pos+i), Trap.HazardAssistTracker.class, HazardAssistTracker.DURATION);
			}
		}

		//once per trigger, not once per gas tick — ToxicGas lingers for many turns
		Heap heap = Dungeon.level.heaps.get(pos);
		if (heap != null) heap.poison();
	}
}
