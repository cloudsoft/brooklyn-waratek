/*
 * Copyright 2014 by Cloudsoft Corporation Limited
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
package com.waratek;

/**
 * Placeholder class for JVC definition.
 */
public class Brooklyn {

    public static final String BANNER =
        " _                     _    _             \n" +
        "| |__  _ __ ___   ___ | | _| |_   _ _ __ (R)\n" +
        "| '_ \\| '__/ _ \\ / _ \\| |/ / | | | | '_ \\ \n" +
        "| |_) | | | (_) | (_) |   <| | |_| | | | |\n" +
        "|_.__/|_|  \\___/ \\___/|_|\\_\\_|\\__, |_| |_|\n" +
        "                              |___/             \n"+
        "${project.artifactId} ${project.version}\n";

    public static void main(String...args) {
        System.out.print(BANNER);
    }

}
