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

package io.github.minecraftcursedlegacy.installer.client;

import io.github.minecraftcursedlegacy.installer.util.InstallerProgress;
import io.github.minecraftcursedlegacy.installer.util.MinecraftLaunchJson;
import io.github.minecraftcursedlegacy.installer.util.Utils;
import io.github.minecraftcursedlegacy.installer.util.data.Reference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClientInstaller {

	public static String install(File mcDir, String gameVersion, String loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion, gameVersion);

		MinecraftLaunchJson launchJson = Utils.getLaunchMeta(loaderVersion);
		launchJson.id = profileName;
		launchJson.inheritsFrom = gameVersion;

		//Adds loader and the mappings
		launchJson.libraries.add(new MinecraftLaunchJson.Library("net.fabricmc:intermediary:1.5.2", "https://maven.legacyfabric.net/"));
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + Reference.LOADER_NAME + ":" + loaderVersion, Reference.mavenServerUrl));

		File versionsDir = new File(mcDir, "versions");
		File profileDir = new File(versionsDir, profileName);
		File profileJson = new File(profileDir, profileName + ".json");

		if (!profileDir.exists()) {
			profileDir.mkdirs();
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		File dummyJar = new File(profileDir, profileName + ".jar");
		dummyJar.createNewFile();

		Utils.writeToFile(profileJson, Utils.GSON.toJson(launchJson));

		if (loaderVersion.contains("-btw")) {
			File loaderLibDir = new File(mcDir,
					"libraries/" + Reference.PACKAGE + "/" + Reference.LOADER_NAME + "/" + loaderVersion);

			if (!loaderLibDir.exists()) {
				loaderLibDir.mkdirs();
			}
			File loaderLibJar = new File(loaderLibDir, Reference.LOADER_NAME + "-" + loaderVersion + ".jar");
			loaderLibJar.createNewFile();

			InputStream loader_in = ClientInstaller.class.getClassLoader().getResource("cursed-fabric-loader.zip").openStream();
			FileOutputStream f = new FileOutputStream(loaderLibJar);
			while (true) {
				int in = loader_in.read();
				if (in == -1) break;
				f.write(in);
			}
		}


		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}
