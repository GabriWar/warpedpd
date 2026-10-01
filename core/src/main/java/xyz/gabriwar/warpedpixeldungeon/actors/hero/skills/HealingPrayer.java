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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;


import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class HealingPrayer extends ActiveSkill2 {

	private static final float MAX_BARRIER = 0.20f;

	{
		name = "Healing Prayer";
		castText = "Mend";
		image = 114;
		mana = 8;
		tier = 2;
	}

	@Override
	public boolean toggleable(){ return false; }

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

            SkillSpectacleFX.show(SkillSpectacleFX.WINGS,hero.pos);
			int amount = SkillInteractions.ofHealth( hero.HT, 0.05f + 0.05f * level );
            int healed = Math.min( amount, hero.HT - hero.HP );
			hero.HP += healed;
			hero.sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 4 );
			hero.sprite.emitter().burst( ShaftParticle.FACTORY, 5 );
			//the prayer: a soft pulse out from the cleric, a warmer second beat, motes drifting up after it
			PulseRingFX.around( hero.sprite, 0xBBFFCC, 14, 0.55f );
			FxTimeline.start()
					.at( 0.18f, () -> { PulseRingFX.around( hero.sprite, 0xFFF1A1, 10, 0.45f ); hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 4 ); } )
					.at( 0.42f, () -> hero.sprite.emitter().burst( ShaftParticle.FACTORY, 3 ) );
			hero.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.POSITIVE, "+" + healed + "HP" );
			//at level 3 the healing past full health stays on as a barrier of light, up to MAX_BARRIER
			if (level >= MAX_LEVEL){
				Barrier current = hero.buff( Barrier.class );
				int added = Math.min( amount - healed, SkillInteractions.ofHealth( hero.HT, MAX_BARRIER ) - (current == null ? 0 : current.shielding()) );
				if (added > 0){
					Buff.affect( hero, Barrier.class ).incShield( added );
					ShieldHalo halo = new ShieldHalo( hero.sprite );
					hero.sprite.parent.add( halo );
					halo.putOut();
				}
			}
			payMana( hero, getManaCost() );
			AsceticVow.tithe( hero, healed );
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHARMS, 1f, 1.2f );
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
