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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ShieldOfTheFaithful;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.AegisArcFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;

/**
 * Shield of the Faithful: a shield of light hovering at the cleric's side. It catches part
 * of each blow and cracks; after enough blows it shatters into shards that cut and blind
 * the enemies next to the cleric, then reforms. At mastery a blow it catches from a
 * distance is thrown back at the shooter as a bolt of light.
 *
 * Its look is an aegis of light, never a shield item nor the SHIELDED state: an arc of gold at
 * the cleric's side turned toward the enemy it guards (AegisArcFX), dimmer with each crack.
 */
public class ShieldOfTheFaithfulWard extends Buff {

	/** share of each blow the shield catches */
	public static final float CATCH = 0.30f;
	/** turns the shattered shield takes to reform */
	public static final int REFORM = 15;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int cracks = 0;
	private int caught = 0;
	private int broken = 0;

	private AegisArcFX arc;
	//the cell of the enemy it guards (-1: none in sight: it hangs at the cleric's front)
	private volatile int facing = -1;

	/** The arc's resting alpha by cracks: 0.7, 0.55, then 0.4. */
	public static float rest( int cracks ){
		return Math.max( 0.4f, 0.7f - 0.15f * cracks );
	}

	/** blows the shield holds before it gives out: 2 / 3 / 4 */
	public static int holds( int rank ){
		return 1 + rank;
	}

	/** the shield hangs between its bearer and the nearest enemy in sight, and only that enemy's blows meet it */
	private Char guarded(){
		Char best = null;
		for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			if (best == null || Dungeon.level.trueDistance( target.pos, m.pos ) < Dungeon.level.trueDistance( target.pos, best.pos )) best = m;
		}
		return best;
	}

	/** catches part of a blow, returning how much it took */
	public int intercept( int damage, Object source, int rank ){
		if (broken > 0 || damage <= 0 || source == null || source != guarded()) return 0;
		int blocked = Math.min( damage, Math.max( 1, Math.round( damage * CATCH ) ) );
		cracks++;
		caught += blocked;
		if (source instanceof Char) facing = ((Char) source).pos;

		if (target.sprite != null && target.sprite.parent != null){
			if (arc != null) arc.caught();
			target.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
		}
		SpatialSound.play( Assets.Sounds.HIT_PARRY, target, 1f, 1.3f );

		//at mastery a shot from afar is thrown back along the line it came
		if (rank >= Skill.MAX_LEVEL && source instanceof Char){
			Char shooter = (Char) source;
			if (shooter.isAlive() && shooter.alignment == Char.Alignment.ENEMY
					&& !Dungeon.level.adjacent( target.pos, shooter.pos )){
				if (target.sprite != null && target.sprite.parent != null){
					//MagicMissile.boltFromChar's line, drawn as a bolt of holy light (its one draw kept)
					Char at = Actor.findChar( shooter.pos );
					if (at != null) SkillBolt.fly( target.sprite.parent, target.sprite.center(), at.sprite.destinationCenter(), SkillBolt.HOLY, null );
					else SkillBolt.fly( target.sprite.parent, target.sprite, shooter.pos, SkillBolt.HOLY, null );
				}
				SpatialSound.play( Assets.Sounds.RAY, target, 0.8f, 1.2f );
				shooter.damage( blocked, this );
			}
		}

		if (cracks >= holds( rank )) shatter();
		return blocked;
	}

	//the shield has caught all it can hold: it dims away and reforms later
	private void shatter(){
		cracks = 0;
		caught = 0;
		broken = REFORM;
		SpatialSound.play( Assets.Sounds.DEGRADE, target, 0.8f, 1.2f );
		CellEmitter.center( target.pos ).burst( Speck.factory( Speck.LIGHT ), 8 );
		xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.chipOn( ShieldOfTheFaithful.class, target );
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || CurrentSkills.skillLevel( ShieldOfTheFaithful.class ) <= 0){
			detach();
			return true;
		}
		Char g = guarded();
		facing = g == null ? -1 : g.pos;
		if (broken > 0 && --broken == 0){
			SpatialSound.play( Assets.Sounds.CHARMS, target, 0.8f, 1.2f );
			if (target.sprite != null){
				target.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
				xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.chipOn( ShieldOfTheFaithful.class, target );
			}
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (arc != null){
			Fx.dispose( arc );
			arc = null;
		}
		if (on && target.sprite != null && target.sprite.parent != null){
			arc = AegisArcFX.hold( target.sprite, new AegisArcFX.Ward(){
				@Override
				public int facing(){
					return facing;
				}
				@Override
				public float rest(){
					return broken > 0 ? 0f : ShieldOfTheFaithfulWard.rest( cracks );
				}
			}, AegisArcFX.GOLD );
		}
	}

	private static final String CRACKS = "cracks", CAUGHT = "caught", BROKEN = "broken";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CRACKS, cracks );
		bundle.put( CAUGHT, caught );
		bundle.put( BROKEN, broken );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cracks = bundle.getInt( CRACKS );
		caught = bundle.getInt( CAUGHT );
		broken = bundle.getInt( BROKEN );
	}
}
