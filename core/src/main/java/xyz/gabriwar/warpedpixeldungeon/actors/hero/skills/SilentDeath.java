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
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;

public class SilentDeath extends PassiveSkillB3 {

	{
		name = "Silent Death";
		castText = "Eternal Slumber";
		tier = 3;
		image = 59;
	}

	private static final float HUSH_TURNS = 3f;
	private static final int HUSH_RANGE = 4;

	//sleepers hushed by a silent kill: mob id -> game time the hush ends (refreshes, never stacks)
	private final java.util.HashMap<Integer, Float> hushed = new java.util.HashMap<>();

	@Override
	protected boolean upgrade(){
		return true;
	}

	//15% / 25% / 35% of its max health: a blow that leaves a sleeper at or below that kills it outright
	private float threshold(){
		return 0.05f + 0.10f * level;
	}

	//no roll: Hero.attackProc asks only for melee blows on a mob that was asleep before this blow,
	//bosses and minibosses already excluded, with the blow's final damage
	@Override
	public boolean instantKill( Char enemy, int damage ){
		if (level <= 0 || enemy == null) return false;
		if (enemy.HP + enemy.shielding() - damage > threshold() * enemy.HT) return false;
		castTextYell();
		//the kill is quiet; the shadows are not
		Wound.hit( enemy );
		CellEmitter.get( enemy.pos ).burst( ShadowParticle.UP, 8 );
		if (Dungeon.hero != null && Dungeon.hero.sprite != null){
			Dungeon.hero.sprite.emitter().burst( ShadowParticle.UP, 6 );
		}
		SpatialSound.play( Assets.Sounds.HIT_STAB, enemy, 1f, 0.6f );
		SpatialSound.play( Assets.Sounds.GHOST, enemy, 0.5f, 0.7f );
		return true;
	}

	//+3: any sleeper killed by a melee blow hushes the room; nearby sleepers can't wake for 3 turns unless hurt
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (level < MAX_LEVEL || ranged || mob == null || !mob.wasAsleepBeforeBlow()) return;
		float until = xyz.gabriwar.warpedpixeldungeon.actors.Actor.now() + HUSH_TURNS;
		boolean any = false;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m == mob || !m.isAlive() || m.alignment != Char.Alignment.ENEMY || m.state != m.SLEEPING
					|| Char.hasProp( m, Char.Property.BOSS ) || Char.hasProp( m, Char.Property.MINIBOSS )
					|| Dungeon.level.distance( mob.pos, m.pos ) > HUSH_RANGE) continue;
			hushed.put( m.id(), until );
			any = true;
			if (m.sprite != null && m.sprite.visible){
				CellEmitter.get( m.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.SMOKE ), 3 );
			}
		}
		if (any && Dungeon.hero != null && Dungeon.hero.sprite != null){
			Dungeon.hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEUTRAL, Messages.get( this, "hush" ) );
			Sample.INSTANCE.play( Assets.Sounds.MELD, 0.6f, 0.8f );
		}
	}

	@Override
	public boolean preventsWaking( Mob mob ){
		Float until = hushed.get( mob.id() );
		if (until == null) return false;
		if (xyz.gabriwar.warpedpixeldungeon.actors.Actor.now() > until || mob.state != mob.SLEEPING){
			hushed.remove( mob.id() );
			return false;
		}
		return true;
	}
}
