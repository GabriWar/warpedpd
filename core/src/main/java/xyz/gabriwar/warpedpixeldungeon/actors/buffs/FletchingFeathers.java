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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Fletching;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit;

/** Fletching's ring: the feathers plucked by ranged hits, circling the hero until they burst. */
public class FletchingFeathers extends Buff {

	public static final int MAX = 3;

	private int count = 0;
	private Ring visual;

	{
		type = buffType.POSITIVE;
	}

	public boolean full(){
		return count >= MAX;
	}

	public void pluck(){
		if (full()) return;
		count++;
		if (target != null && target.sprite != null){
			target.sprite.emitter().burst( Speck.factory( Speck.WOOL ), 2 );
		}
		SpatialSound.play( Assets.Sounds.MISS, target, 0.5f, 1.8f );
	}

	public void empty(){
		count = 0;
	}

	//only Fletching bursts the feathers: once it is gone (taken away in the debug window) they go too
	@Override
	public boolean act(){
		if (CurrentSkills.skillLevel( target, Fletching.class ) <= 0){
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (visual != null){
			Fx.dispose( visual );
			visual = null;
		}
		if (on && target != null && target.sprite != null && target.sprite.parent != null){
			visual = new Ring();
			target.sprite.parent.add( visual );
		}
	}

	/** up to three pale feathers circling the hero in the kit's FEATHERS orbit; none while the ring is empty */
	private class Ring extends SkillOrbit {

		Ring(){
			super( target, FxOrbit.Slot.FEATHERS );
		}

		@Override
		protected int count(){
			return FletchingFeathers.this.count;
		}

		@Override
		protected void look( FxOrbit.Ring ring, int n ){
			ring.motes( n, FxFrames.FEATHER, 0xF3EBD7, false ).trail( n <= 2 ? 1 : 0 ).breath( 0.75f, 0.95f, 0.8f ).speed( 2.2f );
		}
	}

	private static final String COUNT = "count";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( COUNT, count );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		count = Math.min( MAX, bundle.getInt( COUNT ) );
	}
}
