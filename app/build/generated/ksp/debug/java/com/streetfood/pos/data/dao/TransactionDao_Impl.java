package com.streetfood.pos.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.streetfood.pos.data.database.Converters;
import com.streetfood.pos.data.models.Transaction;
import com.streetfood.pos.data.models.TransactionItem;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Transaction> __insertionAdapterOfTransaction;

  private final Converters __converters = new Converters();

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransaction = new EntityInsertionAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `transactions` (`id`,`timestamp`,`totalAmount`,`cashReceived`,`change`,`cashierName`,`items`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindDouble(3, entity.getTotalAmount());
        statement.bindDouble(4, entity.getCashReceived());
        statement.bindDouble(5, entity.getChange());
        statement.bindString(6, entity.getCashierName());
        final String _tmp = __converters.fromCartItemList(entity.getItems());
        statement.bindString(7, _tmp);
      }
    };
  }

  @Override
  public Object insertTransaction(final Transaction transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTransaction.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Transaction>> getAllTransactions() {
    final String _sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfCashReceived = CursorUtil.getColumnIndexOrThrow(_cursor, "cashReceived");
          final int _cursorIndexOfChange = CursorUtil.getColumnIndexOrThrow(_cursor, "change");
          final int _cursorIndexOfCashierName = CursorUtil.getColumnIndexOrThrow(_cursor, "cashierName");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpCashReceived;
            _tmpCashReceived = _cursor.getDouble(_cursorIndexOfCashReceived);
            final double _tmpChange;
            _tmpChange = _cursor.getDouble(_cursorIndexOfChange);
            final String _tmpCashierName;
            _tmpCashierName = _cursor.getString(_cursorIndexOfCashierName);
            final List<TransactionItem> _tmpItems;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfItems);
            _tmpItems = __converters.toCartItemList(_tmp);
            _item = new Transaction(_tmpId,_tmpTimestamp,_tmpTotalAmount,_tmpCashReceived,_tmpChange,_tmpCashierName,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTransactionById(final int id,
      final Continuation<? super Transaction> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Transaction>() {
      @Override
      @Nullable
      public Transaction call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfCashReceived = CursorUtil.getColumnIndexOrThrow(_cursor, "cashReceived");
          final int _cursorIndexOfChange = CursorUtil.getColumnIndexOrThrow(_cursor, "change");
          final int _cursorIndexOfCashierName = CursorUtil.getColumnIndexOrThrow(_cursor, "cashierName");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final Transaction _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpCashReceived;
            _tmpCashReceived = _cursor.getDouble(_cursorIndexOfCashReceived);
            final double _tmpChange;
            _tmpChange = _cursor.getDouble(_cursorIndexOfChange);
            final String _tmpCashierName;
            _tmpCashierName = _cursor.getString(_cursorIndexOfCashierName);
            final List<TransactionItem> _tmpItems;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfItems);
            _tmpItems = __converters.toCartItemList(_tmp);
            _result = new Transaction(_tmpId,_tmpTimestamp,_tmpTotalAmount,_tmpCashReceived,_tmpChange,_tmpCashierName,_tmpItems);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTotalRevenue(final Continuation<? super Double> $completion) {
    final String _sql = "SELECT SUM(totalAmount) FROM transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTotalTransactions(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
