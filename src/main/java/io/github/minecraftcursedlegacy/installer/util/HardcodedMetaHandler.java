/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.minecraftcursedlegacy.installer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.minecraftcursedlegacy.installer.util.data.GithubCommit;

public class HardcodedMetaHandler extends MetaHandler {

	private final List<GameVersion> versions = new ArrayList<>();
	
	public HardcodedMetaHandler() {
		super(null);
	}
	
	public List<GameVersion> getVersions() {
		return Collections.unmodifiableList(versions);
	}
	
	public HardcodedMetaHandler addVersion(GithubCommit version, boolean stable) {
		versions.add(new GameVersion(version, stable));
		return this;
	}

	public HardcodedMetaHandler addVersion(String version, boolean stable) {
		versions.add(new GameVersion(version, stable));
		return this;
	}
	
	public void load() {
		complete(versions);
	}
	
	public GameVersion getLatestVersion(boolean snapshot){
		return versions.get(0); // cursed legacy doesn't exactly need snapshot logic.
	}
}
