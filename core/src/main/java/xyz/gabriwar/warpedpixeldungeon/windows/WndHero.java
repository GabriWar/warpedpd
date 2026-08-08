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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.IconButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.StatusPane;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentButton;
import xyz.gabriwar.warpedpixeldungeon.ui.SkillTreePane;
import xyz.gabriwar.warpedpixeldungeon.ui.TalentsPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.utils.DungeonSeed;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.Locale;

public class WndHero extends WndTabbed {
	
	private static final int WIDTH		= 140;
	private static final int HEIGHT		= 200;
	
	private StatsTab stats;
	private TalentsTab talents;
	private BuffsTab buffs;

	public static int lastIdx = 0;

	public WndHero() {
		
		super();
		
		resize( WIDTH, HEIGHT );
		
		stats = new StatsTab();
		add( stats );
		stats.setRect(0, 0, WIDTH, HEIGHT);

		talents = new TalentsTab();
		add(talents);
		talents.setRect(0, 0, WIDTH, HEIGHT);

		buffs = new BuffsTab();
		add( buffs );
		buffs.setRect(0, 0, WIDTH, HEIGHT);
		buffs.setupList();
		
		add( new IconTab( Icons.get(Icons.RANKINGS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) {
					lastIdx = 0;
					if (!stats.visible) {
						stats.initialize();
					}
				}
				stats.visible = stats.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.TALENT) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 1;
				if (selected) StatusPane.talentBlink = 0;
				talents.visible = talents.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.BUFFS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 2;
				buffs.visible = buffs.active = selected;
			}
		} );

		layoutTabs();

		talents.setRect(0, 0, WIDTH, HEIGHT);
		talents.pane.scrollTo(0, 0);
		talents.layout();

		select( lastIdx );
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == WPDAction.HERO_INFO) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		stats.layout();
		talents.layout();
		buffs.layout();
	}

	//the stats live in a scroll pane: with weather and the day's calendar
	//modifiers the list is taller than the window
	private class StatsTab extends Component {
		
		private static final int GAP = 6;
		
		private float pos;
		private ScrollPane statList;
		private StatsContent content;
		
		public StatsTab() {
			super();
			initialize();
		}

		@Override
		protected void createChildren() {
			super.createChildren();
			content = new StatsContent();
			statList = new ScrollPane( content );
			add( statList );
		}

		@Override
		protected void layout() {
			super.layout();
			statList.setRect(x, y, width, height);
			content.setSize(width, pos);
		}

		public void initialize(){

			content.wipe();
			
			Hero hero = Dungeon.hero;

			IconTitle title = new IconTitle();
			title.icon( HeroSprite.avatar(hero) );
			if (hero.name().equals(hero.className()))
				title.label( Messages.get(this, "title", hero.lvl, hero.className() ).toUpperCase( Locale.ENGLISH ) );
			else
				title.label((hero.name() + "\n" + Messages.get(this, "title", hero.lvl, hero.className())).toUpperCase(Locale.ENGLISH));
			title.color(Window.TITLE_COLOR);
			title.setRect( 0, 0, WIDTH-16, 0 );
			content.add(title);

			IconButton infoButton = new IconButton(Icons.get(Icons.INFO)){
				@Override
				protected void onClick() {
					super.onClick();
					if (WarpedPixelDungeon.scene() instanceof GameScene){
						GameScene.show(new WndHeroInfo(hero.heroClass));
					} else {
						WarpedPixelDungeon.scene().addToFront(new WndHeroInfo(hero.heroClass));
					}
				}

				@Override
				protected String hoverText() {
					return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
				}

			};
			infoButton.setRect(title.right(), 0, 16, 16);
			content.add(infoButton);


			IconButton renameButton = new IconButton(Icons.get(Icons.SCROLL_GREY)){
				@Override
				protected void onClick() {
					super.onClick();
					GameScene.show(new WndTextInput(
							Messages.get(WndHero.class, "rename_title"),
							Messages.get(WndHero.class, "rename_body"),
							hero.customName != null ? hero.customName : "",
							20,
							false,
							Messages.get(WndHero.class, "rename_confirm"),
							Messages.get(WndHero.class, "rename_cancel")){
						@Override
						public void onSelect(boolean positive, String text) {
							if (positive) {
								hero.customName = text.trim().isEmpty() ? null : text.trim();
								WndHero.this.hide();
								GameScene.show(new WndHero());
							}
						}
					});
				}

				@Override
				protected String hoverText() {
					return Messages.get(WndHero.class, "rename_title");
				}
			};
			renameButton.setRect(title.right(), 16, 16, 16);
			content.add(renameButton);

			pos = title.bottom() + 2*GAP;

			int strBonus = hero.STR() - hero.STR;
			if (strBonus > 0)           statSlot( Messages.get(this, "str"), hero.STR + " + " + strBonus );
			else if (strBonus < 0)      statSlot( Messages.get(this, "str"), hero.STR + " - " + -strBonus );
			else                        statSlot( Messages.get(this, "str"), hero.STR() );
			if (hero.shielding() > 0)   statSlot( Messages.get(this, "health"), hero.HP + "+" + hero.shielding() + "/" + hero.HT );
			else                        statSlot( Messages.get(this, "health"), (hero.HP) + "/" + hero.HT );
			int manaBonus = RingOfMagic.manaBonus( hero );
			if (manaBonus > 0)          statSlot( Messages.get(this, "mana"), hero.MP + "/" + hero.MT + "+" + manaBonus );
			else                        statSlot( Messages.get(this, "mana"), hero.MP + "/" + hero.MT );
			statSlot( Messages.get(this, "exp"), hero.exp + "/" + hero.maxExp() );

			float tileT  = TileTemperature.tileTemp(hero.pos);
			float feelsT = TileTemperature.feelsLikeAt(hero.pos);
			statSlot( "Tile temp", Messages.decimalFormat("#.#", tileT) + "°C" );
			if (Math.abs(feelsT - tileT) >= 1f)
				statSlot( "Feels like", Messages.decimalFormat("#.#", feelsT) + "°C" );
			if (!Float.isNaN(hero.bodyTemp))
				statSlot( "Body temp", Messages.decimalFormat("#.#", hero.bodyTemp) + "°C" );

			pos += GAP;

			// Weather
			String weather = ClimateManager.weatherState().toString().toLowerCase().replace('_', ' ');
			statSlot( "Weather", weather );
			statSlot( "Cloud cover", (int)(ClimateManager.cloudCover() * 100) + "%" );

			float wind = ClimateManager.localWindSpeed();
			if (wind >= 1f) {
				float dir = ClimateManager.surfaceWindDir();
				String[] cardinals = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
				String cardinal = cardinals[Math.round(dir / 45f) % 8];
				statSlot( "Wind", Messages.decimalFormat("#.#", wind) + " m/s " + cardinal );
			}

			float precipRate = ClimateManager.localPrecipRate();
			if (precipRate > 0f) {
				PrecipType pt = ClimateManager.localPrecipType();
				String precipName = pt == PrecipType.SNOW     ? "Snow" :
				                    pt == PrecipType.BLIZZARD  ? "Blizzard" : "Rain";
				String intensity  = precipRate < 0.3f ? "light" :
				                    precipRate < 0.7f ? "moderate" : "heavy";
				statSlot( precipName, intensity );
			}

			pos += GAP;

			statSlot( Messages.get(this, "gold"), Statistics.goldCollected );
			statSlot( Messages.get(this, "depth"), Statistics.deepestFloor );
			if (Dungeon.daily){
				if (!Dungeon.dailyReplay) {
					statSlot(Messages.get(this, "daily_for"), "_" + Dungeon.customSeedText + "_");
				} else {
					statSlot(Messages.get(this, "replay_for"), "_" + Dungeon.customSeedText + "_");
				}
			} else if (!Dungeon.customSeedText.isEmpty()){
				statSlot( Messages.get(this, "custom_seed"), "_" + Dungeon.customSeedText + "_" );
			} else {
				statSlot( Messages.get(this, "dungeon_seed"), DungeonSeed.convertToCode(Dungeon.seed) );
			}

			pos += GAP;

			// Today: the calendar and whatever it is doing to the numbers
			RenderedTextBlock today = PixelScene.renderTextBlock( Messages.get(this, "today"), 8 );
			today.hardlight( Window.TITLE_COLOR );
			today.setPos( 0, pos );
			PixelScene.align(today);
			content.add( today );
			pos += today.height() + GAP;

			statSlot( Messages.get(this, "season"), Messages.get(GameCalendar.class, GameCalendar.season().name().toLowerCase()) );
			statSlot( Messages.get(this, "weekday"), GameCalendar.weekdayString() );
			statSlot( Messages.get(this, "moon"), GameCalendar.moonPhaseString() );
			ArrayList<String[]> mods = GameCalendar.activeModifiers();
			if (mods.isEmpty()) {
				statSlot( Messages.get(this, "no_modifiers"), "" );
			} else {
				for (String[] mod : mods) {
					statSlot( mod[0], mod[1] );
				}
			}

			pos += GAP;

			content.setSize(WIDTH, pos);
			statList.scrollTo(0, 0);
		}

		private void statSlot( String label, String value ) {

			int size = 8;
			RenderedTextBlock txt;
			do {
				txt = PixelScene.renderTextBlock( label, size );
				size--;
			} while (txt.width() >= WIDTH * 0.55f);
			txt.setPos(0, pos + (6 - txt.height())/2);
			PixelScene.align(txt);
			content.add( txt );

			size = 8;
			do {
				txt = PixelScene.renderTextBlock( value, size );
				size--;
			} while (txt.width() >= WIDTH * 0.45f);
			txt.setPos(WIDTH * 0.55f, pos + (6 - txt.height())/2);
			PixelScene.align(txt);
			content.add( txt );
			
			pos += GAP + txt.height();
		}
		
		private void statSlot( String label, int value ) {
			statSlot( label, Integer.toString( value ) );
		}
	}

	private static class StatsContent extends Component {
		//destroys the rows so the tab can be rebuilt
		void wipe() {
			for (Gizmo g : members){
				if (g != null) g.destroy();
			}
			clear();
		}
	}

	public class TalentsTab extends Component {

		SkillTreePane pane;

		@Override
		protected void createChildren() {
			super.createChildren();
			pane = new SkillTreePane();
			add(pane);
		}

		@Override
		protected void layout() {
			super.layout();
			pane.setRect(x, y, width, height);
		}

	}

	private class BuffsTab extends Component {
		
		private static final int GAP = 2;
		
		private float pos;
		private ScrollPane buffList;
		private ArrayList<BuffSlot> slots = new ArrayList<>();

		@Override
		protected void createChildren() {

			super.createChildren();

			buffList = new ScrollPane( new Component() ){
				@Override
				public void onClick( float x, float y ) {
					int size = slots.size();
					for (int i=0; i < size; i++) {
						if (slots.get( i ).onClick( x, y )) {
							break;
						}
					}
				}
			};
			add(buffList);
		}
		
		@Override
		protected void layout() {
			super.layout();
			buffList.setRect(0, 0, width, height);
		}
		
		private void setupList() {
			Component content = buffList.content();
			for (Buff buff : Dungeon.hero.buffs()) {
				if (buff.icon() != BuffIndicator.NONE) {
					BuffSlot slot = new BuffSlot(buff);
					slot.setRect(0, pos, WIDTH, slot.icon.height());
					content.add(slot);
					slots.add(slot);
					pos += GAP + slot.height();
				}
			}
			content.setSize(buffList.width(), pos);
			buffList.setSize(buffList.width(), buffList.height());
		}

		private class BuffSlot extends Component {

			private Buff buff;

			Image icon;
			RenderedTextBlock txt;

			public BuffSlot( Buff buff ){
				super();
				this.buff = buff;

				icon = new BuffIcon(buff, true);
				icon.y = this.y;
				add( icon );

				txt = PixelScene.renderTextBlock( Messages.titleCase(buff.name()), 8 );
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
				add( txt );

			}

			@Override
			protected void layout() {
				super.layout();
				icon.y = this.y;
				txt.maxWidth((int)(width - icon.width()));
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
			}
			
			protected boolean onClick ( float x, float y ) {
				if (inside( x, y )) {
					GameScene.show(new WndInfoBuff(buff));
					return true;
				} else {
					return false;
				}
			}
		}
	}
}
