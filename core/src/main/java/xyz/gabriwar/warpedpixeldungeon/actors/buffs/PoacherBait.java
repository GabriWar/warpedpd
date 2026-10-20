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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Poacher;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;

/** Poacher's carcass, held by the hero: a tile on one floor whose scent lures enemies for a few turns. */
public class PoacherBait extends Buff {

	public static final int DURATION = 5;
	public static final float SNARE_TURNS = 3f;

	private int cell = -1, rank, left;
	private int depth, branch;

	{
		type = buffType.NEUTRAL;
	}

	public void set( int cell, int rank ){
		this.cell = cell;
		this.rank = rank;
		left = DURATION;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || depth != Dungeon.depth || branch != Dungeon.branch
				|| !SkillInteractions.valid( cell )){
			detach();
			return true;
		}

		Char prey = Actor.findChar( cell );
		if (rank >= 3 && prey != null && prey != target && prey.alignment == Char.Alignment.ENEMY
				&& prey.isAlive() && !prey.flying){
			snare( prey );
			detach();
			return true;
		}

		SkillInteractions.lure( cell, rank );
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( SkillBursts.SCENT, 2 );
		}

		if (--left <= 0){
			detach();
		} else {
			spend( TICK );
		}
		return true;
	}

	private void snare( Char prey ){
		Buff.prolong( prey, Roots.class, SNARE_TURNS );
		SkillFX.flash( prey );
		//the snare in the carcass jerks tight: leaves and grit kicked up, a noose of nature light
		//closing on the prey's feet, the rope's creak (never a hidden trap going off, nor rockfall)
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( LeafParticle.GENERAL, 10 );
			CellEmitter.get( cell ).burst( SkillBursts.GRIT, 4 );
			if (prey.sprite != null){
				xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.chipOn( Poacher.class, prey );
				noose( prey.sprite );
			}
		}
		SpatialSound.play( Assets.Sounds.ROPE, cell, 1f, 1.2f );
	}

	/** The snare's noose: a ring of nature light closing tight on the prey's feet. Any thread. */
	public static void noose( CharSprite prey ){
		ClosingRing.on( prey, 23, 5, 0.25f, xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette.NATURE_LIGHT );
	}

	private static final String CELL = "cell";
	private static final String RANK = "rank";
	private static final String LEFT = "left";
	private static final String DEPTH = "depth";
	private static final String BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CELL, cell );
		bundle.put( RANK, rank );
		bundle.put( LEFT, left );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cell = bundle.getInt( CELL );
		rank = bundle.getInt( RANK );
		left = bundle.getInt( LEFT );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
	}
}
