package net.frju.androidquery.database;

import android.content.ContentValues;
import android.database.Cursor;

import net.frju.androidquery.operation.function.Count;
import net.frju.androidquery.operation.function.Delete;
import net.frju.androidquery.operation.function.Insert;
import net.frju.androidquery.operation.function.Result;
import net.frju.androidquery.operation.function.Select;
import net.frju.androidquery.operation.function.Update;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public abstract class Query {

    protected static int insert(Insert insert, Class<?> classDef, DatabaseProvider databaseProvider) {
        if (insert.getModels() != null && insert.getModels().length > 0) {
            Object[] models = insert.getModels();
            ContentValues[] valuesArray = new ContentValues[models.length];
            TableDescription tableDescription = getTableDescription(classDef, databaseProvider);
            for (int i = 0; i < models.length; i++) {
                valuesArray[i] = tableDescription.getContentValues(models[i]);
            }
            return databaseProvider.bulkInsert(tableDescription.getTableRealName(), valuesArray);
        }

        return 0;
    }

    protected static Cursor selectCursor(Select select, Class<?> classDef, DatabaseProvider databaseProvider) {

        TableDescription tableDescription = getTableDescription(classDef, databaseProvider);

        return databaseProvider.query(
                tableDescription.getTableRealName(),
                select.getJoin() != null ? tableDescription.getColumnNamesWithTablePrefix() : tableDescription.getColumnNames(),
                select.getClause(),
                select.getJoin(),
                null,
                null,
                select.getOrderBy(),
                select.getLimit()
        );
    }

    protected static <T> Result<T> select(Select select, Class<T> classDef, DatabaseProvider databaseProvider) {
        Cursor cursor = selectCursor(select, classDef, databaseProvider);
        return new Result<>(classDef, databaseProvider.getResolver(), cursor);
    }

    protected static <T> T selectSingle(Select select, Class<T> classDef, DatabaseProvider databaseProvider) {
        Cursor cursor = selectCursor(select, classDef, databaseProvider);

        T[] results = getTableDescription(classDef, databaseProvider).getArrayResult(cursor);

        if (results != null && results.length > 0) {
            return results[0];
        } else {
            return null;
        }
    }

    protected static int update(Update update, Class<?> classDef, DatabaseProvider databaseProvider) {
        return databaseProvider.update(
                getTableDescription(classDef, databaseProvider).getTableRealName(),
                update.getContentValues(),
                update.getConditions()
        );
    }

    protected static long count(Count count, Class<?> classDef, DatabaseProvider databaseProvider) {
        return databaseProvider.count(
                getTableDescription(classDef, databaseProvider).getTableRealName(),
                count.getClause()
        );
    }

    protected static int delete(Delete delete, Class<?> classDef, DatabaseProvider databaseProvider) {
        return databaseProvider.delete(
                getTableDescription(classDef, databaseProvider).getTableRealName(),
                delete.getConditions()
        );
    }

    protected static Cursor rawQuery(String query, DatabaseProvider databaseProvider) {
        return databaseProvider.rawQuery(query);
    }

    protected static <T> Observable<T> wrapRx(final Callable<T> func) {
        return Observable.create(
                new ObservableOnSubscribe<T>() {
                    @Override
                    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
                        try {
                            emitter.onNext(func.call());
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }
                }
        );
    }

    private static TableDescription getTableDescription(Class<?> classDef, DatabaseProvider databaseProvider) {
        return databaseProvider.getResolver().getTableDescription(classDef);
    }
}