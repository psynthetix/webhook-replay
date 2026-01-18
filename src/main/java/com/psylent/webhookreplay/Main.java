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

@Command(
        name = "webhook-replay",
        description = "Replay webhook payloads to any HTTP endpoint",
        version = "webhook-replay 1.0.0"
)
public class Main implements Callable<Integer> {

    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Show this help message and exit."
    )
    boolean helpRequested;

    @Option(
            names = {"-V", "--version"},
            versionHelp = true,
            description = "Print version information and exit."
    )
    boolean versionRequested;

    @Option(
            names = {"-u", "--url"},
            required = true,
            description = "Target webhook URL"
    )
    String url;

    @Option(
            names = {"-f", "--file"},
            required = true,
            description = "JSON payload file"
    )
    Path file;

    @Option(
            names = {"-H", "--header"},
            description = "Custom header (key=value). Can be specified multiple times.",
            split = ","
    )
    String[] headers = new String[0];

    @Option(
            names = {"-r", "--repeat"},
            defaultValue = "1",
            description = "Number of times to send the payload"
    )
    int repeat;

    @Option(
            names = {"-d", "--delay"},
            defaultValue = "0",
            description = "Delay between sends in milliseconds"
    )
    long delayMs;

    @Override
    public Integer call() throws Exception {
        String payload = Files.readString(file);
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 1; i <= repeat; i++) {

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            for (String header : headers) {
                String[] parts = header.split("=", 2);
                if (parts.length == 2) {
                    request.header(parts[0], parts[1]);
                }
            }

            HttpResponse<String> response =
                    client.send(request.build(), HttpResponse.BodyHandlers.ofString());

            System.out.printf(
                    "Send %d/%d → HTTP %d%n",
                    i, repeat, response.statusCode()
            );

            if (delayMs > 0 && i < repeat) {
                Thread.sleep(delayMs);
            }
        }

        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
