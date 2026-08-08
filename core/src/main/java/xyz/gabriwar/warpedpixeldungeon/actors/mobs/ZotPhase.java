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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ZotPhaseSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class ZotPhase extends Mob implements Callback {

	private static final float TIME_TO_ZAP = 2f;

	{
		spriteClass = ZotPhaseSprite.class;

		HP = HT = 1000;
		EXP = 36;
		defenseSkill = 40;
		baseSpeed = 2f;

		loot = Generator.Category.SCROLL;
		lootChance = 0.33f;

		resistances.add(Electricity.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 115, 160 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 100;
	}

	@Override
	public float attackDelay() {
		return 0.5f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 4 );
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack( enemy )
				|| new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID ).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {

		if (Dungeon.level.adjacent( pos, enemy.pos )
				|| new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID ).collisionPos != enemy.pos) {

			return super.doAttack( enemy );

		} else {

			spend( TIME_TO_ZAP );

			Invisibility.dispel( this );
			int dmg = Random.Int( 80, 160 );
			if (Dungeon.level.water[enemy.pos]) {
				dmg = Math.round( dmg * 1.5f );
			}

			if (hit( this, enemy, true )) {
				enemy.damage( dmg, this );

				enemy.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
				enemy.sprite.flash();

				if (enemy == Dungeon.hero) {
					Camera.main.shake( 2, 0.3f );
				}

				if (enemy == Dungeon.hero && !enemy.isAlive()) {
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail( this );
					GLog.n( Messages.get( this, "zap_kill" ) );
				}
			} else {
				enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
			}

			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				sprite.zap( enemy.pos );
				return false;
			} else {
				return true;
			}
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!(src instanceof RelicMeleeWeapon) && !(src instanceof JupitersWraith)) {
			dmg = Random.Int( 1, Math.max( 1, Math.round( dmg * 0.25f ) ) );
		}
		super.damage( dmg, src );
	}

	@Override
	public void call() {
		next();
	}
}
