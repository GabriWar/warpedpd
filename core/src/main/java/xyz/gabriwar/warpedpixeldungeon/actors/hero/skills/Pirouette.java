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


import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

import java.util.ArrayList;

public class Pirouette extends Skill {

	{
		tag = "CC";
		name = "Pirouette";
		castText = "Out of measure!";
		image = 144;
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
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && hero.MP >= getManaCost()){
			if (hero.rooted){
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Pirouette.class, "rooted" ) );
				return;
			}
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.selectCell( new xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector.Listener(){
				@Override
				public void onSelect( Integer target ){
					if (target != null) spin( hero, target );
				}

				@Override
				public String prompt(){
					return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Pirouette.class, "prompt" );
				}
			} );
		}
	}

	//spin away to a chosen tile up to 2 / 3 / 4 away, landing behind a shield of 3 / 6 / 9
	private void spin( Hero hero, int target ){
		if (level <= 0 || hero.MP < getManaCost() || hero.rooted) return;
		if (!SkillInteractions.valid( target ) || target == hero.pos || !Dungeon.level.heroFOV[target]
				|| Dungeon.level.distance( hero.pos, target ) > 1 + level
				|| !Dungeon.level.passable[target] || Dungeon.level.pit[target] || Actor.findChar( target ) != null
				|| !SkillInteractions.clear( hero.pos, target )){
			xyz.gabriwar.warpedpixeldungeon.utils.GLog.w( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Pirouette.class, "no_room" ) );
			return;
		}
		final int from = hero.pos;
		final ArrayList<Char> left = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( from + n );
			if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY) left.add( ch );
		}
		hero.MP -= getManaCost();
		castTextYell();
		hero.busy();
		CellEmitter.bottom( from ).burst( Speck.factory( Speck.DUST ), 10 );
		new Flare( 5, 18 ).color( 0xCCE0FF, true ).show( hero.sprite, 0.5f ).angularSpeed = 240;
		Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.2f );
		Dungeon.hero.heroSkills.lastUsed = this;
		hero.sprite.jump( from, target, 4f, 0.25f, () -> {
			hero.move( target, false );
			Dungeon.level.occupyCell( hero );
			Dungeon.observe();
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();
			Buff.affect( hero, Barrier.class ).setShield( SkillInteractions.ofHealth( hero.HT, 0.03f * level ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
			//+3: the enemies you spun away from are left reeling
			if (level >= MAX_LEVEL){
				for (Char ch : left){
					if (!ch.isAlive()) continue;
					Buff.prolong( ch, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo.class, 2f );
					if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
				}
			}
			hero.spendAndNext( TIME_TO_USE );
		} );
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
