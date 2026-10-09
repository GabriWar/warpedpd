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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

public class Hunting extends PassiveSkillA3 {

	{
		name = "Hunting";
		image = 74;
		tier = 3;
	}

	public static final float TRAIL_TURNS = 10f;

	//the last prey caught unawares; only a kill on it picks up the next trail
	private int ambushedId = -1;

	@Override
	protected boolean upgrade(){
		return true;
	}

	private static boolean unaware( Char enemy ){
		if (!(enemy instanceof Mob)) return false;
		Mob m = (Mob) enemy;
		return m.state == m.SLEEPING || m.state == m.WANDERING;
	}

	//prey that has not noticed you is struck senseless: stunned for 2/3/4 turns
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null || !unaware( enemy )) return damage;
		ambushedId = enemy.id();
		castText = Messages.get( this, "cast" );
		castTextYell();
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
			Surprise.hit( enemy );
			enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		}
		SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 0.8f, 1.2f );
		//the stun lands after the blow does, or the blow itself would shake it off
		final Char prey = enemy;
		final float turns = 1 + level;
		Actor.add( new Actor(){
			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act(){
				if (prey.isAlive() && !Char.hasProp( prey, Char.Property.BOSS ) && !Char.hasProp( prey, Char.Property.MINIBOSS ))
					Buff.prolong( prey, Paralysis.class, turns );
				Actor.remove( this );
				return true;
			}
		});
		return damage;
	}

	//fully trained, killing the ambushed prey picks up the trail of the next one:
	//the nearest enemy out of sight is shown through the walls for a while
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (level < MAX_LEVEL || mob == null || mob.id() != ambushedId || Dungeon.hero == null) return;
		ambushedId = -1;
		Hero hero = Dungeon.hero;
		Mob next = null;
		int best = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m == mob || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (Dungeon.level.heroFOV[m.pos]) continue;
			int d = Dungeon.level.distance( hero.pos, m.pos );
			if (d < best){
				best = d;
				next = m;
			}
		}
		if (next == null) return;
		Buff.append( hero, TalismanOfForesight.CharAwareness.class, TRAIL_TURNS ).charID = next.id();
		Dungeon.observe();
		GameScene.updateFog();
		castText = Messages.get( this, "cast_trail" );
		castTextYell();
		if (hero.sprite != null) hero.sprite.emitter().burst( LeafParticle.GENERAL, 8 );
		SpatialSound.play( Assets.Sounds.GRASS, hero, 1f, 0.9f );
	}

	@Override
	public String castText(){
		return castText;
	}
}
