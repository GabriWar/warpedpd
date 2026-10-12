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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.AlarmBellSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.List;

/**
 * A homestead's alarm bell (DefencePieces). While the hero sleeps or rests anywhere on its home's
 * ground (HomeThreats.AREA round the hearthstone), it rings when anything hostile comes onto that
 * ground and wakes him (ThreatSlice); a sleeper on a bed there who would be found (Bedroll.ambush)
 * wakes to its ringing instead, with whoever found him set down at the edge of the home's ground
 * rather than on top of him (wakeSleeper); and a night's sleep there is cut short when a siege or a
 * raid comes that night (sleepFor): he wakes in the dark as they come, to its ringing (warn). With
 * no bell he sleeps through them, and the night is settled as if he had been away
 * (ThreatSettler). Tap it to ring it by hand. On a home's ground it takes Notice.BELL_OFF from its
 * notice. Its health is the homestead's.
 */
public class AlarmBell extends NPC implements HomesteadActor {

	{
		spriteClass = AlarmBellSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	@Override
	public void damage( int dmg, Object src ){
		//the bell's health is the homestead's (Homestead.damage), not the actor's
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero)) return true;
		Hero hero = (Hero) c;
		ring();
		if (hero.sprite != null) hero.sprite.operate( pos );
		hero.spendAndNext( 1f );
		return true;
	}

	/** Rings it: its swing and its peal, from where it hangs. */
	public void ring(){
		if (sprite instanceof AlarmBellSprite) ((AlarmBellSprite) sprite).ring();
		if (Dungeon.level instanceof OverworldLevel && HomeThreats.live( (OverworldLevel) Dungeon.level )){
			SpatialSound.play( Assets.Sounds.BELL, pos );
		}
	}

	/** The bell standing on a home's ground, or null. */
	public static AlarmBell of( OverworldLevel level, HomesteadWorld.Home home ){
		for (Mob m : level.mobs){
			if (m instanceof AlarmBell && HomeThreats.inArea( level, home, m.pos )) return (AlarmBell) m;
		}
		return null;
	}

	/** Is the hero asleep or resting, as the bell wakes him from? */
	public static boolean sleeping( Hero hero ){
		return hero.resting || hero.buff( MagicalSleep.class ) != null;
	}

	/** Rings and wakes the hero: from his rest, from a magical sleep. */
	static void wake( AlarmBell bell, Hero hero ){
		bell.ring();
		Buff.detach( hero, MagicalSleep.class );
		//the rest broken off; on the screen, his walk and the keys held too
		if (Dungeon.level instanceof OverworldLevel && HomeThreats.live( (OverworldLevel) Dungeon.level )) hero.interrupt();
		else hero.resting = false;
		GLog.w( Messages.get( AlarmBell.class, "rings" ) );
	}

	/** A siege or a raid is coming for its home (ThreatSlice): it rings, and wakes the hero on the
	 *  home's ground. */
	static void warn( OverworldLevel level, AlarmBell bell, HomesteadWorld.Home home, Hero hero ){
		bell.ring();
		if (hero == null || !HomeThreats.inArea( level, home, hero.pos )) return;
		Buff.detach( hero, MagicalSleep.class );
		if (HomeThreats.live( level )) hero.interrupt();
		else hero.resting = false;
		GLog.w( Messages.get( AlarmBell.class, "rings_coming" ) );
	}

	/**
	 * Bedroll.rest, the hero lying down for `turns` (the clock skips them): on the ground of a home
	 * with a bell, when the night his sleep runs into brings a siege or a raid there - the next night
	 * owed, rolled ahead with the notice it will be rolled with, which it keeps - the sleep ends in
	 * that night's dark, as they come (its own turn of the night, pure: HomeThreats.SALT_WAKE), and
	 * that night's own roll sets them going and rings the bell. How many turns he sleeps.
	 */
	public static int sleepFor( Level level, Hero hero, int turns ){
		if (turns <= 0 || !(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror()
				|| ThreatWorld.get() == null) return turns;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadWorld.Home home = HomeThreats.homeAt( ow, hero.pos );
		if (home == null || of( ow, home ) == null) return turns;
		ThreatWorld world = ThreatWorld.get();
		int night = world.settledTo( home ) + 1;
		int now = WorldClock.turn();
		//its dark: after the dawn, the day and the dusk of its day
		int dark = night * DayNightCycle.FULL_CYCLE + DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN )
				+ DayNightCycle.phaseDuration( DayNightCycle.Phase.DAY ) + DayNightCycle.phaseDuration( DayNightCycle.Phase.DUSK );
		int wake = dark + 1 + (int)(HomeThreats.roll( ow.worldSeed, home, night, HomeThreats.SALT_WAKE )
				* DayNightCycle.phaseDuration( DayNightCycle.Phase.NIGHT ) / 3);
		if (dark <= now || wake >= now + turns) return turns;
		Notice.Score score = Notice.of( ow, home, true );
		int notice = score.total();
		if (!HomeThreats.siegeNight( ow.worldSeed, home, night, notice ) && !HomeThreats.raidNight( ow.worldSeed, home, night, notice )){
			return turns;
		}
		world.keepNotice( home, score );
		return wake - now;
	}

	/**
	 * Bedroll.ambush, a sleeper found on a slice: when he lies on the ground of a home with a bell,
	 * its ringing wakes him before they reach him - whoever found him is set down at the edge of
	 * the home's ground (HomeThreats.edgeCell), hunting him, instead of beside him. True when the
	 * bell rang (the ambush is dealt with), false for the ambush as it was.
	 */
	public static boolean wakeSleeper( Level level, Hero hero, List<Mob> found ){
		if (!(level instanceof OverworldLevel) || ((OverworldLevel) level).isMirror()) return false;
		OverworldLevel ow = (OverworldLevel) level;
		HomesteadWorld.Home home = HomeThreats.homeAt( ow, hero.pos );
		if (home == null) return false;
		AlarmBell bell = of( ow, home );
		int hearth = HomeThreats.hearth( ow, home );
		if (bell == null || hearth == -1) return false;
		for (int i = 0; i < found.size(); i++){
			Mob m = found.get( i );
			int c = HomeThreats.edgeCell( ow, hearth, i / (float) found.size() );
			if (c == -1) continue;
			HomeThreats.add( ow, m, c );
			m.aggro( hero );
			//on its way to where he lies (beckon shows its alert: a mob on the screen)
			if (m.sprite != null) m.beckon( hero.pos );
		}
		wake( bell, hero );
		return true;
	}
}
