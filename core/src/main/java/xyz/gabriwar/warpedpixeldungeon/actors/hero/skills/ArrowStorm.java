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


import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArrowRain;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

public class ArrowStorm extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	{
		tag = "A4";
		name = "Arrow Storm";
		castText = "Rain of arrows!";
		image = 125;
		tier = 4;
		mana = 10;
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
			GameScene.selectCell( new Volley() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private class Volley extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;

			Hero curUser = Dungeon.hero;
			if (curUser.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.PROJECTILE );
			int cell = shot.collisionPos;
			curUser.sprite.zap( cell );
			curUser.MP -= getManaCost();
			castTextYell();
			Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 1f, 0.8f );
			//the storm falls now and keeps falling for two more turns
			Buff.append( curUser, ArrowRain.class ).set( cell, level, 3 );
			//the volley, seen: a fan of arrows loosed skyward one after another, the string a note
			//higher each, then dust kicking up under the impact cells in sequence as the storm lands
			if (curUser.sprite.parent != null){
				final int n = 2 + level;
				final PointF top = DungeonTilemap.tileCenterToWorld( cell );
				FxTimeline t = FxTimeline.start();
				for (int i = 0; i < n; i++){
					final int k = i;
					t.at( 0.07f * i, () -> {
						if (curUser.sprite == null || curUser.sprite.parent == null) return;
						PointF sky = new PointF( top.x + (k - (n - 1) / 2f) * 6f, top.y - DungeonTilemap.SIZE * 6 );
						((MissileSprite) curUser.sprite.parent.recycle( MissileSprite.class )).reset( curUser.sprite.center(), sky, new Dart(), null );
						Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 0.7f, 0.9f + 0.08f * k );
					} );
				}
				int order = 0;
				for (int c : SkillInteractions.area( cell, 1 )){
					if (!Dungeon.level.heroFOV[c]) continue;
					final int at = c, k = order++;
					t.at( 0.45f + 0.05f * k, () -> {
						CellEmitter.bottom( at ).burst( Speck.factory( Speck.DUST ), 3 );
						if (k % 3 == 0) Sample.INSTANCE.play( Assets.Sounds.HIT_ARROW, 0.5f, 1.1f + 0.05f * k );
					} );
				}
			}
			Invisibility.dispel();
			curUser.spendAndNext( TIME_TO_USE );
		}

		@Override
		public String prompt(){
			return Messages.get( ArrowStorm.class, "prompt" );
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
