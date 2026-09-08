package com.oriondev.moneywallet.ui.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Draws the elbow onto a real bitmap under Robolectric's native graphics and reads the pixels
 * back, because where the arm lands is the whole point of the mirror in onDraw and nothing short
 * of drawn pixels can tell which side it ran to.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CategoryChildIndicatorTest {

    // an odd width, because the pivot of the mirror is the integer width / 2 and only an odd
    // width can tell that apart from width / 2f
    private static final int WIDTH = 41;
    private static final int HEIGHT = 40;

    // onDraw uses lineSize 8, so the vertical stroke runs from (41 / 2) - 4 = 16 to 24 and the
    // arm band runs from (40 / 2) + 4 = 24 to 32
    private static final int STROKE_FIRST_COLUMN = 16;
    private static final int STROKE_LAST_COLUMN = 23;
    private static final int ROW_ABOVE_THE_ARM = 10;
    private static final int ROW_INSIDE_THE_ARM = 27;
    private static final int ROW_BELOW_THE_ARM = 35;
    private static final int CENTER_COLUMN = 20;

    private static final int LINE = Color.RED;

    private CategoryChildIndicator indicator(int layoutDirection, boolean last) {
        CategoryChildIndicator view =
                new CategoryChildIndicator(ApplicationProvider.getApplicationContext());
        view.setLineColor(LINE);
        view.setLast(last);
        view.setLayoutDirection(layoutDirection);
        view.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, WIDTH, HEIGHT);
        assertEquals("the view did not resolve the layout direction it was given, so the case "
                + "below would run against the wrong one", layoutDirection, view.getLayoutDirection());
        assertNotEquals(0, view.getMeasuredWidth());
        assertNotEquals(0, view.getMeasuredHeight());
        return view;
    }

    private Bitmap drawn(CategoryChildIndicator view) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    /** The columns of one row that carry the line color, as a string of 41 dots and hashes. */
    private String paintedColumns(Bitmap bitmap, int row) {
        StringBuilder columns = new StringBuilder();
        for (int x = 0; x < WIDTH; x++) {
            columns.append(bitmap.getPixel(x, row) == LINE ? '#' : '.');
        }
        return columns.toString();
    }

    @Test
    public void inALeftToRightLayoutTheArmRunsToTheRightEdge() {
        Bitmap bitmap = drawn(indicator(View.LAYOUT_DIRECTION_LTR, true));
        assertEquals("the arm does not reach the right edge",
                LINE, bitmap.getPixel(WIDTH - 1, ROW_INSIDE_THE_ARM));
        assertEquals("the arm reaches the left edge", 0, bitmap.getPixel(0, ROW_INSIDE_THE_ARM));
        assertEquals("the vertical stroke is not on the center column",
                LINE, bitmap.getPixel(CENTER_COLUMN, ROW_ABOVE_THE_ARM));
        assertEquals(0, bitmap.getPixel(0, ROW_ABOVE_THE_ARM));
        assertEquals(0, bitmap.getPixel(WIDTH - 1, ROW_ABOVE_THE_ARM));
    }

    @Test
    public void inARightToLeftLayoutTheArmRunsToTheLeftEdge() {
        Bitmap bitmap = drawn(indicator(View.LAYOUT_DIRECTION_RTL, true));
        assertEquals("the arm does not reach the left edge, so it runs away from the child icon",
                LINE, bitmap.getPixel(0, ROW_INSIDE_THE_ARM));
        assertEquals("the arm still reaches the right edge",
                0, bitmap.getPixel(WIDTH - 1, ROW_INSIDE_THE_ARM));
        assertEquals("the vertical stroke is not on the center column",
                LINE, bitmap.getPixel(CENTER_COLUMN, ROW_ABOVE_THE_ARM));
        assertEquals(0, bitmap.getPixel(0, ROW_ABOVE_THE_ARM));
        assertEquals(0, bitmap.getPixel(WIDTH - 1, ROW_ABOVE_THE_ARM));
    }

    @Test
    public void theVerticalStrokeSitsOnTheSameColumnsInBothDirections() {
        StringBuilder expected = new StringBuilder();
        for (int x = 0; x < WIDTH; x++) {
            expected.append(x >= STROKE_FIRST_COLUMN && x <= STROKE_LAST_COLUMN ? '#' : '.');
        }
        String leftToRight = paintedColumns(
                drawn(indicator(View.LAYOUT_DIRECTION_LTR, true)), ROW_ABOVE_THE_ARM);
        String rightToLeft = paintedColumns(
                drawn(indicator(View.LAYOUT_DIRECTION_RTL, true)), ROW_ABOVE_THE_ARM);
        assertEquals(expected.toString(), leftToRight);
        assertEquals("the mirror moved the vertical stroke instead of mapping it onto itself, "
                + "which is what a pivot other than the integer width / 2 does at an odd width",
                leftToRight, rightToLeft);
    }

    @Test
    public void theStrokeStopsAtTheArmForALastChildAndRunsThroughForAMiddleOne() {
        assertEquals("a last child still draws below the arm",
                0, drawn(indicator(View.LAYOUT_DIRECTION_LTR, true))
                        .getPixel(CENTER_COLUMN, ROW_BELOW_THE_ARM));
        assertEquals("a middle child stops at the arm, so the next child below is not linked",
                LINE, drawn(indicator(View.LAYOUT_DIRECTION_LTR, false))
                        .getPixel(CENTER_COLUMN, ROW_BELOW_THE_ARM));
    }

    @Test
    public void theCanvasIsLeftUnmirroredForWhateverDrawsAfterIt() {
        CategoryChildIndicator view = indicator(View.LAYOUT_DIRECTION_RTL, true);
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int before = canvas.getSaveCount();
        view.draw(canvas);
        assertEquals("onDraw left a save on the canvas, so onDrawForeground and anything else "
                + "drawing after it stays mirrored", before, canvas.getSaveCount());
        // the last two rows are untouched by a last child, so a marker drawn on them reads back
        // the transform the canvas was handed on
        Paint marker = new Paint();
        marker.setColor(Color.GREEN);
        canvas.drawRect(0, HEIGHT - 2, 4, HEIGHT - 1, marker);
        assertEquals("the canvas is still mirrored for whatever draws after onDraw",
                Color.GREEN, bitmap.getPixel(0, HEIGHT - 2));
        assertEquals(0, bitmap.getPixel(WIDTH - 1, HEIGHT - 2));
    }
}
