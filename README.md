# Twitter Sentiment analysis with Elastic Graph API

This is a simple Java application for live streaming tweets, adding some simple sentiment analysis to each, and indexing them in Elastic. You can then use the standard Elastic tools (Kibana, Graph, Timelion) to analyze and visualize tweets, hashtags, locations and sentiment.

## Setup

#### Configure your twitter API credentials

Add a `twitter4j.properties` file to the resource folder with your Twitter API credentials.

	debug=true
	http.prettyDebug=true
	oauth.consumerKey=<..>
	oauth.consumerSecret=<..>
	oauth.accessToken=<..>
	oauth.accessTokenSecret=<..>

#### Initialize Elastic index

	curl -XPUT http://localhost:9200/_template/elastic -u elastic -d @template.json
	curl -XPUT -u elastic http://localhost:9200/tweets
  
