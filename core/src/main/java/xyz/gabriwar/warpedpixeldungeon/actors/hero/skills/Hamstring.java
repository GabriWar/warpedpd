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


import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Random;

public class Hamstring extends Skill {

	{
		tag = "D1";
		name = "Hamstring";
		castText = "Stay down";
		image = 12;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** The strike at the legs: its colour, how far over the sprite's bottom, px. */
	public static final int LEGS = 0xFFF4D8, LEGS_UP = 2;

	//a one-frame impact star at its legs. Any thread
	public static void legs( final CharSprite s ){
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float x = SkillVfx.midX( s ), y = s.y + s.height() - LEGS_UP;
			if (!Fx.seen( x, y )) return;
			FxParticle p = SkillVfx.glow( x, y, FxBudget.P0 );
			if (p != null) p.look( FxFrames.IMPACT_STAR[FxRandom.Int( 2 )] ).paint( LEGS ).life( 0.04f ).alpha( 1f, 0, 0.01f ).snap();
		} );
	}

	// the engine's cripple() hook only rolls on the ranged attack path, so this
	// proc goes through onHitProc to actually fire on melee swings
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		//+3: a hit on legs already cut breaks them, rooting the enemy in place
		if (!ranged && level >= MAX_LEVEL && enemy != null && enemy.isAlive()
				&& enemy.buff( Cripple.class ) != null
				&& !enemy.properties().contains( Char.Property.BOSS )){
			Buff.prolong( enemy, Roots.class, 2f );
			if (enemy.sprite != null && enemy.sprite.visible){
				//its legs give way: grit kicked up, a strike at its legs (never a skeleton's bones)
				CellEmitter.get( enemy.pos ).burst( SkillBursts.GRIT, 3 );
				legs( enemy.sprite );
				CellEmitter.bottom( enemy.pos ).burst( Speck.factory( Speck.DUST ), 3 );
				enemy.sprite.flash();
			}
			SpatialSound.play( Assets.Sounds.HIT_CRUSH, enemy, 1f, 0.7f );
		}
		if (!ranged && level > 0 && enemy != null && enemy.isAlive()
				&& Random.Int( 100 ) < 8 * level){
			Buff.prolong( enemy, Cripple.class, 3 + level );
			castTextYell();
			if (enemy.sprite != null && enemy.sprite.visible){
				enemy.sprite.emitter().burst( BloodParticle.FACTORY, 3 );
				CellEmitter.bottom( enemy.pos ).burst( Speck.factory( Speck.DUST ), 2 );
			}
			SpatialSound.play( Assets.Sounds.HIT_STAB, enemy, 1f, 0.8f );
		}
		return damage;
	}
}
