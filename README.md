# A Telegram Appender for Logback #

## Introduction ##
Instead of applying complicated monitoring system to your appserver log file or using SMTPAppender relying on *if and when* you will read your email, you can get immediate alerts on a Telegram chat (with your bot) for critical errors by using a Telegram Appender.

## Logback configuration ##
A sample configuration `logback.xml` is shown below

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<appender name="TELEGRAM"
		class="com.github.paolodenti.telegram.logback.TelegramAppender">
		<botToken>123456789123456789123456789123456789123456789</botToken>
		<chatId>123456789</chatId>
		<Layout class="ch.qos.logback.classic.PatternLayout">
			<Pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</Pattern>
		</Layout>
	</appender>

	<root level="error">
		<appender-ref ref="TELEGRAM" />
	</root>
</configuration>
```

The `botToken` and `chatId` must be replaced with real values, **do not use the above values**.

The appender is intended to be used sparingly, just for critical errors because of the inherently low speed of http connection needed to send telegrams.

The following are optional properties:

* `minInterval`: threshold in milliseconds for message sending. If `minInterval` msecs are not passed from the last sent telegram, the new telegram is just discarded. Default value is 5000.
* `connectTimeout`: connection timeout to Telegram servers, in seconds. Default value is 5.
* `connectionRequestTimeout`: connection request timeout to Telegram servers, in seconds. Default value is 5.
* `socketTimeout`: socketTimeout to Telegram servers, in seconds. Default value is 5.

## A real usage scenario ##
Telegram appender should not be your only appender; telegram is to be used for critical errors.
This is an example of logback configuration with multiple appenders, using telegram appender only on error level.

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
		</encoder>
	</appender>

	<appender name="TELEGRAM"
		class="com.github.paolodenti.telegram.logback.TelegramAppender">
		<botToken>123456789123456789123456789123456789123456789</botToken>
		<chatId>123456789</chatId>
		<Layout class="ch.qos.logback.classic.PatternLayout">
			<Pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</Pattern>
		</Layout>
		<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
		    <level>ERROR</level>
		</filter>
	</appender>

	<logger name="com.github.paolodenti.telegram.logback" level="DEBUG" />

	<root level="info">
		<appender-ref ref="STDOUT" />
		<appender-ref ref="TELEGRAM" />
	</root>
</configuration>
```

## Maven usage ##
`telegram-logback` is available on maven public repository. You just need to add the maven dependency

```
<dependency>
    <groupId>com.github.paolodenti</groupId>
    <artifactId>telegram-logback</artifactId>
    <version>1.1.1</version>
</dependency>

```

## Project page and Javadocs ##
You can browse the project page and javadocs here: [telegram-logback](http://paolodenti.github.io/telegram-logback)

## How get botToken and chatId ##
In case you do not know how to get your `botToken` and `chatId`, these are simple instruction to follow in order to get the necessary configuration information.

As described in the Telegram Bot API, this is the manual procedure needed in order to get the necessary information.

1. Create the Bot and get the token
	* On a Telegram client open a chat with BotFather.
	* write `/newbot` to BotFather, fill all the needed information, write down the token. This is the `botToken` needed.
2. Create the destination chat and get the `chatId`
	* Open a new chat with your new Bot and post a message on the chat
	* Open a browser and invoke `https://api.telegram.org/bot<botToken>/getUpdates` (where `<botToken>` is the `botToken` previously obtained)
	* Look at the JSON result and write down the value of `result[0].message.from.id`. That is the chatId.
