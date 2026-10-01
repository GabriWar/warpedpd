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

package xyz.gabriwar.warpedpixeldungeon.scenes;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.ShadowBox;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.LostBackpack;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.GameLog;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.windows.WndError;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.BArray;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;
import com.watabou.utils.Signal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class InterlevelScene extends PixelScene {
	
	//slow fade on entering a new region
	private static final float SLOW_FADE = 1f; //.33 in, 1.33 steady, .33 out, 2 seconds total
	//norm fade when loading, falling, returning, or descending to a new floor
	private static final float NORM_FADE = 0.67f; //.33 in, .67 steady, .33 out, 1.33 seconds total
	//fast fade when ascending, or descending to a floor you've been on
	private static final float FAST_FADE = 0.50f; //.33 in, .33 steady, .33 out, 1 second total
	
	private static float fadeTime;
	
	public enum Mode {
		DESCEND, ASCEND, CONTINUE, RESURRECT, RETURN, FALL, RESET, NONE,
		PORT1, PORT2, PORT3, PORT4,
		PORTSEWERS, PORTPRISON, PORTCAVES, PORTCITY, PORTHALLS,
		PORTCRAB, PORTTENGU, PORTCOIN, PORTBONE,
		JOURNAL, PALANTIR, RETURNSAVE
	}
	public static Mode mode;

	public static LevelTransition curTransition = null;
	public static int returnDepth;
	public static int returnBranch;
	public static int returnPos;

	public static boolean fallIntoPit;

	public static int journalpage = 0;
	
	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}
	private Phase phase;
	private float timeLeft;

	public Image background;

	private RenderedTextBlock loadingText;

	private RenderedTextBlock storyMessage;
	private ShadowBox storyBG;
	private StyledButton btnContinue;
	private IconButton btnHideStory;
	
	private static Thread thread;
	private static Exception error = null;
	private float waitingTime;

	public static int lastRegion = -1;

	private RectF insets;

	{
		inGameScene = true;
	}
	
	@Override
	public void create() {
		super.create();
		
		String loadingAsset;
		int loadingDepth;
		int loadingBranch = Dungeon.branch;
		fadeTime = NORM_FADE;

		long seed = Dungeon.seed;
		switch (mode){
			default:
				loadingDepth = Dungeon.depth;
				break;
			case CONTINUE:
				GamesInProgress.Info saved = GamesInProgress.check(GamesInProgress.curSlot);
				loadingDepth = saved == null ? Dungeon.depth : saved.depth;
				loadingBranch = saved == null ? Dungeon.branch : saved.branch;
				seed = saved == null ? Dungeon.seed : saved.seed;
				if (loadingDepth == -1 && saved != null) loadingDepth = saved.maxDepth;
				break;
			case DESCEND:
				if (Dungeon.hero == null){
					loadingDepth = xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH;
					loadingBranch = 0;
					fadeTime = SLOW_FADE;
				} else {
					if (curTransition != null)  loadingDepth = curTransition.destDepth;
					else                        loadingDepth = Dungeon.depth;
					if (Statistics.deepestFloor >= loadingDepth) {
						fadeTime = FAST_FADE;
					} else if (loadingDepth == 6 || loadingDepth == 11
							|| loadingDepth == 16 || loadingDepth == 21 || loadingDepth == 26) {
						fadeTime = SLOW_FADE;
					}
				}
				break;
			case FALL:
				loadingDepth = Dungeon.depth + 1;
				break;
			case ASCEND:
				fadeTime = FAST_FADE;
				if (curTransition != null)  loadingDepth = curTransition.destDepth;
				else                        loadingDepth = Dungeon.depth;
				break;
			case RETURN: case RETURNSAVE:
				loadingDepth = returnDepth;
				loadingBranch = returnBranch;
				break;
		}

		if ((mode == Mode.ASCEND || mode == Mode.DESCEND)
				&& Dungeon.hero != null && curTransition != null) {
			loadingBranch = curTransition.destBranch;
		}
		int portalDepth = LoadingSplash.portalDepth(mode, journalpage);
		if (portalDepth != -1) {
			loadingDepth = portalDepth;
			loadingBranch = 0;
		}
		final LoadingSplash splash = LoadingSplash.forLevel(loadingDepth, loadingBranch);
		final boolean mainDungeon = loadingBranch == 0 && loadingDepth >= 1 && loadingDepth <= 25;

		//flush the texture cache whenever moving between regions, helps reduce memory load
		int region = splash.ordinal() + 1;
		if (region != lastRegion){
			TextureCache.clear();
			TitleBackground.reset();
			lastRegion = region;
		}

		loadingAsset = splash.asset;

		if (DeviceCompat.isDebug()){
			fadeTime = 0f;
		}

		insets = Game.platform.getSafeInsets(PlatformSupport.INSET_BLK).scale(1f/defaultZoom);

		int w = (int)(Camera.main.width - insets.left - insets.right);
		int h = (int)(Camera.main.height - insets.top - insets.bottom);

		background = new Image(loadingAsset);
		// Fill the viewport, cropping the sides on portrait screens.
		background.scale.set(Math.max(w/background.width, h/background.height));
		background.x = insets.left + (w - background.width())/2f;
		background.y = insets.top + (h - background.height())/2f;
		PixelScene.align(background);
		add(background);

		Image fadeLeft, fadeRight;
		fadeLeft = new Image(TextureCache.createGradient(0xFF000000, 0xFF000000, 0x00000000));
		fadeLeft.x = background.x-2;
		fadeLeft.y = background.y;
		fadeLeft.scale.set(3, background.height());
		fadeLeft.visible = background.x > 0;
		add(fadeLeft);

		fadeRight = new Image(fadeLeft);
		fadeRight.x = background.x + background.width() + 2;
		fadeRight.y = background.y + background.height();
		fadeRight.angle = 180;
		fadeRight.visible = fadeLeft.visible;
		add(fadeRight);

		Image im = new Image(TextureCache.createGradient(0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF000000)){
			@Override
			public void update() {
				super.update();
				if (phase == Phase.FADE_IN)         aa = Math.max( 0, 2*(timeLeft - (fadeTime - 0.333f)));
				else if (phase == Phase.FADE_OUT)   aa = Math.max( 0, 2*(0.333f - timeLeft));
				//else                                aa = 0;
			}
		};
		im.angle = 90;
		im.x = insets.left + w;
		im.scale.x = h/5f;
		im.scale.y = w;
		add(im);

		String text = Messages.get(Mode.class, mode.name());
		
		loadingText = PixelScene.renderTextBlock( text, 9 );
		loadingText.setPos(
				insets.left + w - loadingText.width() - 12,
				insets.top + h - loadingText.height() - 6
		);
		align(loadingText);
		add(loadingText);

		if (mode == Mode.DESCEND && mainDungeon && !DeviceCompat.isDebug()){
			if (Dungeon.hero == null || (loadingDepth > Statistics.deepestFloor && loadingDepth % 5 == 1)){
					storyMessage = PixelScene.renderTextBlock(Document.INTROS.pageBody(region), 6);
					storyMessage.maxWidth( PixelScene.landscape() ? 180 : 125);
					storyMessage.setPos(insets.left+(w-storyMessage.width())/2f, insets.top+(h-storyMessage.height())/2f);

					storyBG = new ShadowBox();
					storyBG.boxRect(storyMessage.left()-10, storyMessage.top()-10, storyMessage.width()+20, storyMessage.height()+20);
					storyBG.alpha(0.8f);
					add(storyBG);
					add(storyMessage);

					btnContinue = new StyledButton(Chrome.Type.TOAST_TR, Messages.get(InterlevelScene.class, "continue"), 9){
						@Override
						protected void onClick() {
							phase = Phase.FADE_OUT;
							timeLeft = fadeTime;

							btnContinue.enable(false);
							Document.INTROS.readPage(region);
						}
					};
					btnContinue.icon(Icons.STAIRS.get());
					btnContinue.setSize(btnContinue.reqWidth()+10, 22);
					btnContinue.visible = false;
					btnContinue.enable(false);

					KeyEvent.addKeyListener(new Signal.Listener<KeyEvent>() {
						@Override
						public boolean onSignal(KeyEvent keyEvent) {
							if (!keyEvent.pressed && btnContinue.active){
								if (btnHideStory.active && !btnHideStory.icon().visible){
									btnHideStory.setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
									align(btnHideStory);
									btnHideStory.icon().visible = true;
									btnHideStory.parent.add(new Tweener(parent, 0.5f) {
										@Override
										protected void updateValues(float progress) {
											float uiAlpha = progress;
											btnContinue.alpha(uiAlpha);
											storyBG.alpha(uiAlpha*0.8f);
											storyMessage.alpha(uiAlpha);
											btnHideStory.icon().alpha(uiAlpha);
											loadingText.alpha(uiAlpha);
											im.am = uiAlpha;
										}
									});
								} else {
									phase = Phase.FADE_OUT;
									timeLeft = fadeTime;
									btnContinue.enable(false);
									Document.INTROS.readPage(region);
								}
								return true;
							}
							return false;
						}
					});

					btnContinue.setPos(insets.left + (w - btnContinue.width())/2f, storyMessage.bottom()+10);
					add(btnContinue);

					btnHideStory = new IconButton(Icons.CHEVRON.get()){
						@Override
						protected void onClick() {
							if (btnContinue.alpha() != 0 && btnContinue.alpha() != 1){
								return;
							}
							if (icon.visible) {
								enable(false);
								//button is effectively screen-sized, but invisible
								parent.add(new Tweener(parent, 0.5f) {
									@Override
									protected void updateValues(float progress) {
										float uiAlpha = 1 - progress;
										btnContinue.alpha(uiAlpha);
										storyBG.alpha(uiAlpha * 0.8f);
										storyMessage.alpha(uiAlpha);
										icon.alpha(uiAlpha);
										loadingText.alpha(uiAlpha);
										im.am = uiAlpha;
									}

									@Override
									protected void onComplete() {
										super.onComplete();
										setRect(0, 0, Camera.main.width, Camera.main.height);
										enable(true);
										icon.visible = false;
									}
								});
							} else {
								setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
								align(this);
								icon.visible = true;
								parent.add(new Tweener(parent, 0.5f) {
									@Override
									protected void updateValues(float progress) {
										float uiAlpha = progress;
										btnContinue.alpha(uiAlpha);
										storyBG.alpha(uiAlpha*0.8f);
										storyMessage.alpha(uiAlpha);
										icon.alpha(uiAlpha);
										loadingText.alpha(uiAlpha);
										im.am = uiAlpha;
									}
								});
							}
						}

						@Override
						protected void onPointerDown() {
							if (icon.visible) {
								super.onPointerDown();
							}
						}
					};
					btnHideStory.icon().originToCenter();
					btnHideStory.icon().angle = 180f;
					btnHideStory.setRect(btnContinue.right()+2, btnContinue.top(), 20, 21);
					align(btnHideStory);
					btnHideStory.enable(false);
					add(btnHideStory);

					btnContinue.alpha(0);
					storyBG.alpha(0);
					storyMessage.alpha(0);
					btnHideStory.icon().alpha(0);
			}
		}

		phase = Phase.FADE_IN;
		timeLeft = fadeTime;
		
		if (thread == null) {
			thread = new Thread() {
				@Override
				public void run() {
					
					try {

						Actor.fixTime();

						switch (mode) {
							case DESCEND:
								descend();
								break;
							case ASCEND:
								ascend();
								break;
							case CONTINUE:
								restore();
								break;
							case RESURRECT:
								resurrect();
								break;
							case RETURN:
								returnTo();
								break;
							case FALL:
								fall();
								break;
							case RESET:
								reset();
								break;
							case PORT1:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORT2:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORT3:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORT4:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTSEWERS:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTPRISON:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTCAVES:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTCITY:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTHALLS:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTCRAB:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTTENGU:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTCOIN:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PORTBONE:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case PALANTIR:
								portal(LoadingSplash.portalDepth(mode, journalpage));
								break;
							case RETURNSAVE:
								returnTo();
								break;
							case JOURNAL:
								journalPortal();
								break;
						}
						
					} catch (Throwable e) {
						
						//Errors (NoClassDefFound, StackOverflow, static init) used to kill the
						//thread silently and leave the scene waiting forever
						error = e instanceof Exception ? (Exception) e : new RuntimeException(e);
						
					}

					synchronized (thread) {
						if (phase == Phase.STATIC && error == null) {
							afterLoading();
						}
					}
				}
			};
			thread.start();
		}
		waitingTime = 0f;
	}

	private int dots = 0;
	private boolean textFadingIn = true;

	@Override
	public void update() {
		super.update();

		if (btnContinue == null || !btnContinue.isActive()) {
			waitingTime += Game.elapsed;
		}

		if (mode != Mode.FALL && dots != Math.ceil(waitingTime / ((2*fadeTime)/3f))) {
			String text = Messages.get(Mode.class, mode.name());
			dots = (int)Math.ceil(waitingTime / ((2*fadeTime)/3f))%3;
			switch (dots){
				case 1: default:
					loadingText.text(text + ".");
					break;
				case 2:
					loadingText.text(text + "..");
					break;
				case 0:
					loadingText.text(text + "...");
					break;
			}
		}

		int w = (int)(Camera.main.width - insets.left - insets.right);
		int h = (int)(Camera.main.height - insets.top - insets.bottom);
		
		switch (phase) {
		
		case FADE_IN:
			loadingText.alpha( Math.max(0, fadeTime - (timeLeft-0.333f)));
			if ((timeLeft -= Game.elapsed) <= 0) {
				synchronized (thread) {
					if (!thread.isAlive() && error == null) {
						afterLoading();
					} else {
						phase = Phase.STATIC;
					}
				}
			}
			break;
			
		case FADE_OUT:
			background.acc.set(0);
			background.speed.set(0);

			loadingText.alpha( Math.min(1, timeLeft+0.333f) );

			if (btnContinue != null){
				btnContinue.alpha((timeLeft/fadeTime));
				storyMessage.alpha(btnContinue.alpha());
				storyBG.alpha(btnContinue.alpha()*0.8f);
				btnHideStory.icon().alpha(btnContinue.alpha());
			}
			
			if ((timeLeft -= Game.elapsed) <= 0) {
				if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
					// CONTINUE = game load (no actual floor change). Only relocate
					// netHeroes for true transitions (DESCEND / ASCEND / etc).
					if (mode == Mode.CONTINUE) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.broadcastLevelChange();
					} else {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.onLevelDescent();
					}
				}
				Game.switchScene( GameScene.class );
				KeyEvent.clearListeners(); //removes potential listener for continue
				thread = null;
				error = null;
			}
			break;
			
		case STATIC:

			if (btnContinue != null && textFadingIn) {
				btnContinue.alpha(Math.min(1, btnContinue.alpha() + Game.elapsed));
				storyMessage.alpha(btnContinue.alpha());
				storyBG.alpha(btnContinue.alpha()*0.8f);
				btnHideStory.icon().alpha(btnContinue.alpha());

				if (btnContinue.alpha() == 1){
					textFadingIn = false;
					btnHideStory.enable(true);
				}
			}


			if (error != null) {
				String errorMsg;
				if (error instanceof FileNotFoundException)     errorMsg = Messages.get(this, "file_not_found");
				else if (error instanceof IOException)          errorMsg = Messages.get(this, "io_error");
				else if (error.getMessage() != null &&
						error.getMessage().equals("old save")) errorMsg = Messages.get(this, "io_error");

				else throw new RuntimeException("fatal error occurred while moving between floors. " +
							"Seed:" + Dungeon.seed + " depth:" + Dungeon.depth, error);

				add( new WndError( errorMsg ) {
					public void onBackPressed() {
						super.onBackPressed();
						Game.switchScene( StartScene.class );
					}
				} );
				thread = null;
				error = null;
			} else if (thread != null && (int)waitingTime == 10){
				waitingTime = 11f;
				String s = "";
				for (StackTraceElement t : thread.getStackTrace()){
					s += "\n";
					s += t.toString();
				}
				//we care about reporting game logic exceptions, not slow IO
				if (!s.contains("FileUtils.bundleToFile")){
					WarpedPixelDungeon.reportException(
							new RuntimeException("waited more than 10 seconds on levelgen. " +
									"Seed:" + Dungeon.seed + " depth:" + Dungeon.depth + " trace:" +
									s));
				}
			}
			break;
		}

		if (mode == Mode.FALL) {
			loadingText.setPos(
					//the randomization is effectively -2 to +2
					// we don't use the generator stack as levelgen may be occurring
					// and we don't want to accidentally use a seeded generator
					(w + insets.left - loadingText.width() - 4) + 4*(Random.Float(false)-0.5f),
					(h + insets.top - loadingText.height() - 6) + 4*(Random.Float(false)-0.5f)
			);
			align(loadingText);
		}
	}

	private void afterLoading(){
		if (btnContinue != null){
			btnContinue.visible = true;
			float alpha = btnContinue.alpha();
			btnContinue.enable(true);
			btnContinue.alpha(alpha);
			phase = Phase.STATIC;
		} else {
			phase = Phase.FADE_OUT;
			timeLeft = fadeTime;
		}

	}

	private void descend() throws IOException {

		if (Dungeon.hero == null) {
			Mob.clearHeldAllies();
			Dungeon.init();
			GameLog.wipe();

			//When debugging, we may start a game at a later depth to quickly test something
			// if this happens, the games quickly generates all prior levels on branch 0 first,
			// which ensures levelgen consistency with a regular game that was played to that depth.
			if (DeviceCompat.isDebug() && Dungeon.depth <= 26){
				int trueDepth = Dungeon.depth;
				int trueBranch = Dungeon.branch;
				for (int i = 1; i < trueDepth + (trueBranch == 0 ? 0 : 1); i++){
					if (!Dungeon.levelHasBeenGenerated(i, 0)){
						Dungeon.depth = i;
						Dungeon.branch = 0;
						Dungeon.level = Dungeon.newLevel();
						Dungeon.saveLevel(GamesInProgress.curSlot);
					}
				}
				Dungeon.depth = trueDepth;
				Dungeon.branch = trueBranch;
			}

			Level level = Dungeon.newLevel();
			Dungeon.switchLevel( level, -1 );
		} else {
			if (curTransition.destBranch != Dungeon.branch && Dungeon.depth >= 16 && Dungeon.depth <= 20) {
				//FIXME avoids holding allies when entering city quest area, this is very sloppy though
				// perhaps holding allies could be a property of the transition?
			} else {
				Mob.holdAllies(Dungeon.level);
			}
			Dungeon.saveAll();

			Level level;
			Dungeon.depth = curTransition.destDepth;
			Dungeon.branch = curTransition.destBranch;

			if (Dungeon.levelHasBeenGenerated(Dungeon.depth, Dungeon.branch)) {
				level = Dungeon.loadLevel( GamesInProgress.curSlot );
			} else {
				level = Dungeon.newLevel();
			}

			LevelTransition destTransition = level.getTransition(curTransition.destType);
			curTransition = null;
			Dungeon.switchLevel( level, destTransition.cell() );
		}

	}

	//TODO atm falling always just increments depth by 1, do we eventually want to roll it into the transition system?
	private void fall() throws IOException {
		
		Mob.holdAllies( Dungeon.level );
		
		Buff.affect( Dungeon.hero, Chasm.Falling.class );
		Dungeon.saveAll();
		Level level;
		//off one of the world's slices the fall lands on the slice below
		//(OverworldLevel.fallingFrom); everywhere else one floor down
		int sliceBelow = xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.takeFallDepth();
		if (Dungeon.branch == xyz.gabriwar.warpedpixeldungeon.levels.SpiderNestLevel.SPIDER_BRANCH
				&& Dungeon.depth >= xyz.gabriwar.warpedpixeldungeon.levels.SpiderNestLevel.LAST_DEPTH) {
			//the spider nest loops: nothing is built below its deepest floor, so a fall
			//comes out at the top of the nest, the same way its stairs do
			Dungeon.depth = xyz.gabriwar.warpedpixeldungeon.levels.SpiderNestLevel.FIRST_DEPTH;
		} else {
			Dungeon.depth = sliceBelow > 0 ? sliceBelow : Dungeon.depth + 1;
		}
		if (Dungeon.levelHasBeenGenerated(Dungeon.depth, Dungeon.branch)) {
			level = Dungeon.loadLevel( GamesInProgress.curSlot );
		} else {
			level = Dungeon.newLevel();
		}
		Dungeon.switchLevel( level, level.fallCell( fallIntoPit ));
	}

	private void ascend() throws IOException {
		if (curTransition.destBranch != Dungeon.branch && Dungeon.depth >= 16 && Dungeon.depth <= 20) {
			//FIXME avoids holding allies when entering city quest area, this is very sloppy though
			// perhaps holding allies could be a property of the transition?
		} else {
			Mob.holdAllies(Dungeon.level);
		}
		Dungeon.saveAll();

		Level level;
		Dungeon.depth = curTransition.destDepth;
		Dungeon.branch = curTransition.destBranch;

		if (Dungeon.levelHasBeenGenerated(Dungeon.depth, Dungeon.branch)) {
			level = Dungeon.loadLevel( GamesInProgress.curSlot );
		} else {
			level = Dungeon.newLevel();
		}

		LevelTransition destTransition = level.getTransition(curTransition.destType);
		curTransition = null;
		Dungeon.switchLevel( level, destTransition.cell() );
	}
	
	private void returnTo() throws IOException {
		Mob.holdAllies( Dungeon.level );
		Dungeon.saveAll();

		Level level;
		Dungeon.depth = returnDepth;
		Dungeon.branch = returnBranch;
		if (Dungeon.levelHasBeenGenerated(Dungeon.depth, Dungeon.branch)) {
			level = Dungeon.loadLevel( GamesInProgress.curSlot );
		} else {
			level = Dungeon.newLevel();
		}

		Dungeon.switchLevel( level, returnPos );
	}
	
	private void restore() throws IOException {
		
		Mob.clearHeldAllies();

		GameLog.wipe();

		Dungeon.loadGame( GamesInProgress.curSlot );
		if (Dungeon.depth == -1) {
			Dungeon.depth = Statistics.deepestFloor;
			Dungeon.switchLevel( Dungeon.loadLevel( GamesInProgress.curSlot ), -1 );
		} else if (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.hasPendingArrival()) {
			//a save migrated out of the old town level: land on the surface town's plaza
			Level level = Dungeon.levelHasBeenGenerated( Dungeon.depth, Dungeon.branch )
					? Dungeon.loadLevel( GamesInProgress.curSlot ) : Dungeon.newLevel();
			Dungeon.switchLevel( level, -1 );
		} else {
			Level level = Dungeon.loadLevel( GamesInProgress.curSlot );
			Dungeon.switchLevel( level, Dungeon.hero.pos );
		}
	}
	
	private void resurrect() {
		
		Mob.holdAllies( Dungeon.level );

		Level level;
		if (Dungeon.level.locked) {
			ArrayList<Item> preservedItems = Dungeon.level.getItemsToPreserveFromSealedResurrect();

			Dungeon.hero.resurrect();
			level = Dungeon.newLevel();
			Dungeon.hero.pos = level.randomRespawnCell(Dungeon.hero);
			if (Dungeon.hero.pos == -1) Dungeon.hero.pos = level.entrance();

			for (Item i : preservedItems){
				int pos = level.randomRespawnCell(null);
				if (pos == -1) pos = level.entrance();
				level.drop(i, pos);
			}
			int pos = level.randomRespawnCell(null);
			if (pos == -1) pos = level.entrance();
			level.drop(new LostBackpack(), pos);

			//need to reset key replacement tracking as well
			if (Dungeon.hero.buff(SkeletonKey.KeyReplacementTracker.class) != null){
				Dungeon.hero.buff(SkeletonKey.KeyReplacementTracker.class).clearDepth();
			}

		} else {
			level = Dungeon.level;
			BArray.setFalse(level.heroFOV);
			BArray.setFalse(level.visited);
			BArray.setFalse(level.mapped);
			int invPos = Dungeon.hero.pos;
			int tries = 0;
			do {
				Dungeon.hero.pos = level.randomRespawnCell(Dungeon.hero);
				tries++;

			//prevents spawning on traps or plants, prefers farther locations first
			} while (level.traps.get(Dungeon.hero.pos) != null
					|| (level.plants.get(Dungeon.hero.pos) != null && tries < 500)
					|| level.trueDistance(invPos, Dungeon.hero.pos) <= 30 - (tries/10));

			//directly trample grass
			if (level.map[Dungeon.hero.pos] == Terrain.HIGH_GRASS || level.map[Dungeon.hero.pos] == Terrain.FURROWED_GRASS){
				level.map[Dungeon.hero.pos] = Terrain.GRASS;
			}
			Dungeon.hero.resurrect();
			level.drop(new LostBackpack(), invPos);
		}

		Notes.add(Notes.Landmark.LOST_PACK);

		Dungeon.switchLevel( level, Dungeon.hero.pos );
	}

	private void reset() throws IOException {
		
		Mob.holdAllies( Dungeon.level );

		SpecialRoom.resetPitRoom(Dungeon.depth+1);

		Level level = Dungeon.newLevel();
		Dungeon.switchLevel( level, level.entrance() );
	}
	
	private void portal(int targetDepth) throws IOException {
		Mob.holdAllies( Dungeon.level );
		Dungeon.saveAll();

		Dungeon.depth = targetDepth;
		Dungeon.branch = 0;

		Level level;
		if (Dungeon.levelHasBeenGenerated(Dungeon.depth, Dungeon.branch)) {
			level = Dungeon.loadLevel( GamesInProgress.curSlot );
		} else {
			level = Dungeon.newLevel();
		}
		Dungeon.switchLevel( level, level.entrance() );
	}

	private void journalPortal() throws IOException {
		int targetDepth = LoadingSplash.portalDepth(Mode.JOURNAL, journalpage);
		if (journalpage == 5) {
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.arriveInTown(
					xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.TOWN_PLAZA);
		}

		//The puzzle floors are rebuilt on every visit, as they were in Sprouted. WPD keeps
		//generated levels, and a sokoban puzzle is trivially wedgeable - corner a sheep and
		//the floor is unsolvable forever, taking its iron keys and prizes with it. Only the
		//safe room (50) and the town (55) persist, which is the same split Sprouted used.
		//This is not a farming hole: the unique prizes hang off firsts[], which OtilukesJournal
		//spends when you port, so a rebuilt floor comes back solvable but already stripped of
		//them - exactly as in the source.
		boolean puzzle = (targetDepth >= 51 && targetDepth <= 54)
				|| targetDepth == 66 || targetDepth == 67;

		if (puzzle) {
			//keys already taken out of the old layout would open the new one without a
			//single sheep being pushed
			IronKey key = new IronKey(targetDepth);
			key.quantity(Notes.keyCount(key));
			if (key.quantity() > 0) {
				Notes.remove(key);
			}

			Mob.holdAllies( Dungeon.level );
			Dungeon.saveAll();

			Dungeon.depth = targetDepth;
			Dungeon.branch = 0;

			Level level = Dungeon.newLevel();
			Dungeon.switchLevel( level, level.entrance() );
			return;
		}

		portal(targetDepth);
	}

	@Override
	protected void onBackPressed() {
		//Do nothing
	}
}
