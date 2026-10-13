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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideLayout;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideProgress;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.Cite;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.CiteMatch;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideCites;
import xyz.gabriwar.warpedpixeldungeon.journal.guidedemos.Demo;
import xyz.gabriwar.warpedpixeldungeon.journal.guidedemos.GuideDemos;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.GuideScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.CiteArt;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.CiteChip;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.DemoStage;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.LiveCrop;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

//A Descent Guide page as it is read: its subject alive in the title, its words a paragraph at a
//time, and under each paragraph the things it names (CiteChip), like the figures of a field
//guide, and the scenes that act it out (DemoStage); then, for a boss, what it drops. Only the
//chips and scenes in the scrolled view are alive, and the window takes them all with it when it
//closes.
public class WndGuideNode extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 200;
	private static final int GAP = 2;
	private static final int TITLE_ICON = 20;
	//the most chips one page shows (GuideCitesTest keeps every page within it)
	private static final int MAX_CHIPS = 24;

	private final String key;
	private final Callback onClose;
	private final ScrollPane pane;
	private final ArrayList<CiteChip> chips = new ArrayList<>();
	private final ArrayList<DemoStage> stages = new ArrayList<>();
	//the drop rows' tops and bottoms in the content, and what each drops
	private final ArrayList<float[]> dropRows = new ArrayList<>();
	private final ArrayList<Mob.DropInfo> dropInfos = new ArrayList<>();

	/** A page as the guide shows it: GuideScene builds it on a tap. onClose runs after hide (may be null). */
	public static WndGuideNode forNode( GuideGraph.Node node, Callback onClose, boolean link ){
		return new WndGuideNode( node, onClose, link );
	}

	/** A page shown away from the guide (the journal button, a station's button): marks it read; null for a key the
	 *  guide does not hold or a locked page. link: a button under it opens the Descent Guide on this page. */
	public static WndGuideNode forPage( String key, Callback onClose, boolean link ){
		GuideGraph.Node node = GuideGraph.find( GuideGraph.build(), key );
		if (node == null || node.key == null || !GuideGraph.unlocked( node )) return null;
		GuideProgress.markRead( node.key );
		Sample.INSTANCE.play( Assets.Sounds.READ, 0.5f );
		return new WndGuideNode( node, onClose, link );
	}

	private WndGuideNode( GuideGraph.Node node, Callback onClose, boolean link ){
		super();
		key = node.key;
		this.onClose = onClose;

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle titlebar = new IconTitle( titleIcon( node ), node.title() );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		ArrayList<Mob.DropInfo> drops = null;
		String text = node.text();
		String body;
		if (node.mob != null){
			String msg = "";
			try {
				Mob mob = (Mob) Reflection.newInstance( node.mob );
				if (mob != null){
					msg = mob.description() + mob.dropsInfo();
					drops = mob.getDrops();
				}
			} catch (Exception e){
				//no game state to describe it from (the title screen): the journal alone
				msg = "";
				drops = null;
			}
			if (text != null){
				msg += (msg.isEmpty() ? "" : "\n\n") + Messages.get( GuideScene.class, "from_journal" ) + "\n\n" + text;
			}
			body = msg;
		} else {
			String sub = node.subtitle();
			body = (sub != null ? "_" + sub + "_\n\n" : "")
					+ (text != null ? text : Messages.get( GuideScene.class, "blank" ));
		}

		Component content = new Component();
		float y = page( content, node, body, width );

		if (drops != null && !drops.isEmpty()){
			RenderedTextBlock header = PixelScene.renderTextBlock(
					"_" + Messages.get(Mob.class, "drops_header") + "_", 6);
			header.setHightlighting(true);
			header.maxWidth(width);
			header.setPos(0, y + GAP);
			content.add(header);
			y = header.bottom() + 2;

			for (Mob.DropInfo drop : drops) {
				y = addDropRow(content, drop, width, y);
			}
		}

		content.setSize( width, y );

		//a tap on the page reaches its chips and rows through the pane: it is the newest pointer
		//area over them, so the press is its own
		pane = new ScrollPane( content ){
			@Override
			public void onClick( float x, float y ) {
				tapAt( x, y );
			}
		};
		add( pane );

		RedButton toGuide = null;
		int buttonH = 0;
		if (link){
			toGuide = new RedButton( Messages.get( GuideScene.class, "open_guide" ), 8 ){
				@Override
				protected void onClick() {
					openGuide();
				}
			};
			buttonH = 16;
		}

		float below = link ? GAP + buttonH : 0;
		float maxHeight = PixelScene.uiCamera.height * 0.85f - titlebar.bottom() - 2*GAP - below;
		float paneHeight = Math.min( maxHeight, y );

		resize( width, (int)(titlebar.bottom() + 2*GAP + paneHeight + below) );
		pane.setRect( 0, titlebar.bottom() + 2*GAP, width, paneHeight );
		if (toGuide != null){
			toGuide.setRect( 0, pane.bottom() + GAP, width, buttonH );
			add( toGuide );
		}
		cull();
	}

	//the page's subject, alive: its boss, the thing the page is about, the chapter's own art, else its icon
	private static Image titleIcon( GuideGraph.Node node ){
		if (node.mob != null){
			CharSprite s = GuidePuppet.make( node.mob );
			if (s != null) return new LiveCrop( s, TITLE_ICON, TITLE_ICON, 6, 10 );
		}
		Cite primary = GuideCites.primary( node.key );
		if (primary != null && primary.still()) return CiteArt.icon( primary, TITLE_ICON, TITLE_ICON, 6, 10 );
		if (node.sprite >= 0) return new ItemSprite( node.sprite );
		return Icons.get( node.icon != null ? node.icon : Icons.INFO );
	}

	//--- the words and their figures ---

	//the body a paragraph at a time, each with the chips of what it names first under it; cites no
	//paragraph names (and a boss page's boss) lead above. Returns the content's height so far
	private float page( Component content, GuideGraph.Node node, String body, int width ){
		ArrayList<String> paras = paragraphs( body );
		List<Cite> cites = GuideCites.of( node.key );
		if (cites.size() > MAX_CHIPS) cites = cites.subList( 0, MAX_CHIPS );

		//the subtitle heads the page: what it names is shown under the paragraph that tells of it
		int from = node.mob == null && node.subtitle() != null && paras.size() > 1 ? 1 : 0;
		String[][] tokens = new String[paras.size()][];
		for (int i = 0; i < tokens.length; i++) tokens[i] = i < from ? new String[0] : CiteMatch.tokens( paras.get( i ) );

		ArrayList<Cite> lead = new ArrayList<>();
		ArrayList<ArrayList<Cite>> under = new ArrayList<>();
		ArrayList<ArrayList<Integer>> at = new ArrayList<>();
		for (int i = 0; i < paras.size(); i++){
			under.add( new ArrayList<Cite>() );
			at.add( new ArrayList<Integer>() );
		}
		//each demo under the paragraph it acts out (by its words, as the cites find theirs); a demo
		//that names none, or whose words are gone from the page, plays at the end of the text
		ArrayList<ArrayList<Demo>> acted = new ArrayList<>();
		for (int i = 0; i < paras.size(); i++) acted.add( new ArrayList<Demo>() );
		for (Demo d : GuideDemos.of( node.key )){
			int para = paras.size() - 1;
			if (d.anchor() != null){
				String[] run = CiteMatch.tokens( d.anchor() );
				for (int i = 0; i < tokens.length; i++){
					if (CiteMatch.find( tokens[i], run ) >= 0){
						para = i;
						break;
					}
				}
			}
			if (para >= 0) acted.get( para ).add( d );
		}

		for (Cite c : cites){
			int para = -1, first = Integer.MAX_VALUE;
			if (node.mob == null || c.kind != Cite.Kind.MOB || c.type != node.mob){
				for (int i = 0; i < tokens.length && para < 0; i++){
					for (String w : c.words){
						int idx = CiteMatch.find( tokens[i], CiteMatch.tokens( w ) );
						if (idx >= 0 && idx < first){
							first = idx;
							para = i;
						}
					}
				}
			}
			if (para < 0){
				lead.add( c );
				continue;
			}
			//in the order the paragraph names them (registration order where they tie)
			ArrayList<Cite> row = under.get( para );
			ArrayList<Integer> idx = at.get( para );
			int pos = row.size();
			while (pos > 0 && idx.get( pos - 1 ) > first) pos--;
			row.add( pos, c );
			idx.add( pos, first );
		}

		int accent = node.color;
		int order = 0;
		float y = 0;
		if (!lead.isEmpty()){
			y = chips( content, lead, accent, width, 0, order ) + 5;
			order += lead.size();
		}
		//the gap a blank line left between paragraphs of one block: a line's height and its spacing
		RenderedTextBlock probe = PixelScene.renderTextBlock( "A", 6 );
		float blank = probe.height() + 4;
		probe.destroy();

		for (int i = 0; i < paras.size(); i++){
			RenderedTextBlock block = PixelScene.renderTextBlock( 6 );
			block.text( paras.get( i ), width );
			block.setPos( 0, y );
			content.add( block );
			y = block.bottom();
			boolean last = i == paras.size() - 1;
			//figures or scenes under it: the next paragraph comes 5 px under them, else a blank line under it
			boolean below = false;
			if (!under.get( i ).isEmpty()){
				y = chips( content, under.get( i ), accent, width, y + 3, order );
				order += under.get( i ).size();
				below = true;
			}
			for (Demo d : acted.get( i )){
				y = stage( content, d, accent, width, y + (below ? 5 : 4) );
				below = true;
			}
			if (!last) y += below ? 5 : blank;
		}
		return y + 2;
	}

	//a scene acting the paragraph out, its stage centred on the page; returns its bottom
	private float stage( Component content, Demo d, int accent, int width, float top ){
		DemoStage s = new DemoStage( d, accent, width );
		s.setPos( 0, (int)Math.ceil( top ) );
		content.add( s );
		stages.add( s );
		return s.bottom();
	}

	//the body's paragraphs; a paragraph with an odd number of '_' runs into the next, so a
	//highlight never breaks
	private static ArrayList<String> paragraphs( String body ){
		ArrayList<String> out = new ArrayList<>();
		String held = null;
		for (String p : body.split( "\n\n" )){
			held = held == null ? p : held + "\n\n" + p;
			if (underscores( held ) % 2 == 0){
				if (!held.trim().isEmpty()) out.add( held );
				held = null;
			}
		}
		if (held != null && !held.trim().isEmpty()) out.add( held );
		return out;
	}

	private static int underscores( String s ){
		int n = 0;
		for (int i = 0; i < s.length(); i++) if (s.charAt( i ) == '_') n++;
		return n;
	}

	//a calm left-aligned flow of chips from `top`, 4 px apart, rows 3 px apart; returns its bottom
	private float chips( Component content, ArrayList<Cite> row, int accent, int width, float top, int order ){
		CiteChip[] made = new CiteChip[row.size()];
		int[] widths = new int[made.length];
		for (int i = 0; i < made.length; i++){
			made[i] = new CiteChip( row.get( i ), accent );
			made[i].maxWidth( width );
			widths[i] = (int)Math.ceil( made[i].width() );
		}
		int[][] at = GuideLayout.flow( widths, width, 4 );
		int rows = at[made.length - 1][1] + 1;
		int[] rowH = new int[rows];
		for (int i = 0; i < made.length; i++){
			rowH[at[i][1]] = Math.max( rowH[at[i][1]], (int)Math.ceil( made[i].height() ) );
		}
		int[] rowY = new int[rows];
		rowY[0] = (int)Math.ceil( top );
		for (int r = 1; r < rows; r++) rowY[r] = rowY[r - 1] + rowH[r - 1] + 3;
		for (int i = 0; i < made.length; i++){
			made[i].setPos( at[i][0], rowY[at[i][1]] );
			made[i].appear( 0.04f * (order + i) );
			content.add( made[i] );
			chips.add( made[i] );
		}
		return rowY[rows - 1] + rowH[rows - 1];
	}

	//--- taps, culling, closing ---

	private void tapAt( float x, float y ){
		for (int i = 0; i < stages.size(); i++){
			DemoStage s = stages.get( i );
			if (s.visible && s.inside( x, y )){
				s.tap();
				return;
			}
		}
		for (CiteChip c : chips){
			if (c.visible && c.inside( x, y )){
				c.tap();
				return;
			}
		}
		for (int i = 0; i < dropRows.size(); i++){
			float[] r = dropRows.get( i );
			if (y >= r[0] && y < r[1]){
				showDrop( dropInfos.get( i ) );
				return;
			}
		}
	}

	@Override
	public synchronized void update() {
		super.update();
		cull();
	}

	//only the chips and scenes in the scrolled view (4 px either side) live: off it, their
	//creatures, particles and motion stop. A scene coming back into view plays from its start
	private void cull(){
		float top = pane.content().camera.scroll.y, bottom = top + pane.height();
		for (CiteChip c : chips){
			boolean on = c.bottom() + 4 >= top && c.top() - 4 <= bottom;
			c.active = c.visible = on;
		}
		for (int i = 0; i < stages.size(); i++){
			DemoStage s = stages.get( i );
			boolean on = s.bottom() + 4 >= top && s.top() - 4 <= bottom;
			if (on && !s.visible) s.restart();
			s.active = s.visible = on;
		}
	}

	private void openGuide(){
		hide();
		GuideScene.focusKey = key;
		GuideScene.returnScene = GameScene.class;
		WarpedPixelDungeon.switchScene( GuideScene.class );
	}

	@Override
	public void hide() {
		super.hide();
		if (onClose != null) onClose.call();
	}

	//--- drops ---

	private float addDropRow( Component content, Mob.DropInfo drop, int width, float y ){
		ItemSprite sprite;
		if (drop.item != null) {
			sprite = new ItemSprite(drop.item);
		} else {
			sprite = new ItemSprite(ItemSpriteSheet.SOMETHING, null);
		}

		boolean nameRevealed = drop.slotSeen
				&& (drop.isCategory
					|| drop.item == null
					|| !Catalog.isTracked(drop.item.getClass())
					|| Catalog.isSeen(drop.item.getClass()));

		String label;
		if (!drop.slotSeen) {
			sprite.lightness(0f);
			label = "???";
		} else if (nameRevealed) {
			label = drop.name + " (" + Mob.formatChance(drop.chance) + ")";
		} else {
			label = "??? (" + Mob.formatChance(drop.chance) + ")";
		}

		sprite.x = 1;
		sprite.y = y;
		content.add(sprite);

		RenderedTextBlock text = PixelScene.renderTextBlock(label, 6);
		text.maxWidth(width - 20);
		text.setPos(20, y + (ItemSpriteSheet.SIZE - text.height()) / 2f);
		content.add(text);

		float rowBottom = Math.max(sprite.y + ItemSpriteSheet.SIZE, text.bottom()) + 1;

		if (drop.slotSeen && drop.item != null) {
			dropRows.add(new float[]{ y, rowBottom });
			dropInfos.add(drop);
		}

		return rowBottom;
	}

	private static void showDrop( Mob.DropInfo drop ){
		boolean catalogSeen = drop.isCategory
				|| !Catalog.isTracked(drop.item.getClass())
				|| Catalog.isSeen(drop.item.getClass());

		Image ic = new ItemSprite(drop.item);
		String t;
		String d;
		if (catalogSeen) {
			t = Messages.titleCase(drop.item.name());
			d = drop.item.info();
		} else {
			ic.lightness(0f);
			t = "???";
			d = Messages.get(WndJournal.CatalogTab.class, "not_seen_item");
		}
		WarpedPixelDungeon.scene().addToFront(new WndTitledMessage(ic, t, d));
	}
}
