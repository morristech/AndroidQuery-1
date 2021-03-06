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
package net.frju.androidquery.integration;

import net.frju.androidquery.gen.Q;
import net.frju.androidquery.integration.models.User;
import net.frju.androidquery.integration.utils.SetupUser;
import net.frju.androidquery.operation.condition.Where;

import org.junit.Before;
import org.junit.Test;

import static net.frju.androidquery.operation.condition.Where.where;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Samuel Kirton [sam@memtrip.com]
 */
public class CreateTest extends IntegrationTest {

    @Before
    public void setUp() {
        super.setUp();
        getSetupUser().tearDownFourTestUsers();
    }

    @Test
    public void testSingleInsert() {
        // setup
        String USER_ID = "1234567890";
        long USER_TIMESTAMP = System.currentTimeMillis();
        boolean USER_IS_REGISTERED = true;

        User user = new User();
        user.setUsername(USER_ID);
        user.setIsRegistered(USER_IS_REGISTERED);
        user.setTimestamp(USER_TIMESTAMP);

        // exercise
        Q.User.insert(user).query();

        // verify
        User responseUser = Q.User.select().querySingle();

        assertTrue(user.getUsername().equals(responseUser.getUsername()));
        assertTrue(user.getTimestamp() == responseUser.getTimestamp());
        assertTrue(user.getIsRegistered() == responseUser.getIsRegistered());
    }

    @Test
    public void testMultipleInsert() {
        // setup
        int ANGIE_ID = 1;
        String ANGIE_USERNAME = "angie";
        long ANGIE_TIMESTAMP = System.currentTimeMillis();
        boolean ANGIE_IS_REGISTERED = true;
        double ANGIE_RATING = 2.7;
        int ANGIE_COUNT = 1028;

        int SAM_ID = 2;
        String SAM_USERNAME = "sam";
        long SAM_TIMESTAMP = System.currentTimeMillis() + 1000;
        boolean SAM_IS_REGISTERED = false;
        double SAM_RATING = 2.7;
        int SAM_COUNT = 10024;

        User[] users = new User[]{
                SetupUser.createUser(
                        ANGIE_ID,
                        ANGIE_USERNAME,
                        ANGIE_TIMESTAMP,
                        ANGIE_IS_REGISTERED,
                        ANGIE_RATING,
                        ANGIE_COUNT,
                        0
                ),

                SetupUser.createUser(
                        SAM_ID,
                        SAM_USERNAME,
                        SAM_TIMESTAMP,
                        SAM_IS_REGISTERED,
                        SAM_RATING,
                        SAM_COUNT,
                        0
                ),
        };

        // exercise
        Q.User.insert(users).query();

        // verify
        User angieUser = Q.User.select()
                .where(where(Q.User.USERNAME, Where.Op.IS, ANGIE_USERNAME))
                .querySingle();

        User samUser = Q.User.select()
                .where(where(Q.User.USERNAME, Where.Op.IS, SAM_USERNAME))
                .querySingle();

        assertEquals(ANGIE_USERNAME, angieUser.getUsername());
        assertEquals(ANGIE_TIMESTAMP, angieUser.getTimestamp());
        assertEquals(ANGIE_IS_REGISTERED, angieUser.getIsRegistered());
        assertEquals(ANGIE_RATING, angieUser.getRating(), 0.1f);
        assertEquals(ANGIE_COUNT, angieUser.getCount());

        assertEquals(SAM_USERNAME, samUser.getUsername());
        assertEquals(SAM_TIMESTAMP, samUser.getTimestamp());
        assertEquals(SAM_IS_REGISTERED, samUser.getIsRegistered());
        assertEquals(SAM_RATING, samUser.getRating(), 0.1f);
        assertEquals(SAM_COUNT, samUser.getCount());
    }

    @Test
    public void testMoreThan500RowInsert() {
        int COLUMN_COUNT = 1350;
        int ANGIE_ID = 1;
        String ANGIE_USERNAME = "angie";
        long ANGIE_TIMESTAMP = System.currentTimeMillis();
        boolean ANGIE_IS_REGISTERED = true;
        double ANGIE_RATING = 1.0;
        int ANGIE_COUNT = 300;

        User[] users = new User[COLUMN_COUNT];
        for (int i = 0; i < COLUMN_COUNT; i++) {
            users[i] = SetupUser.createUser(
                    ANGIE_ID,
                    ANGIE_USERNAME,
                    ANGIE_TIMESTAMP + i,
                    ANGIE_IS_REGISTERED,
                    ANGIE_RATING,
                    ANGIE_COUNT,
                    0
            );
        }

        Q.User.insert(users).query();

        User[] usersInserted = Q.User.select().query().toArray();

        for (int i = 0; i < usersInserted.length; i++) {
            assertEquals(ANGIE_TIMESTAMP + i, usersInserted[i].getTimestamp());
        }

        assertEquals(COLUMN_COUNT, usersInserted.length);
    }
}