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

package com.oriondev.moneywallet.ui.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.broadcast.RecurrenceBroadcastReceiver;
import com.oriondev.moneywallet.model.Category;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.RecurrenceSetting;
import com.oriondev.moneywallet.model.Wallet;
import com.oriondev.moneywallet.picker.BudgetTypePicker;
import com.oriondev.moneywallet.picker.CategoryPicker;
import com.oriondev.moneywallet.picker.DateTimePicker;
import com.oriondev.moneywallet.picker.MoneyPicker;
import com.oriondev.moneywallet.picker.RecurrencePicker;
import com.oriondev.moneywallet.picker.WalletPicker;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.SQLiteDataException;
import com.oriondev.moneywallet.storage.database.SyncContentProvider;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.view.text.MaterialEditText;
import com.oriondev.moneywallet.ui.view.text.Validator;
import com.oriondev.moneywallet.ui.view.theme.ThemedDialog;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateFormatter;
import com.oriondev.moneywallet.utils.DateUtils;
import com.oriondev.moneywallet.utils.IconLoader;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by andrea on 06/03/18.
 */
public class NewEditBudgetActivity extends NewEditItemActivity implements MoneyPicker.Controller,
                                                                        BudgetTypePicker.Controller,
                                                                        CategoryPicker.Controller,
                                                                        DateTimePicker.Controller,
                                                                        RecurrencePicker.Controller,
                                                                        WalletPicker.MultiWalletController {

    private static final String TAG_MONEY_PICKER = "NewEditBudgetActivity::Tag::MoneyPicker";
    private static final String TAG_BUDGET_TYPE_PICKER = "NewEditBudgetActivity::Tag::BudgetTypePicker";
    private static final String TAG_CATEGORY_PICKER = "NewEditBudgetActivity::Tag::CategoryPicker";
    private static final String TAG_START_DATE_PICKER = "NewEditBudgetActivity::Tag::StartDatePicker";
    private static final String TAG_END_DATE_PICKER = "NewEditBudgetActivity::Tag::EndDatePicker";
    private static final String TAG_WALLETS_PICKER = "NewEditBudgetActivity::Tag::WalletsPicker";
    private static final String TAG_RECURRENCE_PICKER = "NewEditBudgetActivity::Tag::RecurrencePicker";

    private static final String SS_ANCHORED = "NewEditBudgetActivity::SavedState::Anchored";

    private static final String SS_CLOSED_PERIOD = "NewEditBudgetActivity::SavedState::ClosedPeriod";

    private TextView mCurrencyTextView;
    private TextView mMoneyTextView;
    private MaterialEditText mTypeEditText;
    private MaterialEditText mCategoryEditText;
    private CheckBox mRepeatCheckBox;
    private MaterialEditText mRecurrenceEditText;
    private MaterialEditText mStartDateEditText;
    private MaterialEditText mEndDateEditText;
    private MaterialEditText mWalletsEditText;

    private MoneyPicker mMoneyPicker;
    private BudgetTypePicker mBudgetTypePicker;
    private CategoryPicker mCategoryPicker;
    private RecurrencePicker mRecurrencePicker;
    private DateTimePicker mStartDatePicker;
    private DateTimePicker mEndDatePicker;
    private WalletPicker mWalletsPicker;

    private boolean mAnchored;

    private boolean mClosedPeriod;

    private MoneyFormatter mMoneyFormatter = MoneyFormatter.getInstance();

    @Override
    protected void onCreateHeaderView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_header_new_edit_money_item, parent, true);
        mCurrencyTextView = view.findViewById(R.id.currency_text_view);
        mMoneyTextView = view.findViewById(R.id.money_text_view);
        // attach a listener to the views
        mMoneyTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mMoneyPicker.showPicker();
            }

        });
    }

    @Override
    protected void onCreatePanelView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_new_edit_budget, parent, true);
        mTypeEditText = view.findViewById(R.id.type_edit_text);
        mCategoryEditText = view.findViewById(R.id.category_edit_text);
        mRepeatCheckBox = view.findViewById(R.id.repeat_checkbox);
        mRecurrenceEditText = view.findViewById(R.id.recurrence_edit_text);
        mStartDateEditText = view.findViewById(R.id.start_date_edit_text);
        mEndDateEditText = view.findViewById(R.id.end_date_edit_text);
        mWalletsEditText = view.findViewById(R.id.wallets_edit_text);
        // disable unused edit text
        mTypeEditText.setTextViewMode(true);
        mCategoryEditText.setTextViewMode(true);
        mRecurrenceEditText.setTextViewMode(true);
        mStartDateEditText.setTextViewMode(true);
        mEndDateEditText.setTextViewMode(true);
        mWalletsEditText.setTextViewMode(true);
        // a repeating budget works out its own end date, and a new one its start date too, so the
        // two date fields are replaced by the recurrence. The listener also runs when the
        // framework restores the check box.
        mRepeatCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRecurrenceEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                mStartDateEditText.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                mEndDateEditText.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }

        });
        mRepeatCheckBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // The day the schedule is anchored to is taken from the start date field only
                // when nothing else has named one. A budget that arrived with an anchor keeps it,
                // because the field beside this box shows the start of whichever period the
                // budget has rolled into, and anchoring to that would move a budget that repeats
                // on the 31st onto the 28th the moment it had been through February. A day
                // already chosen in the recurrence dialog is kept for the same reason.
                Date startDate = mStartDatePicker.getCurrentDateTime();
                if (mRepeatCheckBox.isChecked() && startDate != null
                        && !mAnchored && !mRecurrencePicker.isChosen()) {
                    mRecurrencePicker.setCurrentSettings(rebuiltOn(startDate));
                }
            }

        });
        // setup validators
        mTypeEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_budget_type);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mBudgetTypePicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mWalletsEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_multiple_wallets);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mWalletsPicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mWalletsEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_invalid_multiple_wallets);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                CurrencyUnit currencyUnit = null;
                Wallet[] wallets = mWalletsPicker.getCurrentWallets();
                for (Wallet wallet : wallets) {
                    if (currencyUnit == null) {
                        currencyUnit = wallet.getCurrency();
                    } else if (!currencyUnit.equals(wallet.getCurrency())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mRecurrenceEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_invalid_budget_recurrence);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return getRepeatingPeriod() != null;
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mStartDateEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_start_date);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mStartDatePicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mEndDateEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_end_date);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mEndDatePicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mStartDateEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_invalid_date_range);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                Date start = mStartDatePicker.getCurrentDateTime();
                Date end = mEndDatePicker.getCurrentDateTime();
                return start != null && end != null && start.getTime() <= end.getTime();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mCategoryEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_category);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mCategoryPicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        // setup listeners
        mTypeEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mBudgetTypePicker.showPicker();
            }

        });
        mCategoryEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCategoryPicker.showPicker();
            }

        });
        mRecurrenceEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mRecurrencePicker.showPicker();
            }

        });
        mStartDateEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mStartDatePicker.showDatePicker();
            }

        });
        mEndDateEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mEndDatePicker.showDatePicker();
            }

        });
        mWalletsEditText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mWalletsPicker.showMultiWalletPicker();
            }

        });
    }

    @Override
    protected void onViewCreated(Bundle savedInstanceState) {
        super.onViewCreated(savedInstanceState);
        long money = 0L;
        Contract.BudgetType type = null;
        Category category = null;
        Date startDate = null;
        Date endDate = null;
        String rule = null;
        Date ruleStart = null;
        Wallet[] wallets = null;
        if (savedInstanceState == null) {
            ContentResolver contentResolver = getContentResolver();
            if (getMode() == Mode.EDIT_ITEM) {
                Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_BUDGETS, getItemId());
                String[] projection = new String[] {
                        Contract.Budget.TYPE,
                        Contract.Budget.CATEGORY_ID,
                        Contract.Budget.CATEGORY_NAME,
                        Contract.Budget.CATEGORY_ICON,
                        Contract.Budget.CATEGORY_TYPE,
                        Contract.Budget.START_DATE,
                        Contract.Budget.END_DATE,
                        Contract.Budget.MONEY,
                        Contract.Budget.RULE,
                        Contract.Budget.RULE_START
                };
                Cursor cursor = contentResolver.query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        type = Contract.BudgetType.fromValue(cursor.getInt(cursor.getColumnIndex(Contract.Budget.TYPE)));
                        if (type == Contract.BudgetType.CATEGORY) {
                            category = new Category(
                                    cursor.getLong(cursor.getColumnIndex(Contract.Budget.CATEGORY_ID)),
                                    cursor.getString(cursor.getColumnIndex(Contract.Budget.CATEGORY_NAME)),
                                    IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Budget.CATEGORY_ICON))),
                                    Contract.CategoryType.fromValue(cursor.getInt(cursor.getColumnIndex(Contract.Budget.CATEGORY_TYPE)))
                            );
                        }
                        money = cursor.getLong(cursor.getColumnIndex(Contract.Budget.MONEY));
                        startDate = DateUtils.getDateFromSQLDateString(cursor.getString(cursor.getColumnIndex(Contract.Budget.START_DATE)));
                        endDate = DateUtils.getDateFromSQLDateString(cursor.getString(cursor.getColumnIndex(Contract.Budget.END_DATE)));
                        rule = cursor.getString(cursor.getColumnIndex(Contract.Budget.RULE));
                        String ruleStartString = cursor.getString(cursor.getColumnIndex(Contract.Budget.RULE_START));
                        if (ruleStartString != null) {
                            ruleStart = DateUtils.getDateFromSQLDateString(ruleStartString);
                        }
                    }
                    cursor.close();
                }
                // the previous cursor contains only a column with the list of ids of linked wallets.
                // we need instead to buildMaterialDialog the full wallet object so we must perform a separated
                // query to the database to obtain the full cursor.
                uri = Uri.withAppendedPath(uri, "wallets");
                projection = new String[] {
                        Contract.Wallet.ID,
                        Contract.Wallet.NAME,
                        Contract.Wallet.ICON,
                        Contract.Wallet.CURRENCY,
                        Contract.Wallet.START_MONEY
                };
                cursor = contentResolver.query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        wallets = new Wallet[cursor.getCount()];
                        for (int i = 0; cursor.moveToPosition(i) && i < cursor.getCount(); i++) {
                            wallets[i] = new Wallet(
                                    cursor.getLong(cursor.getColumnIndex(Contract.Wallet.ID)),
                                    cursor.getString(cursor.getColumnIndex(Contract.Wallet.NAME)),
                                    IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Wallet.ICON))),
                                    CurrencyManager.getCurrency(cursor.getString(cursor.getColumnIndex(Contract.Wallet.CURRENCY))),
                                    cursor.getLong(cursor.getColumnIndex(Contract.Wallet.START_MONEY)), 0);
                        }
                    }
                    cursor.close();
                }
            } else {
                type = Contract.BudgetType.EXPENSES;
                Calendar calendar = Calendar.getInstance();
                startDate = calendar.getTime();
                endDate = DateUtils.addMonths(calendar, 1);
                String[] projection = new String[] {
                        Contract.Wallet.ID,
                        Contract.Wallet.NAME,
                        Contract.Wallet.ICON,
                        Contract.Wallet.CURRENCY,
                        Contract.Wallet.START_MONEY,
                        Contract.Wallet.TOTAL_MONEY
                };
                long currentWallet = PreferenceManager.getCurrentWallet();
                Cursor cursor;
                if (currentWallet == PreferenceManager.TOTAL_WALLET_ID) {
                    Uri uri = DataContentProvider.CONTENT_WALLETS;
                    cursor = contentResolver.query(uri, projection, null, null, null);
                } else {
                    Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_WALLETS, currentWallet);
                    cursor = contentResolver.query(uri, projection, null, null, null);
                }
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        wallets = new Wallet[] {
                                new Wallet(
                                        cursor.getLong(cursor.getColumnIndex(Contract.Wallet.ID)),
                                        cursor.getString(cursor.getColumnIndex(Contract.Wallet.NAME)),
                                        IconLoader.parse(cursor.getString(cursor.getColumnIndex(Contract.Wallet.ICON))),
                                        CurrencyManager.getCurrency(cursor.getString(cursor.getColumnIndex(Contract.Wallet.CURRENCY))),
                                        cursor.getLong(cursor.getColumnIndex(Contract.Wallet.START_MONEY)),
                                        cursor.getLong(cursor.getColumnIndex(Contract.Wallet.TOTAL_MONEY))
                                )
                        };
                    }
                    cursor.close();
                }
            }
        }
        // now we can create pickers with default values or existing item parameters
        // and update all the views according to the data
        FragmentManager fragmentManager = getSupportFragmentManager();
        mMoneyPicker = MoneyPicker.createPicker(fragmentManager, TAG_MONEY_PICKER, null, money);
        mBudgetTypePicker = BudgetTypePicker.createPicker(fragmentManager, TAG_BUDGET_TYPE_PICKER, type);
        mCategoryPicker = CategoryPicker.createPicker(fragmentManager, TAG_CATEGORY_PICKER, category);
        mStartDatePicker = DateTimePicker.createPicker(fragmentManager, TAG_START_DATE_PICKER, startDate);
        mEndDatePicker = DateTimePicker.createPicker(fragmentManager, TAG_END_DATE_PICKER, endDate);
        mWalletsPicker = WalletPicker.createPicker(fragmentManager, TAG_WALLETS_PICKER, wallets);
        Date recurrenceStartDate = ruleStart != null ? ruleStart : (startDate != null ? startDate : new Date());
        boolean repeats = rule != null && RecurrenceSetting.periodEnd(rule, recurrenceStartDate, recurrenceStartDate) != null;
        RecurrenceSetting recurrenceSetting = repeats
                ? new RecurrenceSetting(recurrenceStartDate, rule)
                : monthlyOn(recurrenceStartDate);
        // a budget period ends the day before the next repeat, so a recurrence that stops after a
        // date or a number of times would leave the last period with no end at all
        mRecurrencePicker = RecurrencePicker.createPicker(fragmentManager, TAG_RECURRENCE_PICKER, recurrenceSetting, false, true);
        if (savedInstanceState == null) {
            mAnchored = ruleStart != null;
            mRepeatCheckBox.setChecked(repeats);
            mClosedPeriod = !repeats && isSupersededPeriod();
        } else {
            mAnchored = savedInstanceState.getBoolean(SS_ANCHORED, false);
            mClosedPeriod = savedInstanceState.getBoolean(SS_CLOSED_PERIOD, false);
        }
        // A period a repeating budget has already moved past is history. Offering to repeat this
        // one either puts a second schedule on the same chain, so that from the next period on the
        // roll walks both and opens every period twice, or asks the roll to open a period the
        // chain already holds, which the unique name of that period refuses without saying so and
        // which leaves the schedule on nothing at all. A chain is restarted from the period it got
        // to. This is applied on every start and not only the first, because a view does not carry
        // whether it was hidden across a rotation, and the box would come back visible.
        if (mClosedPeriod) {
            mRepeatCheckBox.setVisibility(View.GONE);
        }
    }

    /**
     * Whether this budget is a period its chain has already moved past, meaning another budget of
     * the same chain begins after it does. A row flagged deleted counts too, though deleting a
     * budget in the app takes the row away rather than flagging it.
     *
     * @return true when another budget of this chain begins later.
     */
    private boolean isSupersededPeriod() {
        if (getMode() != Mode.EDIT_ITEM) {
            return false;
        }
        String chain = chainUUID();
        String startDate = storedStartDate();
        if (chain == null || startDate == null) {
            return false;
        }
        String[] selectionArgs = new String[] {String.valueOf(getItemId()), chain,
                likeLiteral(chain) + ":%", startDate};
        Cursor cursor = getContentResolver().query(SyncContentProvider.CONTENT_BUDGET,
                new String[] {Contract.Budget.ID}, Contract.LATER_PERIOD_OF_CHAIN_SELECTION, selectionArgs, null);
        if (cursor != null) {
            try {
                return cursor.moveToFirst();
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * The given text with the characters LIKE reads as wildcards taken literally, against the
     * backslash escape {@link Contract#LATER_PERIOD_OF_CHAIN_SELECTION} names.
     */
    private static String likeLiteral(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * The schedule that will be saved: the frequency the picker holds, anchored to the day this
     * budget is anchored to. The two have to be built together, because a monthly schedule writes
     * the day it comes round on into the rule itself. Taking the rule from the picker and the
     * anchor from the row would let a budget anchored to the 1st be saved with a rule that says
     * the 8th, and it would move onto the 8th while the anchor said otherwise.
     */
    private RecurrenceSetting scheduleToSave() {
        return rebuiltOn(anchorToSave());
    }

    /**
     * The day the schedule being saved is anchored to. A budget that already has one keeps it: the
     * anchor is what its whole chain is counted from, and moving it takes the budget off the
     * period it is keeping and everything filed against that period with it.
     *
     * Turning repeat off is what lets a budget be anchored somewhere else. It gives the two date
     * fields back and drops the anchor with the schedule, so the day the budget is set to start on
     * while it is off is the day its next schedule is anchored to.
     *
     * That is why a budget being given its first schedule is anchored to its own start date and
     * not to the day the recurrence dialog offers. A schedule anchored later than the budget
     * starts would leave one period running from the budget to the day the schedule begins,
     * however many months that is, carrying the whole amount and holding no occurrence of the
     * schedule saved beside it. Only a budget being created from nothing takes its anchor from
     * the dialog, and there the period it lands on is worked out from the schedule itself.
     */
    private Date anchorToSave() {
        if (getMode() == Mode.EDIT_ITEM) {
            String stored = storedColumn(Contract.Budget.RULE_START);
            if (stored != null) {
                return DateUtils.getDateFromSQLDateString(stored);
            }
            Date fieldStart = mStartDatePicker.getCurrentDateTime();
            if (fieldStart != null) {
                return fieldStart;
            }
        }
        return mRecurrencePicker.getCurrentSettings().getStartDate();
    }

    /**
     * The day this budget begins on, as stored right now. Read from the table rather than from
     * the editor's own fields, because the roll may have moved this budget on since the editor
     * opened it.
     */
    private String storedStartDate() {
        return storedColumn(Contract.Budget.START_DATE);
    }

    /**
     * One column of this budget as stored right now, read from the table rather than from the
     * editor's own fields, because the roll may have moved this budget on since it opened.
     */
    private String storedColumn(String column) {
        Cursor cursor = getContentResolver().query(SyncContentProvider.CONTENT_BUDGET,
                new String[] {column}, Contract.Budget.ID + " = ?",
                new String[] {String.valueOf(getItemId())}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(column));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * The uuid of the budget this one's chain started from: this budget's own uuid up to the date
     * a roll appends to name a period. Null when the row cannot be read.
     */
    private String chainUUID() {
        Cursor cursor = getContentResolver().query(SyncContentProvider.CONTENT_BUDGET,
                new String[] {Contract.BUDGET_UUID}, Contract.Budget.ID + " = ?",
                new String[] {String.valueOf(getItemId())}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String uuid = cursor.getString(cursor.getColumnIndex(Contract.BUDGET_UUID));
                    if (uuid != null) {
                        int separator = uuid.indexOf(':');
                        return separator >= 0 ? uuid.substring(0, separator) : uuid;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * The period the recurrence the picker currently holds is in today, as the pair {start, end},
     * or null when that recurrence produces no period at all. A schedule anchored in the past
     * saves the period it is in now, so nothing is left for the roll forward to catch up on.
     */
    private Date[] getRepeatingPeriod() {
        RecurrenceSetting recurrenceSetting = mRecurrencePicker.getCurrentSettings();
        return RecurrenceSetting.periodOn(recurrenceSetting.getRule(), recurrenceSetting.getStartDate(), new Date());
    }

    /**
     * A monthly schedule anchored to the given day, built the way the recurrence dialog builds
     * one. The plain constructor writes a bare FREQ=MONTHLY, which names no day and so skips
     * every month too short to have the one it started on.
     */
    private static RecurrenceSetting monthlyOn(Date startDate) {
        RecurrenceSetting.Builder builder = new RecurrenceSetting.Builder(startDate, RecurrenceSetting.TYPE_MONTHLY);
        builder.setRepeatSameMonthDay();
        return builder.build();
    }

    /**
     * The schedule the picker holds, built again from {@code startDate}. Only the day it is
     * anchored to moves; how often it repeats is carried over. The parts that name a day of the
     * month are worked out from the new day, so a schedule built on the 24th and then anchored to
     * the 1st repeats on the 1st, instead of reading as the 1st and repeating on the 24th.
     */
    private RecurrenceSetting rebuiltOn(Date startDate) {
        RecurrenceSetting current = mRecurrencePicker.getCurrentSettings();
        RecurrenceSetting.Builder builder = new RecurrenceSetting.Builder(startDate, current.getType());
        builder.setOffset(current.getOffsetValue());
        if (current.getType() == RecurrenceSetting.TYPE_MONTHLY) {
            builder.setRepeatSameMonthDay();
        } else if (current.getType() == RecurrenceSetting.TYPE_YEARLY) {
            builder.setRepeatSameYearDay();
        }
        return builder.build();
    }

    /**
     * The dates a repeating budget is saved with. A new one takes the period its schedule is in
     * today, so a schedule anchored in the past does not open a period that finished long ago.
     *
     * An existing one starts where its start date field says and ends where its schedule says.
     * That field holds the day the row already starts on unless its owner changed it, so an
     * existing budget only moves when it is moved deliberately, and an edit to its amount, or to
     * how often it repeats, leaves it where it is. Both dates are kept exactly as stored when
     * neither the schedule nor that field has changed, so a budget whose period has ended and has
     * not been rolled yet does not lose a day to being saved.
     *
     * Landing an existing budget on the period its schedule is in today would rewrite a budget
     * that has finished, and everything filed against it, onto another month: ticking repeat on
     * last June's budget would leave its owner with a budget for this month and no record of
     * June. Landing it anywhere else its schedule reaches is the same loss in the other
     * direction, and a period that moves back over one the roll has already closed counts every
     * transaction in the overlap against both.
     */
    private Date[] periodToSave(Mode mode) {
        RecurrenceSetting schedule = scheduleToSave();
        String rule = schedule.getRule();
        Date anchor = schedule.getStartDate();
        if (mode == Mode.EDIT_ITEM) {
            Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_BUDGETS, getItemId());
            String[] projection = new String[] {Contract.Budget.START_DATE, Contract.Budget.END_DATE,
                    Contract.Budget.RULE, Contract.Budget.RULE_START};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String storedStart = cursor.getString(cursor.getColumnIndex(Contract.Budget.START_DATE));
                        String storedEnd = cursor.getString(cursor.getColumnIndex(Contract.Budget.END_DATE));
                        String storedRule = cursor.getString(cursor.getColumnIndex(Contract.Budget.RULE));
                        String storedRuleStart = cursor.getString(cursor.getColumnIndex(Contract.Budget.RULE_START));
                        Date fieldStart = mStartDatePicker.getCurrentDateTime();
                        boolean scheduleHeld = TextUtils.equals(rule, storedRule)
                                && TextUtils.equals(DateUtils.getSQLDateString(anchor), storedRuleStart);
                        boolean startHeld = fieldStart == null
                                || TextUtils.equals(DateUtils.getSQLDateString(fieldStart), storedStart);
                        if (scheduleHeld && startHeld) {
                            return new Date[] {
                                    DateUtils.getDateFromSQLDateString(storedStart),
                                    DateUtils.getDateFromSQLDateString(storedEnd)
                            };
                        }
                        Date periodStart = fieldStart != null ? fieldStart : DateUtils.getDateFromSQLDateString(storedStart);
                        // Changing how often a budget repeats leaves one period between the old
                        // schedule and the new: it runs from the day the budget already starts on
                        // to the day before the new schedule first comes round, and carries the
                        // whole amount however long that is. Only a full period after it is on the
                        // new schedule.
                        Date periodEnd = periodStart != null ? RecurrenceSetting.periodEnd(rule, anchor, periodStart) : null;
                        if (periodEnd != null) {
                            return new Date[] {periodStart, periodEnd};
                        }
                        // nothing here can say where this period should end, so it keeps the dates
                        // it has. Falling through would land it on the period the schedule is in
                        // today, which is the one thing an existing budget must never be given.
                        return new Date[] {
                                DateUtils.getDateFromSQLDateString(storedStart),
                                DateUtils.getDateFromSQLDateString(storedEnd)
                        };
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return getRepeatingPeriod();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SS_ANCHORED, mAnchored);
        outState.putBoolean(SS_CLOSED_PERIOD, mClosedPeriod);
    }

    @Override
    protected int getActivityTileRes(Mode mode) {
        switch (mode) {
            case NEW_ITEM:
                return R.string.title_activity_new_budget;
            case EDIT_ITEM:
                return R.string.title_activity_edit_budget;
            default:
                return -1;
        }
    }

    private boolean validate() {
        boolean datesValid = mRepeatCheckBox.isChecked() ? mRecurrenceEditText.validate()
                : mStartDateEditText.validate() && mEndDateEditText.validate();
        return mTypeEditText.validate() && mWalletsEditText.validate() && datesValid &&
                (mBudgetTypePicker.getCurrentType() != Contract.BudgetType.CATEGORY ||
                        mCategoryEditText.validate());
    }

    @Override
    protected void onSaveChanges(Mode mode) {
        if (validate()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Contract.Budget.TYPE, mBudgetTypePicker.getCurrentType().getValue());
            if (mBudgetTypePicker.getCurrentType() == Contract.BudgetType.CATEGORY) {
                contentValues.put(Contract.Budget.CATEGORY_ID, mCategoryPicker.getCurrentCategory().getId());
            } else {
                contentValues.putNull(Contract.Budget.CATEGORY_ID);
            }
            if (mRepeatCheckBox.isChecked() && isSupersededPeriod()) {
                // the roll opened a later period of this chain while the editor was open, so this
                // budget is history now. Writing a schedule onto it would put a second one on the
                // chain, and every period after this would open twice.
                ThemedDialog.buildMaterialDialog(this)
                        .title(R.string.title_warning)
                        .content(R.string.message_budget_period_superseded)
                        .positiveText(android.R.string.ok)
                        .show();
                return;
            }
            if (mRepeatCheckBox.isChecked()) {
                Date[] period = periodToSave(mode);
                contentValues.put(Contract.Budget.START_DATE, DateUtils.getSQLDateString(period[0]));
                contentValues.put(Contract.Budget.END_DATE, DateUtils.getSQLDateString(period[1]));
                RecurrenceSetting schedule = scheduleToSave();
                contentValues.put(Contract.Budget.RULE, schedule.getRule());
                contentValues.put(Contract.Budget.RULE_START, DateUtils.getSQLDateString(schedule.getStartDate()));
            } else {
                contentValues.put(Contract.Budget.START_DATE, DateUtils.getSQLDateString(mStartDatePicker.getCurrentDateTime()));
                contentValues.put(Contract.Budget.END_DATE, DateUtils.getSQLDateString(mEndDatePicker.getCurrentDateTime()));
                contentValues.putNull(Contract.Budget.RULE);
                contentValues.putNull(Contract.Budget.RULE_START);
            }
            contentValues.put(Contract.Budget.MONEY, mMoneyPicker.getCurrentMoney());
            contentValues.put(Contract.Budget.CURRENCY, mWalletsPicker.getCurrentWallets()[0].getCurrency().getIso());
            contentValues.put(Contract.Budget.WALLET_IDS, Contract.getObjectIds(mWalletsPicker.getCurrentWallets()));
            ContentResolver contentResolver = getContentResolver();
            try {
                switch (mode) {
                    case NEW_ITEM:
                        contentResolver.insert(DataContentProvider.CONTENT_BUDGETS, contentValues);
                        break;
                    case EDIT_ITEM:
                        Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_BUDGETS, getItemId());
                        contentResolver.update(uri, contentValues, null, null);
                        break;
                }
            } catch (SQLiteDataException e) {
                int contentRes = 0;
                switch (e.getErrorCode()) {
                    case Contract.ErrorCode.WALLETS_NOT_FOUND:
                        contentRes = R.string.error_input_missing_multiple_wallets;
                        break;
                    case Contract.ErrorCode.WALLETS_NOT_CONSISTENT:
                        contentRes = R.string.error_input_invalid_multiple_wallets;
                        break;
                }
                if (contentRes != 0) {
                    ThemedDialog.buildMaterialDialog(this)
                            .title(R.string.title_error)
                            .content(contentRes)
                            .positiveText(android.R.string.ok)
                            .show();
                }
            }
            RecurrenceBroadcastReceiver.scheduleRecurrenceTask(this);
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    @Override
    public void onMoneyChanged(String tag, CurrencyUnit currency, long money) {
        mMoneyTextView.setText(mMoneyFormatter.getNotTintedString(currency, money, MoneyFormatter.CurrencyMode.ALWAYS_HIDDEN));
        if (currency != null) {
            mCurrencyTextView.setText(currency.getSymbol());
        } else {
            mCurrencyTextView.setText("?");
        }
    }

    @Override
    public void onTypeChanged(String tag, Contract.BudgetType type) {
        switch (type) {
            case INCOMES:
                mTypeEditText.setText(R.string.hint_incomes);
                mCategoryEditText.setVisibility(View.GONE);
                break;
            case EXPENSES:
                mTypeEditText.setText(R.string.hint_expenses);
                mCategoryEditText.setVisibility(View.GONE);
                break;
            case CATEGORY:
                mTypeEditText.setText(R.string.hint_category);
                mCategoryEditText.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onCategoryChanged(String tag, Category category) {
        if (category != null) {
            mCategoryEditText.setText(category.getName());
        } else {
            mCategoryEditText.setText(null);
        }
    }

    @Override
    public void onDateTimeChanged(String tag, Date date) {
        switch (tag) {
            case TAG_START_DATE_PICKER:
                if (date != null) {
                    DateFormatter.applyDate(mStartDateEditText, date);
                } else {
                    mStartDateEditText.setText(null);
                }
                break;
            case TAG_END_DATE_PICKER:
                if (date != null) {
                    DateFormatter.applyDate(mEndDateEditText, date);
                } else {
                    mEndDateEditText.setText(null);
                }
                break;
        }
    }

    @Override
    public void onRecurrenceSettingChanged(String tag, RecurrenceSetting recurrenceSetting) {
        // what the schedule will be once it is saved, which is not always what the dialog just
        // handed back: a budget that is already anchored keeps the day it is anchored to
        mRecurrenceEditText.setText(scheduleToSave().getUserReadableString(this));
    }

    @Override
    public void onWalletListChanged(String tag, Wallet[] wallets) {
        if (wallets != null && wallets.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < wallets.length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(wallets[i].getName());
            }
            mWalletsEditText.setText(builder);
            mMoneyPicker.setCurrency(wallets[0].getCurrency());
        } else {
            mWalletsEditText.setText(null);
            mMoneyPicker.setCurrency(null);
        }
    }
}