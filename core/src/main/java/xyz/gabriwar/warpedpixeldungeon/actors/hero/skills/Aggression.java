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
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;

public class Aggression extends PassiveSkillB2 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Aggression";
		image = 9;
		tier = 2;
	}

	//the target of the melee swing being resolved, when it was struck, and the blow it took
	private Char struck = null;
	private float struckAt = -1f;
	private int struckDamage = 0;

	@Override
	protected boolean upgrade(){
		return true;
	}

	private float cleaveShare(){
		return 0.2f + 0.2f * level;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null) return damage;
		struck = enemy;
		struckAt = Actor.now();
		struckDamage = damage;
		return damage;
	}

	//a swing whose target dies, however the kill came about in that swing, cleaves into another enemy
	//beside the hero. Fully trained, a cleave that kills is itself a kill: the swing chains
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || hero == null || mob == null || mob != struck || Actor.now() != struckAt) return;
		struck = null;
		int dmg = Math.round( struckDamage * cleaveShare() );
		if (dmg <= 0) return;
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( hero.pos + n );
			if (ch != null && ch != mob && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				if (level >= MAX_LEVEL){
					struck = ch;
					struckAt = Actor.now();
					struckDamage = dmg;
				}
				Wound.hit( ch );
				if (ch.sprite != null && ch.sprite.visible){
					ch.sprite.flash();
					ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
				}
				Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH, 1f, 0.9f );
				ch.damage( dmg, hero );
				if (struck == ch && ch.isAlive()) struck = null;
				return;
			}
		}
	}
}
