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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LockedFloor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.SpiderCharm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderQueenSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//ported from Remixed PD's Spider Nest: the mother of the brood and the nest's boss,
//holding its deepest floor. It lays eggs, poisons heavily, and skitters away once
//wounded; engaging it seals the floor until it dies.
public class SpiderQueen extends Mob {

	{
		spriteClass = SpiderQueenSprite.class;

		HP = HT = 120;
		defenseSkill = 18;

		EXP = 11;
		maxLvl = 12;

		properties.add( Property.BOSS );

		declareExtraLoot( SpiderCharm.class, 1f );

		immunities.add( Burning.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 12, 20 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 21;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 10 );
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (isAlive() && !BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean bleeding = (HP*2 <= HT);
		super.damage( dmg, src );
		if ((HP*2 <= HT) && !bleeding) {
			BossHealthBar.bleed( true );
		}
		LockedFloor lock = Dungeon.hero.buff( LockedFloor.class );
		if (lock != null && !isImmune( src.getClass() ) && !isInvulnerable( src.getClass() )) {
			if (Dungeon.isChallenged( Challenges.STRONGER_BOSSES )) lock.addTime( dmg );
			else                                                    lock.addTime( dmg*1.5f );
		}
	}

	@Override
	protected boolean act() {
		//lays an egg now and then, within the brood cap
		if (state == HUNTING && Random.Int( 20 ) == 0
				&& SpiderServant.population() < SpiderServant.POPULATION_CAP) {
			for (int i : PathFinder.NEIGHBOURS8) {
				int c = pos + i;
				if (c >= 0 && c < Dungeon.level.length()
						&& Dungeon.level.passable[c] && Actor.findChar( c ) == null) {
					SpiderEgg egg = new SpiderEgg();
					egg.pos = c;
					GameScene.add( egg );
					break;
				}
			}
		}
		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		//once bloodied it stops committing to melee and keeps its distance
		return Dungeon.level.adjacent( pos, enemy.pos ) && HP > HT / 2;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (HP < HT / 2 && state == HUNTING && Dungeon.level.distance( pos, target ) < 5) {
			return getFurther( target );
		}
		return super.getCloser( target );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 2 ) == 0) {
			Buff.affect( enemy, Poison.class ).set( Random.NormalIntRange( 7, 9 ) );
		}
		return damage;
	}

	@Override
	public void die( Object cause ) {
		Dungeon.level.drop( new PotionOfHealing(), pos ).sprite.drop();
		Dungeon.level.drop( Generator.random( Generator.Category.WAND ), pos ).sprite.drop();
		super.die( cause );
		Dungeon.level.unseal();
		GameScene.bossSlain();
		yell( Messages.get( this, "die" ) );
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop( new SpiderCharm(), 0 );
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
			yell( Messages.get( this, "notice" ) );
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (state != SLEEPING) BossHealthBar.assignBoss( this );
		if ((HP*2 <= HT)) BossHealthBar.bleed( true );
	}
}
