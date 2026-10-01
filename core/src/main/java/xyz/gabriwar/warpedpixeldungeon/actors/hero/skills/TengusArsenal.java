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

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TenguShockBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TenguSmokeBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;

import java.util.ArrayList;

/**
 * Rogue: Tengu's tricks, learned by watching him. The rogue picks which bomb to
 * throw: the smoke bomb with its three-count fuse and a blast two tiles wide, the
 * fire bomb that bursts into a ring of flame, and at mastery the shocker that sits
 * where it lands and arcs lightning across its cell for three turns. None of them
 * hurt the rogue or an ally.
 */
public class TengusArsenal extends Skill {

	public static final int SMOKE = 0, FIRE = 1, SHOCK = 2;

	{
		tag = "D5";
		name = "Tengu's Arsenal";
		castText = "Catch!";
		image = 198;
		tier = 3;
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
			Dungeon.hero.heroSkills.lastUsed = this;
			ArrayList<String> options = new ArrayList<>();
			options.add( Messages.get(this, "opt_smoke") );
			if (level >= 2) options.add( Messages.get(this, "opt_fire") );
			if (level >= 3) options.add( Messages.get(this, "opt_shock") );
			GameScene.show( new WndOptions( new SkillSprite( image ), name(),
					Messages.get(this, "pick"), options.toArray( new String[0] ) ) {
				@Override
				protected void onSelect( int index ){
					GameScene.selectCell( new Thrower( index ) );
				}
			} );
		}
	}

	private class Thrower extends CellSelector.Listener {

		private final int kind;

		Thrower( int kind ){
			this.kind = kind;
		}

		@Override
		public void onSelect( Integer target ){
			if (target == null) return;
			final Hero hero = Dungeon.hero;
			if (level <= 0 || hero.MP < getManaCost()) return;

			Ballistica shot = new Ballistica( hero.pos, target, Ballistica.PROJECTILE );
			final int cell = shot.collisionPos;
			if (cell == hero.pos){
				GLog.w( Messages.get(TengusArsenal.class, "no_target") );
				return;
			}

			hero.MP -= getManaCost();
			castTextYell();
			Invisibility.dispel();
			Item look = kind == SHOCK ? new TenguShockBomb.RogueShockerItem() : new TenguSmokeBomb.RogueBombItem();
			hero.busy();
			hero.sprite.zap( cell );
			Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.2f + 0.15f * kind );
			SkillFX.streak( hero.sprite, cell, look, () -> {
				land( hero, cell );
				hero.spendAndNext( TIME_TO_USE );
			} );
		}

		private void land( Hero hero, int cell ){
			//thrown over a chasm the toy just tumbles away, nothing goes off
			if (Dungeon.level.map[cell] == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.CHASM || Dungeon.level.pit[cell]){
				GLog.w( Messages.get(TengusArsenal.class, "chasm") );
				return;
			}
			switch (kind){
				case FIRE:
					Sample.INSTANCE.play( Assets.Sounds.BURNING, 1f, 0.9f );
					//the burst at the centre first, then the ring of flame catching around it
					if (Dungeon.level.heroFOV[cell]) CellEmitter.center( cell ).burst( FlameParticle.FACTORY, 10 );
					FxTimeline flames = FxTimeline.start();
					int order = 0;
					for (int n : PathFinder.NEIGHBOURS9){
						int c = cell + n;
						if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
						GameScene.add( Blob.seed( c, 4, Fire.class ) );
						if (Dungeon.level.heroFOV[c] && n != 0){
							final int at = c;
							flames.at( 0.05f + 0.03f * order++, () -> CellEmitter.get( at ).burst( FlameParticle.FACTORY, 4 ) );
						}
						Char ch = Actor.findChar( c );
						if (ch != null && ch.alignment == Char.Alignment.ENEMY){
							Buff.affect( ch, Burning.class ).reignite( ch );
						}
					}
					break;
				case SHOCK:
					Buff.append( hero, TenguShockBomb.class ).set( cell, level );
					break;
				default:
					Buff.append( hero, TenguSmokeBomb.class ).set( cell, level );
					break;
			}
		}

		@Override
		public String prompt(){
			return Messages.get(TengusArsenal.class, "prompt");
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.3 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}
