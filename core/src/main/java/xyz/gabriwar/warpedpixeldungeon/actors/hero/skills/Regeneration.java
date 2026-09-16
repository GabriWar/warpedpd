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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;


public class Regeneration extends PassiveSkillA2 {

	{
		name = "Regeneration";
		image = 2;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		if (Dungeon.hero != null && Dungeon.hero.sprite != null){
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 );
		}
		return true;
	}

	//second wind: the kill that leaves no enemy in sight lets the hero catch his breath
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || !hero.isAlive()) return;
		for (Mob m : Dungeon.level.mobs){
			if (m != mob && m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.heroFOV[m.pos]) return;
		}

		int heal = (int)Math.ceil( (hero.HT - hero.HP) * (0.05f + 0.1f * level) );
		boolean cleansed = false;
		if (level >= MAX_LEVEL){
			cleansed = hero.buff( Bleeding.class ) != null || hero.buff( Poison.class ) != null || hero.buff( Cripple.class ) != null;
			Buff.detach( hero, Bleeding.class );
			Buff.detach( hero, Poison.class );
			Buff.detach( hero, Cripple.class );
		}
		if (heal <= 0 && !cleansed) return;

		hero.HP = Math.min( hero.HT, hero.HP + heal );
		if (hero.sprite != null){
			new Flare( 5, 16 ).color( 0x66FF66, true ).show( hero.sprite, 0.6f );
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 3 + level );
			if (heal > 0) hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK, 0.8f, 1.3f );
	}
}
