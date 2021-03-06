/*
(с) 2016 Copyright by vaclav2016, https://github.com/vaclav2016/JCGA-Tetris/

Boost Software License - Version 1.0 - August 17th, 2003

Permission is hereby granted, free of charge, to any person or organization
obtaining a copy of the software and accompanying documentation covered by
this license (the "Software") to use, reproduce, display, distribute,
execute, and transmit the Software, and to prepare derivative works of the
Software, and to permit third-parties to whom the Software is furnished to
do so, all subject to the following:

The copyright notices in the Software and this entire statement, including
the above license grant, this restriction and the following disclaimer,
must be included in all copies of the Software, in whole or in part, and
all derivative works of the Software, unless such copies or derivative
works are solely in the form of machine-executable object code generated by
a source language processor.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
*/

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import onion.cga.CgaEngine;
import onion.cga.CgaFont;
import onion.cga.CgaGame;
import onion.cga.CgaKey;
import onion.cga.CgaMouseClick;
import onion.cga.CgaScreen;
import onion.cga.Signal;
import onion.cga.VideoBuffer;

public class Tetris implements CgaGame {

	private static final String WIN_TITLE = "Tetris Video Game";
	private static final String BTN_ROTATE = "Up=Rotate";
	private static final String BTN_EXIT = "Esc=Exit";
	private static final String BTN_DROP = "Spc=Drop";
	private static final String BTN_RIGHT = ">>";
	private static final String BTN_LEFT = "<<";

	final static private String VIDEO_GAME_MSG = "VIDEO GAME";
	final private static String PRESS_SPACE_MSG = "Press SPACE";

	private static final long DELAY_STEP = 25L;
	private static final long INITIAL_DELAY = 500L;
	private static final long DELAY_ON_REMOVE_LINE = 80L;
	private static final long DELAY_DROP_DOWN_MODE = 80L;
	private static final int BLOCK_SIZE = 9;

	final static private String[] FIG1 = new String[] { "    ", " *  ", "*** ", "    " };
	final static private String[] FIG2 = new String[] { "    ", "  * ", "*** ", "    " };
	final static private String[] FIG3 = new String[] { "    ", "  * ", "*** ", "    " };
	final static private String[] FIG4 = new String[] { "    ", "****", "    ", "    " };
	final static private String[] FIG5 = new String[] { "    ", " ** ", " ** ", "    " };
	final static private String[] FIG6 = new String[] { "    ", " ** ", "  **", "    " };
	final static private String[] FIG7 = new String[] { "    ", " ** ", "**  ", "    " };

	final static private String END_GAME_MSG = "*** YOUR GAME IS OVER ***";
	final static private String[] TITLE = new String[] { "######  ####  ######  ####   ###   ###", "  ##    #       ##    #   #   #   #", "  ##    ###     ##    ####    #    ###",
			"  ##    #       ##    # #     #       #", "  ##    ####    ##    #  ##  ###  ####" };

	final static private List<String[]> FIGURES;
	static {
		final List<String[]> a = new ArrayList<String[]>();
		a.add(FIG1);
		a.add(FIG2);
		a.add(FIG3);
		a.add(FIG4);
		a.add(FIG5);
		a.add(FIG6);
		a.add(FIG7);
		FIGURES = Collections.unmodifiableList(a);
	}

	final private static String[] SCORE_MSG = new String[] { "Score", "", "Lines", "", "Level", "" };

	private char[][] nextFigure;
	private char[][] curFigure;
	private char[][] tmpFigure;

	private CgaScreen screen;
	private CgaFont font;
	private Signal screenUpdateSignal;

	private long delay;
	private long score;
	private long lines;
	private long level;
	private boolean end;
	private List<VideoBuffer> overlords;
	private List<TouchButton> buttons;

	final private Queue<CgaKey> keyCodes = new ConcurrentLinkedQueue<CgaKey>();
	final private Queue<CgaMouseClick> cgaMouseClicks = new ConcurrentLinkedQueue<CgaMouseClick>();

	final private char[][] field = new char[20][10];

	@Override
	public void run() {
		try {
			this.nextFigure = new char[4][];
			this.curFigure = new char[4][];
			this.tmpFigure = new char[4][];
			for (int i = 0; i < 4; i++) {
				this.nextFigure[i] = new char[4];
				this.curFigure[i] = new char[4];
				this.tmpFigure[i] = new char[4];
			}
			this.loadResources();
			this.welcomeScreen();
			while (true) {
				this.resetGame();
				this.playGame();
				this.endGame();
			}
		} catch (final InterruptedException e) {
			System.exit(0);
		}
	}

	private void loadResources() {
		try {
			this.buttons = new ArrayList<TouchButton>();

			this.buttons.add(new TouchButton(2, 176, 28, 20, BTN_LEFT, new CgaKey(KeyEvent.VK_LEFT, (char) 0)));

			this.buttons.add(new TouchButton(34, 176, 68, 20, BTN_DROP, new CgaKey(0, ' ')));

			this.buttons.add(new TouchButton(248, 70, 70, 20, BTN_EXIT, new CgaKey(KeyEvent.VK_ESCAPE, (char) 0)));
			this.buttons.add(new TouchButton(208, 176, 76, 20, BTN_ROTATE, new CgaKey(KeyEvent.VK_UP, (char) 0)));

			this.buttons.add(new TouchButton(288, 176, 28, 20, BTN_RIGHT, new CgaKey(KeyEvent.VK_RIGHT, (char) 0)));

			this.overlords = new ArrayList<VideoBuffer>();
			this.overlords.add(CgaEngine.loadPcx(Tetris.class.getResourceAsStream("/00.pcx")));

			this.overlords.add(CgaEngine.loadPcx(Tetris.class.getResourceAsStream("/01.pcx")));
			this.overlords.add(CgaEngine.loadPcx(Tetris.class.getResourceAsStream("/02.pcx")));
			this.overlords.add(CgaEngine.loadPcx(Tetris.class.getResourceAsStream("/03.pcx")));

		} catch (final Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private void copyFigure(String[] from, char[][] to) {
		for (int y = 0; y < from.length; y++) {
			for (int x = 0; x < from[y].length(); x++) {
				to[y][x] = from[y].charAt(x);
			}
		}
	}

	private void copyFigure(char[][] from, char[][] to) {
		for (int y = 0; y < from.length; y++) {
			System.arraycopy(from[y], 0, to[y], 0, from[y].length);
		}
	}

	private boolean rotateFigure(int cx, int cy) {
		for (int y = 0; y < this.curFigure.length; y++) {
			for (int x = 0; x < this.curFigure[y].length; x++) {
				this.tmpFigure[this.curFigure[y].length - x - 1][y] = this.curFigure[y][x];
			}
		}
		if (this.checkMove(cx, cy, this.tmpFigure)) {
			this.copyFigure(this.tmpFigure, this.curFigure);
			return true;
		}
		return false;
	}

	private void resetGame() {
		this.delay = INITIAL_DELAY;
		this.score = 0L;
		this.lines = 0L;
		this.end = false;
		this.level = 0;
		this.copyFigure(getRandomFigure(), this.nextFigure);
		this.screen.clear();
		for (int i = 0; i < this.field.length; i++) {
			this.field[i] = new char[10];
			Arrays.fill(this.field[i], ' ');
		}
	}

	private void drawNextFigure() {
		this.screen.drawRectBf(42, 12, (BLOCK_SIZE * this.curFigure.length) + 2, (BLOCK_SIZE * this.curFigure.length) + 2, CgaEngine.CGA1_WHITE, CgaEngine.CGA1_BLACK);
		for (int y = 0; y < this.nextFigure.length; y++) {
			for (int x = 0; x < this.nextFigure.length; x++) {
				if (this.nextFigure[y][x] != ' ') {
					this.drawFieldBox(43 + (x * BLOCK_SIZE), 13 + (y * BLOCK_SIZE), BLOCK_SIZE, BLOCK_SIZE, false);
				}
			}
		}
	}

	private void drawFieldBox(int x, int y, int w, int h, boolean isFigure) {
		this.screen.drawRectF(x, y, w - 1, h - 1, isFigure ? CgaEngine.CGA1_MAGENTA : CgaEngine.CGA1_CYAN);
		this.screen.drawHLine(x, y, w - 1, CgaEngine.CGA1_WHITE);
		this.screen.drawVLine(x, y, h - 1, CgaEngine.CGA1_WHITE);
	}

	private void drawField(int stx, int sty, int w, int h) {
		this.screen.drawRectF(stx, sty, this.field[0].length * w, this.field.length * h, CgaEngine.CGA1_BLACK);
		this.screen.drawVLine(stx - 1, sty, this.field.length * h, CgaEngine.CGA1_WHITE);
		this.screen.drawVLine((stx + (this.field[0].length * w)) - 1, sty, this.field.length * h, CgaEngine.CGA1_WHITE);
		this.screen.drawHLine(stx - 1, (sty + (this.field.length * h)) - 1, this.field[0].length * w, CgaEngine.CGA1_WHITE);
		for (int y = 0; y < this.field.length; y++) {
			int nonEmpty = 0;
			for (int x = 0; x < this.field[y].length; x++) {
				if (this.field[y][x] != ' ') {
					nonEmpty++;
				}
			}
			if (nonEmpty == this.field[y].length) {
				this.screen.drawRectF(stx, sty + (y * h), (w * this.field[y].length) - 1, h - 1, CgaEngine.CGA1_WHITE);
			} else {
				for (int x = 0; x < this.field[y].length; x++) {
					if (this.field[y][x] != ' ') {
						this.drawFieldBox(stx + (x * w), sty + (y * h), w, h, false);
					}
				}
			}
		}
	}

	private void drawBlocks() {
		this.screen.fill(CgaEngine.CGA1_MAGENTA);
		int y = 0;
		while (y < CgaScreen.HEIGHT) {
			this.screen.drawHLine(0, y, CgaScreen.WIDTH, CgaEngine.CGA1_WHITE);
			y += 8;
		}
		y = 0;
		while (y < CgaScreen.HEIGHT) {
			int x = ((y / 8) & 1) == 0 ? 0 : 8;
			while (x < CgaScreen.WIDTH) {
				this.screen.drawVLine(x, y, 8, CgaEngine.CGA1_WHITE);
				x += 16;
			}
			y += 8;
		}
	}

	private static String[] getRandomFigure() {
		final int i = (int) (FIGURES.size() * Math.random());
		return FIGURES.get(i);
	}

	private void drawScore() {

		SCORE_MSG[1] = "" + this.score;
		SCORE_MSG[3] = "" + this.lines;
		SCORE_MSG[5] = "" + this.level;

		this.screen.drawRectBf(250, 2, 319 - 250, (8 * SCORE_MSG.length) + 4, CgaEngine.CGA1_WHITE, CgaEngine.CGA1_BLACK);
		int y = 4;
		for (int i = 0; i < SCORE_MSG.length; i++) {
			this.screen.drawStr(this.font, (250 + ((319 - 250) / 2)) - ((SCORE_MSG[i].length() * 8) / 2), y, SCORE_MSG[i],
					(i & 1) == 0 ? CgaEngine.CGA1_WHITE : CgaEngine.CGA1_MAGENTA);
			y += 8;
		}
	}

	private void playGame() throws InterruptedException {
		this.level++;
		int figures = 0;
		while (!this.end) {
			this.cgaMouseClicks.clear();
			boolean dropDownMode = false;
			this.keyCodes.clear();
			int y = 0;
			int x = 3;
			this.copyFigure(this.nextFigure, this.curFigure);
			this.copyFigure(getRandomFigure(), this.nextFigure);
			figures++;
			if (figures == 10) {
				figures = 0;
				this.level++;
				this.delay -= DELAY_STEP;
			}
			this.drawBlocks();
			this.drawNextFigure();
			this.drawScore();
			for (final TouchButton btn : this.buttons) {
				this.drawButton(btn.getX(), btn.getY(), btn.getDx(), btn.getDy(), btn.getText());
			}

			final VideoBuffer curTotem = this.overlords.get(1 + (int) ((this.level - 1) % (this.overlords.size() - 1)));
			this.screen.copySpriteFrom(curTotem, 12, 64);

			long prevStep = System.currentTimeMillis();
			boolean needredraw = true;
			while (this.checkMove(x, y + 1, this.curFigure)) {
				if (!this.keyCodes.isEmpty()) {
					final CgaKey key = this.keyCodes.poll();
					switch (key.getKeyCode()) {
					case KeyEvent.VK_LEFT:
						if (this.checkMove(x - 1, y, this.curFigure)) {
							needredraw = true;
							x--;
						}
						break;
					case KeyEvent.VK_RIGHT:
						if (this.checkMove(x + 1, y, this.curFigure)) {
							needredraw = true;
							x++;
						}
						break;
					case KeyEvent.VK_UP:
					case KeyEvent.VK_ENTER:
						needredraw = needredraw || this.rotateFigure(x, y);
						break;
					case KeyEvent.VK_ESCAPE:
						throw new InterruptedException();
					}
					if (!dropDownMode && (key.getKeyChar() == ' ')) {
						this.score++;
						dropDownMode = true;
					}
				}
				if (dropDownMode) {
					if ((System.currentTimeMillis() - prevStep) > DELAY_DROP_DOWN_MODE) {
						needredraw = true;
						y++;
						prevStep = System.currentTimeMillis();
					}
				} else if ((System.currentTimeMillis() - prevStep) > this.delay) {
					needredraw = true;
					prevStep = System.currentTimeMillis();
					y++;
				}
				if (needredraw) {
					this.drawField((320 / 2) - BLOCK_SIZE * 5, 2, BLOCK_SIZE, BLOCK_SIZE);
					this.drawCurFigure((320 / 2) - BLOCK_SIZE * 5, 2, BLOCK_SIZE, BLOCK_SIZE, x, y);
					this.screenUpdateSignal.setSignal();
					needredraw = false;
				}
				if (this.delay > 100L) {
					Thread.sleep(this.delay / 4);
				} else if (this.delay > 20L) {
					Thread.sleep(this.delay / 2);
				}
			}
			this.addFigureToField(x, y);
			this.removeLines();
			this.checkEndGame();
		}
	}

	private void drawCurFigure(int stx, int sty, int w, int h, int x, int y) {
		for (int fy = 0; fy < this.curFigure.length; fy++) {
			for (int fx = 0; fx < this.curFigure[fy].length; fx++) {
				if (this.curFigure[fy][fx] != ' ') {
					this.drawFieldBox(stx + ((x + fx) * w), sty + ((y + fy) * h), w, h, false);
				}
			}
		}
	}

	private void addFigureToField(int x, int y) {
		for (int fy = 0; fy < this.curFigure.length; fy++) {
			for (int fx = 0; fx < this.curFigure[fy].length; fx++) {
				if (this.curFigure[fy][fx] != ' ') {
					this.field[y + fy][x + fx] = this.curFigure[fy][fx];
				}
			}
		}
	}

	private boolean checkMove(int x, int y, char[][] fig) {
		for (int ay = 0; ay < fig.length; ay++) {
			for (int ax = 0; ax < fig[ay].length; ax++) {
				if (fig[ay][ax] != ' ') {
					if (((x + ax) >= this.field[y].length) || ((x + ax) < 0)) {
						return false;
					}
					if (((y + ay) >= this.field.length) || ((y + ay) < 0)) {
						return false;
					}
					if (this.field[y + ay][x + ax] != ' ') {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void removeLines() throws InterruptedException {
		int linesToRemove = 0;
		for (int y = 0; y < this.field.length; y++) {
			int nonEmpty = 0;
			for (int x = 0; x < this.field[y].length; x++) {
				if (this.field[y][x] != ' ') {
					nonEmpty++;
				}
			}
			if (nonEmpty == this.field[y].length) {
				linesToRemove++;
				for (int i = y; i > 0; i--) {
					System.arraycopy(this.field[i - 1], 0, this.field[i], 0, this.field[i - 1].length);
				}
				Arrays.fill(this.field[0], ' ');
				this.drawField((320 / 2) - BLOCK_SIZE * 5, 2, BLOCK_SIZE, BLOCK_SIZE);
				this.screenUpdateSignal.setSignal();
				Thread.sleep(DELAY_ON_REMOVE_LINE);
			}
		}
		if (linesToRemove > 0) {
			this.lines += linesToRemove;
			if (linesToRemove == 4) {
				this.score += linesToRemove * 15;
			} else if (linesToRemove > 1) {
				this.score += linesToRemove * 10;
			} else {
				this.score += 5;
			}
		}
	}

	private void checkEndGame() {
		for (int i = 0; i < this.field[4].length; i++) {
			if (this.field[1][i] != ' ') {
				this.end = true;
				return;
			}
		}
	}

	private void endGame() throws InterruptedException {
		this.drawBlocks();
		this.drawScore();

		this.screen.drawStr(this.font, 2, 2, "anarchy3fa3bcada.onion", CgaEngine.CGA1_CYAN);
		// overlord
		this.screen.copyFrom(this.overlords.get(0), (CgaScreen.WIDTH / 2) - (this.overlords.get(0).getWidth() / 2), 90);
		this.screen.copyFrom(this.overlords.get(0), 20, 90);
		this.screen.copyFrom(this.overlords.get(0), CgaScreen.WIDTH - 20 - this.overlords.get(0).getWidth(), 90);

		this.screen.drawRectBf(0, 70, CgaScreen.WIDTH - 1, 13, CgaEngine.CGA1_WHITE, CgaEngine.CGA1_BLACK);
		this.screen.drawStr(this.font, (320 / 2) - ((END_GAME_MSG.length() * 8) / 2), 73, END_GAME_MSG, CgaEngine.CGA1_WHITE);
		this.pressSpace();
	}

	private void pressSpace() throws InterruptedException {

		this.screen.drawRectF(0, 200 - 12, CgaScreen.WIDTH, 12, CgaEngine.CGA1_BLACK);
		this.screen.drawHLine(0, 200 - 12, CgaScreen.WIDTH, CgaEngine.CGA1_WHITE);

		this.keyCodes.clear();
		this.cgaMouseClicks.clear();
		boolean v = false;
		while (true) {

			if (!this.cgaMouseClicks.isEmpty()) {
				this.cgaMouseClicks.poll();
				break;
			}

			if (this.keyCodes.isEmpty()) {
				Thread.sleep(500L);
				v = !v;
				this.screen.drawStr(this.font, (320 / 2) - ((PRESS_SPACE_MSG.length() * 8) / 2), 200 - 9, PRESS_SPACE_MSG, v ? CgaEngine.CGA1_WHITE : CgaEngine.CGA1_BLACK);
				this.screenUpdateSignal.setSignal();
				continue;
			}
			final CgaKey keyCode = this.keyCodes.poll();
			if (keyCode.getKeyChar() == ' ') {
				break;
			}

			if (keyCode.getKeyCode() == KeyEvent.VK_ESCAPE) {
				throw new InterruptedException();
			}
		}
	}

	private void drawTitle(String[] title, int stx, int sty) {
		for (int y = 0; y < title.length; y++) {
			for (int x = 0; x < title[y].length(); x++) {
				if (title[y].charAt(x) != ' ') {
					this.drawFieldBox(stx + (x * 8), sty + (y * 8), 8, 8, false);
				}
			}
		}
	}

	private void drawButton(int stx, int sty, int dx, int dy, String text) {
		screen.drawRectBf(stx, sty, dx, dy, CgaEngine.CGA1_WHITE, CgaEngine.CGA1_CYAN);
		this.screen.drawStr(this.font, stx + dx / 2 - text.length() * 4, sty + dy / 2 - 4, text, CgaEngine.CGA1_WHITE);
	}

	private void welcomeScreen() throws InterruptedException {

		this.screen.clear();
		this.drawBlocks();
		this.drawTitle(TITLE, 4, 60);

		this.screen.drawStr(this.font, (this.screen.getWidth() / 2) - ((VIDEO_GAME_MSG.length() * 8) / 2), 130, VIDEO_GAME_MSG, CgaEngine.CGA1_CYAN);

		this.screen.drawStr(this.font, 110, 200 - 12 - 24, "     '*`", CgaEngine.CGA1_WHITE);
		this.screen.drawStr(this.font, 110, 200 - 12 - 16, "    (o o)", CgaEngine.CGA1_WHITE);
		this.screen.drawStr(this.font, 110, 200 - 12 - 8, "ooO--(_)--Ooo", CgaEngine.CGA1_WHITE);

		this.screenUpdateSignal.setSignal();
		this.pressSpace();
	}

	@Override
	public void setCgaScreen(CgaScreen screen) {
		this.screen = screen;
	}

	@Override
	public void setScreenUpdateSignal(Signal screenUpdateSignal) {
		this.screenUpdateSignal = screenUpdateSignal;
	}

	@Override
	public void setCgaFont(CgaFont font) {
		this.font = font;
	}

	@Override
	public String getTitle() {
		return WIN_TITLE;
	}

	@Override
	public void onKeyEvent(CgaKey cgaKey) {
		this.keyCodes.offer(cgaKey);
	}

	@Override
	public void onMouseEvent(CgaMouseClick cgaMouseClick) {
		if (!CgaMouseClick.Type.CLICK.equals(cgaMouseClick.getType())) {
			return;
		}

		final int x = cgaMouseClick.getX();
		final int y = cgaMouseClick.getY();

		for (final TouchButton btn : this.buttons) {
			if (x >= btn.getX() && x <= btn.getX() + btn.getDx() && y >= btn.getY() && y <= btn.getY() + btn.getDy()) {
				this.keyCodes.offer(btn.getCgaKey());
				return;
			}
		}

		cgaMouseClicks.offer(cgaMouseClick);
	}

	private static class TouchButton {
		private int x;
		private int y;
		private int dx;
		private int dy;
		private String text;
		private CgaKey cgaKey;

		private TouchButton(int x, int y, int dx, int dy, String text, CgaKey cgaKey) {
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.text = text;
			this.cgaKey = cgaKey;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getDx() {
			return dx;
		}

		public int getDy() {
			return dy;
		}

		public String getText() {
			return text;
		}

		public CgaKey getCgaKey() {
			return cgaKey;
		}
	}

	public static void main(String[] args) throws Exception {
		CgaEngine.executeGame(new Tetris());
	}
}
