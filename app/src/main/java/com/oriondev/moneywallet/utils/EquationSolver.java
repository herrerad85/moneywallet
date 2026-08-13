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

package com.oriondev.moneywallet.utils;

import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.MoneyScale;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Created by andrea on 31/03/18.
 */
public class EquationSolver {

    private static final String SS_FIRST_NUMBER = "EquationSolver::SavedState::FirstNumber";
    private static final String SS_SECOND_NUMBER = "EquationSolver::SavedState::SecondNumber";
    private static final String SS_OPERATION_NUMBER = "EquationSolver::SavedState::Operation";
    private static final String SS_CURRENCY = "EquationSolver::SavedState::Currency";
    private static final String SS_COMPUTED = "EquationSolver::SavedState::Computed";

    private final Controller mController;

    @VisibleForTesting
    /*package-local*/ String mFirstNumber;
    private String mSecondNumber;
    private Operation mOperation;
    @VisibleForTesting
    /*package-local*/ CurrencyUnit mCurrency;
    /**
     * True while mFirstNumber holds an answer an operation produced rather than a number the
     * keypad typed. getResult rounds an answer to the currency scale and leaves a typed number
     * to toMinorUnits, which truncates whatever will not fit.
     */
    @VisibleForTesting
    /*package-local*/ boolean mComputed;

    public EquationSolver(Bundle savedInstanceState, Controller controller) {
        mController = controller;
        if (savedInstanceState != null) {
            mFirstNumber = savedInstanceState.getString(SS_FIRST_NUMBER, "0");
            mSecondNumber = savedInstanceState.getString(SS_SECOND_NUMBER, null);
            mOperation = (Operation) savedInstanceState.getSerializable(SS_OPERATION_NUMBER);
            mCurrency = savedInstanceState.getParcelable(SS_CURRENCY);
            mComputed = savedInstanceState.getBoolean(SS_COMPUTED, false);
        } else {
            mFirstNumber = "0";
            mSecondNumber = null;
            mOperation = null;
            mCurrency = null;
            mComputed = false;
        }
        updateDisplaySafely();
    }

    public void setValue(CurrencyUnit currency, long money) {
        if (currency != null && money != 0L && currency.hasDecimals()) {
            mFirstNumber = MoneyScale.toHumanAmount(money, currency.getDecimals()).toPlainString();
        } else {
            mFirstNumber = String.valueOf(money);
        }
        mSecondNumber = null;
        mOperation = null;
        mCurrency = currency;
        mComputed = false;
        updateDisplaySafely();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SS_FIRST_NUMBER, mFirstNumber);
        outState.putString(SS_SECOND_NUMBER, mSecondNumber);
        outState.putSerializable(SS_OPERATION_NUMBER, mOperation);
        // The constructor reads this key back, and the activity only calls setValue when there is
        // no saved state, so leaving the currency out made getResult return the typed number
        // rather than its minor units after a rotation: 12.50 came back as 12, not 1250.
        outState.putParcelable(SS_CURRENCY, mCurrency);
        // Same trap as the currency above: a key the bundle does not carry comes back as the
        // default, so an answer still on the display after a rotation would be truncated rather
        // than rounded when it reaches the ledger.
        outState.putBoolean(SS_COMPUTED, mComputed);
    }

    public void clear() {
        mFirstNumber = "0";
        mSecondNumber = null;
        mOperation = null;
        mComputed = false;
        updateDisplaySafely();
    }

    public void cancel() {
        if (mSecondNumber != null && !mSecondNumber.isEmpty()) {
            mSecondNumber = mSecondNumber.substring(0, mSecondNumber.length() - 1);
        } else if (mOperation != null) {
            mOperation = null;
        } else if (mFirstNumber != null && !mFirstNumber.isEmpty()) {
            String number = mFirstNumber.substring(0, mFirstNumber.length() - 1);
            if (number.isEmpty()) {
                number = "0";
            }
            clearComputedIfValueChanged(number);
            mFirstNumber = number;
        }
        updateDisplaySafely();
    }

    public void appendOperation(Operation operation) {
        if (mOperation == null || execute(false)) {
            mOperation = operation;
            updateDisplaySafely();
        }
    }

    public void appendPoint() {
        String number = mOperation == null ? mFirstNumber : mSecondNumber;
        if (number == null || number.isEmpty() || number.equals("0")) {
            number = "0.";
        } else if (!number.contains(".")) {
            number += ".";
        }
        // No clearComputedIfValueChanged here: a point is the one key that cannot change the
        // amount. Every string this method can build parses to the number already on the display.
        mFirstNumber = mOperation == null ? number : mFirstNumber;
        mSecondNumber = mOperation == null ? null : number;
        updateDisplaySafely();
    }

    public void appendNumber(String digit) {
        String number = mOperation == null ? mFirstNumber : mSecondNumber;
        if (number == null || number.isEmpty() || number.equals("0")) {
            number = digit.equals("000") ? "0" : digit;
        } else {
            number += digit;
        }
        clearComputedIfValueChanged(number);
        mFirstNumber = mOperation == null ? number : mFirstNumber;
        mSecondNumber = mOperation == null ? null : number;
        updateDisplaySafely();
    }

    public boolean isPendingOperation() {
        return mOperation != null;
    }

    public boolean execute(boolean fireCallback) {
        BigDecimal first = parseNumber(mFirstNumber);
        BigDecimal second = parseNumber(mSecondNumber);
        try {
            BigDecimal result;
            switch (mOperation) {
                case ADDITION:
                    result = first.add(second);
                    break;
                case SUBTRACTION:
                    result = first.subtract(second);
                    break;
                case MULTIPLICATION:
                    result = first.multiply(second);
                    break;
                case DIVISION:
                    // divide(divisor, roundingMode) returns a value whose scale is the dividend's,
                    // so a 10 typed with no decimals rounded 10 / 4 to 2. Divide at 16 significant
                    // digits instead. Narrowing to the currency scale here would narrow every step
                    // of a chain rather than the answer: with a zero decimal currency, 10.0 / 4 * 4
                    // would give 8. getResult narrows the finished equation once.
                    result = first.divide(second, MathContext.DECIMAL64);
                    break;
                default:
                    result = BigDecimal.valueOf(0);
                    break;
            }
            // String.valueOf renders a small enough result in scientific notation, the product
            // 0.001 * 0.0001 as 1E-7, and the next keypress appends to whatever string lands here.
            mFirstNumber = result.toPlainString();
            if (mFirstNumber.endsWith(".0")) {
                mFirstNumber = mFirstNumber.substring(0, mFirstNumber.length() - 2);
            }
            mSecondNumber = null;
            mOperation = null;
            // An operation that hands the amount back unchanged has computed nothing the ledger
            // can see, and the display still reads the number that was there before it. The equals
            // key runs the equation whether or not a second number was entered, and appendOperation
            // runs it again when a second operator is pressed, so both reach here against the zero
            // parseNumber returns for a second number nobody typed. Adding that zero and
            // multiplying by one land in the same place: leave the flag as it was found, so a typed
            // amount stays typed and an answer stays an answer.
            if (result.compareTo(first) != 0) {
                mComputed = true;
            }
            if (fireCallback) {
                updateDisplaySafely();
            }
            return true;
        } catch (ArithmeticException ignore) {
            ignore.printStackTrace();
        }
        return false;
    }

    public long getResult() {
        if (isPendingOperation() && !execute(false)) {
            // Error occurred during calculation
            return 0L;
        }
        BigDecimal parsedNumber = parseNumber(mFirstNumber);
        if (mCurrency != null) {
            if (mComputed) {
                // An answer is rounded to the currency scale, so 20.00 / 3 stores 667, which is
                // what master stored for it when it rounded the quotient at the dividend's scale.
                // A typed number falls through to toMinorUnits, which truncates what will not fit:
                // 64.9 on a zero decimal currency stays 64, as the entry tests pin.
                parsedNumber = parsedNumber.setScale(mCurrency.getDecimals(), RoundingMode.HALF_EVEN);
            }
            return MoneyScale.toMinorUnits(parsedNumber, mCurrency.getDecimals());
        }
        return parsedNumber.longValue();
    }

    /**
     * Clears the computed flag when the candidate first number differs in value from the one on
     * the display. Compared as numbers, not as strings: appending a zero to an answer, or
     * backspacing one off it, leaves the display reading the same amount, and a keypress that
     * changes nothing does not retype what is there.
     */
    private void clearComputedIfValueChanged(String number) {
        if (mOperation == null && parseNumber(number).compareTo(parseNumber(mFirstNumber)) != 0) {
            mComputed = false;
        }
    }

    private BigDecimal parseNumber(String number) {
        String safe = number == null || number.isEmpty() ? "0" : number;
        if (safe.endsWith(".")) {
            safe = safe.substring(0, safe.length() - 1);
        }
        try {
            return new BigDecimal(safe);
        } catch (NumberFormatException ignore) {}
        return BigDecimal.valueOf(0);
    }

    public enum Operation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION
    }

    private void updateDisplaySafely() {
        if (mController != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(mFirstNumber);
            if (mOperation != null) {
                switch (mOperation) {
                    case ADDITION:
                        builder.append(" + ");
                        break;
                    case SUBTRACTION:
                        builder.append(" − ");
                        break;
                    case MULTIPLICATION:
                        builder.append(" × ");
                        break;
                    case DIVISION:
                        builder.append(" ÷ ");
                        break;
                }
                if (mSecondNumber != null) {
                    builder.append(mSecondNumber);
                }
            }
            mController.onUpdateDisplay(builder.toString());
        }
    }

    public interface Controller {

        void onUpdateDisplay(String text);
    }
}