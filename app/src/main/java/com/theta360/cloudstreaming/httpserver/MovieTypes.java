/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.cloudstreaming.httpserver;

/**
 * Define the video type to stream
 */
public enum MovieTypes {
    Movie4k("MOVIE4K", 1),
    Movie2k("MOVIE2K", 2),
    Movie1k("MOVIE1K", 3),
    Movie06k("MOVIE06K", 4),
    Movie4k_15fps("MOVIE4K_15FPS", 5),
    Movie2k_15fps("MOVIE2K_15FPS", 6),
    Movie1k_15fps("MOVIE1K_15FPS", 7),
    ;

    private final String name;
    private final int id;

    MovieTypes(final String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public String getString() {
        return this.name;
    }

    public int getInt() {
        return this.id;
    }

}
