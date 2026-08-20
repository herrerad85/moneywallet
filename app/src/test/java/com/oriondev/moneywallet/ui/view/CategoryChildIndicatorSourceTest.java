package com.oriondev.moneywallet.ui.view;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The mirroring checked here happens inside onDraw on a real Canvas, so a JVM unit test cannot
 * observe it: there is no Robolectric on the unit test classpath, and proving what is drawn takes
 * a device. The invariant is pinned by reading the source instead, the same way
 * NewEditTransactionActivitySourceTest does.
 *
 * What this asserts is that onDraw still asks the view for its layout direction and still mirrors
 * the canvas when that direction is right to left. That makes it a tripwire against a revert or a
 * rewrite that drops the branch. It is not a proof that the elbow lands on the child's icon, and a
 * rewrite that keeps these tokens and still draws the arm on the wrong side goes through.
 *
 * Comments are stripped before anything is matched, so no assertion here can be satisfied by the
 * wording of a comment describing the code it no longer has. String literals are NOT stripped,
 * unlike the precedent. The file under test has none, and if one is ever added the literal half
 * has to be added here at the same time, because a literal carrying one of the tokens below would
 * satisfy its assertion with the code gone. That is the false pass the precedent strips literals
 * to prevent.
 *
 * The assertions pin exact spellings, so a rewrite that behaves identically can still fail here:
 * c.restore() for c.restoreToCount(), or (width / 2) for width / 2. A failure means read the
 * source and decide, not that the view is broken.
 *
 * Why it is worth pinning: with the branch gone the elbow's horizontal arm runs to the right edge
 * in every layout, so in a right to left language it runs away from the child icon into empty
 * space and the two never meet. Nothing crashes and nothing logs, so the only signal is somebody
 * looking at the screen in a right to left language.
 */
public class CategoryChildIndicatorSourceTest {

    private static final Pattern COMMENTS = Pattern.compile("//.*?$|/[*].*?[*]/", Pattern.DOTALL | Pattern.MULTILINE);

    private String readSource() {
        String path = "src/main/java/com/oriondev/moneywallet/ui/view/CategoryChildIndicator.java";
        File file = new File(path);
        if (!file.exists()) {
            // a runner rooted at the repo root instead of the module, which the precedent
            // handles the same way
            file = new File("app/" + path);
        }
        if (!file.exists()) {
            fail("Cannot find CategoryChildIndicator.java at " + file.getAbsolutePath());
        }
        try {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return COMMENTS.matcher(source).replaceAll(" ");
        } catch (IOException e) {
            fail("Cannot read CategoryChildIndicator.java: " + e.getMessage());
            return null;
        }
    }

    @Test
    public void onDrawMirrorsTheCanvasInARightToLeftLayout() {
        String source = readSource();
        int onDraw = source.indexOf("public void onDraw(");
        assertTrue("onDraw is gone", onDraw >= 0);
        String body = source.substring(onDraw);
        assertTrue("onDraw no longer reads the layout direction",
                body.contains("getLayoutDirection() == LAYOUT_DIRECTION_RTL"));
        assertTrue("onDraw no longer carries the exact call scale(-1f, 1f, width / 2, ...). Either the "
                        + "mirror is gone, or the pivot is no longer the integer width / 2, which would "
                        + "shift the elbow a pixel at odd widths, or it was respelled",
                body.contains("scale(-1f, 1f, width / 2,"));
        assertTrue("onDraw no longer carries the exact pair = c.save() and restoreToCount(. Either the "
                        + "canvas is left mirrored for onDrawForeground, or it was respelled",
                body.contains("= c.save()") && body.contains("restoreToCount("));
    }
}
