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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;

public class Challenge extends SubSkill3 {

	private static final float REFUND_TURNS = 5f;

	{
		name = "Challenge";
		castText = "Face me!";
		image = 169;
		mana = 8;
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

	//every enemy in sight comes to face you, and each one that answers hardens your guard
	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			int answered = 0;
			for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob : Dungeon.level.mobs.toArray(new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0])){
				if (Dungeon.level.heroFOV[mob.pos] && mob.alignment == xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY){
					mob.aggro( hero );
					mob.beckon( hero.pos );
					if (mob.sprite != null){
						mob.sprite.showStatus( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.WARNING, "!" );
						mob.sprite.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 3 );
					}
					answered++;
				}
			}
			if (answered > 0){
				int shield = SkillInteractions.ofHealth( hero.HT, 0.01f * (1 + level) ) * answered;
				Buff.affect( hero, Barrier.class ).incShield( shield );
				hero.sprite.showStatusWithIcon( xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.POSITIVE, Integer.toString( shield ),
						xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.SHIELDING );
				new Flare( 6, 22 ).color( 0xFF6644, true ).show( hero.sprite, 0.5f );
			}
			//+3: kills among the challengers in the next 5 turns give mana back
			if (level >= MAX_LEVEL) Buff.prolong( hero, Answered.class, REFUND_TURNS );
			hero.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE, 1f, 1.0f );
			Camera.main.shake( 1, 0.3f );
			Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			Dungeon.hero.heroSkills.lastUsed = this;
			hero.spend( TIME_TO_USE );
			hero.busy();
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public void onKill( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || hero.buff( Answered.class ) == null) return;
		int maxMana = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
		int mana = Math.max( 0, Math.min( 2, maxMana - hero.MP ) );
		if (mana <= 0) return;
		hero.MP += mana;
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
	}

	/** the challenge still rings: kills refund mana for a few turns */
	public static class Answered extends xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
