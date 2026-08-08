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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderMindSprite;
import com.watabou.utils.Random;

//ported from Remixed PD's Spider Nest: keeps its distance and drives the rest of the
//brood into a frenzy from afar
public class SpiderMind extends Mob {

	{
		spriteClass = SpiderMindSprite.class;

		HP = HT = 40;
		defenseSkill = 14;

		EXP = 6;
		maxLvl = 9;

		loot = MysteryMeat.class;
		lootChance = 0.067f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 6, 10 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 18;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT ).collisionPos == enemy.pos;
	}

	@Override
	protected boolean getCloser( int target ) {
		//it would rather stay back and let the brood do the work
		if (state == HUNTING) {
			return enemySeen && getFurther( target );
		}
		return super.getCloser( target );
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
			sprite.attack( enemy.pos );
			return false;
		} else {
			frenzy();
			return true;
		}
	}

	@Override
	public void onAttackComplete() {
		frenzy();
		super.onAttackComplete();
	}

	//goads one nearby brood member into a frenzy rather than striking directly
	private void frenzy() {
		for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
			if (mob != this && mob.alignment == alignment
					&& Dungeon.level.heroFOV[mob.pos]
					&& (mob instanceof SpiderServant || mob instanceof SpiderGuard
						|| mob instanceof SpiderExploding)) {
				if (Random.Int( 2 ) == 0) {
					Buff.prolong( mob, Haste.class, 3f );
				} else {
					Buff.prolong( mob, Adrenaline.class, 3f );
				}
				CellEmitter.get( mob.pos ).start( ShaftParticle.FACTORY, 0.2f, 3 );
				break;
			}
		}
	}
}
