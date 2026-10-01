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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Exotic Scroll of Multi-Upgrade. Instead of five levels on one item, every piece of
 * gear you have equipped is uncursed and upgraded twice at once.
 */
public class ScrollOfAscension extends ExoticScroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_ASCEND;
		unique = true;
	}

	@Override
	public void doRead() {
		detach( curUser.belongings.backpack );

		Item[] gear = { curUser.belongings.weapon, curUser.belongings.armor,
				curUser.belongings.artifact, curUser.belongings.misc, curUser.belongings.ring };
		int touched = 0;
		for (Item item : gear){
			if (item == null || !item.isUpgradable()) continue;
			ScrollOfRemoveCurse.uncurse( curUser, item );
			item.upgrade();
			item.upgrade();
			Badges.validateItemLevelAquired( item );
			Enchanting.show( curUser, item );
			touched++;
		}

		if (touched > 0){
			GLog.p( Messages.get( this, "ascend", touched ) );
		} else {
			GLog.w( Messages.get( this, "nothing" ) );
		}
		curUser.sprite.emitter().start( Speck.factory( Speck.UP ), 0.2f, 6 );
		SpellSprite.show( curUser, SpellSprite.CHARGE, 1f, 0.9f, 0.3f );
		Sample.INSTANCE.play( Assets.Sounds.READ );
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
		Invisibility.dispel();
		identify();
		readAnimation();
	}

	@Override
	public int value(){
		return isKnown() ? 400 * quantity : super.value();
	}
}
