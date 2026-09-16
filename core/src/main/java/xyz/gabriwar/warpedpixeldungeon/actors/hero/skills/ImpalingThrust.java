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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;


import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class ImpalingThrust extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "A4";
		name = "Impaling Thrust";
		castText = "Thrust!";
		image = 143;
		tier = 4;
		mana = 9;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && hero.MP >= getManaCost())
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null && target != hero.pos){
						thrust( hero, target );
					}
				}

				@Override
				public String prompt(){
					return Messages.get(ImpalingThrust.class, "prompt");
				}
			} );
		}
	}

	private void thrust( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		Ballistica traj = new Ballistica( hero.pos, target, Ballistica.STOP_SOLID );
		int reach = Math.min( 2 + level, traj.dist );

		//a raw roll of what is in hand. Deliberately not Hero.damageRoll(): that path runs the
		//skill tree's damage modifiers, so an active Riposte would spend its mana and yell its cast
		//text from inside this thrust
		KindOfWeapon wep = hero.belongings.weapon();
		int roll = wep != null ? wep.damageRoll( hero ) : RingOfForce.damageRoll( hero );

		boolean hit = false;
		for (int cell : traj.subPath( 1, reach )){
			Char ch = Actor.findChar( cell );
			if (Dungeon.level.heroFOV[cell] && !Dungeon.level.solid[cell]){
				CellEmitter.center( cell ).burst( Speck.factory( Speck.STAR ), 2 );
			}
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				ch.damage( Math.round( roll * 0.8f ), this );
				Wound.hit( ch );
				if (ch.isAlive()){
					Buff.affect( ch, Bleeding.class ).set( 2 + level );
				}
				hit = true;
			}
		}

		if (!hit){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		if (level >= MAX_LEVEL){
			SkillSequence.start(hero, SkillSequence.LANCES, 1, hero.pos, Math.max(1, roll/3), 2, traj.subPath(1, reach));
		}
		SkillSpectacleFX.fly(SkillSpectacleFX.LANCE, hero.pos, traj.path.get(reach), 0, .4f);
		hero.MP -= getManaCost();
		castTextYell();
		Sample.INSTANCE.play( Assets.Sounds.HIT_STAB, 1f, 1.0f );
		Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.spend( TIME_TO_USE );
		hero.busy();
		hero.sprite.operate( target );
		Invisibility.dispel();
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
