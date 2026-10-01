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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.HolyFlameParticle;


import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ConeAOE;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class ScouringFlame extends Skill {

	{
		name = "Scouring Flame";
		castText = "Burn clean!";
		tag = "D2";
		image = 156;
		tier = 2;
		mana = 7;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && canPayMana( hero, getManaCost() ))
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_CAST) && level > 0 && canPayMana( hero, getManaCost() )){
			GameScene.selectCell( zapper );
			Dungeon.hero.heroSkills.lastUsed = this;
		}
	}

	//the cone reaches 3 tiles at level 1, 4 at level 2, 5 at level 3
	public int reach(){
		return 2 + level;
	}

	private final CellSelector.Listener zapper = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ){
			if (cell == null)
				return;
			final Hero hero = Dungeon.hero;
			if (level <= 0 || !canPayMana( hero, getManaCost() ))
				return;
			if (cell == hero.pos){
				GLog.w( Messages.get(ScouringFlame.this, "no_target") );
				return;
			}

			//a cone of white fire, aimed rather than thrown: everything in it is scoured
			Ballistica aim = new Ballistica( hero.pos, cell, Ballistica.STOP_SOLID );
			ConeAOE cone = new ConeAOE( aim, reach(), 50, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET );

			payMana( hero, getManaCost() );
			castTextYell();
			hero.sprite.zap( cell );
			Sample.INSTANCE.play( Assets.Sounds.BURNING, 1f, 1.3f );
			Sample.INSTANCE.play( Assets.Sounds.RAY, 0.7f, 1.2f );
			for (Ballistica ray : cone.rays){
				MagicMissile.boltFromChar( hero.sprite.parent, MagicMissile.LIGHT_MISSILE, hero.sprite, ray.path.get( ray.dist ), null );
			}

			int dmg = 4 + 3 * level;
			//the white fire rolls outward one ring at a time; the hits themselves land at once
			FxTimeline rings = FxTimeline.start();
			ArrayList<Integer> scoured = new ArrayList<>( cone.cells );
			scoured.sort( (a, b) -> Dungeon.level.distance( hero.pos, a ) - Dungeon.level.distance( hero.pos, b ) );
			boolean any = false;
			for (int c : scoured){
				final int ring = Dungeon.level.distance( hero.pos, c );
				final float when = 0.07f * ring;
				if (Dungeon.level.heroFOV[c]) rings.at( when, () -> CellEmitter.center( c ).burst( HolyFlameParticle.FACTORY, 3 ) );
				Char target = Actor.findChar( c );
				if (target == null || target == hero || target.alignment != Char.Alignment.ENEMY || !target.isAlive()) continue;
				int hit = dmg;
				if (Char.hasProp( target, Char.Property.UNDEAD ) || Char.hasProp( target, Char.Property.DEMONIC ))
					hit *= 2;
				rings.at( when, () -> {
					if (target.sprite != null){
						target.sprite.flash();
						CellEmitter.center( target.pos ).burst( HolyFlameParticle.FACTORY, 7 );
					}
					Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 0.9f + 0.1f * ring );
				} );
				target.damage( hit, ScouringFlame.this );
				//at level 3 the white fire leaves its survivors blinded
				if (level >= MAX_LEVEL && target.isAlive())
					Buff.prolong( target, Blindness.class, 3f );
				any = true;
			}

			Invisibility.dispel();
			hero.spendAndNext( TIME_TO_USE );
		}
		@Override
		public String prompt(){
			return Messages.get(ScouringFlame.this, "prompt");
		}
	};

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}
