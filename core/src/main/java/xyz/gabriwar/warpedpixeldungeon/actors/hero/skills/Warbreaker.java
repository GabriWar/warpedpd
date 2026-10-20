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


import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Random;

public class Warbreaker extends Skill {

	{
		tag = "PB4";
		name = "Warbreaker";
		castText = "Guard broken!";
		image = 23;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** The broken guard: shards of steel light thrown out of it, their colour. */
	public static final int SHARDS = 4, GUARD = 0xB9DEEE;

	//its guard shatters: a steel flare at its middle and SHARDS shards thrown outward from it,
	//falling and glinting where they land (matter, P2). Any thread
	public static void shatter( final CharSprite s ){
		Fx.post( () -> {
			if (s.parent == null || !s.visible) return;
			float x = SkillVfx.midX( s ), y = SkillVfx.midY( s );
			if (!Fx.seen( x, y )) return;
			PixelFlare.show( x, y, GUARD, PixelFlare.S, 0.25f );
			float feet = s.y + s.height() - 1;
			float a0 = FxRandom.Float( 0.3f );
			for (int i = 0; i < SHARDS; i++){
				//outward, and up rather than down
				float a = (float) Math.PI * (1.1f + 0.8f * (i + a0) / SHARDS);
				float v = FxRandom.Float( 45f, 70f );
				FxParticle p = SkillVfx.matter( x, y, FxBudget.P2 );
				if (p != null) p.look( FxFrames.SHARD_GLASS[i % 2] ).paint( GUARD )
						.vel( (float) Math.cos( a ) * v, (float) Math.sin( a ) * v ).gravity( 200f ).spin( 0.06f )
						.ground( feet + FxRandom.Float( 0f, 3f ), FxParticle.BOUNCE, 0.25f ).life( 0.6f ).alpha( 1f, 0, 0.2f ).source( x, y );
			}
		} );
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy != null && enemy.isAlive()
				&& Random.Int( 100 ) < 6 * level){
			Buff.prolong( enemy, Vulnerable.class, 3 + level );
			castTextYell();
			Wound.hit( enemy );
			if (enemy.sprite != null && enemy.sprite.visible){
				enemy.sprite.emitter().burst( SkillBursts.METAL_SPARK, 5 );
				shatter( enemy.sprite );
			}
			SpatialSound.play( Assets.Sounds.HIT_CRUSH, enemy, 1f, 0.8f );
			Fx.shake( 1, 0.15f );

			//fully trained, the shattered guard leaves everyone standing beside it open too
			if (level >= MAX_LEVEL){
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar( enemy.pos + n );
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
						Buff.prolong( ch, Vulnerable.class, 3 + level );
						if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( SkillBursts.METAL_SPARK, 3 );
					}
				}
			}
			return Math.round( damage * 1.3f );
		}
		return damage;
	}
}
