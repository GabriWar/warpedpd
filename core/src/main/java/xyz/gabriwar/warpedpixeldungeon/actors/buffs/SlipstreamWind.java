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

import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Slipstream's trail: the last few tiles the hero ran over hold a swirl of wind.
 * An enemy found standing in a swirl is tripped and the swirl blows away.
 */
public class SlipstreamWind extends Buff {

	public static final float DURATION = 10f;
	private static final float STEP = 0.5f;

	{
		type = buffType.POSITIVE;
	}

	private int rank = 1;
	private float remaining = DURATION;
	private int last = -1;
	private int depth, branch;
	//oldest tile first
	private final ArrayList<Integer> cells = new ArrayList<>();
	private final ArrayList<WindParticle.Wind> swirls = new ArrayList<>();

	public void set( int rank ){
		this.rank = rank;
		remaining = DURATION;
		last = target.pos;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		cells.clear();
		fx( true );
	}

	public boolean sameFloor(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	public boolean isEmpty(){
		return cells.isEmpty();
	}

	/** 4/6/8 tiles of wind */
	private int length(){
		return 2 + 2 * rank;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public boolean act() {
		if (!sameFloor() || !target.isAlive()){
			detach();
			return true;
		}
		if (target.pos != last){
			record( last, target.pos );
			last = target.pos;
		}
		trip();
		remaining -= STEP;
		if (remaining <= 0) detach();
		else spend( STEP );
		return true;
	}

	private void record( int from, int to ){
		if (!SkillInteractions.valid( from )) return;
		ArrayList<Integer> stride = new ArrayList<>();
		//a hasted stride can cover two tiles between checks: fill the tile in between
		if (Dungeon.level.distance( from, to ) == 2){
			Ballistica line = new Ballistica( from, to, Ballistica.STOP_TARGET );
			for (int i = 0; i < line.dist; i++) stride.add( line.path.get( i ) );
		} else {
			stride.add( from );
		}
		for (int c : stride){
			if (Dungeon.level.solid[c] || Dungeon.level.pit[c] || c == target.pos) continue;
			cells.remove( (Integer) c );
			cells.add( c );
			if (Dungeon.level.heroFOV[c]) CellEmitter.get( c ).burst( WindParticle.FACTORY, 4 );
		}
		while (cells.size() > length()) cells.remove( 0 );
		fx( true );
	}

	private void trip(){
		boolean changed = false;
		for (Integer c : cells.toArray( new Integer[0] )){
			Char ch = Actor.findChar( c );
			if (ch == null || ch == target || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			knockDown( ch );
			cells.remove( c );
			changed = true;
		}
		if (changed) fx( true );
	}

	private static void knockDown( Char ch ){
		if (ch.properties().contains( Char.Property.BOSS )) Buff.prolong( ch, Cripple.class, 2f );
		else Buff.prolong( ch, Paralysis.class, 1f );
		if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.DUST ), 8 );
		if (Dungeon.level.heroFOV[ch.pos]){
			CellEmitter.get( ch.pos ).burst( WindParticle.FACTORY, 6 );
			SpatialSound.play( Assets.Sounds.MISS, ch, 1f, 0.6f );
		}
	}

	/** the oldest swirl the hero can stand on, or -1 */
	public int snapCell( Char hero ){
		for (int c : cells){
			if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar( c ) == null
					&& (!Char.hasProp( hero, Char.Property.LARGE ) || Dungeon.level.openSpace[c])) return c;
		}
		return -1;
	}

	/** the whole trail rushes back at once: every enemy on or beside it is tripped, then the wind is spent */
	public void gust( Char hero ){
		HashSet<Char> caught = new HashSet<>();
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				CellEmitter.get( c ).burst( WindParticle.FACTORY, 8 );
				CellEmitter.get( c ).burst( Speck.factory( Speck.DUST ), 2 );
			}
			for (int n : PathFinder.NEIGHBOURS9){
				if (!SkillInteractions.valid( c + n )) continue;
				Char ch = Actor.findChar( c + n );
				if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()) caught.add( ch );
			}
		}
		for (Char ch : caught) knockDown( ch );
		detach();
	}

	@Override
	public void fx( boolean on ) {
		for (WindParticle.Wind w : swirls) w.killAndErase();
		swirls.clear();
		if (on && target != null && target.sprite != null && target.sprite.parent != null && sameFloor()){
			for (int c : cells){
				WindParticle.Wind w = new WindParticle.Wind( c );
				//much denser than the ambient wind the class was made for, so the trail reads at a glance
				w.pour( WindParticle.FACTORY, 0.2f );
				target.sprite.parent.add( w );
				swirls.add( w );
			}
		}
	}

	private static final String RANK = "rank", REMAINING = "remaining", LAST = "last",
			DEPTH = "depth", BRANCH = "branch", CELLS = "cells";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( RANK, rank );
		bundle.put( REMAINING, remaining );
		bundle.put( LAST, last );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
		int[] saved = new int[cells.size()];
		for (int i = 0; i < saved.length; i++) saved[i] = cells.get( i );
		bundle.put( CELLS, saved );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		rank = bundle.getInt( RANK );
		remaining = bundle.getFloat( REMAINING );
		last = bundle.getInt( LAST );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
		cells.clear();
		for (int c : bundle.getIntArray( CELLS )) cells.add( c );
	}
}
