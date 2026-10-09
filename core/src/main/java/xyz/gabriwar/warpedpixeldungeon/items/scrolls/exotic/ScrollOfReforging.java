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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

/**
 * Exotic Scroll of Magical Infusion. Where the plain scroll upgrades a weapon or armor and
 * guarantees some enchantment, this one lets you pick the enchantment or glyph from
 * three, on top of the uncurse and the upgrade.
 */
public class ScrollOfReforging extends ExoticScroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_REFORGE;
		unique = true;
	}

	@Override
	public void doRead() {
		identify();
		GameScene.selectItem( selector );
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt(){
			return Messages.get( ScrollOfReforging.class, "inv_title" );
		}

		@Override
		public Class<? extends Bag> preferredBag(){
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ){
			return (item instanceof Weapon || item instanceof Armor) && item.isUpgradable();
		}

		@Override
		public void onSelect( final Item item ){
			if (item == null) return;
			detach( curUser.belongings.backpack );
			ScrollOfRemoveCurse.uncurse( curUser, item );
			if (item instanceof Weapon){
				Weapon wep = (Weapon) item;
				wep.upgrade( true );
				Class<? extends Weapon.Enchantment> have = wep.enchantment != null ? wep.enchantment.getClass() : null;
				final Weapon.Enchantment[] picks = {
						Weapon.Enchantment.randomCommon( have ),
						Weapon.Enchantment.randomUncommon( have ),
						Weapon.Enchantment.randomRare( have ) };
				GameScene.show( new WndOptions( new ItemSprite( ScrollOfReforging.this ), Messages.titleCase( name() ),
						Messages.get( ScrollOfReforging.class, "weapon", item.name() ),
						picks[0].name(), picks[1].name(), picks[2].name() ){
					@Override
					protected void onSelect( int index ){
						wep.enchant( picks[index] );
						finish( wep );
					}
					@Override
					public void onBackPressed(){ }
				} );
			} else {
				Armor arm = (Armor) item;
				arm.upgrade( true );
				Class<? extends Armor.Glyph> have = arm.glyph != null ? arm.glyph.getClass() : null;
				final Armor.Glyph[] picks = {
						Armor.Glyph.randomCommon( have ),
						Armor.Glyph.randomUncommon( have ),
						Armor.Glyph.randomRare( have ) };
				GameScene.show( new WndOptions( new ItemSprite( ScrollOfReforging.this ), Messages.titleCase( name() ),
						Messages.get( ScrollOfReforging.class, "armor", item.name() ),
						picks[0].name(), picks[1].name(), picks[2].name() ){
					@Override
					protected void onSelect( int index ){
						arm.inscribe( picks[index] );
						finish( arm );
					}
					@Override
					public void onBackPressed(){ }
				} );
			}
		}
	};

	private void finish( Item item ){
		GLog.p( Messages.get( ScrollOfReforging.class, "reforged", item.name() ) );
		Badges.validateItemLevelAquired( item );
		curUser.sprite.emitter().start( Speck.factory( Speck.UP ), 0.2f, 3 );
		Enchanting.show( curUser, item );
		SpatialSound.play( Assets.Sounds.READ, curUser );
		readAnimation();
	}

	@Override
	public int value(){
		return isKnown() ? 120 * quantity : super.value();
	}
}
