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


import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.utils.Random;

public class Condemn extends Skill {

	{
		name = "Condemn";
		tag = "D1";
		image = 151;
		tier = 1;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null || !enemy.isAlive())
			return damage;

		//at level 3 the undead and the demonic are always branded, and the brand burns them
		boolean holy = level >= MAX_LEVEL
				&& (Char.hasProp( enemy, Char.Property.UNDEAD ) || Char.hasProp( enemy, Char.Property.DEMONIC ));
		if (!holy && Random.Int(100) >= 12 * level)
			return damage;

		Buff.prolong( enemy, Vulnerable.class, 3 + level );
		CellEmitter.get( enemy.pos ).burst( Speck.factory( Speck.LIGHT ), 3 );
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "branded" ) );
			new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 4, 10 ).color( 0xFFEE88, true ).show( enemy.sprite, 0.35f );
			if (holy) enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
		}
		Sample.INSTANCE.play( Assets.Sounds.CURSED, 0.6f, holy ? 1.1f : 1.4f );
		if (holy)
			enemy.damage( 6, this );

		return damage;
	}
}
