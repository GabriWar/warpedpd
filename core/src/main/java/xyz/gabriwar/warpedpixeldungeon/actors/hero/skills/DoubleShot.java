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


import java.util.ArrayList;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraBuff;

public class DoubleShot extends ActiveSkill2 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Double Shot";
		castText = "Two for one";
		image = 90;
		tier = 2;
		mana = 5;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		if (level >= 3) actions.add( AC_MARK );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals( AC_MARK )){
			pickMark();
			return;
		}
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			SpatialSound.play( Assets.Sounds.ATK_SPIRITBOW, hero, 1f, 1.3f );
			hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			AimedShot.switchOff( hero, AimedShot.class );
			AimedShot.switchOff( hero, Bombvoyage.class );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Aimed.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Fuse.class, false );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Double.class, true );
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			SpatialSound.play( Assets.Sounds.ATK_SPIRITBOW, hero, 0.6f, 0.9f );
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 2 );
			StanceAuraBuff.sync( hero, RogueHuntressAuras.Double.class, false );
		}
	}

	//the old latch let this fire on every second shot no matter the rank, so ranks 2
	//and 3 bought nothing. damageRoll() and Char.damage() never re-enter attackProc,
	//so there is no loop to guard against and the rank can own the proc rate instead
	@Override
	public boolean doubleShot(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		if (Random.Int(100) >= 25 + 25 * level)
			return false;
		castTextYell();
		Dungeon.hero.MP -= getManaCost();
		//the second arrow leaving the string: a twang a note higher and two sparks off the bow
		Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 0.8f, 1.5f );
		if (Dungeon.hero.sprite != null) Dungeon.hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 2 );
		return true;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	// ---- the mark: the enemy the second shot goes into, chosen by the player ----

	private int mark = -1;

	/** the marked enemy, if it is still there to be hit; marking is the +3 upgrade */
	public Char marked(){
		if (mark == -1 || level < 3) return null;
		Actor a = Actor.findById( mark );
		if (a instanceof Char && ((Char) a).isAlive() && ((Char) a).alignment == Char.Alignment.ENEMY) return (Char) a;
		mark = -1;
		return null;
	}

	private void pickMark(){
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ){
				if (cell == null) return;
				Char ch = Actor.findChar( cell );
				if (ch == null || ch == Dungeon.hero || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]){
					GLog.w( Messages.get( DoubleShot.class, "no_mark" ) );
					return;
				}
				mark = ch.id();
				if (ch.sprite != null){
					new Flare( 6, 14 ).color( 0xFF5555, true ).show( ch.sprite, 0.8f );
					ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), 3 );
				}
				SpatialSound.play( Assets.Sounds.BEACON, ch, 0.7f, 1.4f );
				GLog.i( Messages.get( DoubleShot.class, "marked", ch.name() ) );
			}
			@Override
			public String prompt(){
				return Messages.get( DoubleShot.class, "mark_prompt" );
			}
		} );
	}

	private static final String MARK = "MARK";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SKILL_LEVEL + " " + tag + " " + MARK, mark );
	}

	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		String key = SKILL_LEVEL + " " + tag + " " + MARK;
		mark = bundle.contains( key ) ? bundle.getInt( key ) : -1;
	}
}
