/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.moneywallet.background;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The three report loaders each build their own selection string, so the rule about a category
 * kept out of the reports lives in one constant and is appended three times. SQLDatabaseTest
 * covers what that constant selects. Nothing covers whether a loader still appends it, and a
 * loader that stops puts back the defect where one period reads differently on the overview list,
 * on the two flow tabs and on the summary tab.
 *
 * This reads the source, so it is a tripwire against a revert and not a proof of behavior. It
 * cannot see a loader that appends the filter and then reassigns selection underneath it, or one
 * that builds a string it never queries with, because no loader is executed here.
 *
 * Comments are stripped before anything is matched, so the assertion cannot be satisfied by a
 * comment naming the constant the loader no longer appends. String literals are NOT stripped,
 * unlike the precedent, because the statement being matched contains one. Whitespace is collapsed,
 * so the statement may be wrapped, but the spelling is pinned exactly and a respelling fails with
 * no defect present. A failure means read the loader and decide.
 */
public class ReportLoaderSourceTest {

    private static final Pattern COMMENTS = Pattern.compile("//.*?$|/[*].*?[*]/", Pattern.DOTALL | Pattern.MULTILINE);

    private static final String[] LOADERS = new String[] {
            "OverviewDataLoader",
            "PeriodDetailFlowLoader",
            "PeriodDetailSummaryLoader"
    };

    private static final String APPEND =
            "selection += \" AND \" + Contract.Transaction.REPORT_FILTER;";

    @Test
    public void everyReportLoaderAppliesTheReportFilter() {
        for (String loader : LOADERS) {
            assertTrue("a report screen that stops filtering on REPORT_FILTER counts a category "
                    + "kept out of the reports again, and disagrees with the other two: " + loader,
                    readSource(loader).contains(APPEND));
        }
    }

    private String readSource(String loader) {
        String path = "src/main/java/com/oriondev/moneywallet/background/" + loader + ".java";
        File file = new File(path);
        if (!file.exists()) {
            // a runner rooted at the repo root instead of the module, which the precedent
            // handles the same way
            file = new File("app/" + path);
        }
        if (!file.exists()) {
            fail("Cannot find " + loader + ".java at " + file.getAbsolutePath());
        }
        try {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return COMMENTS.matcher(source).replaceAll(" ").replaceAll("\\s+", " ");
        } catch (IOException e) {
            fail("Cannot read " + loader + ".java: " + e.getMessage());
            return null;
        }
    }

}
