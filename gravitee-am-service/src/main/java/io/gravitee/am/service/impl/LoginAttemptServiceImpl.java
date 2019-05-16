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
package io.gravitee.am.service.impl;

import io.gravitee.am.common.utils.RandomString;
import io.gravitee.am.model.LoginAttempt;
import io.gravitee.am.repository.management.api.LoginAttemptRepository;
import io.gravitee.am.service.LoginAttemptService;
import io.gravitee.am.service.exception.AbstractManagementException;
import io.gravitee.am.service.exception.LoginAttemptNotFoundException;
import io.gravitee.am.service.exception.TechnicalManagementException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptServiceImpl.class);

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Override
    public Maybe<LoginAttempt> findById(String id) {
        LOGGER.debug("Find group by id : {}", id);
        return loginAttemptRepository.findById(id)
                .onErrorResumeNext(ex -> {
                    LOGGER.error("An error occurs while trying to find a login attempt using its ID", id, ex);
                    return Maybe.error(new TechnicalManagementException(
                            String.format("An error occurs while trying to find a login attempt using its ID: %s", id), ex));
                });
    }

    @Override
    public Single<LoginAttempt> create(LoginAttempt loginAttempt) {
        LOGGER.debug("Create a new login attempt {} ", loginAttempt);

        loginAttempt.setId(RandomString.generate());
        loginAttempt.setCreatedAt(new Date());
        loginAttempt.setUpdatedAt(loginAttempt.getCreatedAt());

        return loginAttemptRepository.create(loginAttempt)
                    .onErrorResumeNext(ex -> {
                        if (ex instanceof AbstractManagementException) {
                            return Single.error(ex);
                        } else {
                            LOGGER.error("An error occurs while trying to create a login attempt", ex);
                            return Single.error(new TechnicalManagementException("An error occurs while trying to create a login attempt", ex));
                        }
                    });
    }

    @Override
    public Single<LoginAttempt> update(String id, LoginAttempt loginAttempt) {
        LOGGER.debug("Update a login attempt {}", id);

        return loginAttemptRepository.findById(id)
                .switchIfEmpty(Maybe.error(new LoginAttemptNotFoundException(id)))
                .flatMapSingle(existingGroup -> {
                    existingGroup.setAttempts(loginAttempt.getAttempts());
                    existingGroup.setUpdatedAt(new Date());
                    return loginAttemptRepository.update(existingGroup);
                })
                .onErrorResumeNext(ex -> {
                    if (ex instanceof AbstractManagementException) {
                        return Single.error(ex);
                    }

                    LOGGER.error("An error occurs while trying to update a login attempt", ex);
                    return Single.error(new TechnicalManagementException("An error occurs while trying to update a login attempt", ex));
                });
    }

    @Override
    public Completable delete(String id) {
        LOGGER.debug("Delete login attempt {}", id);
        return loginAttemptRepository.findById(id)
                .switchIfEmpty(Maybe.error(new LoginAttemptNotFoundException(id)))
                .flatMapCompletable(group -> loginAttemptRepository.delete(id))
                .onErrorResumeNext(ex -> {
                    if (ex instanceof AbstractManagementException) {
                        return Completable.error(ex);
                    }
                    LOGGER.error("An error occurs while trying to delete login attempt: {}", id, ex);
                    return Completable.error(new TechnicalManagementException(
                            String.format("An error occurs while trying to delete login attempt: %s", id), ex));
                });
    }
}
