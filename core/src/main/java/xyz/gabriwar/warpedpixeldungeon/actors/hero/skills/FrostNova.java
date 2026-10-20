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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class FrostNova extends Skill {

	private static final int RADIUS = 3;

	{
		tag = "D2";
		name = "Frost Nova";
		castText = "Be still.";
		tier = 2;
		image = 30;
		mana = 6;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){

			ArrayList<Integer> frozenGround = new ArrayList<>();
			for (int c : SkillInteractions.area( hero.pos, RADIUS )) if (!Dungeon.level.pit[c]) frozenGround.add( c );
			//at mastery the ring leaves the ground bristling with ice spikes
			if (level >= MAX_LEVEL && !frozenGround.isEmpty())
				SkillField.place( hero, SkillField.ICE, level, 4, frozenGround );

			ArrayList<Mob> caught = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment == Char.Alignment.ENEMY
						&& frozenGround.contains(mob.pos))
					caught.add( mob );
			}

			CellEmitter.center( hero.pos ).burst( SnowParticle.FACTORY, 12 );
			new Flare( 6, 32 ).color( 0x88DDFF, true ).show( hero.sprite, 0.8f );
			shards( hero.sprite );
			SpatialSound.play( Assets.Sounds.SHATTER, hero, 1f, 0.9f );
			Fx.shake( 1, 0.3f );

			//the cold lands on everyone at once; only the crystals take their time crawling out
			final java.util.HashSet<Integer> caughtCells = new java.util.HashSet<>();
			final int[] rimed = { 0 };
			for (Mob mob : caught){
				caughtCells.add( mob.pos );
				mob.damage( Random.NormalIntRange( 2 + level, 4 + 3 * level ), this );
				if (mob.isAlive())
					Buff.prolong( mob, Chill.class, 3 + 2 * level );
			}
			//frost races outward ring by ring, a glint on every cell, a crack of ice per ring rising in pitch
			StaggerFX.ring( hero.pos, RADIUS, 0.09f, ( c, r ) -> {
				CellEmitter.get( c ).burst( SnowParticle.FACTORY, caughtCells.contains( c ) ? 6 : 2 );
				SkillInteractions.flare( c, 0xA4E9FF );
				if (caughtCells.contains( c )){
					xyz.gabriwar.warpedpixeldungeon.actors.Char ch = xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar( c );
					if (ch != null && ch.sprite != null) ch.sprite.flash();
					//rime under the caught, the first RIMES of them
					if (rimed[0]++ < RIMES) rime( c );
				}
			} );
			for (int r = 1; r <= RADIUS; r++){
				final float pitch = 1.1f + 0.15f * r;
				StaggerFX.after( 0.09f * r, () -> SpatialSound.play( Assets.Sounds.SHATTER, hero, 0.5f, pitch ) );
			}

			hero.MP -= getManaCost();
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	/** The nova's ice thrown out of him: SHARDS shards of ice (P2) at SHARD_MIN..SHARD_MAX px/s
	 *  against a drag of SHARD_DRAG a second for SHARD_LIFE s, so they clear the flare's white heart
	 *  (about 10 px) and none goes past 20 px; FROST light, dying on the floor line. */
	public static final int SHARDS = 8;
	public static final float SHARD_MIN = 70f, SHARD_MAX = 95f, SHARD_DRAG = 4f, SHARD_LIFE = 0.4f, SHARD_FLOOR = 10f;
	/** Rime decals a cast lays at most (one a caught cell; FxDecal's cap 32 is shared), their alpha,
	 *  hold and fade, s. */
	public static final int RIMES = 9;
	public static final float RIME_ALPHA = 0.55f, RIME_HOLD = 1.2f, RIME_FADE = 0.8f;

	/** The shards out of a sprite's middle. Any thread; FxRandom only. */
	public static void shards( CharSprite s ){
		if (s == null) return;
		MoteFX.flick( SkillVfx.midX( s ), SkillVfx.midY( s ), SHARDS, FxFrames.SHARD_ICE, SkillPalette.FROST,
				SHARD_MIN, SHARD_MAX, SHARD_DRAG, 0f, SHARD_LIFE, SHARD_FLOOR, FxBudget.P2 );
	}

	/** A rime decal on a cell's floor, where the frost caught someone. Any thread; FxRandom only. */
	public static void rime( final int cell ){
		Fx.post( () -> {
			if (!SkillVfx.valid( cell ) || !SkillVfx.seen( cell )) return;
			FxDecal.at( SkillVfx.cx( cell ), SkillVfx.cy( cell ) + 4, FxFrames.RIME[FxRandom.Int( FxFrames.RIME.length )],
					SkillPalette.FROST.stop( 0 ), RIME_ALPHA, RIME_HOLD, RIME_FADE, FxRandom.chance( 0.5f ) );
		} );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
