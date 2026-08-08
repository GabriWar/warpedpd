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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlueWraithSprite;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class BlueWraith extends Wraith {

	{
		spriteClass = BlueWraithSprite.class;

		HP = HT = 195;
		defenseSkill = 24;
		baseSpeed = 4f;

		EXP = 11;

		loot = new RedDewdrop();
		lootChance = 1f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20, 90 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 46;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 25 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (Random.Int(10) == 0) {
			Buff.affect( enemy, Vertigo.class, Vertigo.DURATION );
			Buff.affect( enemy, Terror.class, Terror.DURATION ).object = enemy.id();
		}
		return damage;
	}

	@Override
	public void adjustStats( int level ) {
		this.level = level;
		defenseSkill = 24;
		enemySeen = true;
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static BlueWraith spawnAt( int pos ) {
		if (Dungeon.level.solid[pos] || Actor.findChar( pos ) != null) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8) {
				if (!Dungeon.level.solid[pos+i] && Actor.findChar( pos+i ) == null) {
					candidates.add( pos+i );
				}
			}
			if (!candidates.isEmpty()) {
				pos = Random.element( candidates );
			} else {
				return null;
			}
		}

		BlueWraith w = new BlueWraith();
		w.adjustStats( Dungeon.scalingDepth() );
		w.pos = pos;
		w.state = w.HUNTING;
		GameScene.add( w, 2f );
		Dungeon.level.occupyCell( w );

		w.sprite.alpha( 0 );
		w.sprite.parent.add( new AlphaTweener( w.sprite, 1, 0.5f ) );
		w.sprite.emitter().burst( ShadowParticle.CURSE, 5 );

		return w;
	}
}
