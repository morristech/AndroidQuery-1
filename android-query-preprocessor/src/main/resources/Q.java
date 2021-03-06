package ${package_name};

import android.content.Context;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import net.frju.androidquery.database.*;
import net.frju.androidquery.operation.function.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class Q {

    private static DefaultResolver sResolver;

    public static void init(Context context) {
        if (sResolver == null) {
            sResolver = new DefaultResolver();
            sResolver.init(context);
        }
    }

    public static DefaultResolver getResolver() {
        return sResolver;
    }

    public static class DefaultResolver implements Resolver {

        private static HashMap<Class<?>, BaseLocalDatabaseProvider> mLocalProviders = new HashMap<>();
        private static HashMap<Class<?>, BaseContentDatabaseProvider> mContentProviders = new HashMap<>();

        public void init(@NonNull Context context) {
            BaseLocalDatabaseProvider localProvider;
            BaseContentDatabaseProvider contentProvider;

            <#list tables as table>
            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
                    localProvider = new ${table.getLocalDatabaseProvider().toString()}(context.getApplicationContext());
                    mLocalProviders.put(${table.getPackage()}.${table.getName()}.class, localProvider);
                    mLocalProviders.put(${table.getName()}.class, localProvider); // to be more error-tolerant
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
                    contentProvider = new ${table.getContentDatabaseProvider().toString()}(context.getContentResolver());
                    mContentProviders.put(${table.getPackage()}.${table.getName()}.class, contentProvider);
                    mContentProviders.put(${table.getName()}.class, contentProvider); // to be more error-tolerant
            </#if>
            </#list>
        }

        @Override
        public @NonNull TableDescription getTableDescription(@NonNull Class<?> classDef) {
            <#assign isAssignableFrom>
                <#list tables as table>
                } else if (classDef.isAssignableFrom(${table.getPackage()}.${table.getName()}.class)) {
                    return s${table.getName()};
                </#list>
                }
            </#assign>

            ${isAssignableFrom?trim?remove_beginning("} else ")} else {
                throw new IllegalStateException("Please ensure all SQL tables are annotated with @Table");
            }
        }

        @Override
        public @NonNull Class<?>[] getModelsForProvider(Class<? extends DatabaseProvider> providerClass) {
            ArrayList<Class<?>> result = new ArrayList<>();

            <#list tables as table>
            if (${table.getLocalDatabaseProvider().toString()}.class.equals(providerClass) || ${table.getContentDatabaseProvider().toString()}.class.equals(providerClass)) {
                result.add(${table.getPackage()}.${table.getName()}.class);
            }
            </#list>

            if (result.size() == 0) {
                throw new IllegalStateException("This provider does not have any @Table models registered into that resolver");
            }

            return result.toArray(new Class<?>[result.size()]);
        }

        @Override
        public BaseLocalDatabaseProvider getLocalDatabaseProviderForModel(Class<?> model) {
            return mLocalProviders.get(model);
        }

        @Override
        public BaseContentDatabaseProvider getContentDatabaseProviderForModel(Class<?> model) {
            return mContentProviders.get(model);
        }
    }

    <#list tables as table>

        <#assign getColumnNames>
            <#list table.getMutableColumns(tables) as column>
                "${column.getRealName()}",
            </#list>
        </#assign>
        <#assign getColumnNamesWithTablePrefix>
            <#list table.getMutableColumns(tables) as column>
                "${table.getRealName()}.${column.getRealName()}",
            </#list>
        </#assign>

        <#assign unionInsertColumnNames><#list table.getMutableColumns(tables) as column>${column.getRealName()},</#list></#assign>

        <#assign packagedTableName>
            ${table.getPackage()}.${table.getName()}
        </#assign>

        public static class ${table.getName()} implements TableDescription {

            <#list table.getColumns() as column>
                public static final String ${formatConstant(column.getName())} = "${column.getRealName()}";
            </#list>

            @Override
            public @NonNull String getTableRealName() {
                return "${table.getRealName()}";
            }

            @Override
            public @NonNull String getTableCreateQuery() {
                return ${assembleCreateTable(table, tables)}
            }

            @Override
            public @NonNull String[] getColumnsSqlArray() {
                return ${getColumnsSqlArray(table, tables)};
            }

            @Override
            public String getPrimaryKeyRealName() {
                return "${table.getPrimaryKeyRealName()}";
            }

            @Override
            public String[] getIndexNames() {
                return new String[]{
                <#list table.getMutableColumns(tables) as column>
                    <#if column.isIndex()>
                        "${table.getName()}_${column.getName()}_index",
                    </#if>
                </#list>
                };
            }

            @Override
            public String getCreateIndexQuery() {
                StringBuilder sb = new StringBuilder();

                <#list table.getMutableColumns(tables) as column>
                    <#if column.isIndex()>
                        sb.append("CREATE INDEX ${table.getName()}_${column.getName()}_index ON ${table.getRealName()} (${column.getRealName()});");
                    </#if>
                </#list>

                return (sb.length() > 0) ? sb.toString() : null;
            }

            @Override
            public ${packagedTableName} getSingleResult(Cursor cursor) {
                if (cursor != null){
                    ${packagedTableName} ${table.getName()?lower_case} = new ${packagedTableName}();

                    ${joinReferences(table.getName(),tables)}

                    for (int x = 0; x < cursor.getColumnCount(); x++) {
                        <#assign retrieveSQLSelectResults>
                            <#list table.getColumns() as column>
                                <#if column.isJoinable(tables)>
                                    ${join(column.getClassName(),tables)}
                                <#else>
                                    } else if (cursor.getColumnName(x).equals(${formatConstant(column.getName())})) {
                                        ${table.getName()?lower_case}.${column.getName()} = ${getCursorGetter(column.getType())};
                                </#if>
                            </#list>
                            }
                        </#assign>

                        ${retrieveSQLSelectResults?trim?remove_beginning("} else ")}
                    }

                    return ${table.getName()?lower_case};
                }

                return null;
            }

            @Override
            public ${packagedTableName}[] getArrayResult(Cursor cursor) {
                if (cursor != null){
                    ${packagedTableName}[]result = new ${packagedTableName}[cursor.getCount()];

                    cursor.moveToFirst();
                    for(int i=0;!cursor.isAfterLast();i++){
                        result[i]=getSingleResult(cursor);
                        cursor.moveToNext();
                    }

                    cursor.close();

                    return result;
                }

                return null;
            }

            @Override
            public String[] getColumnNames() {
                return new String[]{${getColumnNames?remove_ending(",")}};
            }

            @Override
            public String[] getColumnNamesWithTablePrefix() {
                return new String[]{${getColumnNamesWithTablePrefix?remove_ending(",")}};
            }

            @Override
            public Object getPrimaryKeyValue(@NonNull Object model) {
                ${packagedTableName} ${table.getName()?lower_case} = (${packagedTableName})model;

                return ${getPrimaryKeyValue(table.getName()?lower_case, table)};
            }

            @Override
            public boolean isPrimaryKeyAutoIncrement() {
                return ${isPrimaryKeyAutoIncrement(table)};
            }

            @Override
            public @NonNull ContentValues getContentValues(@NonNull Object model) {
                ${packagedTableName} ${table.getName()?lower_case} = (${packagedTableName})model;

                ContentValues contentValues = new ContentValues();

                <#list table.getMutableColumns(tables) as column>
                    <#if !column.hasAutoIncrement()>
                    contentValues.put(${formatConstant(column.getName())}, ${getContentValue(table.getName()?lower_case, column)});
                    </#if>
                </#list>

                return contentValues;
            }

            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Uri getContentUri() {
                return Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class).getUri(${packagedTableName}.class);
            }
            </#if>

            public static @NonNull Count.Builder<${packagedTableName}> count() {
                <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
                return Count.getBuilder(${packagedTableName}.class, Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class));
                <#else>
                return Count.getBuilder(${packagedTableName}.class, Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class));
                </#if>
            }

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Select.Builder<${packagedTableName}> select() {
                return Select.getBuilder(${packagedTableName}.class, Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Select.Builder<${packagedTableName}> selectViaContentProvider() {
                return Select.getBuilder(${packagedTableName}.class, Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Delete.Builder<${packagedTableName}> delete() {
                return Delete.getBuilder(${packagedTableName}.class, Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Delete.Builder<${packagedTableName}> deleteViaContentProvider() {
                return Delete.getBuilder(${packagedTableName}.class, Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Update.Builder<${packagedTableName}> update() {
                return Update.getBuilder(${packagedTableName}.class, Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Update.Builder<${packagedTableName}> updateViaContentProvider() {
                return Update.getBuilder(${packagedTableName}.class, Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class));
            }
            </#if>

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Save.Builder<${packagedTableName}> save(@NonNull ${packagedTableName}... models) {
                return Save.getBuilder(Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Save.Builder<${packagedTableName}> saveViaContentProvider(@NonNull ${packagedTableName}... models) {
                return Save.getBuilder(Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Save.Builder<${packagedTableName}> save(@NonNull List<${packagedTableName}> models) {
                return Save.getBuilder(Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Save.Builder<${packagedTableName}> saveViaContentProvider(@NonNull List<${packagedTableName}> models) {
                return Save.getBuilder(Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Insert.Builder<${packagedTableName}> insert(@NonNull ${packagedTableName}... models) {
                return Insert.getBuilder(Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Insert.Builder<${packagedTableName}> insertViaContentProvider(@NonNull ${packagedTableName}... models) {
                return Insert.getBuilder(Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Insert.Builder<${packagedTableName}> insert(@NonNull List<${packagedTableName}> models) {
                return Insert.getBuilder(Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>
            <#if table.getContentDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Insert.Builder<${packagedTableName}> insertViaContentProvider(@NonNull List<${packagedTableName}> models) {
                return Insert.getBuilder(Q.getResolver().getContentDatabaseProviderForModel(${packagedTableName}.class), models);
            }
            </#if>

            <#if table.getLocalDatabaseProvider().toString() != "java.lang.Void">
            public static @NonNull Raw.Builder raw(@NonNull String query) {
                return Raw.getBuilder(Q.getResolver().getLocalDatabaseProviderForModel(${packagedTableName}.class), query);
            }
            </#if>

            public static @NonNull CursorResult<${packagedTableName}> fromCursor(Cursor cursor) {
                return new CursorResult<>(${packagedTableName}.class, Q.getResolver(), cursor);
            }
        }

        private static ${table.getName()} s${table.getName()} = new ${table.getName()}();

    </#list>
}