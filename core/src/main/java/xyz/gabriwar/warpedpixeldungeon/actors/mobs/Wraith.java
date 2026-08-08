/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChallengeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.RatSkull;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.WraithSprite;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class Wraith extends Mob {

	private static final float SPAWN_DELAY	= 2f;
	
	protected int level;
	
	{
		spriteClass = WraithSprite.class;
		
		HP = HT = 1;

		maxLvl = -2;

		flying = true;

		loot = xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion.class;
		lootChance = 0.075f;

		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);

		immunities.add(xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost.class);
		immunities.add(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom.class);
	}
	
	private static final String LEVEL = "level";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = bundle.getInt( LEVEL );
		adjustStats( level );
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1 + level/2, 2 + level );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 10 + level;
	}
	
	public void adjustStats( int level ) {
		this.level = level;
		HP = HT = 1 + level;
		EXP = 1 + level;
		defenseSkill = attackSkill( null ) * 5;
		enemySeen = true;
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public boolean reset() {
		state = WANDERING;
		return true;
	}

	public static void spawnAround( int pos ) {
		spawnAround( pos, null );
	}
	
	public static void spawnAround( int pos, Class<? extends Wraith> wraithClass ) {
		for (int n : PathFinder.NEIGHBOURS4) {
			spawnAt( pos + n, wraithClass, false );
		}
	}

	public static Wraith spawnAt( int pos ) {
		return spawnAt( pos, null );
	}

	public static Wraith spawnAt( int pos, Class<? extends Wraith> wraithClass ) {
		return spawnAt( pos, wraithClass, true );
	}

	private static Wraith spawnAt( int pos, Class<? extends Wraith> wraithClass, boolean allowAdjacent ) {

		//if the position itself is blocked, try to place in an adjacent cell if allowed
		if (Dungeon.level.solid[pos] || Actor.findChar( pos ) != null){
			ArrayList<Integer> candidates = new ArrayList<>();

			for (int i : PathFinder.NEIGHBOURS8){
				if (!Dungeon.level.solid[pos+i] && Actor.findChar( pos+i ) == null){
					candidates.add(pos+i);
				}
			}

			if (allowAdjacent && !candidates.isEmpty()){
				pos = Random.element(candidates);
			} else {
				pos = -1;
			}

		}

		if (pos != -1) {

			Wraith w;
			//if no wraith type is specified, 1/100 chance for exotic, otherwise normal
			if (wraithClass == null){
				float altChance = 1/100f * RatSkull.exoticChanceMultiplier();
				if (Random.Float() < altChance){
					w = new TormentedSpirit();
				} else {
					w = new Wraith();
				}
			} else {
				w = Reflection.newInstance(wraithClass);
			}
			w.adjustStats( Dungeon.scalingDepth() );
			w.pos = pos;
			w.state = w.HUNTING;
			GameScene.add( w, SPAWN_DELAY );
			Dungeon.level.occupyCell(w);

			w.sprite.alpha( 0 );
			w.sprite.parent.add( new AlphaTweener( w.sprite, 1, 0.5f ) );

			if (w instanceof TormentedSpirit){
				w.sprite.emitter().burst(ChallengeParticle.FACTORY, 10);
			} else {
				w.sprite.emitter().burst(ShadowParticle.CURSE, 5);
			}

			return w;
		} else {
			return null;
		}
	}

}
