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


import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.UndyingWillWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

/**
 * Berserker: a crimson ward. When blows break it (not when it fades) it shatters, cutting
 * every enemy close by and turning the enemies around on the hero. Fully trained, a killing
 * blow that breaks the ward leaves him on 1 HP instead.
 */
public class UndyingWill extends SubSkill3 {

	{
		name = "Undying Will";
		castText = "I will not fall";
		image = 166;
		mana = 12;
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
			Buff.affect( hero, UndyingWillWard.class ).raise( SkillInteractions.ofHealth( hero.HT, 0.05f + 0.05f * level ) );
			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 0.7f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 5 );
			new Flare( 6, 22 ).color( 0xFF5544, true ).show( hero.sprite, 0.8f );
			Camera.main.shake( 1, 0.2f );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public boolean savesFromDeath(){ return true; }

	//fully trained, the blow that breaks the ward cannot kill: the hero is left on 1 HP
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null) return 0;
		UndyingWillWard ward = hero.buff( UndyingWillWard.class );
		int endurance = hero.HP + hero.shielding();
		if (ward == null || ward.shielding() <= 0 || damage < endurance) return 0;

		if (hero.sprite != null){
			hero.sprite.flash();
			new Flare( 8, 26 ).color( 0xFF2222, true ).show( hero.sprite, 1f );
			hero.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( this, "hold" ) );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 0.5f );
		Camera.main.shake( 2, 0.3f );
		return damage - (endurance - 1);
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
