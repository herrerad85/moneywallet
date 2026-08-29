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

package com.oriondev.moneywallet.ui.adapter.recycler;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.CategoryMoney;
import com.oriondev.moneywallet.model.PeriodDetailFlowData;
import com.oriondev.moneywallet.ui.view.CategoryChildIndicator;
import com.oriondev.moneywallet.utils.IconLoader;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by andrea on 13/08/18.
 */
public class PeriodDetailFlowAdapter extends RecyclerView.Adapter<PeriodDetailFlowAdapter.ViewHolder> {

    private static final int INDEX_PARENT = -1;

    private static final int VIEW_TYPE_PARENT = 0;
    private static final int VIEW_TYPE_CHILD = 1;

    private final Controller mController;
    private final boolean mIncomes;
    private final MoneyFormatter mMoneyFormatter;

    private final List<ItemWrapper> mItems = new ArrayList<>();
    private final Set<Long> mExpandedCategories = new HashSet<>();

    private PeriodDetailFlowData mData;

    public PeriodDetailFlowAdapter(Controller controller, boolean incomes) {
        mController = controller;
        mIncomes = incomes;
        mMoneyFormatter = MoneyFormatter.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_PARENT) {
            return new ViewHolder(inflater.inflate(R.layout.adapter_category_money_item, parent, false));
        }
        return new ViewHolder(inflater.inflate(R.layout.adapter_sub_category_money_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CategoryMoney category = getCategoryAt(position);
        IconLoader.loadInto(category.getIcon(), holder.mIconImageView);
        holder.mNameTextView.setText(category.getName());
        if (mIncomes) {
            mMoneyFormatter.applyTintedIncome(holder.mMoneyTextView, category.getMoney());
        } else {
            mMoneyFormatter.applyTintedExpense(holder.mMoneyTextView, category.getMoney());
        }
        if (holder.getItemViewType() == VIEW_TYPE_CHILD) {
            holder.mChildIndicator.setLast(isLastChild(position));
        } else {
            bindChildrenToggle(holder, category);
        }
    }

    private void bindChildrenToggle(ViewHolder holder, CategoryMoney category) {
        boolean hasChildren = !category.getChildren().isEmpty();
        // Invisible and not gone, so a category without children still reserves the arrow's width
        // and every amount in the list keeps the same right edge. A child's amount is read against
        // the parent total directly above it, so the two have to line up.
        holder.mChildrenToggle.setVisibility(hasChildren ? View.VISIBLE : View.INVISIBLE);
        if (hasChildren) {
            boolean expanded = mExpandedCategories.contains(category.getId());
            holder.mChildrenToggle.setRotation(expanded ? 180f : 0f);
            // Named, because a screen reader reaches the arrow on its own and every one of them
            // would otherwise read the same words.
            holder.mChildrenToggle.setContentDescription(holder.itemView.getContext().getString(
                    expanded ? R.string.description_hide_subcategories
                            : R.string.description_show_subcategories, category.getName()));
        }
    }

    /**
     * Whether the child at this row is the last one drawn under its category, which is what tells
     * the indicator to stop its line half way. A category's children are shown or hidden all
     * together, so the last of them is always the last one on screen.
     */
    private boolean isLastChild(int position) {
        ItemWrapper item = mItems.get(position);
        CategoryMoney parent = mData.getCategory(item.mParentIndex);
        return item.mChildIndex == parent.getChildren().size() - 1;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).mChildIndex == INDEX_PARENT ? VIEW_TYPE_PARENT : VIEW_TYPE_CHILD;
    }

    /**
     * What is expanded is deliberately kept across a reload. The loader redelivers its cached
     * result whenever this screen is started again, so clearing here would collapse the list every
     * time the user opened a category's transactions and came back, which is the way through this
     * feature.
     */
    public void setData(PeriodDetailFlowData data) {
        mData = data;
        rebuildItems();
    }

    /**
     * Every category is a row, and the children of an expanded one follow it. Categories start
     * collapsed so that opening a period still shows the list of totals it always did.
     */
    private void rebuildItems() {
        mItems.clear();
        if (mData != null) {
            for (int i = 0; i < mData.getCategoryCount(); i++) {
                CategoryMoney category = mData.getCategory(i);
                mItems.add(new ItemWrapper(i, INDEX_PARENT));
                if (mExpandedCategories.contains(category.getId())) {
                    for (int j = 0; j < category.getChildren().size(); j++) {
                        mItems.add(new ItemWrapper(i, j));
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public long[] getExpandedCategories() {
        long[] expanded = new long[mExpandedCategories.size()];
        int index = 0;
        for (Long categoryId : mExpandedCategories) {
            expanded[index++] = categoryId;
        }
        return expanded;
    }

    public void setExpandedCategories(long[] expanded) {
        mExpandedCategories.clear();
        for (long categoryId : expanded) {
            mExpandedCategories.add(categoryId);
        }
        rebuildItems();
    }

    private void toggleChildren(long categoryId) {
        if (!mExpandedCategories.remove(categoryId)) {
            mExpandedCategories.add(categoryId);
        }
        rebuildItems();
    }

    private CategoryMoney getCategoryAt(int position) {
        ItemWrapper item = mItems.get(position);
        CategoryMoney parent = mData.getCategory(item.mParentIndex);
        return item.mChildIndex == INDEX_PARENT ? parent : parent.getChildren().get(item.mChildIndex);
    }

    private static class ItemWrapper {

        private final int mParentIndex;
        private final int mChildIndex;

        private ItemWrapper(int parentIndex, int childIndex) {
            mParentIndex = parentIndex;
            mChildIndex = childIndex;
        }
    }

    /*package-local*/ class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private CategoryChildIndicator mChildIndicator;
        private ImageView mIconImageView;
        private ImageView mChildrenToggle;
        private TextView mNameTextView;
        private TextView mMoneyTextView;

        /*package-local*/ ViewHolder(View itemView) {
            super(itemView);
            mChildIndicator = itemView.findViewById(R.id.category_child_indicator);
            mIconImageView = itemView.findViewById(R.id.icon_image_view);
            mChildrenToggle = itemView.findViewById(R.id.children_toggle_image_view);
            mNameTextView = itemView.findViewById(R.id.name_text_view);
            mMoneyTextView = itemView.findViewById(R.id.money_text_view);
            itemView.setOnClickListener(this);
            if (mChildrenToggle != null) {
                // Its own listener, because the row already has a click of its own.
                mChildrenToggle.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            toggleChildren(getCategoryAt(position).getId());
                        }
                    }

                });
            }
        }

        @Override
        public void onClick(View v) {
            if (mController != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mController.onCategoryClick(getCategoryAt(position).getId());
                }
            }
        }
    }

    public interface Controller {

        void onCategoryClick(long id);
    }
}
