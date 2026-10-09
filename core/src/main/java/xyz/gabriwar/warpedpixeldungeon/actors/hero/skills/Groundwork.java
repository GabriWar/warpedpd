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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

public class Groundwork extends Skill {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "D1";
		name = "Groundwork";
		image = 130;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private static boolean heldDown( Char ch ){
		return (ch instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob && ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob) ch).wasAsleepBeforeBlow())
				|| ch.buff( Roots.class ) != null
				|| ch.buff( Cripple.class ) != null
				|| ch.buff( Paralysis.class ) != null
				|| ch.buff( MagicalSleep.class ) != null
				|| ch.buff( Frost.class ) != null;
	}

	//a hit on an enemy that cannot step away makes the ground burst: every enemy
	//next to it takes 20/35/50% of the hit. Fully trained, the burst cripples them too
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null || damage <= 0 || !heldDown( enemy )) return damage;

		int splash = Math.max( 1, Math.round( damage * (0.05f + 0.15f * level) ) );
		if (Dungeon.level.heroFOV[enemy.pos]){
			CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			Camera.main.shake( 1, 0.15f );
		}
		SpatialSound.play( Assets.Sounds.ROCKS, enemy, 0.7f, 1.2f );

		//the ground breaks outward: the thorns come up under the neighbours one after another
		FxTimeline t = FxTimeline.start();
		int order = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = enemy.pos + n;
			Char ch = Actor.findChar( c );
			if (ch == null || ch == Dungeon.hero || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
			if (Dungeon.level.heroFOV[c]){
				final int at = c, k = order++;
				t.at( 0.05f * k, () -> {
					CellEmitter.get( at ).burst( Speck.factory( Speck.ROCK ), 3 );
					SkillSpectacleFX.show( SkillSpectacleFX.THORN, at );
					SpatialSound.play( Assets.Sounds.ROCKS_LIGHT, at, 0.5f, 1.2f + 0.1f * k );
				} );
			}
			ch.damage( splash, this );
			if (level >= MAX_LEVEL && ch.isAlive()){
				Buff.prolong( ch, Cripple.class, 2f );
			}
		}
		return damage;
	}
}
