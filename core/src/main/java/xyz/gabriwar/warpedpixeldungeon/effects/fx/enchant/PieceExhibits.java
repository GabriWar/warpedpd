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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sprites.MissileSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

/**
 * The new pieces' page of the enchantments' gallery (EnchantArea.exhibits): each restaged by hand
 * on puppets through PieceFx's and SenseFx's own entries (nothing sent, no real roll, nothing of
 * the game changed), with the game's missiles flying where a throw is shown. Captions in the log.
 * The puppets stand where the core's exhibits leave room: the room's south-east corner, its north
 * rows and the floor east of the skeleton.
 */
public final class PieceExhibits {

	private PieceExhibits(){}

	public static void exhibits( FxGallery.Page page ){

		page.add( "crit, lifesteal, reload and a spared round", 6.5f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( 2, 4 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( 3, 4 ) );
			final FxStage.Puppet gunner = s.puppet( Gnoll.class, s.cell( 6, 4 ) );
			if (striker == null || foe == null || gunner == null) return;
			s.after( 0.3f, () -> {
				GLog.i( "  a crit: the small red X, one stroke then the other, gone in under half a second" );
				blow( striker, foe );
				PieceFx.runCrit( striker.sprite, foe.sprite, true, false, false );
			} );
			s.after( 1.4f, () -> {
				GLog.i( "  a crit on a blow Blazing stamped: the star in the fire's light, the X still red" );
				EnchantFx.stage( EnchantLooks.of( Blazing.class ), striker.sprite, foe.sprite, EnchantFx.MID, 0, 0 );
				blow( striker, foe );
				PieceFx.runCrit( striker.sprite, foe.sprite, true, false, false );
			} );
			s.after( 2.6f, () -> {
				GLog.i( "  a lifesteal: a thin thread of three beads home" );
				blow( striker, foe );
				PieceFx.runLifesteal( striker.sprite, foe.sprite, false );
			} );
			s.after( 3.7f, () -> {
				GLog.i( "  a free reload: a brass click, shells kicked out" );
				PieceFx.runFreeReload( gunner.sprite, false );
			} );
			s.after( 4.7f, () -> {
				GLog.i( "  a spared arrow, then a spared round, back into the shooter" );
				PieceFx.runSpare( gunner.sprite, ItemSpriteSheet.NORMAL_ARROW, false );
			} );
			s.after( 5.5f, () -> PieceFx.runSpare( gunner.sprite, ItemSpriteSheet.MINI_GUN_AMMO, false ) );
		} );

		page.add( "heave, the shockwave and the scythe's sweep", 7f, s -> {
			final FxStage.Puppet thrower = s.puppet( Gnoll.class, s.cell( -6, -4 ) );
			final FxStage.Puppet target = s.puppet( Rat.class, s.cell( -2, -4 ) );
			final FxStage.Puppet[] around = {
					s.puppet( Rat.class, s.cell( -1, -4 ) ), s.puppet( Rat.class, s.cell( -2, -5 ) ),
					s.puppet( Rat.class, s.cell( -2, -3 ) ), s.puppet( Rat.class, s.cell( -1, -5 ) ) };
			final FxStage.Puppet reaper = s.puppet( Gnoll.class, s.cell( 1, -4 ) );
			final FxStage.Puppet[] swept = {
					s.puppet( Rat.class, s.cell( 2, -4 ) ), s.puppet( Rat.class, s.cell( 0, -3 ) ),
					s.puppet( Rat.class, s.cell( 1, -3 ) ), s.puppet( Rat.class, s.cell( 2, -3 ) ) };
			if (thrower == null || target == null || reaper == null) return;
			final Item javelin = new Javelin();
			s.after( 0.3f, () -> {
				GLog.i( "  a heave armed: a ring closing on the hand, motes drawn in, a mote circling it" );
				PieceFx.runCharge( thrower.sprite, null, null, false );
			} );
			s.after( 1.6f, () -> {
				GLog.i( "  let go: a bloom at the hand, the javelin's ghosts; it lands harder, and an alpha's shockwave rolls out" );
				PieceFx.runRelease( thrower.sprite, javelin.image(), target.ch.pos, null, false );
				((MissileSprite) thrower.sprite.parent.recycle( MissileSprite.class )).reset( thrower.sprite, target.ch.pos, javelin,
						new Callback() {
							@Override
							public void call(){
								if (target.sprite.parent == null) return;
								HitFx.hit( target.sprite, thrower.sprite.x + 8, thrower.sprite.y + 8, 30, HitFx.STRONG, false );
								PieceFx.runHeave( thrower.sprite, target.sprite, null, true, false, false );
								PieceFx.runShockwave( thrower.sprite, target.sprite, sprites( around ), null, true, false, false );
							}
						} );
			} );
			s.after( 4.2f, () -> {
				GLog.i( "  a scythe's sweep round the wielder through the foes it touched, grass flung" );
				reaper.sprite.attack( swept[0].ch.pos );
				HitFx.hit( swept[0].sprite, reaper.sprite.x + 8, reaper.sprite.y + 8, 5, HitFx.SLASH, false );
				CharSprite[] rest = sprites( new FxStage.Puppet[]{ swept[1], swept[2], swept[3] } );
				PieceFx.runSweep( reaper.sprite, swept[0].sprite, rest, null, true, false );
			} );
		} );

		page.add( "a keen throw comes back; the boomerang bites and returns", 6.5f, s -> {
			final FxStage.Puppet keen = s.puppet( Gnoll.class, s.cell( 1, 5 ) );
			final FxStage.Puppet mark = s.puppet( Rat.class, s.cell( 5, 5 ) );
			final FxStage.Puppet arc = s.puppet( Gnoll.class, s.cell( 2, 3 ) );
			final FxStage.Puppet[] line = {
					s.puppet( Rat.class, s.cell( 3, 3 ) ), s.puppet( Rat.class, s.cell( 4, 3 ) ), s.puppet( Rat.class, s.cell( 5, 3 ) ) };
			if (keen == null || mark == null || arc == null) return;
			final Item knife = new ThrowingKnife(), boomerang = new HeavyBoomerang();
			final int landing = s.cell( 6, 3 );
			s.after( 0.3f, () -> {
				GLog.i( "  a keen throw flies back to the hand, and is caught" );
				((MissileSprite) keen.sprite.parent.recycle( MissileSprite.class )).reset( keen.sprite, mark.ch.pos, knife,
						new Callback() {
							@Override
							public void call(){
								HitFx.hit( mark.sprite, keen.sprite.x + 8, keen.sprite.y + 8, 5, HitFx.STAB, false );
								PieceFx.runKeenBack( keen.sprite, mark.ch.pos, knife.image(), null, false );
								((MissileSprite) keen.sprite.parent.recycle( MissileSprite.class )).reset( mark.ch.pos, keen.sprite, knife,
										new Callback() {
											@Override
											public void call(){
												PieceFx.runCatch( keen.sprite, false );
											}
										} );
							}
						} );
			} );
			s.after( 2.4f, () -> {
				GLog.i( "  an Arc boomerang bites through the line, ringing higher each time" );
				((MissileSprite) arc.sprite.parent.recycle( MissileSprite.class )).reset( arc.sprite, landing, boomerang,
						new Callback() {
							@Override
							public void call(){
								for (int i = 0; i < line.length; i++){
									if (line[i] != null) PieceFx.runBite( arc.sprite, line[i].sprite, i, null, true, false );
								}
							}
						} );
			} );
			s.after( 4.0f, () -> {
				GLog.i( "  its return telegraphed, its way home struck, and the catch" );
				PieceFx.runTelegraph( landing, arc.ch.pos, arc.sprite, null, false );
				((MissileSprite) arc.sprite.parent.recycle( MissileSprite.class )).reset( landing, arc.sprite, boomerang,
						new Callback() {
							@Override
							public void call(){
								for (int i = line.length - 1, k = 0; i >= 0; i--, k++){
									if (line[i] != null) PieceFx.runBite( arc.sprite, line[i].sprite, k, null, true, false );
								}
								PieceFx.runCatch( arc.sprite, false );
							}
						} );
			} );
		} );

		page.add( "sensing through walls, the gun's reach, a second seed", 6f, s -> {
			final FxStage.Puppet[] hidden = {
					s.puppet( Rat.class, s.cell( -5, 4 ) ), s.puppet( Rat.class, s.cell( -5, 3 ) ), s.puppet( Rat.class, s.cell( -6, 5 ) ) };
			final FxStage.Puppet gunner = s.puppet( Gnoll.class, s.cell( 2, 4 ) );
			s.after( 0.3f, () -> {
				GLog.i( "  an Ambush blade senses foes behind the wall: one ping, a ring under each, an exclamation" );
				CharSprite from = Dungeon.hero != null ? Dungeon.hero.sprite : null;
				SenseFx.stage( from, sprites( hidden ), 2.6f );
			} );
			s.after( 3.2f, () -> {
				GLog.i( "  a flamethrower and a laser reaching past their own range: a mark at the far tile" );
				if (gunner == null) return;
				int tip = s.cell( 7, 4 );
				if (tip != -1) PieceFx.runReach( gunner.sprite, tip, PieceFx.FLAME_TIP, true, false );
			} );
			s.after( 3.9f, () -> {
				int tip = s.cell( 7, 5 );
				if (tip != -1) PieceFx.runReach( gunner == null ? null : gunner.sprite, tip, PieceFx.BEAM_TIP, true, false );
			} );
			s.after( 4.6f, () -> {
				GLog.i( "  a reaper's cut gives a second seed" );
				int[] grass = s.grass();
				PieceFx.runSeed( Dungeon.hero, grass[2], true, false );
			} );
		} );
	}

	//a striker's blow on a foe: its swing and its star
	private static void blow( FxStage.Puppet a, FxStage.Puppet d ){
		a.sprite.attack( d.ch.pos );
		HitFx.hit( d.sprite, a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, 5, HitFx.SLASH, false );
	}

	//the sprites of the puppets there are
	static CharSprite[] sprites( FxStage.Puppet[] ps ){
		int k = 0;
		CharSprite[] out = new CharSprite[ps.length];
		for (FxStage.Puppet p : ps) if (p != null) out[k++] = p.sprite;
		return java.util.Arrays.copyOf( out, k );
	}
}
