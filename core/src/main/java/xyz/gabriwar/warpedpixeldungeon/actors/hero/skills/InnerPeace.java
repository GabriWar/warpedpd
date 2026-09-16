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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;

public class InnerPeace extends SubSkill2 {

	private static final int MANA_COLOR = 0x44AAFF;

	{
		name = "Inner Peace";
		image = 175;
		tier = 2;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	//a calm mind lets harm slide off: a cripple, weakness, blindness, vertigo or terror about to take
	//hold is breathed out 15% / 25% / 35% of the time. Poison, bleeding and other damage over time
	//are never breathed out
	@Override
	public boolean shrugsOffDebuff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || buff == null) return false;
		if (!(buff instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple || buff instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness
				|| buff instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness || buff instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo
				|| buff instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror)) return false;
		if (com.watabou.utils.Random.Int( 100 ) >= 5 + 10 * level) return false;
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.STEAM ), 5 );
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "breathe" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARMS, 0.5f, 1.4f );
		//+3: each breath out restores 2 mana
		if (level >= MAX_LEVEL){
			int maxMana = hero.MT + RingOfMagic.manaBonus( hero );
			int mana = Math.max( 0, Math.min( 2, maxMana - hero.MP ) );
			if (mana > 0){
				hero.MP += mana;
				if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
			}
		}
		return true;
	}
}
