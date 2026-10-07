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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.EagleSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A mountain eagle, one of the pair guarding an eyrie on the peaks (levels/overworld/MountainSites).
 * It circles within a few cells of its nest and leaves a passer-by alone - but anything that comes
 * within AGGRO of the nest, or strikes it from afar, it stoops on: a dive from range lands half
 * as hard again, and after every strike it climbs off before diving again. Badly hurt it flies off,
 * and a thief who took its eggs (EagleEgg) it chases until it has gone LEASH_HARD cells from home;
 * then it gives up and goes back. It sleeps on the nest at night.
 *
 * Not wildlife: never culled, never counted in the fauna, kept in the parked store forever
 * (OverworldLevel.keepsForever) and placed once per eyrie - when the last of the pair dies the
 * eyrie is cleared for good.
 */
public class Eagle extends Mob {

	{
		spriteClass = EagleSprite.class;

		HP = HT = 95;
		defenseSkill = 30;

		EXP = 12;
		maxLvl = 30;

		baseSpeed = 1.5f;
		flying = true;

		//feathered for the peaks' cold
		thermal = Thermal.COLD_DWELLER;

		loot = MonsterMeat.class;
		lootChance = 0.25f;

		WANDERING = new Circling();
		HUNTING = new Guarding();
		FLEEING = new Retreat();
		state = WANDERING;
	}

	/** it stoops on anything within this many cells of its nest... */
	public static final int AGGRO = 3;
	/** ...gives up on one more than this far from the nest unless robbed... */
	public static final int LEASH = 6;
	/** ...and on anything at all once it is this far from home itself */
	public static final int LEASH_HARD = 12;
	//it circles within this of the nest
	private static final int RANGE = 4;
	//it flies off below this share of its health; a dive from range lands this much harder
	private static final float RETREAT = 0.35f, DIVE = 1.5f;

	public long eyrieKey = 0;
	public int nestX = Integer.MIN_VALUE, nestY = Integer.MIN_VALUE;
	public boolean angered = false;

	//the stoop: a turn spent climbing off after a strike, and a dive under way (not bundled)
	private int climb = 0;
	private boolean diving = false;
	private int regen = 0;

	public static Eagle of( long eyrieKey, int nestX, int nestY ){
		Eagle e = new Eagle();
		e.eyrieKey = eyrieKey;
		e.nestX = nestX;
		e.nestY = nestY;
		return e;
	}

	@Override
	public int damageRoll(){
		return Random.NormalIntRange( 16, 30 );
	}

	@Override
	public int attackSkill( Char target ){
		return 38;
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, 8 );
	}

	/** Is the world cell (wx, wy) within r of the nest (Chebyshev)? Always, when the nest is unknown. */
	public static boolean nearNest( int nestX, int nestY, int wx, int wy, int r ){
		if (nestX == Integer.MIN_VALUE) return true;
		return Math.max( Math.abs( wx - nestX ), Math.abs( wy - nestY ) ) <= r;
	}

	//...the same for a cell of the level the eagle is on (the overworld's window is world-anchored)
	private boolean nearNest( int cell, int r ){
		if (!(Dungeon.level instanceof OverworldLevel) || nestX == Integer.MIN_VALUE) return true;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		return nearNest( nestX, nestY, ow.worldX() + cell % ow.width(), ow.worldY() + cell / ow.width(), r );
	}

	//the nest's cell in the level, or -1 when it is not in the window (or unknown)
	private int nestCell(){
		if (!(Dungeon.level instanceof OverworldLevel) || nestX == Integer.MIN_VALUE) return -1;
		return ((OverworldLevel) Dungeon.level).localCell( nestX, nestY );
	}

	//the nest has slid out of the window: the window's edge cell nearest it, so the eagle heads
	//home rather than off over the whole map (or where it is, when its nest is unknown)
	private int towardNest(){
		if (!(Dungeon.level instanceof OverworldLevel) || nestX == Integer.MIN_VALUE) return pos;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int x = Math.max( 1, Math.min( ow.width() - 2, nestX - ow.worldX() ) );
		int y = Math.max( 1, Math.min( ow.height() - 2, nestY - ow.worldY() ) );
		return x + y * ow.width();
	}

	/** Does this eagle guard the nest on this world cell? */
	public boolean guards( int wx, int wy ){
		return nestX == wx && nestY == wy;
	}

	/** A thief took its eggs: it hunts the robber, however far from the nest, until LEASH_HARD. */
	public void enrage( Char robber ){
		angered = true;
		aggro( robber );
		target = robber.pos;
	}

	//gone further from home than it will ever follow: it drops the chase and flies back
	private boolean goHome(){
		if (nearNest( pos, LEASH_HARD )) return false;
		angered = false;
		state = WANDERING;
		int nest = nestCell();
		target = nest != -1 ? nest : towardNest();
		spend( TICK );
		return true;
	}

	@Override
	protected boolean act(){
		if (state == SLEEPING && !DayNightCycle.isNight()) state = WANDERING;
		//it mends slowly when it is not fighting
		if (state != HUNTING && HP < HT && ++regen % 4 == 0) HP++;
		return super.act();
	}

	@Override
	protected boolean getCloser( int target ){
		boolean far = state == HUNTING && enemy != null && Dungeon.level.distance( pos, enemy.pos ) >= 2;
		boolean moved = super.getCloser( target );
		if (moved && far) diving = true;
		return moved;
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		int d = super.attackProc( enemy, damage );
		if (diving){
			d = Math.round( d * DIVE );
			diving = false;
		}
		climb = 1;
		return d;
	}

	@Override
	public void damage( int dmg, Object src ){
		super.damage( dmg, src );
		if (isAlive() && HP <= HT * RETREAT && state != FLEEING) state = FLEEING;
	}

	@Override
	public void die( Object cause ){
		super.die( cause );
		//the last of the pair gone: the eyrie is empty for good
		if (Dungeon.level instanceof OverworldLevel && eyrieKey != 0){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			if (ow.eyrieGuards( eyrieKey ) == 0) ow.markSiteCleared( eyrieKey );
		}
	}

	//wandering, as an eagle does it: round and round over its nest
	protected class Circling extends Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			//struck from afar: it remembers who did it
			if (enemyInFOV && justAlerted) angered = true;
			//no stealth gets past an eagle watching its nest
			if (enemyInFOV && (angered || nearNest( enemy.pos, AGGRO ))){
				return noticeEnemy();
			}
			int nest = nestCell();
			if (DayNightCycle.isNight() && nest != -1 && Dungeon.level.distance( pos, nest ) <= 1){
				state = SLEEPING;
				spend( TICK );
				return true;
			}
			return continueWandering();
		}

		@Override
		protected int randomDestination(){
			int nest = nestCell();
			if (nest == -1) return towardNest();
			if (DayNightCycle.isNight()) return nest;
			int w = Dungeon.level.width();
			for (int i = 0; i < 12; i++){
				int c = nest + Random.IntRange( -RANGE, RANGE ) + Random.IntRange( -RANGE, RANGE ) * w;
				//it flies: open air over a drop is as good as ground
				if (c > 0 && c < Dungeon.level.length() && Dungeon.level.insideMap( c ) && !Dungeon.level.solid[c]) return c;
			}
			return nest;
		}
	}

	//hunting, held to the nest: a stoop, a climb off, another stoop
	protected class Guarding extends Hunting {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (goHome()) return true;
			if (enemy != null && !angered && !nearNest( enemy.pos, LEASH )){
				//it does not chase a trespasser off its ground
				state = WANDERING;
				int nest = nestCell();
				target = nest != -1 ? nest : ((Wandering) WANDERING).randomDestination();
				spend( TICK );
				return true;
			}
			if (climb > 0 && enemy != null){
				climb--;
				int old = pos;
				if (getFurther( enemy.pos )){
					spend( 1 / speed() );
					return moveSprite( old, pos );
				}
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	//fleeing, but never further than its leash, and back to the nest once clear
	protected class Retreat extends Fleeing {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (goHome()) return true;
			return super.act( enemyInFOV, justAlerted );
		}

		@Override
		protected void escaped(){
			state = WANDERING;
			int nest = nestCell();
			target = nest != -1 ? nest : ((Wandering) WANDERING).randomDestination();
		}
	}

	private static final String EYRIE_KEY = "eyrie_key";
	private static final String NEST_X = "nest_x";
	private static final String NEST_Y = "nest_y";
	private static final String ANGERED = "angered";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( EYRIE_KEY, eyrieKey );
		bundle.put( NEST_X, nestX );
		bundle.put( NEST_Y, nestY );
		bundle.put( ANGERED, angered );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		eyrieKey = bundle.contains( EYRIE_KEY ) ? bundle.getLong( EYRIE_KEY ) : 0;
		nestX = bundle.contains( NEST_X ) ? bundle.getInt( NEST_X ) : Integer.MIN_VALUE;
		nestY = bundle.contains( NEST_Y ) ? bundle.getInt( NEST_Y ) : Integer.MIN_VALUE;
		angered = bundle.contains( ANGERED ) && bundle.getBoolean( ANGERED );
	}
}
