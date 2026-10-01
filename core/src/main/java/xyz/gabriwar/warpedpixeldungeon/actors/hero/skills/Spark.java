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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Spark extends ActiveSkill2 {

	{
		name = "Spark";
		castText = "Spark!";
		tier = 2;
		image = 45;
		mana = 3;
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public boolean rangedSource(){ return true; }

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
			if (target == null) return;
			Hero curUser = Dungeon.hero;
			Spark skill = curUser.heroSkills.get( Spark.class );
			if (skill == null || skill.level <= 0 || curUser.MP < skill.getManaCost()) return;
			if (target == curUser.pos){
				GLog.i( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( xyz.gabriwar.warpedpixeldungeon.items.wands.Wand.class, "self_target" ) );
				return;
			}
			//at mastery the bolt passes through allies and every enemy on its line, up to the first wall
			final boolean pierce = skill.level >= MAX_LEVEL;
			final Ballistica shot = new Ballistica( curUser.pos, target, pierce ? Ballistica.STOP_SOLID : Ballistica.MAGIC_BOLT );
			final int cell = shot.collisionPos;
			curUser.sprite.zap(cell);
			curUser.MP -= skill.getManaCost();
			skill.castTextYell();
			//the whole cast is one turn; the mage only waits for the last arc to fade
			curUser.spend( TIME_TO_USE );
			curUser.busy();
			Sample.INSTANCE.play( Assets.Sounds.ZAP );
			curUser.sprite.centerEmitter().burst( SparkParticle.FACTORY, 4 );
			MagicMissile.boltFromChar( curUser.sprite.parent,
					MagicMissile.MAGIC_MISSILE,
					curUser.sprite,
					cell,
					new Callback() {
						@Override
						public void call(){
							Spark sk = Dungeon.hero.heroSkills.get( Spark.class );
							CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 4 + 2 * sk.level );
							//damage lands now, in bolt order; the arcs then show it body by body
							ArrayList<Integer> struck = new ArrayList<>();
							if (pierce){
								for (int c : shot.subPath( 1, shot.dist )){
									Char ch = Actor.findChar( c );
									if (ch == null || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) continue;
									strike( ch, sk );
									struck.add( c );
								}
							} else {
								Char ch = Actor.findChar( cell );
								if (ch != null && ch != Dungeon.hero){
									strike( ch, sk );
									struck.add( cell );
								}
							}
							if (struck.isEmpty()) GLog.i( xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Spark.class, "no_target" ) );
							arc( Dungeon.hero, Dungeon.hero.pos, struck, 0, cell );
						}
					} );
			Invisibility.dispel();
		}

		@Override
		public String prompt(){
			return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( Spark.class, "prompt" );
		}
	};

	private static int roll( Skill sk ){
		return Random.IntRange( sk.level, 3 * sk.level );
	}

	private static void strike( Char ch, Spark sk ){
		int before = SkillInteractions.beforeMagicHit( ch, sk );
		ch.damage( roll( sk ), sk );
		SkillInteractions.afterMagicHit( ch, before, sk );
	}

	//one arc at a time: mage to the first body, then body to body down the line, each snap a little
	//higher than the last, sparks and a flash on whoever it reaches; the mage is free when the last fades
	private static void arc( final Hero hero, final int from, final ArrayList<Integer> struck, final int i, final int end ){
		if (hero.sprite == null || hero.sprite.parent == null){
			hero.next();
			return;
		}
		if (i >= struck.size()){
			//nothing (more) to hit: a first arc still grounds itself where the bolt broke
			if (i == 0 && from != end){
				hero.sprite.parent.add( new Lightning( from, end, hero::next ) );
			} else {
				hero.next();
			}
			return;
		}
		final int to = struck.get( i );
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING, 0.7f, 1.1f + 0.1f * i );
		CellEmitter.center( to ).burst( SparkParticle.FACTORY, 6 );
		Char ch = Actor.findChar( to );
		if (ch != null && ch.sprite != null) ch.sprite.flash();
		hero.sprite.parent.add( new Lightning( from, to, () -> arc( hero, to, struck, i + 1, end ) ) );
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
