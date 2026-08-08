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


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class LastRites extends Skill {

	//flat mana requirement, and the amount spent - paying it again is the only cooldown this skill has
	public static final int MANA_THRESHOLD = 20;

	{
		name = "Last Rites";
		tag = "PA4";
		image = 154;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//hooked into incoming damage rather than onDefendProc: this runs inside Hero.damage, so the
	//blow has already lost the armour's dr and is the damage the hero is really about to take
	@Override
	public int incomingDamageReduction( int damage ){
		if (level <= 0)
			return 0;

		Hero hero = Dungeon.hero;
		if (hero == null || damage < hero.HP || hero.MP < MANA_THRESHOLD)
			return 0;

		hero.MP -= MANA_THRESHOLD;

		//the blow is cut down to leave exactly 1 HP, then the rites mend 2 per level on top of that
		int heal = 2 * level;
		int survived = Math.max( 0, hero.HP - 1 - heal );

		Buff.prolong( hero, Bless.class, 5 + 5 * level );
		if (hero.sprite != null)
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
		GLog.p( Messages.get(this, "trigger") );

		return damage - survived;
	}

	@Override
	public String info(){
		return Messages.get(this, "desc", MANA_THRESHOLD) + "\n"
				+ costUpgradeInfo();
	}
}
