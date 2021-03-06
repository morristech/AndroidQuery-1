/**
 * Copyright 2013-present memtrip LTD.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.frju.androidquery.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.frju.androidquery.operation.condition.Condition;
import net.frju.androidquery.operation.join.Join;
import net.frju.androidquery.operation.keyword.Limit;
import net.frju.androidquery.operation.keyword.OrderBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Samuel Kirton [sam@memtrip.com]
 */
public abstract class BaseLocalDatabaseProvider extends DatabaseProvider {

    private final SQLiteDatabase mDatabase;
    private final String[] mSchemaArray;
    private final String[][] mColumnsSqlArray;
    private final String[] mTableRealNameArray;
    private final String[] mCreateIndexQuery;
    private final List<String> mIndexNames;

    public BaseLocalDatabaseProvider(Context context) {

        Class<?> modelClassDef[] = getResolver().getModelsForProvider(this.getClass());
        int modelCount = modelClassDef.length;

        mSchemaArray = new String[modelCount];
        mTableRealNameArray = new String[modelCount];
        mColumnsSqlArray = new String[modelCount][];
        mCreateIndexQuery = new String[modelCount];
        mIndexNames = new ArrayList<>();

        for (int i = 0; i < modelClassDef.length; i++) {
            TableDescription tableDescription = getResolver().getTableDescription(modelClassDef[i]);
            mSchemaArray[i] = tableDescription.getTableCreateQuery();
            mColumnsSqlArray[i] = tableDescription.getColumnsSqlArray();
            mTableRealNameArray[i] = tableDescription.getTableRealName();
            mCreateIndexQuery[i] = tableDescription.getCreateIndexQuery();

            Collections.addAll(mIndexNames, tableDescription.getIndexNames());
        }

        SQLiteOpenHelper openHelper = new SQLiteOpenHelper(context, getDbName(), null, getDbVersion()) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                BaseLocalDatabaseProvider.this.onCreate(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                BaseLocalDatabaseProvider.this.onUpgrade(db, oldVersion, newVersion);
            }
        };

        //TODO should handle error cases and notably the corrupted database one: we could reconstruct it
        mDatabase = openHelper.getWritableDatabase();
    }

    protected abstract String getDbName();

    protected abstract int getDbVersion();

    protected void onCreate(SQLiteDatabase db) {
        for (String schema : mSchemaArray) {
            db.execSQL(schema);
        }

        for (String createIndex : mCreateIndexQuery) {
            if (createIndex != null) {
                db.execSQL(createIndex);
            }
        }
    }

    protected void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            for (int i = 0; i < mTableRealNameArray.length; i++) {
                String tableName = mTableRealNameArray[i];

                // By default, create new columns
                for (String columnsSql : mColumnsSqlArray[i]) {
                    try {
                        db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnsSql + ";");
                    } catch (SQLException e) {
                        // columns already exists, nothing to do
                    }
                }

                //TODO also create new indexes, constraints, ...
            }
        }
    }

    protected int bulkInsert(String tableName, ContentValues[] valuesArray) {
        int nbInsert = 0;
        mDatabase.beginTransaction();

        for (ContentValues values : valuesArray) {
            if (mDatabase.insert(tableName, null, values) != -1) {
                nbInsert++;
            }
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        return nbInsert;
    }

    protected int bulkUpdate(String tableName, ContentValues[] valuesArray, Condition[][] conditionsArray) {
        int nbUpdate = 0;
        mDatabase.beginTransaction();

        for (int i = 0; i < valuesArray.length; i++) {
            nbUpdate += mDatabase.update(
                    tableName,
                    valuesArray[i],
                    mClauseHelper.getCondition(conditionsArray[i]),
                    mClauseHelper.getConditionArgs(conditionsArray[i])
            );
        }

        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        return nbUpdate;
    }

    protected Cursor query(String tableName, String[] columns, Condition[] condition, Join[] joins,
                           String groupBy, String having, OrderBy[] orderBy, Limit limit) {

        if (joins != null && joins.length > 0) {
            try {
                String joinQuery = mClauseHelper.buildJoinQuery(
                        columns,
                        joins,
                        tableName,
                        condition,
                        orderBy,
                        limit,
                        getResolver()
                );

                return mDatabase.rawQuery(joinQuery, mClauseHelper.getConditionArgs(condition));
            } catch (Exception e) {
                throw new SQLException(e.getMessage());
            }
        } else {
            return mDatabase.query(
                    tableName,
                    columns,
                    mClauseHelper.getCondition(condition),
                    mClauseHelper.getConditionArgs(condition),
                    groupBy,
                    having,
                    mClauseHelper.getOrderBy(orderBy),
                    mClauseHelper.getLimit(limit)
            );
        }
    }

    protected int delete(String tableName, Condition[] condition) {
        return mDatabase.delete(
                tableName,
                mClauseHelper.getCondition(condition),
                mClauseHelper.getConditionArgs(condition)
        );
    }

    protected long count(String tableName, Condition[] condition) {
        return DatabaseUtils.queryNumEntries(
                mDatabase,
                tableName,
                mClauseHelper.getCondition(condition),
                mClauseHelper.getConditionArgs(condition)
        );
    }

    protected Cursor rawQuery(String sql) {
        return mDatabase.rawQuery(sql, null);
    }

    SQLiteDatabase getDatabase() {
        return mDatabase;
    }
}