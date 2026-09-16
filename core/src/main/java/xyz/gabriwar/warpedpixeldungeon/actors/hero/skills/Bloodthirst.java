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


import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class Bloodthirst extends Skill {

	{
		tag = "CB";
		name = "Bloodthirst";
		image = 5;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//the blood drunk but not yet a whole point of health
	private float pool = 0f;

	//4% / 7% / 10% of the melee damage dealt comes back as health, pooled so small hits add up
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (!ranged && level > 0 && damage > 0 && hero != null){
			pool += damage * (0.01f + 0.03f * level);
			int whole = (int) pool;
			pool -= whole;
			int heal = Math.min( whole, hero.HT - hero.HP );
			if (heal > 0){
				hero.HP += heal;
				if (hero.sprite != null){
					hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), Math.min( heal, 6 ) );
				}
				if (enemy != null && enemy.sprite != null && enemy.sprite.visible){
					enemy.sprite.emitter().burst( BloodParticle.FACTORY, 2 + Math.min( heal, 6 ) );
				}
			}
		}
		return damage;
	}

	//fully trained, a melee kill bursts open and the hero drinks deep
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level < MAX_LEVEL || hero == null) return;
		CellEmitter.center( mob.pos ).burst( BloodParticle.BURST, 12 );
		int heal = Math.min( Math.max( 1, Math.round( hero.HT * 0.05f ) ), hero.HT - hero.HP );
		if (heal <= 0) return;
		hero.HP += heal;
		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.POSITIVE, "+" + heal );
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 4 );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK, 0.8f, 0.7f );
	}
}
