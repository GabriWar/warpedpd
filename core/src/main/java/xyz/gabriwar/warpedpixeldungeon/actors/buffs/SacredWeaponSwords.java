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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * Sacred Weapon: spectral copies of the cleric's weapon planted in the floor. Each one
 * cuts an enemy next to it every turn until it fades; at mastery a fading sword flies
 * at the nearest enemy in reach.
 */
public class SacredWeaponSwords extends Buff {

	/** turns a planted sword stands */
	public static final int DURATION = 3;
	/** share of a weapon roll each cut deals */
	public static final float CUT = 0.35f;
	/** share of a weapon roll the fading throw deals, and how far it flies */
	public static final float THROW = 0.5f;
	public static final int THROW_RANGE = 4;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int[] cells = new int[0];
	private int[] left = new int[0];
	private int rank = 1;
	private int depth = -1, branch = -1;

	private Group visual;

	private boolean sameFloor(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	/** drops a sword beside the struck enemy; false when the cap is reached or there is no room */
	public boolean plant( Char struck, int rank ){
		if (!sameFloor()){
			cells = new int[0];
			left = new int[0];
			depth = Dungeon.depth;
			branch = Dungeon.branch;
		}
		this.rank = rank;
		if (cells.length >= rank) return false;

		//the sword lands on the far side of the enemy when it can, flanking it
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = struck.pos + n;
			if (!SkillInteractions.valid( c ) || c == target.pos || !Dungeon.level.passable[c]
					|| Dungeon.level.pit[c] || indexOf( c ) >= 0) continue;
			if (best == -1 || Dungeon.level.trueDistance( target.pos, c ) > Dungeon.level.trueDistance( target.pos, best )) best = c;
		}
		if (best == -1) return false;

		int[] nc = java.util.Arrays.copyOf( cells, cells.length + 1 );
		int[] nl = java.util.Arrays.copyOf( left, left.length + 1 );
		nc[cells.length] = best;
		nl[left.length] = DURATION;
		cells = nc;
		left = nl;

		final int cell = best;
		KindOfWeapon look = target instanceof Hero ? ((Hero) target).belongings.weapon() : null;
		SkillFX.rain( cell, look, 1, () -> CellEmitter.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 ) );
		SpatialSound.play( Assets.Sounds.HIT_MAGIC, cell, 0.9f, 0.9f );
		fx( true );
		return true;
	}

	private int indexOf( int cell ){
		for (int i = 0; i < cells.length; i++) if (cells[i] == cell) return i;
		return -1;
	}

	private static int roll( Hero hero ){
		KindOfWeapon w = hero.belongings.weapon();
		return w == null ? RingOfForce.damageRoll( hero ) : w.damageRoll( hero );
	}

	private static boolean awakeEnemy( Char ch ){
		return ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
				&& ((Mob) ch).state != ((Mob) ch).SLEEPING;
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || !sameFloor() || cells.length == 0){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		int kept = 0;
		int[] nc = new int[cells.length];
		int[] nl = new int[cells.length];
		for (int i = 0; i < cells.length; i++){
			int c = cells[i];
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( c + n );
				if (!awakeEnemy( ch )) continue;
				SkillSpectacleFX.fly( SkillSpectacleFX.SABER, c, ch.pos, 0, .25f );
				ch.damage( Math.max( 1, Math.round( roll( hero ) * CUT ) ), this );
				SkillFX.flash( ch );
				SpatialSound.play( Assets.Sounds.HIT_SLASH, ch, 0.8f, 1.2f );
				break;
			}
			if (--left[i] > 0){
				nc[kept] = c;
				nl[kept++] = left[i];
			} else {
				if (rank >= Skill.MAX_LEVEL) hurl( hero, c );
				if (Dungeon.level.heroFOV[c]) CellEmitter.center( c ).burst( Speck.factory( Speck.LIGHT ), 4 );
			}
		}
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

	//at mastery the fading sword pulls itself out of the floor and flies at the nearest enemy
	private void hurl( Hero hero, int from ){
		Mob nearest = null;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!awakeEnemy( m ) || Dungeon.level.distance( from, m.pos ) > THROW_RANGE
					|| !SkillInteractions.clear( from, m.pos )) continue;
			if (nearest == null || Dungeon.level.trueDistance( from, m.pos ) < Dungeon.level.trueDistance( from, nearest.pos )) nearest = m;
		}
		if (nearest == null) return;
		final Mob struck = nearest;
		KindOfWeapon look = hero.belongings.weapon();
		if (look != null) SkillFX.streak( from, struck.pos, look, () -> SkillFX.flash( struck ) );
		else SkillSpectacleFX.fly( SkillSpectacleFX.SABER, from, struck.pos, 0, .3f );
		SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.2f );
		struck.damage( Math.max( 1, Math.round( roll( hero ) * THROW ) ), this );
	}

	@Override
	public void fx( boolean on ){
		if (visual != null){
			visual.killAndErase();
			visual = null;
		}
		if (!on || target.sprite == null || target.sprite.parent == null || cells.length == 0) return;
		visual = new Group();
		KindOfWeapon look = target instanceof Hero ? ((Hero) target).belongings.weapon() : null;
		for (int cell : cells){
			Planted sword = new Planted( cell );
			if (look != null) sword.view( look );
			else sword.view( ItemSpriteSheet.WORN_SHORTSWORD, null );
			sword.origin.set( sword.width / 2f, sword.height / 2f );
			sword.angle = 135;
			sword.hardlight( 0xFFF1B8 );
			visual.add( sword );
		}
		target.sprite.parent.add( visual );
	}

	private class Planted extends ItemSprite {
		private final int cell;
		private float time;

		Planted( int cell ){
			super();
			this.cell = cell;
		}

		@Override
		public void update(){
			super.update();
			time += Game.elapsed;
			visible = sameFloor() && SkillInteractions.valid( cell ) && Dungeon.level.heroFOV[cell];
			PointF c = DungeonTilemap.tileCenterToWorld( cell );
			x = c.x - width / 2f;
			y = c.y - height / 2f - 3 + (float) Math.sin( time * 3 + cell );
			alpha( 0.6f + 0.2f * (float) Math.sin( time * 4 ) );
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
