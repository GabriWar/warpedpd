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
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

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
		if (hero.sprite != null) hero.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.STAR ), 4 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.2f );
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
