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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BlobImmunity;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.PikeSprite;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * A pike of the rivers: it keeps to the open water of a river (OverworldFauna, any hour) and lives
 * as the dungeon's piranha does - asleep or idling in the water, a dark shape under it, it swims at
 * twice the hero's pace and bites whatever wades within its reach; it sees nothing it has no way to
 * through the water, and out of the water it dies. Its strength is its own, not the floor's: the
 * surface is not deep. A river that freezes over takes it under the ice (it is gone, nothing left).
 * A fishing spear hits it as it does a piranha (FishingSpear). Half the time it leaves a piece of meat.
 */
public class Pike extends Mob {

	{
		spriteClass = PikeSprite.class;

		HP = HT = 60;
		defenseSkill = 22;

		EXP = 8;
		maxLvl = 28;

		baseSpeed = 2f;

		loot = MysteryMeat.class;
		lootChance = 0.5f;

		SLEEPING = new Sleeping();
		WANDERING = new Wandering();
		HUNTING = new Hunting();
		state = SLEEPING;
	}

	{
		//under the water: no gas reaches it but the spark and the frost, and nothing burns there
		for (Class c : new BlobImmunity().immunities()){
			if (c != Electricity.class && c != Freezing.class) immunities.add( c );
		}
		immunities.add( Burning.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 12, 24 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 34;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	@Override
	protected boolean act() {
		OverworldFauna.sighted( this );
		Level level = Dungeon.level;
		//the river froze over it: gone under the ice
		if (level.map[pos] == Terrain.FROZEN_WATER){
			spend( TICK );
			OverworldLevel.vanish( level, this );
			return true;
		}
		if (!level.water[pos] || flying){
			if (sprite != null && buff( Levitation.class ) != null){
				sprite.emitter().burst( Speck.factory( Speck.JET ), 10 );
			}
			die( null );
			return true;
		}
		return super.act();
	}

	@Override
	public boolean surprisedBy( Char enemy, boolean attacking ) {
		if (enemy == Dungeon.hero && (!attacking || ((Hero) enemy).canSurpriseAttack())){
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
				fieldOfView = new boolean[Dungeon.level.length()];
				Dungeon.level.updateFieldOfView( this, fieldOfView );
			}
			return state == SLEEPING || !fieldOfView[enemy.pos] || enemy.invisible > 0;
		}
		return super.surprisedBy( enemy, attacking );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public boolean reset() {
		return true;
	}

	//it swims only through water
	@Override
	protected boolean getCloser( int target ) {
		if (rooted) return false;
		int step = Dungeon.findStep( this, target, BArray.and( Dungeon.level.water, Dungeon.level.passable, null ), fieldOfView, true );
		if (step == -1) return false;
		move( step );
		return true;
	}

	@Override
	protected boolean getFurther( int target ) {
		int step = Dungeon.flee( this, target, BArray.and( Dungeon.level.water, Dungeon.level.passable, null ), fieldOfView, true );
		if (step == -1) return false;
		move( step );
		return true;
	}

	//what it has no way to through the water, it acts as if it cannot see
	private boolean reachable( boolean enemyInFOV ){
		if (!enemyInFOV) return false;
		PathFinder.buildDistanceMap( enemy.pos, Dungeon.level.water, viewDistance );
		return PathFinder.distance[pos] != Integer.MAX_VALUE;
	}

	private class Sleeping extends Mob.Sleeping {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			return super.act( reachable( enemyInFOV ), justAlerted );
		}
	}

	private class Wandering extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			return super.act( reachable( enemyInFOV ), justAlerted );
		}
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			return super.act( reachable( enemyInFOV ), justAlerted );
		}
	}
}
