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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Group;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;

/**
 * Visuals that unfold instead of popping: a beat of real time between two puffs, a shockwave
 * that leaves its centre one ring after another. Everything here is cosmetic and runs on the
 * hero's sprite layer; the game has already resolved whatever is being shown.
 */
public final class StaggerFX {

	private StaggerFX(){}

	public interface RingStep {
		void cell( int cell, int radius );
	}

	private static Group layer(){
		return Dungeon.hero != null && Dungeon.hero.sprite != null ? Dungeon.hero.sprite.parent : null;
	}

	/** runs fx after delay seconds of screen time; at once when there is nothing to draw on or no delay */
	public static void after( float delay, final Callback fx ){
		Group layer = layer();
		if (layer == null || delay <= 0f){
			fx.call();
			return;
		}
		final int depth = Dungeon.depth, branch = Dungeon.branch;
		layer.add( new Delayer( delay ){
			@Override
			protected void onComplete(){
				//a floor change while the beat was waiting: the show is over
				if (Dungeon.level != null && depth == Dungeon.depth && branch == Dungeon.branch) fx.call();
			}
		} );
	}

	/** every visible cell around center, ring by ring: ring r plays step * r seconds after the cast */
	public static void ring( final int center, int maxRadius, float step, final RingStep fx ){
		for (int r = 1; r <= maxRadius; r++){
			final int radius = r;
			after( step * r, () -> {
				for (int c : SkillInteractions.area( center, radius )){
					if (Dungeon.level.distance( center, c ) == radius && Dungeon.level.heroFOV[c]) fx.cell( c, radius );
				}
			} );
		}
	}
}
