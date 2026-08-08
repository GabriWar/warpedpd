/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.levels.features;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import xyz.gabriwar.warpedpixeldungeon.items.spells.FeatherFall;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.WeakFloorRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Chasm implements Hero.Doom {

	public static boolean jumpConfirmed = false;
	private static int heroPos;
	
	public static void heroJump( final Hero hero ) {
		heroPos = hero.pos;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(
						new WndOptions( new Image(Dungeon.level.tilesTex(), 176, 16, 16, 16),
								Messages.get(Chasm.class, "chasm"),
								Messages.get(Chasm.class, "jump"),
								Messages.get(Chasm.class, "yes"),
								Messages.get(Chasm.class, "no") ) {

							private float elapsed = 0f;

							@Override
							public synchronized void update() {
								super.update();
								elapsed += Game.elapsed;
							}

							@Override
							public void hide() {
								if (elapsed > 0.2f){
									super.hide();
								}
							}

							@Override
							protected void onSelect( int index ) {
								if (index == 0 && elapsed > 0.2f) {
									if (Dungeon.hero.pos == heroPos) {
										jumpConfirmed = true;
										hero.resume();
									}
								}
							}
						}
				);
			}
		});
	}
	
	public static void heroFall( int pos ) {

		jumpConfirmed = false;

		Sample.INSTANCE.play( Assets.Sounds.FALLING );

		//the safe zone has no floor below it: falling here just drops the hero
		//onto solid ground elsewhere on the same safe level, never out of it
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel) {
			Dungeon.hero.interrupt();
			int land = Dungeon.level.randomRespawnCell( Dungeon.hero );
			if (land == -1) {
				//no free landing cell: nudge to a passable neighbour or stay put
				for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8) {
					int c = pos + d;
					if (c >= 0 && c < Dungeon.level.length()
							&& Dungeon.level.passable[c]
							&& xyz.gabriwar.warpedpixeldungeon.actors.Actor.findChar(c) == null) {
						land = c;
						break;
					}
				}
			}
			if (land != -1) {
				Dungeon.hero.pos = land;
				Dungeon.hero.sprite.place( land );
				Dungeon.level.occupyCell( Dungeon.hero );
			}
			Dungeon.observe();
			xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();
			GLog.w( Messages.get( Chasm.class, "safe_fall" ) );
			Dungeon.hero.spendAndNext( 1f );
			return;
		}

		Level.beforeTransition();

		if (Dungeon.hero.isAlive()) {
			Dungeon.hero.interrupt();
			InterlevelScene.mode = InterlevelScene.Mode.FALL;
			if (Dungeon.level instanceof RegularLevel &&
						((RegularLevel)Dungeon.level).room( pos ) instanceof WeakFloorRoom){
				InterlevelScene.fallIntoPit = true;
				Notes.remove(Notes.Landmark.DISTANT_WELL);
			} else {
				InterlevelScene.fallIntoPit = false;
			}
			Game.switchScene( InterlevelScene.class );
		} else {
			Dungeon.hero.sprite.visible = false;
		}
	}

	@Override
	public void onDeath() {
		Badges.validateDeathFromFalling();

		Dungeon.fail( Chasm.class );
		GLog.n( Messages.get(Chasm.class, "ondeath") );
	}

	public static void heroLand() {
		
		Hero hero = Dungeon.hero;
		
		ElixirOfFeatherFall.FeatherBuff b = hero.buff(ElixirOfFeatherFall.FeatherBuff.class);

		if (b != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.JET ), 20);
			b.processFall();
			return;
		}

		FeatherFall.FeatherBuff fb = hero.buff(FeatherFall.FeatherBuff.class);

		if (fb != null){
			hero.sprite.emitter().burst( Speck.factory( Speck.JET ), 20);
			fb.detach();
			return;
		}
		
		PixelScene.shake( 4, 1f );

		Dungeon.level.occupyCell(hero );
		Buff.prolong( hero, Cripple.class, Cripple.DURATION );

		//The lower the hero's HP, the more bleed and the less upfront damage.
		//Hero has a 50% chance to bleed out at 66% HP, and begins to risk instant-death at 25%
		Buff.affect( hero, Bleeding.class).set( Math.round(hero.HT / (6f + (6f*(hero.HP/(float)hero.HT)))), Chasm.class);
		hero.damage( Math.max( hero.HP / 2, Random.NormalIntRange( hero.HP / 2, hero.HT / 4 )), new Chasm() );
	}

	public static void mobFall( Mob mob ) {
		if (mob.isAlive()) {
			Buff.prolong(mob, Trap.HazardAssistTracker.class, Trap.HazardAssistTracker.DURATION);
			mob.die( Chasm.class );
		}
		
		if (mob.sprite != null) ((MobSprite)mob.sprite).fall();
	}
	
	public static class Falling extends Buff {
		
		{
			actPriority = VFX_PRIO;
		}
		
		@Override
		public boolean act() {
			heroLand();
			detach();
			return true;
		}
	}

}
