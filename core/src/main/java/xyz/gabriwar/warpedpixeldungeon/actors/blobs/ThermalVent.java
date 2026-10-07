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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringSoak;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * The heat under a Thermal Spring. It marks the pool: WATER on ordinary water, VENT on the
 * vent at its heart (both over the volume under which BlobEmitter thins a cell's steam). Every turn it lays WARM degrees on each marked cell and CORE more on the
 * vent, and the temperature simulation does the rest - water holds heat well, so the
 * pool settles some twenty-five degrees over the ambient water at the rim and forty at
 * the vent, and the stone round it is warm to stand on. In winter that is the only open
 * water on the floor; in a heat wave it is a pot.
 *
 * A hero in the water is given SpringSoak. Nobody else cares.
 */
public class ThermalVent extends Blob {

	public static final int WATER = 5, VENT = 6;

	private static final float WARM = 5f;
	private static final float CORE = 10f;

	@Override
	protected void evolve(){
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
				if (cur[cell] <= 0) continue;

				TileTemperature.depositHeat( cell, cur[cell] >= VENT ? WARM + CORE : WARM );

				Char ch = Actor.findChar( cell );
				if (ch instanceof Hero && !ch.flying && Dungeon.level.water[cell]){
					Buff.affect( ch, SpringSoak.class ).steep();
				}
			}
		}
	}

	/** is this character standing in a spring's water right now? */
	public static boolean bathing( Char ch ){
		if (ch == null || ch.flying || Dungeon.level == null) return false;
		//a hot spring of the peaks has no vent: the water itself is the spring (levels/overworld/MountainSites)
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).hotSpring( ch.pos )
				&& Dungeon.level.water[ch.pos]) return true;
		ThermalVent vent = (ThermalVent) Dungeon.level.blobs.get( ThermalVent.class );
		return vent != null && vent.volume > 0 && vent.cur != null
				&& vent.cur[ch.pos] > 0 && Dungeon.level.water[ch.pos];
	}

	@Override
	public void use( BlobEmitter emitter ){
		super.use( emitter );
		emitter.pour( Speck.factory( Speck.STEAM ), 0.45f );
	}

	@Override
	public String tileDesc(){
		return Messages.get( this, "desc" );
	}
}
