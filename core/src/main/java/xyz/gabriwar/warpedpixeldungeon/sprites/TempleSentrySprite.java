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

package xyz.gabriwar.warpedpixeldungeon.sprites;

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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleSentry;

//Re-ARranged's temple sentry sprite. Warped's SentryRoom.SentrySprite cannot be
//reused: its link() casts the Char to SentryRoom.Sentry, so a TempleSentry
//crashes on scene load.
public class TempleSentrySprite extends MobSprite {

		private Animation charging;
		private Emitter chargeParticles;

		public TempleSentrySprite(){
			texture( Assets.Sprites.NEW_SENTRY );

			idle = new Animation(1, true);
			idle.frames(texture.uvRect(0, 0, 8, 15));

			run = idle.clone();
			attack = idle.clone();
			charging = idle.clone();
			die = idle.clone();
			zap = idle.clone();

			play( idle );
		}

		@Override
		public void zap( int pos ) {
			idle();
			flash();
			emitter().burst(MagicMissile.WardParticle.UP, 2);
			if (Actor.findChar(pos) != null){
				parent.add(new Beam.DeathRay(center(), Actor.findChar(pos).sprite.center()));
			} else {
				parent.add(new Beam.DeathRay(center(), DungeonTilemap.raisedTileCenterToWorld(pos)));
			}
			((TempleSentry) ch).onZapComplete();
		}

		@Override
		public void link(Char ch) {
			super.link(ch);

			chargeParticles = centerEmitter();
			chargeParticles.autoKill = false;
			chargeParticles.pour(MagicMissile.MagicParticle.ATTRACTING, 0.05f);
			chargeParticles.on = false;

			if (((TempleSentry) ch).curChargeDelay != ((TempleSentry) ch).initialChargeDelay){
				play(charging);
			}
		}

		@Override
		public void die() {
			super.die();
			if (chargeParticles != null){
				chargeParticles.on = false;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (chargeParticles != null){
				chargeParticles.killAndErase();
			}
		}

		public void charge(){
			play(charging);
			if (visible) Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
		}

		@Override
		public void play(Animation anim) {
			if (chargeParticles != null) chargeParticles.on = anim == charging;
			super.play(anim);
		}

		private float baseY = Float.NaN;

		@Override
		public void place(int cell) {
			super.place(cell);
			baseY = y;
		}

		@Override
		public void turnTo(int from, int to) {
			//do nothing
		}

		@Override
		public void update() {
			super.update();
			if (chargeParticles != null){
				chargeParticles.pos( center() );
				chargeParticles.visible = visible;
			}

			if (!paused){
				if (Float.isNaN(baseY)) baseY = y;
				y = baseY + (float) Math.sin(Game.timeTotal);
				shadowOffset = 0.25f - 0.8f*(float) Math.sin(Game.timeTotal);
			}
		}

	}
