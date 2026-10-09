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
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Random;

public class Venom extends PassiveSkillB1 {

	{
		name = "Venom";
		castText = "Poison is my specialty";
		image = 57;
		tier = 1;
	}

	@Override
	public boolean venomousAttack(){
		if (Random.Int(100) < 10 * level){
			castTextYell();
			//venom seen running off the blade
			if (Dungeon.hero != null && Dungeon.hero.sprite != null){
				Dungeon.hero.sprite.emitter().burst( PoisonParticle.MISSILE, 4 );
			}
			Sample.INSTANCE.play( Assets.Sounds.DEBUFF, 0.8f, 1.2f );
			return true;
		}
		return false;
	}

	//at mastery a blow the enemy never saw coming is always envenomed
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level >= Skill.MAX_LEVEL && !ranged && enemy instanceof Mob && enemy.isAlive()
				&& ((Mob) enemy).surprisedBy( Dungeon.hero )){
			Buff.affect( enemy, Poison.class ).set( 2 + level + Dungeon.hero.heroSkills.allVenomBonus() );
			CellEmitter.center( enemy.pos ).burst( PoisonParticle.SPLASH, 6 );
			SpatialSound.play( Assets.Sounds.DEBUFF, enemy, 0.8f, 1.2f );
		}
		return damage;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
