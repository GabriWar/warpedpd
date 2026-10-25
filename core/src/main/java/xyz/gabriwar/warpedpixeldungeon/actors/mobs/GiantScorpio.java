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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.sprites.GiantScorpioSprite;
import com.watabou.utils.Random;

/**
 * A giant scorpion with a name of its own, roaming the desert for days
 * (levels/overworld/RoamerEvent): a scorpio grown huge, four times as hardy, its stings from afar
 * harder, crippling as a scorpio's do and poisoning besides (one in three). Out of a fight it keeps
 * to its haunt of the hour; killed, it leaves its trophy, a purse and a good item.
 */
public class GiantScorpio extends Scorpio {

	{
		spriteClass = GiantScorpioSprite.class;

		HP = HT = 440;
		defenseSkill = 30;

		EXP = 44;
		maxLvl = 36;

		state = WANDERING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 24, 62 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 44;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 3 ) == 0) Buff.affect( enemy, Poison.class ).set( 6 );
		return damage;
	}

	@Override
	public String name() {
		return RoamerEvent.title( this, super.name() );
	}

	//out of a fight it keeps to its haunt of the hour
	@Override
	protected boolean act() {
		if (state == WANDERING){
			int haunt = RoamerEvent.haunt( this );
			if (haunt != -1) target = haunt;
		}
		return super.act();
	}

	@Override
	public void die( Object cause ) {
		RoamerEvent.slain( this );
		super.die( cause );
	}
}
