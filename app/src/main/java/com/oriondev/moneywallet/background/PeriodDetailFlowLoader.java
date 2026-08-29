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

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.oriondev.moneywallet.model.Category;
import com.oriondev.moneywallet.model.CategoryMoney;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.Money;
import com.oriondev.moneywallet.model.PeriodDetailFlowData;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.view.chart.PieData;
import com.oriondev.moneywallet.ui.view.chart.PieSlice;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateUtils;
import com.oriondev.moneywallet.utils.IconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by andrea on 13/08/18.
 */
public class PeriodDetailFlowLoader extends AbstractGenericLoader<PeriodDetailFlowData> {

    /*
    private static final int[] mColors = new int[] {
            Color.rgb(204, 198, 24),
            Color.rgb(229, 163, 25),
            Color.rgb(232, 111, 40),
            Color.rgb(212, 75, 145),
            Color.rgb(117, 96, 165),
            Color.rgb(54, 142, 92),
            Color.rgb(129, 191, 22),
            Color.rgb(224, 184, 26),
            Color.rgb(229, 138, 24),
            Color.rgb(235, 89, 92),
            Color.rgb(167, 78, 160),
            Color.rgb(66, 117, 138),
            Color.rgb(85, 169, 48)
    };*/

    private final Date mStartDate;
    private final Date mEndDate;
    private final boolean mIncomes;

    public PeriodDetailFlowLoader(Context context, Date startDate, Date endDate, boolean incomes) {
        super(context);
        mStartDate = startDate;
        mEndDate = endDate;
        mIncomes = incomes;
    }

    @Override @SuppressLint("UseSparseArrays")
    public PeriodDetailFlowData loadInBackground() {
        Money totalMoney = new Money();
        Map<CurrencyUnit, PieData> pieDataSets = new HashMap<>();
        Map<Long, CategoryMoney> categoryMoneyMap = new HashMap<>();
        Map<Long, Map<Long, CategoryMoney>> childMoneyMap = new HashMap<>();
        Map<Long, Money> directMoneyMap = new HashMap<>();
        // load from content resolver
        Map<Long, Category> categoryCache = loadCategoryCache();
        Map<Long, Category> childCategoryCache = loadChildCategoryCache();
        Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
        String[] projection = new String[] {
                Contract.Transaction.CATEGORY_ID,
                Contract.Transaction.CATEGORY_PARENT_ID,
                Contract.Transaction.MONEY,
                Contract.Transaction.WALLET_CURRENCY
        };
        String selection;
        String[] selectionArgs;
        long currentWallet = PreferenceManager.getCurrentWallet();
        if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
            selection = Contract.Transaction.WALLET_COUNT_IN_TOTAL + " = 1";
            selectionArgs = null;
        } else {
            selection = Contract.Transaction.WALLET_ID + " = ?";
            selectionArgs = new String[] {String.valueOf(currentWallet)};
        }
        selection += " AND " + Contract.Transaction.CONFIRMED + " = '1' AND " + Contract.Transaction.COUNT_IN_TOTAL + " = '1'";
        selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('now', 'localtime')";
        selection += " AND " + Contract.Transaction.DIRECTION + " = " + (mIncomes ? Contract.Direction.INCOME : Contract.Direction.EXPENSE);
        if (mStartDate != null) {
            selection += " AND DATETIME(" + Contract.Transaction.DATE + ") >= DATETIME('" + DateUtils.getSQLDateTimeString(mStartDate) + "')";
        }
        if (mEndDate != null) {
            selection += " AND DATETIME(" + Contract.Transaction.DATE + ") <= DATETIME('" + DateUtils.getSQLDateTimeString(mEndDate) + "')";
        }
        String sortOrder = Contract.Transaction.CATEGORY_ID;
        Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long ownId = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.CATEGORY_ID));
                    long categoryId;
                    long childId;
                    if (cursor.isNull(cursor.getColumnIndex(Contract.Transaction.CATEGORY_PARENT_ID))) {
                        categoryId = ownId;
                        childId = 0L;
                    } else {
                        categoryId = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.CATEGORY_PARENT_ID));
                        childId = ownId;
                    }
                    long money = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.MONEY));
                    String iso = cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY));
                    if (categoryCache.containsKey(categoryId)) {
                        if (childId != 0L) {
                            addChildMoney(childMoneyMap, childCategoryCache, categoryId, childId, iso, money);
                        } else {
                            addDirectMoney(directMoneyMap, categoryId, iso, money);
                        }
                    }
                    if (categoryMoneyMap.containsKey(categoryId)) {
                        CategoryMoney categoryMoney = categoryMoneyMap.get(categoryId);
                        categoryMoney.getMoney().addMoney(iso, money);
                    } else {
                        Category category = categoryCache.get(categoryId);
                        if (category != null) {
                            // if category is null it means that the category must not be showed
                            // inside the reports
                            CategoryMoney categoryMoney = new CategoryMoney(
                                    categoryId,
                                    category.getName(),
                                    category.getIcon(),
                                    new Money(iso, money)
                            );
                            categoryMoneyMap.put(categoryId, categoryMoney);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // now we have all the necessary data stored inside the map, we can iterate all the
        // category and fill the chart data and the total money item
        List<CategoryMoney> categoryMoneyList = new ArrayList<>();
        for (CategoryMoney categoryMoney : categoryMoneyMap.values()) {
            Money money = categoryMoney.getMoney();
            totalMoney.addMoney(money);
            for (Map.Entry<String, Long> entry : money.getCurrencyMoneys().entrySet()) {
                CurrencyUnit currency = CurrencyManager.getCurrency(entry.getKey());
                if (pieDataSets.containsKey(currency)) {
                    PieData pieData = pieDataSets.get(currency);
                    pieData.add(new PieSlice(categoryMoney.getName(), entry.getValue(), categoryMoney.getIcon().getDrawable(getContext())));
                } else {
                    PieData pieData = new PieData();
                    //entries.add(new PieEntry(entry.getValue(), categoryMoney.getName(), categoryMoney.getIcon().getDrawable(getContext())));
                    pieData.add(new PieSlice(categoryMoney.getName(), entry.getValue(), categoryMoney.getIcon().getDrawable(getContext())));
                    pieDataSets.put(currency, pieData);
                }
                // --->
                /* === USE THIS CODE IF MPAndroidChart library is used ===
                currency = CurrencyManager.getCurrency("USD");
                if (pieDataSets.containsKey(currency)) {
                    List<PieEntry> entries = pieDataSets.get(currency);
                    entries.add(new PieEntry(entry.getValue(), categoryMoney.getName()));
                } else {
                    List<PieEntry> entries = new ArrayList<>();
                    entries.add(new PieEntry(entry.getValue(), categoryMoney.getName()));
                    pieDataSets.put(currency, entries);
                }*/
                // <---
            }
            attachChildren(categoryMoney, childMoneyMap.get(categoryMoney.getId()),
                    directMoneyMap.get(categoryMoney.getId()));
            categoryMoneyList.add(categoryMoney);
        }
        // buildMaterialDialog the return object
        /*
        List<PieData> pieDataList = new ArrayList<>();
        for (Map.Entry<CurrencyUnit, List<PieEntry>> entry : pieDataSets.entrySet()) {
            String name = entry.getKey().getName();
            PieDataSet pieDataSet = new PieDataSet(entry.getValue(), name);
            pieDataSet.setColors(mColors);
            pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            pieDataList.add(new PieData(pieDataSet));
        }*/
        List<PieData> pieDataList = new ArrayList<>();
        for (Map.Entry<CurrencyUnit, PieData> entry : pieDataSets.entrySet()) {
            pieDataList.add(entry.getValue());
        }
        return new PeriodDetailFlowData(totalMoney, pieDataList, categoryMoneyList);
    }

    /**
     * Adds one transaction to the running total of the child category it was filed under. The
     * caller has already checked that the parent survived the report filter, so a child is only
     * counted here when its money is also counted in the parent row above it.
     *
     * A child is listed whatever its own show in reports setting says. That setting is never
     * consulted for the parent total either, since the roll up looks the parent up and not the
     * child, so honoring it here would leave the children failing to add up to the row the user
     * opened.
     */
    private void addChildMoney(Map<Long, Map<Long, CategoryMoney>> childMoneyMap,
                               Map<Long, Category> childCategoryCache,
                               long parentId, long childId, String iso, long money) {
        Map<Long, CategoryMoney> children = childMoneyMap.get(parentId);
        if (children != null) {
            CategoryMoney childMoney = children.get(childId);
            if (childMoney != null) {
                childMoney.getMoney().addMoney(iso, money);
                return;
            }
        }
        Category child = childCategoryCache.get(childId);
        if (child == null) {
            return;
        }
        if (children == null) {
            children = new HashMap<>();
            childMoneyMap.put(parentId, children);
        }
        children.put(childId, new CategoryMoney(childId, child.getName(), child.getIcon(),
                new Money(iso, money)));
    }

    private void addDirectMoney(Map<Long, Money> directMoneyMap, long categoryId, String iso, long money) {
        Money direct = directMoneyMap.get(categoryId);
        if (direct == null) {
            directMoneyMap.put(categoryId, new Money(iso, money));
        } else {
            direct.addMoney(iso, money);
        }
    }

    /**
     * Children sorted by name, because the map they arrive in has no order of its own and rows
     * that move between two loads of the same period read as a defect.
     *
     * What was filed on the parent itself leads the list, so that the rows under a category always
     * add up to the total on the category. Without it the money on the parent is on screen in the
     * total and nowhere in the breakdown, which reads as an error in the arithmetic. Nothing is
     * added when the category has no children, since there is nothing to expand and the total
     * already stands on its own.
     */
    private void attachChildren(CategoryMoney parent, Map<Long, CategoryMoney> children, Money direct) {
        if (children == null) {
            return;
        }
        if (direct != null) {
            parent.addChild(new CategoryMoney(parent.getId(), parent.getName(), parent.getIcon(), direct));
        }
        List<CategoryMoney> sorted = new ArrayList<>(children.values());
        Collections.sort(sorted, new Comparator<CategoryMoney>() {

            @Override
            public int compare(CategoryMoney left, CategoryMoney right) {
                return String.CASE_INSENSITIVE_ORDER.compare(left.getName(), right.getName());
            }

        });
        for (CategoryMoney child : sorted) {
            parent.addChild(child);
        }
    }

    private Map<Long, Category> loadChildCategoryCache() {
        return loadCategories(Contract.Category.PARENT + " IS NOT NULL");
    }

    @SuppressLint("UseSparseArrays")
    private Map<Long, Category> loadCategoryCache() {
        return loadCategories(Contract.Category.PARENT + " IS NULL AND " +
                Contract.Category.SHOW_REPORT + " = '1'");
    }

    @SuppressLint("UseSparseArrays")
    private Map<Long, Category> loadCategories(String selection) {
        Map<Long, Category> cache = new HashMap<>();
        Uri uri = DataContentProvider.CONTENT_CATEGORIES;
        String[] projection = new String[] {
                Contract.Category.ID,
                Contract.Category.NAME,
                Contract.Category.ICON,
                Contract.Category.TYPE
        };
        Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    long categoryId = cursor.getLong(cursor.getColumnIndex(Contract.Category.ID));
                    Category category = new Category(categoryId,
                            cursor.getString(cursor.getColumnIndex(Contract.Category.NAME)),
                            IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Category.ICON))),
                            Contract.CategoryType.fromValue(cursor.getInt(cursor.getColumnIndex(Contract.Category.TYPE)))
                    );
                    cache.put(categoryId, category);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return cache;
    }
}