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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlacksmithSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBlacksmith2;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

public class Blacksmith2 extends NPC {

	{
		spriteClass = BlacksmithSprite.class;

		properties.add( Property.IMMOVABLE );
	}

	@Override
	public boolean interact( Char c ) {

		sprite.turnTo( pos, c.pos );

		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.handleNetHero(c, description())) return true;
		if (c != Dungeon.hero) {
			return true;
		}
		if (Shopkeeper.closedForNight()) {
			tell( Messages.get(this, "asleep") );
			return true;
		}

		DarkGold gold = Dungeon.hero.belongings.getItem( DarkGold.class );
		if (!checkAdamant()) {
			tell( Messages.get(this, "no_adamant") );
		} else if (gold == null || gold.quantity() < 50) {
			tell( Messages.get(this, "need_gold") );
		} else {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndBlacksmith2( Blacksmith2.this, Dungeon.hero ) );
				}
			});
		}

		return true;
	}

	public static String verify( Item item1, Item item2 ) {

		if (item1 == item2) {
			return Messages.get(Blacksmith2.class, "same_item");
		}

		if (!item1.isIdentified()) {
			return Messages.get(Blacksmith2.class, "not_identified");
		}

		if (item1.cursed) {
			return Messages.get(Blacksmith2.class, "cursed");
		}

		if (item1.reinforced) {
			return Messages.get(Blacksmith2.class, "already_reinforced");
		}

		if (item1.level() < 0) {
			return Messages.get(Blacksmith2.class, "too_poor");
		}

		if (!item1.isUpgradable()) {
			return Messages.get(Blacksmith2.class, "cant_reinforce");
		}

		if (item1 instanceof Armor && item2 instanceof AdamantArmor) {
			return null;
		}

		if (item1 instanceof MeleeWeapon && item2 instanceof AdamantWeapon) {
			return null;
		}

		if (item1 instanceof Wand && item2 instanceof AdamantWand) {
			return null;
		}

		if (item1 instanceof Ring && item2 instanceof AdamantRing) {
			return null;
		}

		return Messages.get(Blacksmith2.class, "wrong_match");
	}

	public static void upgrade( Item item1, Item item2 ) {

		item1.reinforced = true;
		item2.detach( Dungeon.hero.belongings.backpack );

		DarkGold gold = Dungeon.hero.belongings.getItem( DarkGold.class );
		if (gold != null && gold.quantity() >= 50) {
			if (gold.quantity() == 50) {
				gold.detachAll( Dungeon.hero.belongings.backpack );
			} else {
				gold.quantity( gold.quantity() - 50 );
			}
		}

		GLog.p( Messages.get(Blacksmith2.class, "looks_better", item1.name()) );
		Dungeon.hero.spendAndNext( 2f );
		Badges.validateItemLevelAquired( item1 );
	}

	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( Blacksmith2.this, text ) );
			}
		});
	}

	public static boolean checkAdamant() {
		AdamantArmor armor = Dungeon.hero.belongings.getItem( AdamantArmor.class );
		AdamantWeapon weapon = Dungeon.hero.belongings.getItem( AdamantWeapon.class );
		AdamantRing ring = Dungeon.hero.belongings.getItem( AdamantRing.class );
		AdamantWand wand = Dungeon.hero.belongings.getItem( AdamantWand.class );

		return armor != null || weapon != null || ring != null || wand != null;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}
}
