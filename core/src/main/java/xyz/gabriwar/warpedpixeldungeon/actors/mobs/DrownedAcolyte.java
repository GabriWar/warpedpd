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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ParasiticInfection;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.DrownedAcolyteSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * One of a sunken shrine's drowned acolytes (levels/overworld/WetlandSites): the keepers of the
 * old god of the bog, gone down with its shrine. By night they rise out of the water round its
 * idol when anyone comes near (OverworldLevel's wetlands), wade slowly after him - never further
 * than LEASH from the shrine - and their touch leaves the bog's parasites in a wound (a third of
 * their hits: ParasiticInfection, which the shrine's own blessing keeps off). They sink back into
 * the water at sunrise unless a fight holds them, leaving nothing.
 */
public class DrownedAcolyte extends Mob {

	{
		spriteClass = DrownedAcolyteSprite.class;

		HP = HT = 32;
		defenseSkill = 9;

		EXP = 7;
		maxLvl = 14;

		//they wade
		baseSpeed = 0.8f;

		properties.add( Property.UNDEAD );

		loot = Gold.class;
		lootChance = 0.3f;

		WANDERING = new Keeping();
		state = WANDERING;
	}

	/** How far they wander round the shrine, and how far from it they give up a chase. */
	public static final int KEEP = 3, LEASH = 10;

	public long shrineKey = 0;
	public int homeX = Integer.MIN_VALUE, homeY = Integer.MIN_VALUE;

	public static DrownedAcolyte of( long shrineKey, int homeX, int homeY ){
		DrownedAcolyte a = new DrownedAcolyte();
		a.shrineKey = shrineKey;
		a.homeX = homeX;
		a.homeY = homeY;
		return a;
	}

	@Override
	public int damageRoll(){
		return Random.NormalIntRange( 4, 10 );
	}

	@Override
	public int attackSkill( Char target ){
		return 16;
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, 3 );
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 3 ) == 0 && !enemy.isImmune( ParasiticInfection.class )){
			Buff.prolong( enemy, ParasiticInfection.class, ParasiticInfection.DURATION );
		}
		return damage;
	}

	@Override
	public Item createLoot(){
		return new Gold( Random.IntRange( 15, 40 ) );
	}

	@Override
	protected boolean act(){
		//sunrise: back into the water, unless a fight holds them
		if (DayNightCycle.phase() == DayNightCycle.Phase.DAY && (state != HUNTING || !enemySeen)){
			sink();
			return true;
		}
		//never further than LEASH from the shrine: a chase that strays gives up and goes home
		if (state == HUNTING && !nearHome( pos, LEASH )){
			state = WANDERING;
			target = homeCell();
		}
		return super.act();
	}

	/** Goes back under the water: gone, with a splash where it stood. */
	public void sink(){
		if (sprite != null && sprite.parent != null && Dungeon.level.heroFOV[pos]){
			Splash.at( pos, 0x5C7A6A, 8 );
			SpatialSound.play( Assets.Sounds.WATER, pos, 0.7f, 0.8f );
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	//the shrine's cell in the window, or -1 (unknown, or off the window)
	private int homeCell(){
		if (!(Dungeon.level instanceof OverworldLevel) || homeX == Integer.MIN_VALUE) return -1;
		return ((OverworldLevel) Dungeon.level).localCell( homeX, homeY );
	}

	private boolean nearHome( int cell, int r ){
		if (!(Dungeon.level instanceof OverworldLevel) || homeX == Integer.MIN_VALUE) return true;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		int wx = ow.worldX() + cell % ow.width(), wy = ow.worldY() + cell / ow.width();
		return Math.max( Math.abs( wx - homeX ), Math.abs( wy - homeY ) ) <= r;
	}

	//they wander the water round the idol
	private class Keeping extends Wandering {
		//out past the leash nothing draws one further off: it wades home first. Noticing a foe
		//spends no time and alerts it, so a hero by its side out there would turn it back to the
		//hunt and the leash back to wandering, turn after turn, without the clock ever moving
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ){
			if (!nearHome( pos, LEASH )) return continueWandering();
			return super.act( enemyInFOV, justAlerted );
		}

		@Override
		protected int randomDestination(){
			int home = homeCell();
			if (home == -1) return super.randomDestination();
			int w = Dungeon.level.width();
			for (int i = 0; i < 12; i++){
				int c = home + Random.IntRange( -KEEP, KEEP ) + Random.IntRange( -KEEP, KEEP ) * w;
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]) return c;
			}
			return home;
		}
	}

	private static final String SHRINE = "acolyte_shrine";
	private static final String HOME_X = "home_x";
	private static final String HOME_Y = "home_y";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SHRINE, shrineKey );
		bundle.put( HOME_X, homeX );
		bundle.put( HOME_Y, homeY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		shrineKey = bundle.contains( SHRINE ) ? bundle.getLong( SHRINE ) : 0;
		homeX = bundle.contains( HOME_X ) ? bundle.getInt( HOME_X ) : Integer.MIN_VALUE;
		homeY = bundle.contains( HOME_Y ) ? bundle.getInt( HOME_Y ) : Integer.MIN_VALUE;
	}
}
