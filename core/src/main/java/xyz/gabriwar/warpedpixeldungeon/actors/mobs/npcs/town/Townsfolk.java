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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.TownsfolkSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class Townsfolk extends FlavorNPC {

	{
		spriteClass = TownsfolkSprite.class;
	}

	@Override
	protected int bedtime() { return 40; }

	@Override
	protected int lineCount() { return 5; }

	//buys back what the dungeon took from the family: one weapon or armour a day,
	//upgraded at least twice, for twice what a shop would pay. a place to sell the
	//gear you have outgrown without giving it away
	@Override
	protected void offer( final String greeting ) {
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				if (TownLedger.keepsakeDay == TownLedger.today()) {
					GameScene.show( new WndOptions( sprite(),
							Messages.get( Townsfolk.this, "name" ),
							greeting + "\n\n" + Messages.get( Townsfolk.this, "bought_today" ),
							Messages.get( Townsfolk.this, "bye" ) ) {
						@Override protected void onSelect( int index ) {}
					} );
					return;
				}
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Townsfolk.this, "name" ),
						greeting + "\n\n" + Messages.get( Townsfolk.this, "pitch" ),
						Messages.get( Townsfolk.this, "sell" ),
						Messages.get( Townsfolk.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index == 0) GameScene.selectItem( keepsakeSelector );
					}
				} );
			}
		} );
	}

	private final WndBag.ItemSelector keepsakeSelector = new WndBag.ItemSelector() {
		@Override public String textPrompt() {
			return Messages.get( Townsfolk.class, "select" );
		}
		@Override public boolean itemSelectable( Item item ) {
			return (item instanceof MeleeWeapon || item instanceof Armor) && item.level() >= 2;
		}
		@Override public void onSelect( Item item ) {
			if (item == null || !itemSelectable( item )) return;
			if (item.isEquipped( Dungeon.hero )
					&& !((EquipableItem) item).doUnequip( Dungeon.hero, false )) {
				return;
			}
			int paid = 2 * item.value();
			item.detachAll( Dungeon.hero.belongings.backpack );
			Dungeon.gold += paid;
			TownLedger.keepsakeDay = TownLedger.today();
			TownLedger.used( TownLedger.KEEPSAKE );
			Sample.INSTANCE.play( Assets.Sounds.GOLD );
			GLog.p( Messages.get( Townsfolk.class, "sold", item.name(), paid ) );
		}
		@Override public Class<? extends Bag> preferredBag() { return null; }
	};

}
