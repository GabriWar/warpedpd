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


import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class IronStance extends ActiveSkill {

	{
		tag = "CC";
		name = "Iron Stance";
		castText = "Hold!";
		image = 13;
		tier = 4;
		mana = 3;
	}

	private static final float DIZZY = 3f;

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			// only one stance or attack toggle at a time
			hero.heroSkills.deactivateOtherToggles( this );
			Sample.INSTANCE.play( Assets.Sounds.STURDY, 1f, 1.1f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//the guard only weighs on the arm while there is mana to hold it with
	@Override
	public float damageModifier(){
		Hero hero = Dungeon.hero;
		if (!active || level <= 0 || hero == null || hero.MP < getManaCost())
			return 1f;
		return 0.85f;
	}

	//a parried melee blow is a real miss: no damage, no on-hit effects, the hero keeps his tile
	@Override
	public boolean dodgeChance( Char attacker ){
		Hero hero = Dungeon.hero;
		return active && level > 0 && hero != null && attacker != null && attacker != hero
				&& attacker.alignment == Char.Alignment.ENEMY
				&& hero.MP >= getManaCost()
				&& Dungeon.level.adjacent( hero.pos, attacker.pos )
				&& Random.Int( 100 ) < 15 * level;
	}

	@Override
	public void onDodge( Char attacker ){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		castTextYell();
		hero.MP = Math.max( 0, hero.MP - getManaCost() );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.1f );
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			hero.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( this, "blocked" ) );
		}
		//+3: the attacker reels back from the steel it hit, dizzy
		if (level >= MAX_LEVEL && attacker != null && !attacker.properties().contains( Char.Property.BOSS )){
			SkillInteractions.affectAfterHit( attacker, Vertigo.class, DIZZY );
			if (attacker.sprite != null) attacker.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		}
	}
}
