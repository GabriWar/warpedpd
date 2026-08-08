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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.AdultDragonVioletSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class AdultDragonViolet extends Mob {

	{
		spriteClass = AdultDragonVioletSprite.class;

		HP = HT = 8000;
		defenseSkill = 75;
		baseSpeed = 2f;

		EXP = 20;

		properties.add( Property.BOSS );

		resistances.add( ToxicGas.class );
		resistances.add( Poison.class );
		resistances.add( Grim.class );
		resistances.add( Doom.class );
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 150, 300 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 99;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 75 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID ).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			return super.doAttack( enemy );
		} else {
			boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];
			if (visible) {
				sprite.zap( enemy.pos );
			} else {
				onZapComplete();
			}
			return !visible;
		}
	}

	public void onZapComplete() {
		if (enemy != null && enemy.isAlive()) {
			spend( attackDelay() );

			if (hit( this, enemy, true )) {
				int dmg = damageRoll() * 2;
				enemy.damage( dmg, this );
				Buff.affect( enemy, Poison.class ).set( 8f );
			} else {
				enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
			}
		}
		next();
	}

	@Override
	public void die( Object cause ) {
		yell( Messages.get( this, "defeated" ) );
		super.die( cause );
	}
}
