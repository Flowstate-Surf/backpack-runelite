package com.backpack;

import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@Slf4j
final class DiscordWebhookClient
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private final OkHttpClient httpClient;

	@Inject
	DiscordWebhookClient(OkHttpClient httpClient)
	{
		this.httpClient = httpClient;
	}

	void send(String webhookUrl, String message)
	{
		if (!isDiscordWebhook(webhookUrl))
		{
			log.warn("Backpack ignored an invalid Discord webhook URL");
			return;
		}

		String json = "{\"content\":\"" + escape(message) + "\",\"allowed_mentions\":{\"parse\":[]}}";
		Request request = new Request.Builder()
			.url(webhookUrl)
			.post(RequestBody.create(JSON, json))
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException exception)
			{
				log.warn("Unable to post Backpack event to Discord", exception);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					if (!response.isSuccessful())
					{
						log.warn("Discord webhook returned HTTP {}", response.code());
					}
				}
			}
		});
	}

	void send(String webhookUrl, String message, BufferedImage screenshot)
	{
		if (!isDiscordWebhook(webhookUrl))
		{
			log.warn("Backpack ignored an invalid Discord webhook URL");
			return;
		}
		try
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(screenshot, "png", output);
			String json = "{\"content\":\"" + escape(message) + "\",\"allowed_mentions\":{\"parse\":[]}}";
			RequestBody body = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", json)
				.addFormDataPart("files[0]", "drop.png", RequestBody.create(MediaType.get("image/png"), output.toByteArray()))
				.build();
			enqueue(webhookUrl, body);
		}
		catch (IOException exception)
		{
			log.warn("Unable to encode Backpack screenshot", exception);
		}
	}

	private void enqueue(String webhookUrl, RequestBody body)
	{
		Request request = new Request.Builder().url(webhookUrl).post(body).build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override public void onFailure(Call call, IOException exception) { log.warn("Unable to post Backpack event to Discord", exception); }
			@Override public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					if (!response.isSuccessful()) log.warn("Discord webhook returned HTTP {}", response.code());
				}
			}
		});
	}

	private static boolean isDiscordWebhook(String value)
	{
		if (value == null || value.length() > 2048) return false;
		try
		{
			URI uri = new URI(value);
			String host = uri.getHost();
			return "https".equalsIgnoreCase(uri.getScheme())
				&& ("discord.com".equalsIgnoreCase(host) || "discordapp.com".equalsIgnoreCase(host))
				&& uri.getPath() != null && uri.getPath().startsWith("/api/webhooks/");
		}
		catch (URISyntaxException ignored)
		{
			return false;
		}
	}

	private static String escape(String value)
	{
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
	}
}
