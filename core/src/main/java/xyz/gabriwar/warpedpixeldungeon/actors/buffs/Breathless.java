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


package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.LayerHazards;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * Thin air on the world's high slices (LayerHazards.HIGH and up): the breath grows shorter the
 * longer one stays, a stack at a time up to the slice's ceiling (LayerHazards.maxStacks), every
 * stack slowing natural regeneration (Regeneration) and costing a little hunger. Going down
 * eases it a stack every eight turns; resting beside a fire up there or under a roof (a hut, a
 * waystation: OverworldLevel.shelterAt), every two, until it is gone. HazardWatch puts it on as a
 * hero stands that high and is not resting so sheltered.
 */
public class Breathless extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	//how short of breath, and the turns towards the next stack up or down
	public int stacks = 1;
	private int climb = 0, recover = 0;

	//turns to ease a stack off: below the thin air, and resting in shelter up in it
	private static final int EASE = 8, EASE_SHELTERED = 2;

	private static int altitude(){
		return Dungeon.level instanceof OverworldLevel ? ((OverworldLevel) Dungeon.level).altitude() : Integer.MIN_VALUE;
	}

	/** Resting beside a fire (a camp's embers, a hearth) lets the breath come back up here. */
	public static boolean sheltered( Char ch ){
		return ch instanceof Hero && ((Hero) ch).resting && (TileTemperature.nearFire( ch.pos )
				|| (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).shelterAt( ch.pos )));
	}

	private void say( String prefix, String key ){
		if (target instanceof Hero) NetManager.heroLog( (Hero) target, prefix + Messages.get( this, key ) );
	}

	@Override
	public boolean act(){
		int alt = altitude();
		boolean sheltered = sheltered( target );
		if (alt >= LayerHazards.HIGH && !sheltered){
			int cap = LayerHazards.maxStacks( alt );
			if (stacks < cap){
				recover = 0;
				if (++climb >= LayerHazards.stackEvery( alt )){
					stacks++;
					climb = 0;
					say( GLog.WARNING, "worse" );
				}
			} else if (stacks > cap){
				//come down a slice but still high: it eases to this slice's ceiling
				climb = 0;
				if (++recover >= EASE){
					stacks--;
					recover = 0;
				}
			} else {
				climb = 0;
				recover = 0;
			}
			Hunger hunger = target.buff( Hunger.class );
			if (hunger != null) hunger.affectHunger( -0.05f * stacks );
		} else {
			climb = 0;
			if (++recover >= (sheltered ? EASE_SHELTERED : EASE)){
				stacks--;
				recover = 0;
			}
			if (stacks <= 0){
				say( GLog.POSITIVE, "better" );
				detach();
				return true;
			}
		}
		spend( TICK );
		return true;
	}

	/** What natural regeneration is slowed by: 15% a stack. */
	public float regenFactor(){
		return 1f - 0.15f * stacks;
	}

	@Override
	public int icon(){
		return BuffIndicator.BREATHLESS;
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( stacks );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", Math.round( 100 * (1f - regenFactor()) ), stacks,
				Math.max( stacks, LayerHazards.maxStacks( altitude() ) ) );
	}

	private static final String STACKS = "stacks", CLIMB = "climb", RECOVER = "recover";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
		bundle.put( CLIMB, climb );
		bundle.put( RECOVER, recover );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		stacks = bundle.contains( STACKS ) ? bundle.getInt( STACKS ) : 1;
		climb = bundle.contains( CLIMB ) ? bundle.getInt( CLIMB ) : 0;
		recover = bundle.contains( RECOVER ) ? bundle.getInt( RECOVER ) : 0;
	}
}
