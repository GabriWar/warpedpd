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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MercenarySprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

//Remixed PD town flavour NPC, no mechanics.
public class Mercenary extends FlavorNPC {

	{
		spriteClass = MercenarySprite.class;
	}

	@Override
	protected int bedtime() { return 200; }

	@Override
	protected int lineCount() { return 6; }

	public static final int LESSON_PRICE = 100;
	public static final int LESSON_POINTS = 3;

	//teaches one lesson for every boss you have put down: three skill points, at a
	//price that climbs with depth. no boss, no lesson - he only trains people who
	//have proven they come back
	@Override
	protected void offer( final String greeting ) {
		final int owed = TownLedger.bossesSlain() - TownLedger.lessonsTaken;
		final int price = TownLedger.scaled( LESSON_PRICE );
		Game.runOnRenderThread( new Callback() {
			@Override
			public void call() {
				if (owed <= 0) {
					GameScene.show( new WndOptions( sprite(),
							Messages.get( Mercenary.this, "name" ),
							greeting + "\n\n" + Messages.get( Mercenary.this, "no_lesson" ),
							Messages.get( Mercenary.this, "bye" ) ) {
						@Override protected void onSelect( int index ) {}
					} );
					return;
				}
				GameScene.show( new WndOptions( sprite(),
						Messages.get( Mercenary.this, "name" ),
						greeting + "\n\n" + Messages.get( Mercenary.this, "pitch", owed, price ),
						Messages.get( Mercenary.this, "lesson", price ),
						Messages.get( Mercenary.this, "bye" ) ) {
					@Override
					protected void onSelect( int index ) {
						if (index != 0) return;
						if (Dungeon.gold < price) {
							GLog.w( Messages.get( Mercenary.class, "no_gold" ) );
							return;
						}
						Dungeon.gold -= price;
						TownLedger.lessonsTaken++;
						TownLedger.used( TownLedger.LESSON );
						Badges.validateDrilled();
						Dungeon.hero.heroSkills.availableSkill += LESSON_POINTS;
						GLog.p( Messages.get( Mercenary.class, "taught", LESSON_POINTS ) );
					}
				} );
			}
		} );
	}

}
