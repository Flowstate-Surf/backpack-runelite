package com.backpack;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
final class BackpackApiClient
{
	private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
	private final OkHttpClient http;
	private final Gson gson;
	private final BackpackConfig config;
	@Inject BackpackApiClient(OkHttpClient http, Gson gson, BackpackConfig config) { this.http = http; this.gson = gson; this.config = config; }

	void create(String base, String team, String player, Consumer<Session> success, Consumer<String> failure)
	{
		post(base, "/v1/teams/create", Map.of("name", team, "player", player), null, success, failure);
	}
	void join(String base, String code, String player, Consumer<Session> success, Consumer<String> failure)
	{
		post(base, "/v1/teams/join", Map.of("code", code, "player", player), null, success, failure);
	}
	void event(String base, String token, String type, long amount, long sequence)
	{
		post(base, "/v1/events", Map.of("type", type, "amount", amount, "sequence", sequence), token, ignored -> {}, error -> log.warn("Backpack sync event failed: {}", error));
	}
	private void post(String base, String path, Object payload, String token, Consumer<Session> success, Consumer<String> failure)
	{
		if (!validBase(base)) { failure.accept("Invalid server URL"); return; }
		Request.Builder builder = new Request.Builder().url(base.replaceAll("/$", "") + path).post(RequestBody.create(JSON_TYPE, gson.toJson(payload)));
		if (token != null) builder.header("Authorization", "Bearer " + token);
		if (!config.serverEnabled()) { failure.accept("Backpack server is disabled"); return; }
		http.newCall(builder.build()).enqueue(new Callback()
		{
			@Override public void onFailure(Call call, IOException e) { SwingUtilities.invokeLater(() -> failure.accept("Server unavailable")); }
			@Override public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					String body = response.body() == null ? "" : response.body().string();
					if (!response.isSuccessful()) { SwingUtilities.invokeLater(() -> failure.accept("Server returned " + response.code())); return; }
					Session session = gson.fromJson(body, Session.class); SwingUtilities.invokeLater(() -> success.accept(session));
				}
				catch (IOException | RuntimeException e) { SwingUtilities.invokeLater(() -> failure.accept("Invalid server response")); }
			}
		});
	}
	private static boolean validBase(String value)
	{
		try { URI uri = URI.create(value); return "https".equalsIgnoreCase(uri.getScheme()) || ("http".equalsIgnoreCase(uri.getScheme()) && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))); }
		catch (RuntimeException e) { return false; }
	}
	static final class Session { RemoteTeam team; String memberId; String token; }
	static final class RemoteTeam { String code; String name; Map<String, RemoteMember> members; }
	static final class RemoteMember { String id; String name; String role; long points; long xp; }
}
