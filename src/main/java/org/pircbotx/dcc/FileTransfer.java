/**
 * Copyright (C) 2010-2014 Leon Blakey <lord.quackstar at gmail.com>
 *
 * This file is part of PircBotX.
 *
 * PircBotX is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * PircBotX is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * PircBotX. If not, see <http://www.gnu.org/licenses/>.
 */
package org.pircbotx.dcc;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.dcc.DccHandler.PendingFileTransfer;

/**
 * A general active DCC file transfer
 *
 * @author Leon Blakey
 */
@Slf4j
public abstract class FileTransfer {
	@NonNull
	protected final PircBotX bot;
	@NonNull
	protected final Configuration configuration;
	@NonNull
	protected final DccHandler dccHandler;
	@NonNull
	protected Socket socket;
	@NonNull
	@Getter
	protected final User user;
	@NonNull
	@Getter
	protected final File file;
	@Getter
	protected FileTransferStatus fileTransferStatus;

	protected PendingFileTransfer pendingFileTransfer;

	protected final Object stateLock = new Object();

	public FileTransfer(PircBotX bot, DccHandler dccHandler, PendingFileTransfer pendingFileTransfer,
			File file) {
		this.bot = bot;
		this.configuration = bot.getConfiguration();
		this.pendingFileTransfer = pendingFileTransfer;
		this.user = pendingFileTransfer.user;
		this.file = file;
		this.dccHandler = dccHandler;
		fileTransferStatus = new FileTransferStatus(pendingFileTransfer.fileSize, pendingFileTransfer.position);
	}

	private void connectSocket() throws IOException {
		socket = dccHandler.establishSocketConnection(pendingFileTransfer);
	}

	public void shutdown() {
		fileTransferStatus.dccState = DccState.SHUTDOWN;
	}

	/**
	 * Transfer the file to the user
	 *
	 * @throws IOException If an error occurred during transfer
	 */
	public void transfer() {

		// Prevent being called multiple times
		if (fileTransferStatus.dccState != DccState.INIT) {
			synchronized (stateLock) {
				if (fileTransferStatus.dccState != DccState.INIT) {
					throw new RuntimeException(
							"Cannot receive file twice (Current state: " + fileTransferStatus.dccState + ")");
				}
			}
		}

		fileTransferStatus.dccState = DccState.CONNECTING;

		try {
			connectSocket();
		} catch (IOException e) {
			log.error("FAILED ESTABLISHING SOCKET!!!", e);
			return;
		}

		fileTransferStatus.dccState = DccState.RUNNING;

		transferFile();

	}

	protected abstract void transferFile();

}
