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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.FortuneTellerFolkSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

//Remixed PD's town fortune teller. For gold she reads an item's nature (identifies
//it), or the whole pack at once for a steeper price - the service the old plaza
//fortune teller offered, now inside her building.
public class FortuneTellerFolk extends FlavorNPC {

	private static final int COST_ONE = 50;
	private static final int COST_ALL = 40; //per item, cheaper in bulk

	{
		spriteClass = FortuneTellerFolkSprite.class;
	}

	//her sight follows the moon: the veil is thin under a full moon and the
	//reading comes cheap, under a new moon the stars hide and she charges more
	private static float moonRate() {
		if (GameCalendar.isFullMoon()) return 0.8f;
		if (GameCalendar.isNewMoon()) return 1.3f;
		return 1f;
	}

	private static String greetingKey() {
		if (GameCalendar.isFullMoon()) return "greeting_full";
		if (GameCalendar.isNewMoon()) return "greeting_new";
		return "greeting";
	}

	private static int unidentified( Hero hero ) {
		int n = 0;
		for (Item item : hero.belongings) {
			if (!item.isIdentified()) n++;
		}
		return n;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );

		if (!(c instanceof Hero)) return true;
		final Hero hero = (Hero) c;

		final int unknown = unidentified( hero );
		if (unknown == 0) {
			GameScene.show( new WndOptions( sprite(),
					Messages.get( this, "name" ),
					Messages.get( this, "nothing" ),
					Messages.get( this, "bye" ) ) {
				@Override protected void onSelect( int index ) {}
			} );
			return true;
		}

		final int oneCost = Math.round( COST_ONE * moonRate() );
		final int allCost = Math.round( COST_ALL * unknown * moonRate() );

		GameScene.show( new WndOptions( sprite(),
				Messages.get( this, "name" ),
				Messages.get( this, greetingKey(), oneCost, allCost ),
				Messages.get( this, "one", oneCost ),
				Messages.get( this, "all", allCost ),
				Messages.get( this, "bye" ) ) {

			@Override
			protected void onSelect( int index ) {
				if (index == 0) {
					if (Dungeon.gold < oneCost) {
						GLog.w( Messages.get( FortuneTellerFolk.class, "no_gold" ) );
						return;
					}
					GameScene.selectItem( new WndBag.ItemSelector() {
						@Override public String textPrompt() {
							return Messages.get( FortuneTellerFolk.class, "select" );
						}
						@Override public boolean itemSelectable( Item item ) {
							return !item.isIdentified();
						}
						@Override public void onSelect( Item item ) {
							if (item != null && !item.isIdentified()) {
								Dungeon.gold -= oneCost;
								item.identify();
								GLog.p( Messages.get( FortuneTellerFolk.class, "revealed", item.name() ) );
							}
						}
						@Override public Class<? extends Bag> preferredBag() { return null; }
					} );

				} else if (index == 1) {
					if (Dungeon.gold < allCost) {
						GLog.w( Messages.get( FortuneTellerFolk.class, "no_gold" ) );
						return;
					}
					Dungeon.gold -= allCost;
					hero.belongings.identify();
					GLog.p( Messages.get( FortuneTellerFolk.class, "revealed_all" ) );
				}
			}
		} );

		return true;
	}
}
