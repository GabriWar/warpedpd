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

import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;


public class ArcaneEcho extends SubSkill3 {

	{
		name = "Arcane Echo";
		image = 178;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy.isAlive() && Random.Int(100) < 10 * level){
			int echo = Random.NormalIntRange( 2, 4 + 2 * level );
			//at max level the echo jumps on to one more enemy standing next to the first
			Char chained = level >= MAX_LEVEL ? nextTo( enemy ) : null;

			enemy.damage( echo, this );
			//the echo arcs back along the blow
			if (enemy.sprite != null && Dungeon.hero.sprite != null){
				Dungeon.hero.sprite.parent.add( new Lightning( Dungeon.hero.pos, enemy.pos, null ) );
				enemy.sprite.emitter().burst( SparkParticle.FACTORY, 3 + level );
				enemy.sprite.flash();
			}
			if (chained != null){
				if (chained.sprite != null && Dungeon.hero.sprite != null){
					Dungeon.hero.sprite.parent.add( new Lightning( enemy.pos, chained.pos, null ) );
					chained.sprite.emitter().burst( SparkParticle.FACTORY, 3 + level );
					chained.sprite.flash();
				}
				chained.damage( echo, this );
			}
			SpatialSound.play( Assets.Sounds.ZAP, enemy, 0.6f, 1.3f );
		}
		return damage;
	}

	private Char nextTo( Char enemy ){
		for (int offset : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( enemy.pos + offset );
			if (ch != null && ch != Dungeon.hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive())
				return ch;
		}
		return null;
	}
}
