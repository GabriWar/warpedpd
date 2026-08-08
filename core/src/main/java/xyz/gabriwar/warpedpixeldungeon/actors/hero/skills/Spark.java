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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Spark extends ActiveSkill2 {

	{
		name = "Spark";
		castText = "Basic training";
		tier = 2;
		image = 45;
		mana = 3;
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
		if (action.equals(Skill.AC_CAST) && hero.MP >= getManaCost()){
			GameScene.selectCell( zapper );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	private static CellSelector.Listener zapper = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ){
			if (target != null){
				Hero curUser = Dungeon.hero;
				Skill skill = curUser.heroSkills.active2;
				final Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.MAGIC_BOLT );
				final int cell = shot.collisionPos;
				curUser.sprite.zap(cell);
				curUser.MP -= skill.getManaCost();
				skill.castTextYell();
				curUser.busy();
				Sample.INSTANCE.play( Assets.Sounds.ZAP );
				MagicMissile.boltFromChar( curUser.sprite.parent,
						MagicMissile.MAGIC_MISSILE,
						curUser.sprite,
						cell,
						new Callback() {
							@Override
							public void call(){
								Char ch = Actor.findChar( cell );
								Skill sk = Dungeon.hero.heroSkills.active2;
								if (ch != null){
									ch.damage(roll(sk), Dungeon.hero);
									if (ch.isAlive() && Random.Int(100) < 15 * sk.level){
										Buff.prolong( ch, Blindness.class, Random.Int( 1, 2 ) );
										ch.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
										ch.sprite.showStatus(CharSprite.WARNING, "Blinded!");
									}
									if (sk.level >= MAX_LEVEL)
										fork( cell, ch, sk );
								} else {
									GLog.i( "nothing happened" );
								}
								Dungeon.hero.spendAndNext( TIME_TO_USE );
							}
						} );
				Invisibility.dispel();
			}
		}

		@Override
		public String prompt(){
			return "Choose direction to cast";
		}
	};

	private static int roll( Skill sk ){
		return Random.Int(sk.level + Dungeon.hero.lvl / (6 - sk.level),
				sk.level * 3 + Dungeon.hero.lvl / (5 - sk.level));
	}

	//forked spark: the bolt jumps from the impact to the closest other enemy for half a roll
	private static void fork( int cell, Char struck, Skill sk ){
		Char arc = null;
		int closest = Integer.MAX_VALUE;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob == struck || mob.alignment != Char.Alignment.ENEMY || !mob.isAlive())
				continue;
			int dist = Dungeon.level.distance( cell, mob.pos );
			if (dist <= 3 && dist < closest){
				closest = dist;
				arc = mob;
			}
		}
		if (arc == null)
			return;
		Dungeon.hero.sprite.parent.add( new Lightning( cell, arc.pos, null ) );
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		arc.damage( Math.round( roll(sk) * 0.5f ), Dungeon.hero );
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
