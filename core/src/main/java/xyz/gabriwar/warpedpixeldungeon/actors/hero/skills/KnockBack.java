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
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAura;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;

public class KnockBack extends ActiveSkill2 {

	{
		name = "KnockBack";
		castText = "KnockBack!";
		tier = 2;
		image = 17;
		mana = 5;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.deactivateOtherToggles( this );
			Sample.INSTANCE.play( Assets.Sounds.STURDY, 0.8f, 1.3f );
			//the arm sets: a shower of forge sparks, then a slow trickle of them while the stance holds
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.FORGE ), 6 );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			Sample.INSTANCE.play( Assets.Sounds.STURDY, 0.5f, 0.8f );
		}
		StanceAura.sync( hero );
	}

	@Override
	public float damageModifier(){
		StanceAura.sync( Dungeon.hero );
		if (!active || Dungeon.hero.MP < getManaCost())
			return 1f;
		else {
			return 1f + 0.1f * level;
		}
	}

	//the shove itself, paid only when it can happen: a free tile behind the target, or at +3 the crush.
	//pinned enemies are shoved all the same (SkillInteractions.push ignores roots)
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || !active || level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )
				|| enemy.properties().contains( Char.Property.IMMOVABLE )
				|| hero.MP < getManaCost()) return damage;
		int behind = enemy.pos + (enemy.pos - hero.pos);
		boolean room = SkillInteractions.valid( behind ) && !Dungeon.level.solid[behind]
				&& Dungeon.level.passable[behind] && !Dungeon.level.pit[behind] && Actor.findChar( behind ) == null;
		if (!room && level < MAX_LEVEL) return damage;

		castTextYell();
		hero.MP -= getManaCost();
		if (room){
			Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 0.7f );
			final int from = enemy.pos;
			SkillInteractions.push( enemy, hero.pos, 1, 0 );
			//the shove reads as two puffs a beat apart: one where it stood, one where it came down
			if (Dungeon.level.heroFOV[from]) CellEmitter.bottom( from ).burst( Speck.factory( Speck.DUST ), 4 );
			final Char shoved = enemy;
			StaggerFX.after( 0.08f, () -> {
				if (shoved.sprite != null && shoved.sprite.visible){
					CellEmitter.bottom( shoved.pos ).burst( Speck.factory( Speck.DUST ), 3 );
					shoved.sprite.flash();
				}
			} );
			Camera.main.shake( 1, 0.1f );
			return damage;
		}
		//+3: nowhere to fly, so the target is crushed against what stands behind it
		if (enemy.sprite != null && enemy.sprite.visible){
			CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			Camera.main.shake( 2, 0.2f );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 0.8f );
		return Math.round( damage * 1.25f );
	}

	//the legacy shove in Hero.attackProc stays unused: onHitProc does it, with the mana check first
	@Override
	public boolean knocksBack(){
		return false;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}
}
