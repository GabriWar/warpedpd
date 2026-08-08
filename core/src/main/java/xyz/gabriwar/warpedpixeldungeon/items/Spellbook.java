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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.magic.ManaSpell;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfAmok;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfAmok2;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfArmor;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfBlink;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfCharm;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfCountdown;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfDeath;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfDewDraw;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfDispel;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfFirebolt;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfFirestorm;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfFright;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfGasImmunity;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfIceblast;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfIcebolt;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfIcestorm;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfLight;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfLightningblast;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfLightningbolt;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfLightningstorm;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfMoonFury;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfPoison;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfRecharge;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfRegen;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfRoot;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfSleep;
import xyz.gabriwar.warpedpixeldungeon.items.magic.SpellOfSlowing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Spellbook extends Item {

	public static final String AC_CAST = "CAST";
	public static final String AC_ADD  = "ADD_SPELL";

	public static final int NUM_SPELLS = 32;

	public boolean[] spells = new boolean[NUM_SPELLS];
	public int charge = 0;
	public int level = 1;
	public final int fullCharge = 1000;

	{
		image = ItemSpriteSheet.OTILUKES_JOURNAL;
		defaultAction = AC_CAST;
		unique = true;
	}

	private static final String SPELLS  = "spells";
	private static final String CHARGE  = "charge";
	private static final String LEVEL   = "level";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SPELLS, spells);
		bundle.put(CHARGE, charge);
		bundle.put(LEVEL, level);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
		level = bundle.getInt(LEVEL);
		boolean[] restored = bundle.getBooleanArray(SPELLS);
		if (restored != null && restored.length == NUM_SPELLS) {
			spells = restored;
		}
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_CAST);
		actions.add(AC_ADD);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_CAST)) {
			showCastMenu(hero);
		} else if (action.equals(AC_ADD)) {
			showAddMenu(hero);
		}
	}

	private void showCastMenu(Hero hero) {
		ArrayList<String> names = new ArrayList<>();
		ArrayList<Integer> indices = new ArrayList<>();

		for (int i = 0; i < NUM_SPELLS; i++) {
			if (spells[i]) {
				ManaSpell spell = createSpell(i);
				if (spell != null) {
					names.add(Messages.titleCase(spell.name()));
					indices.add(i);
				}
			}
		}

		if (names.isEmpty()) {
			GLog.w(Messages.get(this, "no_spells"));
			return;
		}

		if (hero.MP < 1) {
			GLog.w(Messages.get(this, "no_mana"));
			return;
		}

		GameScene.show(new WndOptions(
				Messages.titleCase(Messages.get(this, "name")),
				Messages.get(this, "cast_prompt", hero.MP, hero.MT),
				names.toArray(new String[0])
		) {
			@Override
			protected void onSelect(int index) {
				if (index >= 0 && index < indices.size()) {
					ManaSpell spell = createSpell(indices.get(index));
					if (spell != null) {
						spell.castWithTarget(hero);
					}
				}
			}
		});
	}

	private void showAddMenu(Hero hero) {
		// Find spell pages in inventory
		ArrayList<ManaSpell> pages = new ArrayList<>();
		for (Item item : hero.belongings.backpack) {
			if (item instanceof ManaSpell && ((ManaSpell) item).spellNum >= 0) {
				pages.add((ManaSpell) item);
			}
		}

		if (pages.isEmpty()) {
			GLog.w(Messages.get(this, "no_pages"));
			return;
		}

		ArrayList<String> names = new ArrayList<>();
		for (ManaSpell page : pages) {
			names.add(Messages.titleCase(page.name()));
		}

		GameScene.show(new WndOptions(
				Messages.titleCase(Messages.get(this, "name")),
				Messages.get(this, "add_prompt"),
				names.toArray(new String[0])
		) {
			@Override
			protected void onSelect(int index) {
				if (index >= 0 && index < pages.size()) {
					ManaSpell page = pages.get(index);
					int num = page.spellNum;
					if (num >= 0 && num < NUM_SPELLS) {
						spells[num] = true;
						page.detach(hero.belongings.backpack);
						hero.sprite.operate(hero.pos);
						hero.busy();
						hero.spend(2f);
						Sample.INSTANCE.play(Assets.Sounds.BURNING);
						hero.sprite.emitter().burst(ElmoParticle.FACTORY, 12);
						GLog.h(Messages.get(Spellbook.this, "added"));
					}
				}
			}
		});
	}

	public void learnSpell(int spellNum) {
		if (spellNum >= 0 && spellNum < NUM_SPELLS) {
			spells[spellNum] = true;
		}
	}

	public boolean hasSpell(int spellNum) {
		return spellNum >= 0 && spellNum < NUM_SPELLS && spells[spellNum];
	}

	public static ManaSpell createSpell(int num) {
		switch (num) {
			case 0:  return new SpellOfArmor();
			case 1:  return new SpellOfBlink();
			case 2:  return new SpellOfDewDraw();
			case 3:  return new SpellOfDispel();
			case 4:  return new SpellOfGasImmunity();
			case 5:  return new SpellOfHaste();
			case 6:  return new SpellOfInvisibility();
			case 7:  return new SpellOfLevitation();
			case 8:  return new SpellOfLight();
			case 9:  return new SpellOfMoonFury();
			case 10: return new SpellOfRecharge();
			case 11: return new SpellOfRegen();
			case 12: return new SpellOfRoot();
			case 13: return new SpellOfFirebolt();
			case 14: return new SpellOfFireblast();
			case 15: return new SpellOfFirestorm();
			case 16: return new SpellOfIcebolt();
			case 17: return new SpellOfIceblast();
			case 18: return new SpellOfIcestorm();
			case 19: return new SpellOfLightningbolt();
			case 20: return new SpellOfLightningblast();
			case 21: return new SpellOfLightningstorm();
			case 22: return new SpellOfMagicMissile();
			case 23: return new SpellOfPoison();
			case 24: return new SpellOfAmok();
			case 25: return new SpellOfAmok2();
			case 26: return new SpellOfCharm();
			case 27: return new SpellOfCountdown();
			case 28: return new SpellOfDeath();
			case 29: return new SpellOfFright();
			case 30: return new SpellOfSleep();
			case 31: return new SpellOfSlowing();
			default: return null;
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
		return 300 * quantity;
	}
}
