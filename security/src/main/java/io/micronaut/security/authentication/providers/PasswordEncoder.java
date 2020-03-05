/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.security.authentication.providers;

/**
 * Responsible for determining if a given password matches
 * its encoded state and encoding raw passwords.
 *
 * @author Sergio del Amo
 * @since 1.0
 * @deprecated This class will be removed in a future version
 */
@Deprecated
public interface PasswordEncoder {

    /**
     * @param rawPassword The plain text password
     * @return The result of encoding the password
     */
    String encode(String rawPassword);

    /**
     *
     * @param rawPassword The plain text password
     * @param encodedPassword The encoded password to match against
     * @return true if the passwords match
     */
    boolean matches(String rawPassword, String encodedPassword);
}
