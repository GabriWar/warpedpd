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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import com.watabou.utils.Random;

public class Scorpion extends PassiveSkillB2 {

	{
		name = "Scorpion";
		image = 58;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int venomBonus(){ return level * 2; }

	//the sting: hits on a poisoned enemy may cripple it; at mastery a sting on
	//a mark that is already crippled pins it in place
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && enemy != null && enemy.isAlive() && enemy.buff( Poison.class ) != null
				&& Random.Int( 100 ) < 10 * level){
			if (level >= Skill.MAX_LEVEL && enemy.buff( Cripple.class ) != null){
				SkillInteractions.affectAfterHit( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class, 1f );
				CellEmitter.center( enemy.pos ).burst( Speck.factory( Speck.STAR ), 4 );
			}
			Buff.prolong( enemy, Cripple.class, 2 + level );
			CellEmitter.center( enemy.pos ).burst( PoisonParticle.SPLASH, 5 );
			Sample.INSTANCE.play( Assets.Sounds.DEBUFF, 0.8f, 0.8f );
		}
		return damage;
	}
}
