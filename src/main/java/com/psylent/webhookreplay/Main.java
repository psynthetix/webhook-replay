package com.psylent.webhookreplay;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;

@Command(name = "webhook-replay", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @Option(names = "--url", required = true)
    String url;

    @Option(names = "--file", required = true)
    Path file;

    @Option(names = "--header", split = ",")
    String[] headers = new String[0];

    @Override
    public Integer call() throws Exception {
        String payload = Files.readString(file);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        for (String h : headers) {
            String[] p = h.split("=", 2);
            if (p.length == 2) req.header(p[0], p[1]);
        }

        HttpResponse<String> res =
                client.send(req.build(), HttpResponse.BodyHandlers.ofString());

        System.out.println("HTTP " + res.statusCode());
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
