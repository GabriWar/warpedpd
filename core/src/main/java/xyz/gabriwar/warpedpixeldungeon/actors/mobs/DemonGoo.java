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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DemonGooSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class DemonGoo extends Mob {

	private static final float SPAWN_DELAY = 2f;
	private static final float SPLIT_DELAY = 1f;

	private int demonGooGeneration = 0;

	private static final String DEMON_GOO_GENERATION = "demon_goo_generation";

	{
		spriteClass = DemonGooSprite.class;

		HP = HT = 200;
		defenseSkill = 10;
		baseSpeed = 2f;

		EXP = 10;

		loot = new PotionOfMending();
		lootChance = 1f;

		immunities.add( Roots.class );
		resistances.add( ToxicGas.class );
		resistances.add( Grim.class );
		resistances.add( Doom.class );
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( DEMON_GOO_GENERATION, demonGooGeneration );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		demonGooGeneration = bundle.getInt( DEMON_GOO_GENERATION );
	}

	@Override
	protected boolean act() {
		boolean result = super.act();
		if (Dungeon.level.water[pos] && HP < HT) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HP++;
		} else if (Dungeon.level.water[pos] && HP == HT && HT < 200) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HT += 5;
			HP = HT;
		}
		return result;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 30, 60 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 35;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 20 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
		}
		sprite.emitter().burst( ShadowParticle.UP, 5 );
		return damage;
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {
		if (HP >= damage + 2 && demonGooGeneration < 3) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4) {
				if (Dungeon.level.passable[pos + n] && Actor.findChar( pos + n ) == null) {
					candidates.add( pos + n );
				}
			}

			if (!candidates.isEmpty()) {
				DemonGoo clone = split();
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

	private DemonGoo split() {
		DemonGoo clone = new DemonGoo();
		clone.demonGooGeneration = demonGooGeneration + 1;
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
		super.die( cause );
		yell( Messages.get(this, "glurp") );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static DemonGoo spawnAt( int pos ) {
		DemonGoo g = new DemonGoo();
		g.pos = pos;
		g.state = g.HUNTING;
		GameScene.add( g, SPAWN_DELAY );
		return g;
	}
}
