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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.IceGuardianSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//ported from Remixed PD's Ice Caves. Half of an interlocking pair: killing a
//guardian wounds the core it serves, and while that core lives the ice simply
//reforms one, up to four at a time. Every guardian broken tears into the core.
public class IceGuardian extends Mob {

	public static final int CORE_DAMAGE_ON_DEATH = 150;

	{
		spriteClass = IceGuardianSprite.class;

		HP = HT = 70;
		defenseSkill = 30;

		baseSpeed = 0.7f;

		EXP = 5;
		maxLvl = 10;

		loot = MysteryMeat.class;
		lootChance = 0.2f / 4f;

		properties.add( Property.ICY );
		properties.add( Property.INORGANIC );

		immunities.add( Paralysis.class );
		immunities.add( ToxicGas.class );
		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Blindness.class );
		immunities.add( MagicalSleep.class );
		immunities.add( Grim.class );

		//no metabolism to disturb: only the extremes reach it
		thermal = Thermal.INSENSATE;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 10, 15 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 31;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 14 );
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );

		//the core feels every guardian that falls, and replaces it twice over
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (mob instanceof IceGuardianCore && mob.isAlive()) {
				mob.damage( CORE_DAMAGE_ON_DEATH, this );
				if (mob.isAlive()) {
					//it reforms what it can, but never more than four guardians at once
					while (IceGuardianCore.guardians() < IceGuardianCore.MAX_GUARDIANS) {
						if (!resurrect()) break;
					}
				}
			}
		}
	}

	//spawns a fresh guardian next to where this one fell, false if there was no room
	private boolean resurrect() {
		int cell = -1;
		for (int i : PathFinder.NEIGHBOURS8) {
			int c = pos + i;
			if (c >= 0 && c < Dungeon.level.length()
					&& Dungeon.level.passable[c]
					&& Actor.findChar( c ) == null) {
				cell = c;
				break;
			}
		}
		if (cell == -1) return false;

		IceGuardian spawned = new IceGuardian();
		spawned.pos = cell;
		spawned.state = spawned.HUNTING;
		GameScene.add( spawned );
		CellEmitter.get( cell ).burst( Speck.factory( Speck.LIGHT ), 4 );
		if (Dungeon.level.heroFOV[cell]) {
			GLog.w( Messages.get( IceGuardianCore.class, "form" ) );
		}
		return true;
	}
}
