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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MeteorFall;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Mage: a stone called down from the sky. The mage marks a tile in view and the
 * ground there begins to glow; on the next turn the meteor comes down on it and the
 * eight cells around it, in flame, with a blast that shakes the floor. Enemies get
 * one turn to see it coming, which is the whole game of it.
 */
public class MeteorCall extends Skill {

	private Mark pending;

	public int range(){return 5+2*level;}

	{
		tag = "A5";
		name = "Meteor Call";
		castText = "Fall!";
		image = 201;
		tier = 4;
		mana = 12;
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
			if (confirmTarget(hero)) return;
			Mark selection = new Mark();
			selection.suggested = autoTarget(hero);
			GameScene.selectCell(selection);
			if (!selection.finished) { pending = selection; selection.showMarker(); }
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private boolean validTarget(Hero hero, xyz.gabriwar.warpedpixeldungeon.actors.Char enemy) {
        return enemy != null && enemy.isAlive() && enemy.alignment == xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY
            && xyz.gabriwar.warpedpixeldungeon.actors.Actor.chars().contains(enemy)
            && SkillInteractions.valid(enemy.pos) && Dungeon.level.heroFOV[enemy.pos]
            && Dungeon.level.distance(hero.pos, enemy.pos) <= range() && SkillInteractions.clear(hero.pos, enemy.pos);
    }
    private xyz.gabriwar.warpedpixeldungeon.actors.Char autoTarget(Hero hero) {
        xyz.gabriwar.warpedpixeldungeon.actors.Char best = xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton.lastTarget;
        if (validTarget(hero, best)) return best;
        best = null;
        for (xyz.gabriwar.warpedpixeldungeon.actors.Char enemy : xyz.gabriwar.warpedpixeldungeon.actors.Actor.chars())
            if (validTarget(hero, enemy) && (best == null || Dungeon.level.distance(hero.pos, enemy.pos) < Dungeon.level.distance(hero.pos, best.pos))) best = enemy;
        return best;
    }
    @Override public boolean confirmTarget(Hero hero) {
        if (pending == null || !validTarget(hero, pending.suggested)) return false;
        GameScene.handleCell(pending.suggested.pos);
        return true;
    }

	private class Mark extends CellSelector.Listener {
		xyz.gabriwar.warpedpixeldungeon.actors.Char suggested;
		boolean finished;
		com.watabou.noosa.Image marker;

		void showMarker() {
			if (suggested == null || suggested.sprite == null || suggested.sprite.parent == null) return;
			marker = new com.watabou.noosa.Image(xyz.gabriwar.warpedpixeldungeon.ui.Icons.get(
					xyz.gabriwar.warpedpixeldungeon.ui.Icons.TARGET)) {
				@Override public void update() {
					super.update();
					visible = validTarget(Dungeon.hero, suggested) && suggested.sprite.visible;
					point(suggested.sprite.center(this));
				}
			};
			marker.point(suggested.sprite.center(marker));
			suggested.sprite.parent.addToFront(marker);
		}

		void finish() {
			finished = true;
			if (pending == this) pending = null;
			if (marker != null) { marker.killAndErase(); marker.destroy(); marker = null; }
		}


		@Override
		public void onSelect( Integer target ){
			finish();
			if (target == null || !SkillInteractions.valid(target)) return;
			Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;

			int cell = new Ballistica( hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID ).collisionPos;
			if (cell == hero.pos || !Dungeon.level.heroFOV[cell] || Dungeon.level.solid[cell]
					|| Dungeon.level.distance( hero.pos, cell ) > range()){
				GLog.w( Messages.get(MeteorCall.class, "no_target") );
				return;
			}


            if(!hero.isAlive() || hero.MP<getManaCost())return;
            hero.MP -= getManaCost();
			castTextYell();
			Invisibility.dispel();

			//the mark: the ground starts to glow, the sky goes red over it, and a ring of embers
			//spreads a beat later over the cells the stone will cover
			hero.sprite.zap( cell );
			new Flare( 5, 14 ).color( 0xFF6622, true ).show( hero.sprite, 0.6f );
			if (hero.sprite.parent != null){
				new Flare( 6, 16 ).color( 0xFF6622, true ).show( hero.sprite.parent,
						xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( cell ), 0.7f );
			}
			CellEmitter.center( cell ).burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX.ring( cell, 1, 0.15f,
					( c, r ) -> CellEmitter.get( c ).burst( Speck.factory( Speck.RED_LIGHT ), 2 ) );
			SpatialSound.play( Assets.Sounds.CHARGEUP, cell, 1f, 0.7f );
			Buff.append( hero, MeteorFall.class ).set( cell, level );

			hero.spendAndNext( TIME_TO_USE );

		}

		@Override
		public String prompt(){
			return Messages.get(MeteorCall.class, "prompt");
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
