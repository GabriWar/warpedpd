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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AuroraBless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.EmployeeSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class Employee extends FlavorNPC {

	{
		spriteClass = EmployeeSprite.class;
	}

	@Override
	protected int bedtime() { return 60; }

	@Override
	protected int lineCount() { return 5; }

	public static final int TICKET_PRICE = 15;
	public static final float LUCK_TURNS = 100f;

	//a ticket buys a spell of luck: better drops and a steadier hand for a while.
	//the projector hums under a full moon, and the luck lasts twice as long. each
	//show after the first costs half again more until the next day
	@Override
	protected void offer( final String greeting ) {
		if (TownLedger.showDay != TownLedger.today()) {
			TownLedger.showDay = TownLedger.today();
			TownLedger.showsToday = 0;
		}
		final int price = TownLedger.escalated( TICKET_PRICE, TownLedger.showsToday );
		final boolean fullMoon = GameCalendar.moonPhase() == GameCalendar.MoonPhase.FULL_MOON;
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Employee.this, "name" ),
						greeting + "\n\n" + Messages.get( Employee.this, fullMoon ? "pitch_moon" : "pitch", price ),
						Messages.get( Employee.this, "ticket", price ),
						Messages.get( Employee.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (Dungeon.gold < price) {
							GLog.w( Messages.get( Employee.class, "no_gold" ) );
							return;
						}
						Dungeon.gold -= price;
						TownLedger.showsToday++;
						TownLedger.used( TownLedger.SHOW );
						Buff.prolong( Dungeon.hero, AuroraBless.class, fullMoon ? LUCK_TURNS * 2 : LUCK_TURNS );
						GLog.p( Messages.get( Employee.class, fullMoon ? "watched_moon" : "watched" ) );
					}
				} );
			}
		} );
	}

}
