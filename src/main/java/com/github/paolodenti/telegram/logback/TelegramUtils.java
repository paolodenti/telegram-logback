package com.github.paolodenti.telegram.logback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
*
* @author Paolo Denti
*
* Telegram sending methods
*
*/
public class TelegramUtils {

	private TelegramUtils() {
	}

	protected static final String TELEGRAM_SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";

	public static void sendTelegramMessages(RequestConfig requestConfig, String botToken, String chatId, String messageToSend, String messageParseMode, int maxMessageSize, boolean splitMessage) {
		List<String> chunks = splitInTelegramChunks(messageToSend, maxMessageSize);
		for (String chunk : chunks) {
			sendTelegramMessage(requestConfig, botToken, chatId, chunk, messageParseMode);
			if (!splitMessage) {
				break;
			}
		}
	}

	private static void sendTelegramMessage(RequestConfig requestConfig, String botToken, String chatId, String chunk, String messageParseMode) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		try {
			String telegramSendMessageUrl = String.format(TELEGRAM_SEND_MESSAGE_URL, botToken);
			HttpPost httpPost = new HttpPost(telegramSendMessageUrl);
			httpPost.setConfig(requestConfig);
			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("chat_id", chatId));
			nvps.add(new BasicNameValuePair("text", chunk));
			if(messageParseMode != null) {
				nvps.add(new BasicNameValuePair("parse_mode", messageParseMode));
			}

			try {
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
				CloseableHttpResponse response = httpclient.execute(httpPost);
				try {
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						// logging errors on stderr
						System.err.println(String.format("Send telegram failed with http code %d", response.getStatusLine().getStatusCode()));
					}
				} finally {
					response.close();
				}
			} catch (IOException e) {
				// logging errors on stderr
				System.err.println(String.format("Send telegram failed with exception {%s}", e.getMessage()));
			}
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				// nothing to do
			}
		}
	}

	private static List<String> splitInTelegramChunks(String text, int size) {
		List<String> chunks = new ArrayList<>((text.length() + size - 1) / size);

		for (int start = 0; start < text.length(); start += size) {
			chunks.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return chunks;
	}
}
