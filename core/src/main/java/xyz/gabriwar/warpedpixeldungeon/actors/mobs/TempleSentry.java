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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.TempleSentrySprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Bones;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LostInventory;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import java.util.ArrayList;

//extracted from Re-ARranged's TempleLastLevel, which is dead code there (never
//instantiated). Only its Sentry is used, by the temple sentry chambers.
public class TempleSentry extends NPC {

		{
			spriteClass = TempleSentrySprite.class;

			properties.add(Property.IMMOVABLE);
		}

		public float initialChargeDelay;
		public float curChargeDelay;

		@Override
		protected boolean act() {
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
				fieldOfView = new boolean[Dungeon.level.length()];
			}
			Dungeon.level.updateFieldOfView( this, fieldOfView );

			if (properties().contains(Property.IMMOVABLE)){
				throwItems();
			}

			if (Dungeon.hero != null){
				if (fieldOfView[Dungeon.hero.pos]
						&& Dungeon.level.map[Dungeon.hero.pos] == Terrain.EMPTY_SP
						&& Dungeon.hero.buff(LostInventory.class) == null){

					if (curChargeDelay > 0.001f){ //helps prevent rounding errors
						if (curChargeDelay == initialChargeDelay) {
							((TempleSentrySprite) sprite).charge();
						}
						curChargeDelay -= Dungeon.hero.cooldown();
						//pity mechanic so mistaps don't get people instakilled
						if (Dungeon.hero.cooldown() >= 0.34f){
							Dungeon.hero.interrupt();
						}
					}

					if (curChargeDelay <= .001f){
						curChargeDelay = 1f;
						sprite.zap(Dungeon.hero.pos);
						((TempleSentrySprite) sprite).charge();
					}

					spend(Dungeon.hero.cooldown());
					return true;

				} else {
					curChargeDelay = initialChargeDelay;
					sprite.idle();
				}

				spend(Dungeon.hero.cooldown());
			} else {
				spend(1f);
			}
			return true;
		}

		public void onZapComplete(){
			if (hit(this, Dungeon.hero, true)) {
				Dungeon.hero.damage(Random.IntRange(1, 3), new Eye.DeathGaze());
				if (!Dungeon.hero.isAlive()) {
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail(this);
					GLog.n(Messages.capitalize(Messages.get(Char.class, "kill", name())));
				}
			} else {
				Dungeon.hero.sprite.showStatus( CharSprite.NEUTRAL,  Dungeon.hero.defenseVerb() );
			}
		}

		@Override
		public int attackSkill(Char target) {
			return 20 + Dungeon.depth * 2;
		}

		@Override
		public int defenseSkill( Char enemy ) {
			return INFINITE_EVASION;
		}

		@Override
		public void damage( int dmg, Object src ) {
			//do nothing
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public boolean reset() {
			return true;
		}

		@Override
		public boolean interact(Char c) {
			return true;
		}

		private static final String INITIAL_DELAY = "initial_delay";
		private static final String CUR_DELAY = "cur_delay";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(INITIAL_DELAY, initialChargeDelay);
			bundle.put(CUR_DELAY, curChargeDelay);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			initialChargeDelay = bundle.getFloat(INITIAL_DELAY);
			curChargeDelay = bundle.getFloat(CUR_DELAY);
		}
	}
