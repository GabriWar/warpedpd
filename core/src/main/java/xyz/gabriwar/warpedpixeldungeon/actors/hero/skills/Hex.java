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


import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;

import java.util.ArrayList;

public class Hex extends SubSkill2 {

	{
		name = "Hex";
		castText = "Suffer";
		image = 188;
		mana = 8;
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

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			GameScene.selectCell( new Curser() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Curser extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;
			int cell = new Ballistica( hero.pos, target, Ballistica.MAGIC_BOLT ).collisionPos;
			final Char ch = Actor.findChar( cell );
			if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]){
				GLog.w( Messages.get(Hex.class, "no_target") );
				return;
			}
			//the curse lands now; the bolt only shows it travelling
			Buff.prolong( ch, Weakness.class, 3 + 2 * level );
			Buff.prolong( ch, Vulnerable.class, 3 + 2 * level );
			if (level >= MAX_LEVEL) Buff.prolong( ch, Cripple.class, 2 + level );
			hero.MP -= getManaCost();
			castTextYell();
			SpatialSound.play( Assets.Sounds.CURSED, ch, 1f, 1.1f );
			hero.sprite.zap( cell );
			MagicMissile.boltFromChar( hero.sprite.parent, MagicMissile.SHADOW, hero.sprite, cell, () -> {
				if (ch.sprite != null){
					ch.sprite.emitter().burst( ShadowParticle.CURSE, 6 );
					ch.sprite.flash();
				}
			} );
			Dungeon.hero.heroSkills.lastUsed = Hex.this;
			hero.spendAndNext( TIME_TO_USE );
		}

		@Override
		public String prompt(){
			return Messages.get(Hex.class, "prompt");
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
