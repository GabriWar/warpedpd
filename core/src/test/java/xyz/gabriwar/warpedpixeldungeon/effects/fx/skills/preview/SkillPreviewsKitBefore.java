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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Bundle;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The kit-lance package's moments as the game drew them before it (master 72b40617b): each skill's
 * visual calls of that code, restaged on the dump's stage (SkillPreviewDump) - CellEmitter's bursts
 * through the scene's stand-in emitters. Compiles against that code and this, so its "before" side
 * can be dumped from a checkout of the old code (tools/fxpreview/README.md).
 */
public final class SkillPreviewsKitBefore {

	private SkillPreviewsKitBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		//Impaling Thrust: the lane of stars, the streak, the upright spear icon sliding the lane
		r.entry( "impaling-thrust-r3", s -> thrust( s, at( 6, 4 ), 3 ) ).rat( 4, 4 ).rat( 6, 4 );
		r.entry( "impaling-thrust-r5-diagonal", s -> thrust( s, at( 6, 6 ), 5 ) ).hero( 1, 1 ).view( 0, 0, 9, 8 )
				.rat( 2, 2 ).rat( 4, 4 ).rat( 5, 5 );
		r.entry( "impaling-thrust-empty-lane", s -> thrust( s, at( 7, 4 ), 4 ) ).rat( 7, 4 );
		//the +3's lances: every other cell of the lane rises a spear, then all of them
		r.entry( "lances-turn1", s -> lances( s, false ) ).rat( 5, 4 ).rat( 7, 4 );
		r.entry( "lances-turn2-full", s -> lances( s, true ) ).rat( 5, 4 ).rat( 7, 4 );
		//War Cry: the alarm's red brackets on the hero and on every enemy in view
		r.entry( "war-cry-3-enemies", SkillPreviewsKitBefore::warCry ).rat( 4, 4 ).rat( 5, 2 ).rat( 6, 6 );
		SkillPreviewDump.Entry crowd = r.entry( "war-cry-crowd-12", SkillPreviewsKitBefore::warCry );
		for (int[] c : CROWD) crowd.rat( c[0], c[1] );
		//Creeping Dread: a curse's shadow and the brackets
		r.entry( "creeping-dread-proc", s -> dread( s, false ) ).rat( 4, 4 );
		r.entry( "creeping-dread-spill", s -> dread( s, true ) ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		//the spectacles, as their callers show them
		r.entry( "spect-hammer", s -> SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, at( 6, 4 ) ) ).rat( 6, 4 );
		r.entry( "spect-spire", s -> SkillSpectacleFX.show( SkillSpectacleFX.SPIRE, at( 6, 4 ) ) ).rat( 6, 4 );
		r.entry( "spect-saber", s -> SkillSpectacleFX.fly( SkillSpectacleFX.SABER, s.hero.pos, at( 6, 4 ), 0, 0.4f ) ).rat( 6, 4 );
		r.entry( "spect-shadow", s -> SkillSpectacleFX.fly( SkillSpectacleFX.SHADOW, at( 4, 3 ), at( 6, 4 ), 0, 0.45f ) ).rat( 6, 4 );
		r.entry( "spect-jaw", s -> SkillSpectacleFX.show( SkillSpectacleFX.JAW, at( 6, 4 ) ) ).rat( 6, 4 );
		r.entry( "spect-wings", s -> SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos ) );
		r.entry( "spect-thorn", s -> {
			for (int c : new int[]{ at( 5, 3 ), at( 6, 5 ), at( 4, 5 ) }) SkillSpectacleFX.fly( SkillSpectacleFX.THORN, at( 5, 4 ), c, 0, 0.45f );
		} ).rat( 5, 3 ).rat( 6, 5 ).rat( 4, 5 );
		r.entry( "spect-raptor", s -> SkillSpectacleFX.show( SkillSpectacleFX.RAPTOR, at( 6, 4 ) ) ).rat( 6, 4 );
		//the fields
		r.entry( "field-arrows", s -> field( SkillField.ARROWS, around( 6, 4 ) ) ).seconds( 2.5f );
		r.entry( "field-soul", s -> field( SkillField.SOUL, new int[]{ at( 6, 4 ) } ) ).seconds( 2.5f );
		r.entry( "field-smoke", s -> field( SkillField.SMOKE, around( 6, 4 ) ) ).seconds( 2.5f );
		//a heavy landing (Leap, Smash, Earthshatter): the hammer standing in the crack
		r.entry( "impact-heavy", s -> WarriorImpactFX.show( at( 6, 4 ), true ) );
		//things falling out of the sky: items
		r.entry( "rain-meteor", s -> SkillFX.rain( at( 5, 7 ), new Bomb(), 1, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-spirit-arrow", s -> SkillFX.rain( at( 5, 7 ), new Dart(), 3, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-holy-blade", s -> SkillFX.rain( at( 5, 7 ), new Sword(), 1, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-weight", s -> SkillFX.rain( at( 5, 7 ), new StoneOfBlast(), 1, null ) ).view( 1, 0, 9, 9 );
		//thrown: items
		r.entry( "streak-coin", s -> SkillFX.streak( s.h, at( 7, 4 ), new Gold(), null ) ).rat( 7, 4 );
		r.entry( "streak-feather", s -> SkillFX.streak( at( 5, 2 ), at( 7, 5 ), new Dart(), null ) ).rat( 5, 2 ).rat( 7, 5 );
		r.entry( "streak-pellet", s -> SkillFX.streak( s.h, at( 7, 5 ), new Bomb(), null ) );
		r.entry( "streak-snare", s -> SkillFX.streak( s.h, at( 7, 4 ), new Item(){ { image = ItemSpriteSheet.TRAP_MECHANISM; } }, null ) );
		//the flights: wands' and mobs' bolts, a healing beam
		r.entry( "bolt-soul", s -> bolt( s, MagicMissile.SHADOW, false ) ).rat( 7, 4 );
		r.entry( "bolt-mana", s -> bolt( s, MagicMissile.SHADOW, true ) ).rat( 7, 4 );
		r.entry( "bolt-hex", s -> bolt( s, MagicMissile.SHADOW, false ) ).rat( 7, 4 );
		r.entry( "bolt-ember", s -> bolt( s, MagicMissile.FIRE, false ) ).rat( 7, 4 );
		r.entry( "bolt-holy", s -> bolt( s, MagicMissile.LIGHT_MISSILE, false ) ).rat( 7, 4 );
		r.entry( "bolt-heal", s -> s.h.parent.add( new Beam.HealthRay( s.h.center(), s.d[0].center() ) ) ).rat( 7, 4 );
		r.entry( "bolt-spark", s -> bolt( s, MagicMissile.MAGIC_MISSILE, false ) ).rat( 7, 4 );
		r.entry( "bolt-arcane", s -> bolt( s, MagicMissile.MAGIC_MISSILE, false ) ).rat( 7, 4 );
		r.entry( "bolt-spirit", s -> bolt( s, MagicMissile.WARD, false ) ).rat( 7, 4 );
		//what each of the kit's bursts replaces, in the same places
		r.entry( "bursts-sheet", s -> {
			for (int i = 0; i < REPLACED.length; i++) s.center( SHEET[i] ).burst( REPLACED[i], 4 );
		} ).view( 0, 0, 11, 9 ).hero( 5, 4 ).seconds( 1.5f );
		r.entry( "pillar", s -> pillar( s, at( 6, 4 ), 0xFFDD88 ) ).view( 1, 0, 9, 9 );
	}

	/** Twelve enemies about the hero. */
	static final int[][] CROWD = { { 1, 2 }, { 2, 2 }, { 4, 2 }, { 5, 2 }, { 1, 6 }, { 2, 6 }, { 4, 6 }, { 5, 6 },
			{ 5, 4 }, { 6, 3 }, { 6, 5 }, { 1, 4 } };

	/** The bursts' sheet: 23 cells in rows of six, two apart (x 0-10, y 1-7). */
	static final int[] SHEET = new int[23];
	static {
		for (int i = 0; i < SHEET.length; i++) SHEET[i] = at( 2 * (i % 6), 1 + 2 * (i / 6) );
	}

	/** Today's factory each of the kit's bursts takes the place of, in SkillBursts' order. */
	static final Emitter.Factory[] REPLACED = {
			ShadowParticle.CURSE,                 //DREAD
			Speck.factory( Speck.SCREAM ),        //TREMBLE
			Speck.factory( Speck.SCREAM ),        //ROAR
			Speck.factory( Speck.STAR ),          //GRIT
			Speck.factory( Speck.ROCK ),          //STONE_CHIP
			SparkParticle.FACTORY,                //METAL_SPARK
			SparkParticle.FACTORY,                //STEEL_GLINT
			SparkParticle.FACTORY,                //ARCANE_SPARK
			SparkParticle.FACTORY,                //STORM_SPARK
			ShadowParticle.UP,                    //INK_DRIFT
			FlameParticle.FACTORY,                //EMBER_MOTE
			SnowParticle.FACTORY,                 //FROST_GLINT
			Speck.factory( Speck.WOOL ),          //FEATHER
			Speck.factory( Speck.STAR ),          //FLOURISH
			Speck.factory( Speck.COIN ),          //GOLD_GLINT
			Speck.factory( Speck.NOTE ),          //VERSE
			ShadowParticle.UP,                    //SOUL_WISP
			FlameParticle.FACTORY,                //HOLY_MOTE
			Speck.factory( Speck.UP ),            //BREATH
			Speck.factory( Speck.HEART ),         //HEARTBEAT
			SmokeParticle.FACTORY,                //FUSE
			Speck.factory( Speck.TOXIC ),         //ICHOR
			Speck.factory( Speck.STENCH ) };      //SCENT

	// ------------------------------------------------------------------ today's moments

	//ImpalingThrust.thrust's pictures: the lane's steps 0.035 s apart (stars, and on a body its
	//flash, its wound and the stab), the streak, the spectacle's lance and the hero's stars
	static void thrust( SkillPreviewDump.Scene s, int end, int reach ){
		FxTimeline lane = FxTimeline.start();
		int w = SkillPreviewDump.W;
		int hx = s.hero.pos % w, hy = s.hero.pos / w, ex = end % w, ey = end / w;
		int i = 0, pierced = 0;
		for (int k = 1; k <= reach; k++){
			final int c = at( hx + Integer.signum( ex - hx ) * k, hy + Integer.signum( ey - hy ) * k );
			final CharSprite body = bodyAt( s, c );
			final float pitch = 1.0f + 0.1f * pierced;
			lane.at( 0.035f * i++, () -> {
				s.center( c ).burst( Speck.factory( Speck.STAR ), 2 );
				if (body != null){
					body.flash();
					Wound.hit( body.ch );
					SpatialSound.play( Assets.Sounds.HIT_STAB, body.ch, 0.9f, pitch );
				}
			} );
			if (body != null) pierced++;
		}
		StreakFX.show( s.hero.pos, end, 0xFFFFFF, 0.45f, 0.3f + 0.03f * reach );
		SkillSpectacleFX.fly( SkillSpectacleFX.LANCE, s.hero.pos, end, 0, .4f );
		s.h.emitter().burst( Speck.factory( Speck.STAR ), 3 );
	}

	//SkillSequence.LANCES: every other cell of a five-cell lane (turn one), or all of them (the last turn at rank 3)
	static void lances( SkillPreviewDump.Scene s, boolean full ){
		for (int i = 0; i < 5; i++){
			if (full || i % 2 == 0) SkillSpectacleFX.show( SkillSpectacleFX.LANCE, at( 4 + i, 4 ) );
		}
	}

	//WarCry.execute's pictures
	static void warCry( SkillPreviewDump.Scene s ){
		for (CharSprite m : s.d) m.emitter().burst( Speck.factory( Speck.SCREAM ), 3 );
		s.h.emitter().burst( Speck.factory( Speck.SCREAM ), 4 );
	}

	//CreepingDread.onHitProc's pictures, and at mastery the spill onto its neighbours
	static void dread( SkillPreviewDump.Scene s, boolean spill ){
		CharSprite enemy = s.d[0];
		s.get( enemy.ch.pos ).burst( ShadowParticle.CURSE, 5 );
		enemy.emitter().burst( Speck.factory( Speck.SCREAM ), 1 );
		if (!spill) return;
		FxTimeline t = FxTimeline.start();
		for (int k = 1; k < s.d.length; k++){
			final CharSprite next = s.d[k];
			t.at( 0.1f + 0.08f * (k - 1), () -> {
				s.get( next.ch.pos ).burst( ShadowParticle.CURSE, 3 );
				next.emitter().burst( Speck.factory( Speck.SCREAM ), 1 );
			} );
		}
	}

	//SkillFX.pillar: a light ray down onto the cell, a flare at its foot, the light burst
	static void pillar( SkillPreviewDump.Scene s, int cell, int color ){
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PointF top = new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 );
		s.h.parent.add( new Beam.LightRay( top, foot ) );
		PixelFlare.show( foot.x, foot.y, color, PixelFlare.L, 0.8f );
		s.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
	}

	//a bolt from the hero to the first dummy (or back)
	static void bolt( SkillPreviewDump.Scene s, int type, boolean home ){
		MagicMissile m = (MagicMissile) s.h.parent.recycle( MagicMissile.class );
		if (home) m.reset( type, s.d[0], s.h, null );
		else m.reset( type, s.h, s.d[0], null );
	}

	// ------------------------------------------------------------------ helpers

	static CharSprite bodyAt( SkillPreviewDump.Scene s, int cell ){
		for (CharSprite d : s.d) if (d.ch.pos == cell) return d;
		return null;
	}

	static int[] around( int x, int y ){
		int[] out = new int[9];
		int k = 0;
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) out[k++] = at( x + dx, y + dy );
		return out;
	}

	//a field of a kind on cells, as SkillField.place makes one (its floor this one)
	static void field( int kind, int[] cells ){
		Bundle b = new Bundle();
		b.put( "kind", kind );
		b.put( "rank", 1 );
		b.put( "remaining", 5 );
		b.put( "age", 0 );
		b.put( "cells", cells );
		b.put( "depth", Dungeon.depth );
		b.put( "branch", Dungeon.branch );
		b.put( "origin", cells[0] );
		SkillField f = new SkillField();
		f.restoreFromBundle( b );
		SkillFieldFX.show( new SkillFieldFX( f ) );
	}
}
