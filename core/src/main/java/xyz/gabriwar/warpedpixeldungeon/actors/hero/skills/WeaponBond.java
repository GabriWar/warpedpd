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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashSet;

public class WeaponBond extends PassiveSkillB2 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Weapon Bond";
		image = 87;
		tier = 2;
	}

	private static final float GLOW_TURNS = 10f;

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a melee kill feeds the blade: it glows until your next weapon ability, which strikes 20% / 35% / 50% harder
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || ranged || hero == null || !(hero.belongings.weapon() instanceof MeleeWeapon)) return;
		Bonded bond = xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( hero, Bonded.class );
		bond.charge( level >= MAX_LEVEL ? 2 : 1, level >= MAX_LEVEL ? 0f : GLOW_TURNS );
		if (hero.sprite != null){
			new Flare( 6, 20 ).color( 0xCCEEFF, true ).show( hero.sprite, 0.5f );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.6f, 1.5f );
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || ranged || hero == null || hero.belongings.abilityWeapon == null || enemy == null) return damage;
		Bonded bond = hero.buff( Bonded.class );
		if (bond == null || bond.stacks <= 0) return damage;
		bond.spend1();
		CellEmitter.center( enemy.pos ).burst( Speck.factory( Speck.LIGHT ), 6 );
		Wound.hit( enemy );
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 1.2f );
		return Math.round( damage * (1.05f + 0.15f * level) );
	}

	/** the blade still humming from a kill, waiting for the next weapon ability */
	public static class Bonded extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff {

		{
			type = buffType.POSITIVE;
		}

		int stacks = 0;
		//0 = holds until spent
		float left = 0f;

		void charge( int cap, float turns ){
			stacks = Math.min( cap, stacks + 1 );
			left = turns;
		}

		void spend1(){
			if (--stacks <= 0) detach();
		}

		@Override
		public boolean act(){
			if (left > 0){
				left -= TICK;
				if (left <= 0){
					detach();
					return true;
				}
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.WEAPON; }

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight( 0.8f, 0.95f, 1f ); }

		@Override
		public String iconTextDisplay(){ return Integer.toString( stacks ); }

		@Override
		public String desc(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "desc", stacks );
		}

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( "stacks", stacks );
			bundle.put( "left", left );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
			super.restoreFromBundle( bundle );
			stacks = bundle.getInt( "stacks" );
			left = bundle.getFloat( "left" );
		}
	}
}
