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
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;

public class Predation extends Skill {

	{
		tag = "D2";
		name = "Predation";
		image = 119;
		tier = 2;
	}

	private static final float FEED = 0.03f;

	@Override
	protected boolean upgrade(){ return true; }

	//the predator tears into the wounded: a hit on an enemy at or under 40% health can rend it open
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && enemy != null && enemy.HP * 5 <= enemy.HT * 2
				&& Random.Int( 100 ) < 15 + 10 * level){
			Buff.affect( enemy, Bleeding.class ).set( Math.max( 2f, damage / 4f ) );
			CellEmitter.center( enemy.pos ).burst( BloodParticle.BURST, 8 );
			Wound.hit( enemy );
			if (enemy.sprite != null){
				enemy.sprite.flash();
				enemy.sprite.showStatus( CharSprite.WARNING, name() );
			}
			Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 0.8f, 0.7f );
		}
		return damage;
	}

	//at mastery a bleeding enemy you kill feeds you
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || mob.buff( Bleeding.class ) == null || hero.HP >= hero.HT) return;
		int heal = Math.min( SkillInteractions.ofHealth( hero.HT, FEED ), hero.HT - hero.HP );
		hero.HP += heal;
		CellEmitter.center( mob.pos ).burst( BloodParticle.BURST, 10 );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
			hero.sprite.emitter().burst( BloodParticle.FACTORY, 4 );
			new Flare( 4, 12 ).color( 0xCC1111, true ).show( hero.sprite, 0.4f );
			hero.sprite.showStatus( CharSprite.POSITIVE, Integer.toString( heal ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK, 0.7f, 0.8f );
	}
}
