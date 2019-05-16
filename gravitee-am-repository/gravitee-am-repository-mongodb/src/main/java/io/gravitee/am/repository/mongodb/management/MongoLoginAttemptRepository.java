/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.repository.mongodb.management;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.am.common.utils.RandomString;
import io.gravitee.am.model.LoginAttempt;
import io.gravitee.am.repository.management.api.LoginAttemptRepository;
import io.gravitee.am.repository.mongodb.management.internal.model.LoginAttemptMongo;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoLoginAttemptRepository extends AbstractManagementMongoRepository implements LoginAttemptRepository {

    private static final String FIELD_ID = "_id";
    private MongoCollection<LoginAttemptMongo> loginAttemptsCollection;

    @PostConstruct
    public void init() {
        loginAttemptsCollection = mongoOperations.getCollection("login_attempts", LoginAttemptMongo.class);
    }

    @Override
    public Maybe<LoginAttempt> findById(String id) {
        return Observable.fromPublisher(loginAttemptsCollection.find(eq(FIELD_ID, id)).first()).firstElement().map(this::convert);
    }

    @Override
    public Single<LoginAttempt> create(LoginAttempt item) {
        LoginAttemptMongo loginAttempt = convert(item);
        loginAttempt.setId(loginAttempt.getId() == null ? RandomString.generate() : loginAttempt.getId());
        return Single.fromPublisher(loginAttemptsCollection.insertOne(loginAttempt)).flatMap(success -> findById(loginAttempt.getId()).toSingle());
    }

    @Override
    public Single<LoginAttempt> update(LoginAttempt item) {
        LoginAttemptMongo loginAttempt = convert(item);
        return Single.fromPublisher(loginAttemptsCollection.replaceOne(eq(FIELD_ID, loginAttempt.getId()), loginAttempt)).flatMap(success -> findById(loginAttempt.getId()).toSingle());
    }

    @Override
    public Completable delete(String id) {
        return Completable.fromPublisher(loginAttemptsCollection.deleteOne(eq(FIELD_ID, id)));
    }

    private LoginAttempt convert(LoginAttemptMongo loginAttemptMongo) {
        if (loginAttemptMongo == null) {
            return null;
        }
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setId(loginAttemptMongo.getId());
        loginAttempt.setUserId(loginAttemptMongo.getUserId());
        loginAttempt.setUsername(loginAttemptMongo.getUsername());
        loginAttempt.setAttempts(loginAttemptMongo.getAttempts());
        loginAttempt.setCreatedAt(loginAttemptMongo.getCreatedAt());
        loginAttempt.setUpdatedAt(loginAttemptMongo.getUpdatedAt());
        return loginAttempt;
    }

    private LoginAttemptMongo convert(LoginAttempt loginAttempt) {
        if (loginAttempt == null) {
            return null;
        }
        LoginAttemptMongo loginAttemptMongo = new LoginAttemptMongo();
        loginAttemptMongo.setId(loginAttempt.getId());
        loginAttemptMongo.setUserId(loginAttempt.getUserId());
        loginAttemptMongo.setUsername(loginAttempt.getUsername());
        loginAttemptMongo.setAttempts(loginAttempt.getAttempts());
        loginAttemptMongo.setCreatedAt(loginAttempt.getCreatedAt());
        loginAttemptMongo.setUpdatedAt(loginAttempt.getUpdatedAt());
        return loginAttemptMongo;
    }
}
