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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.utils.Random;

public class Demoralize extends Skill {

	{
		tag = "D2";
		name = "Demoralize";
		castText = "Break their nerve";
		image = 6;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive())
			return damage;

		if (Random.Int( 100 ) < 10 * level){
			Buff.prolong( enemy, Weakness.class, 4 + 2 * level );
			if (enemy.sprite != null){
				enemy.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 3 );
			}
			//+3: the fear spreads to every enemy close by
			if (level >= MAX_LEVEL){
				for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )){
					if (mob == enemy || mob.alignment != Char.Alignment.ENEMY || !mob.isAlive()
							|| Dungeon.level.distance( enemy.pos, mob.pos ) > 2) continue;
					Buff.prolong( mob, Weakness.class, 4 + 2 * level );
					if (mob.sprite != null && mob.sprite.visible){
						mob.sprite.emitter().burst( Speck.factory( Speck.SCREAM ), 2 );
					}
				}
			}
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 0.8f, 0.6f );
		}

		return damage;
	}
}
