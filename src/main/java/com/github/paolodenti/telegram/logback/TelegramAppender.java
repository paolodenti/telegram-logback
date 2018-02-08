package com.github.paolodenti.telegram.logback;

import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.client.config.RequestConfig;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.status.ErrorStatus;

/**
 *
 * @author Paolo Denti
 *
 * TelegramAppender appends log to a Telegram chat by using a Telegram BOT, via Telegram BOT Api (https://core.telegram.org/bots/api)
 * The append log execution is slow; use it only for critical errors
 *
 */
public class TelegramAppender<E> extends UnsynchronizedAppenderBase<E> {

	/**
	 * All synchronization in this class is done via the lock object.
	 */
	protected final ReentrantLock lock = new ReentrantLock(true);

	/**
	 * It is the layout used for message formatting
	 */
	protected Layout<E> layout;

	/**
	 * The telegram bot token.
	 */
	private String botToken = null;

	/**
	 * The telegram chat id.
	 */
	private String chatId = null;

	/**
	 * The minimum interval allowed between each telegram message
	 */
	private int minInterval = 5000;

	/**
	 * the connection timeout
	 */
	private int connectTimeout = 5;

	/**
	 * the connection timeout
	 */
	private int connectionRequestTimeout = 5;

	/**
	 * the connection timeout
	 */
	private int socketTimeout = 5;

	/**
	 * max telegram message size
	 */
	private int maxMessageSize = 1024;

	/**
	 * split in chunks or truncate
	 */
	private boolean splitMessage = true;

	/**
	 * send each telegram in separate thread
	 */
	private boolean nonBlocking = true;

	public void setLayout(Layout<E> layout) {
		this.layout = layout;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	public void setMinInterval(String minInterval) {
		try {
			this.minInterval = Integer.parseInt(minInterval);
		} catch (NumberFormatException e) {
			internalAddStatus("Bad minInterval");
		}
	}

	public void setConnectTimeout(String connectTimeout) {
		try {
			this.connectTimeout = Integer.parseInt(connectTimeout);
		} catch (NumberFormatException e) {
			internalAddStatus("Bad connectTimeout");
		}
	}

	public void setConnectionRequestTimeout(String connectionRequestTimeout) {
		try {
			this.connectionRequestTimeout = Integer.parseInt(connectionRequestTimeout);
		} catch (NumberFormatException e) {
			internalAddStatus("Bad connectionRequestTimeout");
		}
	}

	public void setSocketTimeout(String socketTimeout) {
		try {
			this.socketTimeout = Integer.parseInt(socketTimeout);
		} catch (NumberFormatException e) {
			internalAddStatus("Bad socketTimeout");
		}
	}

	public void setMaxMessageSize(String maxMessageSize) {
		try {
			this.maxMessageSize = Integer.parseInt(maxMessageSize);
		} catch (NumberFormatException e) {
			internalAddStatus("Bad maxMessageSize");
		}
	}

	public void setSplitMessage(String splitMessage) {
		this.splitMessage = Boolean.parseBoolean(splitMessage);
	}

	public void setNonBlocking(String nonBlocking) {
		this.nonBlocking = Boolean.parseBoolean(nonBlocking);
	}

	private long lastTimeSentTelegram = 0;

	private RequestConfig requestConfig;

	@Override
	public void start() {
		int errors = 0;

		if (this.layout == null) {
			internalAddStatus("No layout set");
			errors++;
		}

		if (this.botToken == null) {
			internalAddStatus("No botToken set");
			errors++;
		}

		if (this.chatId == null) {
			internalAddStatus("No chatId set");
			errors++;
		}

		if (this.minInterval < 0) {
			internalAddStatus("Bad minInterval");
			errors++;
		}

		if (this.connectTimeout < 0) {
			internalAddStatus("Bad connectTimeout");
			errors++;
		}

		if (this.connectionRequestTimeout < 0) {
			internalAddStatus("Bad connectionRequestTimeout");
			errors++;
		}

		if (this.socketTimeout < 0) {
			internalAddStatus("Bad socketTimeout");
			errors++;
		}

		if (this.maxMessageSize <= 0) {
			internalAddStatus("Bad maxMessageSize");
			errors++;
		}

		if (errors == 0) {
			requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout * 1000).setConnectionRequestTimeout(connectionRequestTimeout * 1000).setSocketTimeout(socketTimeout * 1000).build();

			super.start();
		}
	}

	@Override
	protected void append(E eventObject) {
		if (!isStarted()) {
			return;
		}

		sendTelegramMessage(eventObject);
	}

	protected void sendTelegramMessage(E eventObject) {
		lock.lock();
		try {
			String messageToSend = layout.doLayout(eventObject);

			long now = System.currentTimeMillis();
			if (lastTimeSentTelegram == 0 || (lastTimeSentTelegram + minInterval < now)) {
				if (nonBlocking) {
					new Thread(new TelegramRunnable(requestConfig, botToken, chatId, messageToSend, maxMessageSize, splitMessage)).start();
				} else {
					TelegramUtils.sendTelegramMessages(requestConfig, botToken, chatId, messageToSend, maxMessageSize, splitMessage);
				}
				lastTimeSentTelegram = now;
			}
		} finally {
			lock.unlock();
		}
	}

	private static final String MSG_FORMAT = "%s for the appender named '%s'.";

	private void internalAddStatus(String msgPrefix) {
		addStatus(new ErrorStatus(String.format(MSG_FORMAT, msgPrefix, name), this));
	}
}
