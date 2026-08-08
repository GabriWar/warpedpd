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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MrDestructoSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class MrDestructo extends Mob {

	{
		spriteClass = MrDestructoSprite.class;

		alignment = Alignment.ALLY;
		state = HUNTING;
		HP = HT = 100;
		defenseSkill = 3;

		WANDERING = new Hunting();

		immunities.add( Terror.class );
		immunities.add( ToxicGas.class );

		resistances.add( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class );
		resistances.add( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class );

		properties.add( Property.IMMOVABLE );

		declareExtraLoot(InactiveMrDestructo.class, 1f);
	}

	private static final float SPAWN_DELAY = 0.1f;

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );
		return super.act();
	}

	@Override
	protected Char chooseEnemy() {
		if (enemy == null || !enemy.isAlive() || !fieldOfView[enemy.pos]) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.alignment == Alignment.ENEMY && fieldOfView[mob.pos]) {
					return mob;
				}
			}
			return null;
		}
		return enemy;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		Ballistica beam = new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID );
		return beam.subPath(1, beam.dist).contains(enemy.pos);
	}

	@Override
	public int attackSkill( Char target ) {
		return 20 + Dungeon.depth;
	}

	@Override
	public float attackDelay() {
		return 0.5f;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		spend( attackDelay() );

		Ballistica beam = new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID );
		boolean rayVisible = false;
		for (int c : beam.subPath(0, beam.dist)) {
			if (Dungeon.level.heroFOV[c]) {
				rayVisible = true;
				break;
			}
		}

		if (rayVisible) {
			sprite.zap( beam.collisionPos );
			return false;
		} else {
			deathRay();
			return true;
		}
	}

	public void deathRay() {
		Ballistica beam = new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID );

		for (int c : beam.subPath(1, beam.dist)) {
			Char ch = Actor.findChar( c );
			if (ch == null) continue;

			if (hit( this, ch, true )) {
				ch.damage( Random.NormalIntRange( Dungeon.depth, Dungeon.depth + 12 ), this );
				yell( Messages.get(this, "zap") );
				damage( Random.NormalIntRange(5, 10), this );

				if (Dungeon.level.heroFOV[c]) {
					ch.sprite.flash();
					CellEmitter.center( c ).burst( PurpleParticle.BURST, Random.IntRange(1, 2) );
				}

				if (!ch.isAlive() && ch == Dungeon.hero) {
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail( this );
					GLog.n( Messages.get(this, "deathray_kill") );
				}
			} else {
				ch.sprite.showStatus( CharSprite.NEUTRAL, ch.defenseVerb() );
			}
		}
	}

	@Override
	public void beckon( int cell ) {
		// ignore
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public void die( Object cause ) {
		yell( Messages.get(this, "shutdown") );
		dropExtraLoot();
		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(new InactiveMrDestructo(), 0);
	}

	public static MrDestructo spawnAt( int pos ) {
		MrDestructo m = new MrDestructo();
		m.pos = pos;
		m.state = m.HUNTING;
		GameScene.add( m, SPAWN_DELAY );
		return m;
	}

	private class Hunting extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && canAttack( enemy )) {
				return doAttack( enemy );
			} else {
				enemy = chooseEnemy();
				if (enemy != null && canAttack( enemy )) {
					return doAttack( enemy );
				}
				spend( TICK );
				return true;
			}
		}
	}
}
