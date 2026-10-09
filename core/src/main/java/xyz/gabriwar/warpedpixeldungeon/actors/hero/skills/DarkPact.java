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


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class DarkPact extends SubSkill3 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Dark Pact";
		castText = "My blood, my power";
		image = 186;
		mana = 0;
		tier = 3;
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			int price = Math.max( 1, hero.HT / 10 );
			if (hero.HP <= price){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( this, "no_life" ) );
				return;
			}
			//paid straight out of the body: no ward, shield or skill can take the price instead
			hero.HP -= price;
			if (hero.sprite != null) hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.NEGATIVE, Integer.toString( price ) );
			int maxMana = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
			hero.MP = Math.max( hero.MP, Math.min( maxMana, hero.MP + 10 + 5 * level ) );
			hero.MP -= getManaCost();
			//at mastery the spilled blood lashes every adjacent enemy
			if (level >= MAX_LEVEL){
				for (int n : PathFinder.NEIGHBOURS8){
					Char near = Actor.findChar( hero.pos + n );
					if (near != null && near.isAlive() && near.alignment == Char.Alignment.ENEMY){
						if (near.sprite != null)
							near.sprite.emitter().burst( ShadowParticle.CURSE, 6 );
						near.damage( Math.max( 1, price / 2 ), this );
					}
				}
			}
			castTextYell();
			SpatialSound.play( Assets.Sounds.CURSED, hero, 1f, 0.9f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.SMOKE ), 6 );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 4 );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 + level );
			Dungeon.hero.sprite.flash();
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
