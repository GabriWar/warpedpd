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


import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class BladeBind extends Skill {

	{
		tag = "CD";
		name = "Blade Bind";
		castText = "Bound!";
		image = 137;
		tier = 4;
		mana = 7;
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
					if (target != null){
						bind( hero, target );
					}
				}

				@Override
				public String prompt(){
					return Messages.get(BladeBind.class, "prompt");
				}
			} );
		}
	}

	//blades lock with an adjacent enemy: its blows are weakened
	private void bind( Hero hero, int target ){
		//the selector stays live across other casts, so re-check before spending
		if (level <= 0 || hero.MP < getManaCost()) return;

		final Char ch = Actor.findChar( target );
		if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()
				|| !Dungeon.level.adjacent( hero.pos, target )){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		hero.MP -= getManaCost();
		castTextYell();
		Buff.prolong( ch, Weakness.class, 2 + 2 * level );
		//+3: you wrench it off balance: its next attack goes wide and leaves it open
		if (level >= MAX_LEVEL) Buff.affect( ch, Wrenched.class );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.0f );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 0.8f, 0.7f );
		if (ch.sprite != null){
			ch.sprite.showStatus( CharSprite.WARNING, Messages.get( BladeBind.this, "bound" ) );
			ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			ch.sprite.flash();
		}
		xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.center( ch.pos ).burst( xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle.FACTORY, 8 );
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.sprite.operate( ch.pos );
		hero.spendAndNext( TIME_TO_USE );
	}

	//+3: the wrenched enemy's next attack on you misses outright
	@Override
	public boolean dodgeChance( Char attacker ){
		return level >= MAX_LEVEL && attacker != null && attacker.buff( Wrenched.class ) != null;
	}

	@Override
	public void onDodge( Char attacker ){
		if (attacker == null) return;
		Buff.detach( attacker, Wrenched.class );
		SkillInteractions.affectAfterHit( attacker, Vulnerable.class, 3f );
		if (attacker.sprite != null) attacker.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, 1.4f );
	}

	/** its weapon was wrenched aside: the next attack it makes goes wide */
	public static class Wrenched extends Buff {
		{
			type = buffType.NEGATIVE;
		}

		@Override
		public int icon(){ return xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator.NONE; }
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
