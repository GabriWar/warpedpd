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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

public class CreepingDread extends Skill {

	{
		tag = "D1";
		name = "Creeping Dread";
		castText = "Boo.";
		image = 68;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && !ranged && enemy != null && Random.Int( 100 ) < 8 * level){
			final Char struck = enemy;
			final int heroId = Dungeon.hero.id();
			final float turns = 3 + level;
			SkillInteractions.defer( () -> {
				if (struck.isAlive()) Buff.affect( struck, Terror.class, turns ).object = heroId;
			} );
			castTextYell();
			//fear creeping in: ink crawling in and pooling under it, a violet ring closing on its
			//feet, the body shuddering (never a curse landing, an alarm, a wraith rising)
			CellEmitter.get( enemy.pos ).burst( SkillBursts.DREAD, 5 );
			if (enemy.sprite != null){
				enemy.sprite.emitter().burst( SkillBursts.TREMBLE, 1 );
				ShoutFX.dread( enemy.sprite, false, 0 );
			}
			//at mastery the fear spills onto every enemy standing beside it, one after another
			if (level >= MAX_LEVEL){
				FxTimeline t = FxTimeline.start();
				int order = 0;
				for (int n : PathFinder.NEIGHBOURS8){
					Char other = Actor.findChar( enemy.pos + n );
					if (other == null || other.alignment != Char.Alignment.ENEMY || !other.isAlive()) continue;
					Buff.affect( other, Terror.class, 3 + level ).object = Dungeon.hero.id();
					final Char next = other;
					final int from = enemy.pos;
					t.at( 0.1f + 0.08f * order++, () -> {
						if (next.sprite == null || !next.isAlive()) return;
						CellEmitter.get( next.pos ).burst( SkillBursts.DREAD, 3 );
						next.sprite.emitter().burst( SkillBursts.TREMBLE, 1 );
						ShoutFX.creep( from, next.pos, 0 );
						ShoutFX.dread( next.sprite, true, 0.08f );
					} );
				}
			}
			SpatialSound.play( Assets.Sounds.GHOST, enemy, 0.8f, 0.8f );
		}
		return damage;
	}
}
