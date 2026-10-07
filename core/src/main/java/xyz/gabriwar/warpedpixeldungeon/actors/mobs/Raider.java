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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A bandit come down on a human village for the day of a raid (levels.overworld.RaidEvent).
 * They prowl from door to door, set on the guards at the well, and fight anyone who comes to
 * help - no cutting purses and running. The guards wear them down but never finish one: that is
 * left to the hero. When the day is over the band makes off with what it took, out of sight and
 * out of the world (OverworldBandit's retreat).
 */
public class Raider extends OverworldBandit {

	public static final int LOOTER = 0, FIGHTER = 1;

	//the raided village (packed sector), the raid's day (the world's events' day, RaidEvent.day),
	//what this one does there and the village's well, world cells
	public long sector = Long.MIN_VALUE;
	public int day = Integer.MIN_VALUE;
	public int role = LOOTER;
	public int cx, cy;

	//a hero this close, in sight, draws a raider off whatever else it fights
	private static final int REACH = 3;

	{
		WANDERING = new Prowling();
		state = WANDERING;
		//the armband is no raid's prize, and the coins come by hand (die)
		lootChance = 0f;
	}

	@Override
	public boolean sentOnEvent() {
		return true;
	}

	@Override
	public boolean eventHeld() {
		return day == RaidEvent.day();
	}

	//raiders fight: no cutting purses and running
	@Override
	protected boolean steal( Hero hero ) {
		return false;
	}

	//a brawler swings once a turn: a band of six does not shred a hero the way thieves would
	@Override
	public float attackDelay() {
		return super.attackDelay() * 2f;
	}

	//the watch wears a raider down but never finishes one: that is the hero's to do (or his allies')
	@Override
	public void damage( int dmg, Object src ) {
		if (src instanceof OverworldGuard || src instanceof RoadPatrol) dmg = Math.min( dmg, HP - 1 );
		super.damage( dmg, src );
	}

	@Override
	protected boolean act() {
		//the raid's day is over: the band makes off with what it took (one the hero turned stays his)
		if (alignment == Alignment.ENEMY && !retreating && day != RaidEvent.day()){
			retreating = true;
			RaidEvent.withdrawing( this );
		}
		return super.act();
	}

	@Override
	protected Char chooseEnemy() {
		Char e = super.chooseEnemy();
		if (alignment != Alignment.ENEMY || state == PASSIVE || state == SLEEPING || state == FLEEING
				|| fieldOfView == null || fieldOfView.length != Dungeon.level.length()
				|| buff( Amok.class ) != null || buff( Charm.class ) != null
				|| buff( Terror.class ) != null || buff( Dread.class ) != null) return e;
		Hero hero = Dungeon.hero;
		boolean heroClose = hero != null && hero.isAlive() && hero.invisible <= 0 && fieldOfView[hero.pos]
				&& Dungeon.level.distance( pos, hero.pos ) <= REACH;
		//one busy at the guards turns on the hero once he is in reach
		if (heroClose && (e == null || e instanceof OverworldGuard)) return hero;
		//with nobody else to fight, a fighter goes for the watch
		if (e == null && role == FIGHTER) e = RaidEvent.guardFor( this );
		return e;
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		//what it had on it
		if (Random.Int( 2 ) == 0){
			Heap h = Dungeon.level.drop( new Gold( Random.IntRange( 10, 30 ) ), RaidEvent.dropCell( Dungeon.level, pos ) );
			if (h.sprite != null) h.sprite.drop();
		}
		RaidEvent.raiderDown( this );
	}

	//from door to door of the raided village
	private class Prowling extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			int c = RaidEvent.prowlTarget( Raider.this );
			return c != -1 ? c : super.randomDestination();
		}
	}

	private static final String SECTOR = "raid_sector";
	private static final String DAY    = "raid_day";
	private static final String ROLE   = "raid_role";
	private static final String CX     = "raid_cx";
	private static final String CY     = "raid_cy";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SECTOR, sector );
		bundle.put( DAY, day );
		bundle.put( ROLE, role );
		bundle.put( CX, cx );
		bundle.put( CY, cy );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		sector = bundle.contains( SECTOR ) ? bundle.getLong( SECTOR ) : Long.MIN_VALUE;
		//none: a raid long over, so it makes off
		day    = bundle.contains( DAY )    ? bundle.getInt( DAY )     : Integer.MIN_VALUE;
		role   = bundle.contains( ROLE )   ? bundle.getInt( ROLE )    : LOOTER;
		cx     = bundle.contains( CX )     ? bundle.getInt( CX )      : 0;
		cy     = bundle.contains( CY )     ? bundle.getInt( CY )      : 0;
	}
}
