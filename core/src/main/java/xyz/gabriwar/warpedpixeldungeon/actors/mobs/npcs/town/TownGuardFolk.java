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

import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.TownGuardFolkSprite;
import com.watabou.utils.Random;

//Remixed PD town flavour NPC, no mechanics.
public class TownGuardFolk extends FlavorNPC {

	{
		spriteClass = TownGuardFolkSprite.class;
	}

	@Override
	protected int bedtime() { return 120; }

	@Override
	protected int lineCount() { return 5; }

	//posts one bounty a day on a creature from just below the deepest floor you have
	//reached. the reward is three times the gold such a floor drops in a pile, and it
	//lands at the creature's feet when the hero kills it
	@Override
	protected void offer( String greeting ) {
		int today = TownLedger.today();
		if (TownLedger.bountyTarget == null && TownLedger.bountyDay != today) {
			int depth = Math.min( Statistics.deepestFloor + 1, 25 );
			TownLedger.bountyTarget = Random.element( MobSpawner.getMobRotation( depth ) );
			TownLedger.bountyReward = 3 * (45 + 15 * depth);
			TownLedger.bountyDay = today;
			say( greeting + "\n\n" + Messages.get( this, "bounty_new",
					Messages.get( TownLedger.bountyTarget, "name" ), TownLedger.bountyReward ) );
		} else if (TownLedger.bountyTarget != null) {
			say( greeting + "\n\n" + Messages.get( this, "bounty_open",
					Messages.get( TownLedger.bountyTarget, "name" ), TownLedger.bountyReward ) );
		} else {
			say( greeting + "\n\n" + Messages.get( this, "bounty_done" ) );
		}
	}

}
