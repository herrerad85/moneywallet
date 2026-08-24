package com.oriondev.moneywallet.ui.view.text;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The line this pins is drawn inside onDraw on a real Canvas, so a JVM unit test cannot observe it:
 * there is no Robolectric on the unit test classpath. The invariant is pinned by reading the source
 * instead, the same way CategoryChildIndicatorSourceTest does.
 *
 * What it asserts is that the two statements setting the underline's edges still read as they read
 * now. The pair looks like a typo: one adds the slot the cancel button reserves and the other takes
 * it away. Both are correct, because each reaches outward from its own side, and making them agree
 * puts the button outside the line it is meant to sit on, whichever way they are made to agree.
 * Nothing on the JVM catches that, because the value is only ever compared against pixels on a
 * device.
 *
 * Comments are stripped before anything is matched, so neither assertion can be satisfied by the
 * wording of a comment describing code that is gone. String literals are not stripped; the only ones
 * in the file under test are empty, and if one is ever added that carries either statement below,
 * the literal half has to be stripped here at the same time.
 *
 * The assertions pin exact spellings, so a rewrite that behaves identically can still fail here, and
 * a rewrite that keeps both statements and stops drawing with them goes through. A failure means
 * read the source and decide, not that the view is broken.
 */
public class MaterialEditTextSourceTest {

    private static final Pattern COMMENTS = Pattern.compile("//.*?$|/[*].*?[*]/", Pattern.DOTALL | Pattern.MULTILINE);

    private String readSource() {
        String path = "src/main/java/com/oriondev/moneywallet/ui/view/text/MaterialEditText.java";
        File file = new File(path);
        if (!file.exists()) {
            // a runner rooted at the repo root instead of the module, which the precedent
            // handles the same way
            file = new File("app/" + path);
        }
        if (!file.exists()) {
            fail("Cannot find MaterialEditText.java at " + file.getAbsolutePath());
        }
        try {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return COMMENTS.matcher(source).replaceAll(" ");
        } catch (IOException e) {
            fail("Cannot read MaterialEditText.java: " + e.getMessage());
            return null;
        }
    }

    @Test
    public void theBottomLineReachesBackOverTheCancelButtonOnBothSides() {
        String source = readSource();
        int onDrawBottomLine = source.indexOf("protected void onDrawBottomLine(");
        assertTrue("the file no longer carries the literal protected void onDrawBottomLine(. The method "
                + "is gone, or its header was respelled", onDrawBottomLine >= 0);
        int end = source.indexOf("protected void onDrawCancelButton(", onDrawBottomLine);
        assertTrue("the literal protected void onDrawCancelButton( no longer appears after that header, so "
                + "this reader cannot tell where the method it is reading ends. The method is gone or moved, or "
                + "its own header was respelled", end > onDrawBottomLine);
        String body = source.substring(onDrawBottomLine, end);
        assertTrue("the left edge no longer reads exactly int left = getPaddingLeft() + getScrollX() "
                        + "- (mShowCancelButton && isRtl() ? getPixels(CANCEL_BUTTON_PADDING_DP) : 0); If it adds "
                        + "the slot instead of subtracting it, the line starts inside the field in a right to left "
                        + "layout and the cancel button ends up outside it. Read the source and decide",
                body.contains("int left = getPaddingLeft() + getScrollX() "
                        + "- (mShowCancelButton && isRtl() ? getPixels(CANCEL_BUTTON_PADDING_DP) : 0);"));
        assertTrue("the right edge no longer reads exactly int right = getScrollX() + getMeasuredWidth() "
                        + "- getPaddingRight() + (mShowCancelButton  && !isRtl()? getPixels(CANCEL_BUTTON_PADDING_DP) "
                        + ": 0); If it subtracts the slot instead of adding it, the line ends inside the field in a "
                        + "left to right layout and the cancel button ends up outside it. Read the source and decide",
                body.contains("int right = getScrollX() + getMeasuredWidth() - getPaddingRight() "
                        + "+ (mShowCancelButton  && !isRtl()? getPixels(CANCEL_BUTTON_PADDING_DP) : 0);"));
    }
}
