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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;

public class Reckoning extends Skill {

	public static final int RANGE = 5;

	{
		name = "Reckoning";
		castText = "Judgement!";
		tag = "D3";
		image = 155;
		tier = 3;
		mana = 14;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment != Char.Alignment.ENEMY
						|| !Dungeon.level.heroFOV[mob.pos]
						|| Dungeon.level.distance( hero.pos, mob.pos ) > RANGE)
					continue;

				CellEmitter.get( mob.pos ).burst( Speck.factory( Speck.LIGHT ), 4 );
				Buff.prolong( mob, Vulnerable.class, 4 );

				//only the unholy are judged: they alone are broken and burned
				if (Char.hasProp( mob, Char.Property.UNDEAD ) || Char.hasProp( mob, Char.Property.DEMONIC )){
					Buff.prolong( mob, Terror.class, 3 + 2 * level ).object = hero.id();
					mob.damage( 4 + 4 * level, this );
				}
			}

			if (hero.sprite != null)
				hero.sprite.emitter().start( Speck.factory( Speck.STAR ), 0.3f, 10 );

			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public String info(){
		return Messages.get(this, "desc", RANGE) + "\n"
				+ costUpgradeInfo();
	}
}
