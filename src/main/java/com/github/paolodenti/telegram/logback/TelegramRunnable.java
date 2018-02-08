package com.github.paolodenti.telegram.logback;

import org.apache.http.client.config.RequestConfig;

/**
*
* @author Paolo Denti
*
* Not blocking sender thread
*
*/
public class TelegramRunnable implements Runnable {

	protected static final String TELEGRAM_SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";

	private RequestConfig requestConfig;
	private String botToken;
	private String chatId;
	private String messageToSend;
	private int maxMessageSize;
	private boolean splitMessage;

	public TelegramRunnable(RequestConfig requestConfig, String botToken, String chatId, String messageToSend, int maxMessageSize, boolean splitMessage) {
		this.requestConfig = requestConfig;
		this.botToken = botToken;
		this.chatId = chatId;
		this.messageToSend = messageToSend;
		this.maxMessageSize = maxMessageSize;
		this.splitMessage = splitMessage;
	}

	@Override
	public void run() {
		TelegramUtils.sendTelegramMessages(requestConfig, botToken, chatId, messageToSend, maxMessageSize, splitMessage);
	}
}
