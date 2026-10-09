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


import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class RiposteStance extends ActiveSkill2 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Riposte";
		castText = "Riposte!";
		image = 98;
		mana = 3;
		tier = 2;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.deactivateOtherToggles( this );
			Lunge.stanceTaken( hero );
			//the blade comes up: a ring of steel and a glint along the edge
			if (hero.sprite != null){
				new Flare( 4, 12 ).color( 0xFFFFFF, true ).show( hero.sprite, 0.35f );
				hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			}
			SpatialSound.play( Assets.Sounds.HIT_PARRY, hero, 0.8f, 1.6f );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			SpatialSound.play( Assets.Sounds.DEGRADE, hero, 0.5f, 1.5f );
		}
		StanceAuraBuff.sync( hero, Guard.class, active && level > 0 );
	}

	/** the held guard: the blade catches the light every second or so while the stance is up */
	public static class Guard extends StanceAuraBuff {
		@Override
		protected Class<? extends Skill> stance(){ return RiposteStance.class; }
		@Override
		protected StanceAuraFX build(){
			return new StanceAuraFX( target, 0xDDEEFF, 3, null, 0 ){
				private float glint = Random.Float( 0.6f, 1.2f );
				@Override
				public void update(){
					super.update();
					if (!visible || target.sprite == null || parent == null) return;
					glint -= Game.elapsed;
					if (glint > 0) return;
					glint = Random.Float( 0.9f, 1.5f );
					//a glint at the point of the held blade, in front of the duelist
					PointF c = target.sprite.center();
					float side = target.sprite.flipHorizontal ? -6 : 6;
					new Flare( 4, 5 ).color( 0xFFFFFF, true ).show( (Group) parent, new PointF( c.x + side, c.y - 1 ), 0.25f );
				}
			};
		}
	}

	//every melee swing that misses the Duelist is answered at once, for mana
	@Override
	public void onHeroMissed( Char attacker, boolean melee ){
		Hero hero = Dungeon.hero;
		if (!active || level <= 0 || !melee || hero == null || attacker == null || !attacker.isAlive()
				|| attacker.alignment != Char.Alignment.ENEMY || hero.MP < getManaCost()
				|| !Dungeon.level.adjacent( hero.pos, attacker.pos )) return;
		hero.MP -= getManaCost();
		castTextYell();
		xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon wep = hero.belongings.weapon();
		int roll = wep != null ? wep.damageRoll( hero ) : xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce.damageRoll( hero );
		int dmg = Math.max( 1, Math.round( roll * (0.3f + 0.2f * level) ) - attacker.drRoll() );
		attacker.damage( dmg, this );
		xyz.gabriwar.warpedpixeldungeon.effects.Wound.hit( attacker );
		//the answer: a snap of the blade toward the attacker and a streak into it
		ArcSpinFX.slash( hero.sprite, 0xFFFFFF, attacker.pos % Dungeon.level.width() >= hero.pos % Dungeon.level.width() );
		StreakFX.show( hero.pos, attacker.pos, 0xFFFFFF, 0.4f, 0.25f );
		if (attacker.sprite != null) attacker.sprite.flash();
		if (hero.sprite != null) hero.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 );
		SpatialSound.play( Assets.Sounds.HIT_PARRY, hero, 1f, 1.2f );
		Camera.main.shake( 1, 0.15f );
		//+3: the answered enemy is left open
		if (level >= MAX_LEVEL && attacker.isAlive()){
			Buff.prolong( attacker, Vulnerable.class, 2f );
		}
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}
}
