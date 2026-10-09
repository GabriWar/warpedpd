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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PoacherBait;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

/**
 * Huntress: a ranged kill leaves the carcass as bait. Its scent draws in the enemies
 * that cannot see her, further each level; fully trained, a snare hides in the carcass
 * and roots the first enemy to reach it.
 */
public class Poacher extends Skill {

	{
		tag = "PA5B";
		name = "Poacher";
		image = 134;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (!ranged || level <= 0 || mob == null || Dungeon.hero == null) return;
		int cell = mob.pos;
		if (!SkillInteractions.valid( cell ) || Dungeon.level.solid[cell] || Dungeon.level.pit[cell]) return;

		//one carcass at a time: a fresh kill moves the bait
		Buff.affect( Dungeon.hero, PoacherBait.class ).set( cell, level );
		castTextYell();
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( Speck.factory( Speck.STENCH ), 6 );
			SkillInteractions.flare( cell, 0xB5D67A );
		}
		SpatialSound.play( Assets.Sounds.PUFF, cell, 0.8f, 0.8f );
	}
}
