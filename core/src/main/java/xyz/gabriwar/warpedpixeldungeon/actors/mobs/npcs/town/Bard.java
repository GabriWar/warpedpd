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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BardSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class Bard extends FlavorNPC {

	{
		spriteClass = BardSprite.class;
	}

	@Override
	protected int bedtime() { return 180; }

	@Override
	protected int lineCount() { return 6; }

	//a song is free and takes the edge off: half of whatever tiredness you carry,
	//once a day. the inn keeper's bed does the whole job, for a price
	@Override
	protected void offer( final String greeting ) {
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Bard.this, "name" ),
						greeting + "\n\n" + Messages.get( Bard.this, "pitch" ),
						Messages.get( Bard.this, "listen" ),
						Messages.get( Bard.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (TownLedger.songDay == TownLedger.today()) {
							GLog.w( Messages.get( Bard.class, "played_today" ) );
							return;
						}
						TownLedger.songDay = TownLedger.today();
						TownLedger.used( TownLedger.SONG );
						Sample.INSTANCE.play( Assets.Sounds.LULLABY );
						Sleepiness tired = Dungeon.hero.buff( Sleepiness.class );
						if (tired != null && tired.level() > 0) {
							tired.wake( tired.level() / 2f );
							GLog.p( Messages.get( Bard.class, "rested" ) );
						} else {
							GLog.i( Messages.get( Bard.class, "no_change" ) );
						}
					}
				} );
			}
		} );
	}

}
