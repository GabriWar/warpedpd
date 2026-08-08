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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

import java.util.ArrayList;

public class AvatarOfLight extends Skill {

	{
		name = "Avatar of Light";
		castText = "Be my light!";
		tag = "A4";
		image = 148;
		tier = 4;
		mana = 15;
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
			float duration = 4 + 3 * level;

			Buff.prolong( hero, Bless.class, duration );
			Buff.prolong( hero, Haste.class, duration );
			Buff.prolong( hero, Adrenaline.class, duration );
			Buff.affect( hero, Barrier.class ).setShield( 8 + 7 * level );
			if (hero.sprite != null)
				hero.sprite.emitter().start( Speck.factory( Speck.LIGHT ), 0.3f, 12 );

			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment == Char.Alignment.ALLY && Dungeon.level.heroFOV[mob.pos]){
					Buff.prolong( mob, Bless.class, duration );
					Buff.prolong( mob, Adrenaline.class, duration );
					mob.HP = Math.min( mob.HT, mob.HP + 3 * level );
					if (mob.sprite != null)
						mob.sprite.emitter().start( Speck.factory( Speck.STAR ), 0.3f, 6 );
				}
			}

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
}
