/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;

public class EnchantmentInfusion extends InventorySpell {

	{
		image = ItemSpriteSheet.ENCHANT_INFUSE;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item instanceof Weapon || item instanceof Armor;
	}

	@Override
	protected void onItemSelected(final Item item) {
		if (item instanceof Weapon) {
			final Weapon.Enchantment[] enchants = new Weapon.Enchantment[5];
			Class<? extends Weapon.Enchantment> existing = ((Weapon) item).enchantment != null
					? ((Weapon) item).enchantment.getClass() : null;
			Class<? extends Weapon.Enchantment> existing2 = ((Weapon) item).enchantment2 != null
					? ((Weapon) item).enchantment2.getClass() : null;
			enchants[0] = Weapon.Enchantment.randomCommon(existing, existing2);
			enchants[1] = Weapon.Enchantment.randomUncommon(existing, existing2);
			enchants[2] = Weapon.Enchantment.random(existing, existing2, enchants[0].getClass(), enchants[1].getClass());
			enchants[3] = Weapon.Enchantment.random(existing, existing2, enchants[0].getClass(), enchants[1].getClass(), enchants[2].getClass());
			enchants[4] = Weapon.Enchantment.random(existing, existing2, enchants[0].getClass(), enchants[1].getClass(), enchants[2].getClass(), enchants[3].getClass());

			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(ScrollOfEnchantment.class, "weapon")
							+ "\n\n" + Messages.get(ScrollOfEnchantment.class, "cancel_warn"),
					enchants[0].name(), enchants[1].name(), enchants[2].name(),
					enchants[3].name(), enchants[4].name(),
					Messages.get(ScrollOfEnchantment.class, "cancel")) {
				@Override
				protected void onSelect(int index) {
					if (index < 5) {
						((Weapon) item).enchant(enchants[index]);
						GLog.p(Messages.get(StoneOfEnchantment.class, "weapon"));
						Sample.INSTANCE.play(Assets.Sounds.READ);
						Invisibility.dispel();
						Enchanting.show(curUser, item);
					}
				}
				@Override
				public void onBackPressed() {
					// must make a choice
				}
			});

		} else if (item instanceof Armor) {
			final Armor.Glyph[] glyphs = new Armor.Glyph[5];
			Class<? extends Armor.Glyph> existing = ((Armor) item).glyph != null
					? ((Armor) item).glyph.getClass() : null;
			Class<? extends Armor.Glyph> existing2 = ((Armor) item).glyph2 != null
					? ((Armor) item).glyph2.getClass() : null;
			glyphs[0] = Armor.Glyph.randomCommon(existing, existing2);
			glyphs[1] = Armor.Glyph.randomUncommon(existing, existing2);
			glyphs[2] = Armor.Glyph.random(existing, existing2, glyphs[0].getClass(), glyphs[1].getClass());
			glyphs[3] = Armor.Glyph.random(existing, existing2, glyphs[0].getClass(), glyphs[1].getClass(), glyphs[2].getClass());
			glyphs[4] = Armor.Glyph.random(existing, existing2, glyphs[0].getClass(), glyphs[1].getClass(), glyphs[2].getClass(), glyphs[3].getClass());

			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(ScrollOfEnchantment.class, "armor")
							+ "\n\n" + Messages.get(ScrollOfEnchantment.class, "cancel_warn"),
					glyphs[0].name(), glyphs[1].name(), glyphs[2].name(),
					glyphs[3].name(), glyphs[4].name(),
					Messages.get(ScrollOfEnchantment.class, "cancel")) {
				@Override
				protected void onSelect(int index) {
					if (index < 5) {
						((Armor) item).inscribe(glyphs[index]);
						GLog.p(Messages.get(StoneOfEnchantment.class, "armor"));
						Sample.INSTANCE.play(Assets.Sounds.READ);
						Invisibility.dispel();
						Enchanting.show(curUser, item);
					}
				}
				@Override
				public void onBackPressed() {
					// must make a choice
				}
			});
		}
	}

	@Override
	public int value() {
		return Math.round(quantity * ((30 + 30) / 2f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 2;

		{
			inputs     = new Class[]{ScrollOfEnchantment.class, ScrollOfIdentify.class};
			inQuantity = new int[]{1, 1};
			cost       = 1;
			output     = EnchantmentInfusion.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}
