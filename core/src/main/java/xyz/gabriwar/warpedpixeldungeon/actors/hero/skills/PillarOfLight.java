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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Cleric: a column of light called down on one enemy in view. It burns for a fixed
 * sum that grows with the cleric, half again against the undead and the demonic,
 * blinds the enemies beside it from level 2, and at level 3 unfolds a cross of
 * light spires around the impact.
 */
public class PillarOfLight extends Skill {

	private static final int RANGE = 8;
	private static final int SPIRE_DAMAGE = 7;

	{
		tag = "A5";
		name = "Pillar of Light";
		castText = "Let there be light!";
		image = 200;
		tier = 3;
		mana = 9;
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
			GameScene.selectCell( new Call() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Call extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			Hero hero = Dungeon.hero;
			if (level <= 0 || !canPayMana( hero, getManaCost() )) return;

			int cell = target;
			Char ch = Actor.findChar( cell );
			if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]
					|| Dungeon.level.distance( hero.pos, cell ) > RANGE){
				GLog.w( Messages.get(PillarOfLight.class, "no_target") );
				return;
			}

			payMana( hero, getManaCost() );
			castTextYell();
			Invisibility.dispel();

			hero.sprite.zap( cell );
			//the pillar rises from the ground, flashes at full height, and rains sparks as it fades
			final Char struck = ch;
			PillarRiseFX.show( cell, 0xFFEE88, () -> {
				SkillInteractions.flare( cell, 0xFFF1A1 );
				SkillFX.flash( struck );
				Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 0.9f );
			} );
            SkillSpectacleFX.show(SkillSpectacleFX.SPIRE,cell);
            if (level >= MAX_LEVEL)
                SkillSequence.start(hero,SkillSequence.CATHEDRAL,2,cell,SPIRE_DAMAGE,2,java.util.Collections.emptyList());
			Sample.INSTANCE.play( Assets.Sounds.RAY, 1f, 1.1f );

			int dmg = Random.NormalIntRange( 5 + 3 * level, 10 + 6 * level );
			if (ch.properties().contains( Char.Property.UNDEAD ) || ch.properties().contains( Char.Property.DEMONIC )){
				dmg = Math.round( dmg * 1.5f );
			}
			ch.damage( dmg, PillarOfLight.this );
			SkillFX.flash( ch );

			if (level >= 2){
				for (int n : PathFinder.NEIGHBOURS8){
					Char near = Actor.findChar( cell + n );
					if (near != null && near != hero && near.alignment == Char.Alignment.ENEMY){
						Buff.prolong( near, Blindness.class, 2f );
					}
				}
			}

			hero.spendAndNext( TIME_TO_USE );
		}

		@Override
		public String prompt(){
			return Messages.get(PillarOfLight.class, "prompt");
		}
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
