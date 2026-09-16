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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class PyreAffinity extends Skill {

	{
		tag = "D4A";
		name = "Pyromancy";
		tier = 4;
		image = 36;
		level = 0;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//wand zaps and bolt spells that hurt an enemy can set it alight
	@Override
	public void onMagicDamage( Char target, int damage, Object source ){
		if (level <= 0 || target == null || !target.isAlive() || Random.Int(100) >= 12 * level) return;
		Buff.affect( target, Burning.class ).reignite( target );
		if (target.sprite != null)
			target.sprite.emitter().burst( FlameParticle.FACTORY, 3 + level );
		Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.6f, 1.2f );
		//at mastery the flames leap to one enemy standing next to the target
		if (level >= MAX_LEVEL){
			for (int n : PathFinder.NEIGHBOURS8){
				Char near = Actor.findChar( target.pos + n );
				if (near != null && near != target && near.isAlive() && near.alignment == Char.Alignment.ENEMY){
					Buff.affect( near, Burning.class ).reignite( near );
					if (near.sprite != null)
						near.sprite.emitter().burst( FlameParticle.FACTORY, 6 );
					break;
				}
			}
		}
	}

	@Override
	public String info(){
		return Messages.get(this, "desc", 12 * Math.max(1, level)) + "\n"
				+ costUpgradeInfo();
	}
}
