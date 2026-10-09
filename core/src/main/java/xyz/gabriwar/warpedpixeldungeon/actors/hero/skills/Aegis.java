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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AegisRecharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.noosa.Camera;

/**
 * A shield of light that catches a heavy melee blow, swallows half of it and bashes
 * the attacker away, then needs a few turns to reform. At mastery the half it
 * swallowed is slammed back into the attacker.
 */
public class Aegis extends SubSkill1 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	public static final float REFORM = 8f;

	{
		name = "Aegis";
		image = 164;
		tier = 1;
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
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| damage < Math.max( 1, hero.HT / 10 )
				|| !Dungeon.level.adjacent( hero.pos, enemy.pos )
				|| hero.buff( AegisRecharge.class ) != null){
			return damage;
		}

		int caught = damage / 2;
		damage -= caught;
		Buff.affect( hero, AegisRecharge.class, REFORM );

		if (hero.sprite != null && hero.sprite.parent != null){
			ShieldHalo halo = new ShieldHalo( hero.sprite );
			hero.sprite.parent.add( halo );
			halo.putOut();
			new Flare( 6, 24 ).color( 0xFFEE88, true ).show( hero.sprite, 0.5f );
			PulseRingFX.around( hero.sprite, 0xFFEE88, 12, 0.35f );
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "caught" ) );
		}
		SpatialSound.play( Assets.Sounds.HIT_PARRY, hero, 1f, 0.8f );
		SpatialSound.play( Assets.Sounds.HIT_STRONG, hero, 0.8f, 1.1f );
		Camera.main.shake( 1, 0.2f );

		//at mastery the swallowed half is slammed back into the attacker
		if (level >= MAX_LEVEL && caught > 0){
			enemy.damage( caught, this );
			if (enemy.sprite != null){
				enemy.sprite.flash();
				enemy.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			}
		}

		//the bash itself: 1/2/3 tiles straight away from you
		if (enemy.isAlive()){
			Ballistica trajectory = new Ballistica( enemy.pos, enemy.pos + (enemy.pos - hero.pos), Ballistica.MAGIC_BOLT );
			WandOfBlastWave.throwChar( enemy, trajectory, level, true, false, this );
		}
		return damage;
	}
}
