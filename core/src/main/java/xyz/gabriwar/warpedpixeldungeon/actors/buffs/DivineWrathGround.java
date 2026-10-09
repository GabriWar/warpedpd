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

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Divine Wrath: holy ground left by the cleric's blows. Enemies standing on it burn each
 * turn, twice as hard if unholy; at mastery the ground holds them in place.
 */
public class DivineWrathGround extends Buff {

	/** turns a tile stays consecrated after its last blow */
	public static final int DURATION = 3;
	/** holy damage each turn to an enemy on the ground */
	public static final int BURN = 4;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int[] cells = new int[0];
	private int[] left = new int[0];
	private int rank = 1;
	private int depth = -1, branch = -1;

	private final ArrayList<Emitter> shafts = new ArrayList<>();

	private boolean sameFloor(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	public void consecrate( Collection<Integer> add, int rank ){
		if (!sameFloor()){
			cells = new int[0];
			left = new int[0];
			depth = Dungeon.depth;
			branch = Dungeon.branch;
		}
		this.rank = rank;
		for (int c : add){
			int i = indexOf( c );
			if (i >= 0){
				left[i] = DURATION;
				continue;
			}
			cells = java.util.Arrays.copyOf( cells, cells.length + 1 );
			left = java.util.Arrays.copyOf( left, left.length + 1 );
			cells[cells.length - 1] = c;
			left[left.length - 1] = DURATION;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
		}
		fx( true );
	}

	private int indexOf( int cell ){
		for (int i = 0; i < cells.length; i++) if (cells[i] == cell) return i;
		return -1;
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || !sameFloor() || cells.length == 0){
			detach();
			return true;
		}
		//the cell nearest the hero that burned, where the sound comes from
		int burned = -1;
		int kept = 0;
		int[] nc = new int[cells.length];
		int[] nl = new int[cells.length];
		for (int i = 0; i < cells.length; i++){
			int c = cells[i];
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				boolean unholy = Char.hasProp( ch, Char.Property.UNDEAD ) || Char.hasProp( ch, Char.Property.DEMONIC );
				ch.damage( unholy ? BURN * 2 : BURN, this );
				SkillFX.flash( ch );
				if (Dungeon.level.heroFOV[c]) CellEmitter.center( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), unholy ? 6 : 3 );
				if (ch.isAlive() && rank >= Skill.MAX_LEVEL && !Char.hasProp( ch, Char.Property.BOSS ))
					Buff.prolong( ch, Roots.class, 2f );
				burned = SpatialSound.nearer( burned, c );
			}
			if (--left[i] > 0){
				nc[kept] = c;
				nl[kept++] = left[i];
			}
		}
		if (burned >= 0) SpatialSound.play( Assets.Sounds.BURNING, burned, 0.6f, 1.4f );
		boolean changed = kept != cells.length;
		cells = java.util.Arrays.copyOf( nc, kept );
		left = java.util.Arrays.copyOf( nl, kept );
		if (cells.length == 0){
			detach();
			return true;
		}
		if (changed) fx( true );
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		for (Emitter e : shafts) e.on = false;
		shafts.clear();
		if (!on || target.sprite == null || target.sprite.parent == null || !sameFloor()) return;
		for (int c : cells){
			Emitter e = CellEmitter.get( c );
			e.pour( ShaftParticle.FACTORY, 0.5f );
			shafts.add( e );
		}
	}

	private static final String CELLS = "cells", LEFT = "left", RANK = "rank", DEPTH = "depth", BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CELLS, cells );
		bundle.put( LEFT, left );
		bundle.put( RANK, rank );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cells = bundle.getIntArray( CELLS );
		left = bundle.getIntArray( LEFT );
		rank = bundle.getInt( RANK );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
		if (cells == null || left == null || cells.length != left.length){
			cells = new int[0];
			left = new int[0];
		}
	}
}
