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

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import com.watabou.noosa.particles.Emitter;
import java.util.ArrayList;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * A smoke bomb the rogue threw, counting down where it landed. Modelled on Tengu's,
 * with one difference that matters: it never hurts the rogue or an ally.
 */
public class TenguSmokeBomb extends Buff {

	/** Tengu's bomb with the rogue's own description: it is the hero who threw it */
	public static class RogueBombItem extends Tengu.BombAbility.BombItem {}

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int cell = -1;
	private int depth = -1, branch = 0;
	private int fuse = 2;
	private int level = 1;

	private final ArrayList<Emitter> smokeEmitters = new ArrayList<>();

	public void set( int cell, int level ){
		this.cell = cell;
		this.level = level;
		this.depth = Dungeon.depth;
		this.branch = Dungeon.branch;
		this.fuse = 3;
		//the bomb itself lies where it landed, hissing, until it goes
		Dungeon.level.drop( new RogueBombItem(), cell );
		fx( true );
		puff( 6 );
		spend( TICK );
	}

	/** smoke pours over the whole blast area while the count runs, like Tengu's */
	@Override
	public void fx( boolean on ) {
		if (on && cell != -1 && smokeEmitters.isEmpty()){
			PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
			for (int i = 0; i < PathFinder.distance.length; i++){
				if (PathFinder.distance[i] < Integer.MAX_VALUE){
					Emitter e = CellEmitter.get( i );
					e.pour( SmokeParticle.FACTORY, 0.25f );
					smokeEmitters.add( e );
				}
			}
		} else if (!on){
			for (Emitter e : smokeEmitters){
				e.on = false;
				e.burst( BlastParticle.FACTORY, 2 );
			}
			smokeEmitters.clear();
		}
	}

	private void clearItem(){
		Heap h = Dungeon.level.heaps.get( cell );
		if (h != null){
			for (Item i : h.items.toArray( new Item[0] )){
				if (i instanceof Tengu.BombAbility.BombItem) h.remove( i );
			}
		}
	}

	/** thrown on another floor: it went off, or fizzled, where it was left */
	private boolean offFloor(){
		return depth >= 0 && (depth != Dungeon.depth || branch != Dungeon.branch);
	}

	@Override
	public boolean act() {
		if (offFloor()){
			detach();
			return true;
		}
		if (cell < 0){
			detach();
			return true;
		}
		if (smokeEmitters.isEmpty()) fx( true );
		if (fuse > 0){
			PointF p = DungeonTilemap.raisedTileCenterToWorld( cell );
			if (Dungeon.level.heroFOV[cell]){
				FloatingText.show( p.x, p.y, cell, fuse + "...", CharSprite.WARNING );
			}
			puff( 3 );
			fuse--;
			spend( TICK );
			return true;
		}

		clearItem();
		Sample.INSTANCE.play( Assets.Sounds.BLAST );
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.center( cell ).burst( BlastParticle.FACTORY, 30 );
			Camera.main.shake( 3, 0.5f );
		}
		int lvl = target instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero
				? ((xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) target).lvl : Dungeon.scalingDepth();
		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		for (int c = 0; c < PathFinder.distance.length; c++){
			if (PathFinder.distance[c] == Integer.MAX_VALUE) continue;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get( c ).burst( SmokeParticle.FACTORY, 3 );
			Char ch = Actor.findChar( c );
			if (ch == null || ch.alignment != Char.Alignment.ENEMY) continue;
			int dmg = Random.NormalIntRange( 5 + lvl / 3, 10 + 2 * lvl / 3 ) + 2 * level;
			dmg -= ch.drRoll();
			if (dmg > 0) ch.damage( dmg, this );
		}
		detach();
		return true;
	}

	private void puff( int n ){
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get( cell ).burst( SmokeParticle.FACTORY, n );
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String CELL = "cell", FUSE = "fuse", LEVEL = "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CELL, cell );
		bundle.put( "depth", depth );
		bundle.put( "branch", branch );
		bundle.put( FUSE, fuse );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		cell = bundle.getInt( CELL );
		depth = bundle.contains( "depth" ) ? bundle.getInt( "depth" ) : -1;
		branch = bundle.getInt( "branch" );
		fuse = bundle.getInt( FUSE );
		level = bundle.getInt( LEVEL );
	}
}
