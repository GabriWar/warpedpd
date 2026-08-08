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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;

public class FlashingTrap extends Trap {

	{
		color = GREY;
		shape = STARS;

		disarmedByActivation = false;
		avoidsHallways = true;
	}

	@Override
	public void activate() {
		
		Char c = Actor.findChar( pos );
		
		if (c != null) {
			int damage = Math.max( 0,  (4 + scalingDepth()/2) - c.drRoll()/2 );
			Buff.affect( c, Bleeding.class ).set( damage );
			Buff.prolong( c, Blindness.class, Blindness.DURATION );
			Buff.prolong( c, Cripple.class, Cripple.DURATION*2f );
			
			if (c instanceof Mob) {
				Buff.prolong(c, Trap.HazardAssistTracker.class, HazardAssistTracker.DURATION);
				if (((Mob)c).state == ((Mob)c).HUNTING) ((Mob)c).state = ((Mob)c).WANDERING;
				((Mob)c).beckon( Dungeon.level.randomDestination( c ) );
			}
		}
		
		if (Dungeon.level.heroFOV[pos]) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play( Assets.Sounds.BLAST );
		}
		
	}

}
