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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class Towel extends Item {

	public static final String AC_APPLY = "APPLY";
	public static final String AC_APPLY_PET = "APPLY_TO_PET";

	private int uses = 10;

	{
		image = ItemSpriteSheet.TOWEL_ITEM;
		unique = true;
		defaultAction = AC_APPLY;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_APPLY);
		if (findPet() != null && isPetNear()) {
			actions.add(AC_APPLY_PET);
		}
		return actions;
	}

	private PET findPet() {
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof PET) {
				return (PET) mob;
			}
		}
		return null;
	}

	private boolean isPetNear() {
		Hero hero = Dungeon.hero;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + n;
			if (Actor.findChar(c) instanceof PET) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_APPLY)) {

			Buff.detach(hero, Bleeding.class);
			Buff.detach(hero, Ooze.class);
			Dungeon.observe();

			GLog.w(Messages.get(this, "apply"));
			consumeUse();

		} else if (action.equals(AC_APPLY_PET)) {

			PET pet = findPet();
			if (pet != null) {
				Buff.detach(pet, Bleeding.class);
				Buff.detach(pet, Ooze.class);
				Dungeon.observe();

				GLog.w(Messages.get(this, "apply"));
				consumeUse();
			}
		}
	}

	private void consumeUse() {
		uses--;
		if (uses <= 0) {
			detach(Dungeon.hero.belongings.backpack);
			GLog.w(Messages.get(this, "end"));
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 500 * quantity;
	}

	private static final String USES = "uses";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(USES, uses);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		uses = bundle.getInt(USES);
	}
}
