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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeatAura;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.InnServantSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class InnServant extends FlavorNPC {

	{
		spriteClass = InnServantSprite.class;
	}

	@Override
	protected int bedtime() { return 150; }

	@Override
	protected int lineCount() { return 5; }

	public static final int MEAL_PRICE = 10;
	public static final float WARMTH_TURNS = 30f;

	//a hot meal: a ration's worth of food and a spell of warmth against the cold.
	//the first plate of the day is cheap; every plate after costs half again more,
	//and the kitchen starts over at dawn
	@Override
	protected void offer( final String greeting ) {
		if (TownLedger.mealDay != TownLedger.today()) {
			TownLedger.mealDay = TownLedger.today();
			TownLedger.mealsToday = 0;
		}
		final int price = TownLedger.escalated( MEAL_PRICE, TownLedger.mealsToday );
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(),
						Messages.get( InnServant.this, "name" ),
						greeting + "\n\n" + Messages.get( InnServant.this, "pitch", price ),
						Messages.get( InnServant.this, "meal", price ),
						Messages.get( InnServant.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (Dungeon.gold < price) {
							GLog.w( Messages.get( InnServant.class, "no_gold" ) );
							return;
						}
						Dungeon.gold -= price;
						TownLedger.mealsToday++;
						TownLedger.used( TownLedger.MEAL );
						Buff.affect( Dungeon.hero, Hunger.class ).satisfy( Hunger.HUNGRY );
						Buff.prolong( Dungeon.hero, HeatAura.class, WARMTH_TURNS );
						Sample.INSTANCE.play( Assets.Sounds.EAT );
						GLog.p( Messages.get( InnServant.class, "fed" ) );
					}
				} );
			}
		} );
	}

}
