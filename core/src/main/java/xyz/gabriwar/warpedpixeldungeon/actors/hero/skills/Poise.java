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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

public class Poise extends PassiveSkillA2 {

	//haste triples movement speed: two turns of it is a quick step or three, never a sprint
	private static final float HASTE_TURNS = 2f;

	{
		name = "Poise";
		image = 84;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//each kill steadies the breath; heal() draws the green motes and the number
	@Override
	public void onKill( Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || hero.sprite == null) return;
		hero.heal( SkillInteractions.ofHealth( hero.HT, 0.01f * level ) );
		if (level == MAX_LEVEL){
			Buff.prolong( hero, Haste.class, HASTE_TURNS );
			CellEmitter.bottom( hero.pos ).burst( Speck.factory( Speck.DUST ), 5 );
			SpatialSound.play( Assets.Sounds.MISS, hero, 1f, 1.5f );
		}
	}
}
