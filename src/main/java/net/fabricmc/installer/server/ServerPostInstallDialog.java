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

package net.fabricmc.installer.server;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.google.gson.JsonObject;

import io.github.minecraftcursedlegacy.installer.VersionData;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;

public class ServerPostInstallDialog extends JDialog {
	private static final int MB = 1000000;

	private JPanel panel = new JPanel();

	private ServerHandler serverHandler;
	private String minecraftVersion;
	private File installDir;
	private File minecraftJar;

	private JLabel serverJarLabel;
	private JButton downloadButton;
	private JButton generateButton;

	private ServerPostInstallDialog(ServerHandler handler) throws HeadlessException {
		super(InstallerGui.instance, true);
		this.serverHandler = handler;
		this.minecraftVersion = (String) handler.gameVersionComboBox.getSelectedItem();
		this.installDir = new File(handler.installLocation.getText());
		this.minecraftJar = new File(installDir, minecraftVersion + ".jar");

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		initComponents();
		setContentPane(panel);
		setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));
	}

	private void initComponents() {
		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("progress.done.server")), 20)));

		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("prompt.server.info.jar")), 15)));
		addRow(panel, panel -> {
			updateServerJarLabel();
			panel.add(serverJarLabel);

			downloadButton = new JButton(Utils.BUNDLE.getString("prompt.server.jar"));
			downloadButton.addActionListener(e -> doServerJarDownload());
			panel.add(downloadButton);
		});

		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("prompt.server.info.command")), 15)));
		addRow(panel, panel -> {
			JTextField textField = new JTextField("java -jar fabric-server-launch.jar");
			textField.setHorizontalAlignment(JTextField.CENTER);
			panel.add(textField);


		});

		addRow(panel, panel -> {
			panel.add(new JLabel(Utils.BUNDLE.getString("prompt.server.info.scipt")));
			generateButton = new JButton(Utils.BUNDLE.getString("prompt.server.generate"));
			generateButton.addActionListener(e -> generateLaunchScripts());
			panel.add(generateButton);
		});

		addRow(panel, panel -> {
			JButton closeButton = new JButton(Utils.BUNDLE.getString("progress.done"));
			closeButton.addActionListener(e -> {
				setVisible(false);
				dispose();
			});
			panel.add(closeButton);
		});

	}

	private boolean isValidJarPresent() {
		return minecraftJar.exists();
	}

	private void updateServerJarLabel() {
		if (serverJarLabel == null) {
			serverJarLabel = new JLabel();
		}
		if (isValidJarPresent()) {
			serverJarLabel.setText(new MessageFormat(Utils.BUNDLE.getString("prompt.server.jar.valid")).format(new Object[]{minecraftVersion}));
			color(serverJarLabel, Color.GREEN.darker());
		} else {
			serverJarLabel.setText(new MessageFormat(Utils.BUNDLE.getString("prompt.server.jar.invalid")).format(new Object[]{minecraftVersion}));
			color(serverJarLabel, Color.RED);
		}
	}

	private void doServerJarDownload() {
		downloadButton.setEnabled(false);
		if (minecraftJar.exists()) {
			minecraftJar.delete();
		}
		new Thread(() -> {
			try {
				URL url = new URL(VersionData.SERVER_URL);
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
				int finalSize = httpConnection.getContentLength();

				BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());

				FileOutputStream outputStream = new FileOutputStream(minecraftJar);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, 1024);

				byte[] buffer = new byte[1024];
				long downloaded = 0;
				int len;
				while ((len = inputStream.read(buffer, 0, 1024)) >= 0) {
					downloaded += len;

					final String labelText = String.format("Downloading %d/%d MB", downloaded / MB, finalSize / MB);
					SwingUtilities.invokeLater(() -> color(serverJarLabel, Color.BLUE).setText(labelText));

					bufferedOutputStream.write(buffer, 0, len);
				}
				bufferedOutputStream.close();
				inputStream.close();

				try (PrintWriter out = new PrintWriter(new File(installDir, "fabric-server-launcher.properties"))) {
					out.println("serverJar=" + minecraftVersion + ".jar");
				}

				updateServerJarLabel();
				downloadButton.setEnabled(true);

			} catch (IOException e) {
				color(serverJarLabel, Color.RED).setText(e.getMessage());
				serverHandler.error(e);
			}
		}).start();
	}

	private void generateLaunchScripts() {
		String launchCommand = "java -jar fabric-server-launch.jar";

		Map<File, String> launchScripts = new HashMap<>();
		launchScripts.put(new File(installDir, "start.bat"), launchCommand + "\npause");
		launchScripts.put(new File(installDir, "start.sh"), "#!/usr/bin/env bash\n" + launchCommand);

		boolean exists = launchScripts.entrySet().stream().anyMatch(entry -> entry.getKey().exists());
		if (exists && (JOptionPane.showConfirmDialog(this, Utils.BUNDLE.getString("prompt.server.overwrite"), "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
			return;
		}

		launchScripts.forEach((file, s) -> {
			try {
				Utils.writeToFile(file, s);
				file.setExecutable(true, false);
			} catch (FileNotFoundException e) {
				serverHandler.error(e);
			}
		});
	}

	private JLabel fontSize(JLabel label, int size) {
		label.setFont(new Font(label.getFont().getName(), Font.PLAIN, size));
		return label;
	}

	private JLabel color(JLabel label, Color color) {
		label.setForeground(color);
		return label;
	}

	private void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

	public static void show(ServerHandler serverHandler) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		ServerPostInstallDialog dialog = new ServerPostInstallDialog(serverHandler);
		dialog.pack();
		dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
		dialog.setLocationRelativeTo(InstallerGui.instance);
		dialog.setVisible(true);
	}
}
