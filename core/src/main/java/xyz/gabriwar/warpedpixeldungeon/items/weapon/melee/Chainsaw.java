/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Chainsaw extends MeleeWeapon {

	{
		image = ItemSpriteSheet.CHAINSAW;
		tier = 5;
		DLY = 0.75f;
		ACC = 1.2f;

		bones = false;
		unique = true;
		cursed = true;
	}

	public boolean turnedOn = false;

	public static final String AC_ON  = "ON";
	public static final String AC_OFF = "OFF";

	@Override
	public int min(int lvl) { return 1 + lvl; }

	@Override
	public int max(int lvl) { return 12 + lvl * 6; }

	@Override
	public int STRReq(int lvl) { return 16 - lvl; }

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) {
			if (turnedOn) {
				actions.add(AC_OFF);
			} else {
				actions.add(AC_ON);
			}
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_ON)) {
			turnedOn = true;
			GLog.i(Messages.get(this, "turn_on"));
			hero.next();
		} else if (action.equals(AC_OFF)) {
			turnedOn = false;
			GLog.i(Messages.get(this, "turn_off"));
			hero.next();
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (enchantment == null) {
			enchant(new BuzzSaw());
		}
		if (defender instanceof Gullin || defender instanceof Kupua
				|| defender instanceof MineSentinel || defender instanceof Otiluke
				|| defender instanceof Zot || defender instanceof ZotPhase) {
			defender.damage(Random.Int(damage, damage * 4), this);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private static final String TURNED_ON = "turnedOn";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TURNED_ON, turnedOn);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		turnedOn = bundle.getBoolean(TURNED_ON);
	}

	//Sprouted weapon: no Duelist ability was ever designed for it
	@Override
	public boolean hasDuelistAbility() {
		return false;
	}
}
