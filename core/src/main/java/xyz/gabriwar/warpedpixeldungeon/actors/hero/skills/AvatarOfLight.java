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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AvatarOfLightHalo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

import java.util.ArrayList;

public class AvatarOfLight extends Skill {

	{
		name = "Avatar of Light";
		castText = "Be my light!";
		tag = "A4";
		image = 148;
		tier = 4;
		mana = 15;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && canPayMana( hero, getManaCost() ))
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && canPayMana( hero, getManaCost() )){
			int duration = 4 + 3 * level;

			//blessed, and wrapped in a halo that sears and blinds each enemy the first time it comes inside
			Buff.prolong( hero, Bless.class, duration );
			Buff.affect( hero, AvatarOfLightHalo.class ).set( level, duration );
			if (hero.sprite != null){
				hero.sprite.emitter().start( Speck.factory( Speck.LIGHT ), 0.3f, 12 );
				hero.sprite.emitter().burst( ShaftParticle.FACTORY, 8 );
				new Flare( 12, 44 ).color( 0xFFEE88, true ).show( hero.sprite, 2f ).angularSpeed = 60;
				ShieldHalo halo = new ShieldHalo( hero.sprite );
				hero.sprite.parent.add( halo );
				halo.putOut();
				Camera.main.shake( 2, 0.5f );
			}

			payMana( hero, getManaCost() );
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.MASTERY, 1f, 1.0f );
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
