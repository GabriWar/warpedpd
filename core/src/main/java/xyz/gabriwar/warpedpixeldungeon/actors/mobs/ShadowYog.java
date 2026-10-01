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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShadowYogSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ShadowYog extends Mob {

	{
		spriteClass = ShadowYogSprite.class;

		HP = HT = Math.max(1000, 50 * (Dungeon.hero != null ? Dungeon.hero.lvl : 20));
		EXP = 100;
		defenseSkill = 32;
		baseSpeed = 2f;

		state = PASSIVE;

		properties.add( Property.BOSS );
		properties.add( Property.DEMONIC );

		immunities.add( Grim.class );
		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Charm.class );
		immunities.add( Sleep.class );
		immunities.add( Burning.class );
		immunities.add( ToxicGas.class );
		immunities.add( Vertigo.class );

		declareExtraLoot(OrbOfZot.class, 1f);

		//built for the heat: the chill is what hurts
		thermal = Thermal.HEAT_DWELLER;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 45, 125 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 50;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Dungeon.level.mobs.size();
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void beckon( int cell ) {
		// Ignores beckon
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss( this );

		// every hit seeds hidden summoning traps in sight of the hero
		for (int i = 0; i < 4; i++) {
			int trapPos;
			int tries = 20;
			do {
				trapPos = Random.Int( Dungeon.level.length() );
				tries--;
			} while (tries > 0
					&& (!Dungeon.level.heroFOV[trapPos] || !Dungeon.level.passable[trapPos]));

			if (Dungeon.level.map[trapPos] == Terrain.INACTIVE_TRAP) {
				Dungeon.level.setTrap( new SummoningTrap().hide(), trapPos );
				Level.set( trapPos, Terrain.SECRET_TRAP );
			}
		}

		if (HP < HT / 8 && Random.Int( 2 ) == 0) {
			teleport();
		}

		super.damage( dmg, src );
	}

	private void teleport() {
		int newPos = -1;
		for (int i = 0; i < 20; i++) {
			newPos = Dungeon.level.randomRespawnCell( this );
			if (newPos != -1) {
				break;
			}
		}

		if (newPos == -1) return;

		CellEmitter.get( pos ).start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );

		pos = newPos;
		sprite.place( pos );
		sprite.visible = Dungeon.level.heroFOV[pos];

		GLog.n( Messages.get( this, "vanish" ) );

		// the den only keeps feeding the legion while it is thin
		if (Dungeon.level.mobs.size() < Dungeon.hero.lvl * 2) {
			for (int n : PathFinder.NEIGHBOURS4) {
				int cell = pos + n;
				if (Dungeon.level.passable[cell] && Actor.findChar( cell ) == null
						&& Random.Float() < 0.75f) {
					SpectralRat.spawnAt( cell );
				}
			}
		}
	}

	@Override
	public void die( Object cause ) {

		Statistics.shadowYogsKilled++;

		// Check if any other ShadowYog alive on level
		boolean otherAlive = false;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof ShadowYog && mob != this) {
				otherAlive = true;
				break;
			}
		}

		if (!otherAlive) {
			Dungeon.shadowyogkilled = true;
			GameScene.bossSlain();

			// Kill all Rat/GreyOni/SpectralRat/Eye on level
			for (Mob mob : (Iterable<Mob>) Dungeon.level.mobs.clone()) {
				if (mob instanceof Rat
						|| mob instanceof GreyOni
						|| mob instanceof SpectralRat
						|| mob instanceof Eye) {
					mob.die( cause );
				}
			}

			yell( Messages.get( this, "die" ) );
		}

		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		// Only the last ShadowYog drops the OrbOfZot
		boolean otherAlive = false;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof ShadowYog && mob != this) {
				otherAlive = true;
				break;
			}
		}
		//the orb only exists once per run: repeat infest clears drop nothing
		if (!otherAlive && !Dungeon.orbofzotdropped) {
			Dungeon.orbofzotdropped = true;
			trackedDrop(new OrbOfZot(), 0);
		}
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
