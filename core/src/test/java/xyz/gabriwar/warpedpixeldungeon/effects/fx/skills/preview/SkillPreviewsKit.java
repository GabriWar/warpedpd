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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LanceFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ShoutFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The kit-lance package's moments as the game draws them now: the same stages as
 * SkillPreviewsKitBefore, each skill's visual calls of this code restaged (CellEmitter's bursts
 * through the scene's stand-in emitters).
 */
public final class SkillPreviewsKit {

	private SkillPreviewsKit(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "impaling-thrust-r3", s -> thrust( s, at( 6, 4 ), 3 ) ).rat( 4, 4 ).rat( 6, 4 );
		r.entry( "impaling-thrust-r5-diagonal", s -> thrust( s, at( 6, 6 ), 5 ) ).hero( 1, 1 ).view( 0, 0, 9, 8 )
				.rat( 2, 2 ).rat( 4, 4 ).rat( 5, 5 );
		r.entry( "impaling-thrust-empty-lane", s -> thrust( s, at( 7, 4 ), 4 ) ).rat( 7, 4 );
		r.entry( "lances-turn1", s -> lances( s, false ) ).rat( 5, 4 ).rat( 7, 4 );
		r.entry( "lances-turn2-full", s -> lances( s, true ) ).rat( 5, 4 ).rat( 7, 4 );
		r.entry( "war-cry-3-enemies", SkillPreviewsKit::warCry ).rat( 4, 4 ).rat( 5, 2 ).rat( 6, 6 );
		SkillPreviewDump.Entry crowd = r.entry( "war-cry-crowd-12", SkillPreviewsKit::warCry );
		for (int[] c : SkillPreviewsKitBefore.CROWD) crowd.rat( c[0], c[1] );
		r.entry( "creeping-dread-proc", s -> dread( s, false ) ).rat( 4, 4 );
		r.entry( "creeping-dread-spill", s -> dread( s, true ) ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
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
		r.entry( "field-arrows", s -> SkillPreviewsKitBefore.field( SkillField.ARROWS, SkillPreviewsKitBefore.around( 6, 4 ) ) ).seconds( 2.5f );
		r.entry( "field-soul", s -> SkillPreviewsKitBefore.field( SkillField.SOUL, new int[]{ at( 6, 4 ) } ) ).seconds( 2.5f );
		r.entry( "field-smoke", s -> SkillPreviewsKitBefore.field( SkillField.SMOKE, SkillPreviewsKitBefore.around( 6, 4 ) ) ).seconds( 2.5f );
		r.entry( "impact-heavy", s -> WarriorImpactFX.show( at( 6, 4 ), true ) );
		r.entry( "rain-meteor", s -> SkillFX.rain( at( 5, 7 ), new Bomb(), SkillFX.Fall.METEOR, 1, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-spirit-arrow", s -> SkillFX.rain( at( 5, 7 ), new Dart(), SkillFX.Fall.SPIRIT_ARROW, 3, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-holy-blade", s -> SkillFX.rain( at( 5, 7 ), new Sword(), SkillFX.Fall.HOLY_BLADE, 1, null ) ).view( 1, 0, 9, 9 );
		r.entry( "rain-weight", s -> SkillFX.rain( at( 5, 7 ), new StoneOfBlast(), SkillFX.Fall.WEIGHT, 1, null ) ).view( 1, 0, 9, 9 );
		r.entry( "streak-coin", s -> SkillFX.streak( s.h, at( 7, 4 ), SkillFX.Streak.COIN, null ) ).rat( 7, 4 );
		r.entry( "streak-feather", s -> SkillFX.streak( at( 5, 2 ), at( 7, 5 ), SkillFX.Streak.FEATHER_DART, null ) ).rat( 5, 2 ).rat( 7, 5 );
		r.entry( "streak-pellet", s -> SkillFX.streak( s.h, at( 7, 5 ), SkillFX.Streak.PELLET, null ) );
		r.entry( "streak-snare", s -> SkillFX.streak( s.h, at( 7, 4 ), SkillFX.Streak.SNARE, null ) );
		r.entry( "bolt-soul", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SOUL, null ) ).rat( 7, 4 );
		r.entry( "bolt-mana", s -> SkillBolt.fly( s.h.parent, s.d[0], s.h, SkillBolt.MANA, null ) ).rat( 7, 4 );
		r.entry( "bolt-hex", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.HEX, null ) ).rat( 7, 4 );
		r.entry( "bolt-ember", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.EMBER, null ) ).rat( 7, 4 );
		r.entry( "bolt-holy", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.HOLY, null ) ).rat( 7, 4 );
		r.entry( "bolt-heal", s -> SkillBolt.mote( s.h, s.d[0], SkillBolt.HEAL, null ) ).rat( 7, 4 );
		r.entry( "bolt-spark", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SPARK, null ) ).rat( 7, 4 );
		r.entry( "bolt-arcane", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.ARCANE, null ) ).rat( 7, 4 );
		r.entry( "bolt-spirit", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SPIRIT, null ) ).rat( 7, 4 );
		r.entry( "bursts-sheet", s -> {
			for (int i = 0; i < SkillBursts.ALL.length; i++){
				s.center( SkillPreviewsKitBefore.SHEET[i] ).burst( (Emitter.Factory) SkillBursts.ALL[i][1], 4 );
			}
		} ).view( 0, 0, 11, 9 ).hero( 5, 4 ).seconds( 1.5f );
		r.entry( "pillar", s -> pillar( s, at( 6, 4 ), 0xFFDD88 ) ).view( 1, 0, 9, 9 );
	}

	// ------------------------------------------------------------------ the moments now

	//ImpalingThrust.thrust's pictures: the lance from his hand, the lane's steps LanceFX.STEP apart
	//(grit, and on a body its flash, its wound, the stab and the pierce), his flourish
	static void thrust( SkillPreviewDump.Scene s, int end, int reach ){
		int w = SkillPreviewDump.W;
		int hx = s.hero.pos % w, hy = s.hero.pos / w, ex = end % w, ey = end / w;
		final float angle = (float) Math.toDegrees( Math.atan2( ey - hy, ex - hx ) );
		FxTimeline lane = FxTimeline.start();
		int i = 0, pierced = 0;
		for (int k = 1; k <= reach; k++){
			final int c = at( hx + Integer.signum( ex - hx ) * k, hy + Integer.signum( ey - hy ) * k );
			final CharSprite body = SkillPreviewsKitBefore.bodyAt( s, c );
			final float pitch = 1.0f + 0.1f * pierced;
			lane.at( LanceFX.STEP * i++, () -> {
				s.center( c ).burst( SkillBursts.GRIT, 2 );
				if (body != null){
					body.flash();
					Wound.hit( body.ch );
					SpatialSound.play( Assets.Sounds.HIT_STAB, body.ch, 0.9f, pitch );
					LanceFX.pierce( body, angle, LanceFX.STEEL );
				}
			} );
			if (body != null) pierced++;
		}
		LanceFX.thrust( s.hero.pos, end, reach, LanceFX.STEEL, s.h );
		s.h.emitter().burst( SkillBursts.FLOURISH, 3 );
	}

	//SkillSequence.LANCES: a ghost thrust over every other cell of a five-cell lane, rippling out
	//0.04 s a cell (turn one), or over all of them (the last turn at rank 3)
	static void lances( SkillPreviewDump.Scene s, boolean full ){
		for (int i = 0; i < 5; i++){
			if (full || i % 2 == 0) SkillSpectacleFX.fly( SkillSpectacleFX.LANCE, s.hero.pos, at( 4 + i, 4 ), 0.04f * i, 0.3f );
		}
	}

	//WarCry.execute's pictures
	static void warCry( SkillPreviewDump.Scene s ){
		for (CharSprite m : s.d){
			m.emitter().burst( SkillBursts.TREMBLE, 3 );
			ShoutFX.flinch( m, SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), ShoutFX.MARTIAL,
					Math.min( 0.3f, 0.05f + 0.03f * Dungeon.level.distance( s.hero.pos, m.ch.pos ) ) );
		}
		s.h.emitter().burst( SkillBursts.ROAR, 4 );
		ShoutFX.roar( s.h, ShoutFX.MARTIAL, 72f );
	}

	//CreepingDread.onHitProc's pictures, and at mastery the spill onto its neighbours
	static void dread( SkillPreviewDump.Scene s, boolean spill ){
		CharSprite enemy = s.d[0];
		s.get( enemy.ch.pos ).burst( SkillBursts.DREAD, 5 );
		enemy.emitter().burst( SkillBursts.TREMBLE, 1 );
		ShoutFX.dread( enemy, false, 0 );
		if (!spill) return;
		FxTimeline t = FxTimeline.start();
		for (int k = 1; k < s.d.length; k++){
			final CharSprite next = s.d[k];
			final int from = enemy.ch.pos;
			t.at( 0.1f + 0.08f * (k - 1), () -> {
				s.get( next.ch.pos ).burst( SkillBursts.DREAD, 3 );
				next.emitter().burst( SkillBursts.TREMBLE, 1 );
				ShoutFX.creep( from, next.ch.pos, 0 );
				ShoutFX.dread( next, true, 0.08f );
			} );
		}
	}

	//SkillFX.pillar: a column rising from the cell, a flare at its foot, the light burst
	static void pillar( SkillPreviewDump.Scene s, int cell, int color ){
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PillarRiseFX.show( cell, color, null );
		PixelFlare.show( foot.x, foot.y, color, PixelFlare.L, 0.8f );
		s.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
	}
}
