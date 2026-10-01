/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.AssassinSprite;
import com.watabou.utils.Random;

public class Assassin extends Mob {

	protected static final float SPAWN_DELAY = 2f;

	{
		spriteClass = AssassinSprite.class;

		HP = HT = 25 + 5 * Random.NormalIntRange( 2, 5 );
		defenseSkill = 15;
		baseSpeed = 2f;

		EXP = 10;

		resistances.add( ToxicGas.class );
		resistances.add( Poison.class );
		resistances.add( Grim.class );
		resistances.add( ScrollOfPsionicBlast.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 10, 23 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 5 );
	}

	@Override
	public float attackDelay() {
		return 0.75f;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID ).collisionPos == enemy.pos;
	}

	public static Assassin spawnAt( int pos ) {
		if (Dungeon.level.passable[pos] && Actor.findChar( pos ) == null) {

			Assassin a = new Assassin();
			a.pos = pos;
			a.state = a.HUNTING;
			GameScene.add( a, SPAWN_DELAY );
			Dungeon.level.occupyCell( a );

			return a;

		} else {
			return null;
		}
	}
}
