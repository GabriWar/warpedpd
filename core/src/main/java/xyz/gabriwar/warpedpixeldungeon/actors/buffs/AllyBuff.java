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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

//generic class for buffs which convert an enemy into an ally
// There is a decent amount of logic that ties into this, which is why it has its own abstract class
public abstract class AllyBuff extends Buff {

	{
		revivePersists = true;
	}

	@Override
	public boolean attachTo(Char target) {
		//ally-conversion buffs don't attach to the hero, with one designed
		//exception: Corruption, which the hero can catch and cure via healing
		if (target instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				&& !(this instanceof Corruption)){
			return false;
		}
		if (super.attachTo(target)){
			target.alignment = Char.Alignment.ALLY;
			if (target.buff(PinCushion.class) != null){
				target.buff(PinCushion.class).detach();
			}
			return true;
		} else {
			return false;
		}
	}

	//for when applying an ally buff should also cause that enemy to give exp/loot as if they had died
	//consider that chars with the ally alignment do not drop items or award exp on death
	public static void affectAndLoot(Mob enemy, Hero hero, Class<?extends AllyBuff> buffCls){
		boolean wasEnemy = enemy.alignment == Char.Alignment.ENEMY || enemy instanceof Mimic;
		Buff.affect(enemy, buffCls);

		if (enemy.buff(buffCls) != null && wasEnemy){
			enemy.rollToDropLoot();

			Statistics.enemiesSlain++;
			Badges.validateMonstersSlain();
			Statistics.qualifiedForNoKilling = false;
			Bestiary.setSeen(enemy.getClass());
			Bestiary.countEncounter(enemy.getClass());

			AscensionChallenge.processEnemyKill(enemy);

			int exp = hero.lvl <= enemy.maxLvl ? enemy.EXP : 0;
			if (exp > 0) {
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.EXPERIENCE);
			}
			hero.earnExp(exp, enemy.getClass());

			if (hero.subClass == HeroSubClass.MONK){
				Buff.affect(hero, MonkEnergy.class).gainEnergy(enemy);
			}
		}
	}

}
