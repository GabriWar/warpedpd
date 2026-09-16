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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import com.watabou.noosa.Camera;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import com.watabou.utils.Random;

public class Warbreaker extends Skill {

	{
		tag = "PB4";
		name = "Warbreaker";
		castText = "Guard broken!";
		image = 23;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy != null && enemy.isAlive()
				&& Random.Int( 100 ) < 6 * level){
			Buff.prolong( enemy, Vulnerable.class, 3 + level );
			castTextYell();
			Wound.hit( enemy );
			if (enemy.sprite != null && enemy.sprite.visible) enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 5 );
			Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 0.8f );
			Camera.main.shake( 1, 0.15f );

			//fully trained, the shattered guard leaves everyone standing beside it open too
			if (level >= MAX_LEVEL){
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar( enemy.pos + n );
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
						Buff.prolong( ch, Vulnerable.class, 3 + level );
						if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
					}
				}
			}
			return Math.round( damage * 1.3f );
		}
		return damage;
	}
}
