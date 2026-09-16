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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BishopSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class Bishop extends FlavorNPC {

	{
		spriteClass = BishopSprite.class;
	}

	@Override
	protected int bedtime() { return 0; }

	@Override
	protected int lineCount() { return 6; }

	public static final int TITHE = 50;
	public static final float BLESS_TURNS = 100f;

	//a tithe buys a long blessing, once a day; the price climbs with how deep you
	//have been, like everything else the town sells
	@Override
	protected void offer( final String greeting ) {
		final int price = TownLedger.scaled( TITHE );
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Bishop.this, "name" ),
						greeting + "\n\n" + Messages.get( Bishop.this, "pitch", price ),
						Messages.get( Bishop.this, "tithe", price ),
						Messages.get( Bishop.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (TownLedger.titheDay == TownLedger.today()) {
							GLog.w( Messages.get( Bishop.class, "blessed_today" ) );
							return;
						}
						if (Dungeon.gold < price) {
							GLog.w( Messages.get( Bishop.class, "no_gold" ) );
							return;
						}
						Dungeon.gold -= price;
						TownLedger.titheDay = TownLedger.today();
						TownLedger.used( TownLedger.TITHE );
						Buff.prolong( Dungeon.hero, Bless.class, BLESS_TURNS );
						GLog.p( Messages.get( Bishop.class, "blessed" ) );
					}
				} );
			}
		} );
	}

}
