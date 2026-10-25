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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.sprites.ElderYetiSprite;
import com.watabou.utils.Random;

/**
 * An elder yeti with a name of its own, roaming the cold lands for days
 * (levels/overworld/RoamerEvent): a yeti grown old and huge, four times as hardy and harder
 * hitting, its blows chilling to the bone (one in two slows its foe with the cold). Out of a
 * fight it keeps to its haunt of the hour; killed, it leaves its trophy, a purse and a good item.
 */
public class ElderYeti extends Yeti {

	{
		spriteClass = ElderYetiSprite.class;

		HP = HT = 480;
		defenseSkill = 34;

		EXP = 40;
		maxLvl = 38;

		state = WANDERING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 26, 52 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 42;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 2 ) == 0) Buff.prolong( enemy, Chill.class, 4f );
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
