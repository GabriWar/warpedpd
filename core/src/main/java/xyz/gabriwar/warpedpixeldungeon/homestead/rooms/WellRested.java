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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * A whole night in a bed (HomeBed) leaves the hero WELL RESTED: XP_BONUS more experience from
 * everything he earns (Hero.earnExp) and REGEN_BONUS faster natural healing (Regeneration), until
 * the next dusk in the best rooms - the less comfortable the room, the sooner it wears off
 * (duration: from half the time to dusk at no comfort to all of it at the most).
 */
public class WellRested extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float XP_BONUS = 0.1f, REGEN_BONUS = 0.1f;
	/** On a real-clock run, whose clock does not count turns: the turns to a dusk, as a stand-in. */
	public static final int REAL_CLOCK_TURNS = 1000;

	//what it lasted when it began, for the icon's fade; the share of experience not yet given
	private float total = 1f;
	private float partialExp;

	/** The world turns from now to the start of the next dusk (a whole day's when it is dusk now). */
	public static int turnsToDusk(){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )) return REAL_CLOCK_TURNS;
		int dusk = DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) + DayNightCycle.phaseDuration( DayNightCycle.Phase.DAY );
		int pos = Math.floorMod( Dungeon.cycleTurn, DayNightCycle.FULL_CYCLE );
		return pos < dusk ? dusk - pos : DayNightCycle.FULL_CYCLE - pos + dusk;
	}

	/** How long it lasts after a night in a room of this comfort (0 outside any room). */
	public static int duration( int comfort ){
		return Math.max( 1, Math.round( turnsToDusk() * (0.5f + 0.5f * Comfort.fraction( comfort )) ) );
	}

	/** Leaves the hero well rested for `turns` (never cut short by a worse night). */
	public static void rest( Char hero, int turns ){
		WellRested b = prolong( hero, WellRested.class, turns );
		b.total = Math.max( 1f, b.cooldown() );
	}

	/** The experience over `exp` a well rested hero earns (whole points; the rest is kept). */
	public static int bonusExp( Char hero, int exp ){
		WellRested b = hero.buff( WellRested.class );
		if (b == null || exp <= 0) return 0;
		b.partialExp += exp * XP_BONUS;
		int whole = (int) b.partialExp;
		b.partialExp -= whole;
		return whole;
	}

	/** How much faster a well rested hero heals (Regeneration divides its delay by it). */
	public static float regenFactor( Char ch ){
		return ch.buff( WellRested.class ) != null ? 1f + REGEN_BONUS : 1f;
	}

	@Override
	public int icon(){
		return BuffIndicator.MAGIC_SLEEP;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 1f, 0.85f, 0.45f );
	}

	@Override
	public float iconFadePercent(){
		return Math.max( 0, (total - visualcooldown()) / total );
	}

	private static final String TOTAL = "total", PARTIAL = "partial_exp";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( TOTAL, total );
		bundle.put( PARTIAL, partialExp );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		total = Math.max( 1f, bundle.getFloat( TOTAL ) );
		partialExp = bundle.getFloat( PARTIAL );
	}
}
