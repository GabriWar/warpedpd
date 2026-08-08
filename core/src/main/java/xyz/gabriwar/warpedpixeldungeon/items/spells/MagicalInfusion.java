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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Degrade;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndUpgrade;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class MagicalInfusion extends InventorySpell {
	
	{
		image = ItemSpriteSheet.MAGIC_INFUSE;

		unique = true;

		talentFactor = 2;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item.isUpgradable();
	}

	@Override
	protected void onItemSelected( Item item ) {

		GameScene.show(new WndUpgrade(this, item, false));

	}

	public void reShowSelector(){
		curItem = this;
		GameScene.selectItem(itemSelector);
	}

	public WndBag.ItemSelector getSelector(){
		curItem = this;
		return itemSelector;
	}

	public void useAnimation(){
		curUser.spend(1f);
		curUser.busy();
		(curUser.sprite).operate(curUser.pos);

		Sample.INSTANCE.play(Assets.Sounds.READ);
		Invisibility.dispel();

		Catalog.countUse(curItem.getClass());
		if (Random.Float() < ((Spell) curItem).talentChance) {
			Talent.onScrollUsed(curUser, curUser.pos, ((Spell) curItem).talentFactor, getClass());
		}
	}

	public Item upgradeItem( Item item ){
		ScrollOfUpgrade.upgrade(curUser);

		Degrade.detach( curUser, Degrade.class );

		if (item instanceof Weapon && ((Weapon) item).enchantment != null) {
			item = ((Weapon) item).upgrade(true);
		} else if (item instanceof Armor && ((Armor) item).glyph != null) {
			item = ((Armor) item).upgrade(true);
		} else {
			boolean wasCursed = item.cursed;
			boolean wasCurseInfused = item instanceof Wand && ((Wand) item).curseInfusionBonus;
			item = item.upgrade();
			if (wasCursed) item.cursed = true;
			if (wasCurseInfused) ((Wand) item).curseInfusionBonus = true;
		}

		GLog.p( Messages.get(this, "infuse") );
		Badges.validateItemLevelAquired(item);

		Catalog.countUse(item.getClass());

		Statistics.upgradesUsed++;

		return item;
	}
	
	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public int energyVal() {
		return 12 * quantity;
	}
	
	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {
		
		{
			inputs =  new Class[]{ScrollOfUpgrade.class};
			inQuantity = new int[]{1};
			
			cost = 12;
			
			output = MagicalInfusion.class;
			outQuantity = 1;
		}
		
	}
}
