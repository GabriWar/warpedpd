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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.utils.Random;

public class BloodTithe extends Skill {

	private static final float REFUND_TURNS = 3f;

	{
		name = "Blood Tithe";
		tag = "CAB";
		image = 149;
		tier = 4;
		level = 0;
	}

	//the blood paid for the last cast, and when
	private int bled = 0;
	private float bledAt = -10f;

	@Override
	protected boolean upgrade(){ return true; }

	private int healthPerMana(){
		return level >= 2 ? 2 : 3;
	}

	//when a cast costs more mana than you have, the rest is paid in blood, never below 1 HP
	@Override
	public boolean coversManaShortfall( Hero hero, Skill casting, int missing, boolean commit ){
		if (level <= 0 || hero == null || missing <= 0) return false;
		int price = missing * healthPerMana();
		if (hero.HP - price < 1) return false;
		if (!commit) return true;
		hero.HP -= price;
		bled = price;
		bledAt = xyz.gabriwar.warpedpixeldungeon.actors.Actor.now();
		if (hero.sprite != null){
			hero.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle.BURST, 6 );
			hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEGATIVE, Integer.toString( price ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 0.7f, 0.6f );
		return true;
	}

	//+3: a kill soon after heals back half the blood you paid
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || bled <= 0 || xyz.gabriwar.warpedpixeldungeon.actors.Actor.now() - bledAt > REFUND_TURNS) return;
		int back = Math.min( bled / 2, hero.HT - hero.HP );
		bled = 0;
		if (back <= 0) return;
		hero.HP += back;
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
			hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.POSITIVE, "+" + back );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK, 0.5f, 1.2f );
	}
}
