package com.iclinic.iclinicbackend.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TelegramWebhookBaseUrlResolver {

    private static final Pattern CLOUDFLARE_URL_PATTERN =
            Pattern.compile("https://[a-zA-Z0-9-]+\\.trycloudflare\\.com");

    @Value("${telegram.webhook.base-url:}")
    private String staticWebhookBaseUrl;

    @Value("${telegram.webhook.cloudflare.enabled:false}")
    private boolean cloudflareEnabled;

    @Value("${telegram.webhook.cloudflare.command:cloudflared}")
    private String cloudflareCommand;

    @Value("${telegram.webhook.cloudflare.local-port:${server.port:8080}}")
    private int localPort;

    @Value("${telegram.webhook.cloudflare.startup-timeout-seconds:25}")
    private long startupTimeoutSeconds;

    private volatile String resolvedCloudflareUrl;
    private volatile Process cloudflareProcess;

    public synchronized String resolveBaseUrl() {
        if (isNotBlank(staticWebhookBaseUrl)) {
            return trimTrailingSlash(staticWebhookBaseUrl.trim());
        }

        if (!cloudflareEnabled) {
            return "";
        }

        if (isNotBlank(resolvedCloudflareUrl)) {
            return resolvedCloudflareUrl;
        }

        return startCloudflareTunnelAndResolveUrl();
    }

    private String startCloudflareTunnelAndResolveUrl() {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    cloudflareCommand,
                    "tunnel",
                    "--url",
                    "http://localhost:" + localPort,
                    "--no-autoupdate"
            );
            builder.redirectErrorStream(true);

            cloudflareProcess = builder.start();
            log.info("Iniciando Cloudflare Tunnel para Telegram webhook en puerto local {}", localPort);

            startOutputReader(cloudflareProcess);

            Instant deadline = Instant.now().plus(Duration.ofSeconds(startupTimeoutSeconds));
            while (Instant.now().isBefore(deadline)) {
                if (isNotBlank(resolvedCloudflareUrl)) {
                    log.info("Cloudflare Tunnel listo: {}", resolvedCloudflareUrl);
                    return resolvedCloudflareUrl;
                }
                if (!cloudflareProcess.isAlive()) {
                    break;
                }
                Thread.sleep(200);
            }

            log.warn("Cloudflare Tunnel no devolvio URL publica dentro de {}s", startupTimeoutSeconds);
            return "";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Inicializacion de Cloudflare Tunnel interrumpida");
            return "";
        } catch (IOException ex) {
            log.warn("No se pudo iniciar cloudflared ({}). Se omitira auto-registro de webhook: {}",
                    cloudflareCommand,
                    ex.getMessage());
            return "";
        }
    }

    private void startOutputReader(Process process) {
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!isNotBlank(resolvedCloudflareUrl)) {
                        var matcher = CLOUDFLARE_URL_PATTERN.matcher(line);
                        if (matcher.find()) {
                            resolvedCloudflareUrl = trimTrailingSlash(matcher.group());
                        }
                    }
                }
            } catch (IOException ex) {
                log.debug("Finalizo lectura de salida de cloudflared: {}", ex.getMessage());
            }
        }, "cloudflared-output-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    @PreDestroy
    void stopTunnel() {
        Process process = cloudflareProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
            log.info("Cloudflare Tunnel detenido");
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}