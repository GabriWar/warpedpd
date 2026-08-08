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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class CurseInfusion extends InventorySpell {
	
	{
		image = ItemSpriteSheet.CURSE_INFUSE;

		talentChance = 1/(float)Recipe.OUT_QUANTITY;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return ((item instanceof EquipableItem && item.isUpgradable()) || item instanceof Wand || item instanceof SpiritBow);
	}

	@Override
	protected void onItemSelected(Item item) {
		
		CellEmitter.get(curUser.pos).burst(ShadowParticle.UP, 5);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		
		item.cursed = true;
		if (item instanceof Weapon) {
			Weapon w = (Weapon) item;
			if (w.enchantment != null) {
				//if we are freshly applying curse infusion, don't replace an existing curse
				if (w.hasGoodEnchant() || w.curseInfusionBonus) {
					if (w.enchantment2 != null){
						w.enchant(Weapon.Enchantment.randomCurse(w.enchantment.getClass(), w.enchantment2.getClass()));
					} else {
						w.enchant(Weapon.Enchantment.randomCurse(w.enchantment.getClass()));
					}
				}
			} else {
				w.enchant(Weapon.Enchantment.randomCurse());
			}
			w.curseInfusionBonus = true;
			if (w instanceof MagesStaff){
				((MagesStaff) w).updateWand(true);
			}
		} else if (item instanceof Armor){
			Armor a = (Armor) item;
			if (a.glyph != null){
				//if we are freshly applying curse infusion, don't replace an existing curse
				if (a.hasGoodGlyph() || a.curseInfusionBonus) {
					if (a.glyph2 != null){
						a.inscribe(Armor.Glyph.randomCurse(a.glyph.getClass(), a.glyph2.getClass()));
					} else {
						a.inscribe(Armor.Glyph.randomCurse(a.glyph.getClass()));
					}
				}
			} else {
				a.inscribe(Armor.Glyph.randomCurse());
			}
			a.curseInfusionBonus = true;
		} else if (item instanceof Wand){
			((Wand) item).curseInfusionBonus = true;
			((Wand) item).updateLevel();
		} else if (item instanceof RingOfMight){
			curUser.updateHT(false);
		}
		Badges.validateItemLevelAquired(item);
		updateQuickslot();
	}
	
	@Override
	public int value() {
		return (int)(60 * (quantity/(float)Recipe.OUT_QUANTITY));
	}

	@Override
	public int energyVal() {
		return (int)(12 * (quantity/(float)Recipe.OUT_QUANTITY));
	}
	
	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		private static final int OUT_QUANTITY = 4;
		
		{
			inputs =  new Class[]{ScrollOfRemoveCurse.class, MetalShard.class};
			inQuantity = new int[]{1, 1};
			
			cost = 6;
			
			output = CurseInfusion.class;
			outQuantity = OUT_QUANTITY;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			Catalog.countUse(MetalShard.class);
			return super.brew(ingredients);
		}
	}
}
