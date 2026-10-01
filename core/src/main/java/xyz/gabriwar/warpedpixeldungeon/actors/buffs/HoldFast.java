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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class HoldFast extends Buff {

	{
		type = buffType.POSITIVE;
	}

	public int pos = -1;

	@Override
	public boolean act() {
		if (pos != target.pos) {
			detach();
		} else {
			spend(TICK);
		}
		return true;
	}

	public int armorBonus(){
		if (pos == target.pos && target instanceof Hero){
			int max = 0;
			if (((Hero) target).heroClass == HeroClass.WARRIOR){
				max = 6;
			} else {
				Armor armor = ((Hero) target).belongings.armor();
				if (armor != null) {
					max = armor.tier + armor.buffedLvl();
				}
			}
			int[] range = blockRange((Hero) target);
			if (((Hero) target).heroClass == HeroClass.WARRIOR){
				max = Math.max(max, armorMax((Hero) target) / 2);
			}
			int block = Random.NormalIntRange(range[0], range[1]);
			return Math.min(block, max);
		} else {
			detach();
			return 0;
		}
	}

	public static float buffDecayFactor(Char target){
		HoldFast buff = target.buff(HoldFast.class);
		if (buff != null && target.pos == buff.pos && target instanceof Hero){
			switch (((Hero) target).pointsInTalent(Talent.HOLD_FAST)){
				case 1:
					return 0.5f;
				case 2:
					return 0.25f;
				case 3:
					return 0;
			}

		} else if (buff != null) {
			buff.detach();
		}
		return 1;
	}

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1.9f, 2.4f, 3.25f);
	}

	private static int armorMax( Hero hero ){
		Armor a = hero.belongings.armor();
		return a == null ? 0 : a.DRMax(a.buffedLvl());
	}

	//5-10% of the armor's max block per point, at least 1-2 per point
	public static int[] blockRange( Hero hero ){
		int p = hero.pointsInTalent(Talent.HOLD_FAST);
		int dr = armorMax(hero);
		return new int[]{ Math.max(p, Math.round(dr*0.05f*p)), Math.max(2*p, Math.round(dr*0.10f*p)) };
	}

	@Override
	public String desc() {
		int[] range = blockRange(Dungeon.hero);
		return Messages.get(this, "desc",
				range[0],
				range[1],
				25 + 25*Dungeon.hero.pointsInTalent(Talent.HOLD_FAST));
	}

	private static final String POS = "pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(POS, pos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		pos = bundle.getInt(POS);
	}
}
