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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BedrollSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The bedroll of a Wayfarer's Camp: somewhere to sleep that is not an inn. Resting on it
 * moves the world's clock on to the start of the next phase of the day, or all the way to
 * dawn - the same jump the inn's bed makes (InnKeeper.rest), so the sky, the weather and
 * every timer that reads the game clock move with it. That is the camp's real use: the
 * rooms and rites that want a certain sky can be waited for here.
 *
 * It is a camp and not an inn, so the night is not free:
 *   - sleep is lighter: a quarter of max health back, not a third
 *   - hunger keeps a quarter of its pace through the sleep (it never starves the sleeper)
 *   - an unwatched sleeper can be found. The odds grow with how long is slept and with the
 *     moon (GameCalendar.moonSpawnMultiplier: a new moon half again, a full moon half),
 *     and a fire burning by the bedroll halves them. Found, the hero wakes early, mid-
 *     phase, with company.
 *   - a cold camp with no fire by it sends the sleeper off to a cold awakening
 *
 * The clock is the party's, so in multiplayer the bedroll is the host's to use.
 */
public class Bedroll extends NPC {

	{
		spriteClass = BedrollSprite.class;

		properties.add( Property.IMMOVABLE );
		properties.add( Property.INORGANIC );
	}

	@Override
	public boolean interact( Char c ){
		if (NetDialogs.handleNetHero( c, Messages.get( this, "net_host_only" ) )) return true;
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;

		if (hero.visibleEnemies() > 0){
			GLog.w( Messages.get( this, "not_safe" ) );
			return true;
		}

		final DayNightCycle.Phase next = DayNightCycle.phase().next();
		final boolean dawnIsNext = next == DayNightCycle.Phase.DAWN;
		Game.runOnRenderThread( new Callback(){
			@Override
			public void call(){
				final String[] options = dawnIsNext
						? new String[]{ Messages.get( Bedroll.class, "until_dawn" ),
										Messages.get( Bedroll.class, "stay_up" ) }
						: new String[]{ Messages.get( Bedroll.class, "until_phase", phaseName( next ) ),
										Messages.get( Bedroll.class, "until_dawn" ),
										Messages.get( Bedroll.class, "stay_up" ) };
				GameScene.show( new WndOptions( sprite(),
						Messages.titleCase( name() ),
						Messages.get( Bedroll.class, fireBy() ? "menu_fire" : "menu_cold" ),
						options ){
					@Override
					protected void onSelect( int index ){
						//the last option is always "stay up"
						if (index == options.length - 1) return;
						rest( hero, dawnIsNext || index == 1 );
					}
				} );
			}
		} );
		return true;
	}

	private static String phaseName( DayNightCycle.Phase phase ){
		return Messages.get( Bedroll.class, "phase_" + phase.name().toLowerCase( java.util.Locale.ENGLISH ) );
	}

	/** the camp's fire pit, set by the room; -1 for a bedroll laid down with no pit */
	public int firePit = -1;

	/** a mountain waystation's (levels/overworld/MountainSites): it lies by a fire that is never
	 *  let out, under a roof - always warm, and nobody finds a sleeper there */
	public boolean shelter = false;

	//a fire that is burning, not a pit of old embers: the camp's own pit alight, or
	//failing a pit any open flame beside the bed
	private boolean fireBy(){
		if (shelter) return true;
		if (firePit >= 0){
			CampFire fire = (CampFire) Dungeon.level.blobs.get( CampFire.class );
			return fire != null && fire.burning( firePit );
		}
		Blob flame = Dungeon.level.blobs.get( Fire.class );
		if (flame == null || flame.volume <= 0) return false;
		for (int ofs : PathFinder.NEIGHBOURS9){
			int c = pos + ofs;
			if (Dungeon.level.insideMap( c ) && flame.cur[c] > 0) return true;
		}
		return false;
	}

	//turns from now to the start of the wanted phase
	private static int turnsTo( boolean toDawn ){
		int turns = DayNightCycle.turnsUntilPhaseChange();
		if (!toDawn) return turns;
		DayNightCycle.Phase p = DayNightCycle.phase().next();
		while (p != DayNightCycle.Phase.DAWN){
			turns += DayNightCycle.phaseDuration( p );
			p = p.next();
		}
		return turns;
	}

	private void rest( Hero hero, boolean toDawn ){
		boolean fire = fireBy();
		boolean realClock = Dungeon.isChallenged( Challenges.REAL_CLOCK );
		int turns = realClock ? 0 : Math.max( 1, turnsTo( toDawn ) );

		//is the sleeper found? longer sleeps and darker moons are riskier, a fire safer
		float risk = (0.12f + 0.10f * turns / 600f) * GameCalendar.moonSpawnMultiplier();
		if (fire) risk *= 0.5f;
		boolean found = !realClock && !shelter && Random.Float() < Math.min( 0.75f, risk );
		if (found) turns = Math.round( turns * Random.Float( 0.3f, 0.7f ) );

		hero.sprite.operate( pos );
		GameScene.flash( 0xFF000000, false );

		Sleepiness tired = hero.buff( Sleepiness.class );
		if (tired != null) tired.wake( found ? tired.level() / 2f : tired.level() );
		Buff.detach( hero, Heatstroke.class );
		Buff.detach( hero, SoakedShoes.class );
		Buff.detach( hero, Windswept.class );
		if (!found) hero.heal( hero.HT / 4 );

		if (turns > 0){
			//the sleeper's stomach keeps a quarter of its pace, and never reaches starving
			Hunger hunger = hero.buff( Hunger.class );
			if (hunger != null){
				float cost = Math.min( turns * 0.25f, Math.max( 0f, Hunger.STARVING - 1f - hunger.hunger() ) );
				if (cost > 0) hunger.affectHunger( -cost );
			}
			Dungeon.cycleTurn += turns;
			Statistics.duration += turns;
			ClimateManager.onHeroTurn();
		}

		//a fire by the bed is the whole difference on a cold floor
		if (fire){
			Buff.detach( hero, Hypothermia.class );
		} else if (TileTemperature.feelsLikeAt( hero.pos, hero ) < Hypothermia.WARN_TEMP
				&& hero.buff( Hypothermia.class ) == null){
			Buff.affect( hero, Hypothermia.class );
			GLog.w( Messages.get( this, "woke_cold" ) );
		}

		if (found){
			ambush( hero );
			GLog.n( Messages.get( this, "woke_found" ) );
		} else if (realClock){
			GLog.p( Messages.get( this, "rested_clock" ) );
		} else {
			GLog.p( Messages.get( this, "rested", phaseName( DayNightCycle.phase() ) ) );
		}

		hero.spendAndNext( 1f );
		Dungeon.observe();
		GameScene.updateFog();
	}

	//one or two of the floor's own, already on top of the sleeper
	private void ambush( Hero hero ){
		ArrayList<Class<? extends Mob>> rotation = MobSpawner.getMobRotation( WarpedRooms.threat() );
		if (rotation.isEmpty()) return;
		ArrayList<Integer> spots = new ArrayList<>();
		PathFinder.buildDistanceMap( hero.pos, Dungeon.level.passable, 3 );
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (PathFinder.distance[i] >= 2 && PathFinder.distance[i] <= 3 && Actor.findChar( i ) == null){
				spots.add( i );
			}
		}
		Random.shuffle( spots );
		int count = Math.min( spots.size(), Random.IntRange( 1, 2 ) );
		for (int i = 0; i < count; i++){
			Mob mob = Reflection.newInstance( rotation.get( i % rotation.size() ) );
			if (mob == null) continue;
			mob.pos = spots.get( i );
			mob.state = mob.HUNTING;
			GameScene.add( mob );
			mob.beckon( hero.pos );
		}
	}

	@Override
	public void damage( int dmg, Object src ){
	}

	@Override
	public boolean add( Buff buff ){
		return false;
	}

	@Override
	public boolean reset(){
		return true;
	}

	private static final String FIRE_PIT = "fire_pit";
	private static final String SHELTER = "shelter";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FIRE_PIT, firePit );
		bundle.put( SHELTER, shelter );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
		super.restoreFromBundle( bundle );
		firePit = bundle.contains( FIRE_PIT ) ? bundle.getInt( FIRE_PIT ) : -1;
		shelter = bundle.contains( SHELTER ) && bundle.getBoolean( SHELTER );
	}
}
