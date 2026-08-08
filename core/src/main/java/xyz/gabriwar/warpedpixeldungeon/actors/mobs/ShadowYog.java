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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot;
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

		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Charm.class );
		immunities.add( Sleep.class );
		immunities.add( Burning.class );
		immunities.add( ToxicGas.class );
		immunities.add( Vertigo.class );

		declareExtraLoot(OrbOfZot.class, 1f);
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
		super.damage( dmg, src );

		if (HP < HT / 8 && Random.Int( 2 ) == 0) {
			teleport();
		}
	}

	private void teleport() {
		int newPos;
		int tries = 100;
		do {
			newPos = Random.Int( Dungeon.level.length() );
			tries--;
		} while (tries > 0
				&& (!Dungeon.level.passable[newPos] || Actor.findChar( newPos ) != null));

		if (tries <= 0) return;

		pos = newPos;
		sprite.place( pos );
		sprite.visible = Dungeon.level.heroFOV[pos];

		// Spawn SpectralRats around new position
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar( cell ) == null) {
				SpectralRat rat = new SpectralRat();
				rat.pos = cell;
				rat.state = rat.HUNTING;
				GameScene.add( rat );
			}
		}
	}

	@Override
	public void die( Object cause ) {

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
}
