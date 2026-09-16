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
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;

public class Reckoning extends Skill {

	public static final int RANGE = 5;
	private static final int PILLAR_DAMAGE = 6;
	private static final int DUE_PER_TURN = 3;

	{
		name = "Reckoning";
		castText = "Judgement!";
		tag = "D3";
		image = 155;
		tier = 3;
		mana = 14;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && canPayMana( hero, getManaCost() ))
			actions.add(AC_CAST);
		return actions;
	}

	private static boolean unholy( Char ch ){
		return Char.hasProp( ch, Char.Property.UNDEAD ) || Char.hasProp( ch, Char.Property.DEMONIC );
	}

	//judgement on every enemy in sight within 5 tiles: the unbranded are branded vulnerable, and a brand
	//already on an enemy is called due as a pillar of light
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && canPayMana( hero, getManaCost() )){
			if (!payMana( hero, getManaCost() )) return;
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob.alignment != Char.Alignment.ENEMY
						|| !Dungeon.level.heroFOV[mob.pos]
						|| Dungeon.level.distance( hero.pos, mob.pos ) > RANGE)
					continue;

				Vulnerable brand = mob.buff( Vulnerable.class );
				if (brand != null){
					int due = DUE_PER_TURN * Math.max( 1, Math.round( brand.cooldown() ) );
					if (unholy( mob )) due *= 2;
					brand.detach();
					SkillFX.pillar( mob.pos, 0xFFEE88 );
					mob.damage( due, this );
					if (mob.sprite != null) mob.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( this, "due" ) );
				} else {
					CellEmitter.get( mob.pos ).burst( Speck.factory( Speck.LIGHT ), 4 );
					Buff.prolong( mob, Vulnerable.class, 2 + 2 * level );
					if (mob.sprite != null) mob.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "judged" ) );
				}

				//+3: a pillar of light falls on every judged enemy as well
				if (level >= MAX_LEVEL && mob.isAlive()){
					int dmg = PILLAR_DAMAGE;
					if (unholy( mob )) dmg *= 2;
					SkillFX.pillar( mob.pos, 0xFFEE88 );
					mob.damage( dmg, this );
				}
			}

			if (hero.sprite != null){
				hero.sprite.emitter().start( Speck.factory( Speck.STAR ), 0.3f, 10 );
				new Flare( 10, 32 ).color( 0xFFEE88, true ).show( hero.sprite, 1.2f ).angularSpeed = 90;
				Camera.main.shake( 1, 0.3f );
			}

			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.RAY, 1f, 0.9f );
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

	@Override
	public String info(){
		return Messages.get(this, "desc", RANGE) + "\n"
				+ costUpgradeInfo();
	}
}
