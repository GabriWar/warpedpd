/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2014-2024 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.PoisonGooSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PoisonGoo extends Mob {

	private static final float SPAWN_DELAY = 2f;
	private static final float SPLIT_DELAY = 1f;

	private int gooGeneration = 0;

	private static final String GOO_GENERATION = "goo_generation";

	{
		spriteClass = PoisonGooSprite.class;

		HP = HT = 50;
		defenseSkill = 12;
		baseSpeed = 2f;

		EXP = 10;

		loot = new PotionOfMending();
		lootChance = 1f;

		FLEEING = new Fleeing();

		immunities.add( Roots.class );
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( GOO_GENERATION, gooGeneration );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		gooGeneration = bundle.getInt( GOO_GENERATION );
	}

	@Override
	protected boolean act() {
		boolean result = super.act();
		if (Dungeon.level.water[pos] && HP < HT) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HP++;
		} else if (Dungeon.level.water[pos] && HP == HT && HT < 100) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HT += 5;
			HP = HT;
		}
		return result;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 10 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 5;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 2 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		Buff.affect( enemy, Poison.class ).set( Random.IntRange(7, 10) );
		state = FLEEING;
		return damage;
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {
		if (HP >= damage + 2 && gooGeneration < 3) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4) {
				if (Dungeon.level.passable[pos + n] && Actor.findChar( pos + n ) == null) {
					candidates.add( pos + n );
				}
			}

			if (!candidates.isEmpty()) {
				PoisonGoo clone = split();
				clone.HP = (HP - damage) / 2;
				clone.pos = Random.element( candidates );
				clone.state = clone.HUNTING;

				if (Dungeon.level.map[clone.pos] == Terrain.DOOR) {
					Door.enter( clone.pos );
				}

				GameScene.add( clone, SPLIT_DELAY );
				Actor.addDelayed( new Pushing( clone, pos, clone.pos ), -1 );

				HP -= clone.HP;
				GLog.w( Messages.get(this, "split") );
			}
		}
		return damage;
	}

	private PoisonGoo split() {
		PoisonGoo clone = new PoisonGoo();
		clone.gooGeneration = gooGeneration + 1;
		clone.lootChance = 0;
		if (buff( Burning.class ) != null) {
			Buff.affect( clone, Burning.class ).reignite( clone );
		}
		if (buff( Poison.class ) != null) {
			Buff.affect( clone, Poison.class ).set( 2 );
		}
		return clone;
	}

	@Override
	public void notice() {
		super.notice();
		yell( Messages.get(this, "notice") );
	}

	@Override
	public void die( Object cause ) {
		if (gooGeneration > 0) {
			lootChance = 0;
		}
		super.die( cause );

		// Check if any Goo or PoisonGoo remain alive
		int goosAlive = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof Goo || mob instanceof PoisonGoo) {
				goosAlive++;
			}
		}

		if (goosAlive == 0) {
			Dungeon.level.drop( new SkeletonKey( Dungeon.depth ), pos ).sprite.drop();
			Dungeon.level.drop( new Gold( Random.IntRange(900, 2000) ), pos ).sprite.drop();
			Badges.validateBossSlain();
		} else {
			Dungeon.level.drop( new Gold( Random.IntRange(100, 200) ), pos ).sprite.drop();
		}
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static PoisonGoo spawnAt( int pos ) {
		PoisonGoo g = new PoisonGoo();
		g.pos = pos;
		g.state = g.HUNTING;
		GameScene.add( g, SPAWN_DELAY );
		return g;
	}

	private class Fleeing extends Mob.Fleeing {
		@Override
		protected void nowhereToRun() {
			if (buff( Terror.class ) == null) {
				state = HUNTING;
			} else {
				super.nowhereToRun();
			}
		}
	}
}
