import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawlerMultiThread {

    private Map<String, Integer> visitedUrls;
    private Deque<String> urlsQueue;
    private Instant startTime;
    private final int maxDepth = 4;
    private final int maxUrlsPerPage = 10;
    private final int numThreads = 4; // Number of threads to use for crawling

    public WebCrawlerMultiThread(Instant start) {
        visitedUrls = new HashMap<>();
        urlsQueue = new LinkedList<>();
        startTime = start;
    }

    public void crawl(String rootUrl) throws InterruptedException, ExecutionException {
        urlsQueue.addLast(rootUrl);
        visitedUrls.put(rootUrl, 1);

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        while (!urlsQueue.isEmpty()) {
            String url = urlsQueue.removeFirst();
            int depth = visitedUrls.get(url);

            if (depth < maxDepth) {
                Future<Void> future = executorService.submit(new CrawlTask(url, depth));
                future.get(); // Wait for the task to complete
            }
        }

        executorService.shutdown();
    }

    private class CrawlTask implements Callable<Void> {
        private final String url;
        private final int depth;

        public CrawlTask(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }

        @Override
        public Void call() throws Exception {
            try {
                URL urlObject = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(urlObject.openStream()));
                String inputLine = in.readLine();
                String rawHtml = "";
                while (inputLine != null) {
                    rawHtml += inputLine;
                    inputLine = in.readLine();
                }

                in.close();
                parseAndAddUrls(rawHtml, depth);
            } catch (IOException e) {
                // Handle exceptions
            }
            return null;
        }
    }

    private void parseAndAddUrls(String rawHtml, int depth) {
        String urlPattern = "((\\/wiki\\/)+[^\\s\\.\\#\\:\"]+[\\w])\"";
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(rawHtml);

        int cntUrlsPerPage = 0;

        while (matcher.find()) {
            String newUrl = matcher.group(1);
            newUrl = "https://en.wikipedia.org" + newUrl;

            if (!visitedUrls.containsKey(newUrl)) {
                urlsQueue.addLast(newUrl);
                visitedUrls.put(newUrl, depth + 1);
                cntUrlsPerPage += 1;

                if (cntUrlsPerPage >= maxUrlsPerPage) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        Instant start = Instant.now();
        WebCrawlerMultiThread crawler = new WebCrawlerMultiThread(start);
        crawler.crawl("https://en.wikipedia.org/wiki/Travelling_salesman_problem");

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("Visited " + crawler.visitedUrls.size() + " Urls in " + totalTime / 1000000 + " ms");
    }
}