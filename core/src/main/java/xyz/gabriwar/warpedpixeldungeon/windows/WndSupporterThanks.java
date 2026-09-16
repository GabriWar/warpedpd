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

package xyz.gabriwar.warpedpixeldungeon.windows;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SupporterArt;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

/**
 * Said once, right after a subscription goes through. Somebody just chose to pay for a
 * game they could keep playing for free, so the moment gets more than a line in the log,
 * and the more they chose to give, the more the game gives back: a bronze, silver or
 * gold emblem with its halo, and a celebration that grows with the tier, from stars
 * drifting up the screen to confetti and fireworks.
 */
public class WndSupporterThanks extends Window {

	private static final int MARGIN = 4;
	private static final int BTN_HEIGHT = 18;

	private final int tier;

	private Emitter stars;
	private Emitter confetti;
	private boolean fireworks = false;

	public WndSupporterThanks() {
		this( 1, null );
	}

	/**
	 * A subscription just went through. The thank-you is written down before it is
	 * shown: closing the store sheet can resize the surface, and a resize rebuilds the
	 * scene, which would drop a window that was simply added to it. Every scene that
	 * can host it asks showIfPending() when it is built, and the note is cleared only
	 * when the player closes the window.
	 */
	public static void request( int tier, String tierName ){
		WPDSettings.supporterThanks( tier, tierName );
		if (Game.scene() instanceof PixelScene) showIfPending( (PixelScene) Game.scene() );
	}

	public static void showIfPending( PixelScene scene ){
		int tier = WPDSettings.supporterThanksTier();
		if (tier <= 0) return;
		if (showing != null) return;
		scene.addToFront( new WndSupporterThanks( tier, WPDSettings.supporterThanksName() ) );
	}

	//the one on screen, if any; a scene rebuild destroys it, and the rebuilt scene asks again
	private static WndSupporterThanks showing;

	/** tier 1 to 3; tierName is the store's name for it, or null */
	public WndSupporterThanks( int tier, String tierName ) {
		super();
		this.tier = tier = Math.max( 1, Math.min( 3, tier ) );
		showing = this;
		int width = tier == 3 ? 140 : 130;

		//the emblem, its halo behind it, and the title beside it
		Image emblem = new Image( SupporterArt.get() );
		SupporterArt.frame( emblem, SupporterArt.emblem( tier ) );
		Glow halo = new Glow();
		halo.hardlight( SupporterArt.HALO_COLOR[tier] );
		halo.alpha( SupporterArt.HALO_ALPHA[tier] );
		add( halo );
		IconTitle title = new IconTitle( emblem, Messages.get( this, "title_" + tier ) );
		title.setRect( 0, 0, width, 0 );
		add( title );
		halo.x = emblem.x + (emblem.width() - halo.width()) / 2;
		halo.y = emblem.y + (emblem.height() - halo.height()) / 2;
		PixelScene.align( halo );

		//the flare sits behind the emblem so the burst reads as coming from it
		int rays = tier == 3 ? 12 : tier == 2 ? 9 : 7;
		float radius = tier == 3 ? 52 : tier == 2 ? 40 : 32;
		new Flare( rays, radius ).color( 0xFFDD44, true ).show( emblem, 3f + tier ).angularSpeed = 60;
		if (tier >= 2){
			new Flare( 5, radius * 0.6f ).color( 0xFFFFFF, true ).show( emblem, 4f + tier ).angularSpeed = -35;
		}

		float pos = title.bottom() + 2;
		if (tierName != null && !tierName.isEmpty()){
			RenderedTextBlock name = PixelScene.renderTextBlock( Messages.get( this, "tier_line", tierName ), 6 );
			name.hardlight( SupporterArt.RIM_LIGHT[tier] );
			name.maxWidth( width - MARGIN * 2 );
			name.setPos( MARGIN, pos );
			add( name );
			pos = name.bottom() + 1;
		}
		pos = rule( pos + 2, width ) + MARGIN;

		RenderedTextBlock body = PixelScene.renderTextBlock( Messages.get( this, "body_" + tier ), 6 );
		body.maxWidth( width - MARGIN * 2 );
		body.setPos( MARGIN, pos );
		add( body );

		RenderedTextBlock sig = PixelScene.renderTextBlock( Messages.get( this, "signature_" + tier ), 6 );
		sig.hardlight( TITLE_COLOR );
		sig.maxWidth( width - MARGIN * 2 );
		sig.setPos( width - MARGIN - sig.width(), body.bottom() + MARGIN );
		add( sig );

		pos = rule( sig.bottom() + MARGIN, width ) + MARGIN;

		RedButton close = new RedButton( Messages.get( this, "close_" + tier ) ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		close.icon( Icons.get( Icons.CONTROLLER ) );
		close.setRect( MARGIN, pos, width - MARGIN * 2, BTN_HEIGHT );
		add( close );

		resize( width, (int) close.bottom() + MARGIN );

		//parented to the scene rather than the window: the drift crosses the whole
		//screen, and anything added to a window is clipped to it
		stars = new Emitter();
		stars.pos( 0, Camera.main.height + 8, Camera.main.width, 8 );
		stars.pour( RisingStar.FACTORY, tier == 3 ? 0.025f : tier == 2 ? 0.04f : 0.06f );
		Game.scene().add( stars );

		if (tier >= 2){
			//confetti from the top, in the tier's metal and the game's colours
			confetti = new Emitter();
			confetti.pos( 0, -6, Camera.main.width, 4 );
			Confetti.metal = SupporterArt.RIM_LIGHT[tier];
			confetti.pour( Confetti.FACTORY, tier == 3 ? 0.03f : 0.06f );
			Game.scene().add( confetti );
		}

		switch (tier){
			case 1:
				Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
				break;
			case 2:
				Sample.INSTANCE.play( Assets.Sounds.BADGE );
				Sample.INSTANCE.playDelayed( Assets.Sounds.LEVELUP, 0.5f );
				break;
			default:
				Sample.INSTANCE.play( Assets.Sounds.MASTERY );
				Sample.INSTANCE.playDelayed( Assets.Sounds.LEVELUP, 0.8f );
				Sample.INSTANCE.playDelayed( Assets.Sounds.BADGE, 1.6f );
				fireworks = true;
				firework( 0.4f );
				break;
		}
	}

	/** a thin rule across the window in the tier's metal; returns the y under it */
	private float rule( float y, int width ){
		ColorBlock line = new ColorBlock( width - MARGIN * 2, 1, 0xFFFFFFFF );
		line.hardlight( SupporterArt.RIM_SHADE[tier] );
		line.x = MARGIN;
		line.y = y;
		add( line );
		if (tier == 3){
			ColorBlock glint = new ColorBlock( (width - MARGIN * 2) / 3f, 1, 0xFFFFFFFF );
			glint.hardlight( SupporterArt.RIM_LIGHT[tier] );
			glint.x = MARGIN + (width - MARGIN * 2) / 3f;
			glint.y = y;
			add( glint );
		}
		return y + 1;
	}

	/** a star burst somewhere in the upper screen, then another after a pause, while the window is open */
	private void firework( float delay ){
		Game.scene().add( new Delayer( delay ) {
			@Override
			protected void onComplete() {
				super.onComplete();
				if (!fireworks) return;
				Emitter burst = new Emitter();
				burst.pos( Random.Float( Camera.main.width * 0.1f, Camera.main.width * 0.9f ),
						Random.Float( Camera.main.height * 0.08f, Camera.main.height * 0.45f ) );
				burst.autoKill = true;
				Game.scene().add( burst );
				burst.burst( Speck.factory( Speck.STAR, true ), Random.IntRange( 14, 24 ) );
				firework( Random.Float( 0.5f, 1.1f ) );
			}
		} );
	}

	@Override
	public void destroy() {
		if (showing == this) showing = null;
		super.destroy();
	}

	private float shownFor = 0;

	@Override
	public synchronized void update() {
		super.update();
		//the note is owed only until the window has stood on a scene for a moment: the
		//rebuild that would drop it comes within a frame of the store sheet closing, so
		//two seconds on screen means it was seen, whatever happens to the scene after
		if (shownFor <= 2f && (shownFor += Game.elapsed) > 2f) settle();
	}

	private void settle(){
		if (WPDSettings.supporterThanksTier() == tier) WPDSettings.supporterThanks( 0, null );
	}

	@Override
	public void hide() {
		settle();
		fireworks = false;
		stop( stars );
		stop( confetti );
		stars = confetti = null;
		super.hide();
	}

	/** stop making new ones and let the ones in flight finish, so closing does not blink the screen clear */
	private static void stop( final Emitter e ){
		if (e == null) return;
		e.on = false;
		Game.scene().add( new Delayer( 5f ) {
			@Override
			protected void onComplete() {
				e.killAndErase();
				super.onComplete();
			}
		} );
	}

	/** the halo is added to what is under it, so it reads as light rather than paint */
	private static class Glow extends Image {

		Glow(){
			super( SupporterArt.get() );
			SupporterArt.frame( this, SupporterArt.HALO );
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	/** Slow, wandering, upward: a celebration rather than a spell effect. */
	public static class RisingStar extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit(Emitter emitter, int index, float x, float y) {
				((RisingStar) emitter.recycle(RisingStar.class)).reset(x, y);
			}
		};

		public RisingStar() {
			super();
			lifespan = 4f;
			acc.set(0, -8);
		}

		public void reset(float x, float y) {
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			size(Random.Float(1f, 2.5f));
			speed.set(Random.Float(-12, 12), Random.Float(-30, -14));
			//warm gold through to a pale white, so the drift does not look like one repeated sprite
			color(Random.oneOf(0xFFDD44, 0xFFC400, 0xFFF3B0, 0xFFFFFF));
		}

		@Override
		public void update() {
			super.update();
			//bright at once, fading out over the last third of the life
			am = left > lifespan / 3f ? 1f : left / (lifespan / 3f);
		}
	}

	/** Falling flakes that sway as they drop: gold, white, red, and the tier's metal. */
	public static class Confetti extends PixelParticle {

		static int metal = 0xFFDD44;

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit(Emitter emitter, int index, float x, float y) {
				((Confetti) emitter.recycle(Confetti.class)).reset(x, y);
			}
		};

		private float phase;
		private float sway;

		public Confetti() {
			super();
			lifespan = 5f;
		}

		public void reset(float x, float y) {
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			size(Random.Float(1.5f, 3f));
			speed.set(0, Random.Float(18, 34));
			phase = Random.Float(6.3f);
			sway = Random.Float(8, 18);
			angle = Random.Float(360);
			angularSpeed = Random.Float(-180, 180);
			color(Random.oneOf(0xFFDD44, 0xFFFFFF, 0xE04848, metal, 0x66CCFF));
		}

		@Override
		public void update() {
			speed.x = sway * (float)Math.sin((lifespan - left) * 3f + phase);
			super.update();
			am = left > 1f ? 1f : left;
		}
	}
}
