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


import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Healing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;

public class Symbiosis extends SubSkill2 {

	{
		name = "Symbiosis";
		image = 163;
		tier = 2;
	}

	public static final float MEND_CHANCE = 0.25f;
	public static final float SLOW_TURNS = 3f;

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//when an enemy's blow lands, the wild can answer: leaves burst from you and mend 30/45/60% of
	//the health you really lost over the next few turns
	@Override
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || hpLost <= 0 || !(source instanceof Char) || source == hero
				|| Random.Float() >= MEND_CHANCE) return;
		int heal = Math.max( 1, Math.round( hpLost * (0.15f + 0.15f * level) ) );
		Buff.affect( hero, Healing.class ).setHeal( heal, 0.34f, 0 );
		if (hero.sprite != null) hero.sprite.emitter().burst( LeafParticle.GENERAL, 6 );
		SpatialSound.play( Assets.Sounds.GRASS, hero, 0.8f, 1.2f );
	}

	//+3: while the leaves are mending you, enemies that strike you are caught by clinging vines
	@Override
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || enemy == null || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY || hero.buff( Healing.class ) == null) return damage;
		Buff.prolong( enemy, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow.class, SLOW_TURNS );
		if (enemy.sprite != null) enemy.sprite.emitter().burst( LeafParticle.GENERAL, 4 );
		return damage;
	}

	//saves from before the rework carry SYMBIOSIS_BLOOM_*; nothing reads them any more
}
