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


import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;

public class SecondWind extends SubSkill2 {

	{
		name = "Second Wind";
		castText = "Second wind!";
		image = 161;
		mana = 12;
		tier = 2;
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

	//catch your breath: your weapon abilities regain 1 / 2 / 3 charges at once; +3 overfills one more
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon.Charger charger =
					hero.buff( xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class );
			int cap = charger == null ? 0 : charger.chargeCap() + (level >= MAX_LEVEL ? 1 : 0);
			if (charger == null || charger.charges >= cap){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( Messages.get( this, "full_hp" ) );
				return;
			}
			int before = charger.charges;
			charger.charges = Math.min( cap, charger.charges + level );
			xyz.gabriwar.warpedpixeldungeon.items.Item.updateQuickslot();
			hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.POSITIVE, "+" + (charger.charges - before) );
			new Flare( 6, 24 ).color( 0x88FF88, true ).show( hero.sprite, 0.6f );
			hero.sprite.emitter().burst( xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory( xyz.gabriwar.warpedpixeldungeon.effects.Speck.UP ), 5 );
			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 1f, 0.8f );
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
