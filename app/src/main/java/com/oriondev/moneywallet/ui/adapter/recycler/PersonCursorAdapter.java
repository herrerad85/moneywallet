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

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.Icon;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.utils.IconLoader;

/**
 * Created by andrea on 04/03/18.
 */
public class PersonCursorAdapter extends AbstractCursorAdapter<PersonCursorAdapter.ViewHolder> {

    private final ActionListener mActionListener;

    private int mIndexId;
    private int mIndexName;
    private int mIndexIcon;

    public PersonCursorAdapter(ActionListener actionListener) {
        super(null, Contract.Person.ID);
        mActionListener = actionListener;
    }

    @Override
    protected void onLoadColumnIndices(@NonNull Cursor cursor) {
        mIndexId = cursor.getColumnIndex(Contract.Person.ID);
        mIndexName = cursor.getColumnIndex(Contract.Person.NAME);
        mIndexIcon = cursor.getColumnIndex(Contract.Person.ICON);
    }

    @Override
    public void onBindViewHolder(PersonCursorAdapter.ViewHolder holder, Cursor cursor) {
        Icon icon = IconLoader.parse(cursor.getString(mIndexIcon));
        IconLoader.loadInto(icon, holder.mAvatarImageView);
        holder.mNameTextView.setText(cursor.getString(mIndexName));
    }

    @Override
    public PersonCursorAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.adapter_person_item, parent, false);
        if (parent instanceof RecyclerView) {
            RecyclerViewAccessibilityDelegate parentDelegate =
                    ((RecyclerView) parent).getCompatAccessibilityDelegate();
            if (parentDelegate != null) {
                ViewCompat.setAccessibilityDelegate(itemView,
                        new PersonItemDelegate(parentDelegate.getItemDelegate()));
            } else {
                // Only reachable if something cleared the RecyclerView's own delegate. The row
                // still works, it just loses the named long press, so leave a trace rather than
                // dropping the label in silence.
                Log.w("PersonCursorAdapter", "no RecyclerView accessibility delegate, long press stays unlabelled");
            }
        }
        return new ViewHolder(itemView);
    }

    /**
     * A person row needs two things announced that cannot both come from one delegate. Its
     * position in the list is supplied only by the RecyclerView's own item delegate, and the long
     * press is otherwise announced with no destination. Setting any delegate on a row makes
     * RecyclerView skip attaching its own, so this forwards to that one rather than replacing it.
     */
    private static class PersonItemDelegate extends AccessibilityDelegateCompat {

        private final AccessibilityDelegateCompat mItemDelegate;

        private PersonItemDelegate(AccessibilityDelegateCompat itemDelegate) {
            mItemDelegate = itemDelegate;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            mItemDelegate.onInitializeAccessibilityNodeInfo(host, info);
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                    host.getContext().getString(R.string.action_open_person_details)
            ));
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            // Nothing this forward reaches is implemented on the recyclerview this project
            // resolves, so it changes no behaviour today. It is here so that wrapping the item
            // delegate stays a complete wrap rather than a partial one.
            return mItemDelegate.performAccessibilityAction(host, action, args);
        }

    }

    /*package-local*/ class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ImageView mAvatarImageView;
        private TextView mNameTextView;

        /*package-local*/ ViewHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.avatar_image_view);
            mNameTextView = itemView.findViewById(R.id.primary_text_view);
            itemView.setOnClickListener(this);
            // Named for screen readers by PersonItemDelegate above, which is set in
            // onCreateViewHolder because it needs the RecyclerView to forward to.
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mActionListener != null) {
                Cursor cursor = getSafeCursor(getAdapterPosition());
                if (cursor != null) {
                    mActionListener.onPersonClick(cursor.getLong(mIndexId));
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mActionListener == null) {
                return false;
            }
            Cursor cursor = getSafeCursor(getAdapterPosition());
            if (cursor != null) {
                mActionListener.onPersonLongClick(cursor.getLong(mIndexId));
            }
            // Consumed whenever there is a listener to consume it. Declining it would let the
            // release that follows run performClick instead, so a long press that lost its row
            // would open the transaction list rather than the panel the user was reaching for.
            return true;
        }
    }

    public interface ActionListener {

        void onPersonClick(long id);

        void onPersonLongClick(long id);
    }
}