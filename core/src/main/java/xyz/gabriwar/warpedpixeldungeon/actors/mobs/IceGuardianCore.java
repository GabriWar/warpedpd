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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IceKey;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.IceGuardianSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Random;

//ported from Remixed PD's Ice Caves. The other half of the guardian pair: enormous
//health, but it takes 150 damage every time one of its guardians is destroyed, and
//it drags them all down with it when it finally falls.
public class IceGuardianCore extends Mob {

	{
		spriteClass = IceGuardianSprite.class;

		HP = HT = 1000;
		defenseSkill = 10;

		baseSpeed = 0.5f;

		EXP = 25;
		maxLvl = 30;

		//a set-piece inside the branch, not the branch's boss - that is the demon lord
		properties.add( Property.MINIBOSS );
		properties.add( Property.ICY );
		properties.add( Property.INORGANIC );
		properties.add( Property.IMMOVABLE );

		immunities.add( Paralysis.class );
		immunities.add( ToxicGas.class );
		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Blindness.class );
		immunities.add( MagicalSleep.class );
		immunities.add( Grim.class );

		state = PASSIVE;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 13, 23 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 26;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 11 );
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (isAlive() && !BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
		}
		super.damage( dmg, src );
	}

	@Override
	public void die( Object cause ) {
		//without the core the ice holding the guardians together lets go
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (mob instanceof IceGuardian && mob.isAlive()) {
				mob.die( cause );
			}
		}

		Dungeon.level.drop( new IceKey( Dungeon.depth ), pos ).sprite.drop();
		Dungeon.level.drop( new WandOfFrost().upgrade(), pos ).sprite.drop();

		super.die( cause );

		yell( Messages.get( this, "die" ) );
	}
}
