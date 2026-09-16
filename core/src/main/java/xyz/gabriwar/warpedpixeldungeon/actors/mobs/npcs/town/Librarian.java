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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.items.DiaryPage;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.LibrarianSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//Remixed PD town flavour NPC, no mechanics.
public class Librarian extends FlavorNPC {

	{
		spriteClass = LibrarianSprite.class;
	}

	@Override
	protected int bedtime() { return 20; }

	@Override
	protected int lineCount() { return 6; }

	public static final int PAGE_PRICE = 20;

	//sells a page from a miner's diary: it maps one floor, chosen from the three
	//just below the deepest you have reached. cheaper than a scroll because it only
	//works there; each page costs half again more than the last, for the whole run
	@Override
	protected void offer( final String greeting ) {
		final int price = TownLedger.escalated( PAGE_PRICE, TownLedger.pagesBought );
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Librarian.this, "name" ),
						greeting + "\n\n" + Messages.get( Librarian.this, "pitch", price ),
						Messages.get( Librarian.this, "buy", price ),
						Messages.get( Librarian.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (Dungeon.gold < price) {
							GLog.w( Messages.get( Librarian.class, "no_gold" ) );
							return;
						}
						Dungeon.gold -= price;
						TownLedger.pagesBought++;
						TownLedger.used( TownLedger.PAGE );
						DiaryPage page = new DiaryPage();
						page.depth = Math.min( Statistics.deepestFloor + Random.IntRange( 1, 3 ), 25 );
						if (!page.collect()) {
							Dungeon.level.drop( page, Dungeon.hero.pos ).sprite.drop();
						}
						GLog.p( Messages.get( Librarian.class, "bought", page.depth ) );
					}
				} );
			}
		} );
	}

}
