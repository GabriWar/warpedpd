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

import com.watabou.utils.PointF;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChargedShotDraw;
import com.watabou.noosa.audio.Sample;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Huntress: a sniper's draw. She stands and pulls for ten, seven or five turns, and
 * the arrow that leaves cannot miss: it goes through walls and through every enemy
 * on its line, out to the edge of its reach. One tile wide, a long way, everything
 * on it. It costs the time and the mana of a big spell, because it is one.
 */
public class ChargedShot extends Skill {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }


	@Override
	public boolean rangedSource(){ return true; }

	{
		tag = "A5";
		name = "Charged Shot";
		castText = "Loose!";
		image = 197;
		tier = 4;
		mana = 14;
	}

	public int reach(){
		return 8 + 2 * level;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && (hero.MP >= getManaCost() || hero.buff(ChargedShotDraw.class)!=null))
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && (hero.MP >= getManaCost() || hero.buff(ChargedShotDraw.class)!=null)){
			GameScene.selectCell( new Draw() );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	public int chargeTurns(){
		return level >= 3 ? 5 : level == 2 ? 7 : 10;
	}

	private class Draw extends CellSelector.Listener {

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			Hero hero = Dungeon.hero;
			if (level <= 0 || (hero.MP < getManaCost() && hero.buff(ChargedShotDraw.class)==null)) return;
			if (target == hero.pos){
				GLog.w( Messages.get(ChargedShot.class, "no_target") );
				return;
			}
			if (hero.buff( ChargedShotDraw.class ) != null){
				hero.buff(ChargedShotDraw.class).aim(target);
                hero.spendAndNext(TIME_TO_USE);
				return;
			}

			hero.MP -= getManaCost();
			castText = Messages.get( ChargedShot.class, "cast_draw" );
			castTextYell();
			Dungeon.hero.heroSkills.lastUsed = ChargedShot.this;
			Invisibility.dispel();

			//she stands and draws: the turns pass, the light gathers, the lance goes on the last
			int turns = chargeTurns();
			Buff.append( hero, ChargedShotDraw.class ).set( target, level, turns );
			new Flare( 6, 16 ).color( 0xFFE9A0, true ).show( hero.sprite, 1f ).angularSpeed = 90;
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 1f, 1.1f );
			hero.sprite.zap( target );
			hero.spendAndNext(TIME_TO_USE);
		}

		@Override
		public String prompt(){
			return Messages.get(ChargedShot.class, "prompt");
		}
	}

	/** the lance itself, loosed from where the hero stands along the line to target */
	public static void fire( Hero hero, int target, int level ){
		if (hero == null || !hero.isAlive() || target == hero.pos) return;
		int reach = 8 + 2 * level;

		//through everything: the line runs to the edge of its reach whatever stands in it
		Ballistica line = new Ballistica( hero.pos, target, Ballistica.WONT_STOP );
		int steps = Math.min( reach, line.path.size() - 1 );
		if (steps < 1) return;
		int far = line.path.get( steps );

		//one tile wide: the cells of the line, out to the edge of its reach
		ArrayList<Integer> lane = new ArrayList<>();
		for (int i = 1; i <= steps; i++) lane.add( line.path.get( i ) );

		ChargedShot self = hero.heroSkills != null ? hero.heroSkills.get( ChargedShot.class ) : null;
		if (self != null){
			self.castText = Messages.get( ChargedShot.class, "cast" );
			self.castTextYell();
		}
		Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 1f, 0.8f );
		hero.sprite.zap( far );
		//the arrow, with the beam of its passage drawn behind it along the whole line
		PointF from = hero.sprite.center();
		PointF to = DungeonTilemap.raisedTileCenterToWorld( far );
		hero.sprite.parent.add( new Beam.LightRay( from, to ) );
        SpiritBow bow = hero.belongings.getItem( SpiritBow.class );
		SkillFX.streak( hero.sprite, far, bow != null ? bow.knockArrow() : new Dart(), null );

		int base = bow != null ? bow.knockArrow().damageRoll( hero ) : hero.damageRoll();
		int dmg = Math.round( base * (1.25f + 0.25f * level) );
		//the lance's passage: dust thrown up tile after tile down the line, the ground shaking with it
		Camera.main.shake( 1, 0.25f );
		FxTimeline t = FxTimeline.start();
		int struck = 0;
		for (int i = 0; i < lane.size(); i++){
			int c = lane.get( i );
			if (Dungeon.level.heroFOV[c] && !Dungeon.level.solid[c]){
				final int at = c;
				t.at( 0.02f * i, () -> CellEmitter.center( at ).burst( Speck.factory( Speck.DUST ), 2 ) );
			}
			Char ch = Actor.findChar( c );
			if (ch != null && ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				ch.damage( dmg, self != null ? self : hero );
				SkillFX.flash( ch );
				Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 1f, 1.0f + 0.1f * struck++ );
				if (ch.isAlive() && level >= MAX_LEVEL){
					Buff.prolong( ch, Vulnerable.class, 3f );
				}
			}
		}
	}

	//the shout swaps between the draw and the release
	@Override
	public String castText(){
		return castText;
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
