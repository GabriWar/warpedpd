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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherFront;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.TownsfolkMovieSprite;

//Remixed PD town flavour NPC, no mechanics.
public class TownsfolkMovie extends FlavorNPC {

	{
		spriteClass = TownsfolkMovieSprite.class;
	}

	@Override
	protected int bedtime() { return 80; }

	@Override
	protected int lineCount() { return 6; }

	//the rumour is real: after the gossip comes the weather, read off the next front
	//in the queue - what kind it is, when it lands and how long it stays
	@Override
	protected void offer( String greeting ) {
		WeatherFront front = ClimateManager.nextFront();
		String forecast;
		if (front == null) {
			forecast = Messages.get( this, "forecast_none" );
		} else {
			float turnsPerHour = DayNightCycle.FULL_CYCLE / 24f;
			String kind = Messages.get( this, "front_" + front.type.name().toLowerCase() );
			int lasts = Math.max( 1, Math.round( front.duration / turnsPerHour ) );
			int until = Math.round( (front.arrivalTurn - Dungeon.cycleTurn) / turnsPerHour );
			if (until <= 0) {
				int left = Math.max( 1, Math.round( (front.arrivalTurn + front.duration - Dungeon.cycleTurn) / turnsPerHour ) );
				forecast = Messages.get( this, "forecast_now", kind, left );
			} else {
				forecast = Messages.get( this, "forecast_soon", kind, until, lasts );
			}
		}
		say( greeting + "\n\n" + forecast );
	}

}
