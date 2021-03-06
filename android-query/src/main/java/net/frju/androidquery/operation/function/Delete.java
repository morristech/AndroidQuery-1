/**
 * Copyright 2013-present memtrip LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.frju.androidquery.operation.function;

import android.support.annotation.NonNull;

import net.frju.androidquery.database.DatabaseProvider;
import net.frju.androidquery.database.Query;
import net.frju.androidquery.operation.condition.Condition;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;

/**
 * Executes a Delete query against the SQLite database
 * @author Samuel Kirton [sam@memtrip.com]
 */
public class Delete extends Query {
    private Object[] mModels;
    private Condition[] mConditions;

    public Object[] getModels() {
        return mModels;
    }

    public Condition[] getConditions() {
        return mConditions;
    }

    private Delete(Object[] models) {
        mModels = models;
    }

    private Delete(Condition[] conditions) {
        mConditions = conditions;
    }

    public static
    @NonNull
    <T> Delete.Builder getBuilder(@NonNull Class<T> classDef, @NonNull DatabaseProvider databaseProvider) {
        return new Delete.Builder<>(classDef, databaseProvider);
    }

    public static class Builder<T> {
        private T[] mModels;
        private Condition[] mCondition;
        private final Class<T> mClassDef;
        private final DatabaseProvider mDatabaseProvider;

        private Builder(@NonNull Class<T> classDef, @NonNull DatabaseProvider databaseProvider) {
            mClassDef = classDef;
            mDatabaseProvider = databaseProvider;
        }

        /**
         * Specify a Where condition for the Delete query
         * @param condition Where condition
         * @return Call Builder#query or Builder#rx to run the query
         */
        public
        @NonNull
        Builder<T> where(Condition... condition) {
            mCondition = condition;
            return this;
        }

        /**
         * Specify the values for the Delete query
         * @param models The models that are being deleted
         * @return Call Builder#query or Builder#rx to run the query
         */
        @SafeVarargs
        public final
        @NonNull
        Builder<T> model(T... models) {
            mModels = models;
            return this;
        }

        /**
         * Specify the values for the Delete query
         *
         * @param models The models that are being deleted
         * @return Call Builder#query or Builder#rx to run the query
         */
        public
        @NonNull
        Builder<T> model(List<T> models) {
            //noinspection unchecked,SuspiciousToArrayCall
            mModels = (T[]) models.toArray(new Object[models.size()]);
            return this;
        }

        /**
         * Executes a Delete query
         * @return The rows affected by the Delete query
         */
        public int query() {
            if (mModels != null) {
                return delete(
                        new Delete(mModels),
                        mClassDef,
                        mDatabaseProvider
                );
            } else {
                return delete(
                        new Delete(mCondition),
                        mClassDef,
                        mDatabaseProvider
                );
            }
        }
        /**
         * Executes a Delete query
         * @return An RxJava Observable
         */
        public
        @NonNull
        Observable<Integer> rx() {
            return wrapRx(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return query();
                }
            });
        }
    }
}