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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.InnKeeperSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

//Remixed PD's inn keeper. Rents a bed: for a few coins the hero sleeps until the
//next dawn, waking rested, part-healed and dried off. The one townsperson who is
//up all night - the inn is where you go when everything else is shut.
public class InnKeeper extends FlavorNPC {

	public static final int BED_PRICE = 30;

	{
		spriteClass = InnKeeperSprite.class;
	}

	@Override
	protected boolean sleepsAtNight() { return false; }

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		//renting a bed moves the world clock (Dungeon.cycleTurn) to the next dawn, and
		//that clock is host-authoritative and shared by the whole party — one player
		//can't skip a night out from under everyone else's evening. so in multiplayer
		//the bed is the host's to rent; a remote player is told so plainly instead of
		//getting a dead click
		if (xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.handleNetHero( c,
				Messages.get( this, "net_host_only" ) )) {
			return true;
		}
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;

		GameScene.show( new WndOptions( sprite(),
				Messages.get( this, "name" ),
				Messages.get( this, "greet" ),
				Messages.get( this, "rent", BED_PRICE ),
				Messages.get( this, "looking" ) ) {
			@Override
			protected void onSelect( int index ) {
				if (index != 0) return;
				if (Dungeon.gold < BED_PRICE) {
					GLog.w( Messages.get( InnKeeper.class, "no_gold" ) );
					return;
				}
				Dungeon.gold -= BED_PRICE;
				rest( hero );
			}
		} );
		return true;
	}

	//a night's sleep: tiredness gone, a third of max HP back, the weather shaken
	//off, and the clock moved on to the coming dawn
	private static void rest( Hero hero ) {
		Sleepiness tired = hero.buff( Sleepiness.class );
		if (tired != null) tired.wake( tired.level() );
		Buff.detach( hero, Hypothermia.class );
		Buff.detach( hero, Heatstroke.class );
		Buff.detach( hero, SoakedShoes.class );
		Buff.detach( hero, Windswept.class );
		hero.heal( hero.HT / 3 );

		GameScene.flash( 0xFF000000, false );

		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )) {
			//the day follows the real clock: no skipping, just the rest
			GLog.p( Messages.get( InnKeeper.class, "slept_clock" ) );
			return;
		}

		//jump the day clock to the start of the next dawn. the phase-change message,
		//fog refresh and new-day log fire on the hero's next turn as they normally
		//would; the weather is resimulated once for the new hour. the game clock
		//(Statistics.duration, which the shop and farm timers read) moves with it,
		//so the night really passes for the rest of the world too
		int skipped = DayNightCycle.FULL_CYCLE - Dungeon.cycleTurn % DayNightCycle.FULL_CYCLE;
		Dungeon.cycleTurn += skipped;
		Statistics.duration += skipped;
		ClimateManager.onHeroTurn();

		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()
				&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.getPlayerCount() > 0) {
			//the whole party's clock moved with the host's. say so, or the others
			//just watch the sky snap to dawn with no explanation
			GLog.p( Messages.get( InnKeeper.class, "slept_party" ) );
		} else {
			GLog.p( Messages.get( InnKeeper.class, "slept" ) );
		}
	}

}
