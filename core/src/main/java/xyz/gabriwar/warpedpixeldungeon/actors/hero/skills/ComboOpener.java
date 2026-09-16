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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

//class name and tag kept for saves: the skill is now the Gladiator's Opening Blow
public class ComboOpener extends SubSkill1 {

	{
		name = "Opening Blow";
		image = 167;
		tier = 1;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		return new ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//the first blow of a fight lands as a shockwave: the enemy, still at full health, is Dazed
	//2 / 3 / 4 turns and thrown 1 / 2 / 2 tiles; at +3 the wave throws every other adjacent enemy 1 tile
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || level <= 0 || hero == null || enemy == null || !enemy.isAlive() || enemy.HP < enemy.HT)
			return damage;
		Buff.prolong( enemy, Daze.class, 1 + level );
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "stagger" ) );
			new Flare( 8, 20 ).color( 0xFFB060, true ).show( enemy.sprite, 0.5f );
		}
		SkillFX.land( enemy.pos );
		Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.8f, 1.2f );

		if (Dungeon.level.adjacent( hero.pos, enemy.pos )){
			shove( hero, enemy, level >= 2 ? 2 : 1 );
			if (level >= MAX_LEVEL){
				for (int n : PathFinder.NEIGHBOURS8){
					Char other = Actor.findChar( hero.pos + n );
					if (other != null && other != enemy && other.isAlive() && other.alignment == Char.Alignment.ENEMY){
						shove( hero, other, 1 );
					}
				}
			}
		}
		return damage;
	}

	//straight away from the Gladiator, the way Knock Back throws
	private static void shove( Hero hero, Char ch, int power ){
		SkillInteractions.push( ch, hero.pos, power, 0 );
	}
}
