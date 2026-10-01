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

import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.ui.BossHealthBar;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.misc.AutoPotion;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.sprites.ZotSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Zot extends Mob {

	private static final int JUMP_DELAY = 5;
	private int timeToJump = JUMP_DELAY;

	{
		spriteClass = ZotSprite.class;

		HP = HT = 10000;
		EXP = 20;
		defenseSkill = 70;
		baseSpeed = 2f;

		properties.add( Property.BOSS );

		resistances.add( ToxicGas.class );
		resistances.add( Poison.class );

		immunities.add( Vampiric.class );
		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Charm.class );
		immunities.add( Sleep.class );
		immunities.add( Burning.class );
		immunities.add( ToxicGas.class );
		immunities.add( Vertigo.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 300, 400 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 350;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 100 );
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
	protected boolean act() {
		if (buff( Paralysis.class ) != null) {
			yell( Messages.get( this, "paralysed" ) );
			if (!checkEyes()) {
				spawnEye();
			}
			if (HP < HT) {
				sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
				HP = Math.min( HP + 200, HT );
			}
		}

		boolean result = super.act();

		int regen = Dungeon.hero.buff( AutoPotion.AutoHealPotion.class ) != null ? 1 : Random.Int( 50, 100 );
		if (isAlive() && HP < HT) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HP = Math.min( HP + regen, HT );
		}

		return result;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (Dungeon.level.heroFOV[target]) {
			jump();
			return true;
		} else {
			return super.getCloser( target );
		}
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		timeToJump--;
		if (timeToJump <= 0 && Dungeon.level.adjacent( pos, enemy.pos )) {
			jump();
			return true;
		} else {
			return super.doAttack( enemy );
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss( this );
		if (!(src instanceof RelicMeleeWeapon) && !(src instanceof JupitersWraith)) {
			dmg = Random.Int( 1, Math.max( 1, Math.round( dmg * 0.25f ) ) );
		}

		if (Dungeon.hero.heroClass == HeroClass.HUNTRESS && !checkPhases()) {
			spawnEye();
		}

		super.damage( dmg, src );
	}

	private void jump() {
		timeToJump = JUMP_DELAY;

		if (!checkPhases()) {
			spawnPhase();
		}

		int newPos;
		int tries = 0;
		do {
			newPos = Random.Int( Dungeon.level.length() );
			tries++;
			if (tries > 100) break;
		} while (!Dungeon.level.passable[newPos]
				|| !Dungeon.level.heroFOV[newPos]
				|| (enemy != null && Dungeon.level.adjacent( newPos, enemy.pos ))
				|| Actor.findChar( newPos ) != null);

		if (tries <= 100) {
			sprite.move( pos, newPos );
			move( newPos, false );

			if (Dungeon.level.heroFOV[newPos]) {
				CellEmitter.get( newPos ).burst( Speck.factory( Speck.WOOL ), 6 );
				Sample.INSTANCE.play( Assets.Sounds.PUFF );
			}
		}

		spend( 1 / speed() );
	}

	private void spawnPhase() {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = pos + n;
			if ((Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])
					&& Actor.findChar( cell ) == null) {
				candidates.add( cell );
			}
		}
		if (!candidates.isEmpty()) {
			ZotPhase phase = new ZotPhase();
			phase.pos = Random.element( candidates );
			phase.state = phase.HUNTING;
			GameScene.add( phase );
			Actor.addDelayed( new Pushing( phase, pos, phase.pos ), -1 );
		}
	}

	private void spawnEye() {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = Dungeon.hero.pos + n;
			if ((Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])
					&& Actor.findChar( cell ) == null) {
				candidates.add( cell );
			}
		}
		if (!candidates.isEmpty()) {
			MagicEye eye = new MagicEye();
			eye.pos = Random.element( candidates );
			eye.state = eye.HUNTING;
			GameScene.add( eye );
			Actor.addDelayed( new Pushing( eye, pos, eye.pos ), -1 );
		}
	}

	private boolean checkPhases() {
		int count = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof ZotPhase) {
				count++;
				int limit = (Dungeon.hero.heroClass == HeroClass.HUNTRESS) ? 10 : 6;
				if (count > limit) return true;
			}
		}
		return false;
	}

	private boolean checkEyes() {
		int count = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof MagicEye) {
				count++;
				int limit = (Dungeon.hero.heroClass == HeroClass.HUNTRESS) ? 30 : 20;
				if (count > limit) return true;
			}
		}
		return false;
	}

	@Override
	public void die( Object cause ) {
		Dungeon.level.unseal();
		GameScene.bossSlain();

		for (Mob mob : (Iterable<Mob>) Dungeon.level.mobs.clone()) {
			if (mob instanceof ZotPhase || mob instanceof MagicEye) {
				mob.die( cause );
				mob.destroy();
				mob.sprite.killAndErase();
			}
		}

		super.die( cause );

		yell( Messages.get( this, "die" ) );

		// Sprouted: spawn OtilukeNPC at Zot's death position
		OtilukeNPC.spawnAt( pos );
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss( this );
		yell( Messages.get( this, "notice" ) );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (enemySeen || HP < HT) BossHealthBar.assignBoss( this );
	}
}
