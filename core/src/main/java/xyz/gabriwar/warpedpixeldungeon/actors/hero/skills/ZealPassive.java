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


import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;

public class ZealPassive extends PassiveSkillB2 {

	//a melee kill kindles holy flame: the next melee hit within this many turns erupts
	private static final float KINDLED_TURNS = 5f;

	private float kindledUntil = -1f;

	{
		name = "Fervor";
		image = 109;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** 4 / 7 / 10 fire damage */
	private int eruption(){
		return 1 + 3 * level;
	}

	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (ranged)
			return;
		kindledUntil = Actor.now() + KINDLED_TURNS;
		Hero hero = Dungeon.hero;
		if (hero != null && hero.sprite != null)
			hero.sprite.emitter().burst( FlameParticle.FACTORY, 10 );
		Sample.INSTANCE.play( Assets.Sounds.BURNING, 0.6f, 1.4f );
	}

	//the kill that kindles the flame has already passed through here, so it never spends its own flame;
	//a hit that erupts and kills kindles the next one
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || ranged || enemy == null || kindledUntil < 0)
			return damage;
		boolean lit = Actor.now() <= kindledUntil;
		kindledUntil = -1f;
		if (!lit)
			return damage;

		int burst = eruption();
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst( FlameParticle.FACTORY, 12 );
			new Flare( 6, 24 ).color( 0xFFAA33, true ).show( enemy.sprite, 0.6f );
		}
		Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.8f, 1.2f );

		if (level >= MAX_LEVEL){
			for (int offset : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( enemy.pos + offset );
				if (ch != null && ch != enemy && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					if (ch.sprite != null) ch.sprite.emitter().burst( FlameParticle.FACTORY, 6 );
					ch.damage( burst, this );
				}
			}
		}
		return damage + burst;
	}
}
