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


import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class LockSmith extends PassiveSkillA3 {

	private static final int READ_RANGE = 3;

	{
		name = "Lock Smith";
		tier = 3;
		image = 51;
	}

	@Override
	public boolean disableTrap(){
		if (Random.Int(100) >= 33 * level) return false;
		castTextYell();
		//the mechanism is seen and heard giving up
		Hero hero = Dungeon.hero;
		if (hero != null && hero.sprite != null){
			CellEmitter.center( hero.pos ).burst( SparkParticle.FACTORY, 6 );
		}
		SpatialSound.play( Assets.Sounds.UNLOCK, hero, 1f, 1.1f );
		if (level >= Skill.MAX_LEVEL && hero != null) readMechanism( hero );
		return true;
	}

	//at mastery the jammed plate tells how the whole floor was rigged: hidden traps nearby show themselves
	private void readMechanism( Hero hero ){
		boolean found = false;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.distance( hero.pos, i ) > READ_RANGE) continue;
			xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap trap = Dungeon.level.traps.get( i );
			if (trap == null || trap.visible) continue;
			Dungeon.level.discover( i );
			xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping.discover( i );
			CellEmitter.get( i ).burst( SparkParticle.FACTORY, 5 );
			found = true;
		}
		if (found){
			if (hero.sprite != null) hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEUTRAL, Messages.get( this, "found" ) );
			SpatialSound.play( Assets.Sounds.SECRET, hero, 0.8f, 1.2f );
		}
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
