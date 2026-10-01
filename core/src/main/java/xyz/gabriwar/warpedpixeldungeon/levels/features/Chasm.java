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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
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
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import java.util.ArrayList;
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

		//off a slice of the world the hero drops onto the slice below, on the
		//same world cell (the deepest cave has nothing under it)
		boolean bedrock = false;
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) {
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
					= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
			bedrock = !ow.fallsThrough( pos );
		}
		//the safe zone has no floor below it: falling here just drops the hero
		//onto solid ground elsewhere on the same safe level, never out of it
		if (bedrock || Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel) {
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
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) {
				((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).fallingFrom( pos );
			}
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

	//a monster with more than this share of its health always lives through the fall, and the landing
	//costs it that same share of its maximum health
	public static final float SURVIVE_HEALTH = 0.8f;
	public static void mobFall( Mob mob ) {
		if (mob.isAlive()) {
			boolean frozen = mob.buff( Frost.class ) != null;
			int below = frozen ? -1 : depthBelow( mob );
			if (below != -1 && mob.HP > SURVIVE_HEALTH * mob.HT){
				fallThrough( mob, below );
				return;
			}
			Buff.prolong(mob, Trap.HazardAssistTracker.class, Trap.HazardAssistTracker.DURATION);
			if (frozen){
				//frozen solid, it breaks apart at the bottom whatever its health, and the kill is worth all its EXP.
				//still a chasm death to everything that cares (skeletons don't burst, ghouls don't
				//rise): Mob.die halves the EXP again for a chasm
				final boolean seen = Dungeon.level.heroFOV[mob.pos];
				final MobSprite sprite = (MobSprite) mob.sprite;
				mob.EXP *= 2;
				mob.die( Chasm.class );
				if (sprite != null){
					//the ice block tumbles down and bursts where it vanishes, the way a frozen
					//creature's ice breaks (IceBlock.melt), only harder
					sprite.fall( () -> {
						if (!seen) return;
						PointF at = sprite.center();
						Sample.INSTANCE.play( Assets.Sounds.SHATTER );
						PixelScene.shake( 2, 0.3f );
						Splash.at( at, 0xFFB2D6FF, 14 );
						Splash.at( at, 0xFFFFFFFF, 6 );
					} );
				}
				return;
			} else {
				mob.die( Chasm.class );
			}
		}
		
		if (mob.sprite != null) ((MobSprite)mob.sprite).fall();
	}

	//the depth a monster falling here lands on alive, or -1 when the fall can only kill it: bosses,
	//minibosses, the immovable and friendly folk never leave their level, and nothing falls into a boss
	//floor, out of a branch, or off the deepest cave
	private static int depthBelow( Mob mob ){
		if (Char.hasProp( mob, Char.Property.BOSS ) || Char.hasProp( mob, Char.Property.MINIBOSS )
				|| Char.hasProp( mob, Char.Property.IMMOVABLE ) || mob instanceof NPC
				|| mob.alignment != Char.Alignment.ENEMY || Dungeon.branch != 0) return -1;
		if (Dungeon.level instanceof OverworldLevel){
			return ((OverworldLevel) Dungeon.level).fallDepth( mob.pos );
		}
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel) return -1;
		int below = Dungeon.depth + 1;
		if (Dungeon.depth < 1 || below > 25 || Dungeon.bossLevel( below )) return -1;
		return below;
	}

	//it lives: gone from this level mid-fall, it lands below crippled and bleeding and waits there
	private static void fallThrough( Mob mob, int below ){
		FallenMob fallen = new FallenMob( mob );
		if (Dungeon.level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) Dungeon.level;
			fallen.world = true;
			fallen.wx = ow.worldX() + mob.pos % ow.width();
			fallen.wy = ow.worldY() + mob.pos / ow.width();
		}
		//the landing takes 80% of its maximum health; it had more than that, so it lives
		mob.HP = Math.max( 1, mob.HP - Math.round( SURVIVE_HEALTH * mob.HT ) );
		Buff.prolong( mob, Cripple.class, Cripple.DURATION );
		Buff.affect( mob, Bleeding.class ).set( Math.max( 1, mob.HT / 10f ), Chasm.class );

		Dungeon.level.mobs.remove( mob );
		for (Buff b : mob.buffs()) Actor.remove( b );
		Actor.remove( mob );
		mob.clearTime();
		if (mob.sprite != null){
			((MobSprite) mob.sprite).fall();
			mob.sprite = null;
		}

		ArrayList<FallenMob> list = Dungeon.fallenMobs.get( below );
		if (list == null) Dungeon.fallenMobs.put( below, list = new ArrayList<>() );
		list.add( fallen );
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
