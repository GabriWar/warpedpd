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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.sprites.SquidSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: a heavier deep-water ambusher. Like a piranha it
//suffocates out of water, but it hits far harder and is slower to be provoked.
public class Squid extends Mob {

	{
		spriteClass = SquidSprite.class;

		baseSpeed = 2f;
		EXP = 0;

		loot = MysteryMeat.class;
		lootChance = 1f;

		state = SLEEPING;
	}

	public Squid() {
		super();
		HP = HT = 18 + Dungeon.depth * 6;
		defenseSkill = 14 + Dungeon.depth * 2;
	}

	@Override
	protected boolean act() {
		if (!Dungeon.level.water[pos] || flying) {
			if (sprite != null && buff(Levitation.class) != null){
				sprite.emitter().burst(Speck.factory( Speck.JET ), 10);
			}
			dieOnLand();
			return true;
		} else {
			return super.act();
		}
	}

	public void dieOnLand(){
		die( null );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( Dungeon.depth * 2, 4 + Dungeon.depth * 3 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 24 + Dungeon.depth * 2;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, Dungeon.depth );
	}

	@Override
	public boolean reset() {
		state = WANDERING;
		return true;
	}
}
