/**
 * Created by davido on 11/13/17.
 */

package com.elasticsearch.examples;

import twitter4j.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TwitterSentiment {
    private static final Map<Integer, String> scores;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "Very Negative");
        map.put(1, "Negative");
        map.put(2, "Neutral");
        map.put(3, "Positive");
        map.put(4, "Very Positive");
        scores = Collections.unmodifiableMap(map);
    }

    public static void main(String [] args) {

        StatusListener listener = getStatusListener();

        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);

        FilterQuery filter = new FilterQuery();
        String[] keywordsArray = { "trump", "paradise", "papers" };
        filter.track(keywordsArray);
        twitterStream.filter(filter);
    }

    private static StatusListener getStatusListener() {
        return new StatusListener() {
            @Override
            public void onStatus(Status status) {
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
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
