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

import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;

public class Overwatch extends SubSkill1 {

	{
		name = "Overwatch";
		image = 193;
		tier = 1;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a ranged hit can drive the target back: 20/30/40% chance to push it 2/2/3 tiles, a slam stuns at +3.
	//a target that cannot move (pinned, rooted, immovable) takes the slam where it stands
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (!ranged || level <= 0 || enemy == null || hero == null || enemy.pos == hero.pos) return damage;
		if (Random.Float() >= 0.1f + 0.1f * level) return damage;

		boolean slam = level >= MAX_LEVEL;
		int power = slam ? 3 : 2;
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
			enemy.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
		}
		SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 0.8f, 1.2f );

		if (enemy.rooted || Char.hasProp( enemy, Char.Property.IMMOVABLE )){
			//nowhere to go: the full force lands at once
			enemy.damage( Random.NormalIntRange( power + 1, 2 * power + 2 ), this );
			if (enemy.sprite != null) enemy.sprite.flash();
			xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			if (slam && enemy.isAlive() && !Char.hasProp( enemy, Char.Property.BOSS )){
				SkillInteractions.affectAfterHit( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class, 1f );
			}
			return damage;
		}

		//trace a line to the target, then keep only the part that runs on past it
		Ballistica trajectory = new Ballistica( hero.pos, enemy.pos, Ballistica.STOP_TARGET );
		trajectory = new Ballistica( trajectory.collisionPos, trajectory.path.get( trajectory.path.size() - 1 ), Ballistica.PROJECTILE );
		WandOfBlastWave.throwChar( enemy, trajectory, power, false, slam, this );
		return damage;
	}
}
