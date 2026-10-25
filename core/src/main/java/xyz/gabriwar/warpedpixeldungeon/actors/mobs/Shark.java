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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sea.Afloat;
import xyz.gabriwar.warpedpixeldungeon.sea.LanternGlare;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.sprites.SharkSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * A shark of the open sea (levels/overworld/SeaWorld sets them down out of sight, where the water
 * runs three tiers deep). It swims in deep water alone and goes for whoever is in the water or on it
 * beside its deep: a swimmer, a wader at the deep's edge, a boat. A bite on a boat lands on its hull -
 * a raft takes all of it and now and then its sailor's legs take half, a sailboat's planks take half
 * and nothing gets through - so a shark can sink a boat under its sailor. Its fin shows as it cruises,
 * and the first it is seen it is told of. A lit sea lantern's glare (LanternGlare) keeps it off.
 */
public class Shark extends Mob {

	{
		spriteClass = SharkSprite.class;

		HP = HT = 85;
		defenseSkill = 18;
		baseSpeed = 1.25f;

		EXP = 11;
		maxLvl = 28;

		loot = MysteryMeat.class;
		lootChance = 0.5f;

		WANDERING = new Cruising();
		state = WANDERING;
	}

	//whether its fin has been told of to the heroes in sight
	private boolean told = false;

	@Override
	public int damageRoll(){
		return Random.NormalIntRange( 10, 22 );
	}

	@Override
	public int attackSkill( Char target ){
		return 34;
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, 6 );
	}

	/** Water a shark is set down in: the deep, three tiers down or more (WindowGenerator's tiers). */
	public static boolean openSea( Level l, int cell ){
		if (cell < 0 || cell >= l.length() || l.map[cell] != Terrain.DEEP_WATER) return false;
		return !(l instanceof OverworldLevel) || ((OverworldLevel) l).waterTierAt( cell ) >= 3;
	}

	//the deep water it swims through
	private static boolean[] deep( Level l ){
		boolean[] pass = new boolean[l.length()];
		for (int c = 0; c < pass.length; c++) pass[c] = l.map[c] == Terrain.DEEP_WATER;
		return pass;
	}

	/** Can it get its teeth into this one: in the water, or on it in a boat, and no sea lantern's glare on them. */
	public static boolean prey( Char ch ){
		if (ch == null || !ch.isAlive() || ch.flying || ch.buff( LanternGlare.class ) != null) return false;
		Level l = Dungeon.level;
		if (l == null || ch.pos < 0 || ch.pos >= l.length()) return false;
		return Seafaring.water( l, ch.pos ) || Seafaring.aboard( ch );
	}

	@Override
	protected boolean act(){
		//stranded (the sea froze round it, or it was carried out of the deep): it dies, a death the
		//season dealt and no hero earned, so it leaves no experience and no meat
		if (Dungeon.level.map[pos] != Terrain.DEEP_WATER || flying){
			EXP = 0;
			lootChance = 0;
			die( null );
			return true;
		}
		if (!told && Dungeon.level.heroFOV[pos]){
			told = true;
			GuideGraph.reveal( GuideGraph.TAG_SHARK );
			for (Hero h : OverworldLevel.heroesOn( Dungeon.level )){
				if (h.fieldOfView != null && h.fieldOfView.length == Dungeon.level.length() && h.fieldOfView[pos]){
					NetManager.heroLog( h, GLog.WARNING + Messages.get( this, "fin" ) );
				}
			}
		}
		return super.act();
	}

	//only what it can reach is worth a look: a hero ashore is no prey
	@Override
	protected Char chooseEnemy(){
		Char e = super.chooseEnemy();
		return prey( e ) ? e : null;
	}

	@Override
	protected boolean canAttack( Char enemy ){
		return prey( enemy ) && super.canAttack( enemy );
	}

	@Override
	protected boolean getCloser( int target ){
		if (rooted) return false;
		int step = Dungeon.findStep( this, target, deep( Dungeon.level ), fieldOfView, true );
		if (step == -1) return false;
		move( step );
		return true;
	}

	@Override
	protected boolean getFurther( int target ){
		int step = Dungeon.flee( this, target, deep( Dungeon.level ), fieldOfView, true );
		if (step == -1) return false;
		move( step );
		return true;
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		damage = super.attackProc( enemy, damage );
		Afloat a = Seafaring.aboard( enemy ) ? Seafaring.boat( enemy ) : null;
		if (a != null && enemy instanceof Hero){
			//the teeth go into the hull
			boolean raft = a.kind == Boat.Kind.RAFT;
			NetManager.heroLog( (Hero) enemy, GLog.WARNING + Messages.get( this, raft ? "rams_raft" : "rams_sail" ) );
			Seafaring.damage( (Hero) enemy, a, raft ? damage : Math.max( 1, damage / 2 ) );
			//a raft rides low: one bite in three takes its sailor's legs as well
			return raft && Random.Int( 3 ) == 0 ? damage / 2 : 0;
		}
		if (Random.Int( 4 ) == 0) Buff.affect( enemy, Bleeding.class ).set( damage / 3f );
		return damage;
	}

	@Override
	public float spawningWeight(){
		return 0;
	}

	@Override
	public boolean reset(){
		return true;
	}

	//cruising the deep: a new stretch of open water to swim to now and then, never ashore
	private class Cruising extends Mob.Wandering {
		@Override
		protected int randomDestination(){
			Level l = Dungeon.level;
			int w = l.width();
			for (int tries = 0; tries < 20; tries++){
				int x = pos % w + Random.IntRange( -10, 10 ), y = pos / w + Random.IntRange( -10, 10 );
				if (x < 1 || y < 1 || x >= w - 1 || y >= l.height() - 1) continue;
				int c = x + y * w;
				if (l.map[c] == Terrain.DEEP_WATER) return c;
			}
			return pos;
		}
	}
}
