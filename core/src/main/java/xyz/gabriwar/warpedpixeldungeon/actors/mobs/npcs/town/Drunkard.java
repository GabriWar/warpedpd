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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DrunkardSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTextInput;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//Remixed PD town flavour NPC, no mechanics.
public class Drunkard extends FlavorNPC {

	{
		spriteClass = DrunkardSprite.class;
	}

	@Override
	protected int bedtime() { return 260; }

	@Override
	protected int lineCount() { return 8; }

	public static final int BETS_PER_DAY = 3;
	//the coin is his, and it is not quite fair
	public static final float WIN_CHANCE = 0.45f;

	//a coin toss for any stake you name, doubled or gone. three tosses a day before
	//he loses track of whose coin it is
	@Override
	protected void offer( final String greeting ) {
		if (TownLedger.betDay != TownLedger.today()) {
			TownLedger.betDay = TownLedger.today();
			TownLedger.betsToday = 0;
		}
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				if (TownLedger.betsToday >= BETS_PER_DAY) {
					GameScene.show( new WndOptions( sprite(),
							Messages.get( Drunkard.this, "name" ),
							greeting + "\n\n" + Messages.get( Drunkard.this, "done_today" ),
							Messages.get( Drunkard.this, "bye" ) ) {
						@Override protected void onSelect( int index ) {}
					} );
					return;
				}
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Drunkard.this, "name" ),
						greeting + "\n\n" + Messages.get( Drunkard.this, "pitch" ),
						Messages.get( Drunkard.this, "bet" ),
						Messages.get( Drunkard.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index == 0) askStake();
					}
				} );
			}
		} );
	}

	private void askStake() {
		GameScene.show( new WndTextInput(
				Messages.get( this, "stake_title" ),
				Messages.get( this, "stake_body", Dungeon.gold ),
				"", 7, false,
				Messages.get( this, "toss" ),
				Messages.get( this, "bye" ) ) {
			@Override
			public void onSelect( boolean positive, String text ) {
				if (!positive) return;
				int stake;
				try {
					stake = Integer.parseInt( text.trim() );
				} catch (NumberFormatException e) {
					GLog.w( Messages.get( Drunkard.class, "not_a_number" ) );
					return;
				}
				if (stake < 1) {
					GLog.w( Messages.get( Drunkard.class, "not_a_number" ) );
					return;
				}
				if (stake > Dungeon.gold) {
					GLog.w( Messages.get( Drunkard.class, "no_gold" ) );
					return;
				}
				TownLedger.betsToday++;
				TownLedger.used( TownLedger.BET );
				if (Random.Float() < WIN_CHANCE) {
					Dungeon.gold += stake;
					Sample.INSTANCE.play( Assets.Sounds.GOLD );
					GLog.p( Messages.get( Drunkard.class, "won", stake ) );
					Badges.validateHighRoller( stake );
				} else {
					Dungeon.gold -= stake;
					GLog.n( Messages.get( Drunkard.class, "lost", stake ) );
				}
			}
		} );
	}

}
