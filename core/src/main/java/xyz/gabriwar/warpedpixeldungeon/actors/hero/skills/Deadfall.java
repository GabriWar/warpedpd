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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;


import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Deadfall extends Skill {

	private static final int RIG_TURNS = 30;

	{
		tag = "D3";
		name = "Deadfall";
		castText = "Down it comes.";
		image = 127;
		tier = 3;
		mana = 12;
	}

	private static xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField rig( Hero hero ){
		for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField f : hero.buffs( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.class )){
			if (f.kind == xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.RIGGED && f.sameFloor()) return f;
		}
		return null;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && (rig( hero ) != null || hero.MP >= getManaCost()))
			actions.add(AC_CAST);
		return actions;
	}

	//rig a weight over a 3x3 area; cast again while it is rigged to cut the line early, for free
	@Override
	public void execute( Hero hero, String action ){
		if (!action.equals(Skill.AC_CAST) || level <= 0) return;
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField rigged = rig( hero );
		if (rigged != null){
			drop( hero, rigged );
			hero.heroSkills.lastUsed = this;
			hero.spendAndNext( TIME_TO_USE );
			return;
		}
		if (hero.MP >= getManaCost()){
			GameScene.selectCell( new Rigger() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Rigger extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (curUser.MP < getManaCost()) return;

			int cell = target;
			if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.heroFOV[cell] || Dungeon.level.solid[cell]){
				GLog.w( Messages.get(Deadfall.this, "no_vision") );
				return;
			}

			ArrayList<Integer> cells = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) cells.add( c );
			}
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.place( curUser, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.RIGGED, level, RIG_TURNS, cells ).origin = cell;
			curUser.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.TRAMPLE, 1f, 0.8f );
			CellEmitter.get( cell ).burst( Speck.factory( Speck.DUST ), 6 );
			Dungeon.hero.sprite.operate( Dungeon.hero.pos );
			curUser.spendAndNext( TIME_TO_USE );
		}

		@Override
		public String prompt(){
			return Messages.get(Deadfall.class, "prompt");
		}
	}

	//the first enemy to step under the rigging brings it down
	@Override
	public void onCharMoved( Char ch, int from, boolean travelling ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY) return;
		final xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField rigged = rig( hero );
		if (rigged == null || !rigged.contains( ch.pos )) return;
		rigged.detach();
		SkillInteractions.defer( () -> drop( hero, rigged ) );
	}

	@Override
	public void onFieldTick( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField field ){
		if (field.kind != xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.RIGGED) return;
		//the rigging creaks over its center, so the Huntress can see where it hangs
		SkillInteractions.flare( field.origin, 0xC89A5A );
	}

	private void drop( Hero hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField rigged ){
		rigged.detach();
		int cell = rigged.origin;
		int rank = Math.max( 1, rigged.rank );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS, 1f, 1.0f );
		Camera.main.shake( 2, 0.4f );
		StoneOfBlast look = new StoneOfBlast();
		for (int c : rigged.cells){
			if (Dungeon.level.heroFOV[c]){
				SkillFX.rain( c, look, 1 + Random.Int( 2 ), null );
				CellEmitter.get( c ).burst( Speck.factory( Speck.ROCK ), 4 );
				CellEmitter.bottom( c ).burst( Speck.factory( Speck.DUST ), 3 );
			}
			Char ch = Actor.findChar( c );
			if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY){
				int dmg = Random.NormalIntRange( 4 + 3 * rank, 8 + 6 * rank );
				//+3: prey already pinned by roots or a cripple is crushed for double
				if (rank >= 3 && (ch.buff( Roots.class ) != null || ch.buff( Cripple.class ) != null)){
					dmg *= 2;
					Camera.main.shake( 3, 0.3f );
				}
				ch.damage( dmg, this );
				if (ch.sprite != null) ch.sprite.flash();
				if (ch.isAlive() && !ch.properties().contains( Char.Property.BOSS )){
					SkillInteractions.affectAfterHit( ch, Paralysis.class, rank >= 3 ? 2f : 1f );
				}
			}
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
