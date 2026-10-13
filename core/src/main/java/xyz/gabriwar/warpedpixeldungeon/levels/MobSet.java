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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A level's mobs. The actor thread walks them every turn (Mob.chooseEnemy and the like) while the
 * render thread can add or remove one (a window's choice going through GameScene.add): a plain
 * HashSet threw ConcurrentModificationException then. Changes here take a lock, and a walk goes
 * over a snapshot of the set as it was, made again only after a change, so reading stays cheap.
 * A walk can still remove what it is on (iterator().remove()).
 */
public class MobSet extends HashSet<Mob> {

	//the members as of the last change; null until the next walk asks for it
	private transient Mob[] snapshot = null;

	@Override
	public synchronized boolean add( Mob mob ){
		boolean changed = super.add( mob );
		if (changed) snapshot = null;
		return changed;
	}

	@Override
	public synchronized boolean remove( Object o ){
		boolean changed = super.remove( o );
		if (changed) snapshot = null;
		return changed;
	}

	@Override
	public synchronized void clear(){
		super.clear();
		snapshot = null;
	}

	@Override
	public synchronized boolean contains( Object o ){
		return super.contains( o );
	}

	@Override
	public synchronized int size(){
		return super.size();
	}

	@Override
	public synchronized boolean isEmpty(){
		return super.isEmpty();
	}

	//Yog, Zot and the like walk a copy: made under the lock, as any change is
	@Override
	public synchronized Object clone(){
		return super.clone();
	}

	private synchronized Mob[] snapshot(){
		if (snapshot == null){
			Mob[] all = new Mob[super.size()];
			int i = 0;
			for (Iterator<Mob> it = super.iterator(); it.hasNext(); ) all[i++] = it.next();
			snapshot = all;
		}
		return snapshot;
	}

	@Override
	public Iterator<Mob> iterator(){
		final Mob[] all = snapshot();
		return new Iterator<Mob>(){
			private int next = 0;
			private Mob last = null;

			@Override
			public boolean hasNext(){
				return next < all.length;
			}

			@Override
			public Mob next(){
				if (next >= all.length) throw new NoSuchElementException();
				return last = all[next++];
			}

			@Override
			public void remove(){
				if (last == null) throw new IllegalStateException();
				MobSet.this.remove( last );
				last = null;
			}
		};
	}
}
