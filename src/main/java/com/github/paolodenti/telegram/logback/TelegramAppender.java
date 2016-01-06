package com.github.paolodenti.telegram.logback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

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

	private static final String TELEGRAM_SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";

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

	public void setLayout(Layout<E> layout) {
		this.layout = layout;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	private long lastTimeSentTelegram = 0;

	private RequestConfig requestConfig;

	public void setMinInterval(String minInterval) {
		try {
			this.minInterval = Integer.parseInt(minInterval);
		} catch (NumberFormatException e) {
			addStatus(new ErrorStatus("Bad minInterval for the appender named \"" + name + "\". Leaving to defaultValue ", this));
		}
	}

	public void setSocketTimeout(String socketTimeout) {
		try {
			this.socketTimeout = Integer.parseInt(socketTimeout);
		} catch (NumberFormatException e) {
			addStatus(new ErrorStatus("Bad socketTimeout for the appender named \"" + name + "\". Leaving to defaultValue ", this));
		}
	}

	public void setConnectTimeout(String connectTimeout) {
		try {
			this.connectTimeout = Integer.parseInt(connectTimeout);
		} catch (NumberFormatException e) {
			addStatus(new ErrorStatus("Bad connectTimeout for the appender named \"" + name + "\". Leaving to defaultValue ", this));
		}
	}

	public void setConnectionRequestTimeout(String connectionRequestTimeout) {
		try {
			this.connectionRequestTimeout = Integer.parseInt(connectionRequestTimeout);
		} catch (NumberFormatException e) {
			addStatus(new ErrorStatus("Bad connectionRequestTimeout for the appender named \"" + name + "\". Leaving to defaultValue ", this));
		}
	}

	@Override
	public void start() {
		int errors = 0;

		if (this.botToken == null) {
			addStatus(new ErrorStatus("No botToken set for the appender named \"" + name + "\".", this));
			errors++;
		}

		if (this.chatId == null) {
			addStatus(new ErrorStatus("No chatId set for the appender named \"" + name + "\".", this));
			errors++;
		}

		if (this.minInterval < 0) {
			addStatus(new ErrorStatus("Bad minInterval for the appender named \"" + name + "\".", this));
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

				CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
				try {
					String telegramSendMessageUrl = String.format(TELEGRAM_SEND_MESSAGE_URL, botToken);
					HttpPost httpPost = new HttpPost(telegramSendMessageUrl);
					httpPost.setConfig(requestConfig);
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					nvps.add(new BasicNameValuePair("chat_id", chatId));
					nvps.add(new BasicNameValuePair("text", messageToSend));

					try {
						httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
						CloseableHttpResponse response = httpclient.execute(httpPost);
						try {
							if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
								System.err.println("Appender [" + name + "] failed to send telegram with http code " + response.getStatusLine().getStatusCode());
							}
						} finally {
							response.close();
						}
					} catch (IOException e) {
						System.err.println("Appender [" + name + "] failed to send telegram: " + e.getMessage());
					}
				} finally {
					try {
						httpclient.close();
					} catch (IOException e) {
					}
				}

				lastTimeSentTelegram = now;
			}

		} finally {
			lock.unlock();
		}
	}
}
