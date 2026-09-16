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
import com.watabou.noosa.Camera;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class RecklessFury extends ActiveSkill {

	{
		tag = "CD";
		name = "Reckless Fury";
		castText = "No guard!";
		image = 15;
		tier = 4;
		mana = 4;
	}

	private static final String FURY = "RECKLESS_FURY";

	//damage taken while the guard is down, waiting to be spent on the next paid hit
	private int fury = 0;
	//the fury that hit carried and did not need, handed back if the hit kills (+3)
	private Char spentOn = null;
	private float spentAt = -1f;
	private int spare = 0;

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			// only one stance or attack toggle at a time
			for (Skill s : hero.heroSkills.activeSkills){
				if (s != this) s.active = false;
			}
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 1.2f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 4 );
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

	private boolean payable(){
		Hero hero = Dungeon.hero;
		return active && level > 0 && hero != null && hero.MP >= getManaCost();
	}

	//the dropped guard only costs health while there is mana to pay the fury with
	@Override
	public float incomingDamageModifier(){
		return payable() ? 1.10f : 1f;
	}

	//every blow that lands builds fury: 50% / 75% / 100% of what it took
	@Override
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){
		Hero hero = Dungeon.hero;
		if (!active || level <= 0 || hero == null) return;
		int gained = Math.round( (hpLost + shieldLost) * (0.25f + 0.25f * level) );
		if (gained <= 0) return;
		fury += gained;
		if (hero.sprite != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), Math.min( 6, 1 + gained / 3 ) );
			hero.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( this, "fury", fury ) );
		}
	}

	//on-hit rather than damageModifier(): Hero.damageRoll() also rolls for mirror images and the
	//like, and those swings must not spend the hero's mana or shout in his name
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (ranged || !payable() || enemy == null || fury <= 0) return damage;
		castTextYell();
		hero.MP -= getManaCost();
		int bonus = fury;
		fury = 0;
		spentOn = enemy;
		spentAt = Actor.now();
		spare = Math.min( bonus, Math.max( 0, damage + bonus - (enemy.HP + enemy.shielding()) ) );
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.emitter().burst( BloodParticle.BURST, 4 + Math.min( 8, bonus / 4 ) );
		}
		if (hero.sprite != null) new xyz.gabriwar.warpedpixeldungeon.effects.Flare( 6, 20 ).color( 0xFF3322, true ).show( hero.sprite, 0.5f );
		Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 0.8f );
		Camera.main.shake( 1, 0.2f );
		return damage + bonus;
	}

	//+3: a furious hit that kills keeps the fury it did not need for the next one
	@Override
	public void onKill( Mob mob, boolean ranged ){
		if (level < MAX_LEVEL || ranged || mob == null || mob != spentOn || Actor.now() != spentAt || spare <= 0) return;
		fury += spare;
		spare = 0;
		spentOn = null;
		Hero hero = Dungeon.hero;
		if (hero != null && hero.sprite != null){
			hero.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( this, "fury", fury ) );
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( FURY, fury );
	}

	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		fury = bundle.getInt( FURY );
	}
}
