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

import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.services.news.News;
import xyz.gabriwar.warpedpixeldungeon.services.news.NewsArticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.ExitButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.TitleBackground;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.StyledButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsScene extends PixelScene {

	boolean displayingNoArticles = false;

	private static final int BTN_HEIGHT = 22;
	private static final int BTN_WIDTH = 100;

	@Override
	public void create() {
		super.create();

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		add(BG);

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		int fullWidth = PixelScene.landscape() ? 202 : 100;
		int left = (int)insets.left + (w - fullWidth)/2;

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle( Icons.NEWS.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		float top = 18 + insets.top;

		displayingNoArticles = !News.articlesAvailable();
		if (displayingNoArticles || Messages.lang() != Languages.ENGLISH) {

			Component newsInfo = new NewsInfo();
			newsInfo.setRect(left, top, fullWidth, 0);
			add(newsInfo);

			top = newsInfo.bottom();

		}

		int pages = 1;
		if (!displayingNoArticles) {
			ArrayList<NewsArticle> articles = News.articles();

			float articleSpace = h - top - 2 + insets.top;
			//rows that fit, minus the bottom button row
			int rowsFit = (int)(articleSpace / (BTN_HEIGHT + 0.5f)) - 1;
			if (rowsFit < 1) rowsFit = 1;
			int perPage = PixelScene.landscape() ? rowsFit * 2 : rowsFit;

			pages = Math.max(1, (int)Math.ceil(articles.size() / (float)perPage));
			if (page >= pages) page = pages - 1;
			if (page < 0) page = 0;

			ArrayList<NewsArticle> shown = new ArrayList<>(articles.subList(
					page * perPage, Math.min(articles.size(), (page + 1) * perPage)));

			int rows = shown.size();
			if (PixelScene.landscape()){
				rows = (rows + 1) / 2;
			}
			rows++;

			float gap = ((articleSpace) - (BTN_HEIGHT * rows)) / (float)rows;

			boolean rightCol = false;
			for (NewsArticle article : shown) {
				StyledButton b = new ArticleButton(article);
				b.multiline = true;
				if (!rightCol) {
					top += gap;
					b.setRect( left, top, BTN_WIDTH, BTN_HEIGHT);
				} else {
					b.setRect( left + fullWidth - BTN_WIDTH, top, BTN_WIDTH, BTN_HEIGHT);
				}
				align(b);
				add(b);
				if (!PixelScene.landscape()) {
					top += BTN_HEIGHT;
				} else {
					if (rightCol){
						top += BTN_HEIGHT;
					}
					rightCol = !rightCol;
				}
			}
			top += gap;
		} else {
			top += 20;
		}

		String siteText = Messages.get(this, "read_more");
		if (pages > 1) siteText += "  (" + (page + 1) + "/" + pages + ")";
		StyledButton btnSite = new StyledButton(Chrome.Type.GREY_BUTTON_TR, siteText){
			@Override
			protected void onClick() {
				super.onClick();
				WarpedPixelDungeon.platform.openURI("https://github.com/GabriWar/warpedpd");
			}
		};
		btnSite.icon(Icons.get(Icons.NEWS));
		btnSite.textColor(Window.TITLE_COLOR);

		if (pages > 1){
			int arrowW = 16;
			StyledButton btnPrev = new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
				@Override
				protected void onClick() {
					if (page > 0){
						page--;
						WarpedPixelDungeon.seamlessResetScene();
					}
				}
			};
			btnPrev.icon(Icons.get(Icons.LEFTARROW));
			btnPrev.setRect(left, top, arrowW, BTN_HEIGHT);
			add(btnPrev);

			StyledButton btnNext = new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
				@Override
				protected void onClick() {
					page++;
					WarpedPixelDungeon.seamlessResetScene();
				}
			};
			btnNext.icon(Icons.get(Icons.RIGHTARROW));
			btnNext.setRect(left + fullWidth - arrowW, top, arrowW, BTN_HEIGHT);
			add(btnNext);

			btnSite.setRect(left + arrowW + 1, top, fullWidth - 2*(arrowW + 1), BTN_HEIGHT);
		} else {
			btnSite.setRect(left, top, fullWidth, BTN_HEIGHT);
		}
		add(btnSite);

	}

	//which page of articles is showing; static so it survives scene resets
	private static int page = 0;

	@Override
	protected void onBackPressed() {
		WarpedPixelDungeon.switchNoFade( TitleScene.class );
	}

	@Override
	public void update() {
		if (displayingNoArticles && News.articlesAvailable()){
			WarpedPixelDungeon.seamlessResetScene();
		}
		super.update();
	}

	private static class NewsInfo extends Component {

		NinePatch bg;
		RenderedTextBlock text;
		RedButton button;

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);
			
			String message = "";

			if (Messages.lang() != Languages.ENGLISH){
				message += Messages.get(this, "english_warn");
			}
			
			if (!News.articlesAvailable()){
				if (WPDSettings.news()) {
					if (WPDSettings.WiFi() && !Game.platform.connectedToUnmeteredNetwork()) {
						message += "\n\n" + Messages.get(this, "metered_network");

						button = new RedButton(Messages.get(this, "enable_data")) {
							@Override
							protected void onClick() {
								super.onClick();
								WPDSettings.WiFi(false);
								News.checkForNews();
								WarpedPixelDungeon.seamlessResetScene();
							}
						};
						add(button);
					} else {
						message += "\n\n" + Messages.get(this, "no_internet");
					}
				} else {
					message += "\n\n" + Messages.get(this, "news_disabled");

					button = new RedButton(Messages.get(this, "enable_news")) {
						@Override
						protected void onClick() {
							super.onClick();
							WPDSettings.news(true);
							News.checkForNews();
							WarpedPixelDungeon.seamlessResetScene();
						}
					};
					add(button);
				}
			}

			if (message.startsWith("\n\n")) message = message.replaceFirst("\n\n", "");
			
			text = PixelScene.renderTextBlock(message, 6);
			text.hardlight(CharSprite.WARNING);
			add(text);
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;

			text.maxWidth((int)width - bg.marginHor());
			text.setPos(x + bg.marginLeft(), y + bg.marginTop()+1);

			height = (text.bottom()) - y;

			if (button != null){
				height += 4;
				button.multiline = true;
				button.setSize(width - bg.marginHor(), 16);
				button.setSize(width - bg.marginHor(), Math.max(button.reqHeight(), 16));
				button.setPos(x + (width - button.width())/2, y + height);
				height = button.bottom() - y;
			}

			height += bg.marginBottom() + 1;

			bg.size(width, height);

		}
	}

	private static class ArticleButton extends StyledButton {

		NewsArticle article;

		BitmapText date;

		public ArticleButton(NewsArticle article) {
			super(Chrome.Type.GREY_BUTTON_TR, article.title, 6);
			this.article = article;

			icon(News.parseArticleIcon(article));
			long lastRead = WPDSettings.newsLastRead();
			if (lastRead > 0 && article.date.getTime() > lastRead) {
				textColor(Window.SHPX_COLOR);
			}

			date = new BitmapText( News.parseArticleDate(article), pixelFont);
			date.scale.set(PixelScene.align(0.5f));
			date.hardlight( 0x888888 );
			date.measure();
			add(date);
		}

		@Override
		protected void layout() {
			super.layout();

			icon.x = x + bg.marginLeft() + (16-icon.width())/2f;
			PixelScene.align(icon);
			text.setPos(x + bg.marginLeft() + 18, text.top());

			if (date != null) {
				date.x = x + width - bg.marginRight() - date.width() + 1;
				date.y = y + height - bg.marginBottom() - date.height() + 2.5f;
				align(date);
			}
		}

		@Override
		protected void onClick() {
			super.onClick();
			textColor(Window.WHITE);
			if (article.date.getTime() > WPDSettings.newsLastRead()){
				WPDSettings.newsLastRead(article.date.getTime());
			}
			WarpedPixelDungeon.scene().addToFront(new WndArticle(article));
		}
	}

	//conventional-commit changelog line: "- type(scope): message (hash)". the type
	//may be compound (e.g. "balance+fix", "feat/ui") - allow +, /, - and digits.
	private static final Pattern CHANGE_LINE =
			Pattern.compile("^[-*]\\s*([A-Za-z][A-Za-z0-9+/_-]*)(?:\\([^)]*\\))?!?\\s*:\\s*(.+)$");
	//trailing " (abc1234)" commit hash, stripped from every rendered line
	private static final Pattern TRAILING_HASH =
			Pattern.compile("\\s*\\([0-9a-f]{7,10}\\)\\s*$");

	//one icon per change type, so changelogs read at a glance. compound types
	//("balance+fix") resolve on their first token.
	private static Image changeTypeIcon(String type){
		switch (type.split("[+/]")[0]){
			case "feat":    return Icons.get(Icons.PLUS);
			case "fix":     return Icons.get(Icons.CHECKED);
			case "balance": return Icons.get(Icons.STATS);
			case "art": case "ui": case "polish":
				return Icons.get(Icons.SCROLL_COLOR);
			case "perf":    return Icons.get(Icons.BUSY);
			case "revert":  return Icons.get(Icons.REPEAT);
			case "build": case "ci": case "chore": case "tools": case "docs": case "test":
				return Icons.get(Icons.PREFS);
			default:        return Icons.get(Icons.CHANGES);
		}
	}

	private static class WndArticle extends Window {

		private static final int WND_WIDTH_P = 120;
		private static final int WND_WIDTH_L = 144;
		private static final int MAX_LINES   = 14;

		public WndArticle(NewsArticle article ) {
			super();

			int width = PixelScene.landscape() ? WND_WIDTH_L : WND_WIDTH_P;

			IconTitle title = new IconTitle(News.parseArticleIcon(article), article.title);
			title.setRect(0, 0, width, 0);
			add(title);

			float pos = title.bottom() + 4;

			int shown = 0;
			for (String line : article.summary.split("\n")){
				line = line.trim();
				if (line.isEmpty()) continue;

				if (shown >= MAX_LINES){
					RenderedTextBlock more = PixelScene.renderTextBlock("...", 6);
					more.setPos((width - more.width()) / 2f, pos);
					add(more);
					pos = more.bottom() + 2;
					break;
				}

				//drop the trailing commit hash from any line before rendering
				line = TRAILING_HASH.matcher(line).replaceAll("");

				Matcher m = CHANGE_LINE.matcher(line);
				if (m.matches()){
					//changelog entry: type icon + cleaned message
					Image ic = changeTypeIcon(m.group(1).toLowerCase(Locale.ENGLISH));
					String msg = m.group(2);
					if (!msg.isEmpty()) msg = Character.toUpperCase(msg.charAt(0)) + msg.substring(1);

					RenderedTextBlock txt = PixelScene.renderTextBlock(msg, 6);
					txt.maxWidth(width - (int)ic.width() - 4);
					float rowH = Math.max(ic.height(), txt.height());
					ic.x = 0;
					ic.y = pos + (rowH - ic.height()) / 2f;
					PixelScene.align(ic);
					add(ic);
					txt.setPos(ic.width() + 4, pos + (rowH - txt.height()) / 2f);
					add(txt);
					pos += rowH + 3;
				} else {
					RenderedTextBlock txt = PixelScene.renderTextBlock(line, 6);
					txt.maxWidth(width);
					txt.setPos(0, pos);
					add(txt);
					pos += txt.height() + 3;
				}
				shown++;
			}

			RedButton link = new RedButton(Messages.get(NewsScene.class, "read_more")){
				@Override
				protected void onClick() {
					super.onClick();
					WarpedPixelDungeon.platform.openURI(article.URL);
				}
			};
			link.setRect(0, pos + 2, width, BTN_HEIGHT);
			add(link);
			resize(width, (int) link.bottom());
		}


	}

}
