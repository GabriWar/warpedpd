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


import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

public class PiercingFocus extends SubSkill3 {

	{
		name = "Piercing Focus";
		castText = "Focus...";
		image = 194;
		mana = 10;
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

	//a piercing stare: a ray of light to every enemy in view, dazing each for 5/7/9 turns
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			float turns = 3 + 2 * level;
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
				Buff.prolong( m, Daze.class, turns );
				if (m.sprite != null){
					m.sprite.emitter().burst( Speck.factory( Speck.STAR ), 2 );
					xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.center( m.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 3 );
					m.sprite.showStatus( CharSprite.WARNING, Messages.get( this, "dazed" ) );
				}
			}
			//+3: the focus reaches past the walls, sensing every enemy on the floor for a while
			if (level >= 3){
				Buff.prolong( hero, MindVision.class, 10f );
				Dungeon.observe();
				GameScene.updateFog();
			}
			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 1f, 0.6f );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
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
