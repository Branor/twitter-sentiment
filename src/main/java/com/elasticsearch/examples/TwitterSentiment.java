/**
 * Created by davido on 11/13/17.
 */

package com.elasticsearch.examples;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import twitter4j.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class TwitterSentiment {
    private static final Map<Integer, String> scores;
    private static LinkedBlockingQueue<Status> tweets;
    private static LinkedBlockingQueue<Map<String,Object>> tweetsWithSentiment;
    private static TransportClient client;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "Very Negative");
        map.put(1, "Negative");
        map.put(2, "Neutral");
        map.put(3, "Positive");
        map.put(4, "Very Positive");
        scores = Collections.unmodifiableMap(map);
        tweets = new LinkedBlockingQueue<>(10);
        tweetsWithSentiment = new LinkedBlockingQueue<>(10);
        try {
//            client = new PreBuiltTransportClient(Settings.EMPTY)
//                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));

            client = new PreBuiltXPackTransportClient(Settings.builder()
                    .put("cluster.name", "owlbear")
                    .put("xpack.security.user", "elastic:123456")
                    .build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9301));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public static void main(String [] args) {

        NLP.init();
        StatusListener listener = getStatusListener();

        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);

        FilterQuery filter = new FilterQuery();
        String[] keywordsArray = { "trump", "paradise", "papers","russia", "putin", "usa" };
        filter.track(keywordsArray);
        filter.language("en");
//        double[][] bb= {{-180, -90}, {180, 90}};
//        filter.locations(bb);
        twitterStream.filter(filter);

        StartSentimentProcessor(4);

        StartElasticIndexer(1);

    }

    private static void StartElasticIndexer(int count) {
        for(int i =0; i< count; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        Map<String, Object> json = tweetsWithSentiment.take();
                        IndexResponse response = client.prepareIndex("tweets", "tweet")
                                .setSource(json)
                                .get();
                        if (response.status().getStatus() == 201)
                            System.out.println("Tweet from " + json.get("user") + " indexed successfully.");
                        else
                            System.out.printf(response.toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Elastic-Indexer").start();
        }
    }

    private static void StartSentimentProcessor(int count) {
        for(int i=0; i<count; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        Status status = tweets.take();
                        Map<String, Object> json = GetTweetWithSentiment(status);
                        tweetsWithSentiment.put(json);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Sentiment-Processor").start();
        }
    }

    private static Map<String, Object> GetTweetWithSentiment(Status status) {
        Map<String,Object> json = GetTweetJson(status);
        int sentimentScore = NLP.findSentiment((String)json.get("text"));
        String sentiment = scores.get(sentimentScore);

        json.put("sentimentScore", sentimentScore);
        json.put("sentiment", sentiment);

        return json;
    }

    private static Map<String,Object> GetTweetJson(Status status) {
        Map<String,Object> json = new HashMap<>();

        json.put("text", status.getText());
        json.put("user", status.getUser().getScreenName());
        json.put("date", status.getCreatedAt());
        json.put("userFollowersCount", status.getUser().getFollowersCount());
        json.put("userStatusesCount", status.getUser().getStatusesCount());
        json.put("hashtags", Stream.of(status.getHashtagEntities()).map(h -> h.getText()).toArray());
        json.put("usernames", Stream.of(status.getUserMentionEntities()).map(u -> u.getScreenName()).toArray());
        if(status.getGeoLocation() != null)
            json.put("location", new double[] {status.getGeoLocation().getLongitude(), status.getGeoLocation().getLatitude()});

        return json;
    }

    private static StatusListener getStatusListener() {
        return new StatusListener() {
            @Override
            public void onStatus(Status status) {
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
                try {
                    tweets.put(status);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
    }
}
