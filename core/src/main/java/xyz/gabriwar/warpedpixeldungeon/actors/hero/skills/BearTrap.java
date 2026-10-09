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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;


import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

public class BearTrap extends Skill {

	{
		tag = "D2";
		name = "Bear Trap";
		castText = "Mind your step.";
		image = 126;
		tier = 2;
		mana = 6;
	}

	//how long set jaws wait for prey
	private static final int JAWS_TURNS = 200;
	//marks the chain as the bear trap's own, so the thorn bed answers only its snap
	private static final int JAWS_CHAIN = -7;

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
			GameScene.selectCell( new Placer() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Placer extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (curUser.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			int cell = shot.collisionPos;

			//the jaws are set on the ground itself: the tile stays walkable, so prey walks straight in
			if (!SkillInteractions.valid( cell ) || Dungeon.level.solid[cell] || !Dungeon.level.passable[cell]
					|| Dungeon.level.pit[cell] || Actor.findChar( cell ) != null){
				GLog.w( Messages.get(BearTrap.this, "no_ground") );
				return;
			}
			if (Dungeon.level.traps.get(cell) != null || jawsAt( curUser, cell ) != null){
				GLog.w( Messages.get(BearTrap.this, "occupied") );
				return;
			}

			xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.place( curUser, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.JAWS, level,
					JAWS_TURNS, java.util.Collections.singletonList( cell ) ).origin = cell;
			curUser.MP -= getManaCost();
			castTextYell();
			//the mechanism is seen tossed to its spot; it clicks open as it settles in the dust
			final int at = cell;
			Item look = new Item(){{ image = ItemSpriteSheet.TRAP_MECHANISM; }};
			SpatialSound.play( Assets.Sounds.MISS, curUser, 0.8f, 1.2f );
			SkillFX.streak( curUser.sprite, cell, look, () -> {
				CellEmitter.get( at ).burst( Speck.factory( Speck.DUST ), 4 );
				CellEmitter.center( at ).burst( SparkParticle.FACTORY, 3 );
				SkillSpectacleFX.show( SkillSpectacleFX.JAW, at );
				SpatialSound.play( Assets.Sounds.STURDY, at, 1f, 1.3f );
			} );
			curUser.spend( TIME_TO_USE );
			curUser.busy();
			curUser.sprite.operate( curUser.pos );
		}

		@Override
		public String prompt(){
			return Messages.get(BearTrap.class, "prompt");
		}
	}

	private static xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField jawsAt( Hero hero, int cell ){
		for (xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField f : hero.buffs( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.class )){
			if (f.kind == xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.JAWS && f.contains( cell )) return f;
		}
		return null;
	}

	//the first grounded enemy to step onto set jaws springs them
	@Override
	public void onCharMoved( Char ch, int from, boolean travelling ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || ch == null || ch == hero || ch.flying
				|| ch.alignment != Char.Alignment.ENEMY) return;
		final xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField jaws = jawsAt( hero, ch.pos );
		if (jaws == null) return;
		final int cell = ch.pos;
		final int rank = jaws.rank;
		jaws.detach();
		SkillInteractions.defer( () -> spring( ch, cell, rank ) );
	}

	private static void spring( Char c, int pos, int rank ){
		//the jaws snap shut, sparks flying off the teeth, and bite down once more as they settle
		SkillSpectacleFX.show(SkillSpectacleFX.JAW,pos);
		SpatialSound.play( Assets.Sounds.TRAP, pos, 1f, 0.8f );
		Camera.main.shake( 1, 0.2f );
		CellEmitter.center( pos ).burst( Speck.factory( Speck.STAR ), 4 );
		CellEmitter.center( pos ).burst( SparkParticle.FACTORY, 8 );
		FxTimeline.start().at( 0.14f, () -> {
			SkillSpectacleFX.show( SkillSpectacleFX.JAW, pos );
			CellEmitter.center( pos ).burst( SparkParticle.FACTORY, 4 );
			SpatialSound.play( Assets.Sounds.TRAP, pos, 0.6f, 1.2f );
		} );
		if (c != null && c.isAlive() && c.pos == pos){
			if (c.sprite != null) c.sprite.showStatus( CharSprite.NEGATIVE, "Caught" );
			SkillInteractions.Mark chain = SkillInteractions.mark( c, SkillInteractions.Mark.CHAIN, 1, 5 + 2 * rank );
			chain.cell = pos;
			chain.power = 0;
			chain.other = JAWS_CHAIN;
			Buff.affect( c, Bleeding.class ).set( 2 + 2 * rank );
			Buff.prolong( c, Cripple.class, 5f );
			Wound.hit( c );
			if (c.sprite != null) c.sprite.flash();
		} else {
			Wound.hit( pos );
		}
	}

	//set jaws glint so the Huntress can see where she left them
	@Override
	public void onFieldTick( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField field ){
		if (field.kind != xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField.JAWS) return;
		for (int c : field.cells) SkillInteractions.flare( c, 0xB0B4BE );
	}

	//+3: when the chain finally snaps, a thorn bed wakes where the jaws bit
	@Override
	public void onSkillMarkEnded( Char ch, SkillInteractions.Mark mark ){
		Hero hero = Dungeon.hero;
		if (level < MAX_LEVEL || hero == null || !hero.isAlive() || mark.kind != SkillInteractions.Mark.CHAIN
				|| mark.other != JAWS_CHAIN || !SkillInteractions.valid( mark.cell )) return;
		final int cell = mark.cell;
		SkillInteractions.defer( () -> SkillSequence.start( hero, SkillSequence.BRIAR, 3, cell, 5, 3, java.util.Collections.emptyList() ) );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** traps set before the jaws became a field: kept so older saves still load and still bite */
	public static class BearTrapHazard extends Trap {

		private static final String BUILT_AT = "builtAt";

		public int builtAt = 1;

		{
			color = GREY;
			shape = CROSSHAIR;
			disarmedByActivation = true;
			canBeHidden = false;
		}

		public BearTrapHazard(){
		}

		public BearTrapHazard( int builtAt ){
			this.builtAt = builtAt;
		}

		@Override
		public void activate(){
			spring( Actor.findChar( pos ), pos, builtAt );
		}

		@Override
		public String name(){
			return Messages.get( BearTrap.class, "trap_name" );
		}

		@Override
		public String desc(){
			return Messages.get( BearTrap.class, "trap_desc" );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ){
			super.restoreFromBundle( bundle );
			builtAt = bundle.getInt( BUILT_AT );
		}

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			bundle.put( BUILT_AT, builtAt );
		}
	}
}
